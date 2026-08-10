package com.blackenter.minecraftdune.registry;

import com.blackenter.minecraftdune.MinecraftDune;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MinecraftDune.MOD_ID);

    public static final DeferredItem<DeferredSpawnEggItem> MUADDIB_MOUSE_SPAWN_EGG =
            ITEMS.registerItem(
                    "muaddib_mouse_spawn_egg",
                    properties -> new DeferredSpawnEggItem(
                            ModEntityTypes.MUADDIB_MOUSE,
                            0xB58E57,
                            0x5C412A,
                            properties
                    )
            );

    public static final DeferredItem<BlockItem> SAND =
            ITEMS.registerSimpleBlockItem("sand", ModBlocks.SAND);

    public static final DeferredItem<BlockItem> SAND_LAYER =
            ITEMS.registerSimpleBlockItem("sand_layer", ModBlocks.SAND_LAYER);

    private ModItems() {
    }
}
