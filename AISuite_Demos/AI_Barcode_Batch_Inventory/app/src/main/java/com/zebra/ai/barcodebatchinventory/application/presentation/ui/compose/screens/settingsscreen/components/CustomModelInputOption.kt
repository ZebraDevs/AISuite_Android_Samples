// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.settingsscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.SelectedRadioButton
import com.zebra.ai.barcodebatchinventory.sdkcoordinator.model.AppSettings

private const val MAX_DIGITS = 5

/**
 * Radio option with two numeric fields for an exact model input width and height in pixels.
 * The fields are only shown while the option is selected. A value of 0 is shown as an empty
 * field, and fields that are not a valid size are marked in error.
 */
@Composable
fun CustomModelInputOption(
    selected: Boolean,
    width: Int,
    height: Int,
    onSelected: () -> Unit,
    onSizeChanged: (width: Int, height: Int) -> Unit
) {
    val widthInvalid = selected && !AppSettings.isValidCustomModelSize(width)
    val heightInvalid = selected && !AppSettings.isValidCustomModelSize(height)

    val focusManager = LocalFocusManager.current
    LaunchedEffect(selected) {
        if (!selected) focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(AppDimensions.dimension_12dp))
            ZebraText(
                textValue = stringResource(id = R.string.setting_screen_model_input_custom_title),
                fontSize = AppDimensions.radioButtonTitleFontSize,
                lineHeight = AppDimensions.radioButtonTitleLineHeight,
                modifier = Modifier.weight(AppDimensions.WeightFull)
            )
            RadioButton(
                selected = selected,
                onClick = onSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = SelectedRadioButton,
                    unselectedColor = Color.Gray
                )
            )
        }
        AnimatedVisibility(visible = selected) {
            Column {
            Row(
                modifier = Modifier.padding(
                    start = AppDimensions.dimension_12dp,
                    top = AppDimensions.dimension_10dp,
                    end = AppDimensions.dimension_12dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SizeField(
                    label = stringResource(id = R.string.setting_screen_model_input_custom_width_label),
                    value = width,
                    isError = widthInvalid,
                    onValueChange = { onSizeChanged(it, height) },
                    modifier = Modifier.weight(AppDimensions.WeightFull)
                )
                Text(
                    text = stringResource(id = R.string.setting_screen_model_input_custom_separator),
                    color = Color.Gray
                )
                SizeField(
                    label = stringResource(id = R.string.setting_screen_model_input_custom_height_label),
                    value = height,
                    isError = heightInvalid,
                    onValueChange = { onSizeChanged(width, it) },
                    modifier = Modifier.weight(AppDimensions.WeightFull)
                )
                Text(
                    text = stringResource(id = R.string.setting_screen_model_input_custom_unit),
                    color = Color.Gray
                )
            }
            if (widthInvalid || heightInvalid) {
                ZebraText(
                    textValue = stringResource(
                        id = R.string.setting_screen_model_input_custom_invalid_hint,
                        AppSettings.CUSTOM_MODEL_SIZE_MIN,
                        AppSettings.CUSTOM_MODEL_SIZE_MAX,
                        AppSettings.CUSTOM_MODEL_SIZE_STEP
                    ),
                    textColor = Color.Red,
                    fontSize = AppDimensions.dialogTextFontSizeExtraSmall,
                    lineHeight = AppDimensions.dialogTextFontSizeMedium,
                    modifier = Modifier.padding(
                        start = AppDimensions.dimension_12dp,
                        top = AppDimensions.dimension_2dp
                    )
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeField(
    label: String,
    value: Int,
    isError: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val text = if (value == 0) "" else value.toString()
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() }.take(MAX_DIGITS).toIntOrNull() ?: 0)
        },
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = text,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                label = { Text(label) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SelectedRadioButton,
                    focusedLabelColor = SelectedRadioButton,
                    cursorColor = SelectedRadioButton
                ),
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    top = 8.dp,
                    bottom = 8.dp
                )
            )
        }
    )
}
