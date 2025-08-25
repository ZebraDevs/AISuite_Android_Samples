// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.common.enums

import com.zebra.ai.barcodefinder.R

/**
 * Enum representing the available processor types for running AI inference in the barcode finder app.
 * Each value provides a display name, description, and identifier resource ID for localization and configuration.
 * Used for configuring which hardware (DSP, GPU, CPU) is used for model inference and for displaying options in the UI.
 */
enum class ProcessorType(
    val displayNameResId: Int,   // Resource ID for localized display name
    val identifierResId: Int     // Resource ID for unique identifier string
) {
    /** DSP (Digital Signal Processor) option */
    DSP(
        R.string.processor_type_dsp,
        R.string.processor_type_dsp_id
    ),

    /** GPU (Graphics Processing Unit) option */
    GPU(
        R.string.processor_type_gpu,
        R.string.processor_type_gpu_id
    ),

    /** CPU (Central Processing Unit) option */
    CPU(
        R.string.processor_type_cpu,
        R.string.processor_type_cpu_id
    );
}