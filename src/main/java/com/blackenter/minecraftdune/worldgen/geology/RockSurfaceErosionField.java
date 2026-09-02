package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Low-amplitude, deterministic erosion applied to exposed rock that is not necessarily part of
 * a major 0.5.14 escarpment event.
 *
 * <p>The field is deliberately cheaper and smaller in amplitude than
 * {@link EscarpmentErosionField}. It roughens ordinary massif faces, widens local fissures
 * according to lithology, breaks the silhouettes of foreland/Broken Rock remnants, and lets
 * resistant units stand proud. It is removal-only and never creates rock.</p>
 */
public final class RockSurfaceErosionField {
    private static final long COARSE_SALT = 0x6F53A149D827CBE1L;
    private static final long DETAIL_SALT = 0x3C91E0AF5B6274D8L;
    private static final long VERTICAL_SALT = 0x7249D3B851AEC06FL;
    private static final long FRACTURE_SALT = 0x1B68F4C297D50AE3L;

    private RockSurfaceErosionField() {
    }

    public static Column sample(
            long worldSeed,
            double worldX,
            double worldZ,
            int originalRockTopY,
            int fissureRockTopY,
            MacroGeologyField.Sample geology,
            RockFaceExposure.Sample face,
            MassifFractureField.Sample fracture,
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.ErosionSettings erosion = settings.erosion();
        ArrakisTerrainSettings.SurfaceErosionSettings surface = erosion.surface();
        double faultErosionPermission = ScarpMorphologyField.faultErosionPermission(
                geology.faultCarveMask()
        );

        int rockHeight = originalRockTopY - MacroGeologyField.BASE_SURFACE_Y;
        if (!erosion.enabled()
                || !surface.enabled()
                || rockHeight <= 2
                || geology.sandCorridorMask() > 0.35
                || faultErosionPermission <= 0.015) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion,
                    surface
            );
        }

        double massifPermission = Math.max(
                ScarpMorphologyField.massifErosionPermission(
                        geology,
                        settings.massif()
                ),
                geology.faultedMarginWeight() * 0.72
        );
        double forelandPermission = geology.innerForelandWeight()
                * Math.max(0.0, surface.smallRockStrength());
        double brokenPermission = Math.max(
                geology.brokenRockWeight(),
                geology.sandRockTransitionWeight() * 0.42
        ) * Math.max(0.0, surface.brokenRockStrength());

        double provinceStrength = GeologyNoise.clamp(
                Math.max(
                        massifPermission,
                        Math.max(forelandPermission, brokenPermission)
                ),
                0.0,
                1.25
        );
        if (provinceStrength <= 0.015) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion,
                    surface
            );
        }

        int configuredRetreat = Math.max(
                0,
                Math.min(8, surface.maxRetreatBlocks())
        );
        int maximumRetreat = Math.min(
                configuredRetreat,
                Math.max(0, rockHeight - 1)
        );
        if (maximumRetreat <= 0) {
            return Column.inactive(
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    erosion,
                    surface
            );
        }

        double coarseScale = Math.max(6.0, surface.scale());
        double detailScale = Math.max(2.0, surface.detailScale());
        double coarse = 0.5 + 0.5 * GeologyNoise.value2(
                worldSeed ^ COARSE_SALT,
                worldX / coarseScale,
                worldZ / coarseScale
        );
        double detail = 0.5 + 0.5 * GeologyNoise.value2(
                worldSeed ^ DETAIL_SALT,
                worldX / detailScale,
                worldZ / detailScale
        );
        double pattern = GeologyNoise.clamp(
                coarse * 0.72 + detail * 0.28,
                0.0,
                1.0
        );

        double fractureProximity = 0.0;
        if (fracture.activation() > 0.0 && Double.isFinite(fracture.distance())) {
            double halo = Math.max(
                    4.0,
                    maximumRetreat * 2.0 + 3.0
            );
            fractureProximity = 1.0 - GeologyNoise.smoothStep(
                    fracture.halfWidth(),
                    fracture.halfWidth() + halo,
                    fracture.distance()
            );
        }
        double fractureStrength = GeologyNoise.clamp(
                fractureProximity
                        * fracture.activation()
                        * Math.max(0.0, surface.fissureMultiplier())
                        + fracture.intersectionStrength() * 0.28,
                0.0,
                2.0
        );

        double baseStrength = GeologyNoise.clamp(
                Math.max(0.0, surface.strength())
                        * provinceStrength
                        * faultErosionPermission,
                0.0,
                1.5
        );
        double topStrength = GeologyNoise.clamp(
                baseStrength
                        * (0.18 + pattern * 0.82)
                        * (0.72 + face.exposure() * 0.28),
                0.0,
                1.5
        );
        double faceStrength = GeologyNoise.clamp(
                baseStrength
                        * face.exposure()
                        * (0.52 + pattern * 0.48),
                0.0,
                1.5
        );

        double fractureBottomY = originalRockTopY - Math.max(
                4.0,
                fracture.designDepth()
                        * (1.0 + fracture.intersectionStrength() * 0.25)
        );

        return new Column(
                true,
                worldSeed,
                worldX,
                worldZ,
                originalRockTopY,
                fissureRockTopY,
                face,
                provinceStrength,
                topStrength,
                faceStrength,
                fractureStrength,
                fractureBottomY,
                maximumRetreat,
                coarseScale,
                detailScale,
                erosion,
                surface,
                fracture
        );
    }

    public record Column(
            boolean active,
            long worldSeed,
            double worldX,
            double worldZ,
            int originalRockTopY,
            int fissureRockTopY,
            RockFaceExposure.Sample face,
            double provinceStrength,
            double topStrength,
            double faceErosionStrength,
            double fractureStrength,
            double fractureBottomY,
            int maximumRetreat,
            double coarseScale,
            double detailScale,
            ArrakisTerrainSettings.ErosionSettings erosion,
            ArrakisTerrainSettings.SurfaceErosionSettings settings,
            MassifFractureField.Sample fracture
    ) {
        static Column inactive(
                long worldSeed,
                double worldX,
                double worldZ,
                int originalRockTopY,
                int fissureRockTopY,
                ArrakisTerrainSettings.ErosionSettings erosion,
                ArrakisTerrainSettings.SurfaceErosionSettings settings
        ) {
            return new Column(
                    false,
                    worldSeed,
                    worldX,
                    worldZ,
                    originalRockTopY,
                    fissureRockTopY,
                    RockFaceExposure.Sample.NONE,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    originalRockTopY,
                    0,
                    Math.max(6.0, settings.scale()),
                    Math.max(2.0, settings.detailScale()),
                    erosion,
                    settings,
                    MassifFractureField.NONE
            );
        }

        public boolean occupies(int worldY, LithologyField.Sample lithology) {
            if (worldY > fissureRockTopY) {
                return false;
            }
            int protectedTopY = settings.baseAnchoredErosion()
                    ? MacroGeologyField.BASE_SURFACE_Y
                    : MacroGeologyField.BASE_SURFACE_Y + 2;
            if (!active || worldY <= protectedTopY) {
                return true;
            }

            double retreatMultiplier = EscarpmentErosionField.retreatMultiplierAtY(
                    lithology.resistance(),
                    erosion,
                    worldY
            );
            double lithologyRelief = GeologyNoise.clamp(
                    1.0 + (
                            retreatMultiplier - 1.0
                    ) * GeologyNoise.clamp(
                            settings.lithologyReliefStrength(),
                            0.0,
                            1.5
                    ),
                    0.20,
                    2.0
            );

            int depthBelowSurface = fissureRockTopY - worldY;
            if (depthBelowSurface >= 0 && depthBelowSurface <= maximumRetreat + 1) {
                double topRetreat = maximumRetreat
                        * topStrength
                        * lithologyRelief;
                if (depthBelowSurface < topRetreat) {
                    return false;
                }
            }

            // Widen fissure walls rather than deepening their existing floor. This preserves
            // the 0.5.13 fissure depth clamp while allowing soft units to weather into broader
            // slots and very hard units to stay narrow.
            if (fractureStrength > 0.0
                    && fracture.activation() > 0.0
                    && Double.isFinite(fracture.distance())
                    && fracture.distance() > fracture.halfWidth()
                    && worldY >= fractureBottomY - 2.0) {
                double verticalPattern = 0.5 + 0.5 * GeologyNoise.value3(
                        worldSeed ^ FRACTURE_SALT,
                        worldX / Math.max(6.0, coarseScale * 0.75),
                        worldY / Math.max(5.0, coarseScale * 0.62),
                        worldZ / Math.max(6.0, coarseScale * 0.75)
                );
                double extraWidth = maximumRetreat
                        * fractureStrength
                        * lithologyRelief
                        * (0.42 + verticalPattern * 0.58);
                if (fracture.distance() <= fracture.halfWidth() + extraWidth) {
                    return false;
                }
            }

            // Recede the real height-derived face through its complete exposed vertical
            // interval. faceInset is a bounded near-probe estimate of how far this column lies
            // behind the physical edge; comparing it with a coherent recession field prevents
            // erosion from tunnelling arbitrarily into solid interior rock.
            double faceFloorY = settings.baseAnchoredErosion()
                    ? MacroGeologyField.BASE_SURFACE_Y
                    : face.lowY();
            if (faceErosionStrength > 0.01
                    && face.highSide()
                    && worldY > faceFloorY
                    && worldY <= Math.min(fissureRockTopY, face.highY())) {
                double verticalPattern = 0.5 + 0.5 * GeologyNoise.value3(
                        worldSeed ^ VERTICAL_SALT,
                        worldX / coarseScale,
                        worldY / Math.max(5.0, coarseScale * 0.82),
                        worldZ / coarseScale
                );
                double fullFaceErosionY = settings.baseAnchoredErosion()
                        ? MacroGeologyField.BASE_SURFACE_Y + 2.0
                        : faceFloorY + Math.max(2.0, maximumRetreat);
                double lowerFaceGate = GeologyNoise.smoothStep(
                        faceFloorY,
                        fullFaceErosionY,
                        worldY
                );
                double recessionResponse = GeologyNoise.smoothStep(
                        0.0,
                        0.55,
                        faceErosionStrength
                );
                if (settings.baseAnchoredErosion()) {
                    // Match the major escarpment pass at the toe. The boost ends well below
                    // the upper face, retaining the 0.5.14.8 massif silhouette.
                    double basalFaceBoost = 1.0 - GeologyNoise.smoothStep(
                            MacroGeologyField.BASE_SURFACE_Y + 3.0,
                            MacroGeologyField.BASE_SURFACE_Y + 19.0,
                            worldY
                    );
                    recessionResponse = Math.max(recessionResponse, basalFaceBoost);
                }
                double recession = maximumRetreat
                        * recessionResponse
                        * lithologyRelief
                        * lowerFaceGate
                        * (0.52 + verticalPattern * 0.48);
                recession = Math.min(maximumRetreat, recession);
                double boundaryDetail = GeologyNoise.value3(
                        worldSeed ^ DETAIL_SALT,
                        worldX / detailScale,
                        worldY / Math.max(6.0, coarseScale * 0.90),
                        worldZ / detailScale
                ) * 0.42;
                if (face.faceInset() + boundaryDetail < recession) {
                    return false;
                }
            }

            return true;
        }

        public int highestRockY(
                LithologyField.Column lithology,
                MassifFractureField.Sample fracture,
                EscarpmentErosionField.Column escarpment
        ) {
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
                if (escarpment.occupies(y, material) && occupies(y, material)) {
                    return y;
                }
            }
            return MacroGeologyField.BASE_SURFACE_Y;
        }
    }
}
