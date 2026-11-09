package com.aistudio.drinkyourwater.hydra.billing

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Play Billing through RevenueCat. RevenueCat owns receipt validation and entitlement state; the
 * app only reads the active entitlements ("starter", "builder", "fullstack") and the current
 * offering's packages ("<tier>_monthly" / "<tier>_yearly").
 */
class RevenueCatBillingManager private constructor() : BillingManager {

    private val _state = MutableStateFlow(BillingState(isAvailable = true, isLoading = true))
    override val state: StateFlow<BillingState> = _state

    // Packages from the last offerings fetch, so a Plan can be turned back into a purchase.
    private var packages: Map<String, Package> = emptyMap()

    private fun listenForChanges() {
        Purchases.sharedInstance.updatedCustomerInfoListener = com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener { info ->
            _state.update { it.copy(tier = tierOf(info)) }
        }
    }

    override suspend fun refresh() {
        _state.update { it.copy(isLoading = true, message = null) }
        val info = fetchCustomerInfo()
        val offerings = fetchOfferings()
        _state.update { current ->
            current.copy(
                isLoading = false,
                tier = info?.let(::tierOf) ?: current.tier,
                plans = offerings?.let(::plansOf) ?: current.plans,
                message = if (info == null && offerings == null) "Couldn't reach the store. Check your connection." else null
            )
        }
    }

    override suspend fun purchase(activity: Activity, plan: Plan): PurchaseResult {
        val pkg = packages[plan.packageId] ?: return PurchaseResult.Failed("That plan isn't available right now.")
        return suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.purchase(
                PurchaseParams.Builder(activity, pkg).build(),
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        val tier = tierOf(customerInfo)
                        _state.update { it.copy(tier = tier) }
                        if (cont.isActive) cont.resume(PurchaseResult.Success(tier))
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        val result = if (userCancelled || error.code == PurchasesErrorCode.PurchaseCancelledError) {
                            PurchaseResult.Cancelled
                        } else {
                            PurchaseResult.Failed(error.message)
                        }
                        if (cont.isActive) cont.resume(result)
                    }
                }
            )
        }
    }

    override suspend fun restore(): PurchaseResult = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                val tier = tierOf(customerInfo)
                _state.update { it.copy(tier = tier) }
                if (cont.isActive) cont.resume(PurchaseResult.Success(tier))
            }

            override fun onError(error: PurchasesError) {
                if (cont.isActive) cont.resume(PurchaseResult.Failed(error.message))
            }
        })
    }

    private suspend fun fetchCustomerInfo(): CustomerInfo? = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) { if (cont.isActive) cont.resume(customerInfo) }
            override fun onError(error: PurchasesError) { if (cont.isActive) cont.resume(null) }
        })
    }

    private suspend fun fetchOfferings(): Offerings? = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) { if (cont.isActive) cont.resume(offerings) }
            override fun onError(error: PurchasesError) { if (cont.isActive) cont.resume(null) }
        })
    }

    private fun tierOf(info: CustomerInfo): SubscriptionTier? =
        SubscriptionTier.highestOf(info.entitlements.active.keys)

    private fun plansOf(offerings: Offerings): List<Plan> {
        val available = offerings.current?.availablePackages.orEmpty()
        packages = available.associateBy { it.identifier }
        return available.mapNotNull { pkg ->
            val (tier, period) = Plan.parse(pkg.identifier) ?: return@mapNotNull null
            Plan(tier, period, pkg.product.price.formatted, pkg.identifier)
        }.sortedWith(compareBy({ it.tier.frameworkLimit }, { it.period }))
    }

    companion object {
        /** Configures the SDK once and returns the manager. Call from Application.onCreate. */
        fun create(context: Context, apiKey: String): RevenueCatBillingManager {
            Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
            return RevenueCatBillingManager().also { it.listenForChanges() }
        }
    }
}
