package com.bettercontent.betterindustrialheat

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipeThermalMathTest {
    @Test
    fun `step applies equalization ambient source pull and loss in order`() {
        val stepped = PipeThermalMath.step(
            pipeHeat = 100.0,
            ambientHeat = 60.0,
            neighborAverage = 80.0,
            sourceHeat = 20.0,
            ambientBlendRate = 0.08,
            networkEqualizationStrength = 0.30,
            coldSourcePullRate = 0.18,
            pipeLossPerTick = 0.5,
            minPipeHeat = 0.0,
            maxPipeHeat = 400.0
        )

        assertTrue(abs(stepped - 77.9496) < 0.0001, "expected 77.9496, got $stepped")
    }

    @Test
    fun `step clamps to configured minimum after loss`() {
        assertEquals(
            0.0,
            PipeThermalMath.step(
                pipeHeat = 0.2,
                pipeLossPerTick = 0.5,
                minPipeHeat = 0.0,
                maxPipeHeat = 400.0,
                ambientBlendRate = 0.08,
                networkEqualizationStrength = 0.30,
                coldSourcePullRate = 0.18
            )
        )
    }

    @Test
    fun `step clamps to configured maximum after modifiers`() {
        assertEquals(
            400.0,
            PipeThermalMath.step(
                pipeHeat = 500.0,
                ambientHeat = 1000.0,
                ambientBlendRate = 0.08,
                networkEqualizationStrength = 0.30,
                coldSourcePullRate = 0.18,
                pipeLossPerTick = 0.5,
                minPipeHeat = 0.0,
                maxPipeHeat = 400.0
            )
        )
    }

}
