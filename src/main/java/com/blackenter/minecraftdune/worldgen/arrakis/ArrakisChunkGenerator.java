package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.world.level.block.DuneSandLayerBlock;
import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.blackenter.minecraftdune.worldgen.geology.MacroGeologyField;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.concurrent.CompletableFuture;

/**
 * Native Arrakis Dev terrain generator.
 *
 * <p>0.5.10 serializes the macro-terrain/dune profile into the generator codec. The normal
 * flat Arrakis base remains unchanged; geology and native dunes are still written directly
 * into ChunkAccess during generation.</p>
 */
public final class ArrakisChunkGenerator extends FlatLevelSource {
    public static final MapCodec<ArrakisChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    FlatLevelGeneratorSettings.CODEC
                            .fieldOf("settings")
                            .forGetter(ArrakisChunkGenerator::flatSettingsForCodec),
                    ArrakisTerrainSettings.CODEC
                            .optionalFieldOf(
                                    "terrain",
                                    ArrakisTerrainSettings.DEFAULT
                            )
                            .forGetter(ArrakisChunkGenerator::terrainSettings)
            ).apply(instance, ArrakisChunkGenerator::new));

    private static final BlockState ROCK_STATE =
            Blocks.STONE.defaultBlockState();
    private static final int FIRST_NATIVE_Y =
            MacroGeologyField.BASE_SURFACE_Y + 1;

    private final FlatLevelGeneratorSettings flatSettings;
    private final ArrakisTerrainSettings terrainSettings;

    private volatile long worldSeed;
    private volatile boolean worldSeedInitialized;

    public ArrakisChunkGenerator(FlatLevelGeneratorSettings settings) {
        this(settings, ArrakisTerrainSettings.DEFAULT);
    }

    public ArrakisChunkGenerator(
            FlatLevelGeneratorSettings settings,
            ArrakisTerrainSettings terrainSettings
    ) {
        super(settings);
        this.flatSettings = settings;
        this.terrainSettings = terrainSettings;
    }

    public ArrakisTerrainSettings terrainSettings() {
        return terrainSettings;
    }

    private FlatLevelGeneratorSettings flatSettingsForCodec() {
        return flatSettings;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(
            HolderLookup<StructureSet> structureSetLookup,
            RandomState randomState,
            long seed
    ) {
        worldSeed = seed;
        worldSeedInitialized = true;
        return super.createState(
                structureSetLookup,
                randomState,
                seed
        );
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        return super.fillFromNoise(
                blender,
                randomState,
                structureManager,
                chunk
        ).thenApply(this::applyNativeTerrain);
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState random
    ) {
        int flatHeight = super.getBaseHeight(
                x,
                z,
                type,
                level,
                random
        );
        if (!worldSeedInitialized) {
            return flatHeight;
        }

        TerrainColumn terrain = terrainColumn(x, z);
        int nativeHeight = Mth.clamp(
                terrain.highestOccupiedY() + 1,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight()
        );
        return Math.max(flatHeight, nativeHeight);
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor height,
            RandomState random
    ) {
        NoiseColumn column = super.getBaseColumn(
                x,
                z,
                height,
                random
        );
        if (!worldSeedInitialized) {
            return column;
        }

        TerrainColumn terrain = terrainColumn(x, z);
        int minimumY = height.getMinBuildHeight();
        int maximumY = height.getMaxBuildHeight() - 1;

        int firstRockY = Math.max(FIRST_NATIVE_Y, minimumY);
        int lastRockY = Math.min(terrain.rockTopY(), maximumY);
        for (int y = firstRockY; y <= lastRockY; y++) {
            column.setBlock(y, ROCK_STATE);
        }

        writeDuneColumn(
                terrain,
                minimumY,
                maximumY,
                column::setBlock
        );
        return column;
    }

    private ChunkAccess applyNativeTerrain(ChunkAccess chunk) {
        if (!worldSeedInitialized) {
            return chunk;
        }

        ChunkPos chunkPos = chunk.getPos();
        int minimumX = chunkPos.x << 4;
        int minimumZ = chunkPos.z << 4;
        int minimumY = chunk.getMinBuildHeight();
        int maximumY = chunk.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos position =
                new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minimumZ + localZ;

            for (int localX = 0; localX < 16; localX++) {
                int worldX = minimumX + localX;
                TerrainColumn terrain = terrainColumn(
                        worldX,
                        worldZ
                );

                int firstRockY = Math.max(
                        FIRST_NATIVE_Y,
                        minimumY
                );
                int lastRockY = Math.min(
                        terrain.rockTopY(),
                        maximumY
                );
                for (int y = firstRockY; y <= lastRockY; y++) {
                    position.set(worldX, y, worldZ);
                    chunk.setBlockState(
                            position,
                            ROCK_STATE,
                            false
                    );
                }

                writeDuneColumn(
                        terrain,
                        minimumY,
                        maximumY,
                        (y, state) -> {
                            position.set(worldX, y, worldZ);
                            chunk.setBlockState(
                                    position,
                                    state,
                                    false
                            );
                        }
                );
            }
        }

        return chunk;
    }

    private TerrainColumn terrainColumn(
            int worldX,
            int worldZ
    ) {
        MacroGeologyField.Sample geology =
                MacroGeologyField.sample(
                        worldSeed,
                        worldX + 0.5,
                        worldZ + 0.5,
                        terrainSettings
                );

        NativeTransverseDuneField.Sample dune =
                NativeTransverseDuneField.sample(
                        worldSeed,
                        worldX + 0.5,
                        worldZ + 0.5,
                        geology.duneSuitability(),
                        terrainSettings.nativeDunes()
                );

        int lastRockY = MacroGeologyField.BASE_SURFACE_Y
                + terrainSettings.massif().maxAddedHeight();
        int rockTopY = Mth.clamp(
                Mth.floor(geology.baseElevation() + 0.5),
                MacroGeologyField.BASE_SURFACE_Y,
                lastRockY
        );

        return new TerrainColumn(
                rockTopY,
                dune.surfaceUnits()
        );
    }

    private static void writeDuneColumn(
            TerrainColumn terrain,
            int minimumY,
            int maximumY,
            BlockWriter writer
    ) {
        if (terrain.duneSurfaceUnits() <= 0) {
            return;
        }

        int fullSandTopY = Math.min(
                terrain.duneFullTopY(),
                maximumY
        );
        int firstSandY = Math.max(
                Math.max(
                        FIRST_NATIVE_Y,
                        terrain.rockTopY() + 1
                ),
                minimumY
        );

        BlockState fullSand =
                ModBlocks.SAND.get().defaultBlockState();
        for (int y = firstSandY; y <= fullSandTopY; y++) {
            writer.set(y, fullSand);
        }

        int partialLayers = terrain.partialDuneLayers();
        int partialY = terrain.dunePartialY();
        if (partialLayers > 0
                && partialY > terrain.rockTopY()
                && partialY >= minimumY
                && partialY <= maximumY) {
            BlockState partialSand = ModBlocks.SAND_LAYER.get()
                    .defaultBlockState()
                    .setValue(
                            DuneSandLayerBlock.LAYERS,
                            partialLayers
                    );
            writer.set(partialY, partialSand);
        }
    }

    @FunctionalInterface
    private interface BlockWriter {
        void set(int y, BlockState state);
    }

    private record TerrainColumn(
            int rockTopY,
            int duneSurfaceUnits
    ) {
        int fullDuneBlocks() {
            return duneSurfaceUnits
                    / NativeTransverseDuneField.SUBDIVISIONS;
        }

        int partialDuneLayers() {
            return duneSurfaceUnits
                    % NativeTransverseDuneField.SUBDIVISIONS;
        }

        int duneFullTopY() {
            return MacroGeologyField.BASE_SURFACE_Y
                    + fullDuneBlocks();
        }

        int dunePartialY() {
            return MacroGeologyField.BASE_SURFACE_Y
                    + fullDuneBlocks()
                    + 1;
        }

        int highestOccupiedY() {
            int duneTopY = MacroGeologyField.BASE_SURFACE_Y;
            if (duneSurfaceUnits > 0) {
                duneTopY = duneFullTopY();
                if (partialDuneLayers() > 0) {
                    duneTopY++;
                }
            }
            return Math.max(rockTopY, duneTopY);
        }
    }
}
