package com.bettercontent.betterindustrialheat.api;

import com.bettercontent.betterindustrialheat.food.FoodThermalService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Commit-time age transfer. Carriers reuse existing child clocks, never a new root food profile. */
public final class CookingAgeApi {
    public record Input(ItemStack stack, FoodThermalService.Storage storage) {
        public Input { Objects.requireNonNull(stack); Objects.requireNonNull(storage); }
    }
    public interface Carrier {
        boolean handles(ItemStack stack);
        /** Pure read of actual paid ingredients, not fabricated fresh representative foods. */
        List<ItemStack> read(ItemStack stack);
        /** Replace owned child payload only. Must not change item identity/count or unrelated NBT. */
        void write(ItemStack stack, List<ItemStack> children);
    }
    // Package-private for same-package test isolation; no public reset or reflection escape hatch.
    static final Map<ResourceLocation, Carrier> CARRIERS = new LinkedHashMap<>();
    static boolean frozen;
    private static final int MAX_DEPTH = 32, MAX_NODES = 4096;
    private CookingAgeApi() {}
    public static synchronized void registerCarrier(ResourceLocation owner, Carrier carrier) {
        if (frozen) throw new IllegalStateException("Cooking age carrier registration has closed");
        if (CARRIERS.putIfAbsent(Objects.requireNonNull(owner), Objects.requireNonNull(carrier)) != null)
            throw new IllegalArgumentException("Duplicate cooking age carrier: " + owner);
    }
    private static synchronized Carrier carrier(ItemStack stack) {
        frozen = true;
        Carrier found = null;
        for (Carrier carrier : CARRIERS.values()) {
            if (!carrier.handles(stack)) continue;
            if (stack.isEdible()) throw new IllegalArgumentException("An age carrier cannot be edible");
            if (found != null) throw new IllegalStateException("Overlapping cooking age carriers");
            found = carrier;
        }
        return found;
    }
    public static boolean isCarrier(ItemStack stack) { return !stack.isEmpty() && carrier(stack) != null; }

    private static final class Walk {
        private final Set<ItemStack> active = Collections.newSetFromMap(new IdentityHashMap<>());
        private int nodes;
        private final List<ItemStack> leaves = new ArrayList<>();
        ItemStack copy(ItemStack source, Consumer<ItemStack> leaf, int depth) {
            if (depth > MAX_DEPTH || ++nodes > MAX_NODES) throw new IllegalArgumentException("Cooking age carrier traversal bound exceeded");
            if (!active.add(source)) throw new IllegalArgumentException("Cyclic cooking age carrier");
            try {
                Carrier adapter = carrier(source);
                ItemStack result = source.copy();
                if (adapter != null) {
                    List<ItemStack> children = new ArrayList<>();
                    for (ItemStack child : Objects.requireNonNull(adapter.read(source)))
                        children.add(copy(Objects.requireNonNull(child), leaf, depth + 1));
                    adapter.write(result, List.copyOf(children));
                } else if (source.isEdible()) {
                    leaf.accept(result);
                    leaves.add(result);
                }
                return result;
            } finally { active.remove(source); }
        }
    }
    /** Settle copies of children atomically before replacing the root's owned payload.
     * No rotten conversion: preserve the real child decay record for the final transform. */
    public static boolean settleCarrier(ItemStack stack, FoodThermalService.Storage storage, long gameTime) {
        return updateCarrier(stack, child -> FoodThermalService.INSTANCE.settle(child, storage, gameTime));
    }
    /** Existing carried-food logout/login rebasing must recurse, otherwise offline time would age children. */
    public static boolean rebaseCarrier(ItemStack stack, long gameTime) {
        return updateCarrier(stack, child -> FoodThermalService.rebaseCookingClock(child, gameTime));
    }
    private static boolean updateCarrier(ItemStack stack, Consumer<ItemStack> operation) {
        Carrier adapter = carrier(stack);
        if (adapter == null) return false;
        ItemStack changed = new Walk().copy(stack, operation, 0);
        adapter.write(stack, List.copyOf(adapter.read(changed)));
        return true;
    }
    /** Settle every paid input under actual storage. Edible outputs receive the oldest
     * flattened child record, never average/yield-scaled age. Registered nonedible outputs
     * receive the settled paid input copies. No inputs are consumed or mutated here. */
    public static ItemStack transfer(List<Input> inputs, ItemStack output, long gameTime) {
        Objects.requireNonNull(inputs); Objects.requireNonNull(output);
        Walk walk = new Walk();
        List<ItemStack> settled = new ArrayList<>();
        for (Input input : inputs) settled.add(walk.copy(input.stack(),
                child -> FoodThermalService.INSTANCE.settle(child, input.storage(), gameTime), 0));
        Carrier adapter = carrier(output);
        if (adapter != null) adapter.write(output, List.copyOf(settled));
        else FoodThermalService.carryOldestCookingState(walk.leaves, output);
        return output;
    }
}
