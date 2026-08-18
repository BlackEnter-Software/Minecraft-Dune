package com.blackenter.minecraftdune.worldgen.geology;

/**
 * Deterministic, coordinate-based macro-geology field for the first Gameplay Arrakis region.
 *
 * <p>Version 0.5.9 separates the broad landscape into explicit geological/environmental
 * provinces instead of fading one massif mask directly into open desert. The field remains
 * chunk-order independent: every value is derived only from the world seed and absolute X/Z
 * coordinates.</p>
 */
public final class MacroGeologyField {
    public static final int BASE_SURFACE_Y = 64;
    public static final int MAX_ADDED_ROCK_HEIGHT = 176;

    public static final double ARRAKEEN_PURE_SAND_RADIUS = 800.0;
    public static final double INNER_FORELAND_END_RADIUS = 1120.0;
    public static final double MAIN_MASSIF_OUTER_RADIUS = 3020.0;
    public static final double BROKEN_ROCK_OUTER_RADIUS = 4450.0;
    public static final double SAND_ROCK_TRANSITION_OUTER_RADIUS = 5400.0;
    public static final double OPEN_ERG_START_RADIUS = 4700.0;
    public static final double OPEN_ERG_FULL_RADIUS = 5250.0;

    private static final double TWO_PI = Math.PI * 2.0;

    private static final long BOUNDARY_WARP_SALT = 0x51ED270B6D2A4F1BL;
    private static final long FORMATION_SALT = 0xA24BAED4963EE407L;
    private static final long SECONDARY_FORMATION_SALT = 0x9FB21C651E98DF25L;
    private static final long RELIEF_SALT = 0xD6E8FEB86659FD93L;
    private static final long ANGLE_PHASE_A_SALT = 0xC13FA9A902A6328FL;
    private static final long ANGLE_PHASE_B_SALT = 0x91E10DA5C79E7B1DL;
    private static final long ANGLE_PHASE_C_SALT = 0xD1B54A32D192ED03L;

    private static final long FORELAND_SALT = 0xF1357AEA2E62A9C5L;
    private static final long FORELAND_DETAIL_SALT = 0xB7E151628AED2A6BL;
    private static final long FORELAND_RELIEF_SALT = 0x8AED2A6ABF715880L;

    private static final long FAULT_SALT = 0xA0F2EC75A1FE1575L;
    private static final long FAULT_RELIEF_SALT = 0x89E182857D9ED689L;

    private static final long SAND_PASS_A_SALT = 0x6A09E667F3BCC909L;
    private static final long SAND_PASS_B_SALT = 0xBB67AE8584CAA73BL;

    private static final long OUTLIER_SALT = 0xC6BC279692B5CC83L;
    private static final long OUTLIER_DETAIL_SALT = 0xDB4F0B9175AE2165L;
    private static final long OUTLIER_RELIEF_SALT = 0xB5C0FBCFEC4D3B2FL;

    private static final long TRANSITION_SALT = 0xBBE0563303A4615FL;
    private static final long TRANSITION_DETAIL_SALT = 0xA54FF53A5F1D36F1L;
    private static final long TRANSITION_RELIEF_SALT = 0x510E527FADE682D1L;

    private static final int FAULT_COUNT = 4;

    private MacroGeologyField() {
    }

    public static Sample sample(long worldSeed, double worldX, double worldZ) {
        double radius = Math.hypot(worldX, worldZ);

        // Arrakeen's construction basin is now a strict 800-block pure-sand reservation.
        // No boundary warp, small rock, native dune or massif operator can intrude here.
        if (radius <= ARRAKEEN_PURE_SAND_RADIUS) {
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
                    0.0,
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
        ) * 320.0;
        double effectiveRadius = radius + boundaryWarp;

        // Beyond the mixed transition all geological rock fields are identically zero.
        // Return before evaluating the expensive formation/fault noise stack; far-erg chunk
        // generation then costs one low-frequency boundary sample plus the analytic dune field.
        if (effectiveRadius >= SAND_ROCK_TRANSITION_OUTER_RADIUS) {
            return new Sample(
                    radius,
                    effectiveRadius,
                    boundaryWarp,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    BASE_SURFACE_Y,
                    Province.OPEN_ERG
            );
        }

        double centralBasin = 1.0 - smoothStep(800.0, 970.0, effectiveRadius);
        double innerForeland = smoothStep(790.0, 840.0, radius)
                * (1.0 - smoothStep(1030.0, 1120.0, effectiveRadius));
        double massif = smoothStep(1000.0, 1250.0, radius)
                * smoothStep(980.0, 1280.0, effectiveRadius)
                * (1.0 - smoothStep(2920.0, 3020.0, effectiveRadius));
        double faultedMargin = smoothStep(2450.0, 2780.0, effectiveRadius)
                * (1.0 - smoothStep(3380.0, 3660.0, effectiveRadius));
        double brokenRock = smoothStep(2920.0, 3150.0, effectiveRadius)
                * (1.0 - smoothStep(4250.0, 4450.0, effectiveRadius));
        double sandRockTransition = smoothStep(3900.0, 4200.0, effectiveRadius)
                * (1.0 - smoothStep(5100.0, 5400.0, effectiveRadius));
        double openErg = smoothStep(
                OPEN_ERG_START_RADIUS,
                OPEN_ERG_FULL_RADIUS,
                effectiveRadius
        );

        // 800-~1100: disconnected knobs, shelves and small formations. This is deliberately
        // a separate morphology rather than a scaled-down Shield Wall.
        double forelandPotential =
                0.72 * fbm(
                        worldSeed ^ FORELAND_SALT,
                        worldX / 145.0,
                        worldZ / 145.0,
                        3
                )
                        + 0.28 * fbm(
                        worldSeed ^ FORELAND_DETAIL_SALT,
                        worldX / 62.0,
                        worldZ / 62.0,
                        2
                );
        double smallFormationMask = innerForeland
                * smoothStep(0.14, 0.44, forelandPotential);
        double forelandRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ FORELAND_RELIEF_SALT,
                worldX / 180.0,
                worldZ / 180.0,
                2
        );
        double smallFormationHeight = (5.0 + 23.0 * forelandRelief)
                * smoothStep(0.08, 0.65, smallFormationMask);

        // The main Shield Wall is intentionally more continuous than the 0.5.7 horseshoe.
        // Narrow faults and two explicit sand corridors now provide most of the crossings.
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
        double angle = Math.atan2(worldZ, worldX);
        double angularContinuity =
                0.28 * Math.cos(angle - seedPhase(worldSeed, ANGLE_PHASE_A_SALT))
                        + 0.14 * Math.cos(
                        2.0 * angle - seedPhase(worldSeed, ANGLE_PHASE_B_SALT)
                )
                        + 0.08 * Math.cos(
                        3.0 * angle - seedPhase(worldSeed, ANGLE_PHASE_C_SALT)
                );
        double massifPotential =
                broadFormation * 0.48
                        + secondaryFormation * 0.18
                        + massif * 0.28
                        + angularContinuity * 0.42;
        double massifContinuity = smoothStep(-0.38, -0.02, massifPotential);
        double massifMask = massif * massifContinuity;
        double massifShape = smoothStep(0.07, 0.58, massifMask);
        double massifRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ RELIEF_SALT,
                worldX / 1300.0,
                worldZ / 1300.0,
                3
        );
        double massifHeight = (
                30.0
                        + 105.0 * massif
                        + 30.0 * massifRelief
        ) * massifShape;

        double sandCorridorMask = sandCorridorMask(
                worldSeed,
                worldX,
                worldZ,
                radius
        );
        double faultCarveMask = faultNetworkMask(
                worldSeed,
                worldX,
                worldZ,
                radius
        );

        // The outer Shield Wall does not simply fade away anymore. The massif body ends over
        // a comparatively narrow band; a separate outlier field then produces broken rock.
        double outlierPotential =
                0.68 * fbm(
                        worldSeed ^ OUTLIER_SALT,
                        worldX / 420.0,
                        worldZ / 420.0,
                        4
                )
                        + 0.32 * fbm(
                        worldSeed ^ OUTLIER_DETAIL_SALT,
                        worldX / 190.0,
                        worldZ / 190.0,
                        3
                );
        double outlierMask = brokenRock * smoothStep(0.10, 0.34, outlierPotential);
        double outlierShape = smoothStep(0.12, 0.70, outlierMask);
        double outlierRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ OUTLIER_RELIEF_SALT,
                worldX / 600.0,
                worldZ / 600.0,
                3
        );
        double outlierHeight = (12.0 + 55.0 * outlierRelief) * outlierShape;

        // The sand-rock transition carries lower, smaller remnants before the true erg.
        double transitionPotential =
                0.70 * fbm(
                        worldSeed ^ TRANSITION_SALT,
                        worldX / 260.0,
                        worldZ / 260.0,
                        3
                )
                        + 0.30 * fbm(
                        worldSeed ^ TRANSITION_DETAIL_SALT,
                        worldX / 100.0,
                        worldZ / 100.0,
                        2
                );
        double transitionRockMask = sandRockTransition
                * smoothStep(0.20, 0.45, transitionPotential);
        double transitionRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ TRANSITION_RELIEF_SALT,
                worldX / 350.0,
                worldZ / 350.0,
                2
        );
        double transitionRockHeight = (4.0 + 22.0 * transitionRelief)
                * smoothStep(0.10, 0.65, transitionRockMask);

        double rawRockMask = Math.max(
                smallFormationMask,
                Math.max(
                        massifMask,
                        Math.max(outlierMask, transitionRockMask)
                )
        );
        double addedRockHeight = Math.max(
                smallFormationHeight,
                Math.max(
                        massifHeight,
                        Math.max(outlierHeight, transitionRockHeight)
                )
        );

        // The two seeded sand corridors are broad enough for navigation and sand transport.
        // They fully suppress the provisional rock mass through the Shield Wall and continue
        // into the outer broken-rock province.
        addedRockHeight *= 1.0 - sandCorridorMask;
        rawRockMask *= 1.0 - sandCorridorMask;

        // Fault ravines are narrower and normally retain a low rocky floor. They read as
        // structural cuts instead of wide missing sectors in the massif.
        if (faultCarveMask > 0.0 && addedRockHeight > 0.0) {
            double faultFloor = 3.0 + 5.0 * (
                    0.5 + 0.5 * fbm(
                            worldSeed ^ FAULT_RELIEF_SALT,
                            worldX / 230.0,
                            worldZ / 230.0,
                            2
                    )
            );
            addedRockHeight = lerp(
                    addedRockHeight,
                    Math.min(addedRockHeight, faultFloor),
                    faultCarveMask * 0.93
            );
            rawRockMask *= 1.0 - 0.72 * faultCarveMask;
        }

        addedRockHeight = clamp(
                addedRockHeight,
                0.0,
                MAX_ADDED_ROCK_HEIGHT
        );
        double baseElevation = BASE_SURFACE_Y + addedRockHeight;
        double rockFormationMask = clamp(rawRockMask, 0.0, 1.0);

        // Dunes begin as low broken-desert forms, become common in the transition, and reach
        // their full calibrated 30-block envelope in the open erg. Rock height strongly
        // suppresses them; this lets dunes occupy sand corridors around outcrops without
        // simply coating tall formations.
        double duneProvinceStrength = Math.max(
                0.18 * brokenRock,
                Math.max(0.68 * sandRockTransition, openErg)
        );
        double duneSuitability = clamp(
                duneProvinceStrength
                        * (1.0 - smoothStep(2.0, 18.0, addedRockHeight)),
                0.0,
                1.0
        );

        Province dominantProvince = dominantProvince(
                centralBasin,
                innerForeland,
                massif,
                faultedMargin,
                brokenRock,
                sandRockTransition,
                openErg
        );

        return new Sample(
                radius,
                effectiveRadius,
                boundaryWarp,
                centralBasin,
                innerForeland,
                massif,
                faultedMargin,
                brokenRock,
                sandRockTransition,
                openErg,
                smallFormationMask,
                rockFormationMask,
                faultCarveMask,
                sandCorridorMask,
                duneSuitability,
                baseElevation,
                dominantProvince
        );
    }

    private static double faultNetworkMask(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius
    ) {
        double radialGate = smoothStep(1050.0, 1350.0, radius)
                * (1.0 - smoothStep(3350.0, 3700.0, radius));
        if (radialGate <= 0.0) {
            return 0.0;
        }

        double strongest = 0.0;
        for (int fault = 0; fault < FAULT_COUNT; fault++) {
            long step = (long) fault * 0x9E3779B97F4A7C15L;
            long faultSalt = FAULT_SALT + step;
            double direction = seedPhase(worldSeed, faultSalt);
            double directionX = Math.cos(direction);
            double directionZ = Math.sin(direction);
            double along = worldX * directionX + worldZ * directionZ;
            double perpendicular = -worldX * directionZ + worldZ * directionX;

            double offsetUnit = seedSignedUnit(
                    worldSeed,
                    FAULT_SALT + (long) fault * 0xD1B54A32D192ED03L
            );
            double offset = Math.copySign(
                    350.0 + 650.0 * Math.abs(offsetUnit),
                    offsetUnit == 0.0 ? 1.0 : offsetUnit
            );
            double warp =
                    110.0 * Math.sin(
                            along / 720.0
                                    + seedPhase(
                                    worldSeed,
                                    ANGLE_PHASE_A_SALT + (long) fault * 101L
                            )
                    )
                            + 65.0 * fbm(
                            worldSeed ^ (faultSalt + 0x632BE59BD9B4E019L),
                            worldX / 900.0,
                            worldZ / 900.0,
                            3
                    );

            double distance = Math.abs(perpendicular - offset - warp);
            double faultMask = (1.0 - smoothStep(30.0, 105.0, distance))
                    * radialGate;
            strongest = Math.max(strongest, faultMask);
        }
        return clamp(strongest, 0.0, 1.0);
    }

    private static double sandCorridorMask(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius
    ) {
        double radialGate = smoothStep(1000.0, 1320.0, radius)
                * (1.0 - smoothStep(4050.0, 4450.0, radius));
        if (radialGate <= 0.0) {
            return 0.0;
        }

        double angle = Math.atan2(worldZ, worldX);
        double primaryAngle = seedPhase(worldSeed, SAND_PASS_A_SALT);
        double secondOffset = Math.PI
                + seedSignedUnit(worldSeed, SAND_PASS_B_SALT) * 0.55;
        double secondaryAngle = primaryAngle + secondOffset;

        double primaryCurve = 0.12 * Math.sin(
                radius / 750.0 + seedPhase(worldSeed, ANGLE_PHASE_A_SALT)
        );
        double secondaryCurve = 0.10 * Math.sin(
                radius / 910.0 + seedPhase(worldSeed, ANGLE_PHASE_B_SALT)
        );

        double primaryDistance = Math.abs(wrappedAngleDifference(
                angle,
                primaryAngle + primaryCurve
        )) * Math.max(radius, 800.0);
        double secondaryDistance = Math.abs(wrappedAngleDifference(
                angle,
                secondaryAngle + secondaryCurve
        )) * Math.max(radius, 800.0);

        double primary = 1.0 - smoothStep(105.0, 225.0, primaryDistance);
        double secondary = 1.0 - smoothStep(135.0, 285.0, secondaryDistance);
        return clamp(Math.max(primary, secondary) * radialGate, 0.0, 1.0);
    }

    private static Province dominantProvince(
            double centralBasin,
            double innerForeland,
            double massif,
            double faultedMargin,
            double brokenRock,
            double sandRockTransition,
            double openErg
    ) {
        Province province = Province.CENTRAL_BASIN;
        double best = centralBasin;
        if (innerForeland > best) {
            best = innerForeland;
            province = Province.INNER_ROCK_FORELAND;
        }
        if (massif > best) {
            best = massif;
            province = Province.SHIELD_WALL_MASSIF;
        }
        if (faultedMargin > best) {
            best = faultedMargin;
            province = Province.FAULTED_MARGIN;
        }
        if (brokenRock > best) {
            best = brokenRock;
            province = Province.BROKEN_ROCK_DESERT;
        }
        if (sandRockTransition > best) {
            best = sandRockTransition;
            province = Province.SAND_ROCK_TRANSITION;
        }
        if (openErg > best) {
            province = Province.OPEN_ERG;
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

    private static double seedSignedUnit(long seed, long salt) {
        long mixed = mix64(seed ^ salt);
        return ((mixed >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static double wrappedAngleDifference(double first, double second) {
        double difference = first - second;
        while (difference > Math.PI) {
            difference -= TWO_PI;
        }
        while (difference < -Math.PI) {
            difference += TWO_PI;
        }
        return difference;
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
        INNER_ROCK_FORELAND("inner_rock_foreland"),
        SHIELD_WALL_MASSIF("shield_wall_massif"),
        FAULTED_MARGIN("faulted_margin"),
        BROKEN_ROCK_DESERT("broken_rock_desert"),
        SAND_ROCK_TRANSITION("sand_rock_transition"),
        OPEN_ERG("open_erg");

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
            double innerForelandWeight,
            double massifWeight,
            double faultedMarginWeight,
            double brokenRockWeight,
            double sandRockTransitionWeight,
            double openErgWeight,
            double smallFormationMask,
            double rockFormationMask,
            double faultCarveMask,
            double sandCorridorMask,
            double duneSuitability,
            double baseElevation,
            Province dominantProvince
    ) {
        public double addedRockHeight() {
            return baseElevation - BASE_SURFACE_Y;
        }
    }
}
