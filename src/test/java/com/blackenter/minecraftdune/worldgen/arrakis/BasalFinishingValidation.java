package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.BasalSandSkirt;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField.Material;
import com.blackenter.minecraftdune.worldgen.geology.BoundedBasalComponentCleanup;
import com.mojang.serialization.JsonOps;
import java.util.function.IntBinaryOperator;

/** Fixed semantic regressions; exploratory screenshot searches never gate the build. */
public final class BasalFinishingValidation {
    private BasalFinishingValidation() {}

    public static ArrakisTerrainSettings withoutFinishing(ArrakisTerrainSettings settings) {
        settings = withoutFaultFinishing(settings);
        var json = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        json.getAsJsonObject("erosion").getAsJsonObject("orphan_remnants").remove("basal_component_cleanup_enabled");
        json.getAsJsonObject("lithology").getAsJsonObject("talus").remove("basal_sand_skirt_enabled");
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    public static ArrakisTerrainSettings withoutFaultFinishing(ArrakisTerrainSettings settings) {
        var json = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        json.getAsJsonObject("erosion").getAsJsonObject("orphan_remnants").remove("fault_edge_cleanup_enabled");
        json.getAsJsonObject("lithology").getAsJsonObject("talus").remove("ravine_contact_enabled");
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    public static void validate(ArrakisTerrainSettings settings) {
        validateComponents();
        validateSkirtShape();
        require(settings.erosion().orphanRemnants().basalComponentCleanupEnabled()
                && settings.lithology().talus().basalSandSkirtEnabled(), "development preset did not opt in");
        var previous = withoutFinishing(settings);
        require(!previous.erosion().orphanRemnants().basalComponentCleanupEnabled()
                && !previous.lithology().talus().basalSandSkirtEnabled(), "missing flags changed saved profiles");
        var retired = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, previous).getOrThrow().getAsJsonObject();
        retired.getAsJsonObject("erosion").getAsJsonObject("orphan_remnants").addProperty("micro_trim_enabled", true);
        retired.getAsJsonObject("lithology").getAsJsonObject("talus").addProperty("sand_concealment_enabled", true);
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, retired).getOrThrow().equals(previous),
                "retired keys silently enable replacement behavior");
        validatePipeline(settings, previous);
        System.out.println("Basal finishing passed: bounded components including tall pillars, skirt, ownership, fault and order.");
    }

    private static BoundedBasalComponentCleanup.RockLookup fixture(IntBinaryOperator heights) {
        return new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x, int z) { return heights.applyAsInt(x, z); }
            public boolean occupied(int x, int y, int z) { return y >= 65 && y <= topY(x, z); }
            public boolean cleanupAllowed(int x, int z) { return true; }
        };
    }

    private static void validateComponents() {
        // A continuous Y65 floor must not rescue otherwise isolated posts.
        for (int height : new int[] {2, 3, 6, 10, 40, 150}) {
            var pillar = fixture((x, z) -> x == 0 && z == 0 ? 64 + height : 65);
            var c = BoundedBasalComponentCleanup.sample(0, 0, pillar);
            require(c.removed() && c.componentColumns() == 1 && c.removesY(64 + height)
                    && c.removesY(65) && !c.removesY(64), "isolated pillar retained because of height");
        }
        var pair = fixture((x, z) -> x >= 0 && x <= 1 && z == 0 ? 105 : 65);
        for (int x = 0; x <= 1; x++) {
            var c = BoundedBasalComponentCleanup.sample(x, 0, pair);
            require(c.removed() && c.componentColumns() == 2, "two-column component retained");
        }
        var square = fixture((x, z) -> x >= 0 && x <= 1 && z >= 0 && z <= 1 ? 150 : 65);
        var forward = new BoundedBasalComponentCleanup.Sample[4];
        for (int i = 0; i < 4; i++) {
            forward[i] = BoundedBasalComponentCleanup.sample(i % 2, i / 2, square);
            require(forward[i].removed() && forward[i].componentColumns() == 4, "closed 2x2 component retained");
        }
        for (int i = 3; i >= 0; i--) require(forward[i].equals(
                BoundedBasalComponentCleanup.sample(i % 2, i / 2, square)), "component depends on query order");
        var buttress = fixture((x, z) -> Math.abs(x) <= 1 && Math.abs(z) <= 1 ? 160 : 65);
        require(!BoundedBasalComponentCleanup.sample(0, 0, buttress).removed(), "broad buttress removed");
        var rib = fixture((x, z) -> x >= 2 || z == 0 && x >= 0 ? 160 : 65);
        require(!BoundedBasalComponentCleanup.sample(0, 0, rib).removed(), "inward-connected rib removed");
        var edge = fixture((x, z) -> x >= 0 && x <= 3 && z == 0 ? 120 : 65);
        for (int x = 0; x <= 3; x++) {
            var c = BoundedBasalComponentCleanup.sample(x, 0, edge);
            require(!c.removed() && c.reachesSearchBoundary(), "search-edge component partially removed");
        }
        var highBridge = new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x, int z) { return x >= 2 || x >= 0 && z == 0 ? 190 : 65; }
            public boolean occupied(int x, int y, int z) {
                return y == 65 || y <= 190 && (x >= 2 || x == 0 && z == 0 || x == 1 && z == 0 && y == 190);
            }
            public boolean cleanupAllowed(int x, int z) { return true; }
        };
        require(!BoundedBasalComponentCleanup.sample(0, 0, highBridge).removed(), "high wall connection was missed");
        var fault = new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x, int z) { return pair.topY(x, z); }
            public boolean occupied(int x, int y, int z) { return pair.occupied(x, y, z); }
            public boolean cleanupAllowed(int x, int z) { return x != 1; }
        };
        require(!BoundedBasalComponentCleanup.sample(1, 0, fault).removed()
                && !BoundedBasalComponentCleanup.sample(0, 0, fault).removed(), "fault structure/attachment removed");
        var bounded = new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x, int z) {
                require(Math.abs(x) <= 3 && Math.abs(z) <= 3, "component query escaped radius three");
                return edge.topY(x, z);
            }
            public boolean occupied(int x, int y, int z) { return y <= topY(x, z); }
            public boolean cleanupAllowed(int x, int z) { return true; }
        };
        require(!BoundedBasalComponentCleanup.sample(0, 0, bounded).removed(), "bounded fixture unexpectedly removed");
    }

    private static void validateSkirtShape() {
        for (int signed : new int[] {0, 1, 2, 3, 4}) {
            var skirt = BasalSandSkirt.shape(true, signed, false);
            require(skirt.active() && skirt.depth() == 4 && skirt.bottomY() == 61, "contact/inward burial missing");
            require(skirt.materialAt(64) == Material.SAND && skirt.materialAt(65) == Material.NONE, "unapproved mantle");
        }
        require(!BasalSandSkirt.shape(true, 5, false).active(), "skirt exceeds inward bound");
        require(BasalSandSkirt.shape(true, -12, false).depth() == 2, "midpoint did not taper");
        require(BasalSandSkirt.shape(true, -23, false).depth() == 1, "outer blend missing");
        require(!BasalSandSkirt.shape(true, -24, false).active() && !BasalSandSkirt.shape(true, -25, false).active(),
                "skirt exceeds outward bound");
        var visible = BasalSandSkirt.shape(true, -13, true);
        require(visible.visibleY65Mantle() && visible.materialAt(65) == Material.SAND
                && visible.materialAt(66) == Material.NONE && visible.materialAt(60) == Material.NONE,
                "approved mantle escaped one layer");
        require(!BasalSandSkirt.shape(false, 0, true).active(), "suppressed skirt active");
    }

    private static void validatePipeline(ArrakisTerrainSettings settings, ArrakisTerrainSettings previous) {
        int[][] points = {{3001,464},{2988,464},{2991,464},{2987,464},{2989,464},{2977,464},{3005,464},
                {3043,200},{3050,254},{2963,615},{3042,199},{3050,190},{4086,0},{3005,442},{3005,443}};
        for (long seed : new long[] {0, -5640511200611798902L}) {
            var current = new ArrakisTerrainEvaluator(seed, settings, 1024);
            var old = new ArrakisTerrainEvaluator(seed, previous, 1024);
            for (int[] p : points) {
                int x = p[0], z = p[1];
                var c = current.column(x, z); var b = old.column(x, z); var r = c.rock();
                require(r.rockTopY() == b.rockTopY() && r.originalRockTopY() == b.originalRockTopY()
                        && r.fissureRockTopY() == b.fissureRockTopY() && r.duneSurfaceUnits() == b.duneSurfaceUnits()
                        && r.geology().equals(b.geology()) && r.face().equals(b.face())
                        && r.fracture().equals(b.fracture()), "replacement changed upstream terrain fields");
                var component = current.componentCleanup(x, z);
                if (component.removed()) require(component.componentColumns() <= 4
                        && !component.reachesSupport() && !component.reachesSearchBoundary()
                        && c.localTalusThickness() == 0, "invalid component removal/floating local scree");
                for (int y = 45; y <= r.originalRockTopY(); y++) {
                    var material = r.materialSampleAt(y);
                    require(material.equals(b.materialSampleAt(y)), "lithology/hardness changed");
                    boolean before = old.orphanFilteredRockOccupies(x, z, y, b.rock(), material);
                    boolean after = current.filteredRockOccupies(x, z, y, r, material);
                    require(!after || before, "component cleanup added rock");
                    if (before != after) require(y >= 65 && component.removesY(y), "removal escaped component");
                    var deposit = current.basalMaterialAt(x, y, z, c);
                    if (current.realCliffRock(x, y, z, c)) require(deposit == Material.NONE, "deposit replaced real cliff rock");
                    if (!current.realCliffRock(x, y, z, c) && c.basalTalusApron().materialAt(y) == Material.GRAVEL
                            && !c.talusOccupiesY(y)) require(deposit == Material.GRAVEL, "skirt replaced visible gravel");
                    if (c.skirt().materialAt(y) != Material.NONE) require(y >= 61 && y <= 65, "skirt escaped basal interval");
                }
                if (c.skirt().visibleY65Mantle()) require(current.residualBasalY65(x, z) && current.rockOccupies(x, 65, z)
                        && !current.realCliffRock(x, 65, z, c), "Y65 mantle added height/covered valid cliff");
                if (c.geology().faultCarveMask() > 0.85) require(!component.removed(), "protected fault core component removed");
                if (c.geology().faultCarveMask() > 0.85 || c.geology().sandCorridorMask() > 0.25)
                    require(!c.skirt().active(), "skirt entered protected corridor");
            }
            for (int cap : new int[] {0, 1, 64}) {
                var reversed = new ArrakisTerrainEvaluator(seed, settings, cap);
                reversed.preTalusColumn(-321, 654);
                for (int i = points.length - 1; i >= 0; i--) {
                    int x = points[i][0], z = points[i][1];
                    require(current.componentCleanup(x, z).equals(reversed.componentCleanup(x, z))
                            && current.column(x, z).basal().equals(reversed.column(x, z).basal())
                            && current.column(x, z).skirt().equals(reversed.column(x, z).skirt()),
                            "component/contact/skirt depends on cache or query order");
                }
            }
        }
        var e = new ArrakisTerrainEvaluator(0, settings, 1024);
        // Discovered once by the optional survey; the build only checks these fixed cells.
        var preComponents = new ArrakisTerrainEvaluator(0, previous, 1024);
        for (int z : new int[] {442, 443}) {
            var removed = e.componentCleanup(3005, z);
            require(preComponents.highestFilteredRockY(3005, z) == 81 && removed.removed()
                    && removed.componentColumns() == 2 && e.highestFilteredRockY(3005, z) == 64,
                    "fixed tall two-column orphan was not removed");
            for (int y = 65; y <= 81; y++) require(!e.rockOccupies(3005, y, z), "partial component survived");
        }
        var contact = e.column(3001, 464);
        require(contact.basal().actual().found() && contact.skirt().depth() == 4, "actual contact skirt missing");
        require(e.basalMaterialAt(3001, 70, 464, contact) == Material.GRAVEL
                && e.basalMaterialAt(3001, 64, 464, contact) == Material.SAND, "gravel/underlay order wrong");
        var inward = e.column(3005, 464);
        require(inward.basal().actual().signedDistance() == 4 && inward.skirt().depth() == 4
                && e.basalMaterialAt(3005, 64, 464, inward) == Material.SAND
                && e.realCliffRock(3005, 65, 464, inward) && e.basalMaterialAt(3005, 65, 464, inward) == Material.NONE,
                "inward skirt missing or real cliff replaced");
        require(e.column(2989, 464).skirt().depth() == 2 && !e.column(2977, 464).skirt().active(), "actual outward taper wrong");
        var rim = e.column(2988, 464);
        System.out.printf("Fixed rim 2988/464: original=%d final=%d owner=%s skirt=%s.%n",
                rim.originalRockTopY(), e.highestFilteredRockY(2988, 464), e.preSkirtOwner(2988, 65, 464), rim.skirt());
        require(rim.originalRockTopY() == 65 && !rim.skirt().visibleY65Mantle()
                && e.basalMaterialAt(2988, 65, 464, rim) == Material.NONE,
                "native low foreland rock misclassified as erosion residue");
        var residue = e.column(2991, 464);
        require(residue.originalRockTopY() > 65 && residue.skirt().visibleY65Mantle()
                && e.basalMaterialAt(2991, 65, 464, residue) == Material.SAND,
                "confirmed erosion floor did not receive safe mantle");
        require(!e.column(2987, 464).skirt().visibleY65Mantle(), "mantle added height over empty Y65 desert");
        var core = e.column(3042, 199);
        require(core.geology().faultCarveMask() > 0.999 && core.originalRockTopY() == 64
                && e.highestOccupiedY(3042, 199) == 64 && !core.skirt().active(), "full fault core no longer open at Y64");
        var report = ArrakisTerrainCommand.describe(e, 0, settings, 2991, 65, 464);
        require(report.contains("Component cleanup:") && report.contains("Sand skirt:")
                && report.contains("visible-Y65-mantle=true") && report.contains("BASAL_EROSION_RESIDUE"),
                "inspector disagrees with shared ownership/skirt results");
        for (boolean blockedPath : new boolean[] {false, true}) {
            var forbidden = new BasalTalusApronField.RockLookup() {
                public boolean footPresent(int x, int z) { return x >= 3002; }
                public int topY(int x, int z) { return 180; }
                public boolean allowed(int x, int z) { return !blockedPath; }
                public boolean sourceAllowed(int x, int z) { return false; }
            };
            var excluded = BasalTalusApronField.evaluate(0, 3001, 464, contact.geology(), settings, forbidden);
            require(!BasalSandSkirt.sample(true, excluded, true, false).active(), "skirt bypassed source/path exclusion");
        }
        require(!BasalSandSkirt.sample(true, contact.basal(), true, true).visibleY65Mantle(), "mantle overrides gravel");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
