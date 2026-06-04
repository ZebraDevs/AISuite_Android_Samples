// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.data

import kotlin.random.Random

data class ProductInfo(
    val name: String,
    val price: Double,
    val barcode: String,
    val quantity: Int = Random.nextInt(1, 10)
)

data class CustomerInfo(
    val id: String,
    val products: List<ProductInfo>
)

object CustomerDataGenerator {
    private val availableProducts = listOf(
        ProductInfo("Heidrun opbergbox met klapedeksel", 2.99, "2540068"),
        ProductInfo("Opbergbox+klemdeksel A4", 2.49, "2543429"),
        ProductInfo("Onderbedboxklemdeksel", 5.95, "2568528"),
        ProductInfo("Opbergbox met klemdeksel", 7.49, "3205800")
    )

    fun generateCustomers(): List<CustomerInfo> {
        return listOf("A", "B", "C", "D", "E", "F").map { id ->
            val numProducts = Random.nextInt(1, 4)
            val selectedProducts = availableProducts.shuffled().take(numProducts).map { 
                it.copy(quantity = Random.nextInt(1, 6))
            }
            CustomerInfo(id, selectedProducts)
        }
    }
}
