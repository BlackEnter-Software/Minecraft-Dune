package com.blackenter.minecraftdune;

import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
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
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::addCreativeTabContents);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = MuaddibMouseEntity.createAttributes().build();
        event.put(ModEntityTypes.MUADDIB_MOUSE.get(), attributes);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.MUADDIB_MOUSE_SPAWN_EGG);
        }
    }
}
