package com.bettercontent.betterindustrialheat.food

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FoodEffectSideBoundaryTest {
    private val source = Files.readString(
        Path.of("src/main/kotlin/com/bettercontent/betterindustrialheat/food/FoodEffects.kt"),
    )

    @Test
    fun `system drains require a server player before invoking integrations`() {
        assertContains(source, "val player = entity as? ServerPlayer ?: return")
        assertEquals(
            2,
            Regex("fun drain\\(player: ServerPlayer, amplifier: Int\\)").findAll(source).count(),
            "Both optional integrations must retain a server-only entry point",
        )
        assertFalse(
            source.contains("fun drain(player: Player, amplifier: Int)"),
            "A broad Player bridge signature can admit LocalPlayer and reintroduce client-side mutation",
        )
    }
}
