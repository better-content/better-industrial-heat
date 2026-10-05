package com.bettercontent.betterindustrialheat.food

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fml.ModList
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Walks every food stack a player carries: the vanilla inventory, the ender chest,
 * Curios slots when installed, and the item handlers carried items expose (bags,
 * backpacks) up to [MAX_DEPTH] levels deep. Each visited stack passes through
 * [transform] and the returned stack is written back to the slot it came from.
 */
internal object CarriedFoodWalk {
    const val MAX_DEPTH = 2
    private const val CURIOS_MOD_ID = "curios"

    fun walk(player: ServerPlayer, transform: (ItemStack) -> ItemStack) {
        val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        for (list in listOf(player.inventory.items, player.inventory.armor, player.inventory.offhand)) {
            for (slot in list.indices) {
                val next = transform(list[slot])
                list[slot] = next
                descend(next, transform, seen, depth = 1)
            }
        }
        walkContainer(player.enderChestInventory, transform, seen, depth = 1)
        if (ModList.get().isLoaded(CURIOS_MOD_ID)) CuriosCarriedInventories.walk(player, transform, seen)
    }

    private fun walkContainer(container: Container, transform: (ItemStack) -> ItemStack, seen: MutableSet<Any>, depth: Int) {
        for (slot in 0 until container.containerSize) {
            val stack = container.getItem(slot)
            val next = transform(stack)
            if (next !== stack) container.setItem(slot, next)
            descend(next, transform, seen, depth)
        }
    }

    fun walkHandler(handler: IItemHandler, transform: (ItemStack) -> ItemStack, seen: MutableSet<Any>, depth: Int) {
        if (depth > MAX_DEPTH || !seen.add(handler)) return
        for (slot in 0 until handler.slots) {
            val stack = handler.getStackInSlot(slot)
            val next = transform(stack)
            if (next !== stack && handler is IItemHandlerModifiable) handler.setStackInSlot(slot, next)
            descend(next, transform, seen, depth)
        }
    }

    private fun descend(stack: ItemStack, transform: (ItemStack) -> ItemStack, seen: MutableSet<Any>, depth: Int) {
        if (stack.isEmpty || depth >= MAX_DEPTH) return
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent { handler ->
            walkHandler(handler, transform, seen, depth + 1)
        }
    }
}
