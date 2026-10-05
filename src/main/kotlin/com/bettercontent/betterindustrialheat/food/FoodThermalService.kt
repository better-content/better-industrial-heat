package com.bettercontent.betterindustrialheat.food

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncThermalTags
import com.bettercontent.betterindustrialheat.api.ThermalCapabilities
import com.bettercontent.betterindustrialheat.api.HeatBlockEntity
import com.bettercontent.betterindustrialheat.api.HeatStorageThermalBody
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.items.IItemHandlerModifiable
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * Food spoilage on a storage-category model: food ages by active world time scaled by its
 * storage category (frozen 0x, cold 0.1x, ambient 1x), capped at one stage per settle so
 * no gap can one-step food from fresh to ruined. Food owned by an offline player does not
 * age at all. There is no per-stack physical temperature.
 */
object FoodThermalService {
    private const val KEY = "better_industrial_heat_food"
    private const val DECAY = "decay"
    private const val LAST_TIME = "last_time"
    private const val RATE = "rate"
    private const val WARM_SINCE = "warm_since"
    private const val VERSION = "version"
    private const val LEGACY_PRESERVATION_RATE = "preservation_rate"
    private const val ACTIVE = "better_industrial_heat_food_active"
    private const val CURRENT_VERSION = 5
    private const val CARRIED_SETTLE_INTERVAL = 200

    /** Frozen food thaws after this much settled non-frozen storage before it can be eaten. */
    internal const val THAW_TICKS = 1_200L

    /** Thermal body temperature at or below this is frozen storage; at or below [COLD_K] is cold storage. */
    internal const val FROZEN_K = 273.15
    internal const val COLD_K = 278.15

    private val inventoryFingerprints = Collections.synchronizedMap(WeakHashMap<BlockEntity, Int>())
    private val reconciling = ThreadLocal.withInitial { false }

    enum class Stage { FRESH, STALE, SPOILED, ROTTEN, CONVERTED }

    /** Storage category of a settled food stack; the only driver of the spoilage rate. */
    enum class Storage { FROZEN, COLD, AMBIENT }

    data class Profile(val id: String, val days: Double?, val preserved: Boolean, val meat: Boolean)

    fun profile(stack: ItemStack): Profile {
        val id = stack.item.descriptionId.lowercase()
        val meat = stack.item.foodProperties?.isMeat == true
        return when {
            id.contains("vodka") || id.contains("rum") -> Profile("distilled_alcohol", null, false, false)
            id.contains("beer") || id.contains("wine") || id.contains("mead") -> Profile("fermented_alcohol", null, false, false)
            id.contains("grog") || id.contains("nog") || id.contains("cocktail") -> Profile("preserved", 1.0, true, false)
            stack.`is`(HeatSyncThermalTags.DRIED_FOODS) -> Profile("dried", 1.0, true, meat)
            id.contains("canned") || id.contains("golden_") -> Profile("shelf_stable", null, false, meat)
            id.contains("jerky") || id.contains("pickle") || id.contains("kimchi") || id.contains("jam") || id.contains("marmalade") || id.contains("smoked") || id.contains("cheese") -> Profile("preserved", 1.0, true, meat)
            meat || id.contains("raw_") -> Profile("raw_animal", 1.0, false, meat)
            id.contains("apple") || id.contains("berry") || id.contains("carrot") || id.contains("potato") || id.contains("melon") || id.contains("vegetable") -> Profile("fresh_produce", 1.0, false, false)
            else -> Profile("prepared", 1.0, false, meat)
        }
    }

    /** Spoilage rate of a category: frozen food never ages, cold or preserved food ages at 0.1x. */
    internal fun preservationRate(profile: Profile, storage: Storage): Double = when {
        profile.days == null -> 0.0
        storage == Storage.FROZEN -> 0.0
        storage == Storage.COLD || profile.preserved -> 0.1
        else -> 1.0
    }

    /**
     * Returns the settled v5 record for [stack], migrating any legacy record in place.
     * Migration keeps accumulated decay but starts a fresh interval: a legacy timestamp is
     * rebased to now so no gap from before the migration can be charged.
     */
    fun state(stack: ItemStack, storage: Storage, gameTime: Long): CompoundTag {
        val root = stack.orCreateTag
        val existing = root.getCompound(KEY)
        if (existing.getInt(VERSION) == CURRENT_VERSION) return existing
        val carriedDecay = existing.getDouble(DECAY).coerceIn(0.0, 2.5)
        existing.allKeys.toList().forEach(existing::remove)
        existing.putInt(VERSION, CURRENT_VERSION)
        existing.putDouble(DECAY, carriedDecay)
        existing.putLong(LAST_TIME, gameTime)
        existing.putDouble(RATE, preservationRate(profile(stack), storage))
        existing.putLong(WARM_SINCE, 0L)
        root.put(KEY, existing)
        return existing
    }

    /** Reads the persisted record across the save migration window without forcing a migration. */
    private fun thermalTag(stack: ItemStack): CompoundTag? = stack.tag?.getCompound(KEY)
        ?.takeIf { it.contains(DECAY) || it.getInt(VERSION) != 0 }

    fun stage(stack: ItemStack): Stage = FoodAgePolicy.stage(thermalTag(stack)?.getDouble(DECAY) ?: 0.0)

    fun isFrozen(stack: ItemStack): Boolean {
        if (profile(stack).days == null) return false
        val tag = thermalTag(stack)?.takeIf { it.getInt(VERSION) == CURRENT_VERSION } ?: return false
        return isFrozenTag(tag)
    }

    private fun isFrozenTag(tag: CompoundTag): Boolean =
        tag.getLong(WARM_SINCE) != 0L || tag.getDouble(RATE) == 0.0

    /** Item-model tint: frozen food is visibly ice-blue; spoilage deepens from faded brown to near-black. */
    fun itemTint(stack: ItemStack): Int {
        if (!stack.isEdible) return 0xFFFFFF
        if (isFrozen(stack)) return 0x9DDCFF
        return when (stage(stack)) {
            Stage.FRESH -> 0xFFFFFF
            Stage.STALE -> 0xD9C9AA
            Stage.SPOILED -> 0x916D43
            Stage.ROTTEN, Stage.CONVERTED -> 0x392416
        }
    }

    /** Settles the persisted age in place without converting the item. */
    fun settle(stack: ItemStack, storage: Storage, gameTime: Long) {
        if (!isTrackedFood(stack)) return
        val profile = profile(stack)
        val tag = state(stack, storage, gameTime)
        val elapsed = (gameTime - tag.getLong(LAST_TIME)).coerceAtLeast(0L)
        profile.days?.let { days ->
            // Settle elapsed age under the category persisted at the last update. This
            // makes a late move into cold or frozen storage unable to erase warm time.
            val priorRate = tag.getDouble(RATE).coerceIn(0.0, 1.0)
            tag.putDouble(DECAY, FoodAgePolicy.advanceDecay(tag.getDouble(DECAY), elapsed, priorRate, days))
        }
        val frozenBefore = isFrozenTag(tag)
        val newRate = preservationRate(profile, storage)
        when {
            profile.days == null -> tag.putLong(WARM_SINCE, 0L)
            // Frozen storage keeps the thaw clock reset; leaving it starts the thaw clock.
            newRate == 0.0 -> tag.putLong(WARM_SINCE, 0L)
            frozenBefore -> {
                val warmSince = tag.getLong(WARM_SINCE)
                when {
                    warmSince == 0L -> tag.putLong(WARM_SINCE, gameTime)
                    gameTime - warmSince >= THAW_TICKS -> tag.putLong(WARM_SINCE, 0L)
                }
            }
            else -> tag.putLong(WARM_SINCE, 0L)
        }
        // The new category governs the following lazy interval. Retaining the prior rate
        // here would charge the first interval after a move at the old category's rate.
        tag.putLong(LAST_TIME, gameTime)
        tag.putDouble(RATE, newRate)
    }

    /** Settles the stack and converts it to spoiled produce/meat once it is rotten. */
    fun tick(stack: ItemStack, storage: Storage, gameTime: Long): ItemStack {
        if (!isTrackedFood(stack)) return stack
        settle(stack, storage, gameTime)
        return if (stage(stack) >= Stage.ROTTEN) {
            ItemStack(if (profile(stack).meat) FoodItems.SPOILED_MEAT.get() else FoodItems.SPOILED_PRODUCE.get(), stack.count)
        } else stack
    }

    /** Checks the settled persisted age without mutating a recipe input during repeated matching. */
    @JvmStatic
    fun canDryAt(input: ItemStack, gameTime: Long): Boolean {
        if (!input.isEdible || !FoodItems.isDriedFoodSource(input)) return false
        val profile = profile(input)
        val lifetime = profile.days ?: return true
        val stored = input.tag?.takeIf { it.contains(KEY) }?.getCompound(KEY) ?: return true
        val decay = stored.getDouble(DECAY)
        val elapsed = if (gameTime <= stored.getLong(LAST_TIME)) 0L else
            runCatching { Math.subtractExact(gameTime, stored.getLong(LAST_TIME)) }.getOrDefault(Long.MAX_VALUE)
        val rate = when {
            stored.getInt(VERSION) == CURRENT_VERSION -> stored.getDouble(RATE).coerceIn(0.0, 1.0)
            stored.contains(LEGACY_PRESERVATION_RATE) -> stored.getDouble(LEGACY_PRESERVATION_RATE).coerceIn(0.0, 1.0)
            else -> 1.0
        }
        return FoodAgePolicy.remainsFresh(decay, elapsed, rate, lifetime)
    }

    /** Cooking changes the ordinary item but must not reset its elapsed food state. */
    @JvmStatic
    fun carryCookingState(input: ItemStack, output: ItemStack) {
        if (!input.isEdible || !output.isEdible) return
        val thermal = thermalTag(input) ?: return
        output.orCreateTag.put(KEY, thermal.copy())
    }

    /** Drying racks preserve the input's thermal state while producing a dried food. */
    @JvmStatic
    fun carryDryingState(input: ItemStack, output: ItemStack) = carryCookingState(input, output)

    /** Updates a real block inventory from its storage category: adjacent heat devices and ice decide it. */
    fun tickContainer(level: Level, pos: BlockPos, container: Container, gameTime: Long) {
        val storage = containerStorage(level, pos)
        for (slot in 0 until container.containerSize) {
            val stack = container.getItem(slot)
            if (isTrackedFood(stack)) container.setItem(slot, tick(stack, storage, gameTime))
        }
    }

    /**
     * A powered Better Industrial Heat body is an appliance setpoint; ice and snow are
     * passive frozen storage. Everything else stores food at ambient rates.
     */
    internal fun containerStorage(level: Level, pos: BlockPos): Storage {
        val thermal = adjacentThermalTarget(level, pos)
        if (thermal != null) return when {
            thermal <= FROZEN_K -> Storage.FROZEN
            thermal <= COLD_K -> Storage.COLD
            else -> Storage.AMBIENT
        }
        return if (hasPassiveColdNeighbor(level, pos)) Storage.FROZEN else Storage.AMBIENT
    }

    fun adjacentThermalTarget(level: Level, pos: BlockPos): Double? {
        val temperatures = loadedAdjacentPositions(pos, level::isLoaded).mapNotNull { (direction, sourcePos) ->
            val source = level.getBlockEntity(sourcePos) ?: return@mapNotNull null
            source.getCapability(ThermalCapabilities.BODY, direction.opposite)
                .resolve()
                .orElseGet {
                    (source as? HeatBlockEntity)?.let(::HeatStorageThermalBody)
                }
                ?.temperatureKelvin()
        }
        return temperatures.takeIf { it.isNotEmpty() }?.average()
    }

    private fun hasPassiveColdNeighbor(level: Level, pos: BlockPos): Boolean =
        loadedAdjacentPositions(pos, level::isLoaded).any { (_, sourcePos) ->
            when (level.getBlockState(sourcePos).block) {
                Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE -> true
                else -> false
            }
        }

    /** Neighbor enumeration is loaded-only so thermal inventory ticks can never request chunk generation. */
    internal fun loadedAdjacentPositions(
        pos: BlockPos,
        isLoaded: (BlockPos) -> Boolean,
    ): List<Pair<Direction, BlockPos>> = Direction.values().mapNotNull { direction ->
        val neighbor = pos.relative(direction)
        if (isLoaded(neighbor)) direction to neighbor else null
    }

    // region Carried food lifecycle

    /** Carried food settles every few seconds so its tint and tooltip track reality during play. */
    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val player = event.player as? ServerPlayer ?: return
        if (player.tickCount % CARRIED_SETTLE_INTERVAL != 0) return
        settleCarriedFood(player, player.level().gameTime)
    }

    @SubscribeEvent
    fun onPlayerLoggedIn(event: net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        // Carried food is not simulated while the player is offline: rebase before
        // anything can settle so login cannot charge the absent interval as active age.
        rebaseCarriedFood(player, player.server.overworld().gameTime)
    }

    @SubscribeEvent
    fun onPlayerLoggedOut(event: net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val now = player.server.overworld().gameTime
        // Settle the final online interval, then rebase so the offline interval is never charged.
        settleCarriedFood(player, now)
        rebaseCarriedFood(player, now)
    }

    /** Settles every carried food stack at ambient rates: inventory, ender chest, Curios, and bag handlers. */
    internal fun settleCarriedFood(player: ServerPlayer, gameTime: Long) {
        CarriedFoodWalk.walk(player) { stack -> tick(stack, Storage.AMBIENT, gameTime) }
    }

    internal fun rebaseCarriedFood(player: ServerPlayer, now: Long) {
        CarriedFoodWalk.walk(player) { stack ->
            stack.tag?.getCompound(KEY)?.putLong(LAST_TIME, now)
            stack
        }
    }

    // endregion

    // region Block and machine inventories (change-driven)

    @SubscribeEvent
    fun onBlockPlaced(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? ServerLevel ?: return
        if (event.entity == null) return
        level.server.execute {
            level.getBlockEntity(event.pos)?.let { activateInventory(it, reconcileNow = true) }
        }
    }

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity as? ServerPlayer ?: return
        if (player is FakePlayer) return
        val blockEntity = player.level().getBlockEntity(event.pos) ?: return
        if (!isInventory(blockEntity)) return
        activateInventory(blockEntity, reconcileNow = false)
        player.server.execute { reconcileBlockInventory(blockEntity, force = true) }
    }

    @JvmStatic
    fun onBlockEntityChanged(blockEntity: BlockEntity) {
        if (blockEntity.level?.isClientSide != false || blockEntity.isRemoved || reconciling.get()) return
        // Machine containers may create food without a player opening them. Keep only
        // unresolved generated-loot containers dormant until that loot is unpacked.
        if (!blockEntity.persistentData.getBoolean(ACTIVE)) {
            val hasUnopenedLoot = blockEntity is RandomizableContainerBlockEntity &&
                (blockEntity as com.bettercontent.betterindustrialheat.mixin.minecraft.RandomizableContainerBlockEntityAccessor)
                    .heatSyncLootTable != null
            // Reading a RandomizableContainerBlockEntity's slots unpacks its loot table.
            // Check this first so the guard itself cannot turn an unopened container into
            // an opened one.
            if (hasUnopenedLoot) return
            val hasTrackedFood = inventoryFingerprint(blockEntity) != 0
            if (!hasTrackedFood) return
            if (!ContainerFoodAdmissionPolicy.shouldActivate(hasTrackedFood, hasUnopenedLoot)) return
            activateInventory(blockEntity, reconcileNow = false)
        }
        reconcileBlockInventory(blockEntity, force = false)
    }

    fun activateInventory(blockEntity: BlockEntity, reconcileNow: Boolean) {
        if (!isInventory(blockEntity)) return
        blockEntity.persistentData.putBoolean(ACTIVE, true)
        blockEntity.setChanged()
        if (reconcileNow) reconcileBlockInventory(blockEntity, force = true)
    }

    fun isActivated(blockEntity: BlockEntity): Boolean = blockEntity.persistentData.getBoolean(ACTIVE)

    private fun reconcileBlockInventory(blockEntity: BlockEntity, force: Boolean) {
        val level = blockEntity.level ?: return
        if (level.isClientSide || blockEntity.isRemoved || reconciling.get()) return
        val before = inventoryFingerprint(blockEntity)
        if (!force && inventoryFingerprints[blockEntity] == before) return
        if (before == 0) {
            inventoryFingerprints[blockEntity] = before
            return
        }
        reconciling.set(true)
        try {
            tickBlockInventory(level, blockEntity, level.gameTime)
            inventoryFingerprints[blockEntity] = inventoryFingerprint(blockEntity)
        } finally {
            reconciling.set(false)
        }
    }

    private fun tickBlockInventory(level: Level, blockEntity: BlockEntity, gameTime: Long) {
        if (blockEntity is Container) {
            tickContainer(level, blockEntity.blockPos, blockEntity, gameTime)
            return
        }
        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent { handler ->
            val writable = handler as? IItemHandlerModifiable ?: return@ifPresent
            val storage = containerStorage(level, blockEntity.blockPos)
            for (slot in 0 until writable.slots) {
                val stack = writable.getStackInSlot(slot)
                if (isTrackedFood(stack)) writable.setStackInSlot(slot, tick(stack, storage, gameTime))
            }
        }
    }

    private fun isInventory(blockEntity: BlockEntity): Boolean =
        blockEntity is Container || blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent

    private fun inventoryFingerprint(blockEntity: BlockEntity): Int {
        var result = 1
        var found = false
        fun add(slot: Int, stack: ItemStack) {
            if (!isTrackedFood(stack)) return
            found = true
            result = 31 * result + slot
            result = 31 * result + stack.item.hashCode()
            result = 31 * result + stack.count
            result = 31 * result + (stack.tag?.hashCode() ?: 0)
        }
        if (blockEntity is Container) {
            (0 until blockEntity.containerSize).forEach { add(it, blockEntity.getItem(it)) }
        } else {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent { handler ->
                (0 until handler.slots).forEach { add(it, handler.getStackInSlot(it)) }
            }
        }
        return if (found) result else 0
    }

    // endregion

    // region Eating

    @SubscribeEvent
    fun onUseStart(event: LivingEntityUseItemEvent.Start) {
        val stack = event.item
        if (!stack.isEdible) return
        val player = event.entity as? ServerPlayer
        player?.let { settle(stack, Storage.AMBIENT, it.level().gameTime) }
        if (isFrozen(stack)) {
            player?.let { FoodThermalEpisodes.onFrozenUseRejected(it, stack) }
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onUseFinish(event: LivingEntityUseItemEvent.Finish) {
        val player = event.entity as? ServerPlayer ?: return
        val current = event.item
        FoodThermalEpisodes.onFoodUseFinished(player, current)
        val stage = if (current.item == FoodItems.SPOILED_MEAT.get() || current.item == FoodItems.SPOILED_PRODUCE.get()) Stage.ROTTEN else stage(current)
        val amplifier = when (stage) { Stage.STALE -> 0; Stage.SPOILED -> 1; Stage.ROTTEN, Stage.CONVERTED -> 2; else -> return }
        val duration = debuffDurationTicks(stage)
        player.addEffect(MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, duration, amplifier))
        player.addEffect(MobEffectInstance(FoodEffects.THIRST.get(), duration, amplifier))
        player.addEffect(MobEffectInstance(FoodEffects.MALNOURISHMENT.get(), duration, amplifier))
        if (amplifier == 2) {
            player.addEffect(MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 0))
            player.addEffect(MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 400, 0))
        }
    }

    fun debuffDurationTicks(stage: Stage): Int = if (stage == Stage.STALE) 200 else 1200

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        val stack = event.itemStack
        if (!stack.isEdible || !stack.tag?.contains(KEY).orFalse()) return
        val p = profile(stack)
        event.toolTip.add(Component.literal(stage(stack).name.lowercase()))
        if (isFrozen(stack)) event.toolTip.add(Component.translatable("tooltip.better_industrial_heat.food_frozen"))
        if (p.days == null) event.toolTip.add(Component.translatable("tooltip.better_industrial_heat.food_shelf_stable"))
    }

    private fun Boolean?.orFalse() = this == true

    // endregion

    private fun isTrackedFood(stack: ItemStack): Boolean {
        if (!stack.isEdible) return false
        if (FoodItems.SPOILED_MEAT.isPresent && stack.item === FoodItems.SPOILED_MEAT.get()) return false
        if (FoodItems.SPOILED_PRODUCE.isPresent && stack.item === FoodItems.SPOILED_PRODUCE.get()) return false
        return true
    }
}
