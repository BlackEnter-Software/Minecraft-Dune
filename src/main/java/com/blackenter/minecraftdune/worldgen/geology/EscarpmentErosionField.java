package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Deterministic 3D cliff occupancy for the 0.5.14 escarpment pass.
 *
 * <p>The field is removal-only: it never creates rock above the macro/fissure envelope. A
 * locally estimated signed formation-edge distance replaces the former smooth apron with a
 * steep face. Per-Y lithology then moves that face inward by different amounts, allowing hard
 * benches and bounded rock-air-rock undercuts without bridging faults or sand corridors.</p>
 */
public final class EscarpmentErosionField {
    private static final long FACE_DETAIL_SALT = 0x4F92C7A63D18B5E1L;
    private static final long UNDERCUT_SALT = 0x73D10ACF2569E84BL;
    private static final long TALUS_PATCH_SALT = 0x1E6B94D235A78FC0L;
    private static final long TALUS_MATERIAL_SALT = 0x65A2E819C4F037BDL;

    private EscarpmentErosionField() {
    }

    public static Column sample(
            long worldSeed,
            double worldX,
            double worldZ,
            int originalRockTopY,
            int fissureRockTopY,
            MacroGeologyField.Sample geology,
            LithologyField.Column lithology,
            MassifFractureField.Sample fracture,
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.ErosionSettings erosion = settings.erosion();
        if (!erosion.enabled()
                || geology.sandCorridorMask() > 0.08
                || geology.faultCarveMask() > 0.20) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double edgeThreshold = GeologyNoise.clamp(
                erosion.escarpmentStartStrength(),
                0.05,
                0.90
        );
        double formationMask = geology.rockFormationMask();
        if (formationMask < edgeThreshold - 0.30
                || formationMask > edgeThreshold + 0.54) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double brokenScale = GeologyNoise.clamp(erosion.brokenRockScale(), 0.0, 1.5);
        double provincePermission = Math.max(
                geology.massifWeight(),
                Math.max(
                        geology.faultedMarginWeight() * 0.78,
                        geology.brokenRockWeight() * brokenScale
                )
        );
        if (provincePermission <= 0.06) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double probe = GeologyNoise.clamp(erosion.faceProbeDistance(), 6.0, 48.0);
        MacroGeologyField.Sample west = MacroGeologyField.sample(
                worldSeed, worldX - probe, worldZ, settings
        );
        MacroGeologyField.Sample east = MacroGeologyField.sample(
                worldSeed, worldX + probe, worldZ, settings
        );
        MacroGeologyField.Sample north = MacroGeologyField.sample(
                worldSeed, worldX, worldZ - probe, settings
        );
        MacroGeologyField.Sample south = MacroGeologyField.sample(
                worldSeed, worldX, worldZ + probe, settings
        );

        double westTop = west.baseElevation();
        double eastTop = east.baseElevation();
        double northTop = north.baseElevation();
        double southTop = south.baseElevation();
        double minimumTop = Math.min(
                geology.baseElevation(),
                Math.min(Math.min(westTop, eastTop), Math.min(northTop, southTop))
        );
        double maximumTop = Math.max(
                geology.baseElevation(),
                Math.max(Math.max(westTop, eastTop), Math.max(northTop, southTop))
        );
        double localRelief = maximumTop - minimumTop;
        double minimumRelief = Math.max(4.0, erosion.minimumRelief());
        double reliefGate = GeologyNoise.smoothStep(
                minimumRelief,
                minimumRelief + 26.0,
                localRelief
        );
        double sourceHeightGate = GeologyNoise.smoothStep(
                minimumRelief,
                minimumRelief + 18.0,
                maximumTop - MacroGeologyField.BASE_SURFACE_Y
        );
        if (reliefGate <= 0.0 || sourceHeightGate <= 0.0) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double maskGradientX = (east.rockFormationMask() - west.rockFormationMask())
                / (probe * 2.0);
        double maskGradientZ = (south.rockFormationMask() - north.rockFormationMask())
                / (probe * 2.0);
        double maskGradientLength = Math.hypot(maskGradientX, maskGradientZ);
        double heightGradientX = (eastTop - westTop) / (probe * 2.0);
        double heightGradientZ = (southTop - northTop) / (probe * 2.0);
        double heightGradientLength = Math.hypot(heightGradientX, heightGradientZ);

        double inwardX;
        double inwardZ;
        if (maskGradientLength >= 0.0010) {
            inwardX = maskGradientX / maskGradientLength;
            inwardZ = maskGradientZ / maskGradientLength;
        } else if (heightGradientLength >= 0.01) {
            inwardX = heightGradientX / heightGradientLength;
            inwardZ = heightGradientZ / heightGradientLength;
        } else {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double distanceDenominator = Math.max(maskGradientLength, 1.0 / (probe * 3.2));
        double signedFaceDistance = GeologyNoise.clamp(
                (formationMask - edgeThreshold) / distanceDenominator,
                -probe * 2.0,
                probe * 2.0
        );
        double faceGate = 1.0 - GeologyNoise.smoothStep(
                probe * 0.82,
                probe * 1.72,
                Math.abs(signedFaceDistance)
        );
        double permissionGate = GeologyNoise.smoothStep(0.10, 0.55, provincePermission);
        double verticalBias = GeologyNoise.clamp(erosion.verticalFaceBias(), 0.0, 1.25);
        double escarpmentStrength = GeologyNoise.clamp(
                reliefGate
                        * sourceHeightGate
                        * permissionGate
                        * faceGate
                        * (0.55 + verticalBias * 0.68),
                0.0,
                1.0
        );
        if (escarpmentStrength <= 0.02) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion
            );
        }

        double outwardX = -inwardX;
        double outwardZ = -inwardZ;
        double windRadians = Math.toRadians(settings.nativeDunes().windAngleDegrees());
        double windX = Math.cos(windRadians);
        double windZ = Math.sin(windRadians);
        double windFacing = Math.max(0.0, outwardX * windX + outwardZ * windZ);
        double shelter = 0.72 + 0.28 * (0.5 + 0.5 * GeologyNoise.value2(
                worldSeed ^ FACE_DETAIL_SALT,
                worldX / 310.0,
                worldZ / 310.0
        ));
        double windExposure = GeologyNoise.clamp(
                windFacing * (0.62 + reliefGate * 0.38) * shelter,
                0.0,
                1.0
        );

        int maximumUndercut = Math.max(0, Math.min(16, erosion.maxUndercutBlocks()));
        double fractureHalo = Math.max(5.0, maximumUndercut * 2.0 + 2.0);
        double fractureProximity = 0.0;
        if (fracture.activation() > 0.0 && Double.isFinite(fracture.distance())) {
            fractureProximity = 1.0 - GeologyNoise.smoothStep(
                    fracture.halfWidth(),
                    fracture.halfWidth() + fractureHalo,
                    fracture.distance()
            );
        }
        double fractureErosion = GeologyNoise.clamp(
                Math.max(fracture.strength(), fractureProximity * fracture.activation())
                        + fracture.intersectionStrength() * 0.45,
                0.0,
                1.0
        );
        double fractureReachBottomY = originalRockTopY - Math.max(
                4.0,
                fracture.designDepth()
                        * (1.0 + fracture.intersectionStrength() * 0.35)
        );

        double surfaceRetention = retention(lithology.sample(fissureRockTopY).resistance());
        double capSupport = surfaceRetention;
        for (int depth = 1; depth <= 2; depth++) {
            int y = Math.max(MacroGeologyField.BASE_SURFACE_Y + 1, fissureRockTopY - depth);
            capSupport = Math.min(capSupport, retention(lithology.sample(y).resistance()));
        }
        double undercutPatch = GeologyNoise.smoothStep(
                1.0 - GeologyNoise.clamp(erosion.undercutFrequency(), 0.0, 1.0),
                Math.min(1.0, 1.18 - GeologyNoise.clamp(
                        erosion.undercutFrequency(), 0.0, 1.0
                )),
                0.5 + 0.5 * GeologyNoise.value2(
                        worldSeed ^ UNDERCUT_SALT,
                        worldX / 78.0,
                        worldZ / 78.0
                )
        );
        double undercutPotential = GeologyNoise.clamp(
                escarpmentStrength
                        * undercutPatch
                        * GeologyNoise.smoothStep(0.62, 0.82, capSupport)
                        * GeologyNoise.clamp(erosion.undercutStrength(), 0.0, 1.5),
                0.0,
                1.0
        );
        double exposure = GeologyNoise.clamp(
                0.62
                        + windExposure * Math.max(0.0, erosion.windExposureStrength())
                        + fractureErosion * Math.max(0.0, erosion.fractureErosionStrength()),
                0.0,
                1.8
        );
        double maximumRetreat = Math.min(
                maximumUndercut,
                maximumUndercut
                        * escarpmentStrength
                        * exposure
                        * Math.max(0.0, erosion.softRockMultiplier())
        );

        ArrakisTerrainSettings.TalusSettings talus = settings.lithology().talus();
        double outsideDistance = Math.max(0.0, -signedFaceDistance);
        double talusSpread = Math.max(1.0, talus.spread());
        double talusFalloff = 1.0 - GeologyNoise.smoothStep(
                0.0,
                talusSpread,
                outsideDistance
        );
        double insideSuppression = 1.0 - GeologyNoise.smoothStep(
                0.0,
                3.0,
                Math.max(0.0, signedFaceDistance)
        );
        double lowSideGate = 1.0 - GeologyNoise.smoothStep(
                minimumTop + 3.0,
                minimumTop + Math.max(8.0, localRelief * 0.42),
                geology.baseElevation()
        );
        double talusPatch = GeologyNoise.smoothStep(
                -0.12,
                0.58,
                GeologyNoise.value2(
                        worldSeed ^ TALUS_PATCH_SALT,
                        worldX / Math.max(42.0, talusSpread * 4.0),
                        worldZ / Math.max(42.0, talusSpread * 4.0)
                )
        );
        double fractureOutletBoost = fracture.talusCandidate(talus) ? 0.18 : 0.0;
        double talusSuitability = talus.localScreeEnabled()
                ? GeologyNoise.clamp(
                        escarpmentStrength
                                * talusFalloff
                                * insideSuppression
                                * (0.86 + lowSideGate * 0.14)
                                * talusPatch
                                * (0.72 + windExposure * 0.16
                                + fractureErosion * 0.32 + fractureOutletBoost),
                        0.0,
                        1.0
                )
                : 0.0;
        int talusThickness = 0;
        double talusThreshold = GeologyNoise.clamp(
                talus.minimumFractureStrength(),
                0.0,
                0.98
        );
        int maximumTalusThickness = Math.max(
                0,
                Math.min(32, talus.maximumThickness())
        );
        if (maximumTalusThickness > 0 && talusSuitability >= talusThreshold) {
            double normalized = (talusSuitability - talusThreshold)
                    / Math.max(0.02, 1.0 - talusThreshold);
            talusThickness = Math.max(
                    1,
                    (int) Math.ceil(
                            maximumTalusThickness
                                    * Math.pow(normalized, 0.72)
                                    * talusFalloff
                    )
            );
        }

        return new Column(
                true,
                worldSeed,
                worldX,
                worldZ,
                originalRockTopY,
                fissureRockTopY,
                (int) Math.floor(maximumTop + 0.5),
                escarpmentStrength,
                localRelief,
                signedFaceDistance,
                outwardX,
                outwardZ,
                windExposure,
                fractureErosion,
                fractureReachBottomY,
                undercutPotential,
                maximumRetreat,
                talusSuitability,
                talusThickness,
                capSupport,
                probe,
                erosion
        );
    }

    public static double retreatMultiplier(
            LithologyField.ResistanceClass resistance,
            ArrakisTerrainSettings.ErosionSettings settings
    ) {
        return switch (resistance) {
            case SOFT -> Math.max(0.0, settings.softRockMultiplier());
            case MEDIUM -> 1.0;
            case HARD -> Math.max(0.0, settings.hardRockMultiplier());
            case VERY_HARD -> Math.max(0.0, settings.veryHardRockMultiplier());
            case LOOSE -> Math.max(0.0, settings.softRockMultiplier()) * 1.15;
        };
    }

    private static double retention(LithologyField.ResistanceClass resistance) {
        return switch (resistance) {
            case LOOSE -> 0.0;
            case SOFT -> 0.16;
            case MEDIUM -> 0.48;
            case HARD -> 0.78;
            case VERY_HARD -> 1.0;
        };
    }

    public record Column(
            boolean candidate,
            long worldSeed,
            double worldX,
            double worldZ,
            int originalRockTopY,
            int fissureRockTopY,
            int sourceTopY,
            double escarpmentStrength,
            double localRelief,
            double signedFaceDistance,
            double outwardNormalX,
            double outwardNormalZ,
            double windExposure,
            double fractureErosion,
            double fractureReachBottomY,
            double undercutPotential,
            double maximumRetreat,
            double talusSuitability,
            int talusThickness,
            double capSupport,
            double faceProbeDistance,
            ArrakisTerrainSettings.ErosionSettings settings
    ) {
        static Column inactive(
                long worldSeed,
                double worldX,
                double worldZ,
                int originalRockTopY,
                int fissureRockTopY,
                ArrakisTerrainSettings.ErosionSettings settings
        ) {
            return new Column(
                    false,
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    originalRockTopY,
                    0.0,
                    0.0,
                    Double.POSITIVE_INFINITY,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    originalRockTopY,
                    0.0,
                    0.0,
                    0.0,
                    0,
                    0.0,
                    Math.max(6.0, settings.faceProbeDistance()),
                    settings
            );
        }

        public boolean occupies(int worldY, LithologyField.Sample lithology) {
            if (worldY > fissureRockTopY) {
                return false;
            }
            if (!candidate || worldY <= MacroGeologyField.BASE_SURFACE_Y + 2) {
                return true;
            }

            int maximumUndercut = Math.max(0, Math.min(16, settings.maxUndercutBlocks()));
            double heightGate = GeologyNoise.smoothStep(
                    MacroGeologyField.BASE_SURFACE_Y + 2.0,
                    MacroGeologyField.BASE_SURFACE_Y + 11.0,
                    worldY
            );
            double susceptibility = retreatMultiplier(lithology.resistance(), settings);
            double materialRetention = retention(lithology.resistance());
            double fractureDepthGate = GeologyNoise.smoothStep(
                    fractureReachBottomY - 5.0,
                    fractureReachBottomY + 5.0,
                    worldY
            );
            double localFractureErosion = fractureErosion * fractureDepthGate;
            double exposure = GeologyNoise.clamp(
                    0.62
                            + windExposure * Math.max(0.0, settings.windExposureStrength())
                            + localFractureErosion
                            * Math.max(0.0, settings.fractureErosionStrength()),
                    0.0,
                    1.8
            );
            double baseRetreat = maximumUndercut
                    * 0.34
                    * exposure
                    * susceptibility;
            double fractureRetreat = maximumUndercut
                    * 0.28
                    * localFractureErosion
                    * susceptibility;

            double frequency = GeologyNoise.clamp(settings.undercutFrequency(), 0.0, 1.0);
            double cavitySignal = 0.5 + 0.5 * GeologyNoise.value3(
                    worldSeed ^ UNDERCUT_SALT,
                    worldX / 74.0,
                    worldY / Math.max(7.0, maximumUndercut * 1.55),
                    worldZ / 74.0
            );
            double cavityGate = GeologyNoise.smoothStep(
                    1.0 - frequency,
                    Math.min(1.0, 1.20 - frequency),
                    cavitySignal
            );
            double supportedCap = GeologyNoise.smoothStep(0.62, 0.82, capSupport);
            double contrast = Math.max(0.0, capSupport - materialRetention);
            double undercutRetreat = maximumUndercut
                    * GeologyNoise.clamp(settings.undercutStrength(), 0.0, 1.5)
                    * cavityGate
                    * supportedCap
                    * contrast;
            double resistantProjection = maximumUndercut
                    * 0.34
                    * cavityGate
                    * Math.max(0.0, materialRetention - 0.64);
            double targetBoundary = GeologyNoise.clamp(
                    baseRetreat + fractureRetreat + undercutRetreat - resistantProjection,
                    -maximumUndercut,
                    maximumUndercut
            );
            double effectiveStrength = GeologyNoise.clamp(
                    escarpmentStrength * heightGate,
                    0.0,
                    1.0
            );
            double preserveBoundary = -faceProbeDistance * 1.65;
            double boundary = GeologyNoise.lerp(
                    preserveBoundary,
                    targetBoundary,
                    effectiveStrength
            );
            double faceDetail = GeologyNoise.value3(
                    worldSeed ^ FACE_DETAIL_SALT,
                    worldX / 13.0,
                    worldY / 19.0,
                    worldZ / 13.0
            ) * 1.35;
            return signedFaceDistance + faceDetail >= boundary;
        }

        public int highestRockY(LithologyField.Column lithology) {
            return highestRockY(lithology, MassifFractureField.NONE);
        }

        public int highestRockY(
                LithologyField.Column lithology,
                MassifFractureField.Sample fracture
        ) {
            if (!candidate) {
                return fissureRockTopY;
            }
            for (int y = fissureRockTopY;
                    y >= MacroGeologyField.BASE_SURFACE_Y + 1;
                    y--) {
                LithologyField.Sample material = lithology.sample(y);
                if (fracture.calciteExposure(y, originalRockTopY, fissureRockTopY)) {
                    material = new LithologyField.Sample(
                            LithologyField.Material.CALCITE,
                            LithologyField.ResistanceClass.MEDIUM,
                            material.limestoneHost(),
                            false,
                            false,
                            true
                    );
                }
                if (occupies(y, material)) {
                    return y;
                }
            }
            return MacroGeologyField.BASE_SURFACE_Y;
        }

        /** Selects coherent debris clasts from the source unit with gravel as the matrix. */
        public LithologyField.Material talusMaterialAt(
                int worldY,
                LithologyField.Column lithology
        ) {
            double matrixNoise = GeologyNoise.value3(
                    worldSeed ^ TALUS_MATERIAL_SALT,
                    worldX / 14.0,
                    worldY / 7.0,
                    worldZ / 14.0
            );
            if (matrixNoise >= -0.24) {
                return LithologyField.Material.GRAVEL;
            }

            int sourceY = Math.max(
                    MacroGeologyField.BASE_SURFACE_Y + 1,
                    Math.min(sourceTopY, sourceTopY - Math.max(0, worldY - 65))
            );
            LithologyField.Material source = lithology.sample(sourceY).material();
            return source == LithologyField.Material.GRAVEL
                    ? LithologyField.Material.STONE
                    : source;
        }
    }
}
