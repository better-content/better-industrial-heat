package com.bettercontent.betterindustrialheat

import com.momosoftworks.coldsweat.util.world.WorldHelper
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

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

    fun samplePipeHeat(level: Level, pos: BlockPos): Double =
        ColdSweatHeatMapper.coldSweatToPipeHeat(sampleWorldTemp(level, pos))

    fun shouldSuppressPipeBlockTemp(): Boolean = suppressionDepth.get() > 0
}
