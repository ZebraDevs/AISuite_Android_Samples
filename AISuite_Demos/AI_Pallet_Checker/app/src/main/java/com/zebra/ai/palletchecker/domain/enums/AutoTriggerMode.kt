package com.zebra.ai.palletchecker.domain.enums

/**
 * Defines the criteria used for auto-triggering a capture/snap in SNAP mode.
 *
 * - [PALLET_BASE]: Auto-snap when the pallet base (class ID 3) is detected and stable.
 * - [FIXED_QUANTITY]: Auto-snap when a user-provided fixed number of boxes is detected.
 * - [PERCENTAGE_BASED]: Auto-snap when a user-provided percentage of overall expected boxes is detected.
 */
enum class AutoTriggerMode {
    PALLET_BASE,
    FIXED_QUANTITY,
    PERCENTAGE_BASED
}

