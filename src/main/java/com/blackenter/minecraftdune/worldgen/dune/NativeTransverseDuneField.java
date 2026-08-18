package com.blackenter.minecraftdune.worldgen.dune;

/**
 * Continuous, chunk-order-independent transverse dune morphology for native Arrakis terrain.
 *
 * <p>This is not the iterative 64 x 64 {@code DuneSimulation}. It extracts the calibrated
 * transverse morphology that proved useful in the laboratory and evaluates it directly from
 * absolute world coordinates, making it suitable for arbitrary independently generated
 * chunks.</p>
 */
public final class NativeTransverseDuneField {
    public static final int SUBDIVISIONS = 16;
    public static final double MAX_HEIGHT_BLOCKS = 30.0;
    public static final double DUNE_SPACING_BLOCKS = 350.0;
    public static final double SPACING_VARIATION = 0.18;
    public static final double RIDGE_SHARPNESS = 3.0;
    public static final double VALLEY_CUTOFF = 0.20;
    public static final double SLOPE_ASYMMETRY = 0.82;
    public static final double WIND_ANGLE_DEGREES = 24.0;

    // Maximum of the blended 0.82-asymmetry ridge profile before exponentiation.
    // Normalizing it restores the laboratory convention that a full-suitability crest can
    // reach the configured 30-block envelope.
    private static final double PROFILE_PEAK_NORMALIZATION = 0.9225;

    private static final double TWO_PI = Math.PI * 2.0;
    private static final long PHASE_A_SALT = 0x243F6A8885A308D3L;
    private static final long PHASE_B_SALT = 0x13198A2E03707344L;
    private static final long PHASE_C_SALT = 0xA4093822299F31D0L;

    private NativeTransverseDuneField() {
    }

    public static Sample sample(
            long worldSeed,
            double worldX,
            double worldZ,
            double duneSuitability
    ) {
        double suitability = clamp(duneSuitability, 0.0, 1.0);
        if (suitability <= 0.0) {
            return new Sample(0, 0.0, 0.0);
        }

        double windRadians = Math.toRadians(WIND_ANGLE_DEGREES);
        double windX = Math.cos(windRadians);
        double windZ = Math.sin(windRadians);
        double crosswindX = -windZ;
        double crosswindZ = windX;

        double alongWind = worldX * windX + worldZ * windZ;
        double acrossWind = worldX * crosswindX + worldZ * crosswindZ;

        double phaseA = seedPhase(worldSeed, PHASE_A_SALT);
        double phaseB = seedPhase(worldSeed, PHASE_B_SALT);
        double phaseC = seedPhase(worldSeed, PHASE_C_SALT);

        double phaseWarp = SPACING_VARIATION * TWO_PI * (
                0.60 * Math.sin(
                        acrossWind * TWO_PI / (DUNE_SPACING_BLOCKS * 2.8) + phaseA
                )
                        + 0.28 * Math.sin(
                        acrossWind * TWO_PI / (DUNE_SPACING_BLOCKS * 5.3) + phaseB
                )
                        + 0.12 * Math.sin(
                        alongWind * TWO_PI / (DUNE_SPACING_BLOCKS * 4.7) + phaseC
                )
        );
        double phase = alongWind * TWO_PI / DUNE_SPACING_BLOCKS + phaseWarp;

        double ridgeBase = clamp(
                transverseRidgeBase(phase, SLOPE_ASYMMETRY)
                        / PROFILE_PEAK_NORMALIZATION,
                0.0,
                1.0
        );
        double ridge = Math.pow(ridgeBase, RIDGE_SHARPNESS);
        double cleanedRidge = smoothStep(VALLEY_CUTOFF, 1.0, ridge);

        // A very low-amplitude crosswind modulation prevents every crest from having an
        // identical height without adding stochastic contour-island noise.
        double crestModulation = 0.92 + 0.08 * (
                0.5 + 0.5 * Math.sin(
                        acrossWind * TWO_PI / (DUNE_SPACING_BLOCKS * 6.8) + phaseB
                )
        );
        double heightBlocks = MAX_HEIGHT_BLOCKS
                * suitability
                * cleanedRidge
                * crestModulation;
        int surfaceUnits = (int) Math.round(heightBlocks * SUBDIVISIONS);
        surfaceUnits = Math.max(
                0,
                Math.min((int) (MAX_HEIGHT_BLOCKS * SUBDIVISIONS), surfaceUnits)
        );

        return new Sample(
                surfaceUnits,
                surfaceUnits / (double) SUBDIVISIONS,
                cleanedRidge
        );
    }

    private static double transverseRidgeBase(double phase, double slopeAsymmetry) {
        double symmetric = (Math.sin(phase) + 1.0) * 0.5;
        if (slopeAsymmetry <= 0.0) {
            return symmetric;
        }

        double cycle = phase / TWO_PI + 0.25;
        cycle -= Math.floor(cycle);
        double crestFraction = lerp(0.50, 0.82, slopeAsymmetry);

        double asymmetric;
        if (cycle <= crestFraction) {
            double rise = cycle / crestFraction;
            asymmetric = smoothStep(0.0, 1.0, rise);
        } else {
            double fall = (cycle - crestFraction) / (1.0 - crestFraction);
            asymmetric = 1.0 - smoothStep(0.0, 1.0, fall);
        }

        return lerp(symmetric, asymmetric, slopeAsymmetry);
    }

    private static double seedPhase(long seed, long salt) {
        long mixed = mix64(seed ^ salt);
        return (mixed >>> 11) * 0x1.0p-53 * TWO_PI;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static double smoothStep(double edge0, double edge1, double value) {
        double normalized = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return normalized * normalized * (3.0 - 2.0 * normalized);
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Sample(
            int surfaceUnits,
            double heightBlocks,
            double ridgeFactor
    ) {
    }
}
