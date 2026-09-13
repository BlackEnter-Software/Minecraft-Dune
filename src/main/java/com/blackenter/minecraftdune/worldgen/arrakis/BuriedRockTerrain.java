package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.ChunkPos;

/** Operation-local DAG: raw + sediment -> exposure/erosion -> colluvium -> composition. */
final class BuriedRockTerrain {
    private final long seed;
    private final ArrakisTerrainSettings settings;
    private final int capacity;
    private final TerrainGenerationMetrics.Evaluation metrics;
    private final Long2ObjectLinkedOpenHashMap<Entry> cache = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Entry> erodedCache = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<ExposedCliffCavityField.Feature> cavityCache = new Long2ObjectLinkedOpenHashMap<>();
    private final boolean finishing;

    BuriedRockTerrain(long seed, ArrakisTerrainSettings settings, int capacity, TerrainGenerationMetrics.Evaluation metrics) {
        this.seed = seed; this.settings = settings; this.capacity = capacity; this.metrics = metrics;
        finishing = settings.terrainAlgorithmRevision() == 3 && settings.buriedRock().finishing().enabled();
    }

    private Entry entry(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        Entry value = cache.getAndMoveToLast(key);
        if (value != null) { metrics.cacheHit(); return value; }
        metrics.cacheMiss();
        var raw = RawRockSurfaceField.sample(seed, x + .5, z + .5, settings);
        var sediment = SedimentSurfaceField.sample(seed, x + .5, z + .5, raw.geography(), settings);
        value = new Entry(raw, sediment, LithologyField.column(seed, x + .5, z + .5,
                settings.lithology(), settings.additionalMaterials(), raw.structuralDisplacement()));
        metrics.stage(TerrainGenerationMetrics.Stage.RAW_ROCK);
        metrics.stage(TerrainGenerationMetrics.Stage.FAULT);
        metrics.stage(TerrainGenerationMetrics.Stage.SEDIMENT);
        metrics.stage(TerrainGenerationMetrics.Stage.LITHOLOGY_COLUMN);
        if (capacity > 0) {
            if (cache.size() == capacity) cache.removeFirst();
            cache.put(key, value);
        } else metrics.cacheBypass();
        return value;
    }

    private Entry eroded(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        if (finishing) {
            var existing = erodedCache.getAndMoveToLast(key);
            if (existing != null) return existing;
        }
        Entry entry = entry(x, z);
        if (entry.erosion != null) return entry;
        var config = settings.buriedRock().erosion();
        // Still eight fixed external probes. A sector can retreat farther than dev.1's
        // 18-block halo, so its exposure must see that shoulder before recession begins.
        double farProbe = config.morphology().enabled() ? Math.max(config.probeDistance(),
                Math.min(64, config.morphology().sectorRecession() + config.morphology().mesoscaleRecession())) : config.probeDistance();
        var face = RockFaceExposure.external(x + .5, z + .5, entry.raw.rockTop(), entry.sediment.surfaceY(),
                Math.max(2, config.surfaceRetreat() + 1), farProbe, config.minimumRelief(),
                (sx, sz) -> interpolate(sx, sz, true));
        var resistance = entry.lithology.sample(entry.raw.rockTop()).resistance();
        entry.fracture = finishing ? MassifFractureField.surface(seed, x + .5, z + .5, resistance,
                settings.fractures(), settings.buriedRock().finishing().fissureVariation())
                : MassifFractureField.structural(seed, x + .5, z + .5, resistance, settings.fractures());
        entry.summitRemoval = finishing ? SummitWeatheringField.removal(seed, x + .5, z + .5,
                entry.raw.rockTop(), entry.sediment.surfaceY(), entry.raw.geography().physicalMassifWeight(),
                face, resistance, settings.buriedRock()) : 0;
        entry.erosion = RockErosionField.sample(seed, x + .5, z + .5, entry.raw.rockTop(), entry.sediment.surfaceY(),
                face, entry.fracture, entry.lithology, entry.raw.fault().damage(), settings.nativeDunes().windAngleDegrees(),
                settings.buriedRock(), (sx, sz) -> interpolate(sx, sz, false),
                WallErosionMorphology.sample(seed, x + .5, z + .5, entry.raw.rockTop(), entry.sediment.surfaceY(),
                        face, entry.raw.geography(), settings), entry.summitRemoval,
                finishing ? settings.buriedRock().finishing().erosionWorkBoost() : 0);
        metrics.stage(TerrainGenerationMetrics.Stage.EXPOSURE);
        if (config.morphology().enabled()) metrics.stage(TerrainGenerationMetrics.Stage.MORPHOLOGY);
        metrics.stage(TerrainGenerationMetrics.Stage.EROSION);
        if (finishing && capacity > 0) {
            if (erodedCache.size() == capacity * 4) erodedCache.removeFirst();
            erodedCache.put(key, entry);
        }
        return entry;
    }

    /** Interpolate raw analytic roofs only: never recurse into erosion or composed neighbors. */
    private double interpolate(double x, double z, boolean external) {
        double bx = x - .5, bz = z - .5;
        int x0 = (int) Math.floor(bx), z0 = (int) Math.floor(bz);
        double tx = bx - x0, tz = bz - z0;
        double a = height(entry(x0, z0), external);
        if (tx == 0 && tz == 0) return a;
        double b = tx == 0 ? a : height(entry(x0 + 1, z0), external);
        double c = tz == 0 ? a : height(entry(x0, z0 + 1), external);
        double d = tx == 0 ? c : tz == 0 ? b : height(entry(x0 + 1, z0 + 1), external);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    private static double height(Entry entry, boolean external) {
        return external ? Math.max(entry.raw.rockTop(), entry.sediment.surfaceY()) : entry.raw.rockTop();
    }

    BuriedTerrainColumn column(int x, int z) {
        Entry entry = eroded(x, z);
        if (entry.complete != null) return entry.complete;
        double external = Math.max(Math.floor(entry.erosion.rockTop()), entry.sediment.surfaceY());
        TalusColluviumField.SourceLookup sourceLookup = (sx, sz) -> {
            if (finishing) {
                var rawSource = entry(sx, sz);
                if (rawSource.raw.rockTop() < rawSource.sediment.surfaceY()) return NO_SUPPLY;
            }
            Entry source = eroded(sx, sz);
            var face = source.erosion.face();
            var morphology = source.erosion.morphology();
            boolean erodedSlope = morphology.recessionRemoval() + morphology.gullyIncision() > 0;
            double exposedSupply = source.erosion.rockTop() >= source.sediment.surfaceY() && (face.exposed() || erodedSlope)
                    ? source.erosion.removedAmount() : 0;
            return new TalusColluviumField.Source(source.erosion.rockTop(), exposedSupply,
                    face.outwardNormalX(), face.outwardNormalZ(), source.lithology.sample(source.erosion.rockTop()).material());
        };
        var talus = finishing ? StableTalusField.sample(seed, x, z, external, settings.buriedRock(), sourceLookup,
                (sx, sz) -> {
                    var surface = eroded(sx, sz);
                    return Math.max(Math.floor(surface.erosion.rockTop()), surface.sediment.surfaceY());
                }) : TalusColluviumField.sample(seed, x, z, external, settings.buriedRock().talus(), sourceLookup);
        var cavities = finishing ? cavityColumn(x, z) : ExposedCliffCavityField.Column.NONE;
        metrics.stage(TerrainGenerationMetrics.Stage.TALUS);
        metrics.stage(TerrainGenerationMetrics.Stage.COMPOSITION);
        return entry.complete = new BuriedTerrainColumn(seed, x, z, entry.raw, entry.sediment, entry.erosion,
                entry.fracture, entry.lithology, talus, settings.buriedRock().sediment().compactionDepth(), cavities,
                finishing ? settings.buriedRock().finishing().sourceClastFraction() : 0, entry.summitRemoval);
    }

    private ExposedCliffCavityField.Column cavityColumn(int x, int z) {
        if (capacity < 512) return ExposedCliffCavityField.column(seed, x, z,
                settings.buriedRock().finishing().cliffCavities(), cavityLookup());
        return cavityFeature(Math.floorDiv(x, ExposedCliffCavityField.CELL_SIZE),
                Math.floorDiv(z, ExposedCliffCavityField.CELL_SIZE)).column(x, z);
    }

    ExposedCliffCavityField.Feature cavityFeature(int cx, int cz) {
        var config = settings.buriedRock().finishing().cliffCavities();
        if (!finishing || !config.enabled()) return new ExposedCliffCavityField.Feature(cx, cz, 0, 0, 0, 0, 0, 0, 0, 0, new long[0]);
        long key = ChunkPos.asLong(cx, cz);
        var feature = cavityCache.getAndMoveToLast(key);
        if (feature == null) {
            feature = ExposedCliffCavityField.sample(seed, cx, cz, config, cavityLookup());
            metrics.stage(TerrainGenerationMetrics.Stage.CAVITY_FEATURE);
            if (capacity > 0) {
                if (cavityCache.size() == 16) cavityCache.removeFirst();
                cavityCache.put(key, feature);
            }
        }
        return feature;
    }

    private ExposedCliffCavityField.SurfaceLookup cavityLookup() {
        return new ExposedCliffCavityField.SurfaceLookup() {
                public ExposedCliffCavityField.Surface sample(int sx, int sz) {
                    var e = eroded(sx, sz);
                    return new ExposedCliffCavityField.Surface(e.erosion.rockTop(), e.sediment.surfaceY(),
                            e.raw.geography().physicalMassifWeight(), Math.max(e.fracture.strength(), e.raw.fault().damage()),
                            settings.buriedRock().talus().enabled() ? settings.buriedRock().talus().maximumThickness() : 0);
                }
                public LithologyField.ResistanceClass resistance(int sx, int y, int sz) {
                    return entry(sx, sz).lithology.sample(y).resistance();
                }
            };
    }

    int rockTopY(int x, int z) { return (int) Math.floor(eroded(x, z).erosion.rockTop()); }
    int size() { return cache.size(); }
    private static final TalusColluviumField.Source NO_SUPPLY = new TalusColluviumField.Source(
            Double.NEGATIVE_INFINITY, 0, 0, 0, LithologyField.Material.STONE);

    private static final class Entry {
        final RawRockSurfaceField.Sample raw;
        final SedimentSurfaceField.Sample sediment;
        final LithologyField.Column lithology;
        RockErosionField.Sample erosion;
        MassifFractureField.Sample fracture;
        double summitRemoval;
        BuriedTerrainColumn complete;
        Entry(RawRockSurfaceField.Sample raw, SedimentSurfaceField.Sample sediment, LithologyField.Column lithology) {
            this.raw = raw; this.sediment = sediment; this.lithology = lithology;
        }
    }
}
