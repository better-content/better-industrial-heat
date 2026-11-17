package com.bettercontent.betterindustrialheat.food;

import com.bettercontent.betterindustrialheat.api.CookingAgeApi;
import com.bettercontent.betterdrinkingwater.api.NativeDrinkProducedEvent;
import com.bettercontent.betterspicyfarmers.api.event.NativePlacedAgeEvent;
import com.bettercontent.betterspicyfarmers.seasoning.KaleidoscopePlacedFood;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Existing Heat age transfer; only actual paid native sources, not catalogue representatives. */
public final class NativeAgeOwners {
    private static final String CHILDREN = "better_industrial_heat:native_cup_children";
    private static final Set<String> CUPS = Set.of("kaleidoscope_cookery:clay_pot_milk_tea", "kaleidoscope_cookery:barley_tea", "kaleidoscope_cookery:tieguanyin", "kaleidoscope_cookery:biluochun", "kaleidoscope_cookery:oolong", "kaleidoscope_cookery:sakura_fubuki", "kaleidoscope_cookery:butter_tea", "kaleidoscope_cookery:flower_tea", "kaleidoscope_cookery:mystery_tea");
    private NativeAgeOwners() {}
    public static void register() {
        CookingAgeApi.registerCarrier(new ResourceLocation("better_industrial_heat", "native_drink_cups"), new CookingAgeApi.Carrier() {
            public boolean handles(ItemStack stack) {
                var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                return id != null && CUPS.contains(id.toString());
            }
            public List<ItemStack> read(ItemStack stack) {
                if (!stack.hasTag() || !stack.getTag().contains(CHILDREN)) return List.of();
                if (!stack.getTag().contains(CHILDREN, Tag.TAG_LIST)) throw new IllegalArgumentException("Corrupt native cup age children");
                ListTag saved = stack.getTag().getList(CHILDREN, Tag.TAG_COMPOUND);
                if (saved.size() > 10) throw new IllegalArgumentException("Native cup age children exceed paid inputs");
                List<ItemStack> result = new ArrayList<>();
                for (Tag row : saved) {
                    ItemStack child = ItemStack.of((CompoundTag) row);
                    if (child.isEmpty()) throw new IllegalArgumentException("Empty paid age child");
                    result.add(child);
                }
                return List.copyOf(result);
            }
            public void write(ItemStack stack, List<ItemStack> children) {
                if (children.size() > 10) throw new IllegalArgumentException("Native cup paid age children exceeded");
                ListTag saved = new ListTag();
                for (ItemStack child : children) saved.add(child.save(new CompoundTag()));
                stack.getOrCreateTag().put(CHILDREN, saved);
            }
        });
        if (ModList.get().isLoaded("better_drinking_water")) MinecraftForge.EVENT_BUS.register(NativeAgeOwners.class);
        if (ModList.get().isLoaded("better_spicy_farmers") && ModList.get().isLoaded("kaleidoscope_cookery"))
            MinecraftForge.EVENT_BUS.register(Placed.class);
    }
    private static final class Placed {
        @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) return;
            var state = event.getPlacedBlock();
            int remaining = KaleidoscopePlacedFood.remaining(state);
            if (remaining < 1) return;
            var anchor = KaleidoscopePlacedFood.anchor(event.getPos(), state);
            if (!anchor.equals(event.getPos())) return;
            ItemStack original = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (original.isEmpty() || !original.is(state.getBlock().asItem())) return;
            try { NativePlacedAgeData.get(level).placed(anchor, state, remaining, original); }
            catch (RuntimeException unsupported) {
                com.mojang.logging.LogUtils.getLogger().warn("Native placed food age original not preserved at {}; KC placement remains native", anchor, unsupported);
            }
        }
        @SubscribeEvent public static void paidBite(NativePlacedAgeEvent event) {
            ItemStack portion = event.bite();
            if (!(portion.getItem() instanceof BlockItem block)) return;
            NativePlacedAgeData.get(event.level()).debit(event.anchor(), block.getBlock().defaultBlockState(), event.before())
                .ifPresent(original -> CookingAgeApi.transfer(List.of(new CookingAgeApi.Input(original,
                        FoodThermalService.Storage.AMBIENT)), portion, event.level().getGameTime()));
        }
        @SubscribeEvent public static void broken(BlockEvent.BreakEvent event) {
            if (event.getLevel() instanceof ServerLevel level && KaleidoscopePlacedFood.remaining(event.getState()) > 0)
                NativePlacedAgeData.get(level).clear(KaleidoscopePlacedFood.anchor(event.getPos(), event.getState()));
        }
    }
    @SubscribeEvent public static void nativeDrink(NativeDrinkProducedEvent event) {
        CookingAgeApi.transfer(event.paid().stream().map(stack ->
                new CookingAgeApi.Input(stack, FoodThermalService.Storage.AMBIENT)).toList(),
                event.result(), event.level().getGameTime());
    }
}
