package com.bettercontent.betterindustrialheat.content.heat

import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.MapColor

class BoilerHeaterBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.METAL)
        .strength(4.0f, 6.0f)
        .sound(SoundType.COPPER)
        .requiresCorrectToolForDrops(),
) {
    init {
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false))
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = BoilerHeaterBlockEntity(pos, state)

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        (level.getBlockEntity(pos) as? BoilerHeaterBlockEntity)?.let { ThermalSurfaceVariant.placed(it, stack) }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        builder.add(ACTIVE)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int =
        (level.getBlockEntity(pos) as? BoilerHeaterBlockEntity)?.comparatorOutput() ?: 0

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? = createTickerHelper(
        blockEntityType,
        HeatSyncRegistries.BOILER_HEATER_BLOCK_ENTITY.get(),
        BoilerHeaterBlockEntity::tick,
    )

    companion object {
        val ACTIVE: BooleanProperty = BlockStateProperties.LIT
    }
}
