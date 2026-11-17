package com.bettercontent.betterindustrialheat.food;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Existing whole placed original's age, never an extra edible serving or independent clock. */
public final class NativePlacedAgeData extends SavedData {
    private static final int MAX_ROWS = 4096, MAX_BYTES = 65536;
    private record Row(String block, int remaining, ItemStack original) {}
    private final Map<Long, Row> rows = new HashMap<>();
    private static String id(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    public static NativePlacedAgeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(NativePlacedAgeData::load, NativePlacedAgeData::new,
                "better_industrial_heat_native_placed_age");
    }
    private static void bound(CompoundTag tag) {
        final int[] bytes = {0};
        try { NbtIo.write(tag, new DataOutputStream(new OutputStream() {
            @Override public void write(int value) throws IOException { add(1); }
            @Override public void write(byte[] value, int off, int length) throws IOException { add(length); }
            private void add(int n) throws IOException { if ((long)bytes[0]+n > MAX_BYTES) throw new IOException("Oversized placed age original"); bytes[0] += n; }
        })); } catch (IOException invalid) { throw new IllegalArgumentException("Invalid placed age original", invalid); }
    }
    public void placed(BlockPos anchor, BlockState state, int remaining, ItemStack original) {
        if (remaining < 1 || remaining > 64 || original.isEmpty()) throw new IllegalArgumentException("Invalid placed age source");
        if (original.hasTag()) bound(original.getTag()); // before ItemStack.save/copy
        bound(original.save(new CompoundTag()));
        if (!rows.containsKey(anchor.asLong()) && rows.size() >= MAX_ROWS) throw new IllegalStateException("Placed age bound exceeded");
        rows.put(anchor.asLong(), new Row(id(state), remaining, original.copyWithCount(1))); setDirty();
    }
    public Optional<ItemStack> debit(BlockPos anchor, BlockState before, int remaining) {
        Row previous = rows.get(anchor.asLong());
        if (previous == null) return Optional.empty(); // unmanaged old/native placed food
        if (!previous.block().equals(id(before)) || previous.remaining() != remaining || remaining < 1)
            throw new IllegalStateException("Native placed age/native bite drift");
        if (remaining == 1) rows.remove(anchor.asLong());
        else rows.put(anchor.asLong(), new Row(previous.block(), remaining-1, previous.original()));
        setDirty();
        return Optional.of(previous.original().copy());
    }
    public void clear(BlockPos pos) { if (rows.remove(pos.asLong()) != null) setDirty(); }
    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("v", 1); ListTag entries = new ListTag();
        rows.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag tag = new CompoundTag(); tag.putLong("pos", entry.getKey());
            tag.putString("block", entry.getValue().block()); tag.putInt("remaining", entry.getValue().remaining());
            tag.put("original", entry.getValue().original().save(new CompoundTag())); entries.add(tag);
        });
        root.put("entries", entries); return root;
    }
    public static NativePlacedAgeData load(CompoundTag root) {
        if (root.getInt("v") != 1 || !root.contains("entries", Tag.TAG_LIST)) throw new IllegalArgumentException("Unknown placed age save");
        ListTag entries = root.getList("entries", Tag.TAG_COMPOUND);
        if (entries.size() > MAX_ROWS || entries.size() != ((ListTag)root.get("entries")).size()) throw new IllegalArgumentException("Invalid placed age rows");
        NativePlacedAgeData data = new NativePlacedAgeData();
        for (Tag tag : entries) {
            CompoundTag row = (CompoundTag)tag;
            if (!row.contains("pos",Tag.TAG_LONG) || !row.contains("block",Tag.TAG_STRING)
                    || !row.contains("remaining",Tag.TAG_INT) || !row.contains("original",Tag.TAG_COMPOUND))
                throw new IllegalArgumentException("Invalid saved age row");
            ResourceLocation block = ResourceLocation.tryParse(row.getString("block"));
            if (block == null || !ForgeRegistries.BLOCKS.containsKey(block)) throw new IllegalArgumentException("Unknown placed age block");
            bound(row.getCompound("original"));
            ItemStack original = ItemStack.of(row.getCompound("original"));
            if (original.isEmpty() || original.getCount() != 1 || row.getInt("remaining") < 1 || row.getInt("remaining") > 64)
                throw new IllegalArgumentException("Invalid saved placed age original");
            if (data.rows.putIfAbsent(row.getLong("pos"), new Row(block.toString(),row.getInt("remaining"),original)) != null)
                throw new IllegalArgumentException("Duplicate placed age row");
        }
        return data;
    }
}
