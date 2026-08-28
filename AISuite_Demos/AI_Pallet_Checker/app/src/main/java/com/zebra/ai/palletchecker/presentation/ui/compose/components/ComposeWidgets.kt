package com.zebra.ai.palletchecker.presentation.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.ai.palletchecker.presentation.model.BOX_VALIDATION
import com.zebra.ai.palletchecker.presentation.ui.theme.AppTextStyles.TextFieldReqText
import com.zebra.ai.palletchecker.presentation.ui.theme.Black
import com.zebra.ai.palletchecker.presentation.ui.theme.NotDetectedMainLblColor
import com.zebra.ai.palletchecker.presentation.ui.theme.ThemeDark
import com.zebra.ai.palletchecker.presentation.ui.theme.WhiteLight
import com.zebra.ai.palletchecker.presentation.ui.theme.partialReadColor
import com.zebra.ai.palletchecker.presentation.ui.theme.qtyMismatchedColor
import com.zebra.ai.palletchecker.presentation.ui.theme.validBoxColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    placeHolder: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = RectangleShape,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOption: IME_INPUT = IME_INPUT.ALL
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = getKeyboard(keyboardOption)
    ) { innerTextField ->

        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            visualTransformation = visualTransformation,
            innerTextField = innerTextField,
            singleLine = singleLine,
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(10.dp), // this is how you can remove the padding
            trailingIcon = trailingIcon,
            placeholder = placeHolder,
            leadingIcon = leadingIcon,
            colors = colors,
            label = label,
            isError = isError,
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = true,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                )
            },
            supportingText = {
                if (isError && !supportingText.isNullOrEmpty())
                    Text(supportingText, color = colors.errorTextColor)
            }
        )
    }
}

private fun getKeyboard(input: IME_INPUT) =
    KeyboardOptions(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = when (input) {
            IME_INPUT.EMAIL ->
                KeyboardType.Email

            IME_INPUT.TEXT ->
                KeyboardType.Text

            IME_INPUT.DECIMAL ->
                KeyboardType.Decimal

            IME_INPUT.NUMBERS ->
                KeyboardType.Number

            else ->
                KeyboardType.Unspecified
        },
        imeAction = ImeAction.Unspecified,
        platformImeOptions = null,
        showKeyboardOnFocus = null,
        hintLocales = null
    )


enum class IME_INPUT {
    ALL,
    TEXT,
    EMAIL,
    DECIMAL,
    NUMBERS,
}

@Composable
fun ContentWithLabel(
    titleLabel: String,
    validation: BOX_VALIDATION,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = when (validation) {
                        BOX_VALIDATION.VERIFIED ->
                            validBoxColor

                        BOX_VALIDATION.PARTIAL_DETECTION ->
                            partialReadColor

                        BOX_VALIDATION.MISMATCH_QTY ->
                            qtyMismatchedColor

                        BOX_VALIDATION.NOT_DETECTED ->
                            NotDetectedMainLblColor
                    },
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = 8.dp,
                        bottomEnd = 0.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = titleLabel,
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }

}

@Composable
fun PalletROI(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.5f),
    bracketColor: Color = Color.White,
    bracketStrokeWidth: Dp = 3.dp,
    bracketLength: Dp = 20.dp,
    topReservedSpace: Dp = 0.dp
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp * 0.90
    val screenHeight = configuration.screenHeightDp * 0.90
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val topReservedPx = topReservedSpace.toPx()
            val usableHeight = (canvasHeight - topReservedPx).coerceAtLeast(0f)

            val centerX = canvasWidth / 2
            val centerY = topReservedPx + (usableHeight / 2)

            val rectWidth = screenWidth.dp.toPx()
            val rectHeight = kotlin.math.min(screenHeight.dp.toPx(), usableHeight * 0.90f)

            val left = centerX - rectWidth / 2
            val top = centerY - rectHeight / 2
            val right = centerX + rectWidth / 2
            val bottom = centerY + rectHeight / 2

            drawRect(
                color = overlayColor,
                size = size
            )

            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                blendMode = BlendMode.Clear
            )

            val strokeWidth = bracketStrokeWidth.toPx()
            val bracketLen = bracketLength.toPx()

            drawLine(
                color = bracketColor,
                start = Offset(left, top),
                end = Offset(left + bracketLen, top),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = bracketColor,
                start = Offset(left, top),
                end = Offset(left, top + bracketLen),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = bracketColor,
                start = Offset(right, top),
                end = Offset(right - bracketLen, top),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = bracketColor,
                start = Offset(right, top),
                end = Offset(right, top + bracketLen),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = bracketColor,
                start = Offset(left, bottom),
                end = Offset(left + bracketLen, bottom),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = bracketColor,
                start = Offset(left, bottom),
                end = Offset(left, bottom - bracketLen),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = bracketColor,
                start = Offset(right, bottom),
                end = Offset(right - bracketLen, bottom),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = bracketColor,
                start = Offset(right, bottom),
                end = Offset(right, bottom - bracketLen),
                strokeWidth = strokeWidth
            )
        }
        Text(text = "Please make sure pallet is visible within the box ", color = WhiteLight)
    }
}







