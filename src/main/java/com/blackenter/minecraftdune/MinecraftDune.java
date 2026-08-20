package com.blackenter.minecraftdune;

import com.blackenter.minecraftdune.entity.DesertHareEntity;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.registry.ModChunkGenerators;
import com.blackenter.minecraftdune.registry.ModEntityTypes;
import com.blackenter.minecraftdune.registry.ModItems;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisChunkGenerator;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
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
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(this::addCreativeTabContents);
        NeoForge.EVENT_BUS.addListener(this::restrictArrakisNaturalSpawns);
        NeoForge.EVENT_BUS.addListener(this::restrictArrakisWorldDrivenSpawns);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier desertHareAttributes = DesertHareEntity.createAttributes().build();
        AttributeSupplier muaddibAttributes = MuaddibMouseEntity.createAttributes().build();
        event.put(ModEntityTypes.DESERT_HARE.get(), desertHareAttributes);
        event.put(ModEntityTypes.MUADDIB_MOUSE.get(), muaddibAttributes);
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntityTypes.DESERT_HARE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Rabbit.checkRabbitSpawnRules(EntityType.RABBIT, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                ModEntityTypes.MUADDIB_MOUSE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        Rabbit.checkRabbitSpawnRules(EntityType.RABBIT, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    /**
     * Arrakis uses its own sparse fauna table. The generator check also protects old
     * Arrakis saves whose serialized biome holder still points at minecraft:desert.
     * Spawn eggs and commands remain available because they bypass placement checks.
     */
    private void restrictArrakisNaturalSpawns(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!(event.getLevel().getLevel().getChunkSource().getGenerator()
                instanceof ArrakisChunkGenerator)) {
            return;
        }

        EntityType<?> entityType = event.getEntityType();
        if (entityType != ModEntityTypes.DESERT_HARE.get()
                && entityType != ModEntityTypes.MUADDIB_MOUSE.get()) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    /**
     * Catches autonomous spawn paths that can bypass normal placement checks, such as
     * patrols, events and spawners. Explicit player/testing paths stay available.
     */
    private void restrictArrakisWorldDrivenSpawns(FinalizeSpawnEvent event) {
        if (!(event.getLevel().getLevel().getChunkSource().getGenerator()
                instanceof ArrakisChunkGenerator)
                || isArrakisFauna(event.getEntity().getType())
                || isDeliberateSpawn(event.getSpawnType())) {
            return;
        }

        event.setSpawnCancelled(true);
    }

    private static boolean isArrakisFauna(EntityType<?> entityType) {
        return entityType == ModEntityTypes.DESERT_HARE.get()
                || entityType == ModEntityTypes.MUADDIB_MOUSE.get();
    }

    private static boolean isDeliberateSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.COMMAND
                || spawnType == MobSpawnType.SPAWN_EGG
                || spawnType == MobSpawnType.BUCKET
                || spawnType == MobSpawnType.DISPENSER
                || spawnType == MobSpawnType.BREEDING;
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
