package com.bettercontent.betterindustrialheat.content.coolant

import com.google.gson.JsonParser
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fluids.FluidType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LiquidCoolantDefinitionTest {
    @Test
    fun parsesJsonDefinitions() {
        val json = JsonParser.parseString(
            """
            {
              "cold_fluid": "minecraft:water",
              "hot_fluid": "better_industrial_heat:hot_water",
              "heat_per_bucket": 400.0,
              "cooling_per_bucket": 320.0
            }
            """.trimIndent(),
        ).asJsonObject

        val definition = LiquidCoolantDefinition.fromJson(id("better_industrial_heat:test"), json)

        assertEquals(id("minecraft:water"), definition.coldFluid)
        assertEquals(id("better_industrial_heat:hot_water"), definition.hotFluid)
        assertEquals(400.0f, definition.heatPerBucket)
        assertEquals(320.0f, definition.coolingPerBucket)
    }

    @Test
    fun scalesBucketCostsLinearly() {
        val definition = LiquidCoolantDefinition(
            id = id("better_industrial_heat:test"),
            coldFluid = id("minecraft:water"),
            hotFluid = id("better_industrial_heat:hot_water"),
            heatPerBucket = 400.0f,
            coolingPerBucket = 320.0f,
        )

        assertEquals(400.0f, definition.heatRequired(FluidType.BUCKET_VOLUME))
        assertEquals(100.0f, definition.heatRequired(250))
        assertEquals(160.0f, definition.coolingRequired(500))
    }

    @Test
    fun rejectsInvalidDefinitions() {
        assertFailsWith<IllegalArgumentException> {
            LiquidCoolantDefinition(
                id = id("better_industrial_heat:bad"),
                coldFluid = id("minecraft:water"),
                hotFluid = id("minecraft:water"),
                heatPerBucket = 400.0f,
                coolingPerBucket = 320.0f,
            )
        }

        val badHeat = assertFailsWith<IllegalArgumentException> {
            LiquidCoolantDefinition(
                id = id("better_industrial_heat:bad_heat"),
                coldFluid = id("minecraft:water"),
                hotFluid = id("better_industrial_heat:hot_water"),
                heatPerBucket = 0.0f,
                coolingPerBucket = 320.0f,
            )
        }
        assertTrue(badHeat.message.orEmpty().contains("heat_per_bucket"))

        val badCooling = assertFailsWith<IllegalArgumentException> {
            LiquidCoolantDefinition(
                id = id("better_industrial_heat:bad_cooling"),
                coldFluid = id("minecraft:water"),
                hotFluid = id("better_industrial_heat:hot_water"),
                heatPerBucket = 400.0f,
                coolingPerBucket = 0.0f,
            )
        }
        assertTrue(badCooling.message.orEmpty().contains("cooling_per_bucket"))

        val energyPositiveLoop = assertFailsWith<IllegalArgumentException> {
            LiquidCoolantDefinition(
                id = id("better_industrial_heat:energy_positive"),
                coldFluid = id("minecraft:water"),
                hotFluid = id("better_industrial_heat:hot_water"),
                heatPerBucket = 400.0f,
                coolingPerBucket = 401.0f,
            )
        }
        assertTrue(energyPositiveLoop.message.orEmpty().contains("must be <= heat_per_bucket"))
    }

    @Test
    fun matchesHelpersUseExactFluidIds() {
        val definition = LiquidCoolantDefinition(
            id = id("better_industrial_heat:test"),
            coldFluid = id("minecraft:water"),
            hotFluid = id("better_industrial_heat:hot_water"),
            heatPerBucket = 400.0f,
            coolingPerBucket = 320.0f,
        )

        assertTrue(definition.matchesCold(id("minecraft:water")))
        assertTrue(definition.matchesHot(id("better_industrial_heat:hot_water")))
        assertTrue(!definition.matchesCold(id("better_industrial_heat:hot_water")))
        assertTrue(!definition.matchesHot(id("minecraft:water")))
    }

    private fun id(value: String): ResourceLocation = ResourceLocation.parse(value)
}
