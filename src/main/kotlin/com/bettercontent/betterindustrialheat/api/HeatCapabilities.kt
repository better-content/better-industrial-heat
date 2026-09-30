package com.bettercontent.betterindustrialheat.api

import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken

object HeatCapabilities {
    val HEAT: Capability<IHeatStorage> = CapabilityManager.get(object : CapabilityToken<IHeatStorage>() {})
}
