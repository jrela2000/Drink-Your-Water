package com.aistudio.drinkyourwater.hydra.billing

/**
 * The paid tiers. [id] doubles as the RevenueCat entitlement identifier and as the tier part of
 * its package identifiers (e.g. "builder_yearly"), matching the iOS app's naming.
 */
enum class SubscriptionTier(
    val id: String,
    val displayName: String,
    /** Custom habit frameworks (beyond water) the tier can have active at once. */
    val frameworkLimit: Int,
    val tagline: String,
    val monthlyFallback: String,
    val yearlyFallback: String
) {
    STARTER("starter", "Starter", 2, "Your first custom habit, plus one more", "$2.99", "$23.99"),
    BUILDER("builder", "Builder", 4, "Room to grow your full accountability system", "$5.99", "$47.99"),
    FULL_STACK("fullstack", "Full Stack", 6, "Your complete life accountability system", "$8.99", "$71.99");

    companion object {
        fun fromId(id: String): SubscriptionTier? = entries.firstOrNull { it.id == id }

        /** The highest tier among the active entitlement ids, or null when none is active. */
        fun highestOf(activeEntitlementIds: Set<String>): SubscriptionTier? =
            entries.filter { it.id in activeEntitlementIds }.maxByOrNull { it.frameworkLimit }
    }
}

enum class BillingPeriod { MONTHLY, YEARLY }

/** One purchasable option: a tier at a billing period, priced by the store. */
data class Plan(
    val tier: SubscriptionTier,
    val period: BillingPeriod,
    /** Localized price from the store, e.g. "$47.99". Falls back to the tier's list price. */
    val price: String,
    /** RevenueCat package identifier used to start the purchase. */
    val packageId: String
) {
    companion object {
        /**
         * Maps a RevenueCat package identifier such as "builder_yearly" or "fullstack_monthly"
         * to its tier and period, or null when it isn't one of ours.
         */
        fun parse(packageId: String): Pair<SubscriptionTier, BillingPeriod>? {
            val id = packageId.lowercase()
            val tier = SubscriptionTier.entries.firstOrNull { it.id in id } ?: return null
            val period = when {
                "yearly" in id || "annual" in id -> BillingPeriod.YEARLY
                "monthly" in id -> BillingPeriod.MONTHLY
                else -> return null
            }
            return tier to period
        }
    }
}

/** What the rest of the app knows about the user's subscription. */
data class BillingState(
    /** False when no store key is configured: the app then keeps custom habits "coming soon". */
    val isAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val tier: SubscriptionTier? = null,
    val plans: List<Plan> = emptyList(),
    val message: String? = null
)

sealed interface PurchaseResult {
    data class Success(val tier: SubscriptionTier?) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

/** Rules for how many custom habit frameworks a subscription allows. */
object FrameworkAccess {
    fun canCreate(tier: SubscriptionTier?, activeCustomFrameworks: Int): Boolean =
        tier != null && activeCustomFrameworks < tier.frameworkLimit

    /** The smallest tier that would let the user have [wanted] custom frameworks. */
    fun tierFor(wanted: Int): SubscriptionTier? =
        SubscriptionTier.entries.firstOrNull { it.frameworkLimit >= wanted }
}
