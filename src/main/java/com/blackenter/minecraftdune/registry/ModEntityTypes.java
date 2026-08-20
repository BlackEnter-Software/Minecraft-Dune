package com.blackenter.minecraftdune.registry;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.entity.DesertHareEntity;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    MinecraftDune.MOD_ID
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<DesertHareEntity>
    > DESERT_HARE = ENTITY_TYPES.register(
            "desert_hare",
            id -> EntityType.Builder.of(
                            DesertHareEntity::new,
                            MobCategory.CREATURE
                    )
                    .sized(0.45F, 0.45F)
                    .eyeHeight(0.31F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(id.toString())
    );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<MuaddibMouseEntity>
    > MUADDIB_MOUSE = ENTITY_TYPES.register(
            "muaddib_mouse",
            id -> EntityType.Builder.of(
                            MuaddibMouseEntity::new,
                            MobCategory.CREATURE
                    )
                    .sized(0.30F, 0.32F)
                    .eyeHeight(0.22F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(id.toString())
    );

    private ModEntityTypes() {
    }
}
