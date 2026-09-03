package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField.Material;
import com.blackenter.minecraftdune.worldgen.geology.RavineContactValidation;
import com.mojang.serialization.JsonOps;

/** Fixed in-game diagnoses, upstream preservation, and cross-chunk material determinism. */
public final class RavineFinishingValidation {
    private RavineFinishingValidation() {}

    public static void validate(ArrakisTerrainSettings settings) {
        RavineContactValidation.validate();
        var previous = BasalFinishingValidation.withoutFaultFinishing(settings);
        require(settings.lithology().talus().ravineContactEnabled()
                && settings.erosion().orphanRemnants().faultEdgeCleanupEnabled(), "new preset flags missing");
        require(!previous.lithology().talus().ravineContactEnabled()
                && !previous.erosion().orphanRemnants().faultEdgeCleanupEnabled(), "missing flags changed saved profiles");
        require(settings.equals(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,
                ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()).getOrThrow()),
                "new opt-ins did not round trip");
        var current = new ArrakisTerrainEvaluator(0, settings, 1024);
        var old = new ArrakisTerrainEvaluator(0, previous, 1024);
        require(old.column(3200, 200).basal().actual().reason().equals("outside-scarp-search-band"),
                "saved pre-ravine profile no longer reproduces diagnosed gap");
        var c = current.column(3200, 200);
        var contact = c.basal().actual();
        require(c.basal().source().equals("ravine") && contact.found() && contact.x() == 3200 && contact.z() == 203
                && contact.signedDistance() == -2 && contact.wallRelief() == 147,
                "fixed ravine edge failed to find final wall");
        require(c.basalTalusApron().height() == 6 && current.basalMaterialAt(3200, 70, 200, c) == Material.GRAVEL
                && current.basalMaterialAt(3200, 64, 200, c) == Material.SAND && c.skirt().depth() == 4,
                "ravine gravel/sand underlay composition wrong");
        var adjacent = current.column(3200, 202).basal().actual();
        require(adjacent.found() && adjacent.signedDistance() == 0 && adjacent.x() == contact.x()
                && adjacent.z() == contact.z(), "ravine talus detached from its source wall");
        var north = current.column(3204,125);
        require(north.basal().source().equals("ravine") && north.basal().actual().x() == 3204
                && north.basal().actual().z() == 124 && north.basal().actual().signedDistance() == 0
                && current.basalMaterialAt(3204,70,125,north) == Material.GRAVEL,
                "opposite ravine wall failed to receive attached gravel");
        var tooth = current.componentCleanup(3050,190);
        require(!old.componentCleanup(3050,190).candidate() && tooth.candidate() && !tooth.removed()
                && tooth.reachesSupport() && current.rockOccupies(3050,70,190),
                "fault shoulder not classified or protected toe connection severed");

        // Working inner/outer massif contacts, and a distal original skirt, remain exact.
        for (int[] p : new int[][] {{3001,464},{2991,464},{2988,464},{3005,464},{4086,0}}) {
            require(old.column(p[0],p[1]).basal().equals(current.column(p[0],p[1]).basal())
                    && old.column(p[0],p[1]).skirt().equals(current.column(p[0],p[1]).skirt()),
                    "fallback changed an already-working massif contact/skirt");
        }
        int[][] points = {{3050,190},{3049,190},{3050,191},{3053,190},{3042,199},
                {3199,200},{3200,200},{3201,200},{3200,202},{3200,203},{3200,206},{3200,190},{3200,180},{3204,125}};
        for (long seed : new long[] {0,-5640511200611798902L}) {
            var e = new ArrakisTerrainEvaluator(seed, settings, 1024);
            var legacy = new ArrakisTerrainEvaluator(seed, previous, 1024);
            for (int[] p : points) {
                int x = p[0], z = p[1];
                var now = e.column(x,z); var before = legacy.column(x,z);
                require(now.originalRockTopY() == before.originalRockTopY() && now.rockTopY() == before.rockTopY()
                        && now.fissureRockTopY() == before.fissureRockTopY() && now.geology().equals(before.geology())
                        && now.face().equals(before.face()) && now.fracture().equals(before.fracture())
                        && now.duneSurfaceUnits() == before.duneSurfaceUnits(), "fault finishing altered upstream morphology");
                for (int y = 61; y <= now.originalRockTopY(); y++) {
                    require(now.materialSampleAt(y).equals(before.materialSampleAt(y))
                            && e.rawRockOccupies(now,y) == legacy.rawRockOccupies(before,y), "raw erosion/lithology changed");
                    boolean afterRock = e.rockOccupies(x,y,z), beforeRock = legacy.rockOccupies(x,y,z);
                    if (afterRock != beforeRock) require(beforeRock && e.componentCleanup(x,z).removesY(y)
                            && e.componentCleanup(x,z).componentColumns() <= 4, "rock changed outside small removal");
                    if (e.realCliffRock(x,y,z,now)) require(e.basalMaterialAt(x,y,z,now) == Material.NONE,
                            "ravine deposit replaced real wall rock");
                }
                if (now.geology().faultCarveMask() > 0.85 || now.geology().sandCorridorMask() > 0.25) {
                    require(!e.componentCleanup(x,z).removed() && !now.basalTalusApron().active()
                            && !now.skirt().active(), "fault core or sand pass changed");
                }
            }
            for (int cap : new int[] {0,1,64}) {
                var reverse = new ArrakisTerrainEvaluator(seed,settings,cap);
                reverse.preTalusColumn(-321,654);
                for (int i = points.length - 1; i >= 0; i--) {
                    int x = points[i][0], z = points[i][1];
                    require(e.componentCleanup(x,z).equals(reverse.componentCleanup(x,z))
                            && e.column(x,z).basal().equals(reverse.column(x,z).basal())
                            && ArrakisTerrainEvaluatorValidation.fingerprint(e,x,z)
                                == ArrakisTerrainEvaluatorValidation.fingerprint(reverse,x,z),
                            "fault finishing depends on query/cache/chunk order");
                }
            }
        }
        // Fixed full core inside the diagnosed ravine: no apron bridge or raised sand.
        for (int z = 180; z <= 190; z++) {
            var core = current.column(3200,z);
            require(core.geology().faultCarveMask() > 0.999 && core.originalRockTopY() == 64
                    && current.highestOccupiedY(3200,z) == 64 && !core.skirt().active(),
                    "ravine full core no longer open at Y64");
        }
        var report = ArrakisTerrainCommand.describe(current,0,settings,3200,70,200);
        require(report.contains("source=ravine") && report.contains("basal-material=GRAVEL")
                && report.contains("fault-edge-enabled=true"), "inspector not using shared ravine result");
        // Deterministic cache-policy regression, not a machine-dependent wall-clock gate.
        // A full cache must retain the current working set instead of pinning early probes.
        var cache = new ArrakisTerrainEvaluator(0,settings,2);
        var first = cache.preTalusColumn(3200,200);
        var second = cache.preTalusColumn(3200,201);
        require(cache.preTalusColumn(3200,200) == first, "hot column not reused");
        cache.preTalusColumn(3200,202);
        require(cache.size() == 2 && cache.preTalusColumn(3200,200) == first
                && cache.preTalusColumn(3200,201) != second, "full cache pins obsolete wall probes");
        System.out.println("Fault finishing passed: ravine gravel, skirt, connected toe retention, legacy opt-ins, cores, raw shape and order.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
