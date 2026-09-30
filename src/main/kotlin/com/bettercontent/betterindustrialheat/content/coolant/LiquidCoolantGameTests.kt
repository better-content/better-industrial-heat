package com.bettercontent.betterindustrialheat.content.coolant

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidType
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(HeatSyncMod.MOD_ID)
@PrefixGameTestTemplate(false)
class LiquidCoolantGameTests {
    private val hotWaterId = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "hot_water")

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun heatsWaterIntoHotWater(helper: GameTestHelper) {
        val exchangerPos = BlockPos(1, 1, 1)
        val blockEntity = placeExchanger(helper, exchangerPos)

        blockEntity.setHeat(500.0f)
        fillTank(blockEntity, FluidStack(net.minecraft.world.level.material.Fluids.WATER, FluidType.BUCKET_VOLUME))
        tick(helper, exchangerPos, blockEntity)

        helper.succeedIf {
            val tankFluid = blockEntity.fluidHandler().getFluidInTank(0)
            helper.assertTrue(
                tankFluid.fluid === HeatSyncRegistries.hotFluid(hotWaterId),
                "Expected hot water after heating, found ${tankFluid.fluid.fluidType.descriptionId}",
            )
            helper.assertTrue(blockEntity.getHeat() == 100.0f, "Expected remaining heat to be 100.0, was ${blockEntity.getHeat()}")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun coolsHotWaterIntoWater(helper: GameTestHelper) {
        val exchangerPos = BlockPos(1, 1, 1)
        val blockEntity = placeExchanger(helper, exchangerPos)

        blockEntity.setHeat(100.0f)
        fillTank(blockEntity, FluidStack(HeatSyncRegistries.hotFluid(hotWaterId), FluidType.BUCKET_VOLUME))
        tick(helper, exchangerPos, blockEntity)

        helper.succeedIf {
            val tankFluid = blockEntity.fluidHandler().getFluidInTank(0)
            helper.assertTrue(
                tankFluid.fluid === net.minecraft.world.level.material.Fluids.WATER,
                "Expected water after cooling, found ${tankFluid.fluid.fluidType.descriptionId}",
            )
            helper.assertTrue(blockEntity.getHeat() == 420.0f, "Expected remaining heat to be 420.0, was ${blockEntity.getHeat()}")
        }
    }

    @GameTest(template = "coolant_exchanger", timeoutTicks = 20)
    fun recoveryRetainsPlacingOwnerAcrossSaveWhileOffline(helper: GameTestHelper) {
        val relative = BlockPos(1, 1, 1)
        val pos = helper.absolutePos(relative)
        helper.level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
        val owner = java.util.UUID.randomUUID()
        val player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.level,
            com.mojang.authlib.GameProfile(owner, "coolant-owner"))
        player.setPos(pos.x + 2.0, pos.y.toDouble(), pos.z + 2.0)
        val stack = net.minecraft.world.item.ItemStack(HeatSyncRegistries.COOLANT_EXCHANGER.get(), 2)
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack)
        val hit = net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(pos.below()).add(0.0, 0.5, 0.0), net.minecraft.core.Direction.UP, pos.below(), false)
        val placed = stack.useOn(net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit))
        helper.assertTrue(placed.consumesAction() && stack.count == 1, "Native placement must commit and consume one block")
        val exchanger = helper.getBlockEntity(relative) as CoolantExchangerBlockEntity
        val capture = RecoveryCapture(pos)
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(capture)
        try {
            exchanger.setHeat(900f)
            tick(helper, relative, exchanger)
            val saved = exchanger.saveWithoutMetadata()
            helper.assertTrue(saved.getUUID("CoolantOperator") == owner && saved.hasUUID("CoolantSafetyOwner"),
                "Owner and actual overheat episode must persist without a nearby player")
            helper.assertTrue(helper.level.server.playerList.getPlayer(owner) == null, "Fixture owner must be offline from player list")
            exchanger.load(saved)
            helper.assertTrue(capture.events.isEmpty(), "Overheat alone must not report recovery")
            fillTank(exchanger, FluidStack(net.minecraft.world.level.material.Fluids.WATER, FluidType.BUCKET_VOLUME))
            tick(helper, relative, exchanger)
            helper.assertTrue(exchanger.getHeat() == 500f && exchanger.fluidHandler().getFluidInTank(0).fluid === HeatSyncRegistries.hotFluid(hotWaterId),
                "Recovery requires committed native fluid conversion and a safe remaining heat buffer")
            helper.assertTrue(capture.events.size == 1 && capture.events.single().owner == owner,
                "Actual recovery must immediately credit the persisted owner while offline")
            tick(helper, relative, exchanger)
            helper.assertTrue(capture.events.size == 1 && !exchanger.saveWithoutMetadata().contains("CoolantSafetyEpisode"),
                "A resolved episode must not replay on the next tick or survive a new save")
        } finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(capture) }
        helper.succeed()
    }

    class RecoveryCapture(private val pos: BlockPos) {
        val events = mutableListOf<com.bettercontent.betterindustrialheat.api.event.CoolantRecoveryEvent>()
        @net.minecraftforge.eventbus.api.SubscribeEvent
        fun recovered(event: com.bettercontent.betterindustrialheat.api.event.CoolantRecoveryEvent) {
            if (event.position == pos) events.add(event)
        }
    }

    private fun placeExchanger(helper: GameTestHelper, pos: BlockPos): CoolantExchangerBlockEntity {
        helper.setBlock(pos, HeatSyncRegistries.COOLANT_EXCHANGER.get())
        val blockEntity = helper.getBlockEntity(pos) as? CoolantExchangerBlockEntity
        return requireNotNull(blockEntity) { "Coolant exchanger block entity was not created at $pos" }
    }

    private fun fillTank(blockEntity: CoolantExchangerBlockEntity, stack: FluidStack) {
        val filled = blockEntity.fluidHandler().fill(stack, IFluidHandler.FluidAction.EXECUTE)
        require(filled == stack.amount) { "Expected to insert ${stack.amount} mB but inserted $filled mB" }
    }

    private fun tick(helper: GameTestHelper, pos: BlockPos, blockEntity: CoolantExchangerBlockEntity) {
        CoolantExchangerBlockEntity.tick(helper.level, pos, helper.getBlockState(pos), blockEntity)
    }

    private fun CoolantExchangerBlockEntity.fluidHandler(): IFluidHandler {
        return getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)
            .resolve()
            .orElseThrow { IllegalStateException("Coolant exchanger fluid capability was unavailable") }
    }
}
