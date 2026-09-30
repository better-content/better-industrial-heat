package com.bettercontent.betterindustrialheat.compat.create

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.simibubi.create.AllBlocks
import com.simibubi.create.api.boiler.BoilerHeater
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class CreateActiveHeatGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun boilersRejectPassiveHeatAndAcceptActiveBurners(helper: GameTestHelper) {
        val relativePos = BlockPos(1, 1, 1)
        val absolutePos = helper.absolutePos(relativePos)

        helper.succeedIf {
            listOf(
                Blocks.LAVA.defaultBlockState(),
                Blocks.MAGMA_BLOCK.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState(),
                Blocks.FIRE.defaultBlockState(),
                AllBlocks.LIT_BLAZE_BURNER.defaultState,
            ).forEach { state ->
                helper.assertTrue(
                    BoilerHeater.findHeat(helper.level, absolutePos, state) == BoilerHeater.NO_HEAT.toFloat(),
                    "Passive heater ${state.block} unexpectedly supplied Create boiler heat",
                )
            }

            assertBurnerBoilerHeat(helper, absolutePos, BlazeBurnerBlock.HeatLevel.SMOULDERING, BoilerHeater.NO_HEAT)
            assertBurnerBoilerHeat(helper, absolutePos, BlazeBurnerBlock.HeatLevel.FADING, 1)
            assertBurnerBoilerHeat(helper, absolutePos, BlazeBurnerBlock.HeatLevel.KINDLED, 1)
            assertBurnerBoilerHeat(helper, absolutePos, BlazeBurnerBlock.HeatLevel.SEETHING, 2)
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun bulkBlastingRequiresAnActiveBurner(helper: GameTestHelper) {
        val relativePos = BlockPos(1, 1, 1)
        val absolutePos = helper.absolutePos(relativePos)

        helper.succeedIf {
            helper.setBlock(relativePos, Blocks.LAVA)
            helper.assertTrue(
                !AllFanProcessingTypes.BLASTING.isValidAt(helper.level, absolutePos),
                "Still lava must not activate bulk blasting",
            )

            helper.setBlock(relativePos, Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1))
            helper.assertTrue(
                !AllFanProcessingTypes.BLASTING.isValidAt(helper.level, absolutePos),
                "Flowing lava must not activate bulk blasting",
            )

            assertBurnerBlasting(helper, relativePos, absolutePos, BlazeBurnerBlock.HeatLevel.SMOULDERING, false)
            assertBurnerBlasting(helper, relativePos, absolutePos, BlazeBurnerBlock.HeatLevel.FADING, true)
            assertBurnerBlasting(helper, relativePos, absolutePos, BlazeBurnerBlock.HeatLevel.KINDLED, true)
            assertBurnerBlasting(helper, relativePos, absolutePos, BlazeBurnerBlock.HeatLevel.SEETHING, true)
        }
    }

    private fun assertBurnerBoilerHeat(
        helper: GameTestHelper,
        absolutePos: BlockPos,
        heatLevel: BlazeBurnerBlock.HeatLevel,
        expected: Int,
    ) {
        val state = AllBlocks.BLAZE_BURNER.defaultState.setValue(BlazeBurnerBlock.HEAT_LEVEL, heatLevel)
        helper.assertTrue(
            BoilerHeater.findHeat(helper.level, absolutePos, state) == expected.toFloat(),
            "Burner state $heatLevel did not report expected boiler heat $expected",
        )
    }

    private fun assertBurnerBlasting(
        helper: GameTestHelper,
        relativePos: BlockPos,
        absolutePos: BlockPos,
        heatLevel: BlazeBurnerBlock.HeatLevel,
        expected: Boolean,
    ) {
        helper.setBlock(
            relativePos,
            AllBlocks.BLAZE_BURNER.defaultState.setValue(BlazeBurnerBlock.HEAT_LEVEL, heatLevel),
        )
        helper.assertTrue(
            AllFanProcessingTypes.BLASTING.isValidAt(helper.level, absolutePos) == expected,
            "Burner state $heatLevel bulk-blasting validity did not match $expected",
        )
    }
}
