package com.zebra.ai.barcodebatchinventory.application.presentation.model

/**
 * State to track Entity Tracker initialization progress
 * @property isInitialized Whether EntityTrackerAnalyzer is fully initialized and ready
 */
data class EntityTrackerInitState(
    val isInitialized: Boolean = false,
)
