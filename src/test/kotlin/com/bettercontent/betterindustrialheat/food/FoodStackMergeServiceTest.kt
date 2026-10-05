package com.bettercontent.betterindustrialheat.food

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class FoodStackMergeServiceTest {
    @Test
    fun `actual partial ItemStack merge weights moved count and preserves stack ownership`() {
        TestMinecraftBootstrap.bootstrap()
        val destination = ItemStack(Items.APPLE, 3)
        destination.orCreateTag.putString("better_content_quality", "orchard")
        destination.orCreateTag.put("better_content_details", CompoundTag().also { it.putInt("grade", 2) })
        thermal(destination, decay = 0.2, lastTime = 100)
        val destinationNonThermal = destination.tag!!.copy().also { it.remove("better_industrial_heat_food") }

        val source = destination.copy().also {
            it.count = 2
            thermal(it, decay = 0.8, lastTime = 100)
        }
        val sourceBefore = source.copy()
        val output = destination.copy().also { it.count = 4 } // Native transfer already accepted one item.

        FoodStackMergeService.mergeInto(output, destination, source, movedCount = 1)

        assertEquals(4, output.count)
        assertEquals(Items.APPLE, output.item)
        assertEquals(destinationNonThermal, output.tag!!.copy().also { it.remove("better_industrial_heat_food") })
        val merged = output.tag!!.getCompound("better_industrial_heat_food")
        assertEquals(0.35, merged.getDouble("decay"), 1.0e-9)
        assertEquals(100L, merged.getLong("last_time"))
        assertEquals(1.0, merged.getDouble("rate"), 1.0e-9)
        assertTrue(ItemStack.matches(source, sourceBefore), "merge bookkeeping must not mutate the source remainder")
    }

    @Test
    fun `weighted values use destination count and actual moved count`() {
        val destination = values(decay = 0.2, lastTime = 80)
        val source = values(decay = 0.8, lastTime = 100)

        val merged = FoodStackMergeService.weighted(destination, 3, source, 1)

        assertEquals(0.35, merged.decay, 1.0e-9)
        assertEquals(100L, merged.lastTime)
    }

    @Test
    fun `merging two untracked stacks never invents a food record`() {
        TestMinecraftBootstrap.bootstrap()
        val destination = ItemStack(Items.APPLE, 3)
        val source = destination.copy().also { it.count = 1 }
        val output = destination.copy().also { it.count = 4 }

        FoodStackMergeService.mergeInto(output, destination, source, movedCount = 1)

        assertTrue(
            output.tag?.contains("better_industrial_heat_food") != true,
            "an untracked merge must stay untracked so no stale timestamp can charge the world's age",
        )
    }

    @Test
    fun `merging a tracked stack with an untracked one keeps the tracked timestamp`() {
        TestMinecraftBootstrap.bootstrap()
        val destination = ItemStack(Items.APPLE, 1)
        thermal(destination, decay = 0.4, lastTime = 900)
        val source = ItemStack(Items.APPLE, 1)
        val output = destination.copy()

        FoodStackMergeService.mergeInto(output, destination, source, movedCount = 1)

        val merged = output.tag!!.getCompound("better_industrial_heat_food")
        assertEquals(0.2, merged.getDouble("decay"), 1.0e-9)
        assertEquals(900L, merged.getLong("last_time"))
    }

    @Test
    fun `successive partial transfers preserve the count weighted aggregate`() {
        val original = values(decay = 0.2, lastTime = 10)
        val firstTransfer = values(decay = 0.5, lastTime = 20)
        val secondTransfer = values(decay = 0.8, lastTime = 30)

        val afterFirst = FoodStackMergeService.weighted(original, 3, firstTransfer, 1)
        val afterSecond = FoodStackMergeService.weighted(afterFirst, 4, secondTransfer, 2)

        assertEquals((0.2 * 3 + 0.5 + 0.8 * 2) / 6, afterSecond.decay, 1.0e-9)
        assertEquals(30L, afterSecond.lastTime)
    }

    private fun values(decay: Double, lastTime: Long) =
        FoodStackMergeService.ThermalValues(decay = decay, lastTime = lastTime, rate = 1.0, present = true)

    private fun thermal(stack: ItemStack, decay: Double, lastTime: Long) {
        stack.orCreateTag.put("better_industrial_heat_food", CompoundTag().also {
            it.putInt("version", 5)
            it.putDouble("decay", decay)
            it.putLong("last_time", lastTime)
            it.putDouble("rate", 1.0)
            it.putLong("warm_since", 0L)
        })
    }
}
