package com.bettercontent.betterindustrialheat.api

import kotlin.test.Test
import kotlin.test.assertEquals

/** Reproduces the native pipe heat floor and a receiver with limited space. */
class Wave2FluidsHeatConservationTest {
    private class Reservoir(
        private var heat: Float,
        private val minimum: Float,
        private val maximum: Float,
        private val actualAcceptLimit: Float = Float.POSITIVE_INFINITY,
    ) : IHeatStorage {
        override fun getHeat() = heat
        override fun getMaxHeat() = maximum
        override fun setHeat(heat: Float) { this.heat = heat.coerceIn(minimum, maximum) }
        override fun addHeat(amount: Float, simulate: Boolean): Float {
            val moved = amount.coerceAtMost((maximum - heat).coerceAtLeast(0f))
                .let { if (simulate) it else it.coerceAtMost(actualAcceptLimit) }
            if (!simulate) setHeat(heat + moved)
            return moved
        }
        override fun extractHeat(amount: Float, simulate: Boolean): Float {
            val moved = amount.coerceAtMost((heat - minimum).coerceAtLeast(0f))
            if (!simulate) setHeat(heat - moved)
            return moved
        }
    }

    @Test
    fun `pipe near native floor transfers only extractable heat`() {
        val pipe = Reservoir(105f, 100f, 400f)
        val receiver = Reservoir(10f, 0f, 400f)
        assertEquals(5f, HeatBlockEntity.settleHeat(pipe, receiver, 80f))
        assertEquals(100f, pipe.getHeat())
        assertEquals(15f, receiver.getHeat())
        assertEquals(115f, pipe.getHeat() + receiver.getHeat())
    }

    @Test
    fun `receiver saturation and reduced actual acceptance cannot duplicate or lose heat`() {
        val sender = Reservoir(200f, 0f, 400f)
        val receiver = Reservoir(147f, 0f, 150f, actualAcceptLimit = 1f)
        assertEquals(1f, HeatBlockEntity.settleHeat(sender, receiver, 80f))
        assertEquals(199f, sender.getHeat())
        assertEquals(148f, receiver.getHeat())
        assertEquals(347f, sender.getHeat() + receiver.getHeat())
    }

    @Test
    fun `absent capacity and invalid requests are inert`() {
        val sender = Reservoir(100f, 100f, 400f)
        val receiver = Reservoir(0f, 0f, 400f)
        assertEquals(0f, HeatBlockEntity.settleHeat(sender, receiver, 80f))
        assertEquals(0f, HeatBlockEntity.settleHeat(sender, receiver, Float.NaN))
        assertEquals(100f, sender.getHeat())
        assertEquals(0f, receiver.getHeat())
    }
}
