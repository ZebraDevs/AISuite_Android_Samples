// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.presentation.ui.compose.screens.aboutscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.BuildConfig
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.presentation.ui.theme.aboutBodyGray
import com.zebra.ai.barcodefinder.presentation.ui.theme.darkBackground
import com.zebra.ai.barcodefinder.presentation.ui.theme.settingsCardHeaderBackgroundColor
import com.zebra.ai.barcodefinder.presentation.ui.theme.white

/**
 * Displays the About screen with app and SDK version information.
 * Shows navigation options and handles menu/license/back actions.
 *
 * @param onMenuClick Callback for menu navigation
 * @param onLicenseClick Callback for license information
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onMenuClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // Inline version logic from SDKVersionHelper
    val sdkVersion = remember {
        try {
            BuildConfig.AI_VISION_SDK_VERSION
        } catch (e: Exception) {
            "?.?.?"
        }
    }
    val localizerModelVersion = remember {
        try {
            BuildConfig.BARCODE_LOCALIZER_MODEL_VERSION
        } catch (e: Exception) {
            "?.?.?"
        }
    }

    val spacerHeight =
        if (LocalConfiguration.current.screenHeightDp > 550) AppDimensions.dimension_12dp else AppDimensions.zeroPadding

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
    ) {
        // Header with back arrow and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkBackground)
                .padding(horizontal = AppDimensions.zeroPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = white
                )
            }
            //Spacer(modifier = Modifier.width(8.dp))
            ZebraText(
                textValue = BuildConfig.APP_NAME,
                style = AppTextStyles.TitleTextLight,
                textColor = AppColors.TextWhite
            )
        }

        // Increased vertical spacing between title bar and About section
        Spacer(modifier = Modifier.height(AppDimensions.spacerHeight12))

        // About section header with gray tint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(settingsCardHeaderBackgroundColor)
                .padding(AppDimensions.dimension_8dp)
        ) {
            ZebraText(
                textValue = stringResource(id = R.string.about_header),
                style = AppTextStyles.AboutBoldTextStyle
            )
        }

        // Description area with lighter gray background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(aboutBodyGray)
                .padding(
                    horizontal = AppDimensions.dimension_12dp,
                    vertical = AppDimensions.dimension_16dp
                )
        ) {
            ZebraText(
                textValue = stringResource(id = R.string.about_description),
                fontSize = AppDimensions.dialogTextFontSizeSmall,
                lineHeight = AppDimensions.fontSize_18sp,
                fontWeight = FontWeight(400)
            )
        }

        // Main content with white background for version info and license
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(white)
                .weight(1f),
            horizontalAlignment = Alignment.Start
        ) {

            // Version information section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = AppDimensions.dimension_8dp),
            ) {
                // AI Barcode Finder version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimensions.dimension_14_4dp,
                            vertical = AppDimensions.dimension_6dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZebraText(
                        textValue = BuildConfig.APP_NAME,
                        style = AppTextStyles.AboutBoldTextStyle
                    )
                    ZebraText(
                        textValue = BuildConfig.VERSION_NAME,
                        style = AppTextStyles.AboutSmallTextStyle
                    )
                }

                // AI Data Capture SDK version - now dynamic from libs.versions.toml
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimensions.dimension_14_4dp,
                            vertical = AppDimensions.dimension_6dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZebraText(
                        textValue = stringResource(id = R.string.about_sdk_name),
                        style = AppTextStyles.AboutBoldTextStyle
                    )

                    ZebraText(
                        textValue = sdkVersion,
                        style = AppTextStyles.AboutSmallTextStyle
                    )
                }

                // AI Data Capture SDK version - now dynamic from libs.versions.toml
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimensions.dimension_14_4dp,
                            vertical = AppDimensions.dimension_6dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZebraText(
                        textValue = stringResource(id = R.string.about_barcode_model_version_name),
                        style = AppTextStyles.AboutBoldTextStyle
                    )

                    ZebraText(
                        textValue = localizerModelVersion,
                        style = AppTextStyles.AboutSmallTextStyle
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.dimension_12dp))

            // End User License Agreement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLicenseClick() }
                    .padding(
                        vertical = AppDimensions.dimension_12dp,
                        horizontal = AppDimensions.dimension_16dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZebraText(
                    textValue = stringResource(id = R.string.about_license),
                    style = AppTextStyles.AboutBoldTextStyle
                )
                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = AppColors.TextBlack,
                    modifier = Modifier.size(AppDimensions.modifier24)
                )
            }

            Spacer(modifier = Modifier.height(spacerHeight))

            // Copyright notice at bottom
            Text(
                text = stringResource(id = R.string.about_copyright),
                fontSize = AppDimensions.dialogTextFontSizeMedium,
                fontWeight = FontWeight(AppDimensions.fontWeight400),
                color = AppColors.TextBlack,
                lineHeight = AppDimensions.linePaddingDefault,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(
                    top = AppDimensions.ExtraLargePadding,
                    start = AppDimensions.dimension_16dp,
                    end = AppDimensions.dimension_16dp
                )
            )
        }
    }

    BackHandler {
        onBackPressed()
    }
}

@Preview(showBackground = true, name = "About Screen")
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen()
    }
}