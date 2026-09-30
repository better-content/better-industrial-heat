package com.bettercontent.betterindustrialheat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeatMappingMathTest {
    @Test
    fun `cold sweat to pipe heat applies offset scale and min clamp`() {
        assertEquals(100.0, HeatMappingMath.coldSweatToPipeHeat(0.0, 100.0, 40.0))
        assertEquals(60.0, HeatMappingMath.coldSweatToPipeHeat(-1.0, 100.0, 40.0))
        assertEquals(140.0, HeatMappingMath.coldSweatToPipeHeat(1.0, 100.0, 40.0))
        assertEquals(0.0, HeatMappingMath.coldSweatToPipeHeat(-2.5, 100.0, 40.0))
        assertEquals(10.0, HeatMappingMath.coldSweatToPipeHeat(-2.5, 100.0, 40.0, minPipeHeat = 10.0))
    }

    @Test
    fun `pipe heat to cold sweat inverts using same offset and scale`() {
        assertEquals(0.0, HeatMappingMath.pipeHeatToColdSweat(100.0, 100.0, 40.0))
        assertEquals(-2.0, HeatMappingMath.pipeHeatToColdSweat(20.0, 100.0, 40.0))
        assertEquals(1.5, HeatMappingMath.pipeHeatToColdSweat(160.0, 100.0, 40.0))
    }

    @Test
    fun `mapping round trips across the configured operating range`() {
        listOf(0.0, 20.0, 60.0, 100.0, 180.0, 260.0, 340.0, 400.0).forEach { heat ->
            val coldSweat = HeatMappingMath.pipeHeatToColdSweat(heat, 100.0, 40.0)
            val roundTripped = HeatMappingMath.coldSweatToPipeHeat(coldSweat, 100.0, 40.0)
            assertTrue(kotlin.math.abs(roundTripped - heat) < 0.000001, "$heat mapped back as $roundTripped")
        }
    }
}
