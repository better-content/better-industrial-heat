package com.bettercontent.betterindustrialheat.food

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * Compatibility and commit-time state folding for food stacks whose only differing tag is
 * Better Industrial Heat. Merging never invents state: two ordinary untracked stacks merge
 * natively and stay untracked, so a merge cannot write a stale timestamp that later charges
 * the world's whole age as spoilage.
 */
object FoodStackMergeService {
    private const val KEY = "better_industrial_heat_food"
    private const val DECAY = "decay"
    private const val LAST_TIME = "last_time"
    private const val RATE = "rate"
    private const val WARM_SINCE = "warm_since"
    private const val VERSION = "version"
    private const val LEGACY_PRESERVATION_RATE = "preservation_rate"
    private const val CURRENT_VERSION = 5

    internal data class ThermalValues(
        val decay: Double,
        val lastTime: Long,
        val rate: Double,
        val present: Boolean,
    )

    @JvmStatic
    fun canMerge(first: ItemStack, second: ItemStack): Boolean {
        if (ItemStack.isSameItemSameTags(first, second)) return true
        if (first.isEmpty || second.isEmpty || !first.isEdible || !second.isEdible) return false
        if (!ItemStack.isSameItem(first, second) || !first.areCapsCompatible(second)) return false
        return tagWithoutThermalState(first) == tagWithoutThermalState(second)
    }

    /**
     * Writes the folded state to the real destination after [movedCount] items were committed.
     * Both snapshots must have been taken before the transfer; the source remainder is never mutated.
     */
    @JvmStatic
    fun mergeInto(
        output: ItemStack,
        destinationBefore: ItemStack,
        sourceBefore: ItemStack,
        movedCount: Int,
    ) {
        val destinationCount = destinationBefore.count
        if (movedCount <= 0 || destinationCount <= 0 || output.isEmpty) return
        val comparableSource = sourceBefore.copy().also { if (it.isEmpty) it.count = movedCount }
        if (!destinationBefore.isEdible || !comparableSource.isEdible) return
        if (!canMerge(destinationBefore, comparableSource)) return
        val destination = read(destinationBefore)
        val source = read(comparableSource)
        if (!destination.present && !source.present) return
        val merged = weighted(destination, destinationCount, source, movedCount)
        val target = if (destination.present) destination else source

        val thermal = CompoundTag()
        thermal.putInt(VERSION, CURRENT_VERSION)
        thermal.putDouble(DECAY, merged.decay.coerceIn(0.0, 2.5))
        thermal.putLong(LAST_TIME, merged.lastTime)
        thermal.putDouble(RATE, target.rate)
        thermal.putLong(WARM_SINCE, 0L)
        output.orCreateTag.put(KEY, thermal)
    }

    internal fun weighted(
        destination: ThermalValues,
        destinationCount: Int,
        source: ThermalValues,
        movedCount: Int,
    ): ThermalValues {
        val total = destinationCount + movedCount
        return ThermalValues(
            decay = (destination.decay * destinationCount + source.decay * movedCount) / total,
            lastTime = maxOf(destination.lastTime, source.lastTime),
            rate = destination.rate,
            present = true,
        )
    }

    private fun read(stack: ItemStack): ThermalValues {
        val tag = stack.tag?.getCompound(KEY)?.takeIf { it.getInt(VERSION) == CURRENT_VERSION || it.getInt(VERSION) == 3 || it.getInt(VERSION) == 4 }
            ?: return ThermalValues(0.0, 0L, 1.0, present = false)
        return ThermalValues(
            decay = tag.getDouble(DECAY).coerceIn(0.0, 2.5),
            lastTime = tag.getLong(LAST_TIME),
            rate = when {
                tag.contains(RATE) -> tag.getDouble(RATE).takeIf { it in 0.0..1.0 } ?: 1.0
                tag.contains(LEGACY_PRESERVATION_RATE) -> tag.getDouble(LEGACY_PRESERVATION_RATE).takeIf { it in 0.0..1.0 } ?: 1.0
                else -> 1.0
            },
            present = true,
        )
    }

    private fun tagWithoutThermalState(stack: ItemStack): CompoundTag? {
        val copy = stack.tag?.copy() ?: return null
        copy.remove(KEY)
        return copy.takeUnless { it.isEmpty }
    }
}
