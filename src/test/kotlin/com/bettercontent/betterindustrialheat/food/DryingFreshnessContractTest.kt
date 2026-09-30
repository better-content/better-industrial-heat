package com.bettercontent.betterindustrialheat.food

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class DryingFreshnessContractTest {
    @Test
    fun `rack accepts fresh inputs but rejects stale and already stale catalogued foods`() {
        TestMinecraftBootstrap.bootstrap()
        val fresh = ItemStack(Items.BEEF)
        FoodThermalService.state(fresh, 295.15, 0L)
        assertTrue(FoodThermalService.canDryAt(fresh, 23_999L))
        assertFalse(FoodThermalService.canDryAt(fresh, 24_000L))

        val alreadyStale = ItemStack(Items.COD)
        FoodThermalService.state(alreadyStale, 295.15, 100L).putDouble("decay", 1.0)
        assertFalse(FoodThermalService.canDryAt(alreadyStale, 100L))
    }

    @Test
    fun `drying freshness settles preservation without mutating its input`() {
        TestMinecraftBootstrap.bootstrap()
        val refrigerated = ItemStack(Items.SALMON)
        FoodThermalService.state(refrigerated, 278.15, 0L)
        val before = refrigerated.tag!!.copy()

        assertTrue(FoodThermalService.canDryAt(refrigerated, 24_000L))
        assertEquals(before, refrigerated.tag)

        val frozen = ItemStack(Items.PORKCHOP)
        FoodThermalService.state(frozen, 268.15, 0L)
        assertTrue(FoodThermalService.canDryAt(frozen, 240_000L))
    }

    @Test
    fun `Hexerei rack mixin checks freshness at its pinned recipe match boundary`() {
        val mixins = Files.readString(Path.of("src/main/resources/better_industrial_heat.mixins.json"))
        val mixin = Files.readString(
            Path.of("src/main/java/com/bettercontent/betterindustrialheat/mixin/hexerei/DryingRackRecipeMixin.java"),
        )
        assertTrue(mixins.contains("hexerei.DryingRackRecipeMixin"))
        assertTrue(mixin.contains("@Inject("))
        assertTrue(mixin.contains("matches(Lnet/minecraft/world/SimpleContainer;Lnet/minecraft/world/level/Level;)Z"))
        assertTrue(mixin.contains("FoodThermalService.canDryAt(input, level.getGameTime())"))
        assertTrue(mixin.contains("FoodThermalService.carryDryingState"))
    }
}
