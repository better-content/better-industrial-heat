package com.bettercontent.betterindustrialheat

import com.momosoftworks.coldsweat.util.world.WorldHelper
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraftforge.fml.ModList

object ColdSweatAmbientSampler {
    private val suppressionDepth = ThreadLocal.withInitial { 0 }

    fun sampleWorldTemp(level: Level, pos: BlockPos): Double {
        suppressionDepth.set(suppressionDepth.get() + 1)
        return try {
            WorldHelper.getTemperatureAt(level, pos).takeIf(Double::isFinite) ?: 0.0
        } finally {
            val remainingDepth = suppressionDepth.get() - 1
            if (remainingDepth <= 0) {
                suppressionDepth.remove()
            } else {
                suppressionDepth.set(remainingDepth)
            }
        }
    }

    /**
     * Ambient sampling for the pipe rebalancing. With Weather2 installed the sample must
     * wait for its full probe footprint to be loaded, or the probe would generate chunks.
     */
    fun samplePipeHeat(level: Level, pos: BlockPos): Double {
        if (ModList.get().isLoaded("weather2") && level is ServerLevel &&
            !WeatherProbeFootprint.isLoaded(pos, level::hasChunk)
        ) {
            return HeatSyncConfig.absoluteZeroOffset()
        }
        return ColdSweatHeatMapper.coldSweatToPipeHeat(sampleWorldTemp(level, pos))
    }

    fun shouldSuppressPipeBlockTemp(): Boolean = suppressionDepth.get() > 0
}
