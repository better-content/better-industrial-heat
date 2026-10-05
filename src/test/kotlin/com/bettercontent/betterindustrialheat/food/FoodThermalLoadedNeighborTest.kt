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
}
