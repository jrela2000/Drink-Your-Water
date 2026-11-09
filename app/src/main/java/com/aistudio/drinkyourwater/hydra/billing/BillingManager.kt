package com.aistudio.drinkyourwater.hydra.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    val state: StateFlow<BillingState>

    /** Reloads the subscription status and the store's prices. */
    suspend fun refresh()

    suspend fun purchase(activity: Activity, plan: Plan): PurchaseResult

    suspend fun restore(): PurchaseResult
}

/** Used when no RevenueCat key is configured: nothing can be bought and nothing is unlocked. */
object DisabledBillingManager : BillingManager {
    override val state: StateFlow<BillingState> = MutableStateFlow(BillingState(isAvailable = false))
    override suspend fun refresh() = Unit
    override suspend fun purchase(activity: Activity, plan: Plan) = PurchaseResult.Failed("Billing is not available")
    override suspend fun restore() = PurchaseResult.Failed("Billing is not available")
}
