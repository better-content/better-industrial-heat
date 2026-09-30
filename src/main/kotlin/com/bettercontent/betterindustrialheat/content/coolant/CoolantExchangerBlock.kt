package com.bettercontent.betterindustrialheat.content.coolant

import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.common.capabilities.ForgeCapabilities

class CoolantExchangerBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.COLOR_ORANGE)
        .strength(4.0f, 6.0f)
        .sound(SoundType.COPPER)
        .noOcclusion()
        .requiresCorrectToolForDrops(),
) {
    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(NORTH, InterfaceType.NONE)
                .setValue(EAST, InterfaceType.NONE)
                .setValue(SOUTH, InterfaceType.NONE)
                .setValue(WEST, InterfaceType.NONE)
                .setValue(UP, InterfaceType.NONE)
                .setValue(DOWN, InterfaceType.NONE),
        )
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState,
        placer: net.minecraft.world.entity.LivingEntity?, stack: net.minecraft.world.item.ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (placer is net.minecraft.server.level.ServerPlayer)
            (level.getBlockEntity(pos) as? CoolantExchangerBlockEntity)?.configuredBy(placer.uuid)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape {
        var shape = CORE_SHAPE
        for (direction in Direction.values()) {
            if (state.getValue(propertyFor(direction)) != InterfaceType.NONE) {
                shape = Shapes.or(shape, shapeFor(direction))
            }
        }
        return shape
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN)
    }

    override fun getStateForPlacement(context: net.minecraft.world.item.context.BlockPlaceContext): BlockState {
        return updateConnections(context.level, context.clickedPos, defaultBlockState())
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos,
    ): BlockState {
        return state.setValue(propertyFor(direction), detectInterface(level, neighborPos, direction.opposite))
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CoolantExchangerBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        return createTickerHelper(
            blockEntityType,
            HeatSyncRegistries.COOLANT_EXCHANGER_BLOCK_ENTITY.get(),
            CoolantExchangerBlockEntity::tick,
        )
    }

    private fun updateConnections(level: BlockGetter, pos: BlockPos, state: BlockState): BlockState {
        var updated = state
        for (direction in Direction.values()) {
            updated = updated.setValue(
                propertyFor(direction),
                detectInterface(level, pos.relative(direction), direction.opposite),
            )
        }
        return updated
    }

    private fun detectInterface(level: BlockGetter, neighborPos: BlockPos, side: Direction): InterfaceType {
        val neighbor = level.getBlockEntity(neighborPos) ?: return InterfaceType.NONE
        val hasFluid = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent
        val hasHeat = neighbor.getCapability(HeatCapabilities.HEAT, side).map { it.canConnect(side) }.orElse(false)
        return when {
            hasFluid -> InterfaceType.FLUID
            hasHeat -> InterfaceType.HEAT
            else -> InterfaceType.NONE
        }
    }

    private fun propertyFor(direction: Direction): EnumProperty<InterfaceType> = when (direction) {
        Direction.NORTH -> NORTH
        Direction.EAST -> EAST
        Direction.SOUTH -> SOUTH
        Direction.WEST -> WEST
        Direction.UP -> UP
        Direction.DOWN -> DOWN
    }

    private fun shapeFor(direction: Direction): VoxelShape = when (direction) {
        Direction.NORTH -> NORTH_SHAPE
        Direction.EAST -> EAST_SHAPE
        Direction.SOUTH -> SOUTH_SHAPE
        Direction.WEST -> WEST_SHAPE
        Direction.UP -> UP_SHAPE
        Direction.DOWN -> DOWN_SHAPE
    }

    enum class InterfaceType(private val id: String) : StringRepresentable {
        NONE("none"),
        FLUID("fluid"),
        HEAT("heat");

        override fun getSerializedName(): String = id
    }

    companion object {
        private val CORE_SHAPE: VoxelShape = box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0)
        private val NORTH_SHAPE: VoxelShape = box(4.0, 4.0, 0.0, 12.0, 12.0, 2.0)
        private val EAST_SHAPE: VoxelShape = box(14.0, 4.0, 4.0, 16.0, 12.0, 12.0)
        private val SOUTH_SHAPE: VoxelShape = box(4.0, 4.0, 14.0, 12.0, 12.0, 16.0)
        private val WEST_SHAPE: VoxelShape = box(0.0, 4.0, 4.0, 2.0, 12.0, 12.0)
        private val UP_SHAPE: VoxelShape = box(4.0, 14.0, 4.0, 12.0, 16.0, 12.0)
        private val DOWN_SHAPE: VoxelShape = box(4.0, 0.0, 4.0, 12.0, 2.0, 12.0)

        val NORTH: EnumProperty<InterfaceType> = EnumProperty.create("north", InterfaceType::class.java)
        val EAST: EnumProperty<InterfaceType> = EnumProperty.create("east", InterfaceType::class.java)
        val SOUTH: EnumProperty<InterfaceType> = EnumProperty.create("south", InterfaceType::class.java)
        val WEST: EnumProperty<InterfaceType> = EnumProperty.create("west", InterfaceType::class.java)
        val UP: EnumProperty<InterfaceType> = EnumProperty.create("up", InterfaceType::class.java)
        val DOWN: EnumProperty<InterfaceType> = EnumProperty.create("down", InterfaceType::class.java)
    }
}
