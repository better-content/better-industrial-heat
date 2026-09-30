package com.bettercontent.betterindustrialheat.api

import com.simibubi.create.foundation.utility.CreateLang
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
        fun transferAround(source: HeatBlockEntity) {
            val sourceEntity = source as? BlockEntity ?: return
            val level = sourceEntity.level ?: return
            for (direction in Direction.values()) {
                if (!source.canExtract(direction)) continue
                val targetEntity = level.getBlockEntity(sourceEntity.blockPos.relative(direction)) ?: continue
                // Each adjacent pair is settled once per interval. Without a
                // stable ownership rule, both pipes immediately process the same
                // edge and traversal order changes the result.
                if (sourceEntity.blockPos.asLong() >= targetEntity.blockPos.asLong()) continue
                targetEntity.getCapability(HeatCapabilities.HEAT, direction.opposite).ifPresent { target ->
                    if (source.getHeat() > target.getHeat()) {
                        if (!target.canAdd(direction.opposite)) return@ifPresent
                        val delta = source.getHeat() - target.getHeat()
                        val transfer = min(DEFAULT_TRANSFER_RATE, delta / (1f + target.getThermalResistance().coerceAtLeast(0f)))
                        val accepted = target.addHeat(transfer, true)
                        val extracted = source.extractHeat(accepted, true)
                        val moved = target.addHeat(extracted, false)
                        source.extractHeat(moved, false)
                        if (moved > 0f) {
                            trySync(targetEntity)
                        }
                        return@ifPresent
                    }

                    if (target.getHeat() <= source.getHeat()) return@ifPresent
                    if (!target.canExtract(direction.opposite) || !source.canAdd(direction)) return@ifPresent
                    val delta = target.getHeat() - source.getHeat()
                    val transfer = min(DEFAULT_TRANSFER_RATE, delta / (1f + source.getThermalResistance().coerceAtLeast(0f)))
                    val accepted = source.addHeat(transfer, true)
                    val extracted = target.extractHeat(accepted, true)
                    val moved = source.addHeat(extracted, false)
                    target.extractHeat(moved, false)
                    if (moved > 0f) {
                        trySync(targetEntity)
                    }
                }
            }
            trySync(sourceEntity)
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
