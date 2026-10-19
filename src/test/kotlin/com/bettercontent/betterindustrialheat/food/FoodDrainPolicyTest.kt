package com.bettercontent.betterindustrialheat.food

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FoodDrainPolicyTest {
    @Test
    fun `thirst follows vanilla Hunger scale without losing fractional points`() {
        for (level in listOf(1, 4, 8, 10)) {
            assertEquals(20.0, FoodDrainPolicy.hydrationPerSecond(level - 1) * 800.0 / level, 1e-9)
        }
        for ((amplifier, expected) in listOf(3 to 6, 7 to 12)) {
            var pending = 0.0
            var withdrawn = 0
            repeat(60) {
                val (whole, remainder) = FoodDrainPolicy.hydrationWithdrawal(pending, amplifier)
                withdrawn += whole
                pending = remainder
            }
            assertEquals(expected, withdrawn)
            assertEquals(0.0, pending, 1e-9)
        }
    }

    @Test
    fun `malnourishment has its own linear percentage point scale and stops at zero`() {
        assertEquals(0.01, FoodDrainPolicy.nutrientFractionPerSecond(0), 1e-9)
        assertEquals(0.10, FoodDrainPolicy.nutrientFractionPerSecond(9), 1e-9)
        var nutrient = 1f
        repeat(60) { nutrient = FoodDrainPolicy.remainingNutrient(nutrient, 0) }
        assertEquals(0.4f, nutrient, 1e-5f)
        nutrient = 1f
        repeat(10) { nutrient = FoodDrainPolicy.remainingNutrient(nutrient, 9) }
        assertEquals(0f, nutrient, 1e-5f)
        repeat(15) { nutrient = FoodDrainPolicy.remainingNutrient(nutrient, 9) }
        assertEquals(0f, nutrient)
        assertEquals(0f, FoodDrainPolicy.remainingNutrient(0.05f, 9))
        assertEquals(0.4f, FoodDrainPolicy.remainingNutrient(0.5f, 9), 1e-5f)
    }
}
