package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.api.event.FoodThermalEpisodeEvent
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.registries.ForgeRegistries

/** Correlates a frozen-food rejection with later use of that ordinary item after correction. */
object FoodThermalEpisodes {
    // Keep the legacy NBT name so existing player episodes survive the migration.
    private const val EPISODE_ROOT = "HeatSyncThreadFoodEpisode"
    private const val TOKEN = "token"
    private const val ITEM = "item"

    fun onFrozenUseRejected(player: ServerPlayer, stack: ItemStack) {
        if (!isNonNeutral(stack)) return
        val itemId = ForgeRegistries.ITEMS.getKey(stack.item) ?: return
        val persisted = player.persistentData.getCompound(Player.PERSISTED_NBT_TAG)
        val prior = persisted.getCompound(EPISODE_ROOT)
        if (!shouldStartEpisode(prior.getString(TOKEN))) return
        val token = "${player.uuid}:food:${player.server.tickCount}"

        persisted.put(EPISODE_ROOT, CompoundTag().also {
            it.putString(TOKEN, token)
            it.putString(ITEM, itemId.toString())
        })
        player.persistentData.put(Player.PERSISTED_NBT_TAG, persisted)
        MinecraftForge.EVENT_BUS.post(FoodThermalEpisodeEvent(
            player, itemId, token, FoodThermalEpisodeEvent.Stage.FROZEN_USE_REJECTED,
        ))
    }

    fun onFoodUseFinished(player: ServerPlayer, stack: ItemStack) {
        if (!isAppropriate(stack)) return
        val persisted = player.persistentData.getCompound(Player.PERSISTED_NBT_TAG)
        val episode = persisted.getCompound(EPISODE_ROOT)
        if (!sameOrdinaryItem(episode.getString(ITEM), stack)) return
        val token = episode.getString(TOKEN)
        if (!validToken(token)) return
        val itemId = ForgeRegistries.ITEMS.getKey(stack.item) ?: return
        MinecraftForge.EVENT_BUS.post(FoodThermalEpisodeEvent(
            player, itemId, token, FoodThermalEpisodeEvent.Stage.FRESH_USE_FINISHED,
        ))
        persisted.remove(EPISODE_ROOT)
        player.persistentData.put(Player.PERSISTED_NBT_TAG, persisted)
    }

    internal fun isNonNeutral(stack: ItemStack): Boolean = stack.isEdible && FoodThermalService.isFrozen(stack)
    internal fun isAppropriate(stack: ItemStack): Boolean =
        stack.isEdible && !FoodThermalService.isFrozen(stack) && FoodThermalService.stage(stack) == FoodThermalService.Stage.FRESH
    internal fun sameOrdinaryItem(expectedId: String, stack: ItemStack): Boolean =
        ForgeRegistries.ITEMS.getKey(stack.item)?.toString() == expectedId
    internal fun shouldStartEpisode(existingToken: String): Boolean = !validToken(existingToken)
    private fun validToken(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value.all { it.code in 0x21..0x7e }
}
