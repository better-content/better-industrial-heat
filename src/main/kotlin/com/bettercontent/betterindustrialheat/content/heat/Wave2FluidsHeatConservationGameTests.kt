package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class Wave2FluidsHeatConservationGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun oversizedOptionalSurfaceIsNeverCopiedToNativePipeSave(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos, HeatSyncRegistries.HEAT_PIPE.get())
        val pipe = requireNotNull(helper.getBlockEntity(pos) as? HeatPipeBlockEntity)
        val oversized = CompoundTag().also { it.putString("label", "x".repeat(4096)) }
        val incoming = CompoundTag().also {
            it.put(ThermalSurfaceVariant.SETTINGS_KEY, oversized)
            it.putFloat("Heat", 125f)
        }
        pipe.load(incoming)
        helper.succeedIf {
            helper.assertTrue(pipe.getHeat() == 125f, "Unrelated native heat was discarded with invalid settings")
            helper.assertTrue(!pipe.storedSurfaceTag().contains(ThermalSurfaceVariant.SETTINGS_KEY),
                "Oversized optional settings survived into a native save/packet")
            helper.assertTrue(pipe.transferRatio() == 1f, "Invalid settings changed native thermal transfer")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun nativePipeTransferDoesNotCreditMoreThanSourceCanExtract(helper: GameTestHelper) {
        val pipePos = BlockPos(1, 1, 1)
        val receiverPos = pipePos.east()
        helper.setBlock(pipePos, HeatSyncRegistries.HEAT_PIPE.get())
        helper.setBlock(receiverPos, HeatSyncRegistries.BOILER_HEATER.get())
        val pipe = requireNotNull(helper.getBlockEntity(pipePos) as? HeatPipeBlockEntity)
        val receiver = requireNotNull(helper.getBlockEntity(receiverPos) as? BoilerHeaterBlockEntity)
        val floor = com.bettercontent.betterindustrialheat.HeatSyncConfig.pipeMinHeat().toFloat()
        pipe.setHeat(floor + 5f)
        receiver.setHeat(0f)
        val before = pipe.getHeat() + receiver.getHeat()
        HeatBlockEntity.transferAround(pipe)
        helper.succeedIf {
            helper.assertTrue(pipe.getHeat() >= floor, "Native pipe heat fell below its floor")
            helper.assertTrue(receiver.getHeat() > 0f, "No native heat was transferred")
            helper.assertTrue(kotlin.math.abs(before - pipe.getHeat() - receiver.getHeat()) < .001f,
                "Native transfer created or destroyed heat")
        }
    }
}
