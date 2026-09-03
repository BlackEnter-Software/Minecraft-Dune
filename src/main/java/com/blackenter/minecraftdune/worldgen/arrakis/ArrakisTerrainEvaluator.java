package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.blackenter.minecraftdune.worldgen.geology.BoundedBasalComponentCleanup;
import com.blackenter.minecraftdune.worldgen.geology.BasalSandSkirt;
import com.blackenter.minecraftdune.worldgen.geology.EscarpmentErosionField;
import com.blackenter.minecraftdune.worldgen.geology.LithologyField;
import com.blackenter.minecraftdune.worldgen.geology.MacroGeologyField;
import com.blackenter.minecraftdune.worldgen.geology.MassifFractureField;
import com.blackenter.minecraftdune.worldgen.geology.OrphanRemnantFilter;
import com.blackenter.minecraftdune.worldgen.geology.RockFaceExposure;
import com.blackenter.minecraftdune.worldgen.geology.RockSurfaceErosionField;
import com.blackenter.minecraftdune.worldgen.geology.ScarpMorphologyField;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
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
    private final Long2ObjectLinkedOpenHashMap<ColumnEntry> columns;
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
        columns = new Long2ObjectLinkedOpenHashMap<>(maximumEntries);
    }

    /** Only this final stage requests deposits. Support and contact queries never call it. */
    public TerrainColumn column(int worldX, int worldZ) {
        ColumnEntry entry = entry(worldX, worldZ);
        if (entry.complete == null) {
            var basal = BasalTalusApronField.evaluate(worldSeed, worldX, worldZ,
                    entry.rock.geology(), terrainSettings, new BasalTalusApronField.RockLookup() {
                        @Override public boolean footPresent(int x, int z) {
                            // Talus must touch the exposed wall, not the thin surviving floor
                            // that its own wedge can bury. This is a deposit-only query band;
                            // no rock-height cutoff is applied to occupancy or generation.
                            return talusWallPresent(x, z);
                        }
                        @Override public int topY(int x, int z) { return highestFilteredRockY(x, z); }
                        @Override public boolean allowed(int x, int z) {
                            var g = preTalusColumn(x, z).geology();
                            return g.sandCorridorMask() <= 0.25 && g.faultCarveMask() <= 0.85;
                        }
                        @Override public boolean sourceAllowed(int x, int z) {
                            var g = preTalusColumn(x, z).geology();
                            var side = ScarpMorphologyField.nearestMassifLowSideContact(worldSeed, x + 0.5, z + 0.5,
                                    g.radiusBlocks(), g.effectiveRadiusBlocks(), terrainSettings.massif());
                            return side.valid() && Math.abs(side.signedDistance()) <= side.scarpWidth()
                                    + 2 * BasalTalusApronField.CONTACT_SEARCH_LIMIT;
                        }
                        @Override public boolean ravineSourceAllowed(int x, int z) {
                            var r = preTalusColumn(x, z);
                            return r.geology().faultCarveMask() > 0 && allowed(x, z)
                                    && r.geology().physicalMassifWeight() > 0.18
                                    && r.face().exposed() && r.face().lowY() <= 70
                                    && r.face().localRelief() > 12;
                        }
                    });
            boolean residual = residualBasalY65(worldX, worldZ);
            var skirt = BasalSandSkirt.sample(terrainSettings.lithology().talus().basalSandSkirtEnabled(),
                    basal, residual, basal.apron().materialAt(65) == BasalTalusApronField.Material.GRAVEL);
            entry.complete = new TerrainColumn(entry.rock, basal, skirt, residual,
                    terrainSettings.lithology().talus().basalSandSkirtEnabled(), componentCleanup(worldX, worldZ).removed());
        }
        return entry.complete;
    }

    public PreTalusColumn preTalusColumn(int worldX, int worldZ) {
        return entry(worldX, worldZ).rock;
    }

    public int talusWallQueryMinY() {
        return FIRST_NATIVE_Y + Math.max(0, Math.min(12,
                terrainSettings.lithology().talus().basalApronMaxHeight()));
    }

    public int talusWallQueryMaxY() { return talusWallQueryMinY() + 5; }

    private boolean talusWallPresent(int x, int z) {
        ColumnEntry entry = entry(x, z);
        if (entry.talusWall != 0) return entry.talusWall == 2;
        var rock = entry.rock;
        int minY = talusWallQueryMinY();
        for (int y = minY; y <= Math.min(talusWallQueryMaxY(), rock.rockTopY()); y++) {
            if (filteredRockOccupies(x, z, y, rock, rock.materialSampleAt(y))) {
                entry.talusWall = 2;
                return true;
            }
        }
        entry.talusWall = 1;
        return false;
    }

    private ColumnEntry entry(int worldX, int worldZ) {
        // Both full signed coordinates, not chunk coordinates. One bound for both stages.
        long key = ChunkPos.asLong(worldX, worldZ);
        ColumnEntry cached = columns.getAndMoveToLast(key);
        if (cached != null) {
            metrics.cacheHit();
            return cached;
        }
        metrics.cacheMiss();
        ColumnEntry result = new ColumnEntry(terrainColumn(worldX, worldZ));
        // Four local wall rays can fill the cache early in a chunk. Keep the current
        // working set rather than pinning those first columns and recomputing every
        // later component for every Y. Eviction only changes work, never decisions.
        if (maximumEntries > 0) {
            if (columns.size() == maximumEntries) columns.removeFirst();
            columns.put(key, result);
        } else metrics.cacheBypass();
        return result;
    }

    public int size() { return columns.size(); }

    int completedColumns() {
        int count = 0;
        for (var entry : columns.values()) if (entry.complete != null) count++;
        return count;
    }

    /** Final pre-talus rock only; foundation at/below Y64 is written separately. */
    public boolean rockOccupies(int worldX, int worldY, int worldZ) {
        PreTalusColumn terrain = preTalusColumn(worldX, worldZ);
        return worldY >= FIRST_NATIVE_Y && worldY <= terrain.rockTopY()
                && filteredRockOccupies(worldX, worldZ, worldY, terrain,
                        terrain.materialSampleAt(worldY));
    }

    public int highestFilteredRockY(int worldX, int worldZ) {
        ColumnEntry entry = entry(worldX, worldZ);
        if (entry.filteredTop != Integer.MIN_VALUE) return entry.filteredTop;
        var terrain = entry.rock;
        for (int y = terrain.rockTopY(); y >= FIRST_NATIVE_Y; y--) {
            if (filteredRockOccupies(worldX, worldZ, y, terrain, terrain.materialSampleAt(y))) {
                return entry.filteredTop = y;
            }
        }
        return entry.filteredTop = MacroGeologyField.BASE_SURFACE_Y;
    }

    private static final class ColumnEntry {
        final PreTalusColumn rock;
        int filteredTop = Integer.MIN_VALUE;
        byte talusWall;
        BoundedBasalComponentCleanup.Sample component;
        int orphanTop = Integer.MIN_VALUE;
        byte[] orphanOccupancy;
        TerrainColumn complete;
        ColumnEntry(PreTalusColumn rock) { this.rock = rock; }
    }

    public int highestOccupiedY(int worldX, int worldZ) {
        TerrainColumn terrain = column(worldX, worldZ);
        int talusTopY = terrain.localTalusThickness() > 0
                ? terrain.talusBaseY() + terrain.localTalusThickness() - 1
                : MacroGeologyField.BASE_SURFACE_Y;
        return Math.max(Math.max(highestFilteredRockY(worldX, worldZ), talusTopY),
                Math.max(terrain.basalTalusApron().topY(), terrain.highestDuneY()));
    }

    private PreTalusColumn terrainColumn(
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
        return new PreTalusColumn(
                rockTopY,
                originalRockTopY,
                fissureRockTopY,
                dune.surfaceUnits(),
                lithology,
                fracture,
                erosion,
                surfaceErosion,
                geology,
                face
        );
    }

    public boolean rawRockOccupies(
            PreTalusColumn terrain,
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
            PreTalusColumn terrain,
            LithologyField.Sample material
    ) {
        return orphanFilteredRockOccupies(worldX, worldZ, worldY, terrain, material)
                && (worldY < FIRST_NATIVE_Y
                    || !terrainSettings.erosion().orphanRemnants().basalComponentCleanupEnabled()
                    || !componentCleanup(worldX, worldZ).removesY(worldY));
    }

    /** Only the existing orphan stage; component support never samples itself or deposits. */
    boolean orphanFilteredRockOccupies(int worldX, int worldZ, int worldY,
            PreTalusColumn terrain, LithologyField.Sample material) {
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
                        preTalusColumn(supportX, supportZ),
                        supportY
                )
        );
    }

    private boolean componentContext(int x, int z) {
        var rock = preTalusColumn(x, z);
        var orphan = terrainSettings.erosion().orphanRemnants();
        var face = rock.face();
        return orphan.enabled() && orphan.basalComponentCleanupEnabled()
                && rock.surfaceErosion().settings().baseAnchoredErosion()
                && (rock.erosion().candidate() || rock.surfaceErosion().active())
                && face.exposed() && face.lowY() <= 70
                && face.localRelief() > 12 && rock.geology().physicalMassifWeight() > 0.18
                // Only the opted-in fault shoulder may be classified. The bounded graph
                // still retains every component connected to real wall/toe or core rock.
                && rock.geology().sandCorridorMask() <= 0.25
                && (rock.geology().faultCarveMask() == 0
                    || orphan.faultEdgeCleanupEnabled() && rock.geology().faultCarveMask() <= 0.85);
    }

    public BoundedBasalComponentCleanup.Sample componentCleanup(int x, int z) {
        ColumnEntry entry = entry(x, z);
        if (entry.component != null) return entry.component;
        return entry.component = BoundedBasalComponentCleanup.sample(x, z, new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int sx, int sz) { return highestOrphanRockY(sx, sz); }
            public boolean occupied(int sx, int sy, int sz) { return postOrphanRockOccupies(sx, sy, sz); }
            public boolean cleanupAllowed(int sx, int sz) { return componentContext(sx, sz); }
        });
    }

    private boolean postOrphanRockOccupies(int x, int y, int z) {
        ColumnEntry entry = entry(x, z);
        var rock = entry.rock;
        if (y < FIRST_NATIVE_Y || y > rock.rockTopY()) return false;
        if (entry.orphanOccupancy == null) entry.orphanOccupancy = new byte[rock.rockTopY() - FIRST_NATIVE_Y + 1];
        int index = y - FIRST_NATIVE_Y;
        byte cached = entry.orphanOccupancy[index];
        if (cached == 0) entry.orphanOccupancy[index] = cached = (byte) (orphanFilteredRockOccupies(
                x, z, y, rock, rock.materialSampleAt(y)) ? 2 : 1);
        return cached == 2;
    }

    private int highestOrphanRockY(int x, int z) {
        ColumnEntry entry = entry(x, z);
        if (entry.orphanTop != Integer.MIN_VALUE) return entry.orphanTop;
        for (int y = entry.rock.rockTopY(); y >= FIRST_NATIVE_Y; y--) {
            if (postOrphanRockOccupies(x, y, z)) return entry.orphanTop = y;
        }
        return entry.orphanTop = 64;
    }

    /** A surviving one-layer erosion floor, NOT rock with any final body above it. */
    public boolean residualBasalY65(int x, int z) {
        var c = preTalusColumn(x, z);
        return c.originalRockTopY() > 65 && c.face().exposed() && c.face().lowY() <= 65
                && (c.erosion().candidate() || c.surfaceErosion().active())
                && highestFilteredRockY(x, z) == 65 && rockOccupies(x, 65, z);
    }

    /** Real cliff rock wins; roots below Y65 and classified thin erosion residue may be concealed. */
    public boolean realCliffRock(int x, int y, int z, TerrainColumn column) {
        return rockOccupies(x, y, z) && !(y == 65 && column.residualY65());
    }

    public String preSkirtOwner(int x, int y, int z) {
        if (y <= 64) return "SUBSTRATE_OR_FOUNDATION_ROOT";
        if (!rockOccupies(x, y, z)) return "NO_NATIVE_ROCK";
        return y == 65 && residualBasalY65(x, z) ? "BASAL_EROSION_RESIDUE" : "FINAL_CLIFF_ROCK";
    }

    public BasalTalusApronField.Material basalMaterialAt(int x, int y, int z, TerrainColumn column) {
        if (column.rockPriority() && realCliffRock(x, y, z, column)) return BasalTalusApronField.Material.NONE;
        // Local scree is written first; never bury it with the basal sand skirt.
        if (column.talusOccupiesY(y)) return BasalTalusApronField.Material.NONE;
        var apron = column.basalTalusApron().materialAt(y);
        return apron != BasalTalusApronField.Material.NONE ? apron : column.skirt().materialAt(y);
    }

    public boolean rawRockOccupies(TerrainColumn terrain, int worldY) {
        return rawRockOccupies(terrain.rock(), worldY);
    }

    boolean filteredRockOccupies(int x, int z, int y, TerrainColumn terrain, LithologyField.Sample material) {
        return filteredRockOccupies(x, z, y, terrain.rock(), material);
    }

    public record PreTalusColumn(
            int rockTopY,
            int originalRockTopY,
            int fissureRockTopY,
            int duneSurfaceUnits,
            LithologyField.Column lithology,
            MassifFractureField.Sample fracture,
            EscarpmentErosionField.Column erosion,
            RockSurfaceErosionField.Column surfaceErosion,
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

    }

    public record TerrainColumn(PreTalusColumn rock, BasalTalusApronField.Evaluation basal,
            BasalSandSkirt.Sample skirt, boolean residualY65, boolean rockPriority, boolean componentRemoved) {
        public TerrainColumn(PreTalusColumn rock, BasalTalusApronField.Evaluation basal) {
            this(rock, basal, BasalSandSkirt.shape(false, Double.POSITIVE_INFINITY, false), false, false, false);
        }
        public int localTalusThickness() { return componentRemoved ? 0 : erosion().talusThickness(); }
        public int rockTopY() { return rock.rockTopY(); }
        public int originalRockTopY() { return rock.originalRockTopY(); }
        public int fissureRockTopY() { return rock.fissureRockTopY(); }
        public int duneSurfaceUnits() { return rock.duneSurfaceUnits(); }
        public LithologyField.Column lithology() { return rock.lithology(); }
        public MassifFractureField.Sample fracture() { return rock.fracture(); }
        public EscarpmentErosionField.Column erosion() { return rock.erosion(); }
        public RockSurfaceErosionField.Column surfaceErosion() { return rock.surfaceErosion(); }
        public MacroGeologyField.Sample geology() { return rock.geology(); }
        public RockFaceExposure.Sample face() { return rock.face(); }
        public BasalTalusApronField.Sample basalTalusApron() { return basal.apron(); }
        public LithologyField.Sample materialSampleAt(int y) { return rock.materialSampleAt(y); }
        public boolean hasNativeRock() { return rock.hasNativeRock(); }

        public int fullDuneBlocks() {
            return duneSurfaceUnits()
                    / NativeTransverseDuneField.SUBDIVISIONS;
        }

        public int partialDuneLayers() {
            return duneSurfaceUnits()
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
            int talusTopY = localTalusThickness() > 0
                    ? talusBaseY() + localTalusThickness() - 1
                    : MacroGeologyField.BASE_SURFACE_Y;
            return Math.max(
                    Math.max(
                            Math.max(rockTopY(), talusTopY),
                            basalTalusApron().topY()
                    ),
                    duneTopY
            );
        }

        public int highestDuneY() {
            int duneTopY = MacroGeologyField.BASE_SURFACE_Y;
            if (duneSurfaceUnits() > 0) {
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
                            Math.max(FIRST_NATIVE_Y, rockTopY() + 1),
                            basalTalusApron().topY() + 1
                    ),
                    duneFullTopY() + 1
            );
        }

        public boolean talusOccupiesY(int y) {
            return localTalusThickness() > 0
                    && y >= talusBaseY()
                    && y < talusBaseY() + localTalusThickness();
        }
    }
}
