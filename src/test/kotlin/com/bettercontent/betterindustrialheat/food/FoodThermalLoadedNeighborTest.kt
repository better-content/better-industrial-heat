package com.bettercontent.betterindustrialheat.food

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FoodThermalLoadedNeighborTest {
    @Test
    fun `unloaded adjacent chunks are excluded before world sampling`() {
        val origin = BlockPos(15, 64, 0)
        val checked = mutableListOf<BlockPos>()

        val neighbors = FoodThermalService.loadedAdjacentPositions(origin) { candidate ->
            checked += candidate
            candidate != origin.relative(Direction.EAST)
        }

        assertEquals(Direction.values().size, checked.size)
        assertEquals(
            Direction.values().filter { it != Direction.EAST },
            neighbors.map { it.first },
        )
    }

    @Test
    fun `weather probe requires its full five by five chunk footprint`() {
        val origin = BlockPos(160, 64, -80)
        val missing = 16 to -8
        val checked = mutableSetOf<Pair<Int, Int>>()

        val loaded = FoodThermalService.weatherProbeChunksLoaded(origin) { x, z ->
            checked += x to z
            x to z != missing
        }

        assertEquals(false, loaded)
        assertEquals(true, missing in checked)
    }
}
