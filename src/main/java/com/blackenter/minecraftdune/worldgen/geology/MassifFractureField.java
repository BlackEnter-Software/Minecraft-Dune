package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Absolute-coordinate massif fissures, independent of the regional fault network.
 *
 * <p>Primary traces are continuous warped lines that enter and leave a massif through its
 * exposed boundary. Deterministic finite branches grow from those traces and may terminate
 * inside the rock. This avoids the isolated mid-plateau fracture stars produced by the first
 * 0.5.13 cell-trunk model while retaining chunk-seamless local hazard detail.</p>
 */
public final class MassifFractureField {
    private static final int TRACE_FAMILIES = 2;
    private static final long FAMILY_ROTATION_SALT = 0x79F024AC13D856BEL;
    private static final long TRACE_SALT = 0x6A29D4F1C08B735EL;
    private static final long ACTIVE_SALT = 0x1DB7E53A902C64F8L;
    private static final long OFFSET_SALT = 0x37C8A20E5F14D96BL;
    private static final long WARP_SALT = 0x50E1B739CA6284DFL;
    private static final long SINE_PHASE_SALT = 0x4BC719E05A32D86FL;
    private static final long WIDTH_SALT = 0x68D30FA241B957CEL;
    private static final long DEPTH_SALT = 0x0F96C4B72DE153A8L;
    private static final long MINERAL_PRESENCE_SALT = 0x5EA2C7813FD9046BL;
    private static final long MINERAL_ABUNDANCE_SALT = 0x31D8B657CA04E29FL;
    private static final long MINERAL_DETAIL_SALT = 0x70A2F4195CB83D6EL;
    private static final long MINERAL_BAND_SALT = 0x26CF918A4DE735B0L;
    private static final long BRANCH_NODE_SALT = 0x73A1D8E42BC6059FL;
    private static final long BRANCH_CHANCE_SALT = 0x2CE7049B6F18D35AL;
    private static final long BRANCH_SIDE_SALT = 0x45B91C3E708AD62FL;
    private static final long BRANCH_LENGTH_SALT = 0x248DB1F763A95C0EL;
    private static final long BRANCH_ANGLE_SALT = 0x63E15A90B427DCF8L;
    private static final long BRANCH_BEND_SALT = 0x14B9C3076E5F28ADL;
    private static final long BRANCH_WIDTH_SALT = 0x58F2D6940AC37B1EL;
    private static final long BRANCH_DEPTH_SALT = 0x3CA85F1207D964BEL;
    private static final long BRANCH_MINERAL_SALT = 0x6F0D29B471AE538CL;
    private static final double TWO_PI = Math.PI * 2.0;

    public static final Sample NONE = new Sample(
            0.0,
            0.0,
            0.0,
            0.0,
            Double.POSITIVE_INFINITY,
            0.0,
            0.0,
            0.0,
            false,
            0.0,
            0.0,
            12.0,
            0.0
    );

    private MassifFractureField() {
    }

    public static Sample sample(
            long worldSeed,
            double worldX,
            double worldZ,
            double originalRockTopY,
            MacroGeologyField.Sample geology,
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings
    ) {
        if (!settings.enabled() || settings.density() <= 0.0) {
            return NONE;
        }

        double addedRockHeight = originalRockTopY - MacroGeologyField.BASE_SURFACE_Y;
        double heightGate = GeologyNoise.smoothStep(
                settings.minimumRockHeight(),
                settings.minimumRockHeight() + 22.0,
                addedRockHeight
        );
        double massifPermission = Math.max(
                geology.massifWeight(),
                Math.max(
                        geology.faultedMarginWeight() * 0.62,
                        geology.brokenRockWeight() * 0.24
                )
        );
        double massifGate = GeologyNoise.smoothStep(
                settings.minimumMassifWeight(),
                Math.min(1.0, settings.minimumMassifWeight() + 0.28),
                massifPermission
        );
        double activation = heightGate
                * massifGate
                * GeologyNoise.clamp(geology.rockFormationMask(), 0.0, 1.0);
        if (activation <= 0.0) {
            return NONE;
        }

        return network(worldSeed, worldX, worldZ, resistance, settings, activation);
    }

    /** Geological joint network: existence does not depend on a visible massif or Y64 root. */
    public static Sample structural(long seed, double x, double z, LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings) {
        return settings.enabled() && settings.density() > 0
                ? network(seed, x, z, resistance, settings, 1.0) : NONE;
    }

    private static Sample network(long worldSeed, double worldX, double worldZ,
            LithologyField.ResistanceClass resistance, ArrakisTerrainSettings.FractureSettings settings,
            double activation) {

        double traceSpacing = Math.max(96.0, settings.cellSize());
        double minimumBranchLength = Math.max(12.0, settings.minimumLength());
        double maximumBranchLength = Math.max(
                minimumBranchLength,
                settings.maximumLength()
        );
        double branchSpacing = traceSpacing * 0.76;
        int lineSearchRadius = Math.min(
                3,
                Math.max(1, (int) Math.ceil(maximumBranchLength / traceSpacing) + 1)
        );
        int nodeSearchRadius = Math.min(
                3,
                Math.max(1, (int) Math.ceil(maximumBranchLength / branchSpacing) + 1)
        );
        double globalRotation = GeologyNoise.unit(
                worldSeed,
                FAMILY_ROTATION_SALT
        ) * Math.PI;
        Accumulator accumulator = new Accumulator();

        for (int family = 0; family < TRACE_FAMILIES; family++) {
            long familyStep = (long) family * 0x9E3779B97F4A7C15L;
            double direction = globalRotation
                    + family * (Math.PI / TRACE_FAMILIES)
                    + GeologyNoise.signed(
                            worldSeed,
                            FAMILY_ROTATION_SALT + familyStep + 17L
                    ) * 0.14;
            double directionX = Math.cos(direction);
            double directionZ = Math.sin(direction);
            double along = worldX * directionX + worldZ * directionZ;
            double perpendicular = -worldX * directionZ + worldZ * directionX;
            long nearestLine = Math.round(perpendicular / traceSpacing);

            for (long lineIndex = nearestLine - lineSearchRadius;
                    lineIndex <= nearestLine + lineSearchRadius;
                    lineIndex++) {
                long lineSeed = GeologyNoise.cellSeed(
                        worldSeed ^ familyStep,
                        lineIndex,
                        family,
                        TRACE_SALT
                );
                if (GeologyNoise.unit(lineSeed, ACTIVE_SALT) >= settings.density()) {
                    continue;
                }

                double nominalOffset = lineIndex * traceSpacing
                        + GeologyNoise.signed(lineSeed, OFFSET_SALT) * traceSpacing * 0.16;
                double centerOffset = traceCenterOffset(
                        lineSeed,
                        along,
                        nominalOffset,
                        traceSpacing,
                        family
                );
                double distance = Math.abs(perpendicular - centerOffset);
                double baseWidth = GeologyNoise.lerp(
                        Math.max(1.0, settings.minimumWidth()),
                        Math.max(settings.minimumWidth(), settings.maximumWidth()),
                        Math.pow(GeologyNoise.unit(lineSeed, WIDTH_SALT), 1.35)
                );
                double baseDepth = GeologyNoise.lerp(
                        Math.max(1.0, settings.minimumDepth()),
                        Math.max(settings.minimumDepth(), settings.maximumDepth()),
                        Math.pow(GeologyNoise.unit(lineSeed, DEPTH_SALT), 1.10)
                );
                double mineralization = traceMineralization(
                        lineSeed,
                        along,
                        traceSpacing,
                        settings.mineralizationChance()
                );
                double mineralBandOffset = GeologyNoise.unit(
                        lineSeed,
                        MINERAL_BAND_SALT
                ) * 24.0;
                double tracePhase = along / traceSpacing * 2.15
                        + GeologyNoise.unit(lineSeed, SINE_PHASE_SALT) * TWO_PI;

                considerCandidate(
                        accumulator,
                        distance,
                        baseWidth,
                        baseDepth,
                        1.0,
                        tracePhase,
                        mineralization,
                        mineralBandOffset,
                        activation,
                        resistance,
                        settings
                );

                long nearestNode = Math.round(along / branchSpacing);
                for (long nodeIndex = nearestNode - nodeSearchRadius;
                        nodeIndex <= nearestNode + nodeSearchRadius;
                        nodeIndex++) {
                    addBranch(
                            accumulator,
                            worldX,
                            worldZ,
                            lineSeed,
                            nodeIndex,
                            family,
                            direction,
                            directionX,
                            directionZ,
                            nominalOffset,
                            traceSpacing,
                            branchSpacing,
                            minimumBranchLength,
                            maximumBranchLength,
                            baseWidth,
                            baseDepth,
                            mineralization,
                            mineralBandOffset,
                            activation,
                            resistance,
                            settings
                    );
                }
            }
        }

        return accumulator.toSample(activation, settings.calciteWallThickness());
    }

    private static double traceCenterOffset(
            long lineSeed,
            double along,
            double nominalOffset,
            double traceSpacing,
            int family
    ) {
        double warpScale = traceSpacing * GeologyNoise.lerp(
                2.8,
                4.4,
                GeologyNoise.unit(lineSeed, WARP_SALT + 11L)
        );
        double broadWarp = traceSpacing * 0.16 * GeologyNoise.value2(
                lineSeed ^ WARP_SALT,
                along / warpScale,
                family * 19.375
        );
        double sineScale = traceSpacing * GeologyNoise.lerp(
                1.15,
                1.85,
                GeologyNoise.unit(lineSeed, WARP_SALT + 29L)
        );
        double sineWarp = traceSpacing * 0.055 * Math.sin(
                along / sineScale
                        + GeologyNoise.unit(lineSeed, SINE_PHASE_SALT) * TWO_PI
        );
        return nominalOffset + broadWarp + sineWarp;
    }

    private static double traceMineralization(
            long lineSeed,
            double along,
            double traceSpacing,
            double mineralizationChance
    ) {
        if (GeologyNoise.unit(lineSeed, MINERAL_PRESENCE_SALT)
                >= mineralizationChance) {
            return 0.0;
        }

        double baseAbundance = GeologyNoise.lerp(
                0.18,
                1.0,
                Math.pow(
                        GeologyNoise.unit(lineSeed, MINERAL_ABUNDANCE_SALT),
                        0.72
                )
        );
        double alongDetail = 0.5 + 0.5 * GeologyNoise.value2(
                lineSeed ^ MINERAL_DETAIL_SALT,
                along / Math.max(48.0, traceSpacing * 0.42),
                0.375
        );
        double localPatch = GeologyNoise.smoothStep(0.18, 0.82, alongDetail);
        return baseAbundance * (0.15 + localPatch * 0.85);
    }

    private static void addBranch(
            Accumulator accumulator,
            double worldX,
            double worldZ,
            long lineSeed,
            long nodeIndex,
            int family,
            double traceDirection,
            double directionX,
            double directionZ,
            double nominalOffset,
            double traceSpacing,
            double branchSpacing,
            double minimumLength,
            double maximumLength,
            double primaryWidth,
            double primaryDepth,
            double primaryMineralization,
            double primaryMineralBandOffset,
            double activation,
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings
    ) {
        long branchSeed = GeologyNoise.cellSeed(
                lineSeed,
                nodeIndex,
                family,
                BRANCH_NODE_SALT
        );
        if (GeologyNoise.unit(branchSeed, BRANCH_CHANCE_SALT)
                >= settings.branchChance()) {
            return;
        }

        double startAlong = nodeIndex * branchSpacing
                + GeologyNoise.signed(branchSeed, OFFSET_SALT) * branchSpacing * 0.22;
        double startPerpendicular = traceCenterOffset(
                lineSeed,
                startAlong,
                nominalOffset,
                traceSpacing,
                family
        );
        double startX = startAlong * directionX - startPerpendicular * directionZ;
        double startZ = startAlong * directionZ + startPerpendicular * directionX;
        double side = GeologyNoise.unit(branchSeed, BRANCH_SIDE_SALT) < 0.5
                ? -1.0
                : 1.0;
        double branchDirection = traceDirection + side * GeologyNoise.lerp(
                0.55,
                1.12,
                GeologyNoise.unit(branchSeed, BRANCH_ANGLE_SALT)
        );
        double branchLength = GeologyNoise.lerp(
                minimumLength,
                maximumLength,
                GeologyNoise.unit(branchSeed, BRANCH_LENGTH_SALT)
        );
        double bend = GeologyNoise.signed(branchSeed, BRANCH_BEND_SALT) * 0.36;
        double middleX = startX + Math.cos(branchDirection) * branchLength * 0.56;
        double middleZ = startZ + Math.sin(branchDirection) * branchLength * 0.56;
        double endDirection = branchDirection + bend;
        double endX = middleX + Math.cos(endDirection) * branchLength * 0.44;
        double endZ = middleZ + Math.sin(endDirection) * branchLength * 0.44;
        double branchWidth = primaryWidth * GeologyNoise.lerp(
                0.46,
                0.76,
                GeologyNoise.unit(branchSeed, BRANCH_WIDTH_SALT)
        );
        double branchDepth = primaryDepth * GeologyNoise.lerp(
                0.52,
                0.84,
                GeologyNoise.unit(branchSeed, BRANCH_DEPTH_SALT)
        );
        double branchMineralization = primaryMineralization * GeologyNoise.lerp(
                0.28,
                0.78,
                GeologyNoise.unit(branchSeed, BRANCH_MINERAL_SALT)
        );
        if (GeologyNoise.unit(branchSeed, MINERAL_PRESENCE_SALT)
                < settings.mineralizationChance() * 0.30) {
            branchMineralization = Math.max(
                    branchMineralization,
                    GeologyNoise.lerp(
                            0.22,
                            0.82,
                            GeologyNoise.unit(branchSeed, MINERAL_ABUNDANCE_SALT)
                    )
            );
        }
        double mineralBandOffset = primaryMineralBandOffset
                + GeologyNoise.signed(branchSeed, MINERAL_BAND_SALT) * 4.0;

        considerSegment(
                accumulator,
                worldX,
                worldZ,
                startX,
                startZ,
                middleX,
                middleZ,
                branchWidth,
                branchDepth,
                1.0,
                2.4,
                branchMineralization,
                mineralBandOffset,
                activation,
                resistance,
                settings
        );
        considerSegment(
                accumulator,
                worldX,
                worldZ,
                middleX,
                middleZ,
                endX,
                endZ,
                branchWidth,
                branchDepth,
                0.82,
                4.1,
                branchMineralization * 0.82,
                mineralBandOffset,
                activation,
                resistance,
                settings
        );
    }

    private static void considerSegment(
            Accumulator accumulator,
            double pointX,
            double pointZ,
            double startX,
            double startZ,
            double endX,
            double endZ,
            double baseWidth,
            double baseDepth,
            double branchScale,
            double phase,
            double mineralization,
            double mineralBandOffset,
            double activation,
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings
    ) {
        double segmentX = endX - startX;
        double segmentZ = endZ - startZ;
        double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
        if (lengthSquared <= 0.0001) {
            return;
        }

        double projection = ((pointX - startX) * segmentX
                + (pointZ - startZ) * segmentZ) / lengthSquared;
        double along = GeologyNoise.clamp(projection, 0.0, 1.0);
        double nearestX = startX + segmentX * along;
        double nearestZ = startZ + segmentZ * along;
        double distance = Math.hypot(pointX - nearestX, pointZ - nearestZ);
        considerCandidate(
                accumulator,
                distance,
                baseWidth,
                baseDepth,
                branchScale,
                along * Math.PI * 3.0 + phase,
                mineralization,
                mineralBandOffset,
                activation,
                resistance,
                settings
        );
    }

    private static void considerCandidate(
            Accumulator accumulator,
            double distance,
            double baseWidth,
            double baseDepth,
            double scale,
            double phase,
            double mineralizationAbundance,
            double mineralBandOffset,
            double activation,
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings
    ) {
        double nonuniform = 0.76
                + 0.18 * Math.sin(phase)
                + 0.06 * Math.sin(phase * 0.37 + 1.4);
        double widthResistance = GeologyNoise.lerp(
                1.0,
                resistance.fractureFactor(),
                GeologyNoise.clamp(settings.resistanceWidthInfluence(), 0.0, 1.0)
        );
        double depthResistance = GeologyNoise.lerp(
                1.0,
                resistance.fractureFactor(),
                GeologyNoise.clamp(settings.resistanceDepthInfluence(), 0.0, 1.0)
        );
        double width = GeologyNoise.clamp(
                baseWidth * scale * nonuniform * widthResistance,
                Math.max(1.0, settings.minimumWidth()),
                Math.max(settings.minimumWidth(), settings.maximumWidth())
        );
        double halfWidth = width * 0.5;
        double depth = baseDepth * (0.80 + 0.20 * nonuniform) * depthResistance;
        double strength = 1.0 - GeologyNoise.smoothStep(
                halfWidth * 0.58,
                halfWidth,
                distance
        );
        double wallThickness = Math.max(0.25, settings.calciteWallThickness());
        double mineralHalo = 1.0 - GeologyNoise.smoothStep(
                halfWidth + wallThickness,
                halfWidth + wallThickness * 2.0,
                distance
        );
        double mineralization = mineralizationAbundance * mineralHalo * activation;
        double mineralBandSpacing = GeologyNoise.lerp(
                15.0,
                6.5,
                GeologyNoise.clamp(mineralizationAbundance, 0.0, 1.0)
        );
        accumulator.consider(
                strength * activation,
                depth,
                width,
                halfWidth,
                distance,
                mineralization,
                mineralizationAbundance > 0.0,
                wallThickness,
                mineralBandSpacing,
                mineralBandOffset
        );
    }

    public record Sample(
            double strength,
            double carveDepth,
            double designDepth,
            double width,
            double distance,
            double halfWidth,
            double mineralization,
            double intersectionStrength,
            boolean mineralized,
            double activation,
            double calciteWallThickness,
            double mineralBandSpacing,
            double mineralBandOffset
    ) {
        public boolean calciteExposure(int y, int originalTopY, int carvedTopY) {
            if (!mineralized || mineralization < 0.34) {
                return false;
            }

            int lowestWallY = originalTopY - Math.max(2, (int) Math.ceil(designDepth));
            boolean wallShell = distance >= halfWidth * 0.68
                    && distance <= halfWidth + calciteWallThickness
                    && y >= Math.min(carvedTopY, lowestWallY)
                    && y <= originalTopY;
            if (!wallShell) {
                return false;
            }

            double bandDistance = GeologyNoise.foldedDistance(
                    y + mineralBandOffset,
                    mineralBandSpacing
            );
            double bandThickness = 0.45
                    + GeologyNoise.clamp(mineralization, 0.0, 1.0) * 1.35;
            return bandDistance <= bandThickness;
        }

        /** Fracture-outlet contribution consumed by the 0.5.14 scree/talus pass. */
        public boolean talusCandidate(ArrakisTerrainSettings.TalusSettings settings) {
            return settings.localScreeEnabled()
                    && strength >= settings.minimumFractureStrength();
        }
    }

    private static final class Accumulator {
        private double strength;
        private double designDepth;
        private double width;
        private double halfWidth;
        private double distance = Double.POSITIVE_INFINITY;
        private double mineralization;
        private double strongestCarve;
        private double secondCarve;
        private boolean mineralized;
        private double wallThickness;
        private double mineralBandSpacing;
        private double mineralBandOffset;
        private double bestScore;

        void consider(
                double candidateStrength,
                double candidateDepth,
                double candidateWidth,
                double candidateHalfWidth,
                double candidateDistance,
                double candidateMineralization,
                boolean candidateMineralized,
                double candidateWallThickness,
                double candidateMineralBandSpacing,
                double candidateMineralBandOffset
        ) {
            if (candidateStrength > strongestCarve) {
                secondCarve = strongestCarve;
                strongestCarve = candidateStrength;
            } else if (candidateStrength > secondCarve) {
                secondCarve = candidateStrength;
            }

            // Any actual carve outranks a nearby mineralized halo. This preserves the
            // strongest fissure at intersections while still allowing calcite wall metadata
            // to exist just outside a fissure where carve strength is zero.
            double score = candidateStrength > 0.0
                    ? 2.0 + candidateStrength
                    : candidateMineralization;
            if (score <= bestScore) {
                return;
            }
            bestScore = score;
            strength = candidateStrength;
            designDepth = candidateDepth;
            width = candidateWidth;
            halfWidth = candidateHalfWidth;
            distance = candidateDistance;
            mineralization = candidateMineralization;
            mineralized = candidateMineralized;
            wallThickness = candidateWallThickness;
            mineralBandSpacing = candidateMineralBandSpacing;
            mineralBandOffset = candidateMineralBandOffset;
        }

        Sample toSample(double activation, double configuredWallThickness) {
            if (bestScore <= 0.0) {
                return NONE;
            }
            double intersection = Math.min(strongestCarve, secondCarve);
            double combinedStrength = GeologyNoise.clamp(
                    strength + intersection * 0.20,
                    0.0,
                    1.0
            );
            double intersectionDepth = 1.0 + intersection * 0.20;
            return new Sample(
                    combinedStrength,
                    designDepth * combinedStrength * intersectionDepth,
                    designDepth,
                    width,
                    distance,
                    halfWidth,
                    mineralization,
                    intersection,
                    mineralized,
                    activation,
                    wallThickness > 0.0 ? wallThickness : configuredWallThickness,
                    mineralBandSpacing > 0.0 ? mineralBandSpacing : 12.0,
                    mineralBandOffset
            );
        }
    }
}
