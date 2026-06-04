package com.zebra.aidatacapturedemo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zebra.aidatacapturedemo.ui.view.Variables

private val DarkColorScheme = darkColorScheme(
    surface = Variables.surfaceDefault,
    onSurface = Variables.mainDefault,
    primary = Variables.mainPrimary,
    onPrimary = Variables.borderPrimaryMain
)

private val LightColorScheme = lightColorScheme(
    surface = Variables.surfaceDefault,
    onSurface = Variables.mainDefault,
    primary = Variables.mainPrimary,
    onPrimary = Variables.borderPrimaryMain
)

@Composable
fun AIDataCaptureDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}