package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult

class ThermalFireboxBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.COLOR_BLACK)
        .strength(4.0f, 6.0f)
        .sound(SoundType.NETHER_BRICKS)
        .requiresCorrectToolForDrops(),
) {
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = ThermalFireboxBlockEntity(pos, state)

    @Suppress("DEPRECATION")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        val firebox = level.getBlockEntity(pos) as? ThermalFireboxBlockEntity ?: return InteractionResult.PASS
        val held = player.getItemInHand(hand)
        if (!held.isEmpty) {
            val oneFuel = held.copyWithCount(1)
            if (!firebox.insertFuel(oneFuel.copy(), true).isEmpty) return InteractionResult.PASS
            if (!level.isClientSide) {
                val remainder = firebox.insertFuel(oneFuel)
                if (remainder.isEmpty && !player.abilities.instabuild) held.shrink(1)
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        if (firebox.extractFuel(true).isEmpty) return InteractionResult.PASS
        if (!level.isClientSide) {
            val extracted = firebox.extractFuel()
            if (!player.addItem(extracted)) popResource(level, pos, extracted)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = createTickerHelper(
        blockEntityType,
        HeatSyncRegistries.THERMAL_FIREBOX_BLOCK_ENTITY.get(),
        ThermalFireboxBlockEntity::tick,
    )

    @Suppress("DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, replacement: BlockState, moving: Boolean) {
        if (!state.`is`(replacement.block)) {
            (level.getBlockEntity(pos) as? ThermalFireboxBlockEntity)?.let {
                Containers.dropContents(level, pos, it.fuelInventory())
            }
        }
        super.onRemove(state, level, pos, replacement, moving)
    }
}
