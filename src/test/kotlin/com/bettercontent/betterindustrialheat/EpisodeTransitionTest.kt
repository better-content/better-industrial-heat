package com.bettercontent.betterindustrialheat

import com.bettercontent.betterindustrialheat.api.event.BodyTemperatureEpisodeEvent
import com.bettercontent.betterindustrialheat.compat.BodyTemperatureEpisodes
import com.bettercontent.betterindustrialheat.food.FoodThermalEpisodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpisodeTransitionTest {
    @Test
    fun `temperature episodes emit only on band transitions`() {
        assertEquals(BodyTemperatureEpisodeEvent.Stage.STRESSED_COLD, BodyTemperatureEpisodes.transition(-75.0, ""))
        assertEquals(BodyTemperatureEpisodeEvent.Stage.STRESSED_HOT, BodyTemperatureEpisodes.transition(75.0, ""))
        assertNull(BodyTemperatureEpisodes.transition(90.0, "active"))
        assertNull(BodyTemperatureEpisodes.transition(40.0, "active"))
        assertEquals(BodyTemperatureEpisodeEvent.Stage.COMFORT_RESTORED, BodyTemperatureEpisodes.transition(25.0, "active"))
        assertNull(BodyTemperatureEpisodes.transition(0.0, ""))
    }

    @Test
    fun `frozen food rejection starts one bounded episode`() {
        assertTrue(FoodThermalEpisodes.shouldStartEpisode(""))
        assertFalse(FoodThermalEpisodes.shouldStartEpisode("player:food:123"))
    }
}
