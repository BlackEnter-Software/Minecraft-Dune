package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.MacroGeologyField;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.ChunkPos;
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
 * <p>The existing Arrakis Dev flat stratigraphy remains the base terrain. During the normal
 * chunk NOISE/fill stage, the deterministic {@link MacroGeologyField} is sampled using the
 * actual world seed and absolute X/Z coordinates and the current 0.5.7 macro-rock mass is
 * written directly into {@link ChunkAccess}. No ServerLevel#setBlock post-generation pass
 * is involved.</p>
 *
 * <p>This 0.5.8 generator intentionally preserves the 0.5.7 macro-geology mathematics.
 * Geological restructuring, faults, sand passes, outlier provinces, strata and erosion are
 * later morphology work.</p>
 */
public final class ArrakisChunkGenerator extends FlatLevelSource {
    public static final MapCodec<ArrakisChunkGenerator> CODEC =
            FlatLevelGeneratorSettings.CODEC
                    .fieldOf("settings")
                    .xmap(
                            ArrakisChunkGenerator::new,
                            ArrakisChunkGenerator::settings
                    );

    private static final BlockState ROCK_STATE = Blocks.STONE.defaultBlockState();
    private static final int FIRST_ROCK_Y = MacroGeologyField.BASE_SURFACE_Y + 1;
    private static final int LAST_ROCK_Y =
            MacroGeologyField.BASE_SURFACE_Y + MacroGeologyField.MAX_ADDED_ROCK_HEIGHT;

    /**
     * Chunk-generator codecs do not carry the selected world's seed. Vanilla supplies the
     * actual level seed when the generator structure state is created, before terrain
     * generation begins. Store that value once for the coordinate field.
     */
    private volatile long worldSeed;
    private volatile boolean worldSeedInitialized;

    public ArrakisChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
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
        return super.createState(structureSetLookup, randomState, seed);
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
        ).thenApply(this::applyNativeMacroGeology);
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState random
    ) {
        int flatHeight = super.getBaseHeight(x, z, type, level, random);
        if (!worldSeedInitialized) {
            return flatHeight;
        }

        int geologyHeight = targetTopY(x, z) + 1;
        geologyHeight = Mth.clamp(
                geologyHeight,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight()
        );
        return Math.max(flatHeight, geologyHeight);
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor height,
            RandomState random
    ) {
        NoiseColumn column = super.getBaseColumn(x, z, height, random);
        if (!worldSeedInitialized) {
            return column;
        }

        int firstY = Math.max(FIRST_ROCK_Y, height.getMinBuildHeight());
        int lastY = Math.min(targetTopY(x, z), height.getMaxBuildHeight() - 1);

        for (int y = firstY; y <= lastY; y++) {
            column.setBlock(y, ROCK_STATE);
        }

        return column;
    }

    private ChunkAccess applyNativeMacroGeology(ChunkAccess chunk) {
        if (!worldSeedInitialized) {
            return chunk;
        }

        ChunkPos chunkPos = chunk.getPos();
        int minimumX = chunkPos.x << 4;
        int minimumZ = chunkPos.z << 4;
        int firstY = Math.max(FIRST_ROCK_Y, chunk.getMinBuildHeight());
        int maximumY = Math.min(LAST_ROCK_Y, chunk.getMaxBuildHeight() - 1);

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minimumZ + localZ;

            for (int localX = 0; localX < 16; localX++) {
                int worldX = minimumX + localX;
                int targetTopY = Math.min(targetTopY(worldX, worldZ), maximumY);

                for (int y = firstY; y <= targetTopY; y++) {
                    position.set(worldX, y, worldZ);
                    chunk.setBlockState(position, ROCK_STATE, false);
                }
            }
        }

        return chunk;
    }

    private int targetTopY(int worldX, int worldZ) {
        MacroGeologyField.Sample sample = MacroGeologyField.sample(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5
        );

        return Mth.clamp(
                Mth.floor(sample.baseElevation() + 0.5),
                MacroGeologyField.BASE_SURFACE_Y,
                LAST_ROCK_Y
        );
    }
}
