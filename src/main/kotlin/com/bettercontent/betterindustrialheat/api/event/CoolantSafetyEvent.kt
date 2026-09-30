package com.bettercontent.betterindustrialheat.api.event

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event

/** Observes excess network heat and its subsequent safe conversion through coolant. */
class CoolantSafetyEvent(
    val player: ServerPlayer,
    exchangerPosition: BlockPos,
    val episodeId: String,
    val stage: Stage,
) : Event() {
    val exchangerPosition: BlockPos = exchangerPosition.immutable()

    enum class Stage { EXCESS_HEAT_OBSERVED, SAFE_AFTER_COOLANT_EXCHANGE }
}
