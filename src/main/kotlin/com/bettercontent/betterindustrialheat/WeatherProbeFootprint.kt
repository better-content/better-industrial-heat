package com.bettercontent.betterindustrialheat

import net.minecraft.core.BlockPos

/**
 * Mirrors the chunk footprint Weather2's `WindManager.calculateAverageChunkHeightAround`
 * probes around a position: height samples at (x + offset) * 16 + 8 for offsets
 * -6, -3, 0, 3, 6 on both axes. Those block coordinates address chunks x + offset, so the
 * footprint is checked exactly there. Probing earlier would make Weather2 generate those
 * chunks, so ambient sampling waits until the whole footprint is loaded.
 */
object WeatherProbeFootprint {
    private val OFFSETS = intArrayOf(-6, -3, 0, 3, 6)

    fun isLoaded(pos: BlockPos, hasChunk: (Int, Int) -> Boolean): Boolean =
        OFFSETS.all { dx -> OFFSETS.all { dz -> hasChunk(pos.x + dx, pos.z + dz) } }
}
