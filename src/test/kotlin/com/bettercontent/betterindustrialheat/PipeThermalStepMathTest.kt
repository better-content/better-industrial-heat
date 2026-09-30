package com.bettercontent.betterindustrialheat

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipeThermalStepMathTest {
    @Test
    fun `step applies equalization ambient source pull and loss in order`() {
        val stepped = PipeThermalStepMath.step(
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
            PipeThermalStepMath.step(
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
            PipeThermalStepMath.step(
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

    @Test
    fun `step with no optional influences still applies loss and clamp`() {
        assertEquals(
            99.5,
            PipeThermalStepMath.step(
                pipeHeat = 100.0,
                ambientBlendRate = 0.08,
                networkEqualizationStrength = 0.30,
                coldSourcePullRate = 0.18,
                pipeLossPerTick = 0.5,
                minPipeHeat = 0.0,
                maxPipeHeat = 400.0
            )
        )
    }

    @Test
    fun `paired pipe observations conserve heat when external terms are disabled`() {
        val first = PipeThermalStepMath.step(
            pipeHeat = 300.0, neighborAverage = 100.0,
            ambientBlendRate = 0.0, networkEqualizationStrength = 0.30,
            coldSourcePullRate = 0.0, pipeLossPerTick = 0.0, minPipeHeat = 0.0, maxPipeHeat = 400.0,
        )
        val second = PipeThermalStepMath.step(
            pipeHeat = 100.0, neighborAverage = 300.0,
            ambientBlendRate = 0.0, networkEqualizationStrength = 0.30,
            coldSourcePullRate = 0.0, pipeLossPerTick = 0.0, minPipeHeat = 0.0, maxPipeHeat = 400.0,
        )

        assertEquals(400.0, first + second, 1.0e-9)
        assertEquals(240.0, first, 1.0e-9)
        assertEquals(160.0, second, 1.0e-9)
    }
}
