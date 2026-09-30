package com.bettercontent.betterindustrialheat.food

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

/** Compatibility and commit-time state folding for food stacks whose only differing tag is Better Industrial Heat. */
object FoodStackMergeService {
    private const val KEY = "better_industrial_heat_food"
    private const val TEMPERATURE_BUCKET = "temperature_bucket_c"
    private const val TEMPERATURE_PRECISE = "temperature_precise_k"
    private const val DECAY = "decay"
    private const val LAST_TIME = "last_time"
    private const val LAST_TARGET_BUCKET = "last_target_bucket_c"
    private const val LAST_TARGET_APPLIANCE = "last_target_appliance"
    private const val PRESERVATION_RATE = "preservation_rate"
    private const val VERSION = "version"
    private const val CURRENT_VERSION = 4
    private const val AMBIENT_K = 295.15
    private const val WORLD_TAU_TICKS = 4000.0
    private const val APPLIANCE_TAU_TICKS = 200.0
    private const val TEMPERATURE_BUCKET_C = 5.0
    private const val REFRIGERATION_C = 5.0

    internal data class ThermalValues(
        val temperatureK: Double,
        val decay: Double,
        val lastTime: Long,
        val targetBucket: Int,
        val targetAppliance: Boolean,
        val preservationRate: Double,
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
     * Writes the weighted state to the real destination after [movedCount] items were committed.
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
        val destinationRaw = read(destinationBefore)
        val sourceRaw = read(comparableSource)
        val commonTime = max(destinationRaw.lastTime, sourceRaw.lastTime)
        val destination = advance(destinationRaw, destinationBefore, commonTime)
        val source = advance(sourceRaw, comparableSource, commonTime)
        val merged = weighted(destination, destinationCount, source, movedCount)
        val target = when {
            destinationRaw.present -> destinationRaw
            sourceRaw.present -> sourceRaw
            else -> ThermalValues(AMBIENT_K, 0.0, commonTime, bucketForKelvin(AMBIENT_K), false, 1.0, false)
        }

        val thermal = CompoundTag()
        thermal.putInt(VERSION, CURRENT_VERSION)
        thermal.putInt(TEMPERATURE_BUCKET, bucketForKelvin(merged.temperatureK))
        thermal.putDouble(TEMPERATURE_PRECISE, merged.temperatureK)
        thermal.putDouble(DECAY, merged.decay.coerceIn(0.0, 2.5))
        thermal.putLong(LAST_TIME, commonTime)
        thermal.putInt(LAST_TARGET_BUCKET, target.targetBucket)
        thermal.putBoolean(LAST_TARGET_APPLIANCE, target.targetAppliance)
        thermal.putDouble(PRESERVATION_RATE, target.preservationRate)
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
            temperatureK = (destination.temperatureK * destinationCount + source.temperatureK * movedCount) / total,
            decay = (destination.decay * destinationCount + source.decay * movedCount) / total,
            lastTime = max(destination.lastTime, source.lastTime),
            targetBucket = destination.targetBucket,
            targetAppliance = destination.targetAppliance,
            preservationRate = destination.preservationRate,
            present = true,
        )
    }

    private fun read(stack: ItemStack): ThermalValues {
        val tag = stack.tag?.getCompound(KEY)?.takeIf { it.getInt(VERSION) == CURRENT_VERSION || it.getInt(VERSION) == 3 }
            ?: return ThermalValues(AMBIENT_K, 0.0, 0L, bucketForKelvin(AMBIENT_K), false, 1.0, false)
        return ThermalValues(
            temperatureK = if (tag.contains(TEMPERATURE_PRECISE)) tag.getDouble(TEMPERATURE_PRECISE) else kelvinForBucket(tag.getInt(TEMPERATURE_BUCKET)),
            decay = tag.getDouble(DECAY).coerceIn(0.0, 2.5),
            lastTime = tag.getLong(LAST_TIME),
            targetBucket = tag.getInt(LAST_TARGET_BUCKET),
            targetAppliance = tag.getBoolean(LAST_TARGET_APPLIANCE),
            preservationRate = tag.getDouble(PRESERVATION_RATE).takeIf { it in 0.0..1.0 } ?: 1.0,
            present = true,
        )
    }

    private fun advance(values: ThermalValues, stack: ItemStack, commonTime: Long): ThermalValues {
        if (!values.present) return values.copy(lastTime = commonTime)
        val elapsed = (commonTime - values.lastTime).coerceAtLeast(0L)
        if (elapsed == 0L) return values.copy(lastTime = commonTime)
        val targetK = kelvinForBucket(values.targetBucket)
        val tau = if (values.targetAppliance) APPLIANCE_TAU_TICKS else WORLD_TAU_TICKS
        val temperatureK = targetK + (values.temperatureK - targetK) * exp(-elapsed / tau)
        val days = FoodThermalService.profile(stack).days
        val decay = FoodAgePolicy.advanceDecay(values.decay, elapsed, values.preservationRate, days)
        return values.copy(temperatureK = temperatureK, decay = decay.coerceIn(0.0, 2.5), lastTime = commonTime)
    }

    private fun tagWithoutThermalState(stack: ItemStack): CompoundTag? {
        val copy = stack.tag?.copy() ?: return null
        copy.remove(KEY)
        return copy.takeUnless { it.isEmpty }
    }

    internal fun bucketForKelvin(kelvin: Double): Int =
        floor((kelvin - 273.15) / TEMPERATURE_BUCKET_C + 0.5).toInt()
    internal fun kelvinForBucket(bucket: Int): Double = bucket * TEMPERATURE_BUCKET_C + 273.15
}
