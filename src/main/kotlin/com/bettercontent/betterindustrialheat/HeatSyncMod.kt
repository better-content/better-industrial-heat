package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.command.HeatSyncCommands
import com.bettercontent.betterindustrialheat.compat.powergrid.PowerGridHeatBridge
import com.bettercontent.betterindustrialheat.compat.pneumaticcraft.PneumaticHeatBridge
import com.bettercontent.betterindustrialheat.compat.create.CreateBoilerHeaterBridge
import com.bettercontent.betterindustrialheat.compat.BodyTemperatureEpisodes
import com.bettercontent.betterindustrialheat.food.FoodThermalService
import com.bettercontent.betterindustrialheat.api.ThermalCapabilityBridge
import com.bettercontent.betterindustrialheat.content.coolant.LiquidCoolantManager
import com.mojang.logging.LogUtils
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.slf4j.Logger

@Mod(HeatSyncMod.MOD_ID)
class HeatSyncMod(modLoadingContext: FMLJavaModLoadingContext) {
    init {
        val modBus = modLoadingContext.modEventBus
        HeatSyncRegistries.register(modBus)
        if (ModList.get().isLoaded(HeatSyncAe2Registries.MOD_ID)) {
            HeatSyncAe2Registries.register(modBus)
        }
        if (FMLEnvironment.dist.isClient) {
            HeatSyncClient.register(modBus)
        }

        modLoadingContext.registerConfig(ModConfig.Type.COMMON, HeatSyncConfig.SPEC)
        modBus.addListener(::onCommonSetup)
        MinecraftForge.EVENT_BUS.addListener(::onAddReloadListeners)
        MinecraftForge.EVENT_BUS.addListener(::onRegisterCommands)
        MinecraftForge.EVENT_BUS.register(HeatSyncPipeThermalController)
        ThermalCapabilityBridge.initialize(MinecraftForge.EVENT_BUS)

        if (ModList.get().isLoaded(COLD_SWEAT_MOD_ID)) {
            HeatSyncColdSweatBridge.initialize(MinecraftForge.EVENT_BUS)
            MinecraftForge.EVENT_BUS.register(BodyTemperatureEpisodes)
        }
        if (ModList.get().isLoaded(PowerGridHeatBridge.MOD_ID)) {
            PowerGridHeatBridge.initialize(MinecraftForge.EVENT_BUS)
        }
        if (ModList.get().isLoaded(PneumaticHeatBridge.MOD_ID)) {
            PneumaticHeatBridge.initialize(MinecraftForge.EVENT_BUS)
        }
        MinecraftForge.EVENT_BUS.register(FoodThermalService)

        LOGGER.info("Loaded mod {}", MOD_ID)
    }

    private fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(LiquidCoolantManager)
    }

    private fun onRegisterCommands(event: RegisterCommandsEvent) {
        HeatSyncCommands.register(event.dispatcher)
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork(CreateBoilerHeaterBridge::register)
    }

    companion object {
        const val MOD_ID: String = "better_industrial_heat"
        const val COLD_SWEAT_MOD_ID: String = "cold_sweat"
        val LOGGER: Logger = LogUtils.getLogger()
    }
}
