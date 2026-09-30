package com.bettercontent.betterindustrialheat.content.coolant

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.ConcurrentHashMap

private val COOLANT_GSON: Gson = Gson()
private const val COOLANT_DIRECTORY = "liquid_coolants"

object LiquidCoolantManager : SimpleJsonResourceReloadListener(COOLANT_GSON, COOLANT_DIRECTORY) {
    private val byColdFluid: MutableMap<ResourceLocation, LiquidCoolantDefinition> = ConcurrentHashMap()
    private val byHotFluid: MutableMap<ResourceLocation, LiquidCoolantDefinition> = ConcurrentHashMap()

    override fun apply(
        objectMap: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        val cold = HashMap<ResourceLocation, LiquidCoolantDefinition>()
        val hot = HashMap<ResourceLocation, LiquidCoolantDefinition>()

        objectMap.forEach { (id, json) ->
            runCatching {
                val definition = LiquidCoolantDefinition.fromJson(id, json.asJsonObject)
                validateRegistered(definition.coldFluid, id, "cold_fluid")
                validateRegistered(definition.hotFluid, id, "hot_fluid")
                require(cold.put(definition.coldFluid, definition) == null) {
                    "Duplicate cold coolant mapping for ${definition.coldFluid}"
                }
                require(hot.put(definition.hotFluid, definition) == null) {
                    "Duplicate hot coolant mapping for ${definition.hotFluid}"
                }
            }.onFailure { error ->
                HeatSyncMod.LOGGER.error("Failed to load coolant definition {}", id, error)
            }
        }

        byColdFluid.clear()
        byColdFluid.putAll(cold)
        byHotFluid.clear()
        byHotFluid.putAll(hot)

        HeatSyncMod.LOGGER.info("Loaded {} liquid coolant definitions", byColdFluid.size)
    }

    fun supported(fluidId: ResourceLocation): Boolean = byColdFluid.containsKey(fluidId) || byHotFluid.containsKey(fluidId)

    fun find(fluidId: ResourceLocation): LiquidCoolantDefinition? = byColdFluid[fluidId] ?: byHotFluid[fluidId]

    fun clearForTests() {
        byColdFluid.clear()
        byHotFluid.clear()
    }

    private fun validateRegistered(fluidId: ResourceLocation, definitionId: ResourceLocation, field: String) {
        require(ForgeRegistries.FLUIDS.containsKey(fluidId)) {
            "$field points at unknown fluid $fluidId in $definitionId"
        }
    }
}
