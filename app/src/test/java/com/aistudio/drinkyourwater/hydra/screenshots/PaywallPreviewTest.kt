package com.aistudio.drinkyourwater.hydra.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.drinkyourwater.hydra.billing.BillingPeriod
import com.aistudio.drinkyourwater.hydra.billing.BillingState
import com.aistudio.drinkyourwater.hydra.billing.Plan
import com.aistudio.drinkyourwater.hydra.billing.SubscriptionTier
import com.aistudio.drinkyourwater.hydra.ui.screens.PaywallScreen
import com.aistudio.drinkyourwater.hydra.ui.theme.DrinkYourWaterTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the paywall for review. Writes nothing unless -Proborazzi.test.record=true. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h720dp-port-xxhdpi")
class PaywallPreviewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val plans = SubscriptionTier.entries.flatMap { tier ->
        listOf(
            Plan(tier, BillingPeriod.MONTHLY, tier.monthlyFallback, "${tier.id}_monthly"),
            Plan(tier, BillingPeriod.YEARLY, tier.yearlyFallback, "${tier.id}_yearly")
        )
    }

    @Test fun paywallLight() = render("paywall-light", dark = false, BillingState(isAvailable = true, plans = plans))

    @Test fun paywallDarkSubscribed() =
        render("paywall-dark-subscribed", dark = true, BillingState(isAvailable = true, plans = plans, tier = SubscriptionTier.STARTER))

    private fun render(name: String, dark: Boolean, state: BillingState) {
        composeRule.setContent {
            DrinkYourWaterTheme(darkTheme = dark) {
                PaywallScreen(
                    billing = state, busy = false, message = null,
                    onSubscribe = {}, onRestore = {}, onManageSubscription = {}, onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/ui-previews/$name.png")
    }
}
