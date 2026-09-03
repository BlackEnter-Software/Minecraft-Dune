package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainEvaluator;
import com.mojang.serialization.JsonOps;

/** Exact synthetic fixtures for the bounded, column-wide Shield-Wall shell peel. */
public final class ShieldWallFrontShellCleanupValidation {
    private static final ArrakisTerrainSettings.FrontShellCleanupSettings ENABLED =
            new ArrakisTerrainSettings.FrontShellCleanupSettings(true, 2, 2);

    private ShieldWallFrontShellCleanupValidation() {
    }

    public static void validate() {
        var inner = halfSpace(true, false, false);
        var first = ShieldWallFrontShellCleanup.sample(0, 0, ENABLED, inner);
        require(first.wall() == ShieldWallFrontShellCleanup.Wall.INNER
                && first.pass1Eligible() && first.pass1Removed(),
                "inner first-shell column survived pass 1");
        require(ShieldWallFrontShellCleanup.sample(1, 0, ENABLED, inner).pass1Removed(),
                "inner second block survived pass 1");

        var second = ShieldWallFrontShellCleanup.sample(2, 0, ENABLED, inner);
        require(!second.pass1Removed() && second.pass2Eligible() && second.pass2Removed(),
                "newly exposed inner shell survived pass 2");
        require(ShieldWallFrontShellCleanup.sample(3, 0, ENABLED, inner).pass2Removed(),
                "second pass did not remove its full two-block depth");

        var tall = ShieldWallFrontShellCleanup.sample(0, 0, ENABLED,
                (x, z) -> inner.sample(x, z).rockPresent()
                        ? column(true, x == 0 ? 96 : 72, true,
                                ShieldWallFrontShellCleanup.Wall.INNER, 1.0, 0.0, false, false)
                        : inner.sample(x, z));
        require(tall.preCleanTopY() == 96 && tall.postCleanTopY() == 64 && tall.removed(),
                "front-shell cleanup shortened instead of removing a >10-block column");

        var wall = ShieldWallFrontShellCleanup.sample(4, 0, ENABLED, inner);
        require(!wall.removed() && wall.postCleanTopY() == wall.preCleanTopY(),
                "main wall behind four-block cleanup depth was removed");
        var buttress = ShieldWallFrontShellCleanup.sample(5, 0, ENABLED,
                (x, z) -> x == 5
                        ? column(true, 88, false, ShieldWallFrontShellCleanup.Wall.INNER,
                                1.0, 0.0, false, false)
                        : inner.sample(x, z));
        require(!buttress.pass1Eligible() && !buttress.removed(),
                "massif buttress outside the structural shell band was removed");

        require(!ShieldWallFrontShellCleanup.sample(0, 0, ENABLED,
                halfSpace(true, true, false)).removed(),
                "fault-owned shell intersection was removed");
        require(!ShieldWallFrontShellCleanup.sample(0, 0, ENABLED,
                halfSpace(true, false, true)).removed(),
                "non-massif formation intersection was removed");

        var outer = halfSpace(false, false, false);
        require(ShieldWallFrontShellCleanup.sample(0, 0, ENABLED, outer).pass1Removed()
                        && ShieldWallFrontShellCleanup.sample(-1, 0, ENABLED, outer).pass1Removed()
                        && ShieldWallFrontShellCleanup.sample(-2, 0, ENABLED, outer).pass2Removed()
                        && ShieldWallFrontShellCleanup.sample(-3, 0, ENABLED, outer).pass2Removed()
                        && !ShieldWallFrontShellCleanup.sample(-4, 0, ENABLED, outer).removed(),
                "outer wall cleanup ran inward or used the inner-wall normal");

        var disabled = ShieldWallFrontShellCleanup.sample(0, 0,
                new ArrakisTerrainSettings.FrontShellCleanupSettings(false, 2, 2), inner);
        require(!disabled.removed() && disabled.preCleanTopY() == disabled.postCleanTopY(),
                "disabled compatibility profile changed rock");

        var expected = new ShieldWallFrontShellCleanup.Sample[9];
        for (int x = -2; x <= 6; x++) {
            expected[x + 2] = ShieldWallFrontShellCleanup.sample(x, 0, ENABLED, inner);
        }
        for (int x = 6; x >= -2; x--) {
            require(expected[x + 2].equals(ShieldWallFrontShellCleanup.sample(x, 0, ENABLED, inner)),
                    "front-shell result depends on query order");
        }
    }

    public static void validateProfile(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(0L, settings, 1024);
        var innerPass1 = evaluator.frontShellCleanup(3057, 150);
        var innerPass2 = evaluator.frontShellCleanup(3059, 150);
        var innerWall = evaluator.frontShellCleanup(3060, 150);
        require(innerPass1.wall() == ShieldWallFrontShellCleanup.Wall.INNER
                        && innerPass1.pass1Removed() && innerPass1.preCleanTopY() == 127
                        && innerPass1.postCleanTopY() == 64,
                "Seed-0 inner tall shell fixture was not removed whole in pass 1");
        require(!innerPass2.pass1Removed() && innerPass2.pass2Removed(),
                "Seed-0 inner newly exposed shell fixture was not removed in pass 2");
        require(!innerWall.removed() && innerWall.preCleanTopY() == 178,
                "Seed-0 main inner wall behind cleanup depth was removed");

        var fault = evaluator.frontShellCleanup(3050, 190);
        require(fault.faultOwned() && !fault.removed(),
                "Seed-0 regional-fault intersection lost ownership protection");
        var outerPass1 = evaluator.frontShellCleanup(4098, 0);
        var outerPass2 = evaluator.frontShellCleanup(4096, 0);
        var outerWall = evaluator.frontShellCleanup(4095, 0);
        require(outerPass1.wall() == ShieldWallFrontShellCleanup.Wall.OUTER
                        && outerPass1.pass1Removed() && outerPass2.pass2Removed()
                        && !outerWall.removed(),
                "Seed-0 outer orientation peeled backwards into the massif");

        var json = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                .getOrThrow().getAsJsonObject();
        json.remove("front_shell_cleanup");
        var disabledSettings = ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        var disabled = new ArrakisTerrainEvaluator(0L, disabledSettings, 1024);
        for (int[] point : new int[][] {{3057,150},{3059,150},{3060,150},{3050,190},{4098,0},{4096,0},{4095,0}}) {
            int x = point[0], z = point[1];
            var activeRock = evaluator.preTalusColumn(x, z);
            var disabledRock = disabled.preTalusColumn(x, z);
            require(activeRock.geology().equals(disabledRock.geology())
                            && activeRock.face().equals(disabledRock.face())
                            && activeRock.fracture().equals(disabledRock.fracture())
                            && activeRock.erosion().equals(disabledRock.erosion())
                            && activeRock.surfaceErosion().equals(disabledRock.surfaceErosion())
                            && activeRock.originalRockTopY() == disabledRock.originalRockTopY()
                            && activeRock.fissureRockTopY() == disabledRock.fissureRockTopY()
                            && activeRock.rockTopY() == disabledRock.rockTopY(),
                    "front-shell option changed macro geology or erosion inputs");
            require(!disabled.frontShellCleanup(x, z).removed(),
                    "disabled profile did not preserve previous occupancy");
            var expected = evaluator.frontShellCleanup(x, z);
            for (int capacity : new int[] {0, 1, 64}) {
                var reordered = new ArrakisTerrainEvaluator(0L, settings, capacity);
                reordered.frontShellCleanup(-x, -z);
                require(expected.equals(reordered.frontShellCleanup(x, z))
                                && evaluator.highestFilteredRockY(x, z)
                                    == reordered.highestFilteredRockY(x, z),
                        "production front-shell result depends on cache/query order");
            }
        }
    }

    private static ShieldWallFrontShellCleanup.ColumnLookup halfSpace(
            boolean inner,
            boolean faultOwned,
            boolean otherOwned
    ) {
        return (x, z) -> {
            boolean rock = inner ? x >= 0 : x <= 0;
            return column(rock, rock ? 78 : 64, true,
                    inner ? ShieldWallFrontShellCleanup.Wall.INNER : ShieldWallFrontShellCleanup.Wall.OUTER,
                    inner ? 1.0 : -1.0, 0.0,
                    rock && x == 0 && faultOwned,
                    rock && x == 0 && otherOwned);
        };
    }

    private static ShieldWallFrontShellCleanup.Column column(
            boolean rock,
            int topY,
            boolean inBand,
            ShieldWallFrontShellCleanup.Wall wall,
            double inwardX,
            double inwardZ,
            boolean faultOwned,
            boolean otherOwned
    ) {
        return new ShieldWallFrontShellCleanup.Column(
                rock, topY, inBand, wall, rock, faultOwned, otherOwned,
                rock ? 1.0 : -1.0, inwardX, inwardZ);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
