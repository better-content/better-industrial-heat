package com.bettercontent.betterindustrialheat.food

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.CuriosApi

/**
 * Curios slot contents participate in the carried-food walk when Curios is installed.
 * Callers must gate on the `curios` mod id so this class is never loaded without it.
 */
internal object CuriosCarriedInventories {
    fun walk(player: ServerPlayer, transform: (ItemStack) -> ItemStack, seen: MutableSet<Any>) {
        CuriosApi.getCuriosInventory(player).ifPresent { handler ->
            handler.getCurios().values.forEach { stacksHandler ->
                CarriedFoodWalk.walkHandler(stacksHandler.getStacks(), transform, seen, depth = 1)
            }
        }
    }
}
