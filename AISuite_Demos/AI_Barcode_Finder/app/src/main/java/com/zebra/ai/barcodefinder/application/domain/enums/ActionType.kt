// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.application.domain.enums

/**
 * Action types for barcode items.
 */
enum class ActionType {
    TYPE_NONE,
    TYPE_ACTION_COMPLETE,
    TYPE_RECALL,
    TYPE_QUANTITY_PICKUP,
    TYPE_CONFIRM_PICKUP,
    TYPE_NO_ACTION
}