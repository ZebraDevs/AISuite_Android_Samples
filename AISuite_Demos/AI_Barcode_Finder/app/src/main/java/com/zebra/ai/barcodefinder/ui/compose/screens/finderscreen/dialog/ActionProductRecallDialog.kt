// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.finderscreen.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ButtonType
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraIcon
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.AppShapes
import com.zebra.ai.barcodefinder.ui.theme.mainInverse

@Composable
fun ActionProductRecallDialog(
    productName: String = stringResource(R.string.product_name_here),
    productId: String = stringResource(R.string.id_placeholder),
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.MediumPadding),
            shape = AppShapes.large,
            colors = CardDefaults.cardColors(
                containerColor = mainInverse
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = AppDimensions.largeElevation
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.LargePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimensions.LargePadding)
            ) {
                item {
                    ZebraIcon(
                        icon = painterResource(id = R.drawable.recall_marker),
                        iconColor = Color.Transparent,
                        overlayText = null,
                        size = AppDimensions.iconSizeLarge,
                        shapeRadius = AppDimensions.iconSizeLarge / 2,
                    )
                }

                item {
                    ZebraText(
                        textValue = stringResource(R.string.product_recall_required),
                        fontSize = AppDimensions.dialogTextFontSizeLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Product information
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.dimension_12dp)
                    ) {
                        ZebraText(
                            textValue = productName,
                            fontSize = AppDimensions.dialogTextFontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        ZebraText(
                            textValue = "${stringResource(R.string.id)}: $productId",
                            fontSize = AppDimensions.dialogTextFontSizeSmall,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.modifier20)
                    ) {
                        ZebraButton(
                            buttonType = ButtonType.Outlined,
                            text = stringResource(R.string.cancel),
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                        )
                        ZebraButton(
                            buttonType = ButtonType.Raised,
                            text = stringResource(R.string.confirm),
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Action Product Recall Dialog")
@Composable
fun ActionProductRecallScreenPreview() {
    MaterialTheme {
        ActionProductRecallDialog()
    }
}
