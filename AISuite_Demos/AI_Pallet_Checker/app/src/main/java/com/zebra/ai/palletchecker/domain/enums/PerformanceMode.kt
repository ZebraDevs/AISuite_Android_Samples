// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.palletchecker.domain.enums

/**
 * Operational performance profile for the on-device AI inferencer (DSP).
 *
 * Maps to the AI Data Capture SDK's `InferencerOptions.PerformanceMode` when applied in
 * [com.zebra.ai.palletchecker.helpers.ModelsStorage].
 *
 * - [BALANCED]: (Default) Optimizes for power efficiency. Recommended for continuous use.
 * - [BURST]: Maximizes processing performance ("Turbo Mode") at the expense of increased
 *   power consumption. BURST is supported on select chipsets only (e.g. the Zebra 6690
 *   family); enabling it on an unsupported device throws an `AIVisionSDKException`. The UI
 *   therefore gates this option behind
 *   [com.zebra.ai.palletchecker.helpers.DeviceCapabilities.isBurstModeSupported].
 */
enum class PerformanceMode {
    BALANCED,
    BURST
}
