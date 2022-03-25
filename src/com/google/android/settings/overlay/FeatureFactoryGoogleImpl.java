package com.google.android.settings.overlay;

import android.content.Context;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactoryImpl;
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl;

public final class FeatureFactoryGoogleImpl extends FeatureFactoryImpl {

    private PowerUsageFeatureProviderGoogleImpl mPowerUsageFeatureProviderGoogleImpl;

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProviderGoogleImpl == null) {
            mPowerUsageFeatureProviderGoogleImpl = new PowerUsageFeatureProviderGoogleImpl(
                context.getApplicationContext());
        }
        return mPowerUsageFeatureProviderGoogleImpl;
    }
}