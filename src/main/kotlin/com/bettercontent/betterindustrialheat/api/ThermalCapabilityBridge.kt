package com.bettercontent.betterindustrialheat.api

import com.bettercontent.betterindustrialheat.HeatSyncMod
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.IEventBus

/** Supplies the Kelvin API for legacy Better Industrial Heat blocks while old save data is retired. */
object ThermalCapabilityBridge {
    private val id = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "thermal_body")

    fun initialize(bus: IEventBus) {
        bus.addGenericListener(BlockEntity::class.java, ::attach)
    }

    private fun attach(event: AttachCapabilitiesEvent<BlockEntity>) {
        val legacy = event.`object` as? HeatBlockEntity ?: return
        val provider = Provider(legacy)
        event.addCapability(id, provider)
        event.addListener(provider::invalidate)
    }

    private class Provider(legacy: HeatBlockEntity) : ICapabilityProvider {
        private val body = LazyOptional.of<IThermalBody> { HeatStorageThermalBody(legacy) }
        override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> =
            if (cap === ThermalCapabilities.BODY) body.cast() else LazyOptional.empty()
        fun invalidate() = body.invalidate()
    }
}
