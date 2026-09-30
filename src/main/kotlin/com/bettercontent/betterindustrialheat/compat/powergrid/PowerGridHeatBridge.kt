package com.bettercontent.betterindustrialheat.compat.powergrid

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.bettercontent.betterindustrialheat.api.IHeatStorage
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.IEventBus

object PowerGridHeatBridge {
    const val MOD_ID: String = "powergrid"
    private val CAPABILITY_ID = ResourceLocation(HeatSyncMod.MOD_ID, "powergrid_heat")

    fun initialize(eventBus: IEventBus) {
        eventBus.addGenericListener(BlockEntity::class.java, ::attachBlockEntityCapabilities)
        HeatSyncMod.LOGGER.info("Enabled Create: Power Grid heat bridge hooks")
    }

    private fun attachBlockEntityCapabilities(event: AttachCapabilitiesEvent<BlockEntity>) {
        val blockEntity = event.`object`
        if (!blockEntity.javaClass.name.startsWith("org.patryk3211.powergrid.")) {
            return
        }
        event.addCapability(CAPABILITY_ID, Provider(blockEntity))
    }

    private class Provider(blockEntity: BlockEntity) : ICapabilityProvider {
        private val heat = LazyOptional.of<IHeatStorage> { PowerGridHeatStorage(blockEntity) }

        override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
            if (cap === HeatCapabilities.HEAT) {
                return heat.cast()
            }
            return LazyOptional.empty()
        }
    }
}
