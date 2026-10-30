package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.api.CookingAgeApi
import net.minecraft.nbt.ListTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.test.*

class CookingAgeCarrierTest {
    private val adapter = object : CookingAgeApi.Carrier {
        override fun handles(stack: ItemStack) = stack.`is`(Items.PAPER)
        override fun read(stack: ItemStack): List<ItemStack> {
            if (stack.tag?.getBoolean("cycle") == true) return listOf(stack)
            val children = stack.tag?.getList("paid", 10) ?: return emptyList()
            return (0 until children.size).map { ItemStack.of(children.getCompound(it)) }
        }
        override fun write(stack: ItemStack, children: List<ItemStack>) {
            val stored = ListTag()
            children.forEach { stored.add(it.save(net.minecraft.nbt.CompoundTag())) }
            stack.orCreateTag.put("paid", stored)
        }
    }
    private fun reset() = com.bettercontent.betterindustrialheat.api.CarrierTestScope.reset()
    private fun withCarrier(test: () -> Unit) {
        TestMinecraftBootstrap.bootstrap()
        reset()
        try {
            CookingAgeApi.registerCarrier(ResourceLocation("test", "paid_batch"), adapter)
            test()
        } finally { reset() }
    }
    private fun carrier(child: ItemStack): ItemStack = ItemStack(Items.PAPER).also { adapter.write(it, listOf(child)) }
    private fun state(stack: ItemStack) = stack.tag!!.getCompound("better_industrial_heat_food")

    @Test fun `no carrier provider retains existing nonfood behavior`() {
        TestMinecraftBootstrap.bootstrap(); reset()
        try {
            val paper = ItemStack(Items.PAPER)
            assertFalse(CookingAgeApi.isCarrier(paper))
            assertSame(paper, FoodThermalService.tick(paper, FoodThermalService.Storage.AMBIENT, 500))
            assertNull(paper.tag)
        } finally { reset() }
    }
    @Test fun `paid inputs age inside nonedible carrier and final output receives oldest child`() = withCarrier {
        val old = ItemStack(Items.BEEF)
        FoodThermalService.state(old, FoodThermalService.Storage.AMBIENT, 0).putDouble("decay", .7)
        val original = old.tag!!.copy()
        val batch = ItemStack(Items.PAPER)
        CookingAgeApi.transfer(listOf(CookingAgeApi.Input(old, FoodThermalService.Storage.AMBIENT)), batch, 0)
        assertFalse(batch.isEdible)
        assertTrue(CookingAgeApi.isCarrier(batch))
        assertFalse(batch.tag!!.contains("better_industrial_heat_food"))
        FoodThermalService.tick(batch, FoodThermalService.Storage.AMBIENT, 2400)
        assertEquals(.8, state(adapter.read(batch).single()).getDouble("decay"), .00001)
        val output = ItemStack(Items.COOKED_BEEF, 8)
        CookingAgeApi.transfer(listOf(CookingAgeApi.Input(batch, FoodThermalService.Storage.AMBIENT)), output, 2400)
        assertEquals(.8, state(output).getDouble("decay"), .00001)
        assertEquals(original, old.tag)
    }
    @Test fun `nested carrier retains rotten ingredient identity without converting children`() = withCarrier {
        val food = ItemStack(Items.BEEF)
        FoodThermalService.state(food, FoodThermalService.Storage.AMBIENT, 0).putDouble("decay", 1.5)
        val nested = carrier(carrier(food))
        FoodThermalService.tick(nested, FoodThermalService.Storage.COLD, 240000)
        val leaf = adapter.read(adapter.read(nested).single()).single()
        assertTrue(leaf.`is`(Items.BEEF))
        assertEquals(2.0, state(leaf).getDouble("decay"))
        assertEquals(.1, state(leaf).getDouble("rate"))
    }
    @Test fun `cycles fail before changing original payload`() = withCarrier {
        val cyclic = ItemStack(Items.PAPER).also { it.orCreateTag.putBoolean("cycle", true) }
        val before = cyclic.tag!!.copy()
        assertFailsWith<IllegalArgumentException> { FoodThermalService.settle(cyclic, FoodThermalService.Storage.AMBIENT, 100) }
        assertEquals(before, cyclic.tag)
    }
    @Test fun `depth32 accepts and depth33 rejects without publication`() = withCarrier {
        var tree = ItemStack(Items.BEEF)
        repeat(32) { tree = carrier(tree) }
        assertTrue(CookingAgeApi.settleCarrier(tree, FoodThermalService.Storage.AMBIENT, 100))
        val tooDeep = carrier(tree)
        val before = tooDeep.tag!!.copy()
        assertFailsWith<IllegalArgumentException> { CookingAgeApi.settleCarrier(tooDeep, FoodThermalService.Storage.AMBIENT, 200) }
        assertEquals(before, tooDeep.tag)
    }
    @Test fun `nested login rebase preserves active time only aging`() = withCarrier {
        val food = ItemStack(Items.BEEF)
        FoodThermalService.state(food, FoodThermalService.Storage.AMBIENT, 0).putDouble("decay", .4)
        val batch = carrier(carrier(food))
        assertTrue(CookingAgeApi.rebaseCarrier(batch, 100000))
        FoodThermalService.settle(batch, FoodThermalService.Storage.AMBIENT, 100000)
        val leaf = adapter.read(adapter.read(batch).single()).single()
        assertEquals(.4, state(leaf).getDouble("decay"))
        assertEquals(100000, state(leaf).getLong("last_time"))
    }
    @Test fun `duplicate late overlap and edible carriers fail closed`() = withCarrier {
        assertFailsWith<IllegalArgumentException> { CookingAgeApi.registerCarrier(ResourceLocation("test", "paid_batch"), adapter) }
        CookingAgeApi.registerCarrier(ResourceLocation("test", "overlap"), adapter)
        assertFailsWith<IllegalStateException> { CookingAgeApi.isCarrier(ItemStack(Items.PAPER)) }
        assertFailsWith<IllegalStateException> { CookingAgeApi.registerCarrier(ResourceLocation("test", "late"), adapter) }
        reset()
        CookingAgeApi.registerCarrier(ResourceLocation("test", "edible"), object : CookingAgeApi.Carrier {
            override fun handles(stack: ItemStack) = stack.isEdible
            override fun read(stack: ItemStack) = emptyList<ItemStack>()
            override fun write(stack: ItemStack, children: List<ItemStack>) {}
        })
        assertFailsWith<IllegalArgumentException> { CookingAgeApi.settleCarrier(ItemStack(Items.BEEF), FoodThermalService.Storage.AMBIENT, 0) }
    }
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val tests = CookingAgeCarrierTest()
            tests.`no carrier provider retains existing nonfood behavior`()
            tests.`paid inputs age inside nonedible carrier and final output receives oldest child`()
            tests.`nested carrier retains rotten ingredient identity without converting children`()
            tests.`cycles fail before changing original payload`()
            tests.`depth32 accepts and depth33 rejects without publication`()
            tests.`nested login rebase preserves active time only aging`()
            tests.`duplicate late overlap and edible carriers fail closed`()
            println("CookingAgeCarrierTest: 7 PASS (bootstrap JVM, not native acceptance)")
        }
    }
}
