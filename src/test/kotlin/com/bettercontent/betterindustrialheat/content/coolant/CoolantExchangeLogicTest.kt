package com.bettercontent.betterindustrialheat.content.coolant

import net.minecraft.resources.ResourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoolantExchangeLogicTest {
    private val definition = LiquidCoolantDefinition(
        id = id("better_industrial_heat:test"),
        coldFluid = id("minecraft:water"),
        hotFluid = id("better_industrial_heat:hot_water"),
        heatPerBucket = 400.0f,
        coolingPerBucket = 320.0f,
    )

    @Test
    fun heatsSupportedColdFluidWhenEnoughHeatIsStored() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("minecraft:water"),
            amount = 1000,
            currentHeat = 500.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        requireNotNull(result)
        assertEquals(id("better_industrial_heat:hot_water"), result.targetFluid)
        assertEquals(100.0f, result.resultingHeat)
    }

    @Test
    fun doesNotHeatWithoutEnoughStoredHeat() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("minecraft:water"),
            amount = 1000,
            currentHeat = 399.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        assertNull(result)
    }

    @Test
    fun coolsSupportedHotFluidWhenEnoughCapacityIsAvailable() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("better_industrial_heat:hot_water"),
            amount = 1000,
            currentHeat = 100.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        requireNotNull(result)
        assertEquals(id("minecraft:water"), result.targetFluid)
        assertEquals(420.0f, result.resultingHeat)
    }

    @Test
    fun doesNotCoolWithoutEnoughRemainingCapacity() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("better_industrial_heat:hot_water"),
            amount = 1000,
            currentHeat = 481.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        assertNull(result)
    }

    @Test
    fun ignoresUnsupportedFluids() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("minecraft:lava"),
            amount = 1000,
            currentHeat = 999.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        assertNull(result)
    }

    @Test
    fun heatsWhenStoredHeatIsExactlyRequired() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("minecraft:water"),
            amount = 500,
            currentHeat = 200.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        requireNotNull(result)
        assertEquals(id("better_industrial_heat:hot_water"), result.targetFluid)
        assertEquals(0.0f, result.resultingHeat)
    }

    @Test
    fun coolsWhenCapacityExactlyFitsReleasedHeat() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("better_industrial_heat:hot_water"),
            amount = 1000,
            currentHeat = 480.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        requireNotNull(result)
        assertEquals(id("minecraft:water"), result.targetFluid)
        assertEquals(800.0f, result.resultingHeat)
    }

    @Test
    fun scalesHeatExchangeForPartialBuckets() {
        val result = CoolantExchangeLogic.computeExchange(
            fluidId = id("minecraft:water"),
            amount = 250,
            currentHeat = 120.0f,
            maxHeat = 800.0f,
            definition = definition,
        )

        requireNotNull(result)
        assertEquals(id("better_industrial_heat:hot_water"), result.targetFluid)
        assertEquals(20.0f, result.resultingHeat)
    }

    private fun id(value: String): ResourceLocation = ResourceLocation.parse(value)
}
