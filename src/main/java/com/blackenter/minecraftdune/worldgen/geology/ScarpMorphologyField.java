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
    private ScarpMorphologyField() {
    }

    public static double massifEnvelope(
            double radius,
            double effectiveRadius,
            double legacyMassifEnvelope,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        if (!massif.scarpMorphologyEnabled()) {
            return GeologyNoise.clamp(legacyMassifEnvelope, 0.0, 1.0);
        }

        double availableInner = Math.max(
                1.0,
                massif.fullRadius() - massif.startRadius()
        );
        double innerWidth = GeologyNoise.clamp(
                massif.innerScarpWidth(),
                4.0,
                availableInner
        );

        // Legacy inner geometry effectively required both true and warped radius to enter the
        // massif. min(radius, effectiveRadius) keeps that conservative behavior while the
        // physical rise itself becomes much narrower than the broad province transition.
        double innerCoordinate = Math.min(radius, effectiveRadius);
        double inner = GeologyNoise.smoothStep(
                massif.startRadius(),
                massif.startRadius() + innerWidth,
                innerCoordinate
        );

        double availableOuter = Math.max(
                1.0,
                massif.outerEndRadius() - massif.outerStartRadius()
        );
        double outerWidth = GeologyNoise.clamp(
                massif.outerScarpWidth(),
                4.0,
                availableOuter
        );
        double outer = 1.0 - GeologyNoise.smoothStep(
                massif.outerStartRadius(),
                massif.outerStartRadius() + outerWidth,
                effectiveRadius
        );

        return GeologyNoise.clamp(inner * outer, 0.0, 1.0);
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
        return massifErosionPermission(
                geology.radiusBlocks(),
                geology.effectiveRadiusBlocks(),
                geology.massifWeight(),
                massif
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

    public static FaultProfile faultProfile(
            double distanceFromCenterline,
            double radialGate,
            ArrakisTerrainSettings.FaultSettings fault,
            boolean scarpMorphologyEnabled
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

        double availableWall = Math.max(
                1.0,
                fault.outerWidth() - fault.coreWidth()
        );
        double wallWidth = GeologyNoise.clamp(
                fault.morphology().wallWidth(),
                3.0,
                availableWall
        );
        double wallOuter = fault.coreWidth() + wallWidth;

        // core_width remains the guaranteed absolute-floor core from 0.5.12. wall_width is
        // the physical canyon wall rather than forcing the complete core->outer influence
        // interval to become a percentage-depth ramp.
        double depthMask = (
                1.0 - GeologyNoise.smoothStep(
                        fault.coreWidth(),
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
                    fault.coreWidth(),
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

    public record FaultProfile(
            double depthMask,
            double shoulderMask
    ) {
        public static final FaultProfile NONE = new FaultProfile(0.0, 0.0);
    }
}
