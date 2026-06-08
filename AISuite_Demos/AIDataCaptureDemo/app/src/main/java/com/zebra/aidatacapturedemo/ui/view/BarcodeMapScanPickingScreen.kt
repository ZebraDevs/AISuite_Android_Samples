package com.zebra.aidatacapturedemo.ui.view

import android.content.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun BarcodeMapScanPickingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") innerPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var manualInput by remember { mutableStateOf("") }

    // Register BroadcastReceiver for DataWedge
    DisposableEffect(Unit) {
        val filter = IntentFilter("com.zebra.aidatacapturedemo.SCAN")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val scanData = intent?.getStringExtra("com.symbol.datawedge.data_string")
                if (scanData != null) {
                    viewModel.processHardwareScan(scanData)
                }
            }
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Auto-focus the manual input field to capture keyboard wedge scans
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ready to Scan",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black),
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        // Invisible or small TextField to capture keyboard wedge input
        OutlinedTextField(
            value = manualInput,
            onValueChange = { 
                manualInput = it
                if (it.endsWith("\n")) { // Simple trigger for wedge enter key
                    viewModel.processHardwareScan(it.trim())
                    manualInput = ""
                }
            },
            label = { Text("Scan or Enter Barcode") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color(0xFF006D39)
            )
        )
        
        Button(
            onClick = { 
                viewModel.processHardwareScan(manualInput.trim())
                manualInput = ""
            },
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2BAB2B))
        ) {
            Text("Enter")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feedback Display
        val feedback = uiState.pickingFeedback
        if (feedback != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (feedback.contains("Incorrect")) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = feedback,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (feedback.contains("Incorrect")) Color.Red else Color(0xFF2E7D32)
                        )
                    )
                    
                    val product = uiState.lastScannedProduct
                    if (product != null && !feedback.contains("Incorrect")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Item: ${product.name}", color = Color.Black, fontWeight = FontWeight.Bold)
                        
                        uiState.targetTotes.forEach { pair ->
                            val toteId = pair.first
                            val qty = pair.second
                            val label = uiState.barcodeLabels[toteId]
                            val displayText = if (label != null) "Tote $label ($toteId)" else "Tote $toteId"
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = displayText, color = Color.Black, fontSize = 18.sp)
                                Text(text = "Qty: $qty", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                // Set the target tote for the map highlight
                                viewModel.updateSelectedToteId(uiState.targetTotes.firstOrNull()?.first)
                                navController.navigate(Screen.BarcodeMapPicking.route) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D39))
                        ) {
                            Text("Show on Map")
                        }
                    }
                }
            }
        }
    }
}
