package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.*;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Build-blocking profile-6000 invariants. No running level, cave state or neighbor chunks. */
public final class BuriedRockTerrainValidation {
    private static final int[][] POINTS = {{0,0},{400,200},{-1000,-200},{1499,0},{2200,0},
            {3057,150},{3059,150},{3060,150},{3050,190},{3100,150},{3400,0},
            {4098,0},{4096,0},{4095,0},{657,3306},{2553,1706},{3001,464},
            {3053,190},{3067,106},{3089,173},{-2960,-589},{-17,4103},{-16,4102},
            {6600,1},{9000,9000},{30000000,-30000000}};

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        var profile = ArrakisProfileValidation.loadDevelopmentProfile();
        var settings = profile.settings();
        validateCodec(profile.json(), settings);
        validateGeology(settings);
        validateStructuralLithology(settings);
        validateFaults(settings);
        validateExposureAndTalus(settings);
        validateOrderAndComposition(settings);
        validateIsolation(settings);
        var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
        for (int[] p : new int[][] {{0,0},{3057,150},{3060,150},{3100,150},{3400,0},{4096,0},{9000,9000}}) {
            var c = evaluator.buriedColumn(p[0], p[1]);
            System.out.printf(Locale.ROOT, "Seed0 X/Z=%d/%d regional=%.2f uplift=%.2f R0=%.2f S=%.2f Re=%.2f removed=%.2f H=%.2f talus=%s%n",
                    p[0],p[1],c.raw().regionalRockTop(),c.raw().shieldWallUplift(),c.raw().rockTop(),
                    c.sediment().surfaceY(),c.erosion().rockTop(),c.erosion().removedAmount(),c.finalSurface(),c.talus().active());
        }
        System.out.printf(Locale.ROOT, "Buried-rock validation passed in %.2fs.%n", (System.nanoTime()-start)/1e9);
    }

    private static void validateCodec(JsonObject json, ArrakisTerrainSettings settings) {
        require(settings.isBuriedRock() && settings.profileVersion() == 6000, "development preset must select 6000");
        require(!json.has("front_shell_cleanup") && !json.has("erosion") && !json.has("base_alignment")
                && !json.getAsJsonObject("lithology").has("talus"), "active preset carries obsolete repair groups");
        var encoded = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(settings), "6000 round-trip");
        var missing = json.deepCopy(); missing.remove("buried_rock");
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, missing).getOrThrow().buriedRock().equals(BuriedRockSettings.DEFAULT),
                "optional buried-rock defaults");
        for (String key : new String[] {"scale", "amplitude", "minimum_y"}) {
            var invalid = json.deepCopy();
            invalid.getAsJsonObject("buried_rock").getAsJsonObject("rock_surface").addProperty(key, -1000);
            require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "invalid rock setting accepted: " + key);
        }
        var invalid = json.deepCopy();
        invalid.getAsJsonObject("buried_rock").getAsJsonObject("fault_displacement").addProperty("influence_width", 16);
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "inverted fault dimensions accepted");
        invalid = json.deepCopy(); invalid.addProperty("profile_version", 6001);
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "unknown profile silently reinterpreted");
        invalid = json.deepCopy(); var repair = new JsonObject(); repair.addProperty("enabled", true);
        invalid.add("front_shell_cleanup", repair);
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "6000 accepted shell repair");
    }

    private static void validateGeology(ArrakisTerrainSettings settings) {
        var e = new ArrakisTerrainEvaluator(0, settings, 1024);
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        for (int[] p : new int[][] {{0,0},{400,200},{-1000,-200},{1200,0}}) {
            var c = e.buriedColumn(p[0],p[1]);
            require(c.raw().shieldWallUplift() == 0 && c.raw().rockTop() < c.sediment().surfaceY(), "basin must bury ordinary geology");
            require(c.sediment().surfaceY() == 64 && c.highestOccupiedY() == 64, "basin sediment grading inherited rock relief");
            require(c.erosion().removedAmount() == 0 && !c.erosion().face().exposed(), "buried rock eroded from hypothetical cave air");
            require(!c.talus().active(), "level basin acquired cliff talus");
            low = Math.min(low,c.raw().rockTop()); high = Math.max(high,c.raw().rockTop());
        }
        require(high-low > 1, "basin geology flattened with sediment");
        var wall = e.buriedColumn(3400,0);
        require(wall.raw().shieldWallUplift() > 80 && wall.rockTopY() > 120, "Shield Wall lost recognizable uplift");
        require(wall.erosion().rockTop() >= wall.sediment().surfaceY()
                && wall.cellAt(wall.rockTopY(),-64).kind() == BuriedTerrainColumn.Kind.ROCK, "uplift not naturally exposed");
        var raisedJson = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE,settings).getOrThrow().getAsJsonObject();
        var rockJson = new JsonObject(); var buriedJson = new JsonObject();
        buriedJson.add("rock_surface", rockJson); raisedJson.add("buried_rock", buriedJson);
        raisedJson.getAsJsonObject("buried_rock").getAsJsonObject("rock_surface").addProperty("regional_y", 50);
        var raised = ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,raisedJson).getOrThrow();
        var changed = new ArrakisTerrainEvaluator(0,raised,64).buriedColumn(400,200);
        require(changed.raw().rockTop() != e.buriedColumn(400,200).raw().rockTop()
                && changed.sediment().equals(e.buriedColumn(400,200).sediment()), "sediment is a thickness atop rock");
    }

    private static void validateStructuralLithology(ArrakisTerrainSettings settings) {
        var fixed = LithologyField.column(0,3100.5,150.5,settings.lithology(),settings.additionalMaterials());
        for (double displacement : new double[] {-24,32,140}) {
            var shifted = LithologyField.column(0,3100.5,150.5,settings.lithology(),settings.additionalMaterials(),displacement);
            for (int y=-60;y<200;y++) require(fixed.sample(y).equals(shifted.sample(y+displacement)), "strata did not follow structural displacement");
        }
    }

    private static void validateFaults(ArrakisTerrainSettings settings) {
        boolean positive=false,negative=false,damage=false;
        for(int i=0;i<720;i++) {
            double angle=i*Math.PI/360, x=Math.cos(angle)*3500, z=Math.sin(angle)*3500;
            var a=GeologicalFaultField.sample(0,x,z,settings);
            require(a.equals(GeologicalFaultField.sample(0,x,z,settings)), "fault determinism");
            var b=GeologicalFaultField.sample(0,x+.001,z,settings);
            require(Math.abs(a.displacement()-b.displacement()) < .05, "fault discontinuity");
            positive |= a.displacement()>1; negative |= a.displacement() < -1; damage |= a.damage()>.3;
        }
        require(positive && negative && damage,"faults need signed throw and damage, not datum trenches");
        for(int x : new int[] {-4097,-4096,-17,-16,-1,0,15,16,4095,4096}) {
            var a=GeologicalFaultField.sample(0,x+.5,-3000.5,settings);
            var b=GeologicalFaultField.sample(0,x+.500001,-3000.5,settings);
            require(Math.abs(a.displacement()-b.displacement())<.001,"fault chunk/negative-coordinate seam");
        }
    }

    private static void validateExposureAndTalus(ArrakisTerrainSettings settings) {
        require(!RockFaceExposure.external(0,0,40,64,4,18,18,(x,z)->64).exposed(), "buried exposure");
        var inner=RockFaceExposure.external(0,0,140,64,4,18,18,(x,z)->Math.max(64,140+x*4));
        var outer=RockFaceExposure.external(0,0,140,64,4,18,18,(x,z)->Math.max(64,140-x*4));
        require(inner.exposed() && outer.exposed() && inner.outwardNormalX()<0 && outer.outwardNormalX()>0,"analytical face orientation");
        var material=LithologyField.Material.STONE;
        var t=settings.buriedRock().talus();
        require(!TalusColluviumField.sample(0,0,0,64,t,(x,z)->new TalusColluviumField.Source(160,0,-1,0,material)).active(), "talus without erosion supply");
        require(!TalusColluviumField.sample(0,0,0,64,t,(x,z)->new TalusColluviumField.Source(64,30,-1,0,material)).active(), "talus without relief");
        require(TalusColluviumField.sample(0,0,0,64,t,(x,z)->new TalusColluviumField.Source(160,30,-1,0,material)).active(), "strong eroded cliff unable to supply talus");
    }

    private static void validateOrderAndComposition(ArrakisTerrainSettings settings) {
        for(long seed : new long[] {0,-5640511200611798902L}) {
            var reference=new ArrakisTerrainEvaluator(seed,settings,1024);
            for(int[] p:POINTS) {
                var c=reference.buriedColumn(p[0],p[1]);
                long expected=fingerprint(c);
                require(c.erosion().rockTop()<=c.raw().rockTop() && c.rockTopY()>=-48, "erosion added rock or breached geological floor");
                require(c.erosion().majorRemoval()>=0 && c.erosion().surfaceRemoval()>=0, "negative erosion amount");
                for(int y=-63;y<=c.rockTopY();y++) require(c.cellAt(y,-64).kind()==BuriedTerrainColumn.Kind.ROCK,"gap in continuous geological body");
                BuriedTerrainColumn.Cell[] written=new BuriedTerrainColumn.Cell[384];
                c.compose(-64,320,(y,cell)->written[y+64]=cell);
                // Exercise the real Minecraft NoiseColumn allocation/writer without booting
                // mod registries. Null is a test palette sentinel, never used by production.
                int[] paletteCalls={0};
                var baseColumn=c.toNoiseColumn(-64,320,cell->{paletteCalls[0]++; return null;});
                baseColumn.setBlock(319,null);
                require(paletteCalls[0]==384,"base column truncated above the legacy substrate");
                int highest=-64;
                for(int y=-64;y<320;y++) {
                    require(written[y+64].equals(c.cellAt(y,-64)),"writer/base-column composition mismatch");
                    if(written[y+64].kind()!=BuriedTerrainColumn.Kind.AIR) highest=y;
                    if(y<=c.highestOccupiedY()) require(written[y+64].kind()!=BuriedTerrainColumn.Kind.AIR,"unsupported sediment/deposit");
                }
                require(highest==reference.highestOccupiedY(p[0],p[1]),"height query disagrees with written column");
                require(c.baseHeight(-64,320,cell->cell.kind()!=BuriedTerrainColumn.Kind.AIR)==highest+1,"surface-height predicate mismatch");
                int fullHeight=c.baseHeight(-64,320,cell->cell.kind()!=BuriedTerrainColumn.Kind.AIR && cell.kind()!=BuriedTerrainColumn.Kind.SAND_LAYER);
                require(fullHeight==highest+1-(c.cellAt(highest,-64).kind()==BuriedTerrainColumn.Kind.SAND_LAYER?1:0),"heightmap layer predicate ignored");
                require(written[0].kind()==BuriedTerrainColumn.Kind.BEDROCK,"bottom bedrock replaced");
                for(int limit:new int[] {0,1,64}) {
                    var limited=new ArrakisTerrainEvaluator(seed,settings,limit);
                    limited.buriedColumn(100,200);
                    require(expected==fingerprint(limited.buriedColumn(p[0],p[1])),"cache/query-order altered composition");
                    require(limited.size()<=limit,"cache exceeded bound");
                }
            }
        }
        // Adjacent complete 16x16 tiles, produced in opposing tile/column orders.
        var a=new ArrakisTerrainEvaluator(0,settings,1024);
        var b=new ArrakisTerrainEvaluator(0,settings,64);
        long[] hash=new long[512];
        for(int i=0;i<512;i++) hash[i]=fingerprint(a.buriedColumn(3040+i%32,144+i/32));
        for(int i=511;i>=0;i--) require(hash[i]==fingerprint(b.buriedColumn(3040+i%32,144+i/32)),"adjacent-chunk order seam");
    }

    private static long fingerprint(BuriedTerrainColumn c) {
        long h=0xcbf29ce484222325L;
        for(double v:new double[] {c.raw().rockTop(),c.raw().structuralDisplacement(),c.sediment().surfaceY(),c.erosion().rockTop(),c.finalSurface()}) h=(h^Double.doubleToLongBits(v))*0x100000001b3L;
        for(int y=-64;y<320;y++) {
            var cell=c.cellAt(y,-64);
            h=(h^cell.kind().ordinal())*0x100000001b3L;
            h=(h^(cell.material()==null?-1:cell.material().ordinal()))*0x100000001b3L;
            h=(h^cell.layers())*0x100000001b3L;
        }
        return h;
    }

    private static void validateIsolation(ArrakisTerrainSettings settings) throws Exception {
        var e=new ArrakisTerrainEvaluator(0,settings,64);
        boolean rejected=false;
        try {e.column(3057,150);} catch(IllegalStateException expected) {rejected=true;}
        require(rejected,"6000 can enter legacy repair evaluator");
        var legacy=ArrakisProfileValidation.loadProfile().settings();
        require(!legacy.isBuriedRock() && legacy.profileVersion()==5148,"saved 5148 migrated silently");
        for(String file:new String[] {"arrakis/BuriedRockTerrain.java","arrakis/BuriedTerrainColumn.java","geology/RockErosionField.java","geology/RawRockSurfaceField.java","geology/TalusColluviumField.java"}) {
            String code=Files.readString(Path.of("src/main/java/com/blackenter/minecraftdune/worldgen",file));
            for(String forbidden:new String[] {"OrphanRemnantFilter","BoundedBasalComponentCleanup","ShieldWallFrontShellCleanup","BasalSandSkirt","getBlockState(","ServerLevel"})
                require(!code.contains(forbidden),"new geological DAG depends on repair/world state: "+file+" "+forbidden);
        }
        String generator=Files.readString(Path.of("src/main/java/com/blackenter/minecraftdune/worldgen/arrakis/ArrakisChunkGenerator.java"));
        require(generator.contains("evaluation.buriedColumn(x, z).toNoiseColumn(minimumY, maximumY + 1, this::buriedState)")
                && generator.contains("writeBuriedColumn(evaluation.buriedColumn(worldX, worldZ)")
                && generator.contains("terrain.compose(minimumY, maximumY + 1"),"production writers diverged from shared composer");
        String report=ArrakisTerrainCommand.describe(e,0,settings,3057,70,150);
        require(report.contains("profile=6000") && report.contains("Legacy repair stack: bypassed") && report.contains("R0="),"6000 inspection invoked legacy diagnostics");
    }

    private static void require(boolean condition,String message) {if(!condition) throw new AssertionError(message);}
}
