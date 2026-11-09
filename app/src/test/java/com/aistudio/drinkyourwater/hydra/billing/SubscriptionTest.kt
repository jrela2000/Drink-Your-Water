package com.aistudio.drinkyourwater.hydra.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionTest {

    @Test fun `package ids map to a tier and period`() {
        assertEquals(SubscriptionTier.BUILDER to BillingPeriod.YEARLY, Plan.parse("builder_yearly"))
        assertEquals(SubscriptionTier.FULL_STACK to BillingPeriod.MONTHLY, Plan.parse("fullstack_monthly"))
        assertEquals(SubscriptionTier.STARTER to BillingPeriod.YEARLY, Plan.parse("Starter_Annual"))
    }

    @Test fun `unknown or incomplete package ids are ignored`() {
        assertNull(Plan.parse("\$rc_lifetime"))
        assertNull(Plan.parse("builder"))
        assertNull(Plan.parse("pro_monthly"))
    }

    @Test fun `highest active entitlement wins`() {
        assertEquals(SubscriptionTier.FULL_STACK, SubscriptionTier.highestOf(setOf("starter", "fullstack")))
        assertEquals(SubscriptionTier.BUILDER, SubscriptionTier.highestOf(setOf("builder", "starter")))
        assertNull(SubscriptionTier.highestOf(emptySet()))
        assertNull(SubscriptionTier.highestOf(setOf("something_else")))
    }

    @Test fun `no subscription means no custom habits`() {
        assertFalse(FrameworkAccess.canCreate(null, 0))
    }

    @Test fun `each tier allows custom habits up to its limit`() {
        assertTrue(FrameworkAccess.canCreate(SubscriptionTier.STARTER, 1))
        assertFalse(FrameworkAccess.canCreate(SubscriptionTier.STARTER, 2))
        assertTrue(FrameworkAccess.canCreate(SubscriptionTier.BUILDER, 3))
        assertFalse(FrameworkAccess.canCreate(SubscriptionTier.BUILDER, 4))
        assertTrue(FrameworkAccess.canCreate(SubscriptionTier.FULL_STACK, 5))
        assertFalse(FrameworkAccess.canCreate(SubscriptionTier.FULL_STACK, 6))
    }

    @Test fun `smallest tier that fits`() {
        assertEquals(SubscriptionTier.STARTER, FrameworkAccess.tierFor(1))
        assertEquals(SubscriptionTier.BUILDER, FrameworkAccess.tierFor(3))
        assertEquals(SubscriptionTier.FULL_STACK, FrameworkAccess.tierFor(6))
        assertNull(FrameworkAccess.tierFor(7))
    }
}
