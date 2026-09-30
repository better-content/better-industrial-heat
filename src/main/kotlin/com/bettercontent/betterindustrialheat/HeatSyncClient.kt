package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.compat.ponder.HeatSyncPonderPlugin
import com.bettercontent.betterindustrialheat.food.FoodThermalService
import net.createmod.ponder.foundation.PonderIndex
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.client.event.RegisterColorHandlersEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.registries.ForgeRegistries

object HeatSyncClient {
    fun register(modBus: IEventBus) {
        modBus.addListener(::onClientSetup)
        modBus.addListener(::onItemColors)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            PonderIndex.addPlugin(HeatSyncPonderPlugin())
        }
    }

    private fun onItemColors(event: RegisterColorHandlersEvent.Item) {
        val foods = ForgeRegistries.ITEMS.values.filter { it.defaultInstance.isEdible }.toTypedArray()
        event.register({ stack, tintIndex -> if (tintIndex == 0) FoodThermalService.itemTint(stack) else 0xFFFFFF }, *foods)
    }
}
