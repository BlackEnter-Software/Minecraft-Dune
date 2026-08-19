package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Deterministic, coordinate-based macro geology for Gameplay Arrakis.
 *
 * <p>0.5.10 keeps the 0.5.9 province architecture but moves its principal tuning values
 * into {@link ArrakisTerrainSettings}. Broken rock persists farther outward, the foreland
 * gains a second micro-rock scale, fault centerlines meander more strongly, and some fault
 * segments are fully sand-floored instead of retaining a narrow rock fence.</p>
 */
public final class MacroGeologyField {
    public static final int BASE_SURFACE_Y = 64;

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
    private static final long FORELAND_MICRO_SALT = 0xC2B2AE3D27D4EB4FL;
    private static final long FORELAND_MICRO_RELIEF_SALT = 0x165667B19E3779F9L;

    private static final long FAULT_SALT = 0xA0F2EC75A1FE1575L;
    private static final long FAULT_RELIEF_SALT = 0x89E182857D9ED689L;
    private static final long FAULT_BROAD_WARP_SALT = 0x94D049BB133111EBL;
    private static final long FAULT_MEDIUM_WARP_SALT = 0xBF58476D1CE4E5B9L;
    private static final long FAULT_SAND_FLOOR_SALT = 0xD1342543DE82EF95L;

    private static final long SAND_PASS_A_SALT = 0x6A09E667F3BCC909L;
    private static final long SAND_PASS_B_SALT = 0xBB67AE8584CAA73BL;

    private static final long OUTLIER_SALT = 0xC6BC279692B5CC83L;
    private static final long OUTLIER_DETAIL_SALT = 0xDB4F0B9175AE2165L;
    private static final long OUTLIER_RELIEF_SALT = 0xB5C0FBCFEC4D3B2FL;
    private static final long OUTLIER_MICRO_SALT = 0x8CB92BA72F3D8DD7L;
    private static final long OUTLIER_MICRO_RELIEF_SALT = 0x9E3779B97F4A7C15L;

    private static final long TRANSITION_SALT = 0xBBE0563303A4615FL;
    private static final long TRANSITION_DETAIL_SALT = 0xA54FF53A5F1D36F1L;
    private static final long TRANSITION_RELIEF_SALT = 0x510E527FADE682D1L;

    private MacroGeologyField() {
    }

    public static Sample sample(long worldSeed, double worldX, double worldZ) {
        return sample(worldSeed, worldX, worldZ, ArrakisTerrainSettings.DEFAULT);
    }

    public static Sample sample(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.BasinSettings basinSettings = settings.basin();
        ArrakisTerrainSettings.ForelandSettings forelandSettings = settings.foreland();
        ArrakisTerrainSettings.MassifSettings massifSettings = settings.massif();
        ArrakisTerrainSettings.BrokenRockSettings brokenSettings = settings.brokenRock();
        ArrakisTerrainSettings.OuterTransitionSettings transitionSettings =
                settings.outerTransition();
        ArrakisTerrainSettings.NativeDuneSettings duneSettings = settings.nativeDunes();

        double radius = Math.hypot(worldX, worldZ);

        if (radius <= basinSettings.pureSandRadius()) {
            return emptySample(radius, radius, 0.0, Province.CENTRAL_BASIN, 1.0);
        }

        double boundaryWarp = fbm(
                worldSeed ^ BOUNDARY_WARP_SALT,
                worldX / 1800.0,
                worldZ / 1800.0,
                4
        ) * 320.0;
        double effectiveRadius = radius + boundaryWarp;

        if (effectiveRadius >= transitionSettings.openErgFullRadius()) {
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
                    0.0,
                    1.0,
                    BASE_SURFACE_Y,
                    Province.OPEN_ERG
            );
        }

        double centralBasin = 1.0 - smoothStep(
                basinSettings.pureSandRadius(),
                basinSettings.transitionEndRadius(),
                effectiveRadius
        );
        double innerForeland = smoothStep(
                basinSettings.pureSandRadius() - 10.0,
                basinSettings.pureSandRadius() + 40.0,
                radius
        ) * (1.0 - smoothStep(
                forelandSettings.endRadius() - 100.0,
                forelandSettings.endRadius(),
                effectiveRadius
        ));

        double massif = smoothStep(
                massifSettings.startRadius(),
                massifSettings.fullRadius(),
                radius
        ) * smoothStep(
                massifSettings.startRadius() - 20.0,
                massifSettings.fullRadius() + 30.0,
                effectiveRadius
        ) * (1.0 - smoothStep(
                massifSettings.outerStartRadius(),
                massifSettings.outerEndRadius(),
                effectiveRadius
        ));

        double faultedMargin = smoothStep(
                massifSettings.outerStartRadius() - 570.0,
                massifSettings.outerStartRadius() - 240.0,
                effectiveRadius
        ) * (1.0 - smoothStep(
                massifSettings.outerEndRadius() + 360.0,
                massifSettings.outerEndRadius() + 640.0,
                effectiveRadius
        ));

        double brokenRock = smoothStep(
                brokenSettings.startRadius(),
                brokenSettings.fullRadius(),
                effectiveRadius
        ) * (1.0 - smoothStep(
                brokenSettings.outerFadeStartRadius(),
                brokenSettings.outerRadius(),
                effectiveRadius
        ));

        double sandRockTransition = smoothStep(
                transitionSettings.startRadius(),
                transitionSettings.fullRadius(),
                effectiveRadius
        ) * (1.0 - smoothStep(
                transitionSettings.fadeStartRadius(),
                transitionSettings.outerRadius(),
                effectiveRadius
        ));

        double openErg = smoothStep(
                transitionSettings.openErgStartRadius(),
                transitionSettings.openErgFullRadius(),
                effectiveRadius
        );

        // Medium foreland knobs.
        double forelandPotential =
                0.72 * fbm(
                        worldSeed ^ FORELAND_SALT,
                        worldX / forelandSettings.largeScale(),
                        worldZ / forelandSettings.largeScale(),
                        3
                )
                        + 0.28 * fbm(
                        worldSeed ^ FORELAND_DETAIL_SALT,
                        worldX / forelandSettings.detailScale(),
                        worldZ / forelandSettings.detailScale(),
                        2
                );
        double largeForelandMask = innerForeland * smoothStep(
                forelandSettings.largeThresholdLow(),
                forelandSettings.largeThresholdHigh(),
                forelandPotential
        );
        double forelandRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ FORELAND_RELIEF_SALT,
                worldX / (forelandSettings.largeScale() * 1.25),
                worldZ / (forelandSettings.largeScale() * 1.25),
                2
        );
        double largeForelandHeight = (
                forelandSettings.largeMinHeight()
                        + (
                        forelandSettings.largeMaxHeight()
                                - forelandSettings.largeMinHeight()
                ) * forelandRelief
        ) * smoothStep(0.08, 0.65, largeForelandMask);

        // New micro-rock scale: more numerous, lower remnants between the larger knobs.
        double microForelandPotential =
                0.68 * fbm(
                        worldSeed ^ FORELAND_MICRO_SALT,
                        worldX / forelandSettings.microScale(),
                        worldZ / forelandSettings.microScale(),
                        3
                )
                        + 0.32 * fbm(
                        worldSeed ^ FORELAND_DETAIL_SALT,
                        worldX / (forelandSettings.microScale() * 0.48),
                        worldZ / (forelandSettings.microScale() * 0.48),
                        2
                );
        double microForelandMask = innerForeland
                * smoothStep(
                        forelandSettings.microThresholdLow(),
                        forelandSettings.microThresholdHigh(),
                        microForelandPotential
                )
                * (1.0 - 0.40 * largeForelandMask);
        double microForelandRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ FORELAND_MICRO_RELIEF_SALT,
                worldX / (forelandSettings.microScale() * 1.7),
                worldZ / (forelandSettings.microScale() * 1.7),
                2
        );
        double microForelandHeight = (
                1.5
                        + (
                        forelandSettings.microMaxHeight() - 1.5
                ) * microForelandRelief
        ) * smoothStep(0.04, 0.58, microForelandMask);

        double smallFormationMask = Math.max(
                largeForelandMask,
                microForelandMask
        );
        double smallFormationHeight = Math.max(
                largeForelandHeight,
                microForelandHeight
        );

        // Main Shield Wall remains intentionally close to 0.5.9.
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
        double massifContinuity = smoothStep(
                massifSettings.continuityLow(),
                massifSettings.continuityHigh(),
                massifPotential
        );
        double massifMask = massif * massifContinuity;
        double massifShape = smoothStep(
                massifSettings.shapeLow(),
                massifSettings.shapeHigh(),
                massifMask
        );
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
                radius,
                settings.sandPasses()
        );
        FaultNetworkSample fault = faultNetworkSample(
                worldSeed,
                worldX,
                worldZ,
                radius,
                settings.faults()
        );

        // Broken Rock Desert persists farther and progressively changes scale.
        double brokenProgress = smoothStep(
                brokenSettings.fullRadius(),
                brokenSettings.outerRadius(),
                effectiveRadius
        );

        double outlierPotential =
                0.62 * fbm(
                        worldSeed ^ OUTLIER_SALT,
                        worldX / brokenSettings.largeScale(),
                        worldZ / brokenSettings.largeScale(),
                        4
                )
                        + 0.38 * fbm(
                        worldSeed ^ OUTLIER_DETAIL_SALT,
                        worldX / brokenSettings.detailScale(),
                        worldZ / brokenSettings.detailScale(),
                        3
                );
        double largeOutlierMask = brokenRock
                * (1.0 - 0.38 * brokenProgress)
                * smoothStep(
                        lerp(0.06, 0.28, brokenProgress),
                        lerp(0.31, 0.55, brokenProgress),
                        outlierPotential
                );

        double microOutlierPotential =
                0.66 * fbm(
                        worldSeed ^ OUTLIER_MICRO_SALT,
                        worldX / brokenSettings.microScale(),
                        worldZ / brokenSettings.microScale(),
                        3
                )
                        + 0.34 * fbm(
                        worldSeed ^ OUTLIER_DETAIL_SALT,
                        worldX / (brokenSettings.microScale() * 0.48),
                        worldZ / (brokenSettings.microScale() * 0.48),
                        2
                );
        double microOutlierMask = brokenRock
                * (0.24 + 0.76 * brokenProgress)
                * smoothStep(
                        lerp(0.08, 0.24, brokenProgress),
                        lerp(0.34, 0.50, brokenProgress),
                        microOutlierPotential
                );
        double outlierMask = Math.max(
                largeOutlierMask,
                microOutlierMask
        );

        double outlierRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ OUTLIER_RELIEF_SALT,
                worldX / 600.0,
                worldZ / 600.0,
                3
        );
        double largeOutlierMaxHeight = lerp(
                brokenSettings.maxHeightInner(),
                brokenSettings.maxHeightOuter(),
                brokenProgress
        );
        double largeOutlierHeight = (
                8.0
                        + Math.max(
                        0.0,
                        largeOutlierMaxHeight - 8.0
                ) * outlierRelief
        ) * smoothStep(0.10, 0.58, largeOutlierMask);

        double microOutlierRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ OUTLIER_MICRO_RELIEF_SALT,
                worldX / (brokenSettings.microScale() * 2.1),
                worldZ / (brokenSettings.microScale() * 2.1),
                2
        );
        double microOutlierMaxHeight = lerp(
                Math.min(18.0, brokenSettings.maxHeightInner()),
                brokenSettings.microMaxHeight(),
                brokenProgress
        );
        double microOutlierHeight = (
                2.0
                        + Math.max(
                        0.0,
                        microOutlierMaxHeight - 2.0
                ) * microOutlierRelief
        ) * smoothStep(0.08, 0.58, microOutlierMask);

        double outlierHeight = Math.max(
                largeOutlierHeight,
                microOutlierHeight
        );

        // Outer mixed transition: progressively sparse low remnants.
        double transitionPotential =
                0.68 * fbm(
                        worldSeed ^ TRANSITION_SALT,
                        worldX / 235.0,
                        worldZ / 235.0,
                        3
                )
                        + 0.32 * fbm(
                        worldSeed ^ TRANSITION_DETAIL_SALT,
                        worldX / 88.0,
                        worldZ / 88.0,
                        2
                );
        double transitionProgress = smoothStep(
                transitionSettings.fullRadius(),
                transitionSettings.outerRadius(),
                effectiveRadius
        );
        double transitionRockMask = sandRockTransition
                * (1.0 - 0.45 * transitionProgress)
                * smoothStep(
                        lerp(0.18, 0.31, transitionProgress),
                        lerp(0.43, 0.56, transitionProgress),
                        transitionPotential
                );
        double transitionRelief = 0.5 + 0.5 * fbm(
                worldSeed ^ TRANSITION_RELIEF_SALT,
                worldX / 320.0,
                worldZ / 320.0,
                2
        );
        double transitionRockHeight = (
                3.0
                        + lerp(
                        18.0,
                        8.0,
                        transitionProgress
                ) * transitionRelief
        ) * smoothStep(0.10, 0.62, transitionRockMask);

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

        // Sand corridors are final suppressors.
        addedRockHeight *= 1.0 - sandCorridorMask;
        rawRockMask *= 1.0 - sandCorridorMask;

        // Fault carving is deliberately final so no later outlier can form a fence across it.
        double effectiveFaultSandFloorMask = smoothStep(
                0.55,
                0.90,
                fault.sandFloorMask()
        );

        if (fault.carveMask() > 0.0 && addedRockHeight > 0.0) {
            if (effectiveFaultSandFloorMask > 0.0) {
                addedRockHeight *= 1.0 - effectiveFaultSandFloorMask;
                rawRockMask *= 1.0 - effectiveFaultSandFloorMask;
            }

            double rockyFaultMask = fault.carveMask()
                    * (1.0 - effectiveFaultSandFloorMask);
            if (rockyFaultMask > 0.0 && addedRockHeight > 0.0) {
                double faultFloor = 2.5 + 4.5 * (
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
                        rockyFaultMask * 0.96
                );
                rawRockMask *= 1.0 - 0.85 * rockyFaultMask;
            }
        }

        addedRockHeight = clamp(
                addedRockHeight,
                0.0,
                massifSettings.maxAddedHeight()
        );
        double rockFormationMask = clamp(rawRockMask, 0.0, 1.0);
        double baseElevation = BASE_SURFACE_Y + addedRockHeight;

        double duneProvinceStrength = Math.max(
                duneSettings.brokenRockWeight() * brokenRock,
                Math.max(
                        duneSettings.transitionWeight() * sandRockTransition,
                        openErg
                )
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
                fault.carveMask(),
                effectiveFaultSandFloorMask,
                sandCorridorMask,
                duneSuitability,
                baseElevation,
                dominantProvince
        );
    }

    private static Sample emptySample(
            double radius,
            double effectiveRadius,
            double boundaryWarp,
            Province province,
            double centralBasinWeight
    ) {
        return new Sample(
                radius,
                effectiveRadius,
                boundaryWarp,
                centralBasinWeight,
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
                0.0,
                BASE_SURFACE_Y,
                province
        );
    }

    private static FaultNetworkSample faultNetworkSample(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            ArrakisTerrainSettings.FaultSettings settings
    ) {
        double radialGate = smoothStep(
                settings.startRadius(),
                settings.fullRadius(),
                radius
        ) * (1.0 - smoothStep(
                settings.fadeStartRadius(),
                settings.endRadius(),
                radius
        ));

        if (radialGate <= 0.0 || settings.count() <= 0) {
            return new FaultNetworkSample(0.0, 0.0);
        }

        double strongestCarve = 0.0;
        double strongestSandFloor = 0.0;

        for (int fault = 0; fault < settings.count(); fault++) {
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

            // Centerline warp is evaluated mainly along the fault, so the trace itself bends.
            double broadWarp = settings.broadWarpStrength() * valueNoise(
                    worldSeed ^ (FAULT_BROAD_WARP_SALT + step),
                    along / settings.broadWarpScale(),
                    fault * 17.125
            );
            double mediumWarp = settings.mediumWarpStrength() * valueNoise(
                    worldSeed ^ (FAULT_MEDIUM_WARP_SALT + step),
                    along / settings.mediumWarpScale(),
                    fault * 29.75
            );
            double sineWarp = settings.sineWarpStrength() * Math.sin(
                    along / settings.sineWarpScale()
                            + seedPhase(
                            worldSeed,
                            ANGLE_PHASE_A_SALT + (long) fault * 101L
                    )
            );
            double secondarySine = settings.sineWarpStrength() * 0.35 * Math.sin(
                    along / (settings.sineWarpScale() * 0.43)
                            + seedPhase(
                            worldSeed,
                            ANGLE_PHASE_B_SALT + (long) fault * 173L
                    )
            );

            double centerline = offset
                    + broadWarp
                    + mediumWarp
                    + sineWarp
                    + secondarySine;

            double distance = Math.abs(perpendicular - centerline);
            double faultMask = (
                    1.0 - smoothStep(
                            settings.coreWidth(),
                            settings.outerWidth(),
                            distance
                    )
            ) * radialGate;

            if (faultMask <= 0.0) {
                continue;
            }

            // Low-frequency along-fault variation creates intermittent sandy basin floors.
            double floorNoise = 0.5 + 0.5 * valueNoise(
                    worldSeed ^ (FAULT_SAND_FLOOR_SALT + step),
                    along / 920.0,
                    fault * 13.625
            );
            double sandSegment = smoothStep(
                    settings.sandyFloorThreshold(),
                    Math.min(
                            0.98,
                            settings.sandyFloorThreshold() + 0.22
                    ),
                    floorNoise
            );
            double sandFloorMask = faultMask
                    * smoothStep(0.58, 0.93, faultMask)
                    * sandSegment;

            strongestCarve = Math.max(strongestCarve, faultMask);
            strongestSandFloor = Math.max(
                    strongestSandFloor,
                    sandFloorMask
            );
        }

        return new FaultNetworkSample(
                clamp(strongestCarve, 0.0, 1.0),
                clamp(strongestSandFloor, 0.0, 1.0)
        );
    }

    private static double sandCorridorMask(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            ArrakisTerrainSettings.SandPassSettings settings
    ) {
        double radialGate = smoothStep(
                settings.startRadius(),
                settings.fullRadius(),
                radius
        ) * (1.0 - smoothStep(
                settings.fadeStartRadius(),
                settings.endRadius(),
                radius
        ));
        if (radialGate <= 0.0) {
            return 0.0;
        }

        double angle = Math.atan2(worldZ, worldX);
        double primaryAngle = seedPhase(worldSeed, SAND_PASS_A_SALT);
        double secondOffset = Math.PI
                + seedSignedUnit(worldSeed, SAND_PASS_B_SALT) * 0.55;
        double secondaryAngle = primaryAngle + secondOffset;

        double primaryCurve = 0.12 * Math.sin(
                radius / 750.0
                        + seedPhase(worldSeed, ANGLE_PHASE_A_SALT)
        );
        double secondaryCurve = 0.10 * Math.sin(
                radius / 910.0
                        + seedPhase(worldSeed, ANGLE_PHASE_B_SALT)
        );

        double primaryDistance = Math.abs(
                wrappedAngleDifference(
                        angle,
                        primaryAngle + primaryCurve
                )
        ) * Math.max(radius, 800.0);
        double secondaryDistance = Math.abs(
                wrappedAngleDifference(
                        angle,
                        secondaryAngle + secondaryCurve
                )
        ) * Math.max(radius, 800.0);

        double primary = 1.0 - smoothStep(
                settings.primaryCoreWidth(),
                settings.primaryOuterWidth(),
                primaryDistance
        );
        double secondary = 1.0 - smoothStep(
                settings.secondaryCoreWidth(),
                settings.secondaryOuterWidth(),
                secondaryDistance
        );

        return clamp(
                Math.max(primary, secondary) * radialGate,
                0.0,
                1.0
        );
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

    private static double smoothStep(
            double edge0,
            double edge1,
            double value
    ) {
        if (edge1 <= edge0) {
            return value < edge0 ? 0.0 : 1.0;
        }

        double normalized = clamp(
                (value - edge0) / (edge1 - edge0),
                0.0,
                1.0
        );
        return normalized * normalized * (3.0 - 2.0 * normalized);
    }

    private static double lerp(double start, double end, double amount) {
        return start + (end - start) * amount;
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
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

    private record FaultNetworkSample(
            double carveMask,
            double sandFloorMask
    ) {
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
            double faultSandFloorMask,
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
