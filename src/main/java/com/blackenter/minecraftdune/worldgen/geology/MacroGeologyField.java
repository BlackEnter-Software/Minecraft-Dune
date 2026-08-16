package com.blackenter.minecraftdune.worldgen.geology;

/**
 * Deterministic, coordinate-based macro-geology field for the first Gameplay Arrakis region.
 *
 * <p>This is intentionally a coarse geological framework rather than a final terrain model.
 * The field reserves a flat Arrakeen basin around the world origin, permits an irregular
 * shield-wall/massif province outside it, breaks that province into an eroded outer margin,
 * and fades into open desert at roughly four kilometres from the origin.</p>
 */
public final class MacroGeologyField {
    public static final int BASE_SURFACE_Y = 64;
    public static final int MAX_ADDED_ROCK_HEIGHT = 176;

    public static final double ARRAKEEN_FLAT_RADIUS = 1000.0;
    public static final double OPEN_DESERT_START_RADIUS = 3600.0;
    public static final double OPEN_DESERT_FULL_RADIUS = 4200.0;

    private static final double TWO_PI = Math.PI * 2.0;

    private static final long BOUNDARY_WARP_SALT = 0x51ED270B6D2A4F1BL;
    private static final long FORMATION_SALT = 0xA24BAED4963EE407L;
    private static final long SECONDARY_FORMATION_SALT = 0x9FB21C651E98DF25L;
    private static final long RELIEF_SALT = 0xD6E8FEB86659FD93L;
    private static final long ANGLE_PHASE_A_SALT = 0xC13FA9A902A6328FL;
    private static final long ANGLE_PHASE_B_SALT = 0x91E10DA5C79E7B1DL;
    private static final long ANGLE_PHASE_C_SALT = 0xD1B54A32D192ED03L;

    private MacroGeologyField() {
    }

    public static Sample sample(long worldSeed, double worldX, double worldZ) {
        double radius = Math.hypot(worldX, worldZ);

        // The future Arrakeen construction basin is a hard reservation. Boundary warping
        // is deliberately disabled inside the first kilometre so rock can never intrude.
        if (radius <= ARRAKEEN_FLAT_RADIUS) {
            return new Sample(
                    radius,
                    radius,
                    0.0,
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    BASE_SURFACE_Y,
                    Province.CENTRAL_BASIN
            );
        }

        double boundaryWarp = fbm(
                worldSeed ^ BOUNDARY_WARP_SALT,
                worldX / 1800.0,
                worldZ / 1800.0,
                4
        ) * 250.0;
        double effectiveRadius = radius + boundaryWarp;

        double centralBasin = 1.0 - smoothStep(1000.0, 1400.0, effectiveRadius);
        double rockTransition = smoothStep(1000.0, 1450.0, effectiveRadius)
                * (1.0 - smoothStep(1850.0, 2450.0, effectiveRadius));
        double massif = smoothStep(1250.0, 1700.0, effectiveRadius)
                * (1.0 - smoothStep(2850.0, 3300.0, effectiveRadius));
        double erodedMargin = smoothStep(2600.0, 3100.0, effectiveRadius)
                * (1.0 - smoothStep(3850.0, 4300.0, effectiveRadius));
        double openDesert = smoothStep(
                OPEN_DESERT_START_RADIUS,
                OPEN_DESERT_FULL_RADIUS,
                effectiveRadius
        );

        double rockEnvelope = Math.max(
                rockTransition * 0.72,
                Math.max(massif, erodedMargin * 0.62)
        ) * (1.0 - openDesert * 0.92);

        double broadFormation = fbm(
                worldSeed ^ FORMATION_SALT,
                worldX / 1150.0,
                worldZ / 1150.0,
                4
        );
        double secondaryFormation = fbm(
                worldSeed ^ SECONDARY_FORMATION_SALT,
                worldX / 620.0,
                worldZ / 620.0,
                3
        );

        // Seeded angular lobes deliberately break the permitted rock province into a broad
        // horseshoe / broken shield rather than revealing a circular annulus around 0,0.
        double angle = Math.atan2(worldZ, worldX);
        double angularContinuity =
                0.28 * Math.cos(angle - seedPhase(worldSeed, ANGLE_PHASE_A_SALT))
                        + 0.14 * Math.cos(2.0 * angle - seedPhase(worldSeed, ANGLE_PHASE_B_SALT))
                        + 0.08 * Math.cos(3.0 * angle - seedPhase(worldSeed, ANGLE_PHASE_C_SALT));

        double formationPotential =
                broadFormation * 0.52
                        + secondaryFormation * 0.20
                        + massif * 0.22
                        + rockTransition * 0.08
                        + angularContinuity * 0.85
                        - erodedMargin * 0.06;
        double continuity = smoothStep(-0.20, 0.22, formationPotential);
        double rockFormationMask = clamp(rockEnvelope * continuity, 0.0, 1.0);

        double reliefNoise = 0.5 + 0.5 * fbm(
                worldSeed ^ RELIEF_SALT,
                worldX / 1300.0,
                worldZ / 1300.0,
                3
        );
        double targetAddedHeight =
                28.0
                        + 100.0 * massif
                        + 38.0 * rockTransition
                        + 34.0 * erodedMargin
                        + 30.0 * reliefNoise;

        // Steepen the broad formation boundary without introducing detailed erosion yet.
        // Later releases can replace this crude slab with strata, escarpments and talus.
        double massShape = smoothStep(0.08, 0.70, rockFormationMask);
        double addedRockHeight = clamp(
                targetAddedHeight * massShape,
                0.0,
                MAX_ADDED_ROCK_HEIGHT
        );
        double baseElevation = BASE_SURFACE_Y + addedRockHeight;

        Province dominantProvince = dominantProvince(
                centralBasin,
                rockTransition,
                massif,
                erodedMargin,
                openDesert
        );

        return new Sample(
                radius,
                effectiveRadius,
                boundaryWarp,
                centralBasin,
                rockTransition,
                massif,
                erodedMargin,
                openDesert,
                rockFormationMask,
                baseElevation,
                dominantProvince
        );
    }

    private static Province dominantProvince(
            double centralBasin,
            double rockTransition,
            double massif,
            double erodedMargin,
            double openDesert
    ) {
        Province province = Province.CENTRAL_BASIN;
        double best = centralBasin;
        if (rockTransition > best) {
            best = rockTransition;
            province = Province.ROCK_TRANSITION;
        }
        if (massif > best) {
            best = massif;
            province = Province.SHIELD_WALL_MASSIF;
        }
        if (erodedMargin > best) {
            best = erodedMargin;
            province = Province.ERODED_MARGIN;
        }
        if (openDesert > best) {
            province = Province.OPEN_DESERT;
        }
        return province;
    }

    private static double fbm(long seed, double x, double z, int octaves) {
        double result = 0.0;
        double normalization = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        for (int octave = 0; octave < octaves; octave++) {
            result += valueNoise(
                    seed + (long) octave * 0x9E3779B97F4A7C15L,
                    x * frequency,
                    z * frequency
            ) * amplitude;
            normalization += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        return result / normalization;
    }

    private static double valueNoise(long seed, double x, double z) {
        long x0 = (long) Math.floor(x);
        long z0 = (long) Math.floor(z);
        double fractionX = x - x0;
        double fractionZ = z - z0;
        double smoothX = fractionX * fractionX * (3.0 - 2.0 * fractionX);
        double smoothZ = fractionZ * fractionZ * (3.0 - 2.0 * fractionZ);

        double northWest = latticeValue(seed, x0, z0);
        double northEast = latticeValue(seed, x0 + 1L, z0);
        double southWest = latticeValue(seed, x0, z0 + 1L);
        double southEast = latticeValue(seed, x0 + 1L, z0 + 1L);
        double north = lerp(northWest, northEast, smoothX);
        double south = lerp(southWest, southEast, smoothX);
        return lerp(north, south, smoothZ);
    }

    private static double latticeValue(long seed, long x, long z) {
        long value = seed;
        value ^= x * 0x9E3779B97F4A7C15L;
        value ^= z * 0xC2B2AE3D27D4EB4FL;
        value = mix64(value);
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
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

    public enum Province {
        CENTRAL_BASIN("central_basin"),
        ROCK_TRANSITION("rock_transition"),
        SHIELD_WALL_MASSIF("shield_wall_massif"),
        ERODED_MARGIN("eroded_margin"),
        OPEN_DESERT("open_desert");

        private final String commandName;

        Province(String commandName) {
            this.commandName = commandName;
        }

        public String commandName() {
            return commandName;
        }
    }

    public record Sample(
            double radiusBlocks,
            double effectiveRadiusBlocks,
            double boundaryWarpBlocks,
            double centralBasinWeight,
            double rockTransitionWeight,
            double massifWeight,
            double erodedMarginWeight,
            double openDesertWeight,
            double rockFormationMask,
            double baseElevation,
            Province dominantProvince
    ) {
        public double addedRockHeight() {
            return baseElevation - BASE_SURFACE_Y;
        }
    }
}
