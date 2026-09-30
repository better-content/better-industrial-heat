package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncConfig
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.bettercontent.betterindustrialheat.api.IHeatStorage
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
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

class BoilerHeaterBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(HeatSyncRegistries.BOILER_HEATER_BLOCK_ENTITY.get(), pos, state), HeatBlockEntity,
    IHaveGoggleInformation {
    private var heat = AMBIENT_HEAT
    private var lastDeliveredStrength = 0
    private var active = false
    private var lastComparatorOutput = 0
    private var heatCapability: LazyOptional<IHeatStorage> = LazyOptional.of { this }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        heat = tag.getFloat(HEAT_KEY).coerceIn(0f, maxHeat())
        lastDeliveredStrength = tag.getInt(STRENGTH_KEY).coerceIn(0, 3)
        active = tag.getBoolean(ACTIVE_KEY)
        lastComparatorOutput = BoilerHeaterLogic.comparatorOutput(heat, maxHeat())
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putFloat(HEAT_KEY, heat)
        tag.putInt(STRENGTH_KEY, lastDeliveredStrength)
        tag.putBoolean(ACTIVE_KEY, active)
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()
    override fun handleUpdateTag(tag: CompoundTag) = load(tag)
    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (!remove && cap === HeatCapabilities.HEAT && side != Direction.UP) return heatCapability.cast()
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
    override fun maxHeat(): Float = HeatSyncConfig.boilerHeaterMaxHeat()
    override fun canConnect(side: Direction?): Boolean = side != Direction.UP

    override fun addHeat(heat: Float) = setHeat(this.heat + heat)

    override fun setHeat(heat: Float) {
        val clamped = heat.coerceIn(0f, maxHeat())
        if (this.heat == clamped) return
        this.heat = clamped
        setChanged()
        updateComparatorIfNeeded()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        HeatBlockEntity.addToolTips(this, tooltip)
        CreateLang.translate("tooltip.better_industrial_heat.boiler_heater_strength", lastDeliveredStrength)
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip)
        return true
    }

    fun advertisedStrength(): Float = lastDeliveredStrength.toFloat()
    fun deliveredStrength(): Int = lastDeliveredStrength
    fun isActive(): Boolean = active
    fun comparatorOutput(): Int = BoilerHeaterLogic.comparatorOutput(heat, maxHeat())

    private fun serverTick() {
        val tank = level?.getBlockEntity(blockPos.above()) as? FluidTankBlockEntity
        val controller = tank?.controllerBE
        val boilerRequestsHeat = controller?.boiler?.isActive == true
        val delivery = BoilerHeaterLogic.deliver(heat, boilerRequestsHeat, HeatSyncConfig.boilerHeaterSettings())
        val previousStrength = lastDeliveredStrength
        val previousActive = active

        if (delivery.consumed > 0f) setHeat(delivery.remaining)
        lastDeliveredStrength = delivery.strength
        active = delivery.strength > 0

        if ((level?.gameTime ?: 0L) % TRANSFER_INTERVAL == 0L) HeatBlockEntity.transferAround(this)
        if (previousStrength != lastDeliveredStrength) tank?.updateBoilerTemperature()
        if (previousStrength != lastDeliveredStrength || previousActive != active) {
            updatePresentationState()
            HeatBlockEntity.trySync(this)
            setChanged()
        }
    }

    private fun updatePresentationState() {
        val currentLevel = level ?: return
        val state = blockState
        if (state.hasProperty(BoilerHeaterBlock.ACTIVE) && state.getValue(BoilerHeaterBlock.ACTIVE) != active) {
            currentLevel.setBlock(blockPos, state.setValue(BoilerHeaterBlock.ACTIVE, active), 3)
        }
    }

    private fun updateComparatorIfNeeded() {
        val output = comparatorOutput()
        if (output == lastComparatorOutput) return
        lastComparatorOutput = output
        level?.updateNeighbourForOutputSignal(blockPos, blockState.block)
    }

    companion object {
        const val AMBIENT_HEAT = 100f
        private const val TRANSFER_INTERVAL = 4L
        private const val HEAT_KEY = "Heat"
        private const val STRENGTH_KEY = "LastDeliveredStrength"
        private const val ACTIVE_KEY = "Active"

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: BoilerHeaterBlockEntity) {
            if (!level.isClientSide) blockEntity.serverTick()
        }
    }
}
