package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Absolute-coordinate massif fissures, independent of the regional fault network.
 *
 * <p>Each deterministic spatial cell may contribute a bent trunk and one or two tapered
 * branches. Sampling distances to those finite segments produces long connected cracks,
 * slots and chasms without chunk-local iteration or post-generation block edits.</p>
 */
public final class MassifFractureField {
    private static final long CELL_SALT = 0x6A29D4F1C08B735EL;
    private static final long ACTIVE_SALT = 0x1DB7E53A902C64F8L;
    private static final long ORIGIN_X_SALT = 0x37C8A20E5F14D96BL;
    private static final long ORIGIN_Z_SALT = 0x50E1B739CA6284DFL;
    private static final long DIRECTION_SALT = 0x79F024AC13D856BEL;
    private static final long LENGTH_SALT = 0x248DB1F763A95C0EL;
    private static final long BEND_SALT = 0x4BC719E05A32D86FL;
    private static final long WIDTH_SALT = 0x68D30FA241B957CEL;
    private static final long DEPTH_SALT = 0x0F96C4B72DE153A8L;
    private static final long BRANCH_SALT = 0x73A1D8E42BC6059FL;
    private static final long BRANCH_SIDE_SALT = 0x2CE7049B6F18D35AL;
    private static final long SECOND_BRANCH_SALT = 0x45B91C3E708AD62FL;
    private static final long MINERAL_SALT = 0x5EA2C7813FD9046BL;
    private static final double TWO_PI = Math.PI * 2.0;
    public static final Sample NONE = new Sample(
            0.0,
            0.0,
            0.0,
            0.0,
            Double.POSITIVE_INFINITY,
            0.0,
            0.0,
            false,
            0.0,
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

        double cellSize = Math.max(64.0, settings.cellSize());
        long centerCellX = (long) Math.floor(worldX / cellSize);
        long centerCellZ = (long) Math.floor(worldZ / cellSize);
        double maximumLength = Math.max(settings.minimumLength(), settings.maximumLength());
        int searchRadius = Math.min(3, Math.max(1, (int) Math.ceil(maximumLength / cellSize) + 1));
        Accumulator accumulator = new Accumulator();

        for (long cellZ = centerCellZ - searchRadius;
                cellZ <= centerCellZ + searchRadius;
                cellZ++) {
            for (long cellX = centerCellX - searchRadius;
                    cellX <= centerCellX + searchRadius;
                    cellX++) {
                long cellSeed = GeologyNoise.cellSeed(
                        worldSeed,
                        cellX,
                        cellZ,
                        CELL_SALT
                );
                if (GeologyNoise.unit(cellSeed, ACTIVE_SALT) >= settings.density()) {
                    continue;
                }

                double originX = (cellX + 0.12
                        + GeologyNoise.unit(cellSeed, ORIGIN_X_SALT) * 0.76) * cellSize;
                double originZ = (cellZ + 0.12
                        + GeologyNoise.unit(cellSeed, ORIGIN_Z_SALT) * 0.76) * cellSize;
                double direction = GeologyNoise.unit(cellSeed, DIRECTION_SALT) * TWO_PI;
                double length = GeologyNoise.lerp(
                        Math.max(12.0, settings.minimumLength()),
                        maximumLength,
                        GeologyNoise.unit(cellSeed, LENGTH_SALT)
                );
                double bend = GeologyNoise.signed(cellSeed, BEND_SALT) * 0.52;
                double baseWidth = GeologyNoise.lerp(
                        Math.max(1.0, settings.minimumWidth()),
                        Math.max(settings.minimumWidth(), settings.maximumWidth()),
                        Math.pow(GeologyNoise.unit(cellSeed, WIDTH_SALT), 1.35)
                );
                double baseDepth = GeologyNoise.lerp(
                        Math.max(1.0, settings.minimumDepth()),
                        Math.max(settings.minimumDepth(), settings.maximumDepth()),
                        Math.pow(GeologyNoise.unit(cellSeed, DEPTH_SALT), 1.10)
                );
                boolean mineralized = GeologyNoise.unit(cellSeed, MINERAL_SALT)
                        < settings.mineralizationChance();

                double middleX = originX + Math.cos(direction) * length * 0.46;
                double middleZ = originZ + Math.sin(direction) * length * 0.46;
                double endDirection = direction + bend;
                double endX = middleX + Math.cos(endDirection) * length * 0.54;
                double endZ = middleZ + Math.sin(endDirection) * length * 0.54;

                considerSegment(
                        accumulator,
                        worldX,
                        worldZ,
                        originX,
                        originZ,
                        middleX,
                        middleZ,
                        baseWidth,
                        baseDepth,
                        1.0,
                        0.0,
                        mineralized,
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
                        baseWidth,
                        baseDepth,
                        0.92,
                        1.7,
                        mineralized,
                        activation,
                        resistance,
                        settings
                );

                if (GeologyNoise.unit(cellSeed, BRANCH_SALT) < settings.branchChance()) {
                    double side = GeologyNoise.unit(cellSeed, BRANCH_SIDE_SALT) < 0.5
                            ? -1.0
                            : 1.0;
                    addBranch(
                            accumulator,
                            worldX,
                            worldZ,
                            middleX,
                            middleZ,
                            endDirection,
                            side,
                            length,
                            baseWidth,
                            baseDepth,
                            mineralized,
                            cellSeed,
                            activation,
                            resistance,
                            settings,
                            0
                    );

                    if (GeologyNoise.unit(cellSeed, SECOND_BRANCH_SALT)
                            < settings.branchChance() * 0.42) {
                        addBranch(
                                accumulator,
                                worldX,
                                worldZ,
                                middleX,
                                middleZ,
                                endDirection,
                                -side,
                                length,
                                baseWidth,
                                baseDepth,
                                mineralized,
                                cellSeed,
                                activation,
                                resistance,
                                settings,
                                1
                        );
                    }
                }
            }
        }

        return accumulator.toSample(activation, settings.calciteWallThickness());
    }

    private static void addBranch(
            Accumulator accumulator,
            double worldX,
            double worldZ,
            double startX,
            double startZ,
            double trunkDirection,
            double side,
            double trunkLength,
            double baseWidth,
            double baseDepth,
            boolean mineralized,
            long cellSeed,
            double activation,
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.FractureSettings settings,
            int branchIndex
    ) {
        long step = (long) branchIndex * 0x9E3779B97F4A7C15L;
        double angle = trunkDirection + side * GeologyNoise.lerp(
                0.58,
                1.18,
                GeologyNoise.unit(cellSeed, BRANCH_SIDE_SALT + step)
        );
        double length = trunkLength * GeologyNoise.lerp(
                0.28,
                0.55,
                GeologyNoise.unit(cellSeed, LENGTH_SALT + step + 31L)
        );
        double endX = startX + Math.cos(angle) * length;
        double endZ = startZ + Math.sin(angle) * length;
        considerSegment(
                accumulator,
                worldX,
                worldZ,
                startX,
                startZ,
                endX,
                endZ,
                baseWidth,
                baseDepth,
                0.62,
                3.1 + branchIndex,
                mineralized,
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
            boolean mineralized,
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
        double nonuniform = 0.76 + 0.24 * Math.sin(along * Math.PI * 3.0 + phase);
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
                baseWidth * branchScale * nonuniform * widthResistance,
                Math.max(1.0, settings.minimumWidth()),
                Math.max(settings.minimumWidth(), settings.maximumWidth())
        );
        double halfWidth = width * 0.5;
        double depth = baseDepth * (0.78 + 0.22 * nonuniform) * depthResistance;
        double strength = 1.0 - GeologyNoise.smoothStep(
                halfWidth * 0.58,
                halfWidth,
                distance
        );
        double wallThickness = Math.max(0.25, settings.calciteWallThickness());
        double mineralization = mineralized
                ? 1.0 - GeologyNoise.smoothStep(
                        halfWidth + wallThickness,
                        halfWidth + wallThickness * 2.0,
                        distance
                )
                : 0.0;
        accumulator.consider(
                strength * activation,
                depth,
                width,
                halfWidth,
                distance,
                mineralization * activation,
                mineralized,
                wallThickness
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
            boolean mineralized,
            double activation,
            double calciteWallThickness
    ) {
        public boolean calciteExposure(int y, int originalTopY, int carvedTopY) {
            if (!mineralized || mineralization < 0.30) {
                return false;
            }
            boolean floorPocket = y >= carvedTopY - 1 && y <= carvedTopY;
            int lowestWallY = originalTopY - Math.max(2, (int) Math.ceil(designDepth));
            boolean wallShell = distance >= halfWidth * 0.70
                    && distance <= halfWidth + calciteWallThickness
                    && y >= lowestWallY
                    && y <= originalTopY;
            return floorPocket || wallShell;
        }

        /** Hook for the deliberately deferred 0.5.14 scree/talus placement pass. */
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
        private boolean mineralized;
        private double wallThickness;
        private double bestScore;

        void consider(
                double candidateStrength,
                double candidateDepth,
                double candidateWidth,
                double candidateHalfWidth,
                double candidateDistance,
                double candidateMineralization,
                boolean candidateMineralized,
                double candidateWallThickness
        ) {
            // Any actual carve outranks a nearby mineralized halo. This preserves the
            // deepest fissure at intersections while still allowing calcite wall metadata
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
        }

        Sample toSample(double activation, double configuredWallThickness) {
            if (bestScore <= 0.0) {
                return NONE;
            }
            return new Sample(
                    strength,
                    designDepth * strength,
                    designDepth,
                    width,
                    distance,
                    halfWidth,
                    mineralization,
                    mineralized,
                    activation,
                    wallThickness > 0.0 ? wallThickness : configuredWallThickness
            );
        }
    }
}
