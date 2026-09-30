package com.bettercontent.betterindustrialheat

import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent
import net.minecraftforge.eventbus.api.IEventBus

object HeatSyncColdSweatBridge {
    fun initialize(modBus: IEventBus) {
        AmbientHeatSampling.install(ColdSweatAmbientSampler::samplePipeHeat)
        modBus.addListener(::registerBlockTemps)
        HeatSyncMod.LOGGER.info("Enabled Cold Sweat bridge hooks")
    }

    private fun registerBlockTemps(event: BlockTempRegisterEvent) {
        event.register(PipeBlockTemp())
    }
}
