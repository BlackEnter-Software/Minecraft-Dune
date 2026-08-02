package com.blackenter.minecraftdune.registry;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntityTypes {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(MinecraftDune.MOD_ID);

    public static final Supplier<EntityType<MuaddibMouseEntity>> MUADDIB_MOUSE =
            ENTITY_TYPES.registerEntityType(
                    "muaddib_mouse",
                    MuaddibMouseEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder
                            .sized(0.45F, 0.45F)
                            .eyeHeight(0.31F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
            );

    private ModEntityTypes() {
    }
}
