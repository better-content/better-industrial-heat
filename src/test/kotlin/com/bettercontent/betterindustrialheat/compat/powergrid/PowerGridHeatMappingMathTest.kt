package com.bettercontent.betterindustrialheat.compat.powergrid

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PowerGridHeatMappingMathTest {
    @Test
    fun `ambient power grid temperature maps to neutral better_industrial_heat heat`() {
        assertEquals(
            100f,
            PowerGridHeatMappingMath.temperatureToHeat(
                temperatureC = 22f,
                ambientTemperatureC = 22.0,
                ambientHeat = 100.0,
                heatPerDegreeC = 0.25466893039049235,
                minHeat = 0.0,
                maxHeat = 400.0
            )
        )
    }

    @Test
    fun `seething basin heater temperature maps near pipe maximum`() {
        val heat = PowerGridHeatMappingMath.temperatureToHeat(
            temperatureC = 1200f,
            ambientTemperatureC = 22.0,
            ambientHeat = 100.0,
            heatPerDegreeC = 0.25466893039049235,
            minHeat = 0.0,
            maxHeat = 400.0
        )

        assertTrue(abs(400f - heat) < 0.0001f, "expected 400, got $heat")
    }

    @Test
    fun `temperatures below the configured floor clamp to minimum heat`() {
        assertEquals(
            20f,
            PowerGridHeatMappingMath.temperatureToHeat(
                temperatureC = -273.15f,
                ambientTemperatureC = 22.0,
                ambientHeat = 100.0,
                heatPerDegreeC = 1.0,
                minHeat = 20.0,
                maxHeat = 400.0,
            ),
        )
    }

    @Test
    fun `mapping is invertible inside the configured range`() {
        val temperature = PowerGridHeatMappingMath.heatToTemperature(
            heat = 250f,
            ambientTemperatureC = 22.0,
            ambientHeat = 100.0,
            heatPerDegreeC = 0.25
        )

        assertEquals(622f, temperature)
    }
}
