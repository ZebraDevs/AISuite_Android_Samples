package com.zebra.ai.ppod

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_EULA_ACCEPTED
import com.zebra.ai.ppod.ui.screens.EulaScreen
import com.zebra.ai.ppod.ui.screens.MainScreen
import com.zebra.ai.ppod.ui.theme.AndroidPPODTheme
import com.zebra.ai.ppod.viewmodels.AppViewModel
import com.zebra.ai.ppod.viewmodels.UiEvent
import kotlinx.coroutines.flow.collectLatest

/**************************************************************************************************/
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    /**************************************************************************************************/
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startApplication()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**************************************************************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = getIntent()
        val action = intent.action
        if (action != null && action == MediaStore.ACTION_IMAGE_CAPTURE) {
            viewModel.contentUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
            if (viewModel.contentUri == null) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        }
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
    /**************************************************************************************************/
    override fun finish() {
        if (viewModel.contentUri != null) {
            if (!viewModel.dataSaved) {
                setResult(RESULT_CANCELED)
            } else {
                setResult(
                    RESULT_OK, Intent()
                        .setData(viewModel.contentUri)
                )
            }
        }
        super.finish()
    }
    /**************************************************************************************************/
    private fun startApplication() {
        enableEdgeToEdge()

        setContent {
            var eulaAccepted by remember { mutableStateOf(viewModel.preferences[PREF_KEY_EULA_ACCEPTED] as Boolean)}

            LaunchedEffect(key1 = true) {
                viewModel.uiEvents.collectLatest { event ->
                    when (event) {
                        is UiEvent.FinishApp -> {
                            finish()
                        }
                    }
                }
            }

            LaunchedEffect(key1 = true) {
                viewModel.preferences.addPreferenceListener(listOf(PREF_KEY_EULA_ACCEPTED)) {
                    eulaAccepted = viewModel.preferences[PREF_KEY_EULA_ACCEPTED] as Boolean
                }
            }

            AndroidPPODTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)) {

                    if (!eulaAccepted) {
                        EulaScreen(viewModel)
                    }else {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}