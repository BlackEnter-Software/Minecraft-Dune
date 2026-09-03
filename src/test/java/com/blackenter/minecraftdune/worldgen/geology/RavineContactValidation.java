package com.blackenter.minecraftdune.worldgen.geology;

import java.util.function.IntBinaryOperator;

/** Fixed local-wall fixtures; no coordinate discovery or world writes. */
public final class RavineContactValidation {
    private RavineContactValidation() {}

    private static BasalTalusApronField.RockLookup wall(IntBinaryOperator heights) {
        return new BasalTalusApronField.RockLookup() {
            public boolean footPresent(int x, int z) { return heights.applyAsInt(x, z) >= 71; }
            public int topY(int x, int z) { return heights.applyAsInt(x, z); }
            public boolean allowed(int x, int z) { return true; }
            public boolean ravineSourceAllowed(int x, int z) { return true; }
        };
    }

    public static void validate() {
        for (int direction = 0; direction < 4; direction++) {
            final int dx = direction == 0 ? 1 : direction == 1 ? -1 : 0;
            final int dz = direction == 2 ? 1 : direction == 3 ? -1 : 0;
            var rock = wall((x, z) -> x * dx + z * dz >= 3 ? 180 : 65);
            var contact = BasalTalusApronField.findRavineContact(0, 0, 4, rock);
            require(contact.found() && contact.x() == dx * 3 && contact.z() == dz * 3
                    && contact.signedDistance() == -2 && contact.wallTopY() == 180,
                    "ravine contact depends on cardinal orientation");
            var adjacent = BasalTalusApronField.findRavineContact(dx * 2, dz * 2, 4, rock);
            require(adjacent.signedDistance() == 0 && adjacent.x() == contact.x() && adjacent.z() == contact.z(),
                    "distal source not reproduced at adjacent cell");
            var inward = BasalTalusApronField.findRavineContact(dx * 6, dz * 6, 4, rock);
            require(inward.signedDistance() == 4, "inward overlap lost");
            require(!BasalTalusApronField.findRavineContact(dx * 8, dz * 8, 4, rock).found(),
                    "inside search escaped inset bound");
        }
        var limit = wall((x, z) -> x >= 32 ? 180 : 64);
        require(BasalTalusApronField.findRavineContact(0, 0, 4, limit).found(), "32-block search endpoint excluded");
        require(!BasalTalusApronField.findRavineContact(0, 0, 4,
                wall((x, z) -> x >= 33 ? 180 : 64)).found(), "search exceeded 32 blocks");
        var tie = BasalTalusApronField.findRavineContact(0, 0, 4,
                wall((x, z) -> Math.abs(x) >= 3 ? 180 : 64));
        require(tie.x() == -3 && tie.signedDistance() == -2, "equal-distance tie is not canonical");
        var nearest = BasalTalusApronField.findRavineContact(0, 0, 4,
                wall((x, z) -> x <= -4 || z >= 2 ? 180 : 64));
        require(nearest.x() == 0 && nearest.z() == 2, "farther wall won nearest-foot selection");

        // A low disconnected tooth cannot borrow wall relief across an air gap.
        require(!BasalTalusApronField.findRavineContact(0, 0, 4,
                wall((x, z) -> x == 3 ? 74 : x >= 5 ? 200 : 64)).found(), "air gap pooled wall relief");
        var blocked = new BasalTalusApronField.RockLookup() {
            public boolean allowed(int x, int z) { return x != 2; }
            public boolean footPresent(int x, int z) {
                require(x < 2, "contact searched across protected core");
                return false;
            }
            public int topY(int x, int z) { throw new AssertionError("opposing wall was sampled"); }
            public boolean ravineSourceAllowed(int x, int z) { return true; }
        };
        require(!BasalTalusApronField.findRavineContact(0, 0, 4, blocked).found(), "contact bridged core");
        require(!BasalTalusApronField.findRavineContact(2, 0, 4, blocked).found(), "core query not suppressed");
        var blockedRelief = new BasalTalusApronField.RockLookup() {
            public boolean allowed(int x, int z) { return x != 4; }
            public boolean footPresent(int x, int z) { return x >= 3; }
            public int topY(int x, int z) {
                require(x == 3, "relief probe crossed protected core");
                return 74;
            }
            public boolean ravineSourceAllowed(int x, int z) { return true; }
        };
        require(!BasalTalusApronField.findRavineContact(0, 0, 4, blockedRelief).found(), "relief borrowed across core");
        var excludedSource = new BasalTalusApronField.RockLookup() {
            public boolean allowed(int x, int z) { return true; }
            public boolean footPresent(int x, int z) { return x >= 3; }
            public int topY(int x, int z) { return 180; }
        };
        require(!BasalTalusApronField.findRavineContact(0, 0, 4, excludedSource).found(), "unqualified source admitted");
        var bounded = new BasalTalusApronField.RockLookup() {
            public boolean allowed(int x, int z) {
                require(Math.abs(x) <= 56 && Math.abs(z) <= 56 && (x == 0 || z == 0),
                        "local ray/probe escaped its bound");
                return true;
            }
            public boolean footPresent(int x, int z) { return x >= 32; }
            public int topY(int x, int z) { require(x <= 56, "wall probe escaped 24 blocks"); return 180; }
            public boolean ravineSourceAllowed(int x, int z) { return true; }
        };
        require(BasalTalusApronField.findRavineContact(0, 0, 4, bounded).wallProbeBlocks() == 24,
                "wall probe length changed");
        System.out.println("Ravine raster contact passed: four orientations, nearest/ties, inset, bounds, source and core/gap barriers.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
