package com.blackenter.minecraftdune.registry;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(
                    BuiltInRegistries.CHUNK_GENERATOR,
                    MinecraftDune.MOD_ID
            );

    public static final DeferredHolder<
            MapCodec<? extends ChunkGenerator>,
            MapCodec<ArrakisChunkGenerator>
    > ARRAKIS_DEV = CHUNK_GENERATORS.register(
            "arrakis_dev",
            () -> ArrakisChunkGenerator.CODEC
    );

    private ModChunkGenerators() {
    }
}
