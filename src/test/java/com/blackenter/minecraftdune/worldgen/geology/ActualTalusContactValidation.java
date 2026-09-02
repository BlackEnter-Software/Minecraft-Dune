package com.blackenter.minecraftdune.worldgen.geology;

/** Pure final-rock fixtures: no Minecraft blocks or neighboring generated chunks. */
public final class ActualTalusContactValidation {
    public static void validate() {
        for (int[] direction : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int dx = direction[0], dz = direction[1];
            var structural = new ScarpMorphologyField.LowSideContact(true, -70, dx, dz, 80);
            int[] calls = new int[3];
            BasalTalusApronField.RockLookup wall = new BasalTalusApronField.RockLookup() {
                public boolean footPresent(int x, int z) { calls[0]++; return x * dx + z * dz >= 40; }
                public int topY(int x, int z) { calls[1]++; return x * dx + z * dz >= 52 ? 150 : 78; }
                public boolean allowed(int x, int z) { calls[2]++; return true; }
            };
            var adjacent = BasalTalusApronField.findContact(39 * dx, 39 * dz, structural, 4, wall);
            require(adjacent.found() && adjacent.outwardDistance() == 0, "adjacent cell has artificial gap");
            require(adjacent.x() == 40 * dx && adjacent.z() == 40 * dz, "contact is not surviving rock");
            require(adjacent.rockTopY() == 78 && adjacent.wallTopY() == 150 && adjacent.wallRelief() == 86,
                    "shallow toe did not inherit connected final wall relief");
            require(calls[0] <= 26 && calls[1] <= 25 && calls[2] <= 25, "wall query is unbounded");
            var shifted = BasalTalusApronField.findContact(39 * dx, 39 * dz,
                    new ScarpMorphologyField.LowSideContact(true, 17, dx, dz, 80), 4, wall);
            require(adjacent.equals(shifted), "structural distance controlled final placement");
            var distant = BasalTalusApronField.findContact(34 * dx, 34 * dz, structural, 4, wall);
            require(distant.outwardDistance() == 5, "discrete outward distance is off by one");
            var inside = BasalTalusApronField.findContact(40 * dx, 40 * dz, structural, 4, wall);
            require(inside.found() && inside.x() == adjacent.x() && inside.z() == adjacent.z(),
                    "inside/outside queries disagree on contact");
            var far = BasalTalusApronField.findContact(0, 0, structural, 4, wall);
            require(!far.found() && far.searchedBlocks() == 32, "search escaped its radius");
        }
        var structural = new ScarpMorphologyField.LowSideContact(true, 0, 1, 0, 80);
        var fault = new BasalTalusApronField.RockLookup() {
            public boolean footPresent(int x, int z) { return x <= -5 || x >= 5; }
            public int topY(int x, int z) { return 180; }
            public boolean allowed(int x, int z) { return x < -2 || x > 2; }
        };
        require(!BasalTalusApronField.findContact(-3, 0, structural, 4, fault).found(),
                "contact search crossed opposing fault core");
        var gap = new BasalTalusApronField.RockLookup() {
            public boolean footPresent(int x, int z) { return x >= 0 && x != 3; }
            public int topY(int x, int z) { return x >= 4 ? 240 : 74; }
            public boolean allowed(int x, int z) { return true; }
        };
        var shortToe = BasalTalusApronField.findContact(-1, 0, structural, 4, gap);
        require(shortToe.wallTopY() == 74 && shortToe.wallProbeBlocks() == 3,
                "relief probe jumped disconnected rock");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
