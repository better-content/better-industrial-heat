package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncConfig
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
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.ItemStackHandler

class ThermalFireboxBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : BlockEntity(HeatSyncRegistries.THERMAL_FIREBOX_BLOCK_ENTITY.get(), pos, state), HeatBlockEntity, IHaveGoggleInformation {
    private var heat = AMBIENT_HEAT
    private var burnTicks = 0
    private val fuel = object : ItemStackHandler(1) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean =
            ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0

        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }
    private var heatCapability: LazyOptional<IHeatStorage> = LazyOptional.of { this }
    private var itemCapability: LazyOptional<ItemStackHandler> = LazyOptional.of { fuel }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        heat = tag.getFloat(HEAT_KEY).coerceIn(0f, maxHeat())
        burnTicks = tag.getInt(BURN_KEY).coerceAtLeast(0)
        fuel.deserializeNBT(tag.getCompound(FUEL_KEY))
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putFloat(HEAT_KEY, heat)
        tag.putInt(BURN_KEY, burnTicks)
        tag.put(FUEL_KEY, fuel.serializeNBT())
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()
    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> = when {
        !remove && cap === HeatCapabilities.HEAT -> heatCapability.cast()
        !remove && cap === ForgeCapabilities.ITEM_HANDLER -> itemCapability.cast()
        else -> super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        heatCapability.invalidate()
        itemCapability.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        heatCapability = LazyOptional.of { this }
        itemCapability = LazyOptional.of { fuel }
    }

    override fun getHeat(): Float = heat
    override fun maxHeat(): Float = MAX_HEAT

    override fun addHeat(heat: Float) {
        this.heat = (this.heat + heat).coerceIn(0f, maxHeat())
        setChanged()
    }

    override fun setHeat(heat: Float) {
        this.heat = heat.coerceIn(0f, maxHeat())
        setChanged()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        HeatBlockEntity.addToolTips(this, tooltip)
        return true
    }

    fun insertFuel(stack: ItemStack, simulate: Boolean = false): ItemStack = fuel.insertItem(0, stack, simulate)

    fun extractFuel(simulate: Boolean = false): ItemStack = fuel.extractItem(0, fuel.getSlotLimit(0), simulate)

    fun fuelInventory(): Container = SimpleContainer(1).also { it.setItem(0, fuel.getStackInSlot(0).copy()) }

    private fun consumeFuel(): Boolean {
        val stack = fuel.getStackInSlot(0)
        val duration = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING)
        if (duration <= 0) return false
        val remainder = stack.craftingRemainingItem
        stack.shrink(1)
        if (stack.isEmpty && !remainder.isEmpty) fuel.setStackInSlot(0, remainder)
        burnTicks = duration
        setChanged()
        return true
    }

    private fun serverTick() {
        if (burnTicks <= 0 && heat < HeatSyncConfig.fireboxTargetHeat()) consumeFuel()
        if (burnTicks > 0) {
            burnTicks--
            addHeat(HeatSyncConfig.fireboxHeatPerTick())
        }
        HeatBlockEntity.transferAround(this)
    }

    companion object {
        private const val AMBIENT_HEAT = 100f
        private const val MAX_HEAT = 400f
        private const val HEAT_KEY = "Heat"
        private const val BURN_KEY = "BurnTicks"
        private const val FUEL_KEY = "Fuel"

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: ThermalFireboxBlockEntity) {
            if (!level.isClientSide) blockEntity.serverTick()
        }
    }
}
