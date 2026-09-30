package com.bettercontent.betterindustrialheat.food

import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DriedFoodNutritionContractTest {
    @Test
    fun driedFoodKeepsItsRawInputsFoodPropertiesWithoutCookingBonus() {
        TestMinecraftBootstrap.bootstrap()

        val expectedSources = mapOf(
            "dried_beef" to Items.BEEF,
            "dried_porkchop" to Items.PORKCHOP,
            "dried_chicken" to Items.CHICKEN,
            "dried_mutton" to Items.MUTTON,
            "dried_rabbit" to Items.RABBIT,
            "dried_cod" to Items.COD,
            "dried_salmon" to Items.SALMON,
        )
        assertEquals(expectedSources, FoodItems.DRIED_FOOD_SOURCES.associate { it.outputId to it.rawFood })

        FoodItems.DRIED_FOOD_SOURCES.forEach { source ->
            assertSame(
                source.rawFood.foodProperties,
                FoodItems.driedFoodProperties(source.rawFood),
                "${source.outputId} must not add nutrition, saturation, or cooked food properties",
            )
        }
    }
}
