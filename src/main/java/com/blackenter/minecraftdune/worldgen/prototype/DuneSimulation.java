package com.blackenter.minecraftdune.worldgen.prototype;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Deterministic, reduced-resolution dune simulation for the Arrakis Dev world.
 *
 * <p>This is deliberately a development prototype rather than the final Gameplay
 * Arrakis chunk generator. It models a mobile sand-thickness field, directional
 * saltation-like transport, a reduced wind-shadow effect, and block-scale slope
 * stabilization. The resulting grid is upscaled to Minecraft block columns.</p>
 */
public final class DuneSimulation {
    public static final int GRID_SIZE = 64;
    public static final int BASE_SURFACE_Y = 64;

    private static final double TWO_PI = Math.PI * 2.0;
    private static final double CASCADE_RELAXATION = 0.25;

    private DuneSimulation() {
    }

    public static Result simulate(
            DuneMode mode,
            long seed,
            Settings settings,
            DuneSurfaceResolution surfaceResolution
    ) {
        Wind wind = Wind.fromAngle(settings.windAngleDegrees());
        double[] sand = createInitialSandField(mode, seed, settings, wind);
        double initialMass = sum(sand);

        int transportIterations = settings.effectiveTransportIterations(mode);
        for (int iteration = 0; iteration < transportIterations; iteration++) {
            sand = transportSand(sand, mode, seed, iteration, settings, wind);
        }

        double finalMass = sum(sand);

        // Convert the transported sand field into Minecraft-scale vertical heights first.
        // Cascading is intentionally performed after this conversion so repose_angle is
        // expressed in real block-space and its visual result is not normalized away later.
        double[] physicalHeights = convertToPhysicalHeights(sand, mode, settings);
        if (mode == DuneMode.TRANSVERSE) {
            physicalHeights = attenuateLowSandNoise(physicalHeights, settings);
            physicalHeights = applyInterduneCleanup(physicalHeights, settings);
        }
        for (int pass = 0; pass < settings.cascadePasses(); pass++) {
            physicalHeights = stabilizePhysicalSlopes(physicalHeights, settings);
        }

        int[] surfaceUnits = upscaleToSurfaceUnits(physicalHeights, settings, surfaceResolution);
        double maximumHeight = Arrays.stream(surfaceUnits).max().orElse(0)
                / (double) surfaceResolution.subdivisions();
        return new Result(
                surfaceUnits,
                initialMass,
                finalMass,
                maximumHeight,
                mode,
                seed,
                settings,
                surfaceResolution
        );
    }

    private static double[] createInitialSandField(
            DuneMode mode,
            long seed,
            Settings settings,
            Wind wind
    ) {
        return switch (mode) {
            case TRANSVERSE -> createTransverseField(seed, settings, wind);
            case BARCHAN -> createBarchanField(seed, wind);
        };
    }

    private static double[] createTransverseField(long seed, Settings settings, Wind wind) {
        double[] sand = new double[GRID_SIZE * GRID_SIZE];
        double spacing = settings.duneSpacingBlocks();
        double cellSize = settings.cellSize();

        // Seed-derived phase offsets preserve deterministic regions without imposing a
        // perfectly repeating ridge pattern from one world/region to another.
        double phaseA = unitHash(seed ^ 0x243F6A8885A308D3L, 0, 0, -7) * TWO_PI;
        double phaseB = unitHash(seed ^ 0x13198A2E03707344L, 0, 0, -11) * TWO_PI;
        double phaseC = unitHash(seed ^ 0xA4093822299F31D0L, 0, 0, -13) * TWO_PI;

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                double blockX = (x + 0.5) * cellSize;
                double blockZ = (z + 0.5) * cellSize;
                double alongWind = blockX * wind.x() + blockZ * wind.z();
                double acrossWind = blockX * wind.crosswindX() + blockZ * wind.crosswindZ();

                double phaseWarp = settings.spacingVariation() * TWO_PI * (
                        0.60 * Math.sin(acrossWind * TWO_PI / (spacing * 2.8) + phaseA)
                                + 0.28 * Math.sin(acrossWind * TWO_PI / (spacing * 5.3) + phaseB)
                                + 0.12 * Math.sin(alongWind * TWO_PI / (spacing * 4.7) + phaseC)
                );
                double phase = alongWind * TWO_PI / spacing + phaseWarp;
                double ridgeBase = transverseRidgeBase(phase, settings.slopeAsymmetry());
                double ridge = Math.pow(ridgeBase, settings.ridgeSharpness());

                // Keep only a very small seeded perturbation. Large low-amplitude noise
                // becomes visible as contour islands once mapped to whole Minecraft blocks.
                double variation = (unitHash(seed, x, z, -1) - 0.5) * 0.08;
                sand[index(x, z)] = 2.2 + 4.4 * ridge + variation;
            }
        }

        return sand;
    }

    private static double transverseRidgeBase(double phase, double slopeAsymmetry) {
        double symmetric = (Math.sin(phase) + 1.0) * 0.5;
        if (slopeAsymmetry <= 0.0) {
            return symmetric;
        }

        // Shift the cycle so 0 is an interdune trough and 0.5 is the old symmetric crest.
        // Moving the crest downwind gives a long stoss/windward ramp and a shorter lee face.
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
    private static double[] createBarchanField(long seed, Wind wind) {
        // 0.5.2 deliberately leaves the experimental barchan initializer unchanged while
        // transverse morphology is tuned first. Its sparse/additive behavior is documented
        // as a known limitation and will be revisited separately.
        double[] sand = new double[GRID_SIZE * GRID_SIZE];
        SplittableRandom random = new SplittableRandom(seed);
        int duneCount = 5 + random.nextInt(3);
        for (int dune = 0; dune < duneCount; dune++) {
            double centerX = random.nextDouble(GRID_SIZE);
            double centerZ = random.nextDouble(GRID_SIZE);
            double length = random.nextDouble(8.0, 13.0);
            double width = random.nextDouble(5.0, 9.0);
            double amplitude = random.nextDouble(5.0, 9.0);
            for (int z = 0; z < GRID_SIZE; z++) {
                for (int x = 0; x < GRID_SIZE; x++) {
                    double deltaX = toricDelta(x, centerX);
                    double deltaZ = toricDelta(z, centerZ);
                    double alongWind = deltaX * wind.x() + deltaZ * wind.z();
                    double acrossWind = deltaX * wind.crosswindX() + deltaZ * wind.crosswindZ();
                    double body = amplitude * gaussian(
                            alongWind + length * 0.10,
                            acrossWind,
                            length * 0.55,
                            width
                    );
                    double leeNotch = amplitude * 0.62 * gaussian(
                            alongWind - length * 0.25,
                            acrossWind,
                            length * 0.48,
                            width * 0.35
                    );
                    double horns = amplitude * 0.26 * gaussian(
                            alongWind - length * 0.55,
                            Math.abs(acrossWind) - width * 0.78,
                            length * 0.70,
                            width * 0.22
                    );
                    int index = index(x, z);
                    sand[index] += Math.max(0.0, body - leeNotch + horns);
                }
            }
        }

        for (int i = 0; i < sand.length; i++) {
            if (sand[i] < 0.12) {
                sand[i] = 0.0;
            }
        }

        return sand;
    }

    private static double[] transportSand(
            double[] current,
            DuneMode mode,
            long seed,
            int iteration,
            Settings settings,
            Wind wind
    ) {
        double[] delta = new double[current.length];
        int downwindX = nearestDirection(wind.x());
        int downwindZ = nearestDirection(wind.z());
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int sourceIndex = index(x, z);
                double available = current[sourceIndex];
                if (available < 0.04) {
                    continue;
                }

                int upwindIndex = indexWrapped(x - downwindX, z - downwindZ);
                int downwindIndex = indexWrapped(x + downwindX, z + downwindZ);
                double downwindSlope = available - current[downwindIndex];
                double windShadow = Math.max(0.0, current[upwindIndex] - available - 0.80);
                double randomA = unitHash(seed, x, z, iteration);
                double randomB = unitHash(seed ^ 0xD1B54A32D192ED03L, x, z, iteration);
                int hopDistance = 2 + (int) Math.floor(randomA * 4.0);
                double crosswindJitter = (randomB - 0.5) * 1.4;

                // Low-relief transverse sand should not accumulate as much stochastic
                // transport texture as the actual dune body. Keep the effect deliberately
                // mild here; the support-aware cleanup below handles isolated remnants.
                double lowReliefSignal = 1.0;
                if (mode == DuneMode.TRANSVERSE) {
                    double localRelief = Math.max(
                            Math.abs(available - current[upwindIndex]),
                            Math.abs(available - current[downwindIndex])
                    );
                    lowReliefSignal = smoothStep(0.04, 0.45, localRelief);
                    crosswindJitter *= 0.75 + 0.25 * lowReliefSignal;
                }

                int targetX = wrap((int) Math.round(
                        x + wind.x() * hopDistance + wind.crosswindX() * crosswindJitter
                ));
                int targetZ = wrap((int) Math.round(
                        z + wind.z() * hopDistance + wind.crosswindZ() * crosswindJitter
                ));
                double lifted = Math.min(
                        available * 0.045,
                        0.045 + Math.max(0.0, downwindSlope) * 0.018
                );
                lifted *= 0.65 + randomA * 0.70;
                lifted /= 1.0 + windShadow * 2.5;

                if (mode == DuneMode.BARCHAN) {
                    lifted *= 1.12;
                } else {
                    lifted *= 0.75 + 0.25 * lowReliefSignal;
                }

                lifted *= settings.transportStrength();
                lifted = Math.min(lifted, available);
                delta[sourceIndex] -= lifted;
                delta[index(targetX, targetZ)] += lifted;
            }
        }

        double[] next = new double[current.length];
        for (int i = 0; i < current.length; i++) {
            next[i] = Math.max(0.0, current[i] + delta[i]);
        }
        return next;
    }

    private static double[] convertToPhysicalHeights(
            double[] sand,
            DuneMode mode,
            Settings settings
    ) {
        double[] sorted = sand.clone();
        Arrays.sort(sorted);
        double lowerPercentile = mode == DuneMode.TRANSVERSE ? 0.10 : 0.24;
        double lower = percentile(sorted, lowerPercentile);
        double upper = percentile(sorted, 0.97);
        double range = Math.max(0.0001, upper - lower);
        int maximumHeight = settings.effectiveMaximumHeight(mode);

        double[] heights = new double[sand.length];
        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int cellIndex = index(x, z);
                double normalized = clamp((sand[cellIndex] - lower) / range, 0.0, 1.0);

                if (mode == DuneMode.TRANSVERSE) {
                    double cutoff = settings.valleyCutoff();
                    normalized = normalized <= cutoff
                            ? 0.0
                            : (normalized - cutoff) / (1.0 - cutoff);
                } else {
                    normalized = Math.pow(normalized, 1.05);
                }

                heights[cellIndex] = normalized * maximumHeight;
            }
        }

        return heights;
    }

    private static double[] attenuateLowSandNoise(double[] current, Settings settings) {
        double[] next = current.clone();
        double maximumHeight = settings.effectiveMaximumHeight(DuneMode.TRANSVERSE);

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int cellIndex = index(x, z);
                double height = current[cellIndex];
                if (height <= 0.0) {
                    continue;
                }

                double peak = localPeak(current, x, z, 2);
                double relativeHeight = height / maximumHeight;
                double relativeSupport = peak / maximumHeight;
                double lowness = 1.0 - smoothStep(0.08, 0.28, relativeHeight);
                double isolation = 1.0 - smoothStep(0.18, 0.42, relativeSupport);
                if (lowness <= 0.0 || isolation <= 0.0) {
                    continue;
                }

                double localMean = localAverage(current, x, z, 1);
                if (height > localMean) {
                    double attenuation = 0.45 * lowness * isolation;
                    next[cellIndex] = lerp(height, localMean, attenuation);
                }
            }
        }

        return next;
    }

    private static double[] applyInterduneCleanup(double[] current, Settings settings) {
        double strength = settings.interduneCleanup();
        if (strength <= 0.0) {
            return current;
        }

        double[] next = current.clone();
        double maximumHeight = settings.effectiveMaximumHeight(DuneMode.TRANSVERSE);

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int cellIndex = index(x, z);
                double height = current[cellIndex];
                if (height <= 0.0) {
                    continue;
                }

                // A low toe is retained when a substantial dune body exists within three
                // coarse cells. An equally low isolated patch in an open basin is reduced.
                double peak = localPeak(current, x, z, 3);
                double relativeHeight = height / maximumHeight;
                double relativeSupport = peak / maximumHeight;
                double lowness = 1.0 - smoothStep(0.04, 0.24, relativeHeight);
                double isolation = 1.0 - smoothStep(0.20, 0.48, relativeSupport);

                double removal = strength
                        * maximumHeight
                        * 0.20
                        * lowness
                        * isolation;
                next[cellIndex] = Math.max(0.0, height - removal);
            }
        }

        return next;
    }

    private static double localPeak(double[] values, int centerX, int centerZ, int radius) {
        double peak = 0.0;
        for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                int x = centerX + offsetX;
                int z = centerZ + offsetZ;
                if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
                    continue;
                }
                peak = Math.max(peak, values[index(x, z)]);
            }
        }
        return peak;
    }

    private static double localAverage(double[] values, int centerX, int centerZ, int radius) {
        double sum = 0.0;
        int count = 0;
        for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                int x = centerX + offsetX;
                int z = centerZ + offsetZ;
                if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
                    continue;
                }
                sum += values[index(x, z)];
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }
    private static double[] stabilizePhysicalSlopes(double[] current, Settings settings) {
        double[] delta = new double[current.length];
        double tangentOfRepose = Math.tan(Math.toRadians(settings.reposeAngleDegrees()));
        double cellSize = settings.cellSize();

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int sourceIndex = index(x, z);
                double sourceHeight = current[sourceIndex];
                if (sourceHeight <= 0.0) {
                    continue;
                }

                int targetIndex = -1;
                double greatestExcess = 0.0;
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    for (int offsetX = -1; offsetX <= 1; offsetX++) {
                        if (offsetX == 0 && offsetZ == 0) {
                            continue;
                        }

                        int candidateX = x + offsetX;
                        int candidateZ = z + offsetZ;
                        if (candidateX < 0 || candidateX >= GRID_SIZE
                                || candidateZ < 0 || candidateZ >= GRID_SIZE) {
                            continue;
                        }

                        int candidateIndex = index(candidateX, candidateZ);
                        double horizontalDistance = cellSize * Math.hypot(offsetX, offsetZ);
                        double allowedDifference = tangentOfRepose * horizontalDistance;
                        double excess = sourceHeight - current[candidateIndex] - allowedDifference;
                        if (excess > greatestExcess) {
                            greatestExcess = excess;
                            targetIndex = candidateIndex;
                        }
                    }
                }

                if (targetIndex < 0) {
                    continue;
                }

                double moved = Math.min(sourceHeight * 0.25, greatestExcess * CASCADE_RELAXATION);
                delta[sourceIndex] -= moved;
                delta[targetIndex] += moved;
            }
        }

        double[] next = new double[current.length];
        for (int i = 0; i < current.length; i++) {
            next[i] = Math.max(0.0, current[i] + delta[i]);
        }
        return next;
    }

    private static int[] upscaleToSurfaceUnits(
            double[] physicalHeights,
            Settings settings,
            DuneSurfaceResolution surfaceResolution
    ) {
        int regionBlockSize = settings.regionBlockSize();
        int cellSize = settings.cellSize();
        int maximumHeight = settings.maximumConfiguredHeightCeiling();
        int[] heights = new int[regionBlockSize * regionBlockSize];

        for (int blockZ = 0; blockZ < regionBlockSize; blockZ++) {
            for (int blockX = 0; blockX < regionBlockSize; blockX++) {
                double gridX = blockX / (double) cellSize;
                double gridZ = blockZ / (double) cellSize;
                int x0 = Math.min((int) Math.floor(gridX), GRID_SIZE - 1);
                int z0 = Math.min((int) Math.floor(gridZ), GRID_SIZE - 1);
                int x1 = Math.min(x0 + 1, GRID_SIZE - 1);
                int z1 = Math.min(z0 + 1, GRID_SIZE - 1);
                double fractionX = gridX - x0;
                double fractionZ = gridZ - z0;
                double top = lerp(
                        lerp(physicalHeights[index(x0, z0)], physicalHeights[index(x1, z0)], fractionX),
                        lerp(physicalHeights[index(x0, z1)], physicalHeights[index(x1, z1)], fractionX),
                        fractionZ
                );

                if (settings.edgeBlendCells() > 0) {
                    double edgeDistanceCells = Math.min(
                            Math.min(gridX, gridZ),
                            Math.min((GRID_SIZE - 1) - gridX, (GRID_SIZE - 1) - gridZ)
                    );
                    top *= smoothStep(0.0, settings.edgeBlendCells(), edgeDistanceCells);
                }

                heights[blockZ * regionBlockSize + blockX] = surfaceResolution.quantize(
                        clamp(top, 0.0, maximumHeight)
                );
            }
        }

        return heights;
    }

    private static double gaussian(
            double x,
            double z,
            double standardDeviationX,
            double standardDeviationZ
    ) {
        double exponent =
                x * x / (2.0 * standardDeviationX * standardDeviationX)
                        + z * z / (2.0 * standardDeviationZ * standardDeviationZ);
        return Math.exp(-exponent);
    }

    private static double toricDelta(double coordinate, double center) {
        double delta = coordinate - center;
        if (delta > GRID_SIZE * 0.5) {
            delta -= GRID_SIZE;
        } else if (delta < -GRID_SIZE * 0.5) {
            delta += GRID_SIZE;
        }
        return delta;
    }

    private static int nearestDirection(double component) {
        if (component > 0.25) {
            return 1;
        }
        if (component < -0.25) {
            return -1;
        }
        return 0;
    }

    private static int indexWrapped(int x, int z) {
        return index(wrap(x), wrap(z));
    }

    private static int index(int x, int z) {
        return z * GRID_SIZE + x;
    }

    private static int wrap(int coordinate) {
        return Math.floorMod(coordinate, GRID_SIZE);
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = (int) Math.round((sorted.length - 1) * percentile);
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    private static double sum(double[] values) {
        double result = 0.0;
        for (double value : values) {
            result += value;
        }
        return result;
    }

    private static double unitHash(long seed, int x, int z, int iteration) {
        long value = seed;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) iteration * 0x165667B19E3779F9L;
        value = mix64(value);
        return (value >>> 11) * 0x1.0p-53;
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

    public record Settings(
            int cellSize,
            int maximumHeightOverride,
            double duneSpacingBlocks,
            double spacingVariation,
            double ridgeSharpness,
            double valleyCutoff,
            double slopeAsymmetry,
            double interduneCleanup,
            double reposeAngleDegrees,
            int cascadePasses,
            int transportIterationsOverride,
            double windAngleDegrees,
            int edgeBlendCells,
            double transportStrength
    ) {
        public static final int MINIMUM_CELL_SIZE = 1;
        public static final int MAXIMUM_CELL_SIZE = 8;
        public static final int MAXIMUM_ALLOWED_HEIGHT = 32;
        public static final double MINIMUM_DUNE_SPACING = 32.0;
        public static final double MAXIMUM_DUNE_SPACING = 512.0;
        public static final double MINIMUM_SPACING_VARIATION = 0.0;
        public static final double MAXIMUM_SPACING_VARIATION = 0.50;
        public static final double MINIMUM_RIDGE_SHARPNESS = 1.0;
        public static final double MAXIMUM_RIDGE_SHARPNESS = 8.0;
        public static final double MINIMUM_VALLEY_CUTOFF = 0.0;
        public static final double MAXIMUM_VALLEY_CUTOFF = 0.80;
        public static final double MINIMUM_SLOPE_ASYMMETRY = 0.0;
        public static final double MAXIMUM_SLOPE_ASYMMETRY = 1.0;
        public static final double MINIMUM_INTERDUNE_CLEANUP = 0.0;
        public static final double MAXIMUM_INTERDUNE_CLEANUP = 1.0;
        public static final double MINIMUM_REPOSE_ANGLE = 10.0;
        public static final double MAXIMUM_REPOSE_ANGLE = 45.0;
        public static final int MAXIMUM_CASCADE_PASSES = 64;
        public static final int MAXIMUM_TRANSPORT_ITERATIONS = 1000;
        public static final int MAXIMUM_EDGE_BLEND_CELLS = GRID_SIZE / 2;
        public static final double MINIMUM_TRANSPORT_STRENGTH = 0.0;
        public static final double MAXIMUM_TRANSPORT_STRENGTH = 4.0;

        public static Settings defaults() {
            // Frozen transverse v1 development baseline (0.5.6). Keep changes to these
            // calibrated values explicit so later regional terrain work can treat this
            // local dune synthesizer as a stable input.
            return new Settings(
                    8,
                    0,
                    350.0,
                    0.18,
                    3.0,
                    0.20,
                    0.82,
                    0.40,
                    33.0,
                    25,
                    0,
                    24.0,
                    7,
                    1.0
            );
        }

        public int effectiveMaximumHeight(DuneMode mode) {
            return maximumHeightOverride == 0
                    ? mode.maximumHeight()
                    : maximumHeightOverride;
        }

        public int effectiveTransportIterations(DuneMode mode) {
            return transportIterationsOverride == 0
                    ? mode.transportIterations()
                    : transportIterationsOverride;
        }

        public int maximumConfiguredHeightCeiling() {
            return maximumHeightOverride == 0
                    ? Math.max(DuneMode.TRANSVERSE.maximumHeight(), DuneMode.BARCHAN.maximumHeight())
                    : maximumHeightOverride;
        }

        public int regionBlockSize() {
            return GRID_SIZE * cellSize;
        }

        public Settings withCellSize(int value) {
            return copy(value, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withMaximumHeightOverride(int value) {
            return copy(cellSize, value, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withDuneSpacingBlocks(double value) {
            return copy(cellSize, maximumHeightOverride, value, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withSpacingVariation(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, value,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withRidgeSharpness(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    value, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withValleyCutoff(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, value, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withSlopeAsymmetry(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, value, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withInterduneCleanup(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, value,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withReposeAngleDegrees(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    value, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withCascadePasses(int value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, value, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withTransportIterationsOverride(int value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, value,
                    windAngleDegrees, edgeBlendCells, transportStrength);
        }

        public Settings withWindAngleDegrees(double value) {
            double normalized = value % 360.0;
            if (normalized < 0.0) {
                normalized += 360.0;
            }
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    normalized, edgeBlendCells, transportStrength);
        }

        public Settings withEdgeBlendCells(int value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, value, transportStrength);
        }

        public Settings withTransportStrength(double value) {
            return copy(cellSize, maximumHeightOverride, duneSpacingBlocks, spacingVariation,
                    ridgeSharpness, valleyCutoff, slopeAsymmetry, interduneCleanup,
                    reposeAngleDegrees, cascadePasses, transportIterationsOverride,
                    windAngleDegrees, edgeBlendCells, value);
        }

        private static Settings copy(
                int cellSize,
                int maximumHeightOverride,
                double duneSpacingBlocks,
                double spacingVariation,
                double ridgeSharpness,
                double valleyCutoff,
                double slopeAsymmetry,
                double interduneCleanup,
                double reposeAngleDegrees,
                int cascadePasses,
                int transportIterationsOverride,
                double windAngleDegrees,
                int edgeBlendCells,
                double transportStrength
        ) {
            return new Settings(
                    cellSize,
                    maximumHeightOverride,
                    duneSpacingBlocks,
                    spacingVariation,
                    ridgeSharpness,
                    valleyCutoff,
                    slopeAsymmetry,
                    interduneCleanup,
                    reposeAngleDegrees,
                    cascadePasses,
                    transportIterationsOverride,
                    windAngleDegrees,
                    edgeBlendCells,
                    transportStrength
            );
        }
    }
    public record Result(
            int[] surfaceUnits,
            double initialMass,
            double finalMass,
            double maximumHeight,
            DuneMode mode,
            long seed,
            Settings settings,
            DuneSurfaceResolution surfaceResolution
    ) {
        public int surfaceUnitsAt(int localX, int localZ) {
            return surfaceUnits[localZ * settings.regionBlockSize() + localX];
        }

        public int fullBlocksAt(int localX, int localZ) {
            return surfaceResolution.fullBlocks(surfaceUnitsAt(localX, localZ));
        }

        public int partialLayersAt(int localX, int localZ) {
            return surfaceResolution.partialLayers(surfaceUnitsAt(localX, localZ));
        }

        public double massDifference() {
            return finalMass - initialMass;
        }
    }

    private record Wind(double x, double z, double crosswindX, double crosswindZ) {
        static Wind fromAngle(double angleDegrees) {
            // Generator convention: 0 degrees points toward +X and 90 degrees toward +Z.
            // This is intentionally independent of Minecraft player/camera yaw.
            double radians = Math.toRadians(angleDegrees);
            double x = Math.cos(radians);
            double z = Math.sin(radians);
            return new Wind(x, z, -z, x);
        }
    }
}
