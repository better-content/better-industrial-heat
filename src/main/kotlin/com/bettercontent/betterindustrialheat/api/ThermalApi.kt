package com.bettercontent.betterindustrialheat.api

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import java.util.Collections
import java.util.IdentityHashMap

object ThermalApi {
    /** Fairly inserts generated HU into adjacent bodies and returns the unaccepted remainder. */
    @JvmStatic
    fun distributeHeat(level: ServerLevel, pos: BlockPos, heatHU: Double): Double {
        if (!heatHU.isFinite() || heatHU <= 0.0) return 0.0
        val targets = mutableListOf<IThermalBody>()
        val seen = Collections.newSetFromMap(IdentityHashMap<IThermalBody, Boolean>())
        for (direction in Direction.values()) {
            val entity = level.getBlockEntity(pos.relative(direction)) ?: continue
            entity.getCapability(ThermalCapabilities.BODY, direction.opposite).ifPresent { body ->
                if (body.canConnect(direction.opposite) && seen.add(body)) targets += body
            }
        }
        var remaining = heatHU
        targets.forEachIndexed { index, target ->
            val accepted = target.insertHeatHU(remaining / (targets.size - index), false)
            remaining -= accepted
        }
        return remaining.coerceAtLeast(0.0)
    }
}
