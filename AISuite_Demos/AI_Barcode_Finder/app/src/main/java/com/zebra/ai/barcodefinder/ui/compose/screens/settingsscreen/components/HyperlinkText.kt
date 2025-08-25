// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.ui.compose.screens.settingsscreen.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions

@Composable
fun HyperlinkText(text: String, uri: String) {
    val context = LocalContext.current // Access the context within a composable

    ZebraText(
        textValue = text,
        modifier = Modifier.clickable {
            // Open the URL when the text is clicked
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            context.startActivity(intent)
        },
        style = TextStyle(
            textDecoration = TextDecoration.Underline,
            color = Color.Blue // Blue color for a hyperlink-like appearance
        ),
        lineHeight = AppDimensions.linePaddingDefault,
        fontSize = AppDimensions.dialogTextFontSizeMedium
    )
}