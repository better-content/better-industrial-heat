package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class ThermalFireboxGameTests {
    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun consumesFuelBeforeProducingHeat(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos, HeatSyncRegistries.THERMAL_FIREBOX.get())
        val firebox = requireNotNull(helper.getBlockEntity(pos) as? ThermalFireboxBlockEntity)
        val inventory = firebox.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve()
            .orElseThrow { IllegalStateException("Thermal firebox item capability was unavailable") }
        val remainder = inventory.insertItem(0, ItemStack(Items.COAL), false)
        require(remainder.isEmpty) { "Thermal firebox rejected valid furnace fuel" }

        ThermalFireboxBlockEntity.tick(helper.level, pos, helper.getBlockState(pos), firebox)

        helper.succeedIf {
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty, "Firebox produced heat without consuming its fuel item")
            helper.assertTrue(firebox.getHeat() == 106.0f, "Expected one configured heat tick, found ${firebox.getHeat()}")
        }
    }
}
