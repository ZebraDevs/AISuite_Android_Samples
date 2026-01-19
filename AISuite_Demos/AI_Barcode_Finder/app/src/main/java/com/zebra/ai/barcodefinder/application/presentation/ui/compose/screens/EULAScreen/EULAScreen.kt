// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.EULAScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles.EulaText
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles.TitleTextDark
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.white

/**
 * Displays the End User License Agreement (EULA) screen.
 * Shows EULA content and provides a close button.
 *
 * @param onCloseClick Callback for closing the EULA screen
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EULAScreen(
    onCloseClick: () -> Unit = {}, onBackPressed: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
            .padding(AppDimensions.dimension_24dp)
            .semantics{contentDescription = "EULAScreen"},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        ZebraText(
            textValue = stringResource(id = R.string.eula_screen_header),
            style = TitleTextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = AppDimensions.dimension_12dp)
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .semantics{contentDescription = "ScrollableEULA"},
            verticalArrangement = Arrangement.Center
        ) {
            ZebraText(
                textValue = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append(stringResource(id = R.string.eula_screen_title))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n")
                        append(stringResource(id = R.string.eula_screen_intro))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_acceptance))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_term))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_license_ownership))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_restrictions))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_permissions))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_updates_support))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_data))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_modifications))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_third_party_resources))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_disclaimers_warranty))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_limitations_liability))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_governing_law))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_handling_disputes))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_open_source_software))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(400))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_government_end_user_restricted_rights))
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight(500))) {
                        append("\n\n")
                        append(stringResource(id = R.string.eula_screen_export_control))
                    }
                }.toString(),
                style = EulaText,
                modifier = Modifier.padding(top = AppDimensions.dimension_8dp)
            )
        }

        // Close button
        ZebraButton(
            text = stringResource(id = R.string.eula_screen_close_button),
            onClick = onCloseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.dimension_56dp)
                .padding(top = AppDimensions.dimension_16dp),
        )
    }

    BackHandler {
        onBackPressed()
    }
}

@Preview(showBackground = true, name = "EULA Screen")
@Composable
fun EULAScreenPreview() {
    MaterialTheme {
        EULAScreen()
    }
}

//@Preview(showBackground = true, name = "EULA Screen - Dark Theme")
@Composable
fun EULAScreenDarkPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        EULAScreen()
    }
}
