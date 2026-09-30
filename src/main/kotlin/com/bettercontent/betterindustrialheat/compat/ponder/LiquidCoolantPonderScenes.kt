package com.bettercontent.betterindustrialheat.compat.ponder

import com.bettercontent.betterindustrialheat.HeatSyncMod
import com.bettercontent.betterindustrialheat.HeatSyncRegistries
import com.bettercontent.betterindustrialheat.content.coolant.CoolantExchangerBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object HeatSyncPonderScenes {
    private val HOT_WATER_ID = ResourceLocation.fromNamespaceAndPath(HeatSyncMod.MOD_ID, "hot_water")

    fun coolantExchanger(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        val exchangerPos = util.grid().at(2, 1, 2)
        val heatPipe = util.select().position(2, 1, 1)
        val exchanger = util.select().position(exchangerPos)
        val coldPipe = util.select().position(3, 1, 1)
        val fullSetup = util.select().fromTo(2, 1, 1, 3, 2, 3)

        scene.title("coolant_exchanger", "Heating and cooling fluids")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(5)

        scene.world().showSection(util.select().layer(0), Direction.UP)
        scene.idle(10)
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 2, 3), Direction.DOWN)
        scene.idle(10)

        scene.world().setBlock(exchangerPos, HeatSyncRegistries.COOLANT_EXCHANGER.get().defaultBlockState(), false)
        scene.overlay().showText(80)
            .text("Connect a Coolant Exchanger to heat pipes and fill its tank with a supported coolant.")
            .colored(PonderPalette.WHITE)
            .pointAt(util.vector().topOf(exchangerPos))
            .placeNearTarget()
        scene.idle(90)

        scene.overlay().showOutline(PonderPalette.RED, "heat_input", heatPipe, 60)
        scene.overlay().showText(60)
            .text("Heat from connected pipes converts cold coolant into hot coolant.")
            .colored(PonderPalette.RED)
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 1), Direction.NORTH))
            .placeNearTarget()
        scene.world().modifyBlockEntity(exchangerPos, CoolantExchangerBlockEntity::class.java) {
            it.setHeat(700.0f)
        }
        scene.world().modifyBlockEntityNBT(exchanger, CoolantExchangerBlockEntity::class.java, { tag ->
            fillTank(tag, "minecraft:water")
        }, false)
        scene.idle(10)
        scene.world().modifyBlockEntityNBT(exchanger, CoolantExchangerBlockEntity::class.java, { tag ->
            fillTank(tag, "$HOT_WATER_ID")
        }, false)
        scene.idle(70)

        scene.addKeyframe()
        scene.overlay().showOutline(PonderPalette.BLUE, "cooling_loop", coldPipe, 60)
        scene.overlay().showText(70)
            .text("Hot coolant releases heat into the network and becomes cold coolant again, if the network can accept the heat.")
            .colored(PonderPalette.BLUE)
            .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 1), Direction.WEST))
            .placeNearTarget()
        scene.world().modifyBlockEntity(exchangerPos, CoolantExchangerBlockEntity::class.java) {
            it.setHeat(100.0f)
        }
        scene.idle(10)
        scene.world().modifyBlockEntityNBT(exchanger, CoolantExchangerBlockEntity::class.java, { tag ->
            fillTank(tag, HOT_WATER_ID.toString())
        }, false)
        scene.idle(80)

        scene.overlay().showControls(util.vector().topOf(exchangerPos), Pointing.DOWN, 50)
            .rightClick()
            .withItem(ItemStack(Items.WATER_BUCKET))
        scene.overlay().showText(70)
            .text("Use a bucket or fluid pipes to fill the tank. Water is one supported coolant.")
            .pointAt(util.vector().topOf(exchangerPos))
            .placeNearTarget()
        scene.idle(80)

        scene.overlay().showOutline(PonderPalette.GREEN, "full_setup", fullSetup, 80)
        scene.overlay().showText(80)
            .text("Keep the exchanger connected to heat pipes and provide fluid access. Different coolants transfer different amounts of heat.")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().centerOf(exchangerPos))
            .placeNearTarget()
        scene.idle(90)
    }

    private fun fillTank(tag: CompoundTag, fluidId: String) {
        val tankTag = CompoundTag()
        tankTag.putString("FluidName", fluidId)
        tankTag.putInt("Amount", 1000)
        tag.put("Tank", tankTag)
    }
}
