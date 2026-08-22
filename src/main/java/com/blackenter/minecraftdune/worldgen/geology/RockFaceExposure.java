package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Shared, deterministic description of an exposed rock face derived from terrain height.
 *
 * <p>A formation mask says which geological body owns a column; it is not a cliff detector.
 * This field therefore compares the current terrain envelope with two fixed cardinal probe
 * rings. The near ring finds the physical boundary and bounds ordinary retreat, while the far
 * ring measures the full relief and supplies a stable downhill normal. The resulting sample is
 * computed once per X/Z column and reused for every Y block in that column.</p>
 */
public final class RockFaceExposure {
    private RockFaceExposure() {
    }

    public static Sample sample(
            long worldSeed,
            double worldX,
            double worldZ,
            int currentTopY,
            MacroGeologyField.Sample geology,
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.ErosionSettings erosion = settings.erosion();
        ArrakisTerrainSettings.SurfaceErosionSettings surface = erosion.surface();
        double provincePermission = Math.max(
                geology.massifWeight(),
                Math.max(
                        geology.faultedMarginWeight() * 0.78,
                        Math.max(
                                geology.brokenRockWeight() * Math.max(
                                        erosion.brokenRockScale(),
                                        surface.brokenRockStrength()
                                ),
                                Math.max(
                                        geology.innerForelandWeight()
                                                * surface.smallRockStrength(),
                                        geology.sandRockTransitionWeight()
                                                * surface.brokenRockStrength() * 0.42
                                )
                        )
                )
        );
        if (!erosion.enabled()
                || (currentTopY <= MacroGeologyField.BASE_SURFACE_Y + 2
                && geology.rockFormationMask() <= 0.015)
                || geology.sandCorridorMask() > 0.35
                || geology.faultCarveMask() > 0.86
                || provincePermission <= 0.015) {
            return Sample.NONE;
        }

        int retreat = Math.max(1, Math.min(8, erosion.surface().maxRetreatBlocks()));
        double nearProbe = Math.max(2.0, retreat + 1.0);
        double farProbe = GeologyNoise.clamp(erosion.faceProbeDistance(), 6.0, 48.0);
        nearProbe = Math.min(nearProbe, farProbe);

        double westFar = top(worldSeed, worldX - farProbe, worldZ, settings);
        double eastFar = top(worldSeed, worldX + farProbe, worldZ, settings);
        double northFar = top(worldSeed, worldX, worldZ - farProbe, settings);
        double southFar = top(worldSeed, worldX, worldZ + farProbe, settings);
        double westNear = westFar;
        double eastNear = eastFar;
        double northNear = northFar;
        double southNear = southFar;
        if (surface.enabled() && nearProbe < farProbe - 0.01) {
            westNear = top(worldSeed, worldX - nearProbe, worldZ, settings);
            eastNear = top(worldSeed, worldX + nearProbe, worldZ, settings);
            northNear = top(worldSeed, worldX, worldZ - nearProbe, settings);
            southNear = top(worldSeed, worldX, worldZ + nearProbe, settings);
        } else {
            nearProbe = farProbe;
        }

        return fromHeights(
                currentTopY,
                westNear,
                eastNear,
                northNear,
                southNear,
                westFar,
                eastFar,
                northFar,
                southFar,
                nearProbe,
                farProbe,
                erosion.minimumRelief()
        );
    }

    private static double top(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings settings
    ) {
        return MacroGeologyField.sample(worldSeed, worldX, worldZ, settings).baseElevation();
    }

    /** Package-visible synthetic-height entry point used by deterministic validation. */
    static Sample fromHeights(
            double currentTop,
            double westNear,
            double eastNear,
            double northNear,
            double southNear,
            double westFar,
            double eastFar,
            double northFar,
            double southFar,
            double nearProbe,
            double farProbe,
            double configuredMinimumRelief
    ) {
        double minimumTop = minimum(
                currentTop,
                westNear,
                eastNear,
                northNear,
                southNear,
                westFar,
                eastFar,
                northFar,
                southFar
        );
        double maximumTop = maximum(
                currentTop,
                westNear,
                eastNear,
                northNear,
                southNear,
                westFar,
                eastFar,
                northFar,
                southFar
        );
        double localRelief = Math.max(0.0, maximumTop - minimumTop);

        double gradientX = (eastFar - westFar) / (farProbe * 2.0);
        double gradientZ = (southFar - northFar) / (farProbe * 2.0);
        double gradientLength = Math.hypot(gradientX, gradientZ);
        double outwardX;
        double outwardZ;
        if (gradientLength >= 0.01) {
            outwardX = -gradientX / gradientLength;
            outwardZ = -gradientZ / gradientLength;
        } else {
            Direction direction = dominantDirection(
                    currentTop,
                    westNear,
                    eastNear,
                    northNear,
                    southNear,
                    westFar,
                    eastFar,
                    northFar,
                    southFar
            );
            outwardX = direction.outwardX();
            outwardZ = direction.outwardZ();
        }

        double westWeight = Math.max(0.0, -outwardX);
        double eastWeight = Math.max(0.0, outwardX);
        double northWeight = Math.max(0.0, -outwardZ);
        double southWeight = Math.max(0.0, outwardZ);
        double weight = Math.max(1.0e-6, westWeight + eastWeight + northWeight + southWeight);
        double nearDownhillTop = (
                westNear * westWeight
                        + eastNear * eastWeight
                        + northNear * northWeight
                        + southNear * southWeight
        ) / weight;
        double farDownhillTop = (
                westFar * westWeight
                        + eastFar * eastWeight
                        + northFar * northWeight
                        + southFar * southWeight
        ) / weight;

        double nearDifference = currentTop - nearDownhillTop;
        double farDifference = currentTop - farDownhillTop;
        boolean highSide = currentTop >= (minimumTop + maximumTop) * 0.5;
        double nearRelief = Math.abs(nearDifference);
        double directionalRelief = Math.max(Math.abs(farDifference), nearRelief);
        double steepness = Math.max(
                nearRelief / Math.max(1.0, nearProbe),
                directionalRelief / Math.max(1.0, farProbe)
        );

        double minimumRelief = Math.max(4.0, configuredMinimumRelief);
        double reliefGate = GeologyNoise.smoothStep(
                Math.max(3.0, minimumRelief * 0.28),
                minimumRelief + 18.0,
                localRelief
        );
        double adjacencyGate = GeologyNoise.smoothStep(
                1.25,
                Math.max(4.0, minimumRelief * 0.48),
                nearRelief
        );
        double steepnessGate = GeologyNoise.smoothStep(0.45, 2.25, steepness);
        double exposure = GeologyNoise.clamp(
                reliefGate * steepnessGate * (0.24 + adjacencyGate * 0.76),
                0.0,
                1.0
        );

        double signedFaceDistance;
        if (localRelief <= 1.0e-6) {
            signedFaceDistance = Double.POSITIVE_INFINITY;
        } else {
            double position = GeologyNoise.clamp(
                    (currentTop - minimumTop) / localRelief,
                    0.0,
                    1.0
            );
            signedFaceDistance = (position - 0.5) * nearProbe;
        }

        double faceInset = nearProbe;
        if (highSide && localRelief > 1.0e-6) {
            faceInset = nearProbe * (
                    1.0 - GeologyNoise.clamp(nearRelief / localRelief, 0.0, 1.0)
            );
        }

        return new Sample(
                exposure > 0.001 && localRelief > 0.0,
                exposure,
                localRelief,
                nearRelief,
                steepness,
                outwardX,
                outwardZ,
                (int) Math.floor(minimumTop + 0.5),
                (int) Math.floor(maximumTop + 0.5),
                signedFaceDistance,
                faceInset,
                highSide,
                nearProbe,
                farProbe
        );
    }

    private static Direction dominantDirection(
            double currentTop,
            double westNear,
            double eastNear,
            double northNear,
            double southNear,
            double westFar,
            double eastFar,
            double northFar,
            double southFar
    ) {
        double[] near = {westNear, eastNear, northNear, southNear};
        double[] far = {westFar, eastFar, northFar, southFar};
        double[] x = {-1.0, 1.0, 0.0, 0.0};
        double[] z = {0.0, 0.0, -1.0, 1.0};
        int best = 0;
        double bestDifference = 0.0;
        for (int index = 0; index < near.length; index++) {
            double difference = currentTop - (near[index] * 0.62 + far[index] * 0.38);
            if (Math.abs(difference) > Math.abs(bestDifference)) {
                bestDifference = difference;
                best = index;
            }
        }
        double sign = bestDifference >= 0.0 ? 1.0 : -1.0;
        return new Direction(x[best] * sign, z[best] * sign);
    }

    private static double minimum(double... values) {
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static double maximum(double... values) {
        double result = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private record Direction(double outwardX, double outwardZ) {
    }

    public record Sample(
            boolean exposed,
            double exposure,
            double localRelief,
            double nearRelief,
            double steepness,
            double outwardNormalX,
            double outwardNormalZ,
            int lowY,
            int highY,
            double signedFaceDistance,
            double faceInset,
            boolean highSide,
            double nearProbeDistance,
            double farProbeDistance
    ) {
        public static final Sample NONE = new Sample(
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                MacroGeologyField.BASE_SURFACE_Y,
                MacroGeologyField.BASE_SURFACE_Y,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                false,
                1.0,
                1.0
        );
    }
}
