package com.bettercontent.betterindustrialheat

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object PipeThermalSourceResolver {
    private val DIRECTIONS: Array<Direction> = Direction.values()

    fun resolveAdjacentAverageTargetHeat(level: Level, pos: BlockPos): Double? {
        var totalHeat = 0.0
        var sourceCount = 0

        for (direction in DIRECTIONS) {
            val targetHeat = targetHeatFor(level.getBlockState(pos.relative(direction))) ?: continue
            totalHeat += targetHeat
            sourceCount++
        }

        return if (sourceCount == 0) null else totalHeat / sourceCount
    }

    fun targetHeatFor(state: BlockState): Double? = when {
        state.`is`(Blocks.BLUE_ICE) -> HeatSyncConfig.blueIceSourceHeat()
        state.`is`(Blocks.PACKED_ICE) -> HeatSyncConfig.packedIceSourceHeat()
        state.`is`(Blocks.ICE) -> HeatSyncConfig.iceSourceHeat()
        state.`is`(HeatSyncThermalTags.PIPE_COLD_SOURCES) -> HeatSyncConfig.taggedColdSourceDefaultHeat()
        else -> null
    }
}
