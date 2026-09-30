package com.bettercontent.betterindustrialheat.content.energy

import kotlin.test.Test
import kotlin.test.assertEquals

class EnergyLadderMathTest {
    @Test
    fun `FE limits containment but never contributes AE`() {
        val plan = EnergyLadderMath.planUnbinding(
            availableUnits = 8,
            previewAe = 800.0,
            previewHeat = 40f,
            storedFe = 450,
            fePerUnit = 200,
            gridDemandAe = 1000.0,
            unitsPerTick = 4,
        )

        assertEquals(2, plan.units)
        assertEquals(400, plan.containmentFe)
        assertEquals(200.0, plan.ae)
        assertEquals(10f, plan.heat)
    }

    @Test
    fun `grid demand prevents unbinding and daughter production`() {
        val plan = EnergyLadderMath.planUnbinding(4, 400.0, 20f, 10_000, 200, 99.0, 4)
        assertEquals(UnbindingPlan(0, 0, 0.0, 0.0f), plan)
    }

    @Test
    fun `parallel scale remains fixed per transducer`() {
        val plan = EnergyLadderMath.planUnbinding(20, 2_000.0, 100f, 20_000, 200, 20_000.0, 3)
        assertEquals(3, plan.units)
        assertEquals(300.0, plan.ae)
    }

    @Test
    fun `invalid or non-finite inputs produce an empty plan`() {
        val empty = UnbindingPlan(0, 0, 0.0, 0.0f)
        val valid = listOf<Number>(4L, 400.0, 20f, 10_000, 200, 400.0, 4L)
        val invalidRows = listOf(
            valid.toMutableList().also { it[0] = 0L },
            valid.toMutableList().also { it[1] = 0.0 },
            valid.toMutableList().also { it[1] = Double.NaN },
            valid.toMutableList().also { it[2] = -1f },
            valid.toMutableList().also { it[2] = Float.NaN },
            valid.toMutableList().also { it[3] = -1 },
            valid.toMutableList().also { it[4] = 0 },
            valid.toMutableList().also { it[5] = 0.0 },
            valid.toMutableList().also { it[5] = Double.POSITIVE_INFINITY },
            valid.toMutableList().also { it[6] = 0L },
        )

        invalidRows.forEach { row ->
            assertEquals(
                empty,
                EnergyLadderMath.planUnbinding(
                    availableUnits = row[0].toLong(),
                    previewAe = row[1].toDouble(),
                    previewHeat = row[2].toFloat(),
                    storedFe = row[3].toInt(),
                    fePerUnit = row[4].toInt(),
                    gridDemandAe = row[5].toDouble(),
                    unitsPerTick = row[6].toLong(),
                ),
            )
        }
    }

    @Test
    fun `per-unit AE underflow produces an empty plan`() {
        val plan = EnergyLadderMath.planUnbinding(
            availableUnits = Long.MAX_VALUE,
            previewAe = Double.MIN_VALUE,
            previewHeat = 1f,
            storedFe = Int.MAX_VALUE,
            fePerUnit = 1,
            gridDemandAe = 1.0,
            unitsPerTick = 1,
        )

        assertEquals(UnbindingPlan(0, 0, 0.0, 0.0f), plan)
    }
}
