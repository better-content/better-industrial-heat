package com.bettercontent.betterindustrialheat.compat.pneumaticcraft

import com.bettercontent.betterindustrialheat.api.IThermalBody
import java.nio.file.Files
import java.nio.file.Path
import net.minecraft.core.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PneumaticHeatBridgeTest {
    @Test
    fun `Pneumatic interface delegates heat and preserves thermal policy`() {
        val body = TestThermalBody()
        val exchanger = PneumaticHeatBridge.HeatSyncPneumaticLogic(body)

        assertEquals(295.15, exchanger.temperature, 0.0001)
        assertEquals(16.0, exchanger.thermalResistance, 0.0001)
        assertEquals(1.0, exchanger.thermalCapacity, 0.0001)
        assertTrue(exchanger.isSideConnected(Direction.NORTH))
        assertFalse(exchanger.isSideConnected(Direction.UP))

        exchanger.addHeat(5.0)
        assertEquals(5.0, body.heat, 0.0001)
        exchanger.addHeat(-2.0)
        assertEquals(3.0, body.heat, 0.0001)
        exchanger.temperature = 315.15
        assertEquals(315.15, body.temperature, 0.0001)
    }

    @Test
    fun `attachment source never queries a block entity capability`() {
        val source = Files.readString(
            Path.of("src/main/kotlin/com/bettercontent/betterindustrialheat/compat/pneumaticcraft/PneumaticHeatBridge.kt"),
        )
        val attachBody = source.substringAfter("internal fun attachCapabilities")
            .substringBefore("private class PneumaticHeatProvider")

        assertFalse(attachBody.contains("getCapability"))
        assertTrue(attachBody.contains("as? HeatBlockEntity ?: return"))
    }

    private class TestThermalBody : IThermalBody {
        var heat = 0.0
        var temperature = 295.15

        override fun temperatureKelvin(): Double = temperature
        override fun minTemperatureKelvin(): Double = 0.0
        override fun maxTemperatureKelvin(): Double = 1495.15
        override fun heatCapacityHUPerK(): Double = 0.25
        override fun conductanceHUPerKTick(side: Direction?): Double = 0.0625
        override fun canConnect(side: Direction?): Boolean = side != Direction.UP
        override fun insertHeatHU(amount: Double, simulate: Boolean): Double = amount.also {
            if (!simulate) heat += it
        }
        override fun extractHeatHU(amount: Double, simulate: Boolean): Double = amount.coerceAtMost(heat).also {
            if (!simulate) heat -= it
        }
        override fun setTemperatureKelvin(temperature: Double) {
            this.temperature = temperature
        }
    }
}
