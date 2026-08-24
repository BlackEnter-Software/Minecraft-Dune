package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

import java.util.function.IntPredicate;

/**
 * Authoritative post-erosion, post-orphan Shield-Wall foot classification.
 *
 * <p>The chunk generator supplies analytical filtered occupancy. Keeping the pure scan and
 * cutoff here lets every generation entry point share the same decision without loading or
 * inspecting generated chunks.</p>
 */
public final class FinalCliffFootField {
    public static final int PROFILE_VERSION = 51412;
    private static final int FIRST_NATIVE_Y = MacroGeologyField.BASE_SURFACE_Y + 1;

    private FinalCliffFootField() {
    }

    public static boolean enabled(int profileVersion) {
        return profileVersion >= PROFILE_VERSION;
    }

    public static int resolveFinalPreTalusRockTopY(
            int profileVersion,
            int candidateRockTopY,
            double signedContactDistance,
            ArrakisTerrainSettings.BaseAlignmentSettings alignment,
            IntPredicate filteredRockOccupancy
    ) {
        int filteredTopY = MacroGeologyField.BASE_SURFACE_Y;
        for (int y = candidateRockTopY; y >= FIRST_NATIVE_Y; y--) {
            if (filteredRockOccupancy.test(y)) {
                filteredTopY = y;
                break;
            }
        }

        if (!enabled(profileVersion)) {
            return filteredTopY;
        }

        double resolvedHeight = MacroGeologyField.hardCliffFootHeight(
                filteredTopY - MacroGeologyField.BASE_SURFACE_Y,
                signedContactDistance,
                alignment.minimumCliffFootHeight(),
                alignment.cliffFootCutWidth()
        );
        return resolvedHeight > 0.0
                ? filteredTopY
                : MacroGeologyField.BASE_SURFACE_Y;
    }

    /** Profiles below 51412 retain their exact-Y65 contact test. */
    public static boolean hasBasalContactRock(
            int profileVersion,
            int finalPreTalusRockTopY,
            double minimumCliffFootHeight,
            IntPredicate finalRockOccupancy
    ) {
        if (finalPreTalusRockTopY < FIRST_NATIVE_Y) {
            return false;
        }

        int lastContactY = FIRST_NATIVE_Y;
        if (enabled(profileVersion)) {
            lastContactY = Math.min(
                    finalPreTalusRockTopY,
                    MacroGeologyField.BASE_SURFACE_Y
                            + Math.max(
                                    2,
                                    (int) Math.ceil(Math.max(0.0, minimumCliffFootHeight)) + 2
                            )
            );
        }
        for (int y = FIRST_NATIVE_Y; y <= lastContactY; y++) {
            if (finalRockOccupancy.test(y)) {
                return true;
            }
        }
        return false;
    }
}
