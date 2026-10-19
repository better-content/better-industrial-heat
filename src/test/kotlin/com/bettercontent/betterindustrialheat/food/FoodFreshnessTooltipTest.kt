package com.bettercontent.betterindustrialheat.food

import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.contents.TranslatableContents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoodFreshnessTooltipTest {
    @Test
    fun `converted foods and custom effects have player facing names`() {
        val language = javaClass.getResourceAsStream("/assets/better_industrial_heat/lang/en_us.json")!!
            .bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        assertEquals("Rotten Meat", language.get("item.better_industrial_heat.spoiled_meat").asString)
        assertEquals("Rotten Produce", language.get("item.better_industrial_heat.spoiled_produce").asString)
        assertEquals("Thirst", language.get("effect.better_industrial_heat.thirst").asString)
        assertEquals("Malnourishment", language.get("effect.better_industrial_heat.malnourishment").asString)
    }

    @Test
    fun `freshness labels are capitalized and progress from green through yellow to red`() {
        TestMinecraftBootstrap.bootstrap()
        val language = javaClass.getResourceAsStream("/assets/better_industrial_heat/lang/en_us.json")!!
            .bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        val expected = listOf(
            Triple(FoodThermalService.Stage.FRESH, "Fresh", ChatFormatting.GREEN),
            Triple(FoodThermalService.Stage.STALE, "Stale", ChatFormatting.YELLOW),
            Triple(FoodThermalService.Stage.SPOILED, "Spoiled", ChatFormatting.GOLD),
            Triple(FoodThermalService.Stage.ROTTEN, "Rotten", ChatFormatting.RED),
            Triple(FoodThermalService.Stage.CONVERTED, "Rotten", ChatFormatting.RED)
        )
        for ((stage, label, color) in expected) {
            val tooltip = FoodThermalService.freshnessTooltip(stage)
            assertTrue(tooltip.contents is TranslatableContents)
            val key = (tooltip.contents as TranslatableContents).key
            assertEquals(label, language.get(key).asString)
            assertEquals(color.color, tooltip.style.color?.value)
        }
    }
}
