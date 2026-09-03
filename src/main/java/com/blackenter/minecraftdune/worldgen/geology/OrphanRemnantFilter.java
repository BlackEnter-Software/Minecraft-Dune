package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Legacy profiles only (through 5148); profile 6000 never invokes this filter.
 * Final support/connectivity cleanup for exposed erosion survivors.
 *
 * <p>The escarpment fields are intentionally removal-only and can leave a narrow resistant
 * column standing on the base after the material between it and the main cliff has eroded.
 * Vertical support alone is not enough for such an exposed remnant. This filter therefore
 * requires a same-Y connection back into the rock body, either directly inward or through a
 * short contiguous lateral path.</p>
 *
 * <p>The lookup supplied by the chunk generator must represent the raw deterministic
 * erosion result, before this filter. That prevents recursive support tests and keeps the
 * result independent of chunk generation order.</p>
 */
public final class OrphanRemnantFilter {
    private OrphanRemnantFilter() {
    }

    public static boolean keeps(
            int worldX,
            int worldY,
            int worldZ,
            EscarpmentErosionField.Column erosion,
            ArrakisTerrainSettings.OrphanRemnantSettings settings,
            RawRockLookup rawRock
    ) {
        return keeps(
                worldX,
                worldY,
                worldZ,
                erosion.candidate(),
                erosion.localRelief(),
                erosion.outwardNormalX(),
                erosion.outwardNormalZ(),
                erosion.settings().surface().baseAnchoredErosion(),
                settings,
                rawRock
        );
    }

    /**
     * Applies orphan cleanup to either major escarpment carving or the ordinary surface pass.
     * Surface-only faces previously bypassed cleanup even though that pass can create the same
     * thin sheets and ribs as the major erosion field.
     */
    public static boolean keeps(
            int worldX,
            int worldY,
            int worldZ,
            EscarpmentErosionField.Column erosion,
            RockSurfaceErosionField.Column surfaceErosion,
            ArrakisTerrainSettings.OrphanRemnantSettings settings,
            RawRockLookup rawRock
    ) {
        RockFaceExposure.Sample face = surfaceErosion.face();
        boolean surfaceCandidate = surfaceErosion.active() && face.exposed();
        boolean majorCandidate = erosion.candidate();
        double outwardX = majorCandidate
                ? erosion.outwardNormalX()
                : face.outwardNormalX();
        double outwardZ = majorCandidate
                ? erosion.outwardNormalZ()
                : face.outwardNormalZ();
        return keeps(
                worldX,
                worldY,
                worldZ,
                majorCandidate || surfaceCandidate,
                Math.max(erosion.localRelief(), face.localRelief()),
                outwardX,
                outwardZ,
                surfaceErosion.settings().baseAnchoredErosion(),
                settings,
                rawRock
        );
    }

    static boolean keeps(
            int worldX,
            int worldY,
            int worldZ,
            boolean erosionCandidate,
            double localRelief,
            double outwardNormalX,
            double outwardNormalZ,
            ArrakisTerrainSettings.OrphanRemnantSettings settings,
            RawRockLookup rawRock
    ) {
        return keeps(worldX, worldY, worldZ, erosionCandidate, localRelief,
                outwardNormalX, outwardNormalZ, false, settings, rawRock);
    }

    public static int protectedThroughY(boolean baseAnchored,
            ArrakisTerrainSettings.OrphanRemnantSettings settings) {
        return MacroGeologyField.BASE_SURFACE_Y
                + (baseAnchored ? 0 : Math.max(0, settings.minimumHeightAboveBase()));
    }

    static boolean keeps(
            int worldX, int worldY, int worldZ, boolean erosionCandidate,
            double localRelief, double outwardNormalX, double outwardNormalZ,
            boolean baseAnchored,
            ArrakisTerrainSettings.OrphanRemnantSettings settings,
            RawRockLookup rawRock
    ) {
        if (!settings.enabled()
                || !erosionCandidate
                || worldY <= protectedThroughY(baseAnchored, settings)
                || localRelief < Math.max(0.0, settings.minimumFaceRelief())) {
            return true;
        }

        double absX = Math.abs(outwardNormalX);
        double absZ = Math.abs(outwardNormalZ);
        if (absX < 0.05 && absZ < 0.05) {
            return true;
        }

        int inwardX;
        int inwardZ;
        int lateralX;
        int lateralZ;
        if (absX >= absZ) {
            inwardX = outwardNormalX >= 0.0 ? -1 : 1;
            inwardZ = 0;
            lateralX = 0;
            lateralZ = 1;
        } else {
            inwardX = 0;
            inwardZ = outwardNormalZ >= 0.0 ? -1 : 1;
            lateralX = 1;
            lateralZ = 0;
        }

        int inwardDepth = Math.max(1, Math.min(16, settings.inwardSupportDepth()));
        if (hasInwardChain(
                worldX,
                worldY,
                worldZ,
                inwardX,
                inwardZ,
                inwardDepth,
                rawRock
        )) {
            return true;
        }

        int lateralRadius = Math.max(0, Math.min(4, settings.lateralSearchRadius()));
        for (int sign = -1; sign <= 1; sign += 2) {
            for (int offset = 1; offset <= lateralRadius; offset++) {
                int lateralWorldX = worldX + lateralX * offset * sign;
                int lateralWorldZ = worldZ + lateralZ * offset * sign;

                // The lateral route itself must remain solid. We never jump across an air gap
                // to rescue a detached island.
                if (!rawRock.occupied(lateralWorldX, worldY, lateralWorldZ)) {
                    break;
                }

                if (hasInwardChain(
                        lateralWorldX,
                        worldY,
                        lateralWorldZ,
                        inwardX,
                        inwardZ,
                        inwardDepth,
                        rawRock
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasInwardChain(
            int worldX,
            int worldY,
            int worldZ,
            int inwardX,
            int inwardZ,
            int depth,
            RawRockLookup rawRock
    ) {
        for (int step = 1; step <= depth; step++) {
            if (!rawRock.occupied(
                    worldX + inwardX * step,
                    worldY,
                    worldZ + inwardZ * step
            )) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    public interface RawRockLookup {
        boolean occupied(int worldX, int worldY, int worldZ);
    }
}
