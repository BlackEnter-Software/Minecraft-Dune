package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Structural precursor for steep Arrakis scarps.
 *
 * <p>Province envelopes remain broad geological/geographical fields. This helper derives a
 * separate physical envelope for the actual Shield Wall and a separate fault cross-section,
 * so hundreds of blocks of province transition no longer have to double as a climbable
 * mountain slope. Erosion still owns detailed recession, lithology relief, undercuts and
 * talus.</p>
 */
public final class ScarpMorphologyField {
    private static final long INNER_SCARP_BROAD_SALT = 0x55AF29C1E6B3847DL;
    private static final long INNER_SCARP_DETAIL_SALT = 0xAC42F1970D63BE25L;
    private static final long OUTER_SCARP_BROAD_SALT = 0x71E35C9AB402DF68L;
    private static final long OUTER_SCARP_DETAIL_SALT = 0xC83D641E5A97B20FL;
    private static final long FAULT_WALL_VARIATION_SALT = 0x394A7D15C6B8E20FL;
    private static final long FAULT_CORE_VARIATION_SALT = 0xB5F13C7E4092AD68L;

    private ScarpMorphologyField() {
    }

    public static double massifEnvelope(
            double radius,
            double effectiveRadius,
            double legacyMassifEnvelope,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        return massifEnvelope(0L, 0.0, 0.0, radius, effectiveRadius,
                legacyMassifEnvelope, massif, false);
    }

    public static double massifEnvelope(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            double effectiveRadius,
            double legacyMassifEnvelope,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        return massifEnvelope(worldSeed, worldX, worldZ, radius, effectiveRadius,
                legacyMassifEnvelope, massif, true);
    }

    private static double massifEnvelope(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            double effectiveRadius,
            double legacyMassifEnvelope,
            ArrakisTerrainSettings.MassifSettings massif,
            boolean applyRoughness
    ) {
        if (!massif.scarpMorphologyEnabled()) {
            return GeologyNoise.clamp(legacyMassifEnvelope, 0.0, 1.0);
        }

        double availableInner = Math.max(1.0, massif.fullRadius() - massif.startRadius());
        double innerWidth = GeologyNoise.clamp(massif.innerScarpWidth(), 4.0, availableInner);
        double innerOffset = applyRoughness
                ? massifBoundaryOffset(worldSeed, worldX, worldZ, massif, true) : 0.0;
        double outerOffset = applyRoughness
                ? massifBoundaryOffset(worldSeed, worldX, worldZ, massif, false) : 0.0;

        double innerCoordinate = Math.min(radius, effectiveRadius);
        double innerStart = massif.startRadius() + innerOffset;
        double inner = GeologyNoise.smoothStep(innerStart, innerStart + innerWidth, innerCoordinate);

        double availableOuter = Math.max(1.0, massif.outerEndRadius() - massif.outerStartRadius());
        double outerWidth = GeologyNoise.clamp(massif.outerScarpWidth(), 4.0, availableOuter);
        double outerStart = massif.outerStartRadius() + outerOffset;
        double outer = 1.0 - GeologyNoise.smoothStep(
                outerStart, outerStart + outerWidth, effectiveRadius
        );

        return GeologyNoise.clamp(inner * outer, 0.0, 1.0);
    }

    static double massifBoundaryOffset(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings.MassifSettings massif,
            boolean inner
    ) {
        double broadStrength = Math.max(0.0, massif.scarpWarpStrength());
        double detailStrength = Math.max(0.0, massif.scarpDetailStrength());
        if (broadStrength <= 0.0 && detailStrength <= 0.0) {
            return 0.0;
        }

        double broadScale = Math.max(16.0, massif.scarpWarpScale());
        double detailScale = Math.max(8.0, massif.scarpDetailScale());
        long broadSalt = inner ? INNER_SCARP_BROAD_SALT : OUTER_SCARP_BROAD_SALT;
        long detailSalt = inner ? INNER_SCARP_DETAIL_SALT : OUTER_SCARP_DETAIL_SALT;

        // Project sampling onto the nominal boundary radius. The lateral offset therefore
        // varies mainly along the scarp instead of changing again while crossing its width.
        double radius = Math.hypot(worldX, worldZ);
        double referenceRadius = inner
                ? massif.startRadius()
                : massif.outerStartRadius();
        double sampleX = worldX;
        double sampleZ = worldZ;
        if (radius > 1.0) {
            sampleX = worldX / radius * referenceRadius;
            sampleZ = worldZ / radius * referenceRadius;
        }

        double broad = broadStrength * GeologyNoise.value2(
                worldSeed ^ broadSalt, sampleX / broadScale, sampleZ / broadScale
        );
        double detail = detailStrength * GeologyNoise.value2(
                worldSeed ^ detailSalt, sampleX / detailScale, sampleZ / detailScale
        );
        double maximum = broadStrength + detailStrength;
        return GeologyNoise.clamp(broad + detail, -maximum, maximum);
    }

    public static LowSideContact nearestMassifLowSideContact(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            double effectiveRadius,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        if (!massif.scarpMorphologyEnabled() || radius < 1.0) {
            return LowSideContact.NONE;
        }

        double radialX = worldX / radius;
        double radialZ = worldZ / radius;

        double availableInner = Math.max(
                1.0,
                massif.fullRadius() - massif.startRadius()
        );
        double innerWidth = GeologyNoise.clamp(
                massif.innerScarpWidth(),
                4.0,
                availableInner
        );
        double innerOffset = massifBoundaryOffset(
                worldSeed,
                worldX,
                worldZ,
                massif,
                true
        );
        double innerCoordinate = Math.min(radius, effectiveRadius);
        double innerEdge = massif.startRadius() + innerOffset;
        double innerSigned = innerCoordinate - innerEdge;

        double availableOuter = Math.max(
                1.0,
                massif.outerEndRadius() - massif.outerStartRadius()
        );
        double outerWidth = GeologyNoise.clamp(
                massif.outerScarpWidth(),
                4.0,
                availableOuter
        );
        double outerOffset = massifBoundaryOffset(
                worldSeed,
                worldX,
                worldZ,
                massif,
                false
        );
        double outerEdge = massif.outerStartRadius()
                + outerOffset
                + outerWidth;
        double outerSigned = outerEdge - effectiveRadius;

        if (Math.abs(innerSigned) <= Math.abs(outerSigned)) {
            return new LowSideContact(
                    true,
                    innerSigned,
                    radialX,
                    radialZ,
                    innerWidth
            );
        }

        return new LowSideContact(
                true,
                outerSigned,
                -radialX,
                -radialZ,
                outerWidth
        );
    }

    /**
     * Permission for Shield-Wall erosion.
     *
     * <p>The broad massif weight remains a geographical/province field. 0.5.14.3 introduced
     * a much narrower physical scarp, so erosion must also be allowed by that physical
     * envelope or the inner wall can finish before the old province ramp becomes strong
     * enough to authorize erosion.</p>
     */
    public static double massifErosionPermission(
            MacroGeologyField.Sample geology,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        return GeologyNoise.clamp(
                Math.max(geology.massifWeight(), geology.physicalMassifWeight()),
                0.0,
                1.0
        );
    }

    static double massifErosionPermission(
            double radius,
            double effectiveRadius,
            double legacyMassifEnvelope,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        double physical = massifEnvelope(
                radius,
                effectiveRadius,
                legacyMassifEnvelope,
                massif
        );
        return GeologyNoise.clamp(
                Math.max(legacyMassifEnvelope, physical),
                0.0,
                1.0
        );
    }

    /**
     * Separates protection of the 0.5.12 absolute fault floor from weathering of the
     * 0.5.14.3 physical fault wall.
     *
     * <p>A full carve mask remains protected. Permission rises quickly once the column leaves
     * the guaranteed floor core, so almost the complete wall can use normal face erosion.</p>
     */
    public static double faultErosionPermission(double faultCarveMask) {
        double carve = GeologyNoise.clamp(faultCarveMask, 0.0, 1.0);
        return 1.0 - GeologyNoise.smoothStep(0.90, 0.995, carve);
    }

    static double faultWallWidth(
            long worldSeed,
            double along,
            int faultIndex,
            ArrakisTerrainSettings.FaultSettings fault
    ) {
        ArrakisTerrainSettings.FaultMorphologySettings morphology = fault.morphology();
        double availableWall = Math.max(1.0, fault.outerWidth() - fault.coreWidth());
        double baseWidth = GeologyNoise.clamp(morphology.wallWidth(), 3.0, availableWall);
        double variation = Math.max(0.0, morphology.wallVariation());
        if (variation <= 0.0) return baseWidth;

        double scale = Math.max(16.0, morphology.wallVariationScale());
        long salt = FAULT_WALL_VARIATION_SALT
                ^ ((long) faultIndex * 0x9E3779B97F4A7C15L);
        double signal = GeologyNoise.value2(
                worldSeed ^ salt, along / scale, faultIndex * 17.375
        );
        return GeologyNoise.clamp(baseWidth + variation * signal, 3.0, availableWall);
    }

    static double faultCoreWidth(
            long worldSeed,
            double along,
            int faultIndex,
            ArrakisTerrainSettings.FaultSettings fault
    ) {
        ArrakisTerrainSettings.FaultMorphologySettings morphology = fault.morphology();
        double variation = Math.max(0.0, morphology.wallVariation());
        if (variation <= 0.0) return fault.coreWidth();

        double scale = Math.max(16.0, morphology.wallVariationScale() * 1.35);
        long salt = FAULT_CORE_VARIATION_SALT
                ^ ((long) faultIndex * 0xD1B54A32D192ED03L);
        double signal = GeologyNoise.value2(
                worldSeed ^ salt, along / scale, faultIndex * 9.625
        );
        double expansionLimit = Math.min(2.0, variation * 0.40);
        double expansion = Math.max(0.0, signal) * expansionLimit;
        return Math.min(
                fault.coreWidth() + expansion,
                Math.max(fault.coreWidth(), fault.outerWidth() - 3.0)
        );
    }

    public static FaultProfile faultProfile(
            double distanceFromCenterline,
            double radialGate,
            ArrakisTerrainSettings.FaultSettings fault,
            boolean scarpMorphologyEnabled
    ) {
        return faultProfile(0L, 0.0, 0, distanceFromCenterline, radialGate,
                fault, scarpMorphologyEnabled, false);
    }

    public static FaultProfile faultProfile(
            long worldSeed,
            double along,
            int faultIndex,
            double distanceFromCenterline,
            double radialGate,
            ArrakisTerrainSettings.FaultSettings fault,
            boolean scarpMorphologyEnabled
    ) {
        return faultProfile(worldSeed, along, faultIndex, distanceFromCenterline,
                radialGate, fault, scarpMorphologyEnabled, true);
    }

    private static FaultProfile faultProfile(
            long worldSeed,
            double along,
            int faultIndex,
            double distanceFromCenterline,
            double radialGate,
            ArrakisTerrainSettings.FaultSettings fault,
            boolean scarpMorphologyEnabled,
            boolean applyVariation
    ) {
        double gate = GeologyNoise.clamp(radialGate, 0.0, 1.0);
        if (gate <= 0.0) {
            return FaultProfile.NONE;
        }

        if (!scarpMorphologyEnabled) {
            double legacy = (
                    1.0 - GeologyNoise.smoothStep(
                            fault.coreWidth(),
                            fault.outerWidth(),
                            distanceFromCenterline
                    )
            ) * gate;
            return new FaultProfile(
                    GeologyNoise.clamp(legacy, 0.0, 1.0),
                    0.0
            );
        }

        double effectiveCoreWidth = applyVariation
                ? faultCoreWidth(worldSeed, along, faultIndex, fault)
                : fault.coreWidth();
        double availableWall = Math.max(1.0, fault.outerWidth() - effectiveCoreWidth);
        double wallWidth = applyVariation
                ? faultWallWidth(worldSeed, along, faultIndex, fault)
                : GeologyNoise.clamp(fault.morphology().wallWidth(), 3.0, availableWall);
        wallWidth = Math.min(wallWidth, availableWall);
        double wallOuter = effectiveCoreWidth + wallWidth;

        // core_width remains the guaranteed minimum absolute-floor core from 0.5.12.
        double depthMask = (
                1.0 - GeologyNoise.smoothStep(
                        effectiveCoreWidth,
                        wallOuter,
                        distanceFromCenterline
                )
        ) * gate;

        // Keep the remainder of outer_width as a weak structural shoulder. MacroGeologyField
        // uses this only for a few blocks of toe lowering, leaving erosion/talus to make the
        // final base morphology.
        double shoulderMask = 0.0;
        if (wallOuter < fault.outerWidth() - 0.01) {
            double outsideWall = GeologyNoise.smoothStep(
                    effectiveCoreWidth,
                    wallOuter,
                    distanceFromCenterline
            );
            double insideInfluence = 1.0 - GeologyNoise.smoothStep(
                    wallOuter,
                    fault.outerWidth(),
                    distanceFromCenterline
            );
            shoulderMask = outsideWall * insideInfluence * gate;
        }

        return new FaultProfile(
                GeologyNoise.clamp(depthMask, 0.0, 1.0),
                GeologyNoise.clamp(shoulderMask, 0.0, 1.0)
        );
    }

    public record LowSideContact(
            boolean valid,
            double signedDistance,
            double inwardX,
            double inwardZ,
            double scarpWidth
    ) {
        public static final LowSideContact NONE = new LowSideContact(
                false,
                Double.POSITIVE_INFINITY,
                0.0,
                0.0,
                1.0
        );
    }

    public record FaultProfile(
            double depthMask,
            double shoulderMask
    ) {
        public static final FaultProfile NONE = new FaultProfile(0.0, 0.0);
    }
}
