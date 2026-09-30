package com.bettercontent.betterindustrialheat.content.coolant

import net.minecraft.resources.ResourceLocation

object CoolantExchangeLogic {
    fun computeExchange(
        fluidId: ResourceLocation,
        amount: Int,
        currentHeat: Float,
        maxHeat: Float,
        definition: LiquidCoolantDefinition,
    ): ExchangeResult? {
        return when {
            definition.matchesCold(fluidId) -> {
                val required = definition.heatRequired(amount)
                if (currentHeat < required) {
                    null
                } else {
                    ExchangeResult(definition.hotFluid, currentHeat - required)
                }
            }

            definition.matchesHot(fluidId) -> {
                val released = definition.coolingRequired(amount)
                if (currentHeat + released > maxHeat) {
                    null
                } else {
                    ExchangeResult(definition.coldFluid, currentHeat + released)
                }
            }

            else -> null
        }
    }

    data class ExchangeResult(
        val targetFluid: ResourceLocation,
        val resultingHeat: Float,
    )
}
