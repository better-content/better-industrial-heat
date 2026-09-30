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
        thermal(destination, temperatureK = 273.15, decay = 0.2, lastTime = 100)
        val destinationNonThermal = destination.tag!!.copy().also { it.remove("better_industrial_heat_food") }

        val source = destination.copy().also {
            it.count = 2
            thermal(it, temperatureK = 373.15, decay = 0.8, lastTime = 100)
        }
        val sourceBefore = source.copy()
        val output = destination.copy().also { it.count = 4 } // Native transfer already accepted one item.

        FoodStackMergeService.mergeInto(output, destination, source, movedCount = 1)

        assertEquals(4, output.count)
        assertEquals(Items.APPLE, output.item)
        assertEquals(destinationNonThermal, output.tag!!.copy().also { it.remove("better_industrial_heat_food") })
        val merged = output.tag!!.getCompound("better_industrial_heat_food")
        assertEquals(298.15, merged.getDouble("temperature_precise_k"), 1.0e-9)
        assertEquals(0.35, merged.getDouble("decay"), 1.0e-9)
        assertTrue(ItemStack.matches(source, sourceBefore), "merge bookkeeping must not mutate the source remainder")
    }

    @Test
    fun `weighted values use destination count and actual moved count`() {
        val destination = values(temperatureK = 273.15, decay = 0.2, lastTime = 80)
        val source = values(temperatureK = 373.15, decay = 0.8, lastTime = 100)

        val merged = FoodStackMergeService.weighted(destination, 3, source, 1)

        assertEquals(298.15, merged.temperatureK, 1.0e-9)
        assertEquals(0.35, merged.decay, 1.0e-9)
        assertEquals(100, merged.lastTime)
    }

    @Test
    fun `temperature bucket is deterministic while decay retains fractions`() {
        assertEquals(4, FoodStackMergeService.bucketForKelvin(295.15))
        assertEquals(5, FoodStackMergeService.bucketForKelvin(295.65))
        val destination = values(295.15, 0.501, 100)
        val source = values(295.15, 0.506, 100)
        assertEquals(0.5035, FoodStackMergeService.weighted(destination, 1, source, 1).decay, 1.0e-9)
    }

    @Test
    fun `successive partial transfers preserve the count weighted aggregate`() {
        val original = values(temperatureK = 273.15, decay = 0.2, lastTime = 10)
        val firstTransfer = values(temperatureK = 313.15, decay = 0.5, lastTime = 20)
        val secondTransfer = values(temperatureK = 373.15, decay = 0.8, lastTime = 30)

        val afterFirst = FoodStackMergeService.weighted(original, 3, firstTransfer, 1)
        val afterSecond = FoodStackMergeService.weighted(afterFirst, 4, secondTransfer, 2)

        assertEquals((273.15 * 3 + 313.15 + 373.15 * 2) / 6, afterSecond.temperatureK, 1.0e-9)
        assertEquals((0.2 * 3 + 0.5 + 0.8 * 2) / 6, afterSecond.decay, 1.0e-9)
        assertEquals(30, afterSecond.lastTime)
    }

    private fun values(temperatureK: Double, decay: Double, lastTime: Long) =
        FoodStackMergeService.ThermalValues(
            temperatureK,
            decay,
            lastTime,
            targetBucket = 4,
            targetAppliance = false,
            preservationRate = 1.0,
            present = true,
        )

    private fun thermal(stack: ItemStack, temperatureK: Double, decay: Double, lastTime: Long) {
        stack.orCreateTag.put("better_industrial_heat_food", CompoundTag().also {
            it.putInt("version", 4)
            it.putInt("temperature_bucket_c", FoodStackMergeService.bucketForKelvin(temperatureK))
            it.putDouble("temperature_precise_k", temperatureK)
            it.putDouble("decay", decay)
            it.putLong("last_time", lastTime)
            it.putInt("last_target_bucket_c", 4)
            it.putBoolean("last_target_appliance", false)
            it.putDouble("preservation_rate", 1.0)
        })
    }
}
