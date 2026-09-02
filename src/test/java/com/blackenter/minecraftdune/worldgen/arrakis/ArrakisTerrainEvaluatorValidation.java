package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.LithologyField;
import com.blackenter.minecraftdune.worldgen.geology.ArrakisProfileValidation;

/** Golden production-occupancy checks, including additional materials, rooting and deposits. */
public final class ArrakisTerrainEvaluatorValidation {
    // Captured only after exact agreement with a temporary copy of 9789ea8's evaluator.
    // Includes every written rock Y, additional lithology, deposits and height queries.
    private static final long EXPECTED_FINGERPRINT = 0x624F66B5A25A22A3L;
    private static final long[] SEEDS = {0L, -5640511200611798902L, 7640891576956012809L};
    private static final int[][] POINTS = {
        {0, 0}, {1500, 0}, {657, 3306}, {2553, 1706}, {3053, 190}, {3067, 106},
        {3089, 173}, {-2960, -589}, {0, 3934}, {-1, -4123}, {4500, 0}, {6600, 1},
        {-16, 4102}, {-17, 4103}, {9000, 9000}, {30000000, -30000000}
    };

    public static void main(String[] args) throws Exception {
        ArrakisTerrainSettings settings = ArrakisProfileValidation.loadProfile().settings();
        long hash = 0xCBF29CE484222325L;
        for (long seed : SEEDS) {
            for (int[] point : POINTS) {
                ArrakisTerrainEvaluator evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
                long actual = fingerprint(evaluator, point[0], point[1]);
                hash = mix(hash, actual);
                for (int limit : new int[] {0, 1, 64}) {
                    ArrakisTerrainEvaluator limited = new ArrakisTerrainEvaluator(seed, settings, limit);
                    limited.column(-123, 456); // Saturate the one-entry cache before the target.
                    require(actual == fingerprint(limited, point[0], point[1]),
                            "cache saturation changed occupancy");
                    require(limited.size() <= limit, "cache exceeded its bound");
                }
            }
        }
        require(hash == EXPECTED_FINGERPRINT, "production occupancy fingerprint changed");
        validateGenerationOrder(settings);
        validateCoordinateKeys(settings);
        validateInspection(settings);
        System.out.printf("Production evaluator fingerprint=%016x; extraction/cache comparisons passed.%n", hash);
    }

    private static void validateInspection(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(SEEDS[1], settings, 64);
        var block = evaluator.column(3067, 106);
        require(evaluator.rawRockOccupies(block, 96) && !evaluator.rockOccupies(3067, 96, 106),
                "shared evaluator lost the photographed orphan regression");
        String report = ArrakisTerrainCommand.describe(evaluator, SEEDS[1], settings, 3067, 96, 106);
        require(report.contains("raw=true kept=false") && report.contains("XYZ=3067/96/106"),
                "inspection output disagrees with production occupancy");
        var seedZero = new ArrakisTerrainEvaluator(0L, settings, 64);
        require(seedZero.highestFilteredRockY(3053, 190) == 65,
                "refactor unexpectedly retuned the remaining Seed-0 basal step");
    }

    private static void validateGenerationOrder(ArrakisTerrainSettings settings) {
        long[] forward = new long[512];
        ArrakisTerrainEvaluator first = new ArrakisTerrainEvaluator(SEEDS[1], settings, 1024);
        for (int i = 0; i < 512; i++) {
            forward[i] = fingerprint(first, 3040 + i % 32, 96 + i / 32);
        }
        ArrakisTerrainEvaluator reverse = new ArrakisTerrainEvaluator(SEEDS[1], settings, 64);
        for (int i = 511; i >= 0; i--) {
            require(forward[i] == fingerprint(reverse, 3040 + i % 32, 96 + i / 32),
                    "filtered occupancy/deposits depend on order across a chunk seam");
        }
    }

    private static void validateCoordinateKeys(ArrakisTerrainSettings settings) {
        ArrakisTerrainEvaluator evaluator = new ArrakisTerrainEvaluator(0, settings, 64);
        int[][] coordinates = {{1, -1}, {-1, 1}, {1, 1}, {-1, -1},
                {Integer.MIN_VALUE, Integer.MAX_VALUE}, {Integer.MAX_VALUE, Integer.MIN_VALUE}};
        for (int[] coordinate : coordinates) evaluator.column(coordinate[0], coordinate[1]);
        require(evaluator.size() == coordinates.length, "packed signed coordinate keys collide");
        for (int[] coordinate : coordinates) {
            require(evaluator.column(coordinate[0], coordinate[1])
                            == evaluator.column(coordinate[0], coordinate[1]),
                    "cached analytical column was not reused");
        }
    }

    static long fingerprint(ArrakisTerrainEvaluator evaluation, int x, int z) {
        ArrakisTerrainEvaluator.TerrainColumn c = evaluation.column(x, z);
        long hash = 0xCBF29CE484222325L;
        hash = mix(hash, c.rockTopY());
        hash = mix(hash, c.originalRockTopY());
        hash = mix(hash, c.fissureRockTopY());
        hash = mix(hash, c.duneSurfaceUnits());
        hash = mix(hash, c.basalTalusApron().topY());
        hash = mix(hash, c.talusBaseY());
        hash = mix(hash, c.erosion().talusThickness());
        hash = mix(hash, evaluation.highestOccupiedY(x, z));
        for (int y = 44; y <= Math.max(c.highestOccupiedY(), c.fissureRockTopY()) + 1; y++) {
            LithologyField.Sample material = c.materialSampleAt(y);
            boolean rock = c.hasNativeRock() && y <= c.rockTopY()
                    && evaluation.filteredRockOccupies(x, z, y, c, material);
            hash = mix(hash, rock ? material.material().ordinal() + 1 : 0);
            hash = mix(hash, c.basalTalusApron().materialAt(y).ordinal());
            hash = mix(hash, c.talusOccupiesY(y)
                    ? c.erosion().talusMaterialAt(y, c.lithology()).ordinal() + 1 : 0);
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        return (hash ^ value) * 0x100000001B3L;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
