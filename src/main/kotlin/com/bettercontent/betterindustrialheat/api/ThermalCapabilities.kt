package com.bettercontent.betterindustrialheat.api

import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken

object ThermalCapabilities {
    @JvmField
    val BODY: Capability<IThermalBody> = CapabilityManager.get(object : CapabilityToken<IThermalBody>() {})
}
