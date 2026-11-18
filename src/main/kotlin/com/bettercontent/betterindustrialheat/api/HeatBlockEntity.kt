package com.bettercontent.betterindustrialheat.api

import com.simibubi.create.foundation.utility.CreateLang
import com.bettercontent.betterindustrialheat.content.heat.ThermalSurfaceMachine
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntity
import kotlin.math.min

interface HeatBlockEntity : IHeatStorage {
    override fun getHeat(): Float
    fun maxHeat(): Float
    fun addHeat(heat: Float)
    override fun setHeat(heat: Float)

    override fun getMaxHeat(): Float = maxHeat()

    override fun addHeat(amount: Float, simulate: Boolean): Float {
        if (amount <= 0f) return 0f
        val accepted = min(amount, (maxHeat() - getHeat()).coerceAtLeast(0f))
        if (!simulate && accepted > 0f) {
            addHeat(accepted)
        }
        return accepted
    }

    override fun extractHeat(amount: Float, simulate: Boolean): Float {
        if (amount <= 0f) return 0f
        val extracted = min(amount, getHeat().coerceAtLeast(0f))
        if (!simulate && extracted > 0f) {
            setHeat(getHeat() - extracted)
        }
        return extracted
    }

    override fun canConnect(side: Direction?): Boolean = true
    override fun canAdd(side: Direction?): Boolean = canConnect(side)
    override fun canExtract(side: Direction?): Boolean = canConnect(side)

    companion object {
        private const val DEFAULT_TRANSFER_RATE = 80f
        private const val OVERHEAT_LIMIT_FACTOR = 1.25f

        @JvmStatic
        fun transferAround(source: HeatBlockEntity) = transferAround(source, true)

        /** Machines with independent tick changes publish their final state once after exchange. */
        internal fun transferAround(source: HeatBlockEntity, syncSource: Boolean) {
            val sourceEntity = source as? BlockEntity ?: return
            val level = sourceEntity.level ?: return
            transferAround(source, level::isLoaded, level::getBlockEntity, ::trySync, syncSource)
        }

        /** Production exchange with injectable world access and sync for native behavioral tests. */
        internal fun transferAround(
            source: HeatBlockEntity,
            isLoaded: (net.minecraft.core.BlockPos) -> Boolean,
            getNeighbor: (net.minecraft.core.BlockPos) -> BlockEntity?,
            sync: (BlockEntity) -> Unit,
            syncSource: Boolean = true,
        ) {
            val sourceEntity = source as? BlockEntity ?: return
            if (sourceEntity.level == null) return
            val initialHeat = source.getHeat()
            for (direction in Direction.values()) {
                // A one-way input may receive heat from a hotter neighbour even when
                // it cannot extract on this side; direction is checked after the gradient.
                if (!source.canExtract(direction) && !source.canAdd(direction)) continue
                val neighborPos = sourceEntity.blockPos.relative(direction)
                if (!isLoaded(neighborPos)) continue
                val targetEntity = getNeighbor(neighborPos) ?: continue
                // IH peers run this same settlement loop; settle their edge once, but
                // do not skip an external heat capability which never runs our loop.
                if (targetEntity is HeatBlockEntity &&
                    sourceEntity.blockPos.asLong() >= targetEntity.blockPos.asLong()) continue
                targetEntity.getCapability(HeatCapabilities.HEAT, direction.opposite).ifPresent { target ->
                    val targetHeatBefore = target.getHeat()
                    if (source.getHeat() > target.getHeat()) {
                        if (!source.canExtract(direction) || !target.canAdd(direction.opposite)) return@ifPresent
                        val delta = source.getHeat() - target.getHeat()
                        val transfer = min(DEFAULT_TRANSFER_RATE, delta / transferResistance(source, target))
                        settleHeat(source, target, transfer)
                        if (target.getHeat() != targetHeatBefore) sync(targetEntity)
                        return@ifPresent
                    }

                    if (target.getHeat() <= source.getHeat()) return@ifPresent
                    if (!target.canExtract(direction.opposite) || !source.canAdd(direction)) return@ifPresent
                    val delta = target.getHeat() - source.getHeat()
                    val transfer = min(DEFAULT_TRANSFER_RATE, delta / transferResistance(target, source))
                    settleHeat(target, source, transfer)
                    if (target.getHeat() != targetHeatBefore) sync(targetEntity)
                }
            }
            if (syncSource && source.getHeat() != initialHeat) sync(sourceEntity)
        }

        /** Extract first: a source such as a pipe may have a nonzero native heat floor.
         * Crediting the receiver before a real extraction creates heat when that floor is reached.
         * The remainder is returned to the sender if the receiver accepts less than simulated.
         */
        internal fun settleHeat(from: IHeatStorage, into: IHeatStorage, requested: Float): Float {
            if (!requested.isFinite() || requested <= 0f) return 0f
            val available = from.extractHeat(requested, true).coerceIn(0f, requested)
            val room = into.addHeat(available, true).coerceIn(0f, available)
            val extracted = from.extractHeat(room, false).coerceIn(0f, room)
            if (extracted <= 0f) return 0f
            val accepted = into.addHeat(extracted, false).coerceIn(0f, extracted)
            if (accepted < extracted) from.addHeat(extracted - accepted, false)
            return accepted
        }

        // Keep every existing non-variant transfer denominator unchanged. A rated surface
        // contributes on both sides of an edge, but cannot bypass the 80-unit native cap.
        private fun transferResistance(from: IHeatStorage, into: IHeatStorage): Float {
            val receiving = into.getThermalResistance().coerceAtLeast(0f)
            val rated = (from is ThermalSurfaceMachine && from.transferRatio() != 1f) ||
                (into is ThermalSurfaceMachine && into.transferRatio() != 1f)
            if (!rated) return 1f + receiving
            val outgoing = from.getThermalResistance().coerceAtLeast(0f)
            return 1f + (outgoing + receiving) / 2f
        }

        @JvmStatic
        fun trySync(entity: BlockEntity) {
            val level = entity.level ?: return
            if (level.isClientSide) return
            level.sendBlockUpdated(entity.blockPos, entity.blockState, entity.blockState, 3)
        }

        @JvmStatic
        fun handleOverheat(source: HeatBlockEntity) {
            if (source.getHeat() > source.maxHeat() * OVERHEAT_LIMIT_FACTOR) {
                source.setHeat(source.maxHeat())
            }
        }

        @JvmStatic
        fun addToolTips(storage: IHeatStorage, tooltip: MutableList<Component>) {
            CreateLang.translate("tooltip.better_industrial_heat.heat", storage.getHeat().toInt(), storage.getMaxHeat().toInt())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip)
        }
    }
}
