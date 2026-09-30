package com.bettercontent.betterindustrialheat.content.energy

import appeng.api.config.Actionable
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IInWorldGridNodeHost
import appeng.api.networking.IManagedGridNode
import appeng.api.util.AECableType
import com.bettercontent.betterindustrialheat.HeatSyncConfig
import com.bettercontent.betterindustrialheat.HeatSyncAe2Registries
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.bettercontent.betterindustrialheat.api.IHeatStorage
import com.bettercontent.betterindustrialheat.api.impossible.ImpossibleMatterCapabilities
import com.bettercontent.betterindustrialheat.api.impossible.IImpossibleMatterSource
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import java.util.EnumSet

class ImpossibleMatterTransducerBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(HeatSyncAe2Registries.IMPOSSIBLE_TRANSDUCER_BLOCK_ENTITY.get(), pos, state),
    HeatBlockEntity,
    IHaveGoggleInformation,
    IInWorldGridNodeHost {

    private val mainNode: IManagedGridNode = GridHelper.createManagedNode(
        this,
        IGridNodeListener { owner: ImpossibleMatterTransducerBlockEntity, _: IGridNode -> owner.setChanged() },
    )
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))
        .setIdlePowerUsage(0.0)

    private val feStorage = object : EnergyStorage(HeatSyncConfig.transducerFeCapacity()) {
        override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
            val received = super.receiveEnergy(maxReceive, simulate)
            if (!simulate && received > 0) setChanged()
            return received
        }

        override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
            val extracted = super.extractEnergy(maxExtract, simulate)
            if (!simulate && extracted > 0) setChanged()
            return extracted
        }
    }
    private var heat = AMBIENT_HEAT
    private var pendingAe = 0.0
    private var heatCapability: LazyOptional<IHeatStorage> = LazyOptional.of { this }
    private var feCapability: LazyOptional<EnergyStorage> = LazyOptional.of { feStorage }

    override fun onLoad() {
        super.onLoad()
        val currentLevel = level
        if (currentLevel != null && !currentLevel.isClientSide) {
            mainNode.setVisualRepresentation(HeatSyncAe2Registries.IMPOSSIBLE_TRANSDUCER_ITEM.get())
            mainNode.create(currentLevel, blockPos)
        }
    }

    override fun setRemoved() {
        mainNode.destroy()
        super.setRemoved()
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        heat = tag.getFloat(HEAT_KEY).coerceAtLeast(0f)
        pendingAe = tag.getDouble(PENDING_AE_KEY).coerceAtLeast(0.0)
        feStorage.receiveEnergy(tag.getInt(FE_KEY).coerceAtLeast(0), false)
        mainNode.loadFromNBT(tag.getCompound(GRID_NODE_KEY))
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putFloat(HEAT_KEY, heat)
        tag.putDouble(PENDING_AE_KEY, pendingAe)
        tag.putInt(FE_KEY, feStorage.energyStored)
        val nodeTag = CompoundTag()
        mainNode.saveToNBT(nodeTag)
        tag.put(GRID_NODE_KEY, nodeTag)
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()
    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> = when {
        !remove && cap === HeatCapabilities.HEAT -> heatCapability.cast()
        !remove && cap === ForgeCapabilities.ENERGY -> feCapability.cast()
        else -> super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        heatCapability.invalidate()
        feCapability.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        heatCapability = LazyOptional.of { this }
        feCapability = LazyOptional.of { feStorage }
    }

    override fun getGridNode(side: Direction?): IGridNode =
        mainNode.node ?: error("Impossible matter transducer grid node is not ready")
    override fun getCableConnectionType(side: Direction?): AECableType = AECableType.SMART

    override fun getHeat(): Float = heat
    override fun maxHeat(): Float = HeatSyncConfig.transducerMaxHeat()

    override fun addHeat(heat: Float) {
        this.heat = (this.heat + heat).coerceAtLeast(0f)
        setChanged()
    }

    override fun setHeat(heat: Float) {
        this.heat = heat.coerceAtLeast(0f)
        setChanged()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        HeatBlockEntity.addToolTips(this, tooltip)
        return true
    }

    private fun injectPending(): Boolean {
        if (pendingAe <= 0.0) return true
        val grid = mainNode.grid ?: return false
        val overflow = grid.energyService.injectPower(pendingAe, Actionable.MODULATE)
        pendingAe = overflow.coerceAtLeast(0.0)
        setChanged()
        return pendingAe <= MIN_AE
    }

    private fun source(): IImpossibleMatterSource? {
        val currentLevel = level ?: return null
        for (direction in Direction.values()) {
            val neighbor = currentLevel.getBlockEntity(blockPos.relative(direction)) ?: continue
            val source = neighbor.getCapability(ImpossibleMatterCapabilities.SOURCE, direction.opposite)
            if (source.isPresent) return source.orElseThrow { IllegalStateException("Impossible source disappeared") }
        }
        return null
    }

    private fun serverTick() {
        HeatBlockEntity.transferAround(this)
        if (!injectPending()) return
        val grid = mainNode.grid ?: return
        val source = source() ?: return
        val preview = source.unbind(HeatSyncConfig.transducerUnitsPerTick(), true)
        val plan = EnergyLadderMath.planUnbinding(
            availableUnits = preview.units(),
            previewAe = preview.ae(),
            previewHeat = preview.heat(),
            storedFe = feStorage.energyStored,
            fePerUnit = HeatSyncConfig.transducerFePerUnit(),
            gridDemandAe = grid.energyService.getEnergyDemand(preview.ae()),
            unitsPerTick = HeatSyncConfig.transducerUnitsPerTick(),
        )
        if (plan.units <= 0) return

        val containment = feStorage.extractEnergy(plan.containmentFe, false)
        if (containment != plan.containmentFe) return
        val actual = source.unbind(plan.units, false)
        if (actual.units() <= 0 || actual.units() > plan.units || actual.ae() > plan.ae + MIN_AE) return

        pendingAe += actual.ae()
        addHeat(actual.heat())
        injectPending()
        if (heat > maxHeat()) {
            level?.setBlockAndUpdate(blockPos, Blocks.LAVA.defaultBlockState())
        }
    }

    companion object {
        private const val AMBIENT_HEAT = 100f
        private const val MIN_AE = 0.000_001
        private const val HEAT_KEY = "Heat"
        private const val FE_KEY = "ContainmentFE"
        private const val PENDING_AE_KEY = "PendingAE"
        private const val GRID_NODE_KEY = "GridNode"

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: ImpossibleMatterTransducerBlockEntity) {
            if (!level.isClientSide) blockEntity.serverTick()
        }
    }
}
