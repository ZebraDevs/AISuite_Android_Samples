package com.zebra.aidatacapturedemo.ui.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun ExpandableSettingsItemsList.AddProductSettings() {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.import_database)))
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.export_database)))
    itemsTitle.add(ExpandableSettingsItem(stringResource((R.string.clear_active_database))))
}

@Composable
fun AddImportDatabaseOptions(viewModel: AIDataCaptureDemoViewModel) {
    val productLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                viewModel.loadProductIndex(uri)
            }
        }

    fun productLauncherFunc() {
        productLauncher.launch(arrayOf("*/*"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.mainInverse)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.importdb_description),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainDefault,
                )
            )
        }
    }

        ButtonOption(ButtonData(R.string.import_database, mainPrimary, 1.0F, true, onButtonClick = {
            productLauncherFunc()
        }))
}

@Composable
fun AddExportDatabaseOptions(viewModel: AIDataCaptureDemoViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.mainInverse)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.exportdb_description),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainDefault,
                )
            )
        }
    }

        ButtonOption(ButtonData(R.string.export_database, mainPrimary, 1.0F, true, onButtonClick = {
            FileUtils.saveProductDBFile()
            viewModel.toast("Saved Active Database to \"Download\" folder ")
        }))
}

@Composable
fun AddClearActiveDatabaseOptions(viewModel: AIDataCaptureDemoViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.mainInverse)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.cleardb_description),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainDefault,
                )
            )
        }
    }


        ButtonOption(
            ButtonData(
                R.string.clear_active_database,
                mainPrimary,
                1.0F,
                true,
                onButtonClick = {
                    viewModel.deleteProductIndex()
                    viewModel.toast("Cleared Active Database")
                })
        )
}