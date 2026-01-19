// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
/**
 * Defines shape styles for UI components using RoundedCornerShape.
 * Centralizes small, medium, and large shape definitions for consistent appearance.
 */
package com.zebra.ai.barcodefinder.application.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),

    )
