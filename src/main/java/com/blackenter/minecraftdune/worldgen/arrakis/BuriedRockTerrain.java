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

    BuriedRockTerrain(long seed, ArrakisTerrainSettings settings, int capacity, TerrainGenerationMetrics.Evaluation metrics) {
        this.seed = seed; this.settings = settings; this.capacity = capacity; this.metrics = metrics;
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
        Entry entry = entry(x, z);
        if (entry.erosion != null) return entry;
        var config = settings.buriedRock().erosion();
        var face = RockFaceExposure.external(x + .5, z + .5, entry.raw.rockTop(), entry.sediment.surfaceY(),
                Math.max(2, config.surfaceRetreat() + 1), config.probeDistance(), config.minimumRelief(),
                (sx, sz) -> interpolate(sx, sz, true));
        entry.fracture = MassifFractureField.structural(seed, x + .5, z + .5,
                entry.lithology.sample(entry.raw.rockTop()).resistance(), settings.fractures());
        entry.erosion = RockErosionField.sample(seed, x + .5, z + .5, entry.raw.rockTop(), entry.sediment.surfaceY(),
                face, entry.fracture, entry.lithology, entry.raw.fault().damage(), settings.nativeDunes().windAngleDegrees(),
                settings.buriedRock(), (sx, sz) -> interpolate(sx, sz, false));
        metrics.stage(TerrainGenerationMetrics.Stage.EXPOSURE);
        metrics.stage(TerrainGenerationMetrics.Stage.EROSION);
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
        var talus = TalusColluviumField.sample(seed, x, z, external, settings.buriedRock().talus(), (sx, sz) -> {
            Entry source = eroded(sx, sz);
            var face = source.erosion.face();
            double exposedSupply = source.erosion.rockTop() >= source.sediment.surfaceY() && face.exposed()
                    ? source.erosion.removedAmount() : 0;
            return new TalusColluviumField.Source(source.erosion.rockTop(), exposedSupply,
                    face.outwardNormalX(), face.outwardNormalZ(), source.lithology.sample(source.erosion.rockTop()).material());
        });
        metrics.stage(TerrainGenerationMetrics.Stage.TALUS);
        metrics.stage(TerrainGenerationMetrics.Stage.COMPOSITION);
        return entry.complete = new BuriedTerrainColumn(seed, x, z, entry.raw, entry.sediment, entry.erosion,
                entry.fracture, entry.lithology, talus, settings.buriedRock().sediment().compactionDepth());
    }

    int rockTopY(int x, int z) { return (int) Math.floor(eroded(x, z).erosion.rockTop()); }
    int size() { return cache.size(); }

    private static final class Entry {
        final RawRockSurfaceField.Sample raw;
        final SedimentSurfaceField.Sample sediment;
        final LithologyField.Column lithology;
        RockErosionField.Sample erosion;
        MassifFractureField.Sample fracture;
        BuriedTerrainColumn complete;
        Entry(RawRockSurfaceField.Sample raw, SedimentSurfaceField.Sample sediment, LithologyField.Column lithology) {
            this.raw = raw; this.sediment = sediment; this.lithology = lithology;
        }
    }
}
