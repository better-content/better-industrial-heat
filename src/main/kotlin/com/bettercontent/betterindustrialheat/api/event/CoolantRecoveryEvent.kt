package com.bettercontent.betterindustrialheat.api.event

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.eventbus.api.Event
import java.util.UUID

/** Committed coolant recovery credited to the persisted device owner, including while offline. */
class CoolantRecoveryEvent(val level: ServerLevel, val owner: UUID, position: BlockPos, val episodeId: String) : Event() {
    val position: BlockPos = position.immutable()
}
