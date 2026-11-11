// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.palletchecker.helpers

import android.os.Build

/**
 * Device-capability checks used to gate hardware-dependent features.
 */
object DeviceCapabilities {

    // Qualcomm Q-6690 family SoC identifier substring. Zebra 6690 devices
    // (e.g. TC701/TC501/TC201/ET401/KC401) report Build.SOC_MODEL values such as
    // "QCM6690" (with modem) or "QCS6690" (without modem); both contain "6690".
    private const val BURST_CAPABLE_SOC_TOKEN = "6690"

    /**
     * Returns true if the current device's chipset supports the AI inferencer's
     * BURST ("Turbo") performance mode.
     *
     * BURST is supported on select chipsets only (the Zebra 6690 family). The SDK's own
     * capability flag is private, so support is inferred from the primary SoC model name
     * reported by [Build.SOC_MODEL] (available since API 31; app minSdk is 33).
     */
    val isBurstModeSupported: Boolean
        get() = Build.SOC_MODEL.contains(BURST_CAPABLE_SOC_TOKEN, ignoreCase = true)
}
