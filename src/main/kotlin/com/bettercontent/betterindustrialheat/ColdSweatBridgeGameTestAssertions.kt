package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper

/** Loaded only by the Cold Sweat-present GameTest profile. */
object ColdSweatBridgeGameTestAssertions {
    fun assertPipeRadiation(helper: GameTestHelper, relativePos: BlockPos, pipe: HeatPipeBlockEntity) {
        val blockTemp = PipeBlockTemp()
        val absolutePos = helper.absolutePos(relativePos)
        val state = helper.getBlockState(relativePos)

        pipe.setHeat(HeatSyncConfig.absoluteZeroOffset().toFloat())
        val neutral = blockTemp.getTemperature(helper.level, null, state, absolutePos, 0.0)
        pipe.setHeat(HeatSyncConfig.pipeMaxHeat().toFloat())
        val hot = blockTemp.getTemperature(helper.level, null, state, absolutePos, 0.0)
        pipe.setHeat(HeatSyncConfig.pipeMinHeat().toFloat())
        val cold = blockTemp.getTemperature(helper.level, null, state, absolutePos, 0.0)

        helper.succeedIf {
            helper.assertTrue(neutral == 0.0, "Neutral pipe emitted $neutral instead of zero")
            helper.assertTrue(hot == HeatSyncConfig.pipeBlockTempMaxEffect(), "Hot pipe did not reach the configured positive cap")
            helper.assertTrue(cold == -HeatSyncConfig.pipeBlockTempMaxEffect(), "Cold pipe did not reach the configured negative cap")
            helper.assertTrue(blockTemp.hasBlock(HeatSyncRegistries.THERMAL_FIREBOX.get()), "Thermal firebox is missing from the radiator tag")
            helper.assertTrue(blockTemp.hasBlock(HeatSyncRegistries.BOILER_HEATER.get()), "Boiler heater is missing from the radiator tag")
            helper.assertTrue(blockTemp.hasBlock(HeatSyncRegistries.CREATIVE_HEAT_SOURCE.get()), "Creative heat source is missing from the radiator tag")
            helper.assertTrue(blockTemp.hasBlock(HeatSyncRegistries.CREATIVE_COLD_SOURCE.get()), "Creative cold source is missing from the radiator tag")
            helper.assertTrue(AmbientHeatSampling.samplePipeHeat(helper.level, absolutePos).isFinite(), "Ambient sampler returned a non-finite heat")
        }
    }
}
