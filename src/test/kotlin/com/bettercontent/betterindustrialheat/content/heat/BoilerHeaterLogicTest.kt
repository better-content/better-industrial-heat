package com.bettercontent.betterindustrialheat.content.heat

import kotlin.test.Test
import kotlin.test.assertEquals

class BoilerHeaterLogicTest {
    private val settings = BoilerHeaterSettings(180f, 260f, 340f, 1f, 2f, 3f)

    @Test
    fun `maps exact configured thresholds`() {
        assertEquals(0, BoilerHeaterLogic.strength(179.999f, settings))
        assertEquals(1, BoilerHeaterLogic.strength(180f, settings))
        assertEquals(1, BoilerHeaterLogic.strength(259.999f, settings))
        assertEquals(2, BoilerHeaterLogic.strength(260f, settings))
        assertEquals(2, BoilerHeaterLogic.strength(339.999f, settings))
        assertEquals(3, BoilerHeaterLogic.strength(340f, settings))
        assertEquals(3, BoilerHeaterLogic.strength(400f, settings))
    }

    @Test
    fun `consumes the cost proportional to delivered strength`() {
        assertEquals(BoilerHeatDelivery(1, 1f, 199f), BoilerHeaterLogic.deliver(200f, true, settings))
        assertEquals(BoilerHeatDelivery(2, 2f, 298f), BoilerHeaterLogic.deliver(300f, true, settings))
        assertEquals(BoilerHeatDelivery(3, 3f, 397f), BoilerHeaterLogic.deliver(400f, true, settings))
    }

    @Test
    fun `does not consume below threshold or without request`() {
        assertEquals(BoilerHeatDelivery(0, 0f, 179f), BoilerHeaterLogic.deliver(179f, true, settings))
        assertEquals(BoilerHeatDelivery(0, 0f, 400f), BoilerHeaterLogic.deliver(400f, false, settings))
        assertEquals(BoilerHeatDelivery(0, 0f, 0f), BoilerHeaterLogic.deliver(0f, true, settings))
    }

    @Test
    fun `never reports output when configured cost cannot be paid`() {
        val expensive = settings.copy(firstCost = 200f)
        assertEquals(BoilerHeatDelivery(0, 0f, 180f), BoilerHeaterLogic.deliver(180f, true, expensive))
        val invalid = settings.copy(firstCost = -1f)
        assertEquals(BoilerHeatDelivery(0, 0f, 180f), BoilerHeaterLogic.deliver(180f, true, invalid))
    }

    @Test
    fun `multiple heaters aggregate independently`() {
        val deliveries = listOf(200f, 300f, 400f).map { BoilerHeaterLogic.deliver(it, true, settings) }
        assertEquals(6, deliveries.sumOf { it.strength })
        assertEquals(6f, deliveries.sumOf { it.consumed.toDouble() }.toFloat())
    }

    @Test
    fun `scales comparator output from empty through full`() {
        assertEquals(0, BoilerHeaterLogic.comparatorOutput(0f, 400f))
        assertEquals(3, BoilerHeaterLogic.comparatorOutput(100f, 400f))
        assertEquals(7, BoilerHeaterLogic.comparatorOutput(200f, 400f))
        assertEquals(15, BoilerHeaterLogic.comparatorOutput(400f, 400f))
        assertEquals(15, BoilerHeaterLogic.comparatorOutput(500f, 400f))
        assertEquals(0, BoilerHeaterLogic.comparatorOutput(100f, 0f))
    }
}
