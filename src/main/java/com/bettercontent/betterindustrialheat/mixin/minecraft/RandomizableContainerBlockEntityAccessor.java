package com.bettercontent.betterindustrialheat.mixin.minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomizableContainerBlockEntity.class)
public interface RandomizableContainerBlockEntityAccessor {
    @Accessor("lootTable")
    ResourceLocation getHeatSyncLootTable();

    @Accessor("lootTable")
    void setHeatSyncLootTable(ResourceLocation lootTable);
}
