package com.bettercontent.betterindustrialheat

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherProbeFootprintTest {
    @Test
    fun `weather probe requires its full five by five chunk footprint`() {
        val origin = BlockPos(160, 64, -80)
        val missing = 166 to -83
        val checked = mutableSetOf<Pair<Int, Int>>()

        val loaded = WeatherProbeFootprint.isLoaded(origin) { x, z ->
            checked += x to z
            x to z != missing
        }

        assertEquals(false, loaded)
        assertEquals(true, missing in checked)
        assertEquals(false, (16 to -8) in checked)
        assertEquals(true, checked.all { (x, z) -> x in 154..166 && z in -86..-74 })
    }
}
