package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.ArrakisProfileValidation;
import com.blackenter.minecraftdune.worldgen.geology.ScarpMorphologyField;
import net.minecraft.world.level.ChunkPos;

import java.util.Locale;
import java.util.function.IntPredicate;

/** Offline analytical measurements, never a build-blocking visual acceptance test. */
public final class ArrakisContactDiagnostics {
    public static void main(String[] args) throws Exception {
        var settings = ArrakisProfileValidation.loadProfile().settings();
        profileChunk(settings, 0L, 190, 12, false); // JVM warmup; not reported.
        profileChunk(settings, 0L, 190, 12, true);
        profileChunk(settings, -5640511200611798902L, 191, 6, true);
        profileChunk(settings, -5640511200611798902L, 193, 10, true);
        profileChunk(settings, 0L, 41, 206, true);
        profileChunk(settings, 0L, 159, 106, true);
        profileChunk(settings, 0L, 0, 0, true);
        profileChunk(settings, 0L, 409, 0, true);
        transect(settings, 0L, 190, 2920, 3130, false);
        transect(settings, 0L, 3053, 100, 220, true);
        transect(settings, -5640511200611798902L, 173, 2990, 3160, false);
        measureApronGap(settings, 0L);
        recommendOuterContact(settings);
        var evaluator = new ArrakisTerrainEvaluator(0L, settings, 64);
        System.out.println(ArrakisTerrainCommand.describe(evaluator, 0L, settings, 3053, 65, 190));
    }

    private static void recommendOuterContact(ArrakisTerrainSettings settings) {
        for (int direction = 0; direction < 32; direction++) {
            double angle = direction * Math.PI / 16.0;
            var evaluator = new ArrakisTerrainEvaluator(0L, settings, 64);
            for (int radius = 3600; radius <= 4800; radius += 4) {
                int x = (int) Math.floor(Math.cos(angle) * radius);
                int z = (int) Math.floor(Math.sin(angle) * radius);
                var c = evaluator.column(x, z);
                if (!c.basalTalusApron().active()) continue;
                var g = c.geology();
                var contact = ScarpMorphologyField.nearestMassifLowSideContact(0L, x + 0.5, z + 0.5,
                        g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
                if (contact.valid() && contact.inwardX() * x + contact.inwardZ() * z < 0) {
                    System.out.printf("Seed-0 outer contact diagnostic location: X/Z=%d/%d filtered-top=%d apron=%d%n",
                            x, z, evaluator.highestFilteredRockY(x, z), c.basalTalusApron().height());
                    return;
                }
            }
        }
        System.out.println("No outer apron found in the bounded diagnostic scan; not a test failure.");
    }

    private static void profileChunk(ArrakisTerrainSettings settings, long seed,
            int chunkX, int chunkZ, boolean report) {
        var metrics = TerrainGenerationMetrics.evaluation();
        var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024, metrics);
        long start = System.nanoTime();
        int blocks = 0;
        for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                var c = evaluator.column(x, z);
                if (!c.hasNativeRock()) continue;
                // The current flat profile's hard foundation ends at Y44.
                for (int y = 45; y <= c.rockTopY(); y++) {
                    if (evaluator.filteredRockOccupies(x, z, y, c, c.materialSampleAt(y))) blocks++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        if (!report) return;
        TerrainGenerationMetrics.recordChunk(new ChunkPos(chunkX, chunkZ), elapsed, metrics, evaluator.size());
        System.out.printf(Locale.ROOT,
                "Analytical-only chunk seed=%d chunk=%d/%d: %.2f ms evaluations=%d hit=%.2f%% cached=%d bypass=%d rock-writes=%d%n",
                seed, chunkX, chunkZ, elapsed / 1_000_000.0, metrics.misses(),
                100.0 * metrics.hits() / Math.max(1, metrics.hits() + metrics.misses()),
                evaluator.size(), metrics.bypasses(), blocks);
    }

    private static void transect(ArrakisTerrainSettings settings, long seed,
            int fixed, int start, int end, boolean alongZ) {
        var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
        int n = end - start + 1;
        int[] original = new int[n], raw = new int[n], filtered = new int[n];
        int[] apron = new int[n], scree = new int[n], screeBase = new int[n];
        boolean[] major70 = new boolean[n], surface70 = new boolean[n], filtered70 = new boolean[n];
        boolean[] contact = new boolean[n];
        for (int coordinate = start; coordinate <= end; coordinate++) {
            int i = coordinate - start;
            int x = alongZ ? fixed : coordinate;
            int z = alongZ ? coordinate : fixed;
            var c = evaluator.column(x, z);
            original[i] = c.originalRockTopY();
            raw[i] = c.rockTopY();
            filtered[i] = evaluator.highestFilteredRockY(x, z);
            apron[i] = c.basalTalusApron().height();
            scree[i] = c.erosion().talusThickness();
            screeBase[i] = c.talusBaseY();
            major70[i] = original[i] >= 70 && c.erosion().occupies(70, c.materialSampleAt(70));
            surface70[i] = original[i] >= 70 && c.surfaceErosion().occupies(70, c.materialSampleAt(70));
            filtered70[i] = evaluator.rockOccupies(x, 70, z);
            var g = c.geology();
            var structural = ScarpMorphologyField.nearestMassifLowSideContact(seed, x + 0.5, z + 0.5,
                    g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
            contact[i] = structural.valid() && Math.abs(structural.signedDistance()) <= 1.0;
        }
        System.out.printf("%nContact transect seed=%d %s=%d %s=%d..%d (inclusive intervals)%n",
                seed, alongZ ? "X" : "Z", fixed, alongZ ? "Z" : "X", start, end);
        intervals("structural contact +/-1", start, n, i -> contact[i]);
        intervals("macro rock at Y70", start, n, i -> original[i] >= 70);
        intervals("major-only rock at Y70", start, n, i -> major70[i]);
        intervals("surface-only rock at Y70", start, n, i -> surface70[i]);
        intervals("filtered rock at Y70", start, n, i -> filtered70[i]);
        intervals("raw rock reaches Y84", start, n, i -> raw[i] >= 84);
        intervals("filtered rock reaches Y84", start, n, i -> filtered[i] >= 84);
        intervals("basal apron present", start, n, i -> apron[i] > 0);
        intervals("local scree present", start, n, i -> scree[i] > 0);
        for (int i = 0; i < n; i++) {
            if (apron[i] > 0 || scree[i] > 0) {
                if (i % 4 == 0) System.out.printf(
                        "deposit coordinate=%d original/raw/filtered=%d/%d/%d apron=%d local=%d fromY=%d%n",
                        start + i, original[i], raw[i], filtered[i], apron[i], scree[i], screeBase[i]);
            }
        }
    }

    private static void intervals(String label, int start, int n, IntPredicate included) {
        StringBuilder result = new StringBuilder(label).append(":");
        for (int i = 0; i < n; i++) {
            if (!included.test(i)) continue;
            int first = i;
            while (i + 1 < n && included.test(i + 1)) i++;
            result.append(' ').append(start + first).append("..").append(start + i);
        }
        System.out.println(result);
    }

    private static void measureApronGap(ArrakisTerrainSettings settings, long seed) {
        int found = 0, missed = 0, maximumGap = -1;
        String representative = "none";
        for (int z = -100; z <= 300; z += 4) {
            var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
            for (int x = 2900; x <= 3120; x += 4) {
                var c = evaluator.column(x, z);
                if (!c.basalTalusApron().active() || c.basalTalusApron().outwardDistance() > 0.0) continue;
                var g = c.geology();
                var contact = ScarpMorphologyField.nearestMassifLowSideContact(seed, x + 0.5, z + 0.5,
                        g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
                if (!contact.valid()) continue;
                int lastApron = 0;
                boolean apronEnded = false, reached = false;
                for (int step = 1; step <= 64; step++) {
                    int px = (int) Math.floor(x + 0.5 + contact.inwardX() * step);
                    int pz = (int) Math.floor(z + 0.5 + contact.inwardZ() * step);
                    var next = evaluator.column(px, pz);
                    if (!apronEnded && next.basalTalusApron().active()) lastApron = step;
                    else apronEnded = true;
                    // Diagnostic definition, not a generation rule: a surviving wall that
                    // reaches Y84 and contains solid rock at Y70 on the same inward ray.
                    if (evaluator.highestFilteredRockY(px, pz) >= 84 && evaluator.rockOccupies(px, 70, pz)) {
                        int gap = Math.max(0, step - lastApron - 1);
                        found++;
                        reached = true;
                        if (gap > maximumGap) {
                            maximumGap = gap;
                            representative = String.format(Locale.ROOT,
                                    "apron=%d/%d last-apron-step=%d wall=%d/%d step=%d fault=%.3f",
                                    x, z, lastApron, px, pz, step, next.geology().faultCarveMask());
                        }
                        break;
                    }
                }
                if (!reached) missed++;
            }
        }
        System.out.printf("Seed-%d bounded apron survey: %d rays reached Y84/Y70 wall, %d did not within 64; "
                + "largest apron-to-wall separation=%d steps; %s%n", seed, found, missed, maximumGap, representative);
    }
}
