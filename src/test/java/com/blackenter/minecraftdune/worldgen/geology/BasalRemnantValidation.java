package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/** Explicit connectivity fixtures, independent of incidental terrain populations. */
public final class BasalRemnantValidation {
    public static void validate(ArrakisTerrainSettings settings) {
        var orphan = settings.erosion().orphanRemnants();
        require(orphan.inwardSupportDepth() == 8, "support depth was retuned");
        require(OrphanRemnantFilter.protectedThroughY(false, orphan) == 69, "legacy floor changed");
        require(OrphanRemnantFilter.protectedThroughY(true, orphan) == 64, "anchored floor is not Y64");
        for (int y = 65; y <= 69; y++) {
            require(keeps(y, false, orphan, (x, h, z) -> x == 0 && z == 0), "legacy low remnant changed");
            require(!keeps(y, true, orphan, (x, h, z) -> x == 0 && z == 0), "unsupported low remnant kept");
            require(keeps(y, true, orphan, (x, h, z) -> x <= 0 && x >= -8 && z == 0),
                    "supported basal buttress removed");
            require(keeps(y, true, orphan, (x, h, z) -> (x == 0 && z == 0)
                    || (z == 1 && x <= 0 && x >= -8)), "connected lateral basal route removed");
            require(!keeps(y, true, orphan, (x, h, z) -> (x == 0 && z == 0)
                    || (z == 2 && x <= 0 && x >= -8)), "basal route jumped air");
        }
        for (int y : new int[] {64, 70, 100, 200}) {
            for (boolean attached : new boolean[] {false, true}) {
                OrphanRemnantFilter.RawRockLookup rock = (x, h, z) -> z == 0 && (x == 0 || attached && x >= -8 && x < 0);
                require(keeps(y, false, orphan, rock) == keeps(y, true, orphan, rock),
                        "anchoring changed foundation/high-wall behavior");
            }
        }
    }

    private static boolean keeps(int y, boolean anchored,
            ArrakisTerrainSettings.OrphanRemnantSettings settings, OrphanRemnantFilter.RawRockLookup rock) {
        return OrphanRemnantFilter.keeps(0, y, 0, true, 80, 1, 0, anchored, settings, rock);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
