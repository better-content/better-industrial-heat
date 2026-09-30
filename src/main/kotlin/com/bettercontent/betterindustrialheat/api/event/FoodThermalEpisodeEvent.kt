package com.bettercontent.betterindustrialheat.api.event

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event

/** Observes a frozen-food rejection and later successful use of the corrected ordinary item. */
class FoodThermalEpisodeEvent(
    val player: ServerPlayer,
    val itemId: ResourceLocation,
    val episodeId: String,
    val stage: Stage,
) : Event() {
    enum class Stage { FROZEN_USE_REJECTED, FRESH_USE_FINISHED }
}
