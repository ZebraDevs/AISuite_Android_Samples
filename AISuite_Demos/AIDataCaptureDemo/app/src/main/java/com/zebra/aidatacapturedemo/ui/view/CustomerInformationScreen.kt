package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.data.ProductInfo
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun CustomerInformationScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    viewModel.updateAppBarTitle("Product List")

    val customers = uiState.allCustomers

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

    val toPickGroups = productGroups.filter { !uiState.pickedProductBarcodes.contains(it.first.barcode) }
    val pickedGroups = productGroups.filter { uiState.pickedProductBarcodes.contains(it.first.barcode) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Products to Pick", "Products Already Picked")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(innerPadding)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF006D39),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF006D39)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color(0xFF006D39) else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val currentGroups = if (selectedTabIndex == 0) toPickGroups else pickedGroups
            
            items(currentGroups) { (product, totes) ->
                ProductPickingItem(product, totes)
            }
            
            if (selectedTabIndex == 0) {
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

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ProductPickingItem(product: ProductInfo, totes: List<Pair<String, Int>>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    )
                    Text(
                        text = "Barcode: ${if (product.barcode.length > 5) product.barcode.takeLast(5) else product.barcode}",
                        style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.Gray
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tote Distribution:",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                )
                
                totes.forEach { (toteId, qty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Tote $toteId", style = TextStyle(fontSize = 14.sp, color = Color.Black))
                        Text(text = "Qty: $qty", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black))
                    }
                }
            }
        }
    }
}
