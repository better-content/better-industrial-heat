package com.bettercontent.betterindustrialheat.food

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class FoodThermalStateTest {
    @Test
    fun `legacy records migrate without charging the pre-migration gap`() {
        TestMinecraftBootstrap.bootstrap()
        val stack = ItemStack(Items.APPLE)
        stack.orCreateTag.put("better_industrial_heat_food", CompoundTag().also {
            it.putInt("version", 4)
            it.putInt("temperature_bucket_c", 5)
            it.putInt("last_target_bucket_c", 5)
            it.putBoolean("last_target_appliance", false)
            it.putDouble("temperature_precise_k", 298.15)
            it.putDouble("decay", 0.9)
            it.putLong("last_time", 5)
            it.putDouble("preservation_rate", 1.0)
        })

        FoodThermalService.tick(stack, FoodThermalService.Storage.AMBIENT, 1_000_000)

        assertEquals(0.9, decayOf(stack), 1.0e-9)
        assertFalse(FoodThermalService.isFrozen(stack), "a migrated record must not read as frozen")

        FoodThermalService.tick(stack, FoodThermalService.Storage.AMBIENT, 1_024_000)
        assertEquals(1.0, decayOf(stack), 1.0e-9)
    }

    @Test
    fun `frozen food thaws only after its thaw window in non-frozen storage`() {
        TestMinecraftBootstrap.bootstrap()
        val stack = ItemStack(Items.APPLE)
        FoodThermalService.tick(stack, FoodThermalService.Storage.FROZEN, 0)
        assertTrue(FoodThermalService.isFrozen(stack), "frozen storage must make food uneatable")

        FoodThermalService.tick(stack, FoodThermalService.Storage.AMBIENT, 1_000)
        assertTrue(FoodThermalService.isFrozen(stack), "frozen food must stay uneatable while thawing")

        FoodThermalService.tick(stack, FoodThermalService.Storage.AMBIENT, 1_000 + FoodThermalService.THAW_TICKS - 1)
        assertTrue(FoodThermalService.isFrozen(stack))

        FoodThermalService.tick(stack, FoodThermalService.Storage.AMBIENT, 1_000 + FoodThermalService.THAW_TICKS)
        assertFalse(FoodThermalService.isFrozen(stack), "frozen food must become eatable once the thaw window passes")
    }

    private fun decayOf(stack: ItemStack): Double =
        stack.tag!!.getCompound("better_industrial_heat_food").getDouble("decay")
}
