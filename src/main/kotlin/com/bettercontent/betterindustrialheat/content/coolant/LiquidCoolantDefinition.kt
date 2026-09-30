package com.bettercontent.betterindustrialheat.content.coolant

import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraftforge.fluids.FluidType

data class LiquidCoolantDefinition(
    val id: ResourceLocation,
    val coldFluid: ResourceLocation,
    val hotFluid: ResourceLocation,
    val heatPerBucket: Float,
    val coolingPerBucket: Float,
) {
    init {
        require(heatPerBucket > 0f) { "heat_per_bucket must be > 0 for $id" }
        require(coolingPerBucket > 0f) { "cooling_per_bucket must be > 0 for $id" }
        require(coolingPerBucket <= heatPerBucket) {
            "cooling_per_bucket must be <= heat_per_bucket for $id"
        }
        require(coldFluid != hotFluid) { "cold_fluid and hot_fluid must be different for $id" }
    }

    fun matchesCold(fluidId: ResourceLocation): Boolean = coldFluid == fluidId

    fun matchesHot(fluidId: ResourceLocation): Boolean = hotFluid == fluidId

    fun heatRequired(amount: Int): Float = scalePerBucket(heatPerBucket, amount)

    fun coolingRequired(amount: Int): Float = scalePerBucket(coolingPerBucket, amount)

    companion object {
        fun fromJson(id: ResourceLocation, json: JsonObject): LiquidCoolantDefinition {
            return LiquidCoolantDefinition(
                id = id,
                coldFluid = ResourceLocation.parse(GsonHelper.getAsString(json, "cold_fluid")),
                hotFluid = ResourceLocation.parse(GsonHelper.getAsString(json, "hot_fluid")),
                heatPerBucket = GsonHelper.getAsFloat(json, "heat_per_bucket"),
                coolingPerBucket = GsonHelper.getAsFloat(json, "cooling_per_bucket"),
            )
        }

        private fun scalePerBucket(value: Float, amount: Int): Float {
            return value * amount.toFloat() / FluidType.BUCKET_VOLUME.toFloat()
        }
    }
}
