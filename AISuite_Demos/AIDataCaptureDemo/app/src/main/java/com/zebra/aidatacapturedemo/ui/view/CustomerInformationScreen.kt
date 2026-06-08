package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.data.CustomerDataGenerator
import com.zebra.aidatacapturedemo.data.ProductInfo
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import java.util.Locale

@Composable
fun CustomerInformationScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") innerPadding: PaddingValues
) {
    // Generate data once
    val customers = remember { CustomerDataGenerator.generateCustomers() }
    
    // Store in ViewModel so we can access it during scanning
    LaunchedEffect(customers) {
        viewModel.setAllCustomers(customers)
    }

    // Process data to group by product
    val productGroups = remember(customers) {
        val groups = mutableMapOf<String, MutableList<Pair<String, Int>>>() // Barcode to List of (ToteId, Quantity)
        val productInfoMap = mutableMapOf<String, ProductInfo>()
        
        customers.forEach { customer ->
            customer.products.forEach { product ->
                groups.getOrPut(product.barcode) { mutableListOf() }.add(customer.id to product.quantity)
                productInfoMap[product.barcode] = product
            }
        }
        
        productInfoMap.values.sortedBy { it.name }.map { it to groups[it.barcode]!! }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(top = 40.dp) // Moved down to avoid being blocked
    ) {
        // Title with bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .drawBehind {
                    val borderSize = 1.dp.toPx()
                    val y = size.height - borderSize / 2
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = borderSize
                    )
                }
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Product Picking List",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(productGroups) { (product, totes) ->
                ProductPickingItem(product, totes)
            }
            
            item {
                Button(
                    onClick = {
                        viewModel.updatePickingFeedback(null)
                        navController.navigate(Screen.BarcodeScanPicking.route)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D39))
                ) {
                    Text("Proceed to Scanning", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProductPickingItem(product: ProductInfo, totes: List<Pair<String, Int>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = product.name,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            )
            Text(
                text = "Barcode: ${if (product.barcode.length > 5) product.barcode.takeLast(5) else product.barcode} | Price: $${String.format(Locale.US, "%.2f", product.price)}",
                style = TextStyle(fontSize = 14.sp, color = Color.Black)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tote Distribution:",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            )
            
            totes.forEach { (toteId, qty) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tote $toteId", style = TextStyle(fontSize = 14.sp, color = Color.Black))
                    Text(text = "Qty: $qty", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                }
            }
        }
    }
}
