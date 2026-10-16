package com.zebra.aidatacapturedemo.ui.view.filters

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

enum class AppButtonStyle { Filled, Outlined }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    style: AppButtonStyle = AppButtonStyle.Outlined
) {
    val shape = RoundedCornerShape(4.dp)
    val content: @Composable RowScope.() -> Unit = {
        icon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (style == AppButtonStyle.Filled) Color.White else Variables.blackText
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight(500),
            fontSize = 14.sp,
            color = if (style == AppButtonStyle.Filled) Color.White else Variables.mainDefault
        )
    }

    if (style == AppButtonStyle.Filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            modifier = modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Variables.mainPrimary),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            modifier = modifier.height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Variables.mainDefault),
            content = content
        )
    }
}

@Composable
fun AppHeaderTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 22.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight.Bold,
            color = Variables.mainDefault
        )
    )
}

@Composable
fun AppBodyText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight.Normal,
            color = Variables.colorsTextBody
        )
    )
}

@Composable
fun AppRowDialogTitleText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight(700),
            color = Variables.mainDefault
        )
    )
}

@Composable
fun AppRowTitleText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight(400),
            color = Variables.colorsTextBody
        )
    )
}

@Composable
fun AppBorderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            color = Variables.mainDefault
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.DarkGray,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(start = 10.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(0.dp, Color.Transparent)
                    .then(Modifier),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                color = Color.Gray
                            ),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.CenterStart)) { innerTextField() }
                }
            }
        }
    )
}

@Composable
fun MenuItemText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight.Normal,
            color = Variables.mainDefault
        )
    )
}

@Composable
fun MenuItemIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Variables.mainDefault
    )
}

@Composable
fun FieldActionsOverflowMenu(
    onDeleteField: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onReset: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More options"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        offset = DpOffset(x = (0).dp, y = 10.dp),
        modifier = Modifier.background(color = Variables.surfaceDefault)
    ) {
        DropdownMenuItem(
            text = { MenuItemText("Delete field") },
            onClick = { expanded = false; onDeleteField() },
            leadingIcon = { MenuItemIcon(Icons.Outlined.Delete) }
        )
        DropdownMenuItem(
            text = { MenuItemText("Select All") },
            onClick = { expanded = false; onSelectAll() },
            leadingIcon = { MenuItemIcon(Icons.Filled.CheckCircle) }
        )
        DropdownMenuItem(
            text = { MenuItemText("Deselect All") },
            onClick = { expanded = false; onDeselectAll() },
            leadingIcon = { MenuItemIcon(Icons.Outlined.CheckCircle) }
        )
        DropdownMenuItem(
            text = { MenuItemText("Reset") },
            onClick = { expanded = false; onReset() },
            leadingIcon = { MenuItemIcon(Icons.Outlined.Refresh) }
        )
    }
}

@Composable
fun FieldDialog(
    initialFieldName: String = "", // Pass the initial name for editing, default is empty for adding
    isEditing: Boolean = false,   // Flag to determine if it's an edit operation
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit   // Callback for add/edit action
) {
    var fieldName by remember { mutableStateOf(initialFieldName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            AppRowDialogTitleText(if (isEditing) "Edit Field" else "Add New Field")
        },
        text = {
            Column {
                AppBodyText(if (isEditing) "Edit field name:" else "Enter field name:")
                Spacer(Modifier.height(6.dp))
                AppBorderTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    placeholder = if (isEditing) "Edit field name" else "e.g., Invoice Number"
                )
            }
        },
        confirmButton = {
            AppButton(
                text = if (isEditing) "Save" else "Add",
                onClick = {
                    if (fieldName.isNotBlank()) {
                        onConfirm(fieldName)
                        onDismiss()
                    }
                },
                style = AppButtonStyle.Filled
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                style = AppButtonStyle.Outlined
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onAddField: (String) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppRowDialogTitleText("Add New Field") },
        text = {
            Column {
                AppBodyText("Enter field name:")
                Spacer(Modifier.height(6.dp))
                AppBorderTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    placeholder = "e.g., Invoice Number"
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Add",
                onClick = {
                    if (fieldName.isNotBlank()) {
                        onAddField(fieldName)
                        onDismiss()
                    }
                },
                style = AppButtonStyle.Filled
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss,
                style = AppButtonStyle.Outlined
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun DeleteFieldDialog(
    onDismiss: () -> Unit,
    onDeleteField: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.warning_icon),
                    contentDescription = "Warning",
                    tint = Variables.mainSubtle,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                AppRowDialogTitleText("Confirm Delete")
            }
        },
        text = { AppBodyText("Are you sure you want to delete the selected fields?") },
        confirmButton = {
            AppButton(
                text = "Yes",
                onClick = { onDeleteField(); onDismiss() },
                style = AppButtonStyle.Filled
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = { onDismiss() },
                style = AppButtonStyle.Outlined,
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun ResetFieldsDialog(
    onDismiss: () -> Unit,
    onResetField: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row() {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.warning_icon),
                    contentDescription = "Warning",
                    tint = Variables.mainSubtle,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                AppRowDialogTitleText("Confirm Reset")
            }
        },
        text = { AppBodyText("Are you sure you want to reset  all values? This will remove all changes including any custom fields or hints.") },
        confirmButton = {
            AppButton(
                text = "Yes",
                onClick = { onResetField(); onDismiss() },
                style = AppButtonStyle.Filled
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = { onDismiss() },
                style = AppButtonStyle.Outlined,
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    )
}


@Composable
fun FilterRow(
    title: String,
    subTitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowItemClicked: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkmarkColor = Color.White,
                checkedColor = Variables.mainDefault,
                uncheckedColor = Variables.mainSubtle
            )
        )
        Spacer(modifier = Modifier.width(width = 8.dp))
        Row(modifier = Modifier.clickable { onRowItemClicked() }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = Variables.TypefaceLineHeight18,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(700),
                        color = Variables.mainDefault,
                    )
                )
                Spacer(modifier = Modifier.height(height = 4.dp))
                Text(
                    text = subTitle,
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsTextBody,
                    )
                )
            }
            Spacer(modifier = Modifier.width(width = 8.dp))
            Image(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = R.drawable.ic_right_exapand),
                contentDescription = "image description",
                contentScale = ContentScale.None
            )
        }
    }
    HorizontalDivider(color = Color(0xFFE8EAF0), thickness = 1.dp)
}

@Composable
fun FilterBottomBar(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            style = AppButtonStyle.Outlined
        )

        AppButton(
            text = "Save",
            onClick = onSave,
            modifier = Modifier.weight(1f),
            style = AppButtonStyle.Filled
        )
    }
}

@Composable
fun String?.ToToast(duration: Int = Toast.LENGTH_SHORT) {
    if (this == null) return
    val context = LocalContext.current
    Toast.makeText(context, this, duration).show()
}

@Composable
fun InitToast(uiState: AIDataCaptureDemoUiState, viewModel: AIDataCaptureDemoViewModel) {
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            viewModel.toast(message)
            viewModel.updateToastMessage(message = null)
        }
    }
}