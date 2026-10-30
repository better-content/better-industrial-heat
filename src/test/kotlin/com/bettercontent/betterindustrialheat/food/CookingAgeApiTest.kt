package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.api.CookingAgeApi
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.*

class CookingAgeApiTest {
    @Test fun `multi input transfer settles actual storage and never averages age`() {
        TestMinecraftBootstrap.bootstrap()
        val old = ItemStack(Items.BEEF)
        val cold = ItemStack(Items.CARROT)
        FoodThermalService.state(old, FoodThermalService.Storage.AMBIENT, 0).putDouble("decay", .7)
        FoodThermalService.state(cold, FoodThermalService.Storage.COLD, 0).putDouble("decay", .75)
        val beforeOld = old.tag!!.copy()
        val beforeCold = cold.tag!!.copy()
        val output = ItemStack(Items.COOKED_BEEF, 8)
        val expected = old.copy()
        FoodThermalService.settle(expected, FoodThermalService.Storage.AMBIENT, 2400)
        assertSame(output, CookingAgeApi.transfer(listOf(
            CookingAgeApi.Input(cold, FoodThermalService.Storage.COLD),
            CookingAgeApi.Input(old, FoodThermalService.Storage.AMBIENT)
        ), output, 2400))
        assertEquals(expected.tag!!.getCompound("better_industrial_heat_food"), output.tag!!.getCompound("better_industrial_heat_food"))
        assertEquals(.8, output.tag!!.getCompound("better_industrial_heat_food").getDouble("decay"), .00001)
        assertEquals(beforeOld, old.tag)
        assertEquals(beforeCold, cold.tag)
    }
    @Test fun `existing older result cannot be rejuvenated by fresh ingredient`() {
        TestMinecraftBootstrap.bootstrap()
        val output = ItemStack(Items.BREAD)
        FoodThermalService.state(output, FoodThermalService.Storage.AMBIENT, 500).putDouble("decay", 1.25)
        val before = output.tag!!.copy()
        CookingAgeApi.transfer(listOf(CookingAgeApi.Input(ItemStack(Items.CARROT), FoodThermalService.Storage.AMBIENT)), output, 500)
        assertEquals(before, output.tag)
    }
    @Test fun `equal decay cannot rebase an existing output clock`() {
        TestMinecraftBootstrap.bootstrap()
        val output = ItemStack(Items.BREAD)
        FoodThermalService.state(output, FoodThermalService.Storage.COLD, 123).putDouble("decay", .6)
        val input = ItemStack(Items.CARROT)
        FoodThermalService.state(input, FoodThermalService.Storage.COLD, 500).putDouble("decay", .6)
        val before = output.tag!!.copy()
        CookingAgeApi.transfer(listOf(CookingAgeApi.Input(input, FoodThermalService.Storage.COLD)), output, 500)
        assertEquals(before, output.tag)
    }
    @Test fun `empty nonfood inputs cannot overwrite existing clocks`() {
        TestMinecraftBootstrap.bootstrap()
        val output = ItemStack(Items.BREAD)
        FoodThermalService.state(output, FoodThermalService.Storage.COLD, 123).putDouble("decay", .6)
        val before = output.tag!!.copy()
        CookingAgeApi.transfer(listOf(CookingAgeApi.Input(ItemStack(Items.STICK), FoodThermalService.Storage.AMBIENT)), output, 500)
        assertEquals(before, output.tag)
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val tests = CookingAgeApiTest()
            tests.`multi input transfer settles actual storage and never averages age`()
            tests.`existing older result cannot be rejuvenated by fresh ingredient`()
            tests.`empty nonfood inputs cannot overwrite existing clocks`()
            tests.`equal decay cannot rebase an existing output clock`()
            println("CookingAgeApiTest: 4 PASS (Minecraft bootstrap JVM tests; not native cooking acceptance)")
        }
    }
}
