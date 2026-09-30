package com.bettercontent.betterindustrialheat.compat.ponder

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.minecraft.resources.ResourceLocation

class HeatSyncPonderPlugin : PonderPlugin {
    override fun getModId(): String = HeatSyncMod.MOD_ID

    override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        helper.addStoryBoard(
            HeatSyncRegistries.COOLANT_EXCHANGER.id,
            HEATING_TAG,
            HeatSyncPonderScenes::coolantExchanger,
            HEAT_MANAGEMENT,
        )
    }

    override fun registerTags(helper: PonderTagRegistrationHelper<ResourceLocation>) {
        helper.registerTag(HEAT_MANAGEMENT)
            .item(HeatSyncRegistries.COOLANT_EXCHANGER.get())
            .title("Heat Management")
            .description("Heating and cooling fluids")
            .addToIndex()
            .register()

        helper.addToTag(HEAT_MANAGEMENT)
            .add(HeatSyncRegistries.COOLANT_EXCHANGER.id)
    }

    companion object {
        private val HEATING_TAG: ResourceLocation = ResourceLocation.parse("better_industrial_heat:heating")
        val HEAT_MANAGEMENT: ResourceLocation = ResourceLocation.parse("${HeatSyncMod.MOD_ID}:heat_management")
    }
}
