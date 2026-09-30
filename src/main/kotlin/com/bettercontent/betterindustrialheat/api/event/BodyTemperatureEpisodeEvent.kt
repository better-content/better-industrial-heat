package com.bettercontent.betterindustrialheat.api.event

import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event

/** Observes entry into or recovery from an authoritative body-temperature band. */
class BodyTemperatureEpisodeEvent(
    val player: ServerPlayer,
    val episodeId: String,
    val stage: Stage,
) : Event() {
    enum class Stage { STRESSED_COLD, STRESSED_HOT, COMFORT_RESTORED }
}
