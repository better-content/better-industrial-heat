package com.bettercontent.betterindustrialheat.content.heat

import net.minecraft.nbt.CollectionTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.Event

/** An optional crafting integration can supply a validated, baked surface record.
 * Native heat machinery only consumes bounded scalars; it never resolves material data.
 */
interface ThermalSurfaceMachine {
    val thermalOutput: String
    fun applySurface(transfer: Double, envelope: Double, validatedSettings: CompoundTag)
    fun resetSurface()
    fun storedSurfaceTag(): CompoundTag
    fun transferRatio(): Float
    fun envelopeRatio(): Float
}

class ThermalSurfacePlacedEvent(val machine: ThermalSurfaceMachine, val stack: ItemStack) : Event()
class ThermalSurfaceLoadedEvent(val machine: ThermalSurfaceMachine) : Event()

internal class ThermalSurfaceVariant(private val owner: ThermalSurfaceMachine) {
    private var transfer = 1f
    private var envelope = 1f
    private var serialized = CompoundTag()

    fun load(tag: CompoundTag) {
        reset()
        // Opaque settings are validated by the optional manufacturing listener. Invalid/missing
        // settings never affect native heat operation, and only a validated record can set rates.
        if (tag.contains(SETTINGS_KEY, Tag.TAG_COMPOUND.toInt())) {
            val offered = tag.getCompound(SETTINGS_KEY)
            if (bounded(offered)) serialized.put(SETTINGS_KEY, offered.copy())
        }
        MinecraftForge.EVENT_BUS.post(ThermalSurfaceLoadedEvent(owner))
    }

    fun save(tag: CompoundTag) {
        if (serialized.contains(SETTINGS_KEY, Tag.TAG_COMPOUND.toInt()))
            tag.put(SETTINGS_KEY, serialized.getCompound(SETTINGS_KEY).copy())
    }

    fun apply(transfer: Double, envelope: Double, validatedSettings: CompoundTag) {
        if (!transfer.isFinite() || transfer !in 0.75..1.5 || !envelope.isFinite() || envelope !in 0.75..1.5 ||
            !validatedSettings.contains(SETTINGS_KEY, Tag.TAG_COMPOUND.toInt()) ||
            !bounded(validatedSettings.getCompound(SETTINGS_KEY))) {
            reset()
            return
        }
        this.transfer = transfer.toFloat()
        this.envelope = envelope.toFloat()
        this.serialized = validatedSettings.copy()
    }

    fun reset() {
        transfer = 1f
        envelope = 1f
        serialized = CompoundTag()
    }

    fun storedTag(): CompoundTag = serialized.copy()
    fun transferRatio(): Float = transfer
    fun envelopeRatio(): Float = envelope

    /** Lower rated bodies can have smaller working buffers; none exceed existing native caps. */
    fun cappedCapacity(nativeMaximum: Float): Float = nativeMaximum * envelope.coerceIn(.75f, 1f)
    /** Native resistance 1 gives delta / 2; at rate r, delta / (2/r), no heat is created. */
    fun resistance(): Float = 2f / transfer - 1f

    /** Native IH cannot link optional Manufacturing's codec. Bound the opaque surface record
     * before copying it into saves or packets, including when Manufacturing is not loaded.
     */
    private fun bounded(root: Tag): Boolean {
        val pending = java.util.ArrayDeque<Pair<Tag, Int>>()
        pending.add(root to 0)
        var nodes = 0
        var textBytes = 0
        fun text(value: String): Boolean {
            if (value.length > 2048) return false
            textBytes += value.toByteArray(Charsets.UTF_8).size
            return textBytes <= 2048
        }
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            if (depth > 8 || ++nodes > 256) return false
            when (node) {
                is CompoundTag -> {
                    if (node.size() > 256 - nodes) return false
                    for (key in node.allKeys) {
                        if (!text(key)) return false
                        pending.add((node.get(key) ?: return false) to depth + 1)
                    }
                }
                is CollectionTag<*> -> {
                    if (node.size > 256 - nodes) return false
                    for (entry in node) pending.add(entry to depth + 1)
                }
                is StringTag -> if (!text(node.asString)) return false
                is NumericTag -> Unit
                else -> return false
            }
        }
        return true
    }

    companion object {
        const val SETTINGS_KEY = "better_manufacturing:settings"

        fun placed(machine: ThermalSurfaceMachine, stack: ItemStack) {
            MinecraftForge.EVENT_BUS.post(ThermalSurfacePlacedEvent(machine, stack))
        }
    }
}
