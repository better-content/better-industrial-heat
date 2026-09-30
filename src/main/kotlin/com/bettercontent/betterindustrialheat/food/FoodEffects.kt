package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.illusivesoulworks.diet.platform.Services
import dev.ghen.thirst.foundation.common.capability.ModCapabilities
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import net.minecraftforge.fml.ModList

object FoodEffects {
    val EFFECTS: DeferredRegister<MobEffect> = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, HeatSyncMod.MOD_ID)
    val THIRST: RegistryObject<MobEffect> = EFFECTS.register("thirst") { SystemDrainEffect(0x5AA9E6, false) }
    val MALNOURISHMENT: RegistryObject<MobEffect> = EFFECTS.register("malnourishment") { SystemDrainEffect(0x9B875D, true) }
}

object FoodItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, HeatSyncMod.MOD_ID)
    val SPOILED_MEAT: RegistryObject<Item> = ITEMS.register("spoiled_meat") { Item(Item.Properties().food(FoodProperties.Builder().nutrition(1).saturationMod(0.1f).meat().build())) }
    val SPOILED_PRODUCE: RegistryObject<Item> = ITEMS.register("spoiled_produce") { Item(Item.Properties().food(FoodProperties.Builder().nutrition(1).saturationMod(0.1f).build())) }
    internal val DRIED_FOOD_SOURCES = listOf(
        DriedFoodSource("dried_beef", Items.BEEF),
        DriedFoodSource("dried_porkchop", Items.PORKCHOP),
        DriedFoodSource("dried_chicken", Items.CHICKEN),
        DriedFoodSource("dried_mutton", Items.MUTTON),
        DriedFoodSource("dried_rabbit", Items.RABBIT),
        DriedFoodSource("dried_cod", Items.COD),
        DriedFoodSource("dried_salmon", Items.SALMON),
    )

    @JvmStatic
    fun isDriedFoodSource(stack: net.minecraft.world.item.ItemStack): Boolean =
        DRIED_FOOD_SOURCES.any { stack.`is`(it.rawFood) }

    private val driedFoodItems = DRIED_FOOD_SOURCES.associate { source ->
        source.outputId to ITEMS.register(source.outputId) {
            Item(Item.Properties().food(driedFoodProperties(source.rawFood)))
        }
    }

    val DRIED_BEEF: RegistryObject<Item> get() = driedFoodItems.getValue("dried_beef")
    val DRIED_PORKCHOP: RegistryObject<Item> get() = driedFoodItems.getValue("dried_porkchop")
    val DRIED_CHICKEN: RegistryObject<Item> get() = driedFoodItems.getValue("dried_chicken")
    val DRIED_MUTTON: RegistryObject<Item> get() = driedFoodItems.getValue("dried_mutton")
    val DRIED_RABBIT: RegistryObject<Item> get() = driedFoodItems.getValue("dried_rabbit")
    val DRIED_COD: RegistryObject<Item> get() = driedFoodItems.getValue("dried_cod")
    val DRIED_SALMON: RegistryObject<Item> get() = driedFoodItems.getValue("dried_salmon")

    internal fun driedFoodProperties(rawFood: Item): FoodProperties = requireNotNull(rawFood.foodProperties) {
        "Dried food source ${rawFood.descriptionId} must be edible"
    }
}

internal data class DriedFoodSource(val outputId: String, val rawFood: Item)

private class SystemDrainEffect(color: Int, private val diet: Boolean) : MobEffect(MobEffectCategory.HARMFUL, color) {
    override fun isDurationEffectTick(duration: Int, amplifier: Int): Boolean = duration % 20 == 0
    override fun applyEffectTick(entity: LivingEntity, amplifier: Int) {
        val player = entity as? ServerPlayer ?: return
        if (diet) DietBridge.drain(player, amplifier) else ThirstBridge.drain(player, amplifier)
    }
}

private object ThirstBridge {
    private val pendingLoss = mutableMapOf<java.util.UUID, Double>()

    fun drain(player: ServerPlayer, amplifier: Int) {
        if (!ModList.get().isLoaded("thirst")) return
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent { thirst ->
            val now = thirst.thirst
            val quenched = thirst.quenched
            val loss = doubleArrayOf(2.0, 4.0, 6.0)[amplifier.coerceIn(0, 2)] / 60.0
            val accumulated = (pendingLoss[player.uuid] ?: 0.0) + loss
            val whole = accumulated.toInt()
            pendingLoss[player.uuid] = accumulated - whole
            val next = (now - whole).coerceAtLeast(0)
            thirst.thirst = next
            thirst.quenched = quenched.coerceAtMost(next)
            thirst.updateThirstData(player)
        }
    }
}

private object DietBridge {
    fun drain(player: ServerPlayer, amplifier: Int) {
        if (!ModList.get().isLoaded("diet")) return
        Services.CAPABILITY.get(player).ifPresent { tracker ->
            val total = doubleArrayOf(0.02, 0.06, 0.10)[amplifier.coerceIn(0, 2)] / 60.0
            tracker.values.forEach { (key, value) -> tracker.setValue(key, (value - total.toFloat()).coerceAtLeast(0f)) }
            tracker.sync()
        }
    }
}
