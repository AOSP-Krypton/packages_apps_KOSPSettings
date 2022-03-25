package com.google.android.settings.overlay

import android.content.Context

import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.overlay.FeatureFactoryImpl
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl

class FeatureFactoryGoogleImpl : FeatureFactoryImpl() {

    private var powerUsageFeatureProviderGoogleImpl: PowerUsageFeatureProvider? = null

    override fun getPowerUsageFeatureProvider(context: Context): PowerUsageFeatureProvider {
        if (powerUsageFeatureProviderGoogleImpl == null) {
            powerUsageFeatureProviderGoogleImpl = PowerUsageFeatureProviderGoogleImpl(context.applicationContext)
        }
        return powerUsageFeatureProviderGoogleImpl!!
    }
}