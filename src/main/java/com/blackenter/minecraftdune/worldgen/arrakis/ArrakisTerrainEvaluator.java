package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.blackenter.minecraftdune.worldgen.geology.EscarpmentErosionField;
import com.blackenter.minecraftdune.worldgen.geology.LithologyField;
import com.blackenter.minecraftdune.worldgen.geology.MacroGeologyField;
import com.blackenter.minecraftdune.worldgen.geology.MassifFractureField;
import com.blackenter.minecraftdune.worldgen.geology.OrphanRemnantFilter;
import com.blackenter.minecraftdune.worldgen.geology.RockFaceExposure;
import com.blackenter.minecraftdune.worldgen.geology.RockSurfaceErosionField;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

/**
 * Deterministic analytical terrain and raw/filtered occupancy, independent of world writes.
 * One instance belongs to one operation and one immutable seed/profile. Cache saturation
 * only causes recomputation; it never changes a terrain decision. Instances are not shared
 * across generation threads.
 */
public final class ArrakisTerrainEvaluator {
    public static final int QUERY_CACHE_LIMIT = 64;
    public static final int CHUNK_CACHE_LIMIT = 1_024;
    private static final int FIRST_NATIVE_Y = MacroGeologyField.BASE_SURFACE_Y + 1;

    private final long worldSeed;
    private final ArrakisTerrainSettings terrainSettings;
    private final Long2ObjectOpenHashMap<TerrainColumn> columns;
    private final int maximumEntries;
    private final TerrainGenerationMetrics.Evaluation metrics;

    public ArrakisTerrainEvaluator(long worldSeed, ArrakisTerrainSettings settings, int maximumEntries) {
        this(worldSeed, settings, maximumEntries, TerrainGenerationMetrics.evaluation());
    }

    ArrakisTerrainEvaluator(long worldSeed, ArrakisTerrainSettings settings, int maximumEntries,
            TerrainGenerationMetrics.Evaluation metrics) {
        if (maximumEntries < 0 || maximumEntries > CHUNK_CACHE_LIMIT) {
            throw new IllegalArgumentException("Cache limit must be between 0 and " + CHUNK_CACHE_LIMIT);
        }
        this.worldSeed = worldSeed;
        this.terrainSettings = java.util.Objects.requireNonNull(settings);
        this.maximumEntries = maximumEntries;
        this.metrics = metrics;
        columns = new Long2ObjectOpenHashMap<>(maximumEntries);
    }

    public TerrainColumn column(int worldX, int worldZ) {
        // ChunkPos.asLong packs both full signed 32-bit coordinates, without chunk division.
        long key = ChunkPos.asLong(worldX, worldZ);
        TerrainColumn cached = columns.get(key);
        if (cached != null) {
            metrics.cacheHit();
            return cached;
        }
        metrics.cacheMiss();
        TerrainColumn terrain = terrainColumn(worldX, worldZ);
        if (columns.size() < maximumEntries) {
            columns.put(key, terrain);
        } else {
            metrics.cacheBypass();
        }
        return terrain;
    }

    public int size() {
        return columns.size();
    }

    /** Final pre-talus rock only; the flat foundation at/below Y64 is written separately. */
    public boolean rockOccupies(int worldX, int worldY, int worldZ) {
        TerrainColumn terrain = column(worldX, worldZ);
        return worldY >= FIRST_NATIVE_Y && worldY <= terrain.rockTopY()
                && filteredRockOccupies(worldX, worldZ, worldY, terrain,
                        terrain.materialSampleAt(worldY));
    }

    public int highestFilteredRockY(int worldX, int worldZ) {
        TerrainColumn terrain = column(worldX, worldZ);
        for (int y = terrain.rockTopY(); y >= FIRST_NATIVE_Y; y--) {
            if (filteredRockOccupies(worldX, worldZ, y, terrain, terrain.materialSampleAt(y))) {
                return y;
            }
        }
        return MacroGeologyField.BASE_SURFACE_Y;
    }

    public int highestOccupiedY(int worldX, int worldZ) {
        TerrainColumn terrain = column(worldX, worldZ);
        int talusTopY = terrain.erosion().talusThickness() > 0
                ? terrain.talusBaseY() + terrain.erosion().talusThickness() - 1
                : MacroGeologyField.BASE_SURFACE_Y;
        return Math.max(Math.max(highestFilteredRockY(worldX, worldZ), talusTopY),
                Math.max(terrain.basalTalusApron().topY(), terrain.highestDuneY()));
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
        BasalTalusApronField.Sample basalTalusApron =
                BasalTalusApronField.sample(
                        worldSeed,
                        worldX + 0.5,
                        worldZ + 0.5,
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
                lithology,
                fracture,
                erosion,
                surfaceErosion,
                basalTalusApron,
                geology,
                face
        );
    }

    public boolean rawRockOccupies(
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

    boolean filteredRockOccupies(
            int worldX,
            int worldZ,
            int worldY,
            TerrainColumn terrain,
            LithologyField.Sample material
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
                terrain.surfaceErosion(),
                terrainSettings.erosion().orphanRemnants(),
                (supportX, supportY, supportZ) -> rawRockOccupies(
                        column(supportX, supportZ),
                        supportY
                )
        );
    }

    public record TerrainColumn(
            int rockTopY,
            int originalRockTopY,
            int fissureRockTopY,
            int duneSurfaceUnits,
            LithologyField.Column lithology,
            MassifFractureField.Sample fracture,
            EscarpmentErosionField.Column erosion,
            RockSurfaceErosionField.Column surfaceErosion,
            BasalTalusApronField.Sample basalTalusApron,
            MacroGeologyField.Sample geology,
            RockFaceExposure.Sample face
    ) {
        public LithologyField.Sample materialSampleAt(int y) {
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

        public boolean hasNativeRock() {
            return rockTopY > MacroGeologyField.BASE_SURFACE_Y;
        }

        public int fullDuneBlocks() {
            return duneSurfaceUnits
                    / NativeTransverseDuneField.SUBDIVISIONS;
        }

        public int partialDuneLayers() {
            return duneSurfaceUnits
                    % NativeTransverseDuneField.SUBDIVISIONS;
        }

        public int duneFullTopY() {
            return MacroGeologyField.BASE_SURFACE_Y
                    + fullDuneBlocks();
        }

        public int dunePartialY() {
            return MacroGeologyField.BASE_SURFACE_Y
                    + fullDuneBlocks()
                    + 1;
        }

        public int highestOccupiedY() {
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

        public int highestDuneY() {
            int duneTopY = MacroGeologyField.BASE_SURFACE_Y;
            if (duneSurfaceUnits > 0) {
                duneTopY = duneFullTopY();
                if (partialDuneLayers() > 0) {
                    duneTopY++;
                }
            }
            return duneTopY;
        }

        public int talusBaseY() {
            return Math.max(
                    Math.max(
                            Math.max(FIRST_NATIVE_Y, rockTopY + 1),
                            basalTalusApron.topY() + 1
                    ),
                    duneFullTopY() + 1
            );
        }

        public boolean talusOccupiesY(int y) {
            return erosion.talusThickness() > 0
                    && y >= talusBaseY()
                    && y < talusBaseY() + erosion.talusThickness();
        }
    }
}
