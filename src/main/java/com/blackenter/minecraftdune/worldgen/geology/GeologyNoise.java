package com.blackenter.minecraftdune.worldgen.geology;

/** Small allocation-free deterministic noise/hash helpers shared by native geology fields. */
final class GeologyNoise {
    private GeologyNoise() {
    }

    static double value2(long seed, double x, double z) {
        long x0 = (long) Math.floor(x);
        long z0 = (long) Math.floor(z);
        double smoothX = fade(x - x0);
        double smoothZ = fade(z - z0);

        double north = lerp(
                lattice(seed, x0, 0L, z0),
                lattice(seed, x0 + 1L, 0L, z0),
                smoothX
        );
        double south = lerp(
                lattice(seed, x0, 0L, z0 + 1L),
                lattice(seed, x0 + 1L, 0L, z0 + 1L),
                smoothX
        );
        return lerp(north, south, smoothZ);
    }

    static double value3(long seed, double x, double y, double z) {
        long x0 = (long) Math.floor(x);
        long y0 = (long) Math.floor(y);
        long z0 = (long) Math.floor(z);
        double smoothX = fade(x - x0);
        double smoothY = fade(y - y0);
        double smoothZ = fade(z - z0);

        double lowerNorth = lerp(
                lattice(seed, x0, y0, z0),
                lattice(seed, x0 + 1L, y0, z0),
                smoothX
        );
        double lowerSouth = lerp(
                lattice(seed, x0, y0, z0 + 1L),
                lattice(seed, x0 + 1L, y0, z0 + 1L),
                smoothX
        );
        double upperNorth = lerp(
                lattice(seed, x0, y0 + 1L, z0),
                lattice(seed, x0 + 1L, y0 + 1L, z0),
                smoothX
        );
        double upperSouth = lerp(
                lattice(seed, x0, y0 + 1L, z0 + 1L),
                lattice(seed, x0 + 1L, y0 + 1L, z0 + 1L),
                smoothX
        );

        return lerp(
                lerp(lowerNorth, lowerSouth, smoothZ),
                lerp(upperNorth, upperSouth, smoothZ),
                smoothY
        );
    }

    static long cellSeed(long worldSeed, long cellX, long cellZ, long salt) {
        long value = worldSeed ^ salt;
        value ^= cellX * 0x9E3779B97F4A7C15L;
        value ^= cellZ * 0xC2B2AE3D27D4EB4FL;
        return mix64(value);
    }

    static double unit(long seed, long salt) {
        return (mix64(seed ^ salt) >>> 11) * 0x1.0p-53;
    }

    static double signed(long seed, long salt) {
        return unit(seed, salt) * 2.0 - 1.0;
    }

    static double foldedDistance(double coordinate, double spacing) {
        double safeSpacing = Math.max(1.0, spacing);
        double wrapped = coordinate - Math.floor(coordinate / safeSpacing) * safeSpacing;
        return Math.min(wrapped, safeSpacing - wrapped);
    }

    static double smoothStep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value < edge0 ? 0.0 : 1.0;
        }
        double normalized = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return fade(normalized);
    }

    static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double fade(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static double lattice(long seed, long x, long y, long z) {
        long value = seed;
        value ^= x * 0x9E3779B97F4A7C15L;
        value ^= y * 0xD1B54A32D192ED03L;
        value ^= z * 0xC2B2AE3D27D4EB4FL;
        value = mix64(value);
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
