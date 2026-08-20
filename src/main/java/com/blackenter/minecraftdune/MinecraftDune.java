package com.blackenter.minecraftdune;

import com.blackenter.minecraftdune.entity.DesertHareEntity;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.registry.ModChunkGenerators;
import com.blackenter.minecraftdune.registry.ModEntityTypes;
import com.blackenter.minecraftdune.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(MinecraftDune.MOD_ID)
public final class MinecraftDune {
    public static final String MOD_ID = "minecraftdune";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftDune(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::addCreativeTabContents);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier desertHareAttributes = DesertHareEntity.createAttributes().build();
        AttributeSupplier muaddibAttributes = MuaddibMouseEntity.createAttributes().build();
        event.put(ModEntityTypes.DESERT_HARE.get(), desertHareAttributes);
        event.put(ModEntityTypes.MUADDIB_MOUSE.get(), muaddibAttributes);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.DESERT_HARE_SPAWN_EGG);
            event.accept(ModItems.MUADDIB_MOUSE_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.SAND);
            event.accept(ModItems.SAND_LAYER);
        }
    }
}
