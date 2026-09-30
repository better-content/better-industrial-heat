package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.AmbientHeatSampling
import com.bettercontent.betterindustrialheat.ColdSweatBridgeGameTestAssertions
import com.bettercontent.betterindustrialheat.HeatSyncConfig
import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.HeatSyncPipeThermalController
import com.bettercontent.betterindustrialheat.HeatSyncThermalTags
import com.bettercontent.betterindustrialheat.PipeThermalSourceResolver
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.fml.ModList
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class HeatPipeGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun directBlockInsertionEnrollsPipeBeforeReload(helper: GameTestHelper) {
        val relative = BlockPos(1, 1, 1)
        val absolute = helper.absolutePos(relative)
        helper.level.setBlockAndUpdate(absolute, HeatSyncRegistries.HEAT_PIPE.get().defaultBlockState())
        val pipe = helper.level.getBlockEntity(absolute) as? HeatPipeBlockEntity

        helper.succeedIf {
            helper.assertTrue(pipe != null, "Direct insertion did not create a pipe block entity")
            helper.assertTrue(
                HeatSyncPipeThermalController.isEnrolled(helper.level, absolute),
                "Direct insertion must join thermal scheduling without a reload or placement event"
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun capabilityHonorsConfiguredBoundsAndSimulation(helper: GameTestHelper) {
        val pipe = placePipe(helper, BlockPos(1, 1, 1))
        val storage = pipe.getCapability(HeatCapabilities.HEAT).resolve()
            .orElseThrow { IllegalStateException("Heat pipe capability was unavailable") }

        storage.setHeat(Float.MAX_VALUE)
        val simulated = storage.extractHeat(Float.MAX_VALUE, true)
        val extracted = storage.extractHeat(Float.MAX_VALUE, false)
        val minimumAfterExtraction = pipe.getHeat()
        storage.setHeat(Float.NaN)

        helper.succeedIf {
            helper.assertTrue(simulated == (HeatSyncConfig.pipeMaxHeat() - HeatSyncConfig.pipeMinHeat()).toFloat(), "Simulated extraction returned the wrong amount")
            helper.assertTrue(extracted == simulated, "Executed extraction did not match simulation")
            helper.assertTrue(minimumAfterExtraction == HeatSyncConfig.pipeMinHeat().toFloat(), "Extraction crossed the configured minimum")
            helper.assertTrue(pipe.getHeat() == HeatSyncConfig.absoluteZeroOffset().toFloat(), "Non-finite heat was not reset to neutral")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun loadAndUpdateTagClampPersistedHeat(helper: GameTestHelper) {
        val pipe = placePipe(helper, BlockPos(1, 1, 1))
        pipe.load(CompoundTag().also { it.putFloat("Heat", Float.MAX_VALUE) })

        helper.succeedIf {
            helper.assertTrue(pipe.getHeat() == HeatSyncConfig.pipeMaxHeat().toFloat(), "Loaded heat was not clamped")
            helper.assertTrue(pipe.updateTag.getFloat("Heat") == pipe.getHeat(), "Update tag did not contain the authoritative heat")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun adjacentThermalStorageTransfersConservatively(helper: GameTestHelper) {
        val sourcePos = BlockPos(1, 1, 1)
        val pipePos = BlockPos(2, 1, 1)
        helper.setBlock(sourcePos, HeatSyncRegistries.CREATIVE_HEAT_SOURCE.get())
        val source = requireNotNull(helper.getBlockEntity(sourcePos) as? ConstantTemperatureBlockEntity)
        val pipe = placePipe(helper, pipePos)
        source.setHeat(10_000f)
        pipe.setHeat(100f)

        HeatBlockEntity.transferAround(source)

        helper.succeedIf {
            helper.assertTrue(source.getHeat() == 9_920f, "Heat source did not lose the transferred heat")
            helper.assertTrue(pipe.getHeat() == 180f, "Pipe did not receive the transferred heat")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun snowIsAConfiguredPipeColdSource(helper: GameTestHelper) {
        val pipePos = BlockPos(1, 1, 1)
        placePipe(helper, pipePos)
        helper.setBlock(pipePos.east(), Blocks.SNOW_BLOCK)

        helper.succeedIf {
            helper.assertTrue(
                PipeThermalSourceResolver.resolveAdjacentAverageTargetHeat(helper.level, helper.absolutePos(pipePos)) ==
                    HeatSyncConfig.taggedColdSourceDefaultHeat(),
                "Snow block was not resolved through ${HeatSyncThermalTags.PIPE_COLD_SOURCES.location()}",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun coldSweatBridgeMatchesRuntimeProfile(helper: GameTestHelper) {
        val pipePos = BlockPos(1, 1, 1)
        val pipe = placePipe(helper, pipePos)
        if (!ModList.get().isLoaded(HeatSyncMod.COLD_SWEAT_MOD_ID)) {
            helper.succeedIf {
                helper.assertTrue(
                    AmbientHeatSampling.samplePipeHeat(helper.level, helper.absolutePos(pipePos)) ==
                        HeatSyncConfig.absoluteZeroOffset(),
                    "Cold Sweat-absent runtime did not use neutral ambient heat",
                )
            }
            return
        }

        ColdSweatBridgeGameTestAssertions.assertPipeRadiation(helper, pipePos, pipe)
    }

    private fun placePipe(helper: GameTestHelper, pos: BlockPos): HeatPipeBlockEntity {
        helper.setBlock(pos, HeatSyncRegistries.HEAT_PIPE.get())
        return requireNotNull(helper.getBlockEntity(pos) as? HeatPipeBlockEntity)
    }
}
