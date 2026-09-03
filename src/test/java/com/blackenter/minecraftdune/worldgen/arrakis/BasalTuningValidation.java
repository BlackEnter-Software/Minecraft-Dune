package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.BasalTuningShapeValidation;
import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField.Material;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;

/** Active basal-depth/organic-apron invariants alongside exact saved-profile regressions. */
public final class BasalTuningValidation {
    private BasalTuningValidation() {}
    static final int[][] POINTS = {{3001,464},{2991,464},{2988,464},{3005,464},{3043,200},
            {3050,190},{3053,190},{3042,199},{3050,254},{2963,615},{4086,0},{3005,442},{3005,443},
            {3199,200},{3200,200},{3200,202},{3200,203},{3204,125},{3200,190},{3200,180},
            {3067,106},{3089,173},{0,0},{6600,1}};

    /** Reconstruct exactly the serialized development profile at 47874b9. */
    public static ArrakisTerrainSettings previousSettings(ArrakisTerrainSettings settings) {
        var json = encode(settings);
        json.getAsJsonObject("erosion").getAsJsonObject("surface").remove("basal_erosion_depth");
        var orphan = json.getAsJsonObject("erosion").getAsJsonObject("orphan_remnants");
        orphan.remove("component_search_radius");
        orphan.addProperty("inward_support_depth",8);
        var talus = json.getAsJsonObject("lithology").getAsJsonObject("talus");
        talus.remove("organic_apron_enabled");
        talus.addProperty("basal_apron_max_height",6);
        talus.addProperty("basal_apron_spread",12);
        talus.addProperty("basal_apron_sand_start",.62);
        return decode(json);
    }

    public static void validate(ArrakisTerrainSettings settings) {
        validateCodec(settings);
        BasalTuningShapeValidation.validate(settings);
        int erodedBasalVoxels = 0;
        for (long seed : new long[] {0,-5640511200611798902L,7640891576956012809L}) {
            var e = new ArrakisTerrainEvaluator(seed,settings,1024);
            var old = new ArrakisTerrainEvaluator(seed,previousSettings(settings),1024);
            for (int[] p : POINTS) {
                int x = p[0], z = p[1];
                var now = e.column(x,z); var before = old.column(x,z);
                require(now.geology().equals(before.geology()) && now.face().equals(before.face())
                        && now.fracture().equals(before.fracture()) && now.duneSurfaceUnits() == before.duneSurfaceUnits()
                        && now.originalRockTopY() == before.originalRockTopY()
                        && now.fissureRockTopY() == before.fissureRockTopY(), "upstream morphology changed");
                for (int y = 54; y <= now.originalRockTopY(); y++) {
                    var m = now.materialSampleAt(y);
                    require(m.equals(before.materialSampleAt(y)), "lithology changed");
                    boolean raw = now.erosion().occupies(y,m) && now.surfaceErosion().occupies(y,m);
                    boolean priorRaw = before.erosion().occupies(y,m) && before.surfaceErosion().occupies(y,m);
                    require(!raw || priorRaw, "lower erosion floor added native rock");
                    if (y <= 60 || y >= 66) require(raw == priorRaw, "raw erosion changed outside Y61..65");
                    else if (!raw && priorRaw) erodedBasalVoxels++;
                    if (e.realCliffRock(x,y,z,now)) require(e.basalMaterialAt(x,y,z,now) == Material.NONE,
                            "deposits replaced valid final wall rock");
                    if (!e.realCliffRock(x,y,z,now) && !now.talusOccupiesY(y)
                            && now.basalTalusApron().materialAt(y) == Material.GRAVEL)
                        require(e.basalMaterialAt(x,y,z,now) == Material.GRAVEL, "skirt covered gravel");
                }
                require(e.talusWallQueryMinY() == 71 && e.talusWallQueryMaxY() == 76,
                        "shrinking deposit changed wall contact detection band");
                require(now.basalTalusApron().height() <= 4 && now.skirt().depth() <= 4
                        && now.skirt().outwardReach() >= 16 && now.skirt().outwardReach() <= 20,
                        "active deposit dimensions escaped bounds");
                if (now.geology().faultCarveMask() > .85 || now.geology().sandCorridorMask() > .25) {
                    require(!e.componentCleanup(x,z).removed() && !now.basalTalusApron().active()
                            && !now.skirt().active(), "protected fault core/corridor changed");
                }
            }
            for (int capacity : new int[] {0,1,64}) {
                var reverse = new ArrakisTerrainEvaluator(seed,settings,capacity);
                for (int i = POINTS.length-1; i >= 0; i--) {
                    int x = POINTS[i][0], z = POINTS[i][1];
                    require(e.column(x,z).skirt().equals(reverse.column(x,z).skirt())
                            && e.column(x,z).basal().equals(reverse.column(x,z).basal())
                            && e.componentCleanup(x,z).equals(reverse.componentCleanup(x,z))
                            && ArrakisTerrainEvaluatorValidation.fingerprint(e,x,z)
                                == ArrakisTerrainEvaluatorValidation.fingerprint(reverse,x,z), "tuning depends on cache/query order");
                    for (int y = 60; y <= 64; y++) require(e.nativeFoundationOccupies(x,y,z)
                            == reverse.nativeFoundationOccupies(x,y,z), "native roots depend on cache/query order");
                }
            }
        }
        require(erodedBasalVoxels > 0, "tuning only hides rock instead of eroding it");
        validateRavineAndDatum(settings);
        System.out.printf("Basal tuning passed: %d raw basal voxels removed in fixed probes; upper raw walls unchanged.%n",erodedBasalVoxels);
    }

    private static void validateRavineAndDatum(ArrakisTerrainSettings settings) {
        var e = new ArrakisTerrainEvaluator(0,settings,1024);
        for (int z = 180; z <= 190; z++) {
            var c = e.column(3200,z);
            require(c.geology().faultCarveMask() > .999 && c.originalRockTopY() == 64
                    && e.highestOccupiedY(3200,z) == 64 && !c.skirt().active()
                    && !c.basalTalusApron().active(), "Y64 sandy ravine core obstructed");
        }
        require(e.highestOccupiedY(0,0) == 64, "desert datum changed");
        var old = new ArrakisTerrainEvaluator(0,previousSettings(settings),1024);
        for (int[] p : new int[][] {{3001,464},{2991,464},{3053,190},{3050,254},{3200,200}}) {
            require(old.rockOccupies(p[0],65,p[1]) && !e.rockOccupies(p[0],65,p[1])
                    && old.nativeFoundationOccupies(p[0],64,p[1]) && !e.nativeFoundationOccupies(p[0],64,p[1]),
                    "diagnosed basal shelf was concealed rather than removed before deposits");
        }
        require(e.rockOccupies(3050,70,190) && e.componentCleanup(3050,190).reachesSupport(),
                "protected connected toe removed");
        for (int[] p : new int[][] {{3200,200},{3204,125}}) {
            var c = e.column(p[0],p[1]);
            require(c.basal().source().equals("ravine") && c.basal().actual().found()
                    && c.basalTalusApron().active(), "working ravine contact disappeared");
            int top = c.basalTalusApron().topY();
            require(e.basalMaterialAt(p[0],top,p[1],c) == Material.GRAVEL
                    && e.basalMaterialAt(p[0],64,p[1],c) == Material.SAND,
                    "ravine gravel and buried sand composition wrong");
        }
        String report = ArrakisTerrainCommand.describe(e,0,settings,3001,64,464);
        require(report.contains("floor=Y60 depth=4") && report.contains("search-radius=5")
                && report.contains("organic-talus=true"), "inspector omitted tuning parameters");
    }

    private static void validateCodec(ArrakisTerrainSettings settings) {
        require(settings.profileVersion() == 5148 && settings.erosion().surface().erosionFloorY() == 60
                && settings.erosion().orphanRemnants().inwardSupportDepth() == 12
                && settings.erosion().orphanRemnants().componentSearchRadius() == 5
                && settings.lithology().talus().organicApronEnabled(), "active preset tuning missing");
        require(decode(encode(settings)).equals(settings), "tuning failed to serialize");
        var previous = previousSettings(settings);
        require(previous.erosion().surface().erosionFloorY() == 64
                && !previous.lithology().talus().organicApronEnabled()
                && previous.erosion().orphanRemnants().componentSearchRadius() == 3, "saved-profile defaults changed");
        for (int depth : new int[] {0,2,4,10}) {
            var json = encode(settings);
            json.getAsJsonObject("erosion").getAsJsonObject("surface").addProperty("basal_erosion_depth",depth);
            require(decode(json).erosion().surface().erosionFloorY() == 64-depth, "depth setting mapped incorrectly");
            json.getAsJsonObject("erosion").getAsJsonObject("surface").addProperty("base_anchored_erosion",false);
            require(decode(json).erosion().surface().erosionFloorY() == 64, "depth leaked into legacy non-anchored profile");
        }
        for (int invalid : new int[] {-1,11}) {
            var json = encode(settings);
            json.getAsJsonObject("erosion").getAsJsonObject("surface").addProperty("basal_erosion_depth",invalid);
            require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,json).error().isPresent(), "invalid burial depth accepted");
        }
        for (int invalid : new int[] {2,6}) {
            var json = encode(settings);
            json.getAsJsonObject("erosion").getAsJsonObject("orphan_remnants").addProperty("component_search_radius",invalid);
            require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,json).error().isPresent(), "unbounded component radius accepted");
        }
    }

    private static JsonObject encode(ArrakisTerrainSettings settings) {
        return ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE,settings).getOrThrow().getAsJsonObject();
    }
    private static ArrakisTerrainSettings decode(JsonObject json) {
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
    }
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
}
