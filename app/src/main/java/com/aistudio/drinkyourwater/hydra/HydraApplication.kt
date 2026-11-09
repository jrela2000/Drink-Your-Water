package com.aistudio.drinkyourwater.hydra

import android.app.Application
import com.aistudio.drinkyourwater.hydra.billing.BillingManager
import com.aistudio.drinkyourwater.hydra.billing.DisabledBillingManager
import com.aistudio.drinkyourwater.hydra.billing.RevenueCatBillingManager

class HydraApplication : Application() {

    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.REVENUECAT_API_KEY
        billing = if (key.isBlank()) DisabledBillingManager else RevenueCatBillingManager.create(this, key)
    }
}
