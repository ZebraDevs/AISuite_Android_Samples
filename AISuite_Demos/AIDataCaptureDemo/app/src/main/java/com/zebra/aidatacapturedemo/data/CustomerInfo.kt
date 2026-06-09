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
        ProductInfo("Purell Hand Sanitizer", 5.99, "073852401097"),
        ProductInfo("Maltese", 2.49, "6936749026602"),
        ProductInfo("Clip", 3.95, "6936590040130"),
        ProductInfo("Premium Skincare Facial Tissue", 7.49, "627987553635"),
        ProductInfo("Blue Pen", 1.49, "4901681143122"),
        ProductInfo("Red Pen", 1.49, "4901681143139"),
        ProductInfo("Green Highlighter", 1.79, "04588878145"),
        ProductInfo("Yellow Highlighter", 1.79, "045888783508"),
        ProductInfo("Staedtler Mars Plastic Erazer", 2.25, "031901907983"),
        ProductInfo("Uni-Ball Black Pen", 1.99, "4902778497814"),
        ProductInfo("Ain Stein 0.5 HB", 1.55, "4902506269249"),
        ProductInfo("Black board", 1.79, "024680135791"),
        ProductInfo("Pencil", 1.25, "4007817182260"),
        ProductInfo("Equate Instant Hand Sanitizer Gel", 1.25, "628915089622"),
        ProductInfo("Math Sudoku", 1.25, "4952583053415"),
        )

    fun generateCustomers(toteIds: List<String> = listOf("A", "B", "C", "D", "E", "F")): List<CustomerInfo> {
        return toteIds.map { id ->
            val numProducts = Random.nextInt(1, 4)
            val selectedProducts = availableProducts.shuffled().take(numProducts).map { 
                it.copy(quantity = Random.nextInt(1, 6))
            }
            CustomerInfo(id, selectedProducts)
        }
    }
}
