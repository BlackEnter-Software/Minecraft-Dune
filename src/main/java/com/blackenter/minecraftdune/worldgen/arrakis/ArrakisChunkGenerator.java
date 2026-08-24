package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.world.level.block.DuneSandLayerBlock;
import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.blackenter.minecraftdune.worldgen.geology.EscarpmentErosionField;
import com.blackenter.minecraftdune.worldgen.geology.LithologyBlockPalette;
import com.blackenter.minecraftdune.worldgen.geology.LithologyField;
import com.blackenter.minecraftdune.worldgen.geology.MacroGeologyField;
import com.blackenter.minecraftdune.worldgen.geology.MassifFractureField;
import com.blackenter.minecraftdune.worldgen.geology.OrphanRemnantFilter;
import com.blackenter.minecraftdune.worldgen.geology.RockFaceExposure;
import com.blackenter.minecraftdune.worldgen.geology.RockSurfaceErosionField;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Native Arrakis Dev terrain generator.
 *
 * <p>The normal flat Arrakis base remains unchanged. Macro relief, 3D lithology, massif-top
 * fissures, per-Y escarpment occupancy, supported talus and native dunes are written directly
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
            return flatHeight;
        }

        Map<Long, TerrainColumn> terrainCache = new HashMap<>(16);
        Map<Long, BasalTalusApronField.RockColumn> contactCache = new HashMap<>(16);
        TerrainColumn terrain = terrainColumnWithBasalTalusCached(
                x,
                z,
                terrainCache,
                contactCache
        );
        int nativeHeight = Mth.clamp(
                highestFilteredOccupiedY(x, z, terrain, terrainCache) + 1,
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

        Map<Long, TerrainColumn> terrainCache = new HashMap<>(16);
        Map<Long, BasalTalusApronField.RockColumn> contactCache = new HashMap<>(16);
        TerrainColumn terrain = terrainColumnWithBasalTalusCached(
                x,
                z,
                terrainCache,
                contactCache
        );
        int minimumY = height.getMinBuildHeight();
        int maximumY = height.getMaxBuildHeight() - 1;

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
                if (filteredRockOccupies(
                        x,
                        z,
                        y,
                        terrain,
                        material,
                        terrainCache
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
        Map<Long, TerrainColumn> terrainCache = new HashMap<>(2048);
        Map<Long, BasalTalusApronField.RockColumn> contactCache = new HashMap<>(2048);

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minimumZ + localZ;

            for (int localX = 0; localX < 16; localX++) {
                int worldX = minimumX + localX;
                TerrainColumn terrain = terrainColumnWithBasalTalusCached(
                        worldX,
                        worldZ,
                        terrainCache,
                        contactCache
                );

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
                        if (filteredRockOccupies(
                                worldX,
                                worldZ,
                                y,
                                terrain,
                                material,
                                terrainCache
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
        int originalRockTopY = Mth.clamp(
                Mth.floor(geology.baseElevation() + 0.5),
                MacroGeologyField.BASE_SURFACE_Y,
                lastRockY
        );
        LithologyField.Column lithology = LithologyField.column(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5,
                terrainSettings.lithology(),
                terrainSettings.additionalMaterials()
        );
        LithologyField.Sample surfaceLithology = lithology.sample(originalRockTopY);
        MassifFractureField.Sample fracture = MassifFractureField.sample(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5,
                originalRockTopY,
                geology,
                surfaceLithology.resistance(),
                terrainSettings.fractures()
        );
        int carveDepth = Math.min(
                Mth.floor(fracture.carveDepth()),
                Math.max(
                        0,
                        originalRockTopY - (MacroGeologyField.BASE_SURFACE_Y + 1)
                )
        );
        int fissureRockTopY = originalRockTopY - carveDepth;
        RockFaceExposure.Sample face = RockFaceExposure.sample(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5,
                originalRockTopY,
                geology,
                terrainSettings
        );
        EscarpmentErosionField.Column erosion = EscarpmentErosionField.sample(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5,
                originalRockTopY,
                fissureRockTopY,
                geology,
                face,
                lithology,
                fracture,
                terrainSettings
        );
        RockSurfaceErosionField.Column surfaceErosion = RockSurfaceErosionField.sample(
                worldSeed,
                worldX + 0.5,
                worldZ + 0.5,
                originalRockTopY,
                fissureRockTopY,
                geology,
                face,
                fracture,
                terrainSettings
        );
        int rockTopY = surfaceErosion.highestRockY(
                lithology,
                fracture,
                erosion
        );

        return new TerrainColumn(
                rockTopY,
                originalRockTopY,
                fissureRockTopY,
                dune.surfaceUnits(),
                geology,
                lithology,
                fracture,
                erosion,
                surfaceErosion,
                BasalTalusApronField.Sample.NONE
        );
    }

    private TerrainColumn terrainColumnWithBasalTalusCached(
            int worldX,
            int worldZ,
            Map<Long, TerrainColumn> terrainCache,
            Map<Long, BasalTalusApronField.RockColumn> contactCache
    ) {
        TerrainColumn terrain = terrainColumnCached(worldX, worldZ, terrainCache);
        BasalTalusApronField.Sample apron;
        if (!BasalTalusApronField.usesActualContact(
                terrainSettings.profileVersion()
        )) {
            apron = BasalTalusApronField.sampleLegacy(
                    worldSeed,
                    worldX + 0.5,
                    worldZ + 0.5,
                    terrain.geology(),
                    terrainSettings
            );
        } else {
            apron = BasalTalusApronField.sample(
                    worldSeed,
                    worldX,
                    worldZ,
                    terrain.geology(),
                    terrainSettings,
                    (sampleX, sampleZ) -> survivingRockContactColumn(
                            sampleX,
                            sampleZ,
                            terrainCache,
                            contactCache
                    )
            );
        }
        return terrain.withBasalTalusApron(apron);
    }

    private TerrainColumn terrainColumnCached(
            int worldX,
            int worldZ,
            Map<Long, TerrainColumn> terrainCache
    ) {
        long key = ((long) worldX << 32) ^ (worldZ & 0xffffffffL);
        TerrainColumn cached = terrainCache.get(key);
        if (cached != null) {
            return cached;
        }

        TerrainColumn terrain = terrainColumn(worldX, worldZ);
        terrainCache.put(key, terrain);
        return terrain;
    }

    private BasalTalusApronField.RockColumn survivingRockContactColumn(
            int worldX,
            int worldZ,
            Map<Long, TerrainColumn> terrainCache,
            Map<Long, BasalTalusApronField.RockColumn> contactCache
    ) {
        long key = ((long) worldX << 32) ^ (worldZ & 0xffffffffL);
        BasalTalusApronField.RockColumn cached = contactCache.get(key);
        if (cached != null) {
            return cached;
        }

        TerrainColumn terrain = terrainColumnCached(worldX, worldZ, terrainCache);
        boolean occupied = finalRockOccupiesBeforeTalus(
                worldX,
                worldZ,
                FIRST_NATIVE_Y,
                terrain,
                terrainCache
        );
        MacroGeologyField.Sample geology = terrain.geology();
        boolean massifSource = geology.massifWeight() > 0.06
                || geology.faultedMarginWeight() > 0.06;
        BasalTalusApronField.RockColumn result = new BasalTalusApronField.RockColumn(
                occupied,
                terrain.rockTopY(),
                massifSource,
                geology.faultCarveMask()
        );
        contactCache.put(key, result);
        return result;
    }

    private boolean finalRockOccupiesBeforeTalus(
            int worldX,
            int worldZ,
            int worldY,
            TerrainColumn terrain,
            Map<Long, TerrainColumn> terrainCache
    ) {
        if (!terrain.hasNativeRock()
                || worldY < FIRST_NATIVE_Y
                || worldY > terrain.rockTopY()) {
            return false;
        }
        return filteredRockOccupies(
                worldX,
                worldZ,
                worldY,
                terrain,
                terrain.materialSampleAt(worldY),
                terrainCache
        );
    }

    private boolean rawRockOccupies(
            TerrainColumn terrain,
            int worldY
    ) {
        if (!terrain.hasNativeRock()
                || worldY < FIRST_NATIVE_Y
                || worldY > terrain.rockTopY()) {
            return false;
        }

        LithologyField.Sample material = terrain.materialSampleAt(worldY);
        return terrain.erosion().occupies(worldY, material)
                && terrain.surfaceErosion().occupies(worldY, material);
    }

    private boolean filteredRockOccupies(
            int worldX,
            int worldZ,
            int worldY,
            TerrainColumn terrain,
            LithologyField.Sample material,
            Map<Long, TerrainColumn> terrainCache
    ) {
        if (!terrain.erosion().occupies(worldY, material)
                || !terrain.surfaceErosion().occupies(worldY, material)) {
            return false;
        }

        return OrphanRemnantFilter.keeps(
                worldX,
                worldY,
                worldZ,
                terrain.erosion(),
                terrainSettings.erosion().orphanRemnants(),
                (supportX, supportY, supportZ) -> rawRockOccupies(
                        terrainColumnCached(
                                supportX,
                                supportZ,
                                terrainCache
                        ),
                        supportY
                )
        );
    }

    private int highestFilteredOccupiedY(
            int worldX,
            int worldZ,
            TerrainColumn terrain,
            Map<Long, TerrainColumn> terrainCache
    ) {
        int filteredRockTopY = MacroGeologyField.BASE_SURFACE_Y;
        for (int y = terrain.rockTopY();
                y >= FIRST_NATIVE_Y;
                y--) {
            LithologyField.Sample material = terrain.materialSampleAt(y);
            if (filteredRockOccupies(
                    worldX,
                    worldZ,
                    y,
                    terrain,
                    material,
                    terrainCache
            )) {
                filteredRockTopY = y;
                break;
            }
        }

        int talusTopY = terrain.erosion().talusThickness() > 0
                ? terrain.talusBaseY() + terrain.erosion().talusThickness() - 1
                : MacroGeologyField.BASE_SURFACE_Y;
        return Math.max(
                Math.max(
                        Math.max(filteredRockTopY, talusTopY),
                        terrain.basalTalusApron().topY()
                ),
                terrain.highestDuneY()
        );
    }

    /**
     * Finds the highest existing hard-rock layer in the flat Arrakis base column. Native
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

    private static int findFoundationTopY(
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
            TerrainColumn terrain,
            int minimumY,
            int maximumY,
            BlockWriter writer
    ) {
        BasalTalusApronField.Sample apron = terrain.basalTalusApron();
        if (!apron.active()) {
            return;
        }

        int firstY = Math.max(
                minimumY,
                MacroGeologyField.BASE_SURFACE_Y + 1
        );
        int lastY = Math.min(maximumY, apron.topY());
        for (int y = firstY; y <= lastY; y++) {
            BasalTalusApronField.Material material = apron.materialAt(y);
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
        int thickness = terrain.erosion().talusThickness();
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

    private record TerrainColumn(
            int rockTopY,
            int originalRockTopY,
            int fissureRockTopY,
            int duneSurfaceUnits,
            MacroGeologyField.Sample geology,
            LithologyField.Column lithology,
            MassifFractureField.Sample fracture,
            EscarpmentErosionField.Column erosion,
            RockSurfaceErosionField.Column surfaceErosion,
            BasalTalusApronField.Sample basalTalusApron
    ) {
        TerrainColumn withBasalTalusApron(BasalTalusApronField.Sample apron) {
            return new TerrainColumn(
                    rockTopY,
                    originalRockTopY,
                    fissureRockTopY,
                    duneSurfaceUnits,
                    geology,
                    lithology,
                    fracture,
                    erosion,
                    surfaceErosion,
                    apron
            );
        }

        LithologyField.Sample materialSampleAt(int y) {
            LithologyField.Sample sample = lithology.sample(y);
            if (fracture.calciteExposure(y, originalRockTopY, fissureRockTopY)) {
                return new LithologyField.Sample(
                        LithologyField.Material.CALCITE,
                        LithologyField.ResistanceClass.MEDIUM,
                        sample.limestoneHost(),
                        false,
                        false,
                        true
                );
            }
            return sample;
        }

        boolean hasNativeRock() {
            return rockTopY > MacroGeologyField.BASE_SURFACE_Y;
        }

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
            int duneTopY = highestDuneY();
            int talusTopY = erosion.talusThickness() > 0
                    ? talusBaseY() + erosion.talusThickness() - 1
                    : MacroGeologyField.BASE_SURFACE_Y;
            return Math.max(
                    Math.max(
                            Math.max(rockTopY, talusTopY),
                            basalTalusApron.topY()
                    ),
                    duneTopY
            );
        }

        int highestDuneY() {
            int duneTopY = MacroGeologyField.BASE_SURFACE_Y;
            if (duneSurfaceUnits > 0) {
                duneTopY = duneFullTopY();
                if (partialDuneLayers() > 0) {
                    duneTopY++;
                }
            }
            return duneTopY;
        }

        int talusBaseY() {
            return Math.max(
                    Math.max(
                            Math.max(FIRST_NATIVE_Y, rockTopY + 1),
                            basalTalusApron.topY() + 1
                    ),
                    duneFullTopY() + 1
            );
        }

        boolean talusOccupiesY(int y) {
            return erosion.talusThickness() > 0
                    && y >= talusBaseY()
                    && y < talusBaseY() + erosion.talusThickness();
        }
    }
}
