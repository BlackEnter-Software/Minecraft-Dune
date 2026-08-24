package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Hard Shield-Wall contact introduced by terrain profile 51413.
 *
 * <p>The older low-side contact is the zero-height edge of the smooth structural scarp.
 * That is useful for describing the whole transition, but it is the wrong place to terminate
 * an escarpment: the transition itself becomes a broad rooted rock skirt. This field moves
 * the authoritative cliff face to the high-rock edge of that structural transition.</p>
 *
 * <p>Signed distance is positive into the massif and negative toward the desert. The returned
 * scarp width is the complete low-side transition width which profile 51413 may remove as a
 * structural ramp. Fault corridors remain handled separately by the fault system.</p>
 */
public final class HardCliffContactField {
    public static final int PROFILE_VERSION = 51413;

    private HardCliffContactField() {
    }

    public static boolean enabled(int profileVersion) {
        return profileVersion >= PROFILE_VERSION;
    }

    public static ScarpMorphologyField.LowSideContact contact(
            long worldSeed,
            double worldX,
            double worldZ,
            double radius,
            double effectiveRadius,
            ArrakisTerrainSettings.MassifSettings massif
    ) {
        if (!massif.scarpMorphologyEnabled() || radius < 1.0) {
            return ScarpMorphologyField.LowSideContact.NONE;
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
        double innerOffset = ScarpMorphologyField.massifBoundaryOffset(
                worldSeed,
                worldX,
                worldZ,
                massif,
                true
        );
        double innerCoordinate = Math.min(radius, effectiveRadius);

        // Inner rock grows from startRadius toward startRadius + innerWidth.
        // The hard cliff is the HIGH-rock end, not the zero-height low-side edge.
        double innerFace = massif.startRadius() + innerOffset + innerWidth;
        double innerSigned = innerCoordinate - innerFace;

        double availableOuter = Math.max(
                1.0,
                massif.outerEndRadius() - massif.outerStartRadius()
        );
        double outerWidth = GeologyNoise.clamp(
                massif.outerScarpWidth(),
                4.0,
                availableOuter
        );
        double outerOffset = ScarpMorphologyField.massifBoundaryOffset(
                worldSeed,
                worldX,
                worldZ,
                massif,
                false
        );

        // Outer rock is fully developed at outerStart and fades outward over outerWidth.
        // The hard cliff therefore sits at the HIGH-rock start of that fade.
        double outerFace = massif.outerStartRadius() + outerOffset;
        double outerSigned = outerFace - effectiveRadius;

        if (Math.abs(innerSigned) <= Math.abs(outerSigned)) {
            return new ScarpMorphologyField.LowSideContact(
                    true,
                    innerSigned,
                    radialX,
                    radialZ,
                    innerWidth
            );
        }

        return new ScarpMorphologyField.LowSideContact(
                true,
                outerSigned,
                -radialX,
                -radialZ,
                outerWidth
        );
    }

    /**
     * True only inside the structural ramp on the desert side of the new hard face.
     *
     * <p>This deliberately does not cull arbitrary low formations farther into the basin or
     * broken-rock desert. At the inner wall the removable interval is one inner-scarp width;
     * at the outer wall it is one outer-scarp width.</p>
     */
    public static boolean cullsLowSideRamp(
            double signedDistance,
            double scarpWidth
    ) {
        if (scarpWidth <= 0.0) {
            return false;
        }
        double width = Math.max(1.0, scarpWidth);
        return signedDistance < 0.0
                && signedDistance >= -width;
    }
}
