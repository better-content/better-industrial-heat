package com.bettercontent.betterindustrialheat.food

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class RecipeInputGateTest {
    @Test
    fun `rejects recipe when any input is dried food`() {
        assertTrue(RecipeInputGate.rejects(listOf("raw", "dried", "seasoning")) { it == "dried" })
        assertFalse(RecipeInputGate.rejects(listOf("raw", "seasoning")) { it == "dried" })
        assertFalse(RecipeInputGate.rejects(emptyList<String>()) { it == "dried" })
    }

    @Test
    fun `optional cooking pot mixin is pseudo and registered at recipe match boundary`() {
        val root = Path.of(".")
        val mixins = Files.readString(root.resolve("src/main/resources/better_industrial_heat.mixins.json"))
        val adapter = Files.readString(
            root.resolve("src/main/java/com/bettercontent/betterindustrialheat/mixin/food/FarmersDelightCookingPotRecipeMixin.java"),
        )
        assertTrue(mixins.contains("food.FarmersDelightCookingPotRecipeMixin"))
        assertTrue(adapter.contains("@Pseudo"))
        assertTrue(adapter.contains("vectorwing.farmersdelight.common.crafting.CookingPotRecipe"))
        assertTrue(adapter.contains("matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z"))
        assertTrue(adapter.contains("RecipeInputGate.rejects(inputs, isDriedFood)"))
    }
    @Test
    fun `optional Ube baking mat mixin is pseudo and registered at recipe match boundary`() {
        val root = Path.of(".")
        val mixins = Files.readString(root.resolve("src/main/resources/better_industrial_heat.mixins.json"))
        val adapter = Files.readString(
            root.resolve("src/main/java/com/bettercontent/betterindustrialheat/mixin/food/UbesDelightBakingMatRecipeMixin.java"),
        )
        assertTrue(mixins.contains("food.UbesDelightBakingMatRecipeMixin"))
        assertTrue(adapter.contains("@Pseudo"))
        assertTrue(adapter.contains("com.chefmooon.ubesdelight.common.crafting.forge.BakingMatRecipeImpl"))
        assertTrue(adapter.contains("matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z"))
        assertTrue(adapter.contains("RecipeInputGate.rejects(inputs, isDriedFood)"))
    }

}
