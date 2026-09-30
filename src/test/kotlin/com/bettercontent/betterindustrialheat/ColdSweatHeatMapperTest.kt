package com.bettercontent.betterindustrialheat

import kotlin.test.Test
import kotlin.test.assertEquals

class ColdSweatHeatMapperTest {
    @Test
    fun `offset mapping matches the bridge examples`() {
        assertEquals(100.0, ColdSweatHeatMapper.coldSweatToPipeHeat(0.0, 100.0, 40.0))
        assertEquals(60.0, ColdSweatHeatMapper.coldSweatToPipeHeat(-1.0, 100.0, 40.0))
        assertEquals(140.0, ColdSweatHeatMapper.coldSweatToPipeHeat(1.0, 100.0, 40.0))
        assertEquals(0.0, ColdSweatHeatMapper.coldSweatToPipeHeat(-2.5, 100.0, 40.0))
    }

    @Test
    fun `inverse mapping treats the freezing offset as neutral`() {
        assertEquals(0.0, ColdSweatHeatMapper.pipeHeatToColdSweat(100.0, 100.0, 40.0))
        assertEquals(-2.0, ColdSweatHeatMapper.pipeHeatToColdSweat(20.0, 100.0, 40.0))
        assertEquals(1.5, ColdSweatHeatMapper.pipeHeatToColdSweat(160.0, 100.0, 40.0))
    }
}
