package com.bettercontent.betterindustrialheat.compat

import com.bettercontent.betterindustrialheat.api.event.BodyTemperatureEpisodeEvent
import com.momosoftworks.coldsweat.api.util.Temperature
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

/** Uses Cold Sweat's authoritative body-temperature trait, never elapsed-time inference. */
object BodyTemperatureEpisodes {
    // Keep the legacy NBT name so existing player episodes survive the migration.
    private const val EPISODE_ROOT = "HeatSyncThreadTemperatureEpisode"
    private const val STRESS = 75.0
    private const val COMFORT = 25.0

    @SubscribeEvent
    fun onTick(event: TickEvent.PlayerTickEvent) {
        val player = event.player as? ServerPlayer ?: return
        if (event.phase != TickEvent.Phase.END || player.tickCount % 20 != 0) return
        val body = Temperature.get(player, Temperature.Trait.BODY)
        val persisted = player.persistentData.getCompound(Player.PERSISTED_NBT_TAG)
        var token = persisted.getString(EPISODE_ROOT)
        val transition = transition(body, token) ?: return
        if (transition != BodyTemperatureEpisodeEvent.Stage.COMFORT_RESTORED) {
            token = "${player.uuid}:temperature:${player.server.tickCount}"
            persisted.putString(EPISODE_ROOT, token);player.persistentData.put(Player.PERSISTED_NBT_TAG, persisted)
            MinecraftForge.EVENT_BUS.post(BodyTemperatureEpisodeEvent(player, token, transition))
        } else {
            MinecraftForge.EVENT_BUS.post(BodyTemperatureEpisodeEvent(
                player, token, BodyTemperatureEpisodeEvent.Stage.COMFORT_RESTORED,
            ))
            persisted.remove(EPISODE_ROOT);player.persistentData.put(Player.PERSISTED_NBT_TAG, persisted)
        }
    }

    internal fun transition(body: Double, episodeId: String): BodyTemperatureEpisodeEvent.Stage? = when {
        kotlin.math.abs(body) >= STRESS && episodeId.isBlank() ->
            if (body < 0) BodyTemperatureEpisodeEvent.Stage.STRESSED_COLD else BodyTemperatureEpisodeEvent.Stage.STRESSED_HOT
        kotlin.math.abs(body) <= COMFORT && episodeId.isNotBlank() -> BodyTemperatureEpisodeEvent.Stage.COMFORT_RESTORED
        else -> null
    }
}
