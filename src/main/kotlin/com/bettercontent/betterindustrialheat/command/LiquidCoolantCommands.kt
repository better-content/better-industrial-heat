package com.bettercontent.betterindustrialheat.command

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.content.coolant.CoolantExchangerBlockEntity
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidType
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.registries.ForgeRegistries

object HeatSyncCommands {
    private val HOT_WATER_ID = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "hot_water")
    private const val DEMO_NAME = HeatSyncMod.MOD_ID

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("demo")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal(DEMO_NAME)
                        .executes { placeTestRig(it.source) },
                ),
        )
    }

    private fun placeTestRig(source: CommandSourceStack): Int {
        val player = source.playerOrException
        val level = source.level
        val facing = horizontalFacing(player)
        val right = facing.clockWise
        val origin = player.blockPosition().relative(facing, 4)

        val heatPipe = requireBlock("better_industrial_heat:heat_pipe")
        val fluidTank = requireBlock("create:fluid_tank")
        val fluidPipe = requireBlock("create:fluid_pipe")
        val mechanicalPump = requireBlock("create:mechanical_pump")
        val cogwheel = requireBlock("create:cogwheel")
        val creativeMotor = requireBlock("create:creative_motor")

        val coldTankPos = origin
        val coldTopPipePos = coldTankPos.relative(right)
        val coldTopPumpPos = coldTankPos.relative(right, 2)
        val topMidPipePos = coldTankPos.relative(right, 3)
        val hotExchangerPos = coldTankPos.relative(right, 4)
        val topHotPipePos = coldTankPos.relative(right, 5)
        val hotTopPumpPos = coldTankPos.relative(right, 6)
        val hotTopPipePos = coldTankPos.relative(right, 7)
        val hotTankPos = coldTankPos.relative(right, 8)

        val bottomLeftPos = coldTankPos.relative(facing, 4)
        val coldBottomPipePos = bottomLeftPos.relative(right)
        val coldBottomPumpPos = bottomLeftPos.relative(right, 2)
        val bottomMidPipePos = bottomLeftPos.relative(right, 3)
        val coldExchangerPos = bottomLeftPos.relative(right, 4)
        val bottomHotPipePos = bottomLeftPos.relative(right, 5)
        val hotBottomPumpPos = bottomLeftPos.relative(right, 6)
        val hotBottomPipePos = bottomLeftPos.relative(right, 7)

        val leftVertical = listOf(coldTankPos.relative(facing), coldTankPos.relative(facing, 2), coldTankPos.relative(facing, 3))
        val rightVertical = listOf(hotTankPos.relative(facing), hotTankPos.relative(facing, 2), hotTankPos.relative(facing, 3))

        val topCogSide = facing.opposite
        val bottomCogSide = facing

        val coldTopCogPos = coldTopPumpPos.relative(topCogSide)
        val coldTopMotorPos = coldTopCogPos.relative(right.opposite)
        val hotTopCogPos = hotTopPumpPos.relative(topCogSide)
        val hotTopMotorPos = hotTopCogPos.relative(right)
        val coldBottomCogPos = coldBottomPumpPos.relative(bottomCogSide)
        val coldBottomMotorPos = coldBottomCogPos.relative(right.opposite)
        val hotBottomCogPos = hotBottomPumpPos.relative(bottomCogSide)
        val hotBottomMotorPos = hotBottomCogPos.relative(right)

        val hotHeatPipePos = hotExchangerPos.relative(topCogSide)
        val hotSourcePos = hotExchangerPos.relative(topCogSide, 2)
        val coldHeatPipePos = coldExchangerPos.relative(bottomCogSide)
        val coldSourcePos = coldExchangerPos.relative(bottomCogSide, 2)

        placeTank(level, coldTankPos, fluidTank)
        placeTank(level, hotTankPos, fluidTank)

        placeBlock(level, coldTopPipePos, fluidPipe)
        placePump(level, coldTopPumpPos, mechanicalPump, right)
        placeBlock(level, topMidPipePos, fluidPipe)
        placeBlock(level, hotExchangerPos, HeatSyncRegistries.COOLANT_EXCHANGER.get())
        placeBlock(level, topHotPipePos, fluidPipe)
        placePump(level, hotTopPumpPos, mechanicalPump, right)
        placeBlock(level, hotTopPipePos, fluidPipe)

        placeBlock(level, coldBottomPipePos, fluidPipe)
        placePump(level, coldBottomPumpPos, mechanicalPump, right.opposite)
        placeBlock(level, bottomMidPipePos, fluidPipe)
        placeBlock(level, coldExchangerPos, HeatSyncRegistries.COOLANT_EXCHANGER.get())
        placeBlock(level, bottomHotPipePos, fluidPipe)
        placePump(level, hotBottomPumpPos, mechanicalPump, right.opposite)
        placeBlock(level, hotBottomPipePos, fluidPipe)

        leftVertical.forEach { placeBlock(level, it, fluidPipe) }
        rightVertical.forEach { placeBlock(level, it, fluidPipe) }

        placeCog(level, coldTopCogPos, cogwheel, right.axis)
        placeMotor(level, coldTopMotorPos, creativeMotor, right)
        placeCog(level, hotTopCogPos, cogwheel, right.axis)
        placeMotor(level, hotTopMotorPos, creativeMotor, right.opposite)
        placeCog(level, coldBottomCogPos, cogwheel, right.axis)
        placeMotor(level, coldBottomMotorPos, creativeMotor, right)
        placeCog(level, hotBottomCogPos, cogwheel, right.axis)
        placeMotor(level, hotBottomMotorPos, creativeMotor, right.opposite)

        placeBlock(level, hotHeatPipePos, heatPipe)
        placeBlock(level, hotSourcePos, HeatSyncRegistries.CREATIVE_HEAT_SOURCE.get())
        placeBlock(level, coldHeatPipePos, heatPipe)
        placeBlock(level, coldSourcePos, HeatSyncRegistries.CREATIVE_COLD_SOURCE.get())

        seedFluidHandler(hotExchangerPos, player, FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME))
        seedFluidHandler(coldExchangerPos, player, FluidStack(HeatSyncRegistries.hotFluid(HOT_WATER_ID), FluidType.BUCKET_VOLUME))
        seedFluidHandler(coldTankPos, player, FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME * 4))
        seedFluidHandler(hotTankPos, player, FluidStack(HeatSyncRegistries.hotFluid(HOT_WATER_ID), FluidType.BUCKET_VOLUME * 4))

        player.addItem(net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET, 2))
        player.addItem(net.minecraft.world.item.ItemStack(HeatSyncRegistries.hotBucket(HOT_WATER_ID), 2))
        player.addItem(net.minecraft.world.item.ItemStack(requireItem("create:fluid_pipe"), 16))
        player.addItem(net.minecraft.world.item.ItemStack(requireItem("create:cogwheel"), 4))

        source.sendSuccess(
            {
                Component.literal(
                    "Placed Liquid Coolant 2D rig at ${origin.x} ${origin.y} ${origin.z}. " +
                        "Two tanks only: top row heats water into the hot tank, bottom row cools hot water back into the cold tank.",
                )
            },
            true,
        )
        return Command.SINGLE_SUCCESS
    }

    private fun seedFluidHandler(pos: BlockPos, player: ServerPlayer, stack: FluidStack) {
        val level = player.serverLevel()
        val blockEntity = level.getBlockEntity(pos)
            ?: error("Block entity missing at $pos")
        if (blockEntity is CoolantExchangerBlockEntity) {
            blockEntity.setHeat(0f)
        }
        val handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER)
            .resolve()
            .orElseThrow { IllegalStateException("Fluid handler missing at $pos") }
        val inserted = handler.fill(stack, IFluidHandler.FluidAction.EXECUTE)
        require(inserted == stack.amount) { "Expected to insert ${stack.amount} mB but inserted $inserted at $pos" }
    }

    private fun placeBlock(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, block: Block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState())
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState())
    }

    private fun placeBlock(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, state: BlockState) {
        level.setBlockAndUpdate(pos, state)
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState())
    }

    private fun placePump(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, block: Block, facing: Direction) {
        placeBlock(
            level,
            pos,
            block.defaultBlockState().setValue(BlockStateProperties.FACING, facing),
        )
    }

    private fun placeMotor(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, block: Block, facing: Direction) {
        placeBlock(
            level,
            pos,
            block.defaultBlockState().setValue(BlockStateProperties.FACING, facing),
        )
    }

    private fun placeCog(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, block: Block, axis: Direction.Axis) {
        placeBlock(
            level,
            pos,
            block.defaultBlockState().setValue(BlockStateProperties.AXIS, axis),
        )
    }

    private fun placeTank(level: net.minecraft.server.level.ServerLevel, pos: BlockPos, block: Block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState())
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState())
    }

    private fun requireBlock(id: String): Block {
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id))
            ?: error("Required block not found: $id")
    }

    private fun requireItem(id: String): net.minecraft.world.item.Item {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))
            ?: error("Required item not found: $id")
    }

    private fun horizontalFacing(player: ServerPlayer): Direction {
        val facing = player.direction
        return if (facing.axis.isHorizontal) facing else Direction.NORTH
    }
}
