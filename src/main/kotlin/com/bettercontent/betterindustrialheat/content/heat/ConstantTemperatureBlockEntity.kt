package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.bettercontent.betterindustrialheat.api.IHeatStorage
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional

class ConstantTemperatureBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(HeatSyncRegistries.CONSTANT_TEMPERATURE_BLOCK_ENTITY.get(), pos, state), HeatBlockEntity,
    IHaveGoggleInformation {
    private var heat: Float = 0f
    private var heatCapability: LazyOptional<IHeatStorage> = LazyOptional.of { this }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        heat = tag.getFloat(HEAT_KEY).coerceAtLeast(ABSOLUTE_ZERO)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putFloat(HEAT_KEY, heat)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (!remove && cap === HeatCapabilities.HEAT) {
            return heatCapability.cast()
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        heatCapability.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        heatCapability = LazyOptional.of { this }
    }

    override fun getHeat(): Float = heat

    override fun addHeat(heat: Float) {
        this.heat = (this.heat + heat).coerceAtLeast(ABSOLUTE_ZERO)
        setChanged()
    }

    override fun setHeat(heat: Float) {
        this.heat = heat.coerceAtLeast(ABSOLUTE_ZERO)
        setChanged()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        HeatBlockEntity.addToolTips(this, tooltip)
        return true
    }

    override fun maxHeat(): Float {
        return TARGET_TEMPERATURE
    }

    private fun serverTick() {
        heat = targetTemperature()
        HeatBlockEntity.transferAround(this)
        HeatBlockEntity.trySync(this)
    }

    private fun targetTemperature(): Float {
        val block = blockState.block as? ConstantTemperatureBlock
        return (block?.setpoint ?: 0f).coerceAtLeast(ABSOLUTE_ZERO)
    }

    companion object {
        private const val ABSOLUTE_ZERO = 0f
        private const val HEAT_KEY = "Heat"
        private const val TARGET_TEMPERATURE = 10_000f

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: ConstantTemperatureBlockEntity) {
            if (!level.isClientSide) {
                blockEntity.serverTick()
            }
        }
    }
}
