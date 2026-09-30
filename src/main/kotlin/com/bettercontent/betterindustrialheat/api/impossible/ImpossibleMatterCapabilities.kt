package com.bettercontent.betterindustrialheat.api.impossible

import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken

object ImpossibleMatterCapabilities {
    @JvmField
    val SOURCE: Capability<IImpossibleMatterSource> =
        CapabilityManager.get(object : CapabilityToken<IImpossibleMatterSource>() {})
}
