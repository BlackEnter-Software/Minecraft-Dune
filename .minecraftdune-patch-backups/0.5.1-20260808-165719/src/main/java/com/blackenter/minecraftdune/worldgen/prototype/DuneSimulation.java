package com.blackenter.minecraftdune.worldgen.prototype;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Deterministic, reduced-resolution dune simulation for the Arrakis Dev world.
 *
 * <p>This is deliberately a development prototype rather than the final Gameplay
 * Arrakis chunk generator. It models a mobile sand-thickness field, directional
 * saltation-like transport, lee-side erosion dampening, and repeated slope
 * stabilization. The resulting grid is upscaled to Minecraft block columns.</p>
 */
public final class DuneSimulation {
    public static final int GRID_SIZE = 64;
    public static final int CELL_SIZE = 2;
    public static final int REGION_BLOCK_SIZE = GRID_SIZE * CELL_SIZE;
    public static final int BASE_SURFACE_Y = 64;
    public static final int MAXIMUM_PROTOTYPE_HEIGHT = 20;

    private static final double WIND_ANGLE_RADIANS = Math.toRadians(24.0);
    private static final double WIND_X = Math.cos(WIND_ANGLE_RADIANS);
    private static final double WIND_Z = Math.sin(WIND_ANGLE_RADIANS);
    private static final double CROSSWIND_X = -WIND_Z;
    private static final double CROSSWIND_Z = WIND_X;

    // A two-block simulation cell may rise by roughly one block before cascading.
    private static final double MAXIMUM_STABLE_SLOPE = 1.15;
    private static final int EDGE_BLEND_CELLS = 7;

    private DuneSimulation() {
    }

    public static Result simulate(DuneMode mode, long seed) {
        double[] sand = createInitialSandField(mode, seed);
        double initialMass = sum(sand);

        for (int iteration = 0; iteration < mode.transportIterations(); iteration++) {
            sand = transportSand(sand, mode, seed, iteration);
            sand = stabilizeSlopes(sand);
            sand = stabilizeSlopes(sand);
        }

        double finalMass = sum(sand);
        int[] heights = convertToBlockHeights(sand, mode);
        int maximumHeight = Arrays.stream(heights).max().orElse(0);

        return new Result(
                heights,
                initialMass,
                finalMass,
                maximumHeight,
                mode,
                seed
        );
    }

    private static double[] createInitialSandField(DuneMode mode, long seed) {
        return switch (mode) {
            case TRANSVERSE -> createTransverseField(seed);
            case BARCHAN -> createBarchanField(seed);
        };
    }

    private static double[] createTransverseField(long seed) {
        double[] sand = new double[GRID_SIZE * GRID_SIZE];

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                double alongWind = x * WIND_X + z * WIND_Z;
                double acrossWind = x * CROSSWIND_X + z * CROSSWIND_Z;
                double phaseWarp =
                        0.60 * Math.sin(acrossWind * 0.13)
                                + 0.35 * Math.sin(acrossWind * 0.31 + 1.2);
                double phase = alongWind * (Math.PI * 2.0 / 11.5) + phaseWarp;
                double ridge = Math.pow((Math.sin(phase) + 1.0) * 0.5, 2.2);
                double variation = unitHash(seed, x, z, -1) * 0.16;

                sand[index(x, z)] = 2.2 + 4.4 * ridge + variation;
            }
        }

        return sand;
    }

    private static double[] createBarchanField(long seed) {
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
                    double alongWind = deltaX * WIND_X + deltaZ * WIND_Z;
                    double acrossWind = deltaX * CROSSWIND_X + deltaZ * CROSSWIND_Z;

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
            int iteration
    ) {
        double[] delta = new double[current.length];
        int downwindX = nearestDirection(WIND_X);
        int downwindZ = nearestDirection(WIND_Z);

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

                int targetX = wrap((int) Math.round(
                        x + WIND_X * hopDistance + CROSSWIND_X * crosswindJitter
                ));
                int targetZ = wrap((int) Math.round(
                        z + WIND_Z * hopDistance + CROSSWIND_Z * crosswindJitter
                ));

                double lifted = Math.min(
                        available * 0.045,
                        0.045 + Math.max(0.0, downwindSlope) * 0.018
                );
                lifted *= 0.65 + randomA * 0.70;
                lifted /= 1.0 + windShadow * 2.5;

                if (mode == DuneMode.BARCHAN) {
                    lifted *= 1.12;
                }

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

    private static double[] stabilizeSlopes(double[] current) {
        double[] delta = new double[current.length];

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int sourceIndex = index(x, z);
                double sourceHeight = current[sourceIndex];
                int lowestIndex = sourceIndex;
                double lowestHeight = sourceHeight;

                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    for (int offsetX = -1; offsetX <= 1; offsetX++) {
                        if (offsetX == 0 && offsetZ == 0) {
                            continue;
                        }

                        int candidateIndex = indexWrapped(x + offsetX, z + offsetZ);
                        double candidateHeight = current[candidateIndex];
                        if (candidateHeight < lowestHeight) {
                            lowestHeight = candidateHeight;
                            lowestIndex = candidateIndex;
                        }
                    }
                }

                double excessSlope = sourceHeight - lowestHeight - MAXIMUM_STABLE_SLOPE;
                if (excessSlope <= 0.0) {
                    continue;
                }

                double moved = Math.min(sourceHeight * 0.25, excessSlope * 0.18);
                delta[sourceIndex] -= moved;
                delta[lowestIndex] += moved;
            }
        }

        double[] next = new double[current.length];
        for (int i = 0; i < current.length; i++) {
            next[i] = Math.max(0.0, current[i] + delta[i]);
        }
        return next;
    }

    private static int[] convertToBlockHeights(double[] sand, DuneMode mode) {
        double[] sorted = sand.clone();
        Arrays.sort(sorted);

        double lowerPercentile = mode == DuneMode.TRANSVERSE ? 0.10 : 0.24;
        double lower = percentile(sorted, lowerPercentile);
        double upper = percentile(sorted, 0.97);
        double range = Math.max(0.0001, upper - lower);
        double[] scaled = new double[sand.length];

        for (int z = 0; z < GRID_SIZE; z++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int index = index(x, z);
                double normalized = clamp((sand[index] - lower) / range, 0.0, 1.0);
                normalized = Math.pow(normalized, mode == DuneMode.TRANSVERSE ? 0.90 : 1.05);

                int edgeDistance = Math.min(
                        Math.min(x, z),
                        Math.min(GRID_SIZE - 1 - x, GRID_SIZE - 1 - z)
                );
                double edgeBlend = smoothStep(
                        0.0,
                        EDGE_BLEND_CELLS,
                        edgeDistance
                );

                scaled[index] = normalized * mode.maximumHeight() * edgeBlend;
            }
        }

        int[] heights = new int[REGION_BLOCK_SIZE * REGION_BLOCK_SIZE];
        for (int blockZ = 0; blockZ < REGION_BLOCK_SIZE; blockZ++) {
            for (int blockX = 0; blockX < REGION_BLOCK_SIZE; blockX++) {
                double gridX = blockX / (double) CELL_SIZE;
                double gridZ = blockZ / (double) CELL_SIZE;
                int x0 = Math.min((int) Math.floor(gridX), GRID_SIZE - 1);
                int z0 = Math.min((int) Math.floor(gridZ), GRID_SIZE - 1);
                int x1 = Math.min(x0 + 1, GRID_SIZE - 1);
                int z1 = Math.min(z0 + 1, GRID_SIZE - 1);
                double fractionX = gridX - x0;
                double fractionZ = gridZ - z0;

                double top = lerp(
                        lerp(scaled[index(x0, z0)], scaled[index(x1, z0)], fractionX),
                        lerp(scaled[index(x0, z1)], scaled[index(x1, z1)], fractionX),
                        fractionZ
                );

                heights[blockZ * REGION_BLOCK_SIZE + blockX] = (int) Math.round(
                        clamp(top, 0.0, MAXIMUM_PROTOTYPE_HEIGHT)
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

    public record Result(
            int[] heights,
            double initialMass,
            double finalMass,
            int maximumHeight,
            DuneMode mode,
            long seed
    ) {
        public int heightAt(int localX, int localZ) {
            return heights[localZ * REGION_BLOCK_SIZE + localX];
        }

        public double massDifference() {
            return finalMass - initialMass;
        }
    }
}
