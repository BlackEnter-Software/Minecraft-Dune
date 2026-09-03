package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.world.level.block.DuneSandLayerBlock;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainEvaluator.TerrainColumn;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.blackenter.minecraftdune.worldgen.geology.LithologyBlockPalette;
import com.blackenter.minecraftdune.worldgen.geology.LithologyField;
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
 * <p>Profile 6000 replaces the complete flat column with continuous geology, sediment and
 * erosion-derived deposits. The old native-rock pipeline remains isolated for legacy worlds.</p>
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

    private static final int FIRST_NATIVE_Y =
            MacroGeologyField.BASE_SURFACE_Y + 1;

    private final FlatLevelGeneratorSettings flatSettings;
    private final ArrakisTerrainSettings terrainSettings;
    private final LithologyBlockPalette lithologyPalette;

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
        lithologyPalette = new LithologyBlockPalette(
                terrainSettings.lithology().materials(),
                terrainSettings.additionalMaterials()
        );
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
            if (terrainSettings.isBuriedRock()) throw new IllegalStateException("Arrakis seed must be initialized before height queries");
            return flatHeight;
        }

        long startNanos = System.nanoTime();
        TerrainGenerationMetrics.Evaluation metrics = TerrainGenerationMetrics.evaluation();
        ArrakisTerrainEvaluator evaluation = new ArrakisTerrainEvaluator(
                worldSeed, terrainSettings, ArrakisTerrainEvaluator.QUERY_CACHE_LIMIT, metrics);
        int nativeHeight = terrainSettings.isBuriedRock()
                ? evaluation.buriedColumn(x, z).baseHeight(level.getMinBuildHeight(), level.getMaxBuildHeight(),
                        cell -> type.isOpaque().test(buriedState(cell)))
                : Mth.clamp(
                evaluation.highestOccupiedY(x, z) + 1,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight()
        );
        TerrainGenerationMetrics.recordQuery(
                "base-height",
                System.nanoTime() - startNanos,
                metrics,
                evaluation.size()
        );
        return terrainSettings.isBuriedRock() ? nativeHeight : Math.max(flatHeight, nativeHeight);
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor height,
            RandomState random
    ) {
        if (!worldSeedInitialized) {
            if (terrainSettings.isBuriedRock()) throw new IllegalStateException("Arrakis seed must be initialized before base-column queries");
            return super.getBaseColumn(x, z, height, random);
        }

        long startNanos = System.nanoTime();
        TerrainGenerationMetrics.Evaluation metrics = TerrainGenerationMetrics.evaluation();
        ArrakisTerrainEvaluator evaluation = new ArrakisTerrainEvaluator(
                worldSeed, terrainSettings, ArrakisTerrainEvaluator.QUERY_CACHE_LIMIT, metrics);
        int minimumY = height.getMinBuildHeight();
        int maximumY = height.getMaxBuildHeight() - 1;
        if (terrainSettings.isBuriedRock()) {
            NoiseColumn column = evaluation.buriedColumn(x, z).toNoiseColumn(minimumY, maximumY + 1, this::buriedState);
            TerrainGenerationMetrics.recordQuery("base-column", System.nanoTime() - startNanos, metrics, evaluation.size());
            return column;
        }
        NoiseColumn column = super.getBaseColumn(x, z, height, random);
        TerrainColumn terrain = evaluation.column(x, z);

        if (terrain.hasNativeRock()) {
            int foundationTopY = findFoundationTopY(
                    column,
                    minimumY
            );
            int firstRockY = Math.max(
                    foundationTopY + 1,
                    minimumY
            );
            int lastRockY = Math.min(
                    terrain.rockTopY(),
                    maximumY
            );
            for (int y = firstRockY; y <= lastRockY; y++) {
                LithologyField.Sample material = terrain.materialSampleAt(y);
                if (evaluation.filteredRockOccupies(
                        x,
                        z,
                        y,
                        terrain,
                        material
                )) {
                    column.setBlock(
                            y,
                            lithologyPalette.state(material.material())
                    );
                }
            }
        }

        writeTalusColumn(
                terrain,
                minimumY,
                maximumY,
                column::setBlock
        );
        writeBasalTalusApronColumn(
                evaluation, x, z,
                terrain,
                minimumY,
                maximumY,
                column::setBlock
        );
        writeDuneColumn(
                terrain,
                minimumY,
                maximumY,
                column::setBlock
        );
        TerrainGenerationMetrics.recordQuery(
                "base-column",
                System.nanoTime() - startNanos,
                metrics,
                evaluation.size()
        );
        return column;
    }

    private ChunkAccess applyNativeTerrain(ChunkAccess chunk) {
        if (!worldSeedInitialized) {
            if (terrainSettings.isBuriedRock()) throw new IllegalStateException("Arrakis seed must be initialized before terrain generation");
            return chunk;
        }

        long startNanos = System.nanoTime();
        TerrainGenerationMetrics.Evaluation metrics = TerrainGenerationMetrics.evaluation();
        ChunkPos chunkPos = chunk.getPos();
        int minimumX = chunkPos.x << 4;
        int minimumZ = chunkPos.z << 4;
        int minimumY = chunk.getMinBuildHeight();
        int maximumY = chunk.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos position =
                new BlockPos.MutableBlockPos();
        ArrakisTerrainEvaluator evaluation = new ArrakisTerrainEvaluator(
                worldSeed, terrainSettings, ArrakisTerrainEvaluator.CHUNK_CACHE_LIMIT, metrics);

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minimumZ + localZ;

            for (int localX = 0; localX < 16; localX++) {
                int worldX = minimumX + localX;
                if (terrainSettings.isBuriedRock()) {
                    writeBuriedColumn(evaluation.buriedColumn(worldX, worldZ), minimumY, maximumY, (y, state) -> {
                        position.set(worldX, y, worldZ);
                        chunk.setBlockState(position, state, false);
                    });
                    continue;
                }
                TerrainColumn terrain = evaluation.column(worldX, worldZ);

                if (terrain.hasNativeRock()) {
                    int foundationTopY = findFoundationTopY(
                            chunk,
                            position,
                            worldX,
                            worldZ,
                            minimumY
                    );
                    int firstRockY = Math.max(
                            foundationTopY + 1,
                            minimumY
                    );
                    int lastRockY = Math.min(
                            terrain.rockTopY(),
                            maximumY
                    );
                    for (int y = firstRockY; y <= lastRockY; y++) {
                        LithologyField.Sample material = terrain.materialSampleAt(y);
                        if (evaluation.filteredRockOccupies(
                                worldX,
                                worldZ,
                                y,
                                terrain,
                                material
                        )) {
                            position.set(worldX, y, worldZ);
                            chunk.setBlockState(
                                    position,
                                    lithologyPalette.state(material.material()),
                                    false
                            );
                        }
                    }
                }

                writeTalusColumn(
                        terrain,
                        minimumY,
                        maximumY,
                        (y, state) -> {
                            position.set(worldX, y, worldZ);
                            chunk.setBlockState(position, state, false);
                        }
                );
                writeBasalTalusApronColumn(
                        evaluation, worldX, worldZ,
                        terrain,
                        minimumY,
                        maximumY,
                        (y, state) -> {
                            position.set(worldX, y, worldZ);
                            chunk.setBlockState(position, state, false);
                        }
                );
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

        if (terrainSettings.isBuriedRock()) {
            Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        }
        TerrainGenerationMetrics.recordChunk(
                chunkPos,
                System.nanoTime() - startNanos,
                metrics,
                evaluation.size()
        );
        return chunk;
    }

    /** Both production writers consume this exact composition, including air above the roof. */
    private void writeBuriedColumn(BuriedTerrainColumn terrain, int minimumY, int maximumY, BlockWriter writer) {
        terrain.compose(minimumY, maximumY + 1, (y, cell) -> writer.set(y, buriedState(cell)));
    }

    private BlockState buriedState(BuriedTerrainColumn.Cell cell) {
        return switch (cell.kind()) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case ROCK, TALUS -> lithologyPalette.state(cell.material());
            case SAND -> ModBlocks.SAND.get().defaultBlockState();
            case SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
            case SAND_LAYER -> ModBlocks.SAND_LAYER.get().defaultBlockState().setValue(DuneSandLayerBlock.LAYERS, cell.layers());
        };
    }

    /**
     * Legacy only. Finds the highest existing hard-rock layer in the flat Arrakis base column. Native
     * geology then replaces every softer layer above it (currently sandstone + sand) with
     * coherent native lithology before continuing upward into the visible formation. This
     * roots rock bodies in the underlying crust instead of leaving a sand pocket beneath them.
     */
    private static int findFoundationTopY(
            NoiseColumn column,
            int minimumY
    ) {
        for (int y = MacroGeologyField.BASE_SURFACE_Y;
                y >= minimumY;
                y--) {
            if (isFoundationRock(column.getBlock(y))) {
                return y;
            }
        }
        return minimumY - 1;
    }

    static int findFoundationTopY(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos position,
            int worldX,
            int worldZ,
            int minimumY
    ) {
        for (int y = MacroGeologyField.BASE_SURFACE_Y;
                y >= minimumY;
                y--) {
            position.set(worldX, y, worldZ);
            if (isFoundationRock(chunk.getBlockState(position))) {
                return y;
            }
        }
        return minimumY - 1;
    }

    private static boolean isFoundationRock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.BEDROCK);
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
                        Math.max(
                                terrain.rockTopY(),
                                terrain.basalTalusApron().topY()
                        ) + 1
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
                && partialY > Math.max(
                        terrain.rockTopY(),
                        terrain.basalTalusApron().topY()
                )
                && !terrain.talusOccupiesY(partialY)
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

    private void writeBasalTalusApronColumn(
            ArrakisTerrainEvaluator evaluation, int x, int z,
            TerrainColumn terrain,
            int minimumY,
            int maximumY,
            BlockWriter writer
    ) {
        BasalTalusApronField.Sample apron = terrain.basalTalusApron();
        if (!apron.active() && !terrain.skirt().active()) {
            return;
        }

        int firstY = Math.max(
                minimumY,
                terrain.skirt().active() ? terrain.skirt().bottomY() : FIRST_NATIVE_Y
        );
        int lastY = Math.min(maximumY, Math.max(apron.topY(), terrain.skirt().topY()));
        for (int y = firstY; y <= lastY; y++) {
            BasalTalusApronField.Material material = evaluation.basalMaterialAt(x, y, z, terrain);
            switch (material) {
                case GRAVEL -> writer.set(
                        y,
                        lithologyPalette.state(LithologyField.Material.GRAVEL)
                );
                case SAND -> writer.set(
                        y,
                        ModBlocks.SAND.get().defaultBlockState()
                );
                case NONE -> {
                }
            }
        }
    }

    private void writeTalusColumn(
            TerrainColumn terrain,
            int minimumY,
            int maximumY,
            BlockWriter writer
    ) {
        int thickness = terrain.localTalusThickness();
        if (thickness <= 0) {
            return;
        }

        int firstTalusY = Math.max(
                minimumY,
                terrain.talusBaseY()
        );
        int lastTalusY = Math.min(
                maximumY,
                firstTalusY + thickness - 1
        );
        for (int y = firstTalusY; y <= lastTalusY; y++) {
            LithologyField.Material material = terrain.erosion().talusMaterialAt(
                    y,
                    terrain.lithology()
            );
            writer.set(y, lithologyPalette.state(material));
        }
    }

    @FunctionalInterface
    private interface BlockWriter {
        void set(int y, BlockState state);
    }

}
