package com.zebra.ai.barcodefinder.application.presentation.model

/**
 * State to track whether Entity Tracker state
 * @property isInitialized Whether EntityTrackerAnalyzer successfully initialzed
 *
 */
data class EntityTrackerInitState(
    val isInitialized: Boolean = false
)
