package com.aistudio.drinkyourwater.hydra.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.drinkyourwater.hydra.billing.BillingPeriod
import com.aistudio.drinkyourwater.hydra.billing.BillingState
import com.aistudio.drinkyourwater.hydra.billing.Plan
import com.aistudio.drinkyourwater.hydra.billing.SubscriptionTier
import com.aistudio.drinkyourwater.hydra.ui.components.RippleButton
import com.aistudio.drinkyourwater.hydra.ui.theme.FreshBlue
import com.aistudio.drinkyourwater.hydra.ui.theme.IceTeal

/**
 * Subscription plans for custom habit reminders. Water reminders are always free; this screen
 * only unlocks more reminder types. Prices come from the store through [BillingState.plans] and
 * fall back to the tier's list price while they load.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    billing: BillingState,
    busy: Boolean,
    message: String?,
    onSubscribe: (Plan) -> Unit,
    onRestore: () -> Unit,
    onManageSubscription: () -> Unit,
    onBack: () -> Unit,
    initialTier: SubscriptionTier = SubscriptionTier.BUILDER,
    initialPeriod: BillingPeriod = BillingPeriod.YEARLY
) {
    var period by remember { mutableStateOf(initialPeriod) }
    var selected by remember { mutableStateOf(initialTier) }

    fun planFor(tier: SubscriptionTier): Plan? =
        billing.plans.firstOrNull { it.tier == tier && it.period == period }

    fun priceFor(tier: SubscriptionTier): String =
        planFor(tier)?.price ?: if (period == BillingPeriod.YEARLY) tier.yearlyFallback else tier.monthlyFallback

    val selectedPlan = planFor(selected)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More Reminder Types", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Build lock-screen check-ins for medications, movement, study, prayer and more. " +
                        "Water reminders stay free, always.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FreshBlue.copy(alpha = 0.12f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PeriodChip("Monthly", period == BillingPeriod.MONTHLY, Modifier.weight(1f).testTag("period_monthly")) {
                        period = BillingPeriod.MONTHLY
                    }
                    PeriodChip("Yearly", period == BillingPeriod.YEARLY, Modifier.weight(1f).testTag("period_yearly")) {
                        period = BillingPeriod.YEARLY
                    }
                }
            }

            SubscriptionTier.entries.forEach { tier ->
                item(key = tier.id) {
                    TierCard(
                        tier = tier,
                        price = priceFor(tier),
                        perLabel = if (period == BillingPeriod.YEARLY) "/year" else "/month",
                        isSelected = tier == selected,
                        isCurrent = billing.tier == tier,
                        popular = tier == SubscriptionTier.BUILDER,
                        onClick = { selected = tier }
                    )
                }
            }

            if (message != null) {
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("paywall_message")
                    )
                }
            }

            item {
                RippleButton(
                    onClick = { selectedPlan?.let(onSubscribe) },
                    enabled = !busy && selectedPlan != null && billing.tier != selected,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "subscribe_btn"
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = when {
                                billing.tier == selected -> "Your current plan"
                                billing.isLoading && selectedPlan == null -> "Loading plans…"
                                else -> "Subscribe to ${selected.displayName}"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onRestore, enabled = !busy) { Text("Restore purchases") }
                    if (billing.tier != null) {
                        TextButton(onClick = onManageSubscription) { Text("Manage subscription") }
                    }
                }
            }

            item {
                Text(
                    text = "Subscriptions renew automatically until you cancel in Google Play > Payments & subscriptions. " +
                        "Payment is charged to your Google Play account at confirmation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FreshBlue else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TierCard(
    tier: SubscriptionTier,
    price: String,
    perLabel: String,
    isSelected: Boolean,
    isCurrent: Boolean,
    popular: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("tier_${tier.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FreshBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, FreshBlue) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (popular) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Popular",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IceTeal
                        )
                    }
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Current plan", tint = IceTeal)
                    }
                }
                Text(
                    text = "${tier.frameworkLimit} custom habits · ${tier.tagline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = perLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
