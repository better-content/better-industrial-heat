package com.bettercontent.betterindustrialheat.compat.pneumaticcraft

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlockEntity
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import me.desht.pneumaticcraft.api.PNCCapabilities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class PneumaticHeatBridgeGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun createTankConstructionDoesNotQueryUninitializedCapabilities(helper: GameTestHelper) {
        val tank = FluidTankBlockEntity(
            BlockEntityType.FURNACE,
            BlockPos.ZERO,
            Blocks.AIR.defaultBlockState(),
        )

        helper.succeedIf {
            helper.assertTrue(
                !tank.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY).isPresent,
                "Create fluid tanks must not receive Better Industrial Heat's Pneumatic capability",
            )
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun heatSyncEntityExposesLiveInvalidatingPneumaticInterface(helper: GameTestHelper) {
        val position = BlockPos(1, 1, 1)
        helper.setBlock(position, HeatSyncRegistries.HEAT_PIPE.get())
        val pipe = requireNotNull(helper.getBlockEntity(position) as? HeatPipeBlockEntity)
        pipe.setHeat(100f)
        val capability = pipe.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY)
        val exchanger = capability.resolve()
            .orElseThrow { IllegalStateException("Better Industrial Heat pipe had no Pneumatic heat capability") }
        exchanger.addHeat(5.0)

        helper.succeedIf {
            helper.assertTrue(pipe.getHeat() == 105f, "Pneumatic heat did not reach Better Industrial Heat storage")
            helper.assertTrue(exchanger.isSideConnected(Direction.NORTH), "Expected connected pipe side")
            pipe.invalidateCaps()
            helper.assertTrue(!capability.isPresent, "Pneumatic capability survived block-entity invalidation")
        }
    }
}
