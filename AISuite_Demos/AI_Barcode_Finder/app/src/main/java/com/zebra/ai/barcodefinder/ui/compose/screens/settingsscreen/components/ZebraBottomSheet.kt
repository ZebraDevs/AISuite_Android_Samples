// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.AppFonts
import com.zebra.ai.barcodefinder.ui.theme.BottomSheetDividerColor
import com.zebra.ai.barcodefinder.ui.theme.CardContainerColor
import com.zebra.ai.barcodefinder.ui.theme.CardDividerColor
import com.zebra.ai.barcodefinder.ui.theme.CardHeaderBackgroundColor
import com.zebra.ai.barcodefinder.ui.theme.CardHeaderTextPrimaryColor
import com.zebra.ai.barcodefinder.ui.theme.IconTintColor
import com.zebra.ai.barcodefinder.ui.theme.ScrimColor
import com.zebra.ai.barcodefinder.ui.theme.dividerColor
import kotlinx.coroutines.delay

@Composable
fun ZebraBottomSheet(
    title: String,
    onBackClick: () -> Unit = {},
    content: @Composable (ColumnScope.() -> Unit) = {},
    descriptionText: @Composable (ColumnScope.() -> Unit) = {},
    subTitleText: String,
    descriptionBulletItems: List<Pair<String, List<Pair<String, List<String>>>>>,
    recommendationBulltetItems: List<Pair<String, List<Pair<String, List<String>>>>>?,
    tipText: @Composable (ColumnScope.() -> Unit) = {}
) {
    var isOptionsExpanded by remember { mutableStateOf(false) }
    var isRecAndTipOptionsExpanded by remember { mutableStateOf(false) }
    // Start hidden so appear animation plays
    var isVisible by remember { mutableStateOf(false) }

    // Trigger enter animation after first composition
    LaunchedEffect(Unit) { isVisible = true }

    // Exit callback delay
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(AppDimensions.animationDurationMillis.toLong())
            onBackClick()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .clickable { isVisible = false }
        )

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(AppDimensions.animationDurationMillis)
            ) + fadeIn(animationSpec = tween(AppDimensions.animationDurationMillis)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(AppDimensions.animationDurationMillis)
            ) + fadeOut(animationSpec = tween(AppDimensions.animationDurationMillis))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(AppDimensions.BottomSheetHeightFraction)
                    .background(
                        Color.White, shape = RoundedCornerShape(
                            topStart = AppDimensions.BottomSheetShapeCorner,
                            topEnd = AppDimensions.BottomSheetShapeCorner
                        )
                    )
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.dimension_24dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = AppDimensions.BottomSheetTopPadding)
                            .width(AppDimensions.BottomSheetHandleWidth)
                            .height(AppDimensions.BottomSheetHandleHeight)
                            .background(
                                BottomSheetDividerColor,
                                RoundedCornerShape(AppDimensions.BottomSheetHandleShapeCorner)
                            )
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.BottomSheetSpacerHeight))
                    ZebraText(
                        textValue = title,
                        fontSize = AppDimensions.fontSize_20sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = AppDimensions.HorizontalDividerThickness,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimensions.HorizontalDividerVerticalPadding)
                    )
                }

                // Scrollable body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimensions.dimension_24dp)
                ) {
                    descriptionText()

                    Spacer(modifier = Modifier.height(AppDimensions.BottomSheetSpacerHeight))

                    // Details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardContainerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.HorizontalDividerThickness),
                        shape = RectangleShape
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardHeaderBackgroundColor)
                                    .clickable { isOptionsExpanded = !isOptionsExpanded }
                                    .padding(AppDimensions.CardPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = IconTintColor,
                                    modifier = Modifier.size(AppDimensions.IconSizeSmall)
                                )
                                Spacer(Modifier.width(AppDimensions.IconSpacerWidth))
                                ZebraText(
                                    textValue = subTitleText,
                                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textColor = CardHeaderTextPrimaryColor,
                                )
                                Spacer(Modifier.weight(AppDimensions.WeightFull))
                                Icon(
                                    imageVector = if (isOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isOptionsExpanded) "Collapse" else "Expand",
                                    tint = IconTintColor,
                                    modifier = Modifier.size(AppDimensions.IconSizeSmall)
                                )
                            }
                            if (isOptionsExpanded) {
                                Column(Modifier.padding(AppDimensions.CardPadding)) {
                                    descriptionBulletItems.forEach { (t, list) ->
                                        DetailOptionsBullets(title = t, bulletItems = list)
                                        Spacer(Modifier.height(AppDimensions.dimension_16dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.BottomSheetSpacerHeight))

                    // Recommendation card
                    if (recommendationBulltetItems != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardContainerColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.HorizontalDividerThickness),
                            shape = RectangleShape
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardHeaderBackgroundColor)
                                        .clickable {
                                            isRecAndTipOptionsExpanded = !isRecAndTipOptionsExpanded
                                        }
                                        .padding(AppDimensions.CardPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ZebraText(
                                        textValue = stringResource(id = R.string.setting_recommendation_tips),
                                        fontSize = AppDimensions.dialogTextFontSizeMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textColor = CardHeaderTextPrimaryColor,
                                    )
                                    Spacer(Modifier.weight(AppDimensions.WeightFull))
                                    Icon(
                                        imageVector = if (isRecAndTipOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isRecAndTipOptionsExpanded) "Collapse" else "Expand",
                                        tint = IconTintColor,
                                        modifier = Modifier.size(AppDimensions.IconSizeSmall)
                                    )
                                }
                                if (isRecAndTipOptionsExpanded) {
                                    Column(Modifier.padding(AppDimensions.CardPadding)) {
                                        recommendationBulltetItems.forEach { (t, list) ->
                                            DetailOptionsBullets(title = t, bulletItems = list)
                                        }
                                        Spacer(Modifier.height(AppDimensions.BottomSheetSpacerHeight))
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .padding(vertical = AppDimensions.HorizontalDividerVerticalPadding)
                                                .width(AppDimensions.HorizontalDividerWidth),
                                            thickness = AppDimensions.HorizontalDividerThickness,
                                            color = CardDividerColor
                                        )
                                        tipText()
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.BottomSheetSpacerHeight))
                }

                // Additional slot content
                content()
            }
        }
    }

    BackHandler { isVisible = false }
}

@Composable
private fun DetailOptionsBullets(
    title: String,
    bulletItems: List<Pair<String, List<String>>>
) {
    Column {
        Text(
            text = title,
            fontSize = AppDimensions.dialogTextFontSizeMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            fontFamily = AppFonts.IBMPlexSans
        )
        Spacer(modifier = Modifier.height(AppDimensions.IconSpacerWidth))
        bulletItems.forEach { (bullet, boldWords) ->
            Row(
                modifier = Modifier.padding(
                    start = AppDimensions.BulletTextStartPadding,
                    bottom = AppDimensions.BulletTextBottomPadding
                )
            ) {
                Text(
                    text = "• ",
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    fontFamily = AppFonts.IBMPlexSans
                )
                Text(
                    text = buildAnnotatedString {
                        var currentIndex = 0
                        boldWords.forEach { word ->
                            val startIndex = bullet.indexOf(word, currentIndex)
                            if (startIndex != -1) {
                                append(bullet.substring(currentIndex, startIndex))
                                withStyle(style = SpanStyle(fontWeight = FontWeight(AppDimensions.fontWeight700))) {
                                    append(word)
                                }
                                currentIndex = startIndex + word.length
                            }
                        }
                        append(bullet.substring(currentIndex))
                    },
                    fontSize = AppDimensions.dialogTextFontSizeMedium,
                    color = Color.Black,
                    lineHeight = AppDimensions.linePaddingDefault,
                    fontFamily = AppFonts.IBMPlexSans
                )
            }
        }
    }
}