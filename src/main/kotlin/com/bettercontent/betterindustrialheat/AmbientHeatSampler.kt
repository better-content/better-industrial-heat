package com.bettercontent.betterindustrialheat

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

fun interface AmbientHeatSampler {
    fun samplePipeHeat(level: Level, pos: BlockPos): Double
}

/** Keeps the core thermal controller free of optional Cold Sweat class references. */
object AmbientHeatSampling {
    @Volatile
    private var sampler: AmbientHeatSampler = neutralSampler()

    fun install(sampler: AmbientHeatSampler) {
        this.sampler = sampler
    }

    fun samplePipeHeat(level: Level, pos: BlockPos): Double {
        val sampled = sampler.samplePipeHeat(level, pos)
        return if (sampled.isFinite()) sampled else HeatSyncConfig.absoluteZeroOffset()
    }

    internal fun resetToNeutral() {
        sampler = neutralSampler()
    }

    private fun neutralSampler(): AmbientHeatSampler = AmbientHeatSampler { _, _ ->
        HeatSyncConfig.absoluteZeroOffset()
    }
}
