package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Statistical dev.2 morphology and source-coherence checks, independent of rendering/LOD. */
public final class BuriedRockMorphologyValidation {
    private BuriedRockMorphologyValidation() {}

    public static void validate(ArrakisTerrainSettings current) throws Exception {
        var oldJson = JsonParser.parseString(Files.readString(Path.of("src/test/resources/terrain/arrakis_6000_dev1.json")))
                .getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld")
                .getAsJsonObject("generator").getAsJsonObject("terrain");
        var old = ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, oldJson).getOrThrow();
        require(!old.buriedRock().erosion().morphology().enabled() && !old.buriedRock().talus().coherentSources(), "dev.1 settings silently migrated");
        require(current.profileVersion()==6000 && current.buriedRock().erosion().morphology().enabled()
                && current.buriedRock().talus().coherentSources(), "dev.2 preset not opted in");
        for (String key : new String[] {"sector_scale","sector_recession","mesoscale_recession","gully_spacing","gully_depth"}) {
            var invalid = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE,current).getOrThrow().getAsJsonObject();
            invalid.getAsJsonObject("buried_rock").getAsJsonObject("erosion").getAsJsonObject("morphology").addProperty(key,-1);
            require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,invalid).error().isPresent(),"unbounded morphology setting: "+key);
        }
        validatePreservedFields(old,current);
        validateSlopeAndDepthRegressions(current);
        validateWalls(old,current);
        validateFinalSurface(old,current);
        validateFrontage(current);
        validatePatterns(current);
        validateResistanceAndExposure(current);
        validateDeposits(old,current);
        benchmark(old,current);
    }

    private static void validateSlopeAndDepthRegressions(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
        var column = evaluator.buriedColumn(3100, 150);
        // A planar exposed slope is not a steep cliff. Run the production morphology and
        // roof evaluator on it, including actual displaced lithology and burial protection.
        double x = 3100.5, z = 150.5, top = 220;
        RockFaceExposure.HeightLookup slope = (sx, sz) -> top + (sx - x) * .3;
        var face = RockFaceExposure.external(x, z, top, 64, 5, 48, 18, slope);
        require(!face.exposed(), "gentle-slope regression no longer exercises the old cliff rejection");
        var morphology = WallErosionMorphology.sample(0, x, z, top, 64, face, column.raw().geography(), settings);
        var roof = RockErosionField.sample(0, x, z, top, 64, face, MassifFractureField.NONE,
                column.lithology(), 0, 0, settings.buriedRock(), slope, morphology);
        var control = RockErosionField.sample(0, x, z, top, 64, face, MassifFractureField.NONE,
                column.lithology(), 0, 0, settings.buriedRock(), slope);
        require(morphology.exposureGate() > .25 && control.rockTop() - roof.rockTop() > 1,
                "externally exposed gentle slope still cannot receive meaningful morphology");
        var buried = RockErosionField.sample(0, x, z, top, top + 1, face, MassifFractureField.NONE,
                column.lithology(), 0, 0, settings.buriedRock(), slope, morphology);
        require(buried.rockTop() == top, "new slope erosion entered buried rock");

        var without = new ArrakisTerrainEvaluator(0, morphologySetting(settings, "enabled", false), 1024);
        require(without.buriedColumn(3100, 150).erosion().rockTop() - column.erosion().rockTop() > 8,
                "reported 3100/150 shoulder still has recession distance without roof change");

        // The same surface material, but an additional resistant formation first encountered
        // 60 blocks down. This catches the old top/8/20/40-only resistance approximation.
        double soft = RockErosionField.erodeThroughStrata(220, -48, 120, y -> 1);
        double layered = RockErosionField.erodeThroughStrata(220, -48, 120, y -> y < 160 ? .28 : 1);
        require(Math.abs(soft - 100) < 1e-8 && layered > soft + 30 && layered < 160,
                "deep resistant formation did not slow erosion after being encountered");
        double shifted = RockErosionField.erodeThroughStrata(237.3, -30.7, 120, y -> y < 177.3 ? .28 : 1);
        require(Math.abs(shifted - layered - 17.3) < 1e-8, "resistance integration is tied to world-Y steps");
        double previous = 220;
        for (double work = .25; work < 160; work += .25) {
            double result = RockErosionField.erodeThroughStrata(220, -48, work, y -> y < 160 ? .28 : 1);
            require(result <= previous && previous - result <= .251,
                    "strata integration creates a discontinuous ledge or reverses erosion");
            previous = result;
        }
        require(RockErosionField.erodeThroughStrata(220, -48, 10000, y -> 1) == -48,
                "strata integration passed geological minimum");
    }

    private static ArrakisTerrainSettings morphologySetting(ArrakisTerrainSettings settings, String name, Object value) {
        var json = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        var morphology = json.getAsJsonObject("buried_rock").getAsJsonObject("erosion").getAsJsonObject("morphology");
        if (value instanceof Boolean enabled) morphology.addProperty(name, enabled);
        else morphology.addProperty(name, (Number) value);
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static void validatePreservedFields(ArrakisTerrainSettings old, ArrakisTerrainSettings current) {
        var a = new ArrakisTerrainEvaluator(0,old,1024);
        var b = new ArrakisTerrainEvaluator(0,current,1024);
        for (int[] p : new int[][] {{0,0},{400,200},{-1000,-200},{1200,0},{9000,9000},{3057,150},{-1200,-4000}}) {
            var ca=a.buriedColumn(p[0],p[1]); var cb=b.buriedColumn(p[0],p[1]);
            require(ca.raw().equals(cb.raw()) && ca.sediment().equals(cb.sediment()),"dev.2 changed uplift, faults, deep geology or sediment");
            for(int y=-63;y<300;y++) require(ca.lithology().sample(y).equals(cb.lithology().sample(y)),"structural lithology changed");
            if(ca.raw().rockTop()<ca.sediment().surfaceY()) require(ca.erosion().rockTop()==cb.erosion().rockTop(),"buried roof was eroded");
        }
        require(Math.abs(a.buriedColumn(3057,150).erosion().rockTop()-110.90)<.005,"saved dev.1 reference roof changed");
        require(Math.abs(a.buriedColumn(4096,0).erosion().rockTop()-162.31)<.005,"saved dev.1 outer roof changed");
    }

    private static void validateWalls(ArrakisTerrainSettings old, ArrakisTerrainSettings current) {
        List<Double> removal = new ArrayList<>(), extra = new ArrayList<>();
        var a=new ArrakisTerrainEvaluator(0,old,1024); var b=new ArrakisTerrainEvaluator(0,current,1024);
        int gullies=0, retained=0; double oldTotal=0, newTotal=0;
        // Keep the initial dev.2 active-field checks as supplementary diagnostics. Independent
        // region coverage and actual surface-shape acceptance are checked in validateFinalSurface.
        for(int degrees=0;degrees<360;degrees+=6) {
            double angle=Math.toRadians(degrees);
            for(int radius=2950;radius<=4300;radius+=25) {
                int x=(int)Math.round(radius*Math.cos(angle)), z=(int)Math.round(radius*Math.sin(angle));
                var c=b.buriedColumn(x,z);
                if(c.erosion().morphology().sectorRecession()+c.erosion().morphology().mesoscaleRecession()<.2) continue;
                var previous=a.buriedColumn(x,z);
                double loss=c.erosion().removedAmount();
                removal.add(loss); extra.add(loss-previous.erosion().removedAmount());
                oldTotal+=previous.erosion().removedAmount(); newTotal+=loss;
                if(c.erosion().morphology().gullyIncision()>2) gullies++;
                if(c.erosion().rockTop()>c.sediment().surfaceY()+40) retained++;
                require(c.erosion().horizontalRecession()<=58.000001,"unbounded recession");
                require(c.rockTopY()>=-48 && c.erosion().rockTop()<=c.raw().rockTop(),"invalid eroded roof");
                var erosion=c.erosion();
                require(Math.abs(erosion.incision()+erosion.majorRemoval()+erosion.surfaceRemoval()
                        +erosion.morphology().gullyIncision()-erosion.removedAmount())<1e-8,"erosion breakdown does not sum to total");
            }
        }
        require(removal.size()>80,"insufficient eroding wall coverage");
        double mean=average(removal), sd=deviation(removal);
        require(mean>8 && average(extra)>4 && newTotal>oldTotal*1.3,"erosion still negligible compared with dev.1");
        require(sd>5 && gullies>8 && retained>25,"wall lacks variance, gullies or surviving buttresses");
        System.out.printf(Locale.ROOT,"Selected active-field diagnostic (not coverage): n=%d dev1-mean=%.2f dev2-mean=%.2f SD=%.2f added=%.2f gullies=%d retained-high=%d.%n",
                removal.size(),oldTotal/removal.size(),mean,sd,average(extra),gullies,retained);
        // User-reported broad northwest slope, beyond the old inner-toe fixtures.
        double northwest=0, largest=0; int count=0, inspectX=0, inspectZ=0;
        for(int x=-1400;x<=-800;x+=20) for(int z=-4100;z<=-3900;z+=20) {
            var c=b.buriedColumn(x,z);
            if(c.erosion().morphology().sectorRecession()>.2) {
                northwest+=c.erosion().removedAmount(); count++;
                if(c.erosion().removedAmount()>largest) {largest=c.erosion().removedAmount(); inspectX=x; inspectZ=z;}
            }
        }
        require(count>10 && northwest/count>5,"northwest smooth-slope report not addressed");
        System.out.printf(Locale.ROOT,"Northwest exposed morphology: n=%d mean removal=%.2f strongest sampled X/Z=%d/%d removed=%.2f.%n",count,northwest/count,inspectX,inspectZ,largest);
        // Same absolute negative-coordinate chunk queried in opposite traversal orders.
        var forward=new ArrakisTerrainEvaluator(0,current,1024); var reverse=new ArrakisTerrainEvaluator(0,current,1);
        double[] roofs=new double[256];
        for(int i=0;i<256;i++) roofs[i]=forward.buriedColumn(inspectX+i%16,inspectZ+i/16).erosion().rockTop();
        for(int i=255;i>=0;i--) require(roofs[i]==reverse.buriedColumn(inspectX+i%16,inspectZ+i/16).erosion().rockTop(),"negative chunk morphology order seam");
    }

    private static void validateFinalSurface(ArrakisTerrainSettings old, ArrakisTerrainSettings settings) {
        var baseline = new ArrakisTerrainEvaluator(0, old, 1024);
        var current = new ArrakisTerrainEvaluator(0, settings, 1024);
        var noGully = new ArrakisTerrainEvaluator(0, morphologySetting(settings, "gully_depth", 0), 1024);
        int exposed = 0, slopes = 0, changedSlopes = 0, shoulders = 0, changedShoulders = 0;
        int visibleGullies = 0, adjoiningGullies = 0;
        double visibleChange = 0, alongDifference = 0, acrossDifference = 0;
        // Select by raw geology and envelope shape BEFORE inspecting any erosion output.
        for (int degrees = 0; degrees < 360; degrees += 6) {
            double angle = Math.toRadians(degrees);
            for (int radius = 2950; radius <= 4300; radius += 25) {
                int x = (int) Math.round(radius * Math.cos(angle)), z = (int) Math.round(radius * Math.sin(angle));
                var c = current.buriedColumn(x, z);
                if (c.raw().rockTop() <= c.sediment().surfaceY() + 12
                        || c.raw().geography().physicalMassifWeight() <= .45) continue;
                exposed++;
                double change = visibleRoof(baseline.buriedColumn(x, z)) - visibleRoof(c);
                visibleChange += change;
                var face = c.erosion().face();
                if (face.steepness() >= .15 && face.steepness() <= .45) {
                    slopes++; if (change > 1) changedSlopes++;
                }
                if (face.nearRelief() / face.nearProbeDistance() < .1 && face.steepness() > .6) {
                    shoulders++; if (change > 4) changedShoulders++;
                }
                double gully = visibleRoof(noGully.buriedColumn(x, z)) - visibleRoof(c);
                int ax = x + (int) Math.round(face.outwardNormalX() * 2);
                int az = z + (int) Math.round(face.outwardNormalZ() * 2);
                int bx = x - (int) Math.round(face.outwardNormalZ() * 16);
                int bz = z + (int) Math.round(face.outwardNormalX() * 16);
                double along = visibleRoof(noGully.buriedColumn(ax, az)) - visibleRoof(current.buriedColumn(ax, az));
                double across = visibleRoof(noGully.buriedColumn(bx, bz)) - visibleRoof(current.buriedColumn(bx, bz));
                alongDifference += Math.abs(gully - along);
                acrossDifference += Math.abs(gully - across);
                if (gully > 2) { visibleGullies++; if (along > 1) adjoiningGullies++; }
            }
        }
        System.out.printf(Locale.ROOT, "Independent exposed-wall sample: n=%d mean visible change=%.2f; gentle slopes >1 block=%d/%d; shoulders >4 blocks=%d/%d; visible gullies=%d adjoining=%d; final gully along/across=%.3f.%n",
                exposed, visibleChange / exposed, changedSlopes, slopes, changedShoulders, shoulders,
                visibleGullies, adjoiningGullies, alongDifference / acrossDifference);
        require(exposed > 1500 && slopes > 30 && shoulders > 20, "independent wall regions missing");
        // A majority must change by a full block, while low-potential sectors and resistant
        // beds may remain. Requiring every slope to retreat would erase the intended buttresses.
        require(changedSlopes > slopes * .5 && changedShoulders > shoulders * .5,
                "morphology still misses most gentle slopes or exposed shoulders");
        require(visibleGullies > 25 && adjoiningGullies > visibleGullies * .6
                        && alongDifference < acrossDifference * .65,
                "gully pattern does not survive into coherent visible terrain");
    }

    private static double visibleRoof(BuriedTerrainColumn column) {
        return Math.max(column.erosion().rockTop(), column.sediment().surfaceY());
    }

    private static void validateFrontage(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
        List<Double> retreat = new ArrayList<>();
        // A fixed inner-wall window independent of the erosion pattern. Measure an actual
        // height contour before/after erosion, rather than counting high columns as buttresses.
        double adjacentDelta = 0;
        for (int z = -128; z <= 384; z += 4) {
            double rawCrossing = Double.NaN, erodedCrossing = Double.NaN;
            double previousRaw = Double.NaN, previousEroded = Double.NaN;
            for (int x = 2980; x <= 3200; x += 4) {
                var c = evaluator.buriedColumn(x, z);
                double raw = c.raw().rockTop(), eroded = visibleRoof(c);
                if (Double.isFinite(previousRaw) && previousRaw < 150 && raw >= 150 && !Double.isFinite(rawCrossing))
                    rawCrossing = x - 4 + 4 * (150 - previousRaw) / (raw - previousRaw);
                if (Double.isFinite(previousEroded) && previousEroded < 150 && eroded >= 150 && !Double.isFinite(erodedCrossing))
                    erodedCrossing = x - 4 + 4 * (150 - previousEroded) / (eroded - previousEroded);
                previousRaw = raw; previousEroded = eroded;
            }
            require(Double.isFinite(rawCrossing) && Double.isFinite(erodedCrossing), "frontage window no longer spans the wall");
            double displacement = erodedCrossing - rawCrossing;
            require(displacement >= -1e-8, "erosion advanced the height contour");
            if (!retreat.isEmpty()) adjacentDelta += Math.abs(displacement - retreat.getLast());
            retreat.add(displacement);
        }
        double distantDelta = 0;
        for (int i = 16; i < retreat.size(); i++) distantDelta += Math.abs(retreat.get(i) - retreat.get(i - 16));
        adjacentDelta /= retreat.size() - 1;
        distantDelta /= retreat.size() - 16;
        System.out.printf(Locale.ROOT, "Visible Y150 frontage: mean retreat=%.2f SD=%.2f; 4/64-block delta=%.2f/%.2f.%n",
                average(retreat), deviation(retreat), adjacentDelta, distantDelta);
        require(average(retreat) > 5 && deviation(retreat) > 3 && adjacentDelta < distantDelta * .6,
                "final frontage lacks coherent recessed bays and projecting neighboring sectors");
    }

    private static void validatePatterns(ArrakisTerrainSettings settings) {
        var config=settings.buriedRock().erosion().morphology();
        double near=0,far=0, gullyNear=0,gullyFar=0; int active=0;
        for(boolean inner:new boolean[]{true,false}) {
            double r=inner?3000:4048;
            for(int i=0;i<1500;i++) {
                double angle=i*Math.PI*2/1500;
                var a=pattern(r,angle,0,inner,config);
                var b=pattern(r,angle+1/r,0,inner,config);
                var c=pattern(r,angle+100/r,0,inner,config);
                near+=Math.abs(a.sector()-b.sector()); far+=Math.abs(a.sector()-c.sector());
                var along=pattern(r,angle,2,inner,config);
                var across=pattern(r,angle+16/r,0,inner,config);
                gullyNear+=Math.abs(a.gully()-along.gully()); gullyFar+=Math.abs(a.gully()-across.gully());
                if(a.gully()>.15) active++;
            }
            var left=WallErosionMorphology.pattern(0,-r,-.000001,r,inner,config);
            var right=WallErosionMorphology.pattern(0,-r,.000001,r,inner,config);
            require(Math.abs(left.sector()-right.sector())<.00001 && Math.abs(left.gully()-right.gully())<.00001,"angular/negative-coordinate seam");
        }
        require(near<far*.08 && far>10,"sector field lacks broad spatial coherence");
        require(gullyNear<gullyFar*.3 && active>100,"channels are not downslope coherent");
        System.out.printf(Locale.ROOT,"Pattern coherence: sector 1/100-block delta ratio=%.4f, gully along/across=%.4f.%n",near/far,gullyNear/gullyFar);
    }

    private static WallErosionMorphology.Pattern pattern(double r,double angle,double distance,boolean inner,BuriedRockSettings.Morphology config) {
        double radius=r+distance;
        return WallErosionMorphology.pattern(0,Math.cos(angle)*radius,Math.sin(angle)*radius,r,inner,config);
    }

    private static void validateResistanceAndExposure(ArrakisTerrainSettings settings) {
        var low=new ArrakisTerrainEvaluator(0,withResistance(settings,0),1024);
        var high=new ArrakisTerrainEvaluator(0,withResistance(settings,2),1024);
        int different=0;
        // Same uplift, lithology and exposure, changing only the existing susceptibility controls.
        for(int x=3040;x<=3110;x+=2) {
            var a=low.buriedColumn(x,150).erosion();
            var b=high.buriedColumn(x,150).erosion();
            require(a.face().equals(b.face()),"resistance changed external exposure");
            require(b.rockTop()<=a.rockTop()+1e-9,"resistance response inverted");
            if(a.rockTop()-b.rockTop()>1) different++;
        }
        require(different>=3,"lithology susceptibility has no meaningful morphology effect");
        var e=new ArrakisTerrainEvaluator(0,settings,1024);
        var inner=e.buriedColumn(3057,150); var outer=e.buriedColumn(4096,0);
        require(inner.erosion().face().outwardNormalX()<0 && outer.erosion().face().outwardNormalX()>0,
                "production inner/outer recession normal reversed");
        var plateau=e.buriedColumn(3400,0);
        var flat=RockFaceExposure.external(3400.5,.5,220,64,5,48,18,(x,z)->220);
        require(plateau.raw().geography().physicalMassifWeight()>.9,"plateau ownership fixture missing");
        require(WallErosionMorphology.sample(0,3400.5,.5,220,64,flat,plateau.raw().geography(),settings)
                .equals(WallErosionMorphology.Sample.NONE),"formation mask used as cliff detector");
    }

    private static ArrakisTerrainSettings withResistance(ArrakisTerrainSettings settings,double value) {
        var json=ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE,settings).getOrThrow().getAsJsonObject();
        var erosion=json.getAsJsonObject("buried_rock").getAsJsonObject("erosion");
        for(String key:new String[]{"soft_multiplier","hard_multiplier","very_hard_multiplier"}) erosion.addProperty(key,value);
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
    }

    private static void validateDeposits(ArrakisTerrainSettings old,ArrakisTerrainSettings settings) {
        var material=LithologyField.Material.STONE;
        TalusColluviumField.SourceLookup isolated=(x,z)->new TalusColluviumField.Source(140,x==8&&z==0?40:0,-1,0,material);
        var previous=TalusColluviumField.sample(0,0,0,64,old.buriedRock().talus(),isolated);
        var missed=TalusColluviumField.sample(0,0,1,64,old.buriedRock().talus(),isolated);
        require(previous.active() && !missed.active(),"dev.1 ray-aliasing diagnosis fixture no longer reproduces");
        var config=settings.buriedRock().talus();
        TalusColluviumField.SourceLookup region=(x,z)->new TalusColluviumField.Source(140,
                x>=4&&x<=12&&Math.abs(z)<=12?30:0,-1,0,material);
        double last=Double.NaN; int coherent=0;
        for(int z=-12;z<=12;z++) {
            var c=TalusColluviumField.sample(0,0,z,64,config,region);
            require(c.equals(TalusColluviumField.sample(0,0,z,64,config,region)),"deposit query order changed");
            if(Double.isFinite(last)) require(Math.abs(c.tendency()-last)<.7,"one-column deposit maximum");
            last=c.tendency(); if(c.active()) coherent++;
        }
        require(coherent>=15,"valid source region did not produce a coherent patch");
        var single=TalusColluviumField.sample(0,6,0,64,config,isolated);
        require(single.tendency()<1.4,"isolated source produced tall post");
        System.out.printf("Talus diagnosis reproduced ray miss at neighboring recipient; coherent patch has %d/25 occupied columns.%n",coherent);
    }

    private static void benchmark(ArrakisTerrainSettings old,ArrakisTerrainSettings current) {
        double[] times=new double[2];
        for(int repeat=0;repeat<4;repeat++) for(int mode=0;mode<2;mode++) {
            var e=new ArrakisTerrainEvaluator(0,mode==0?old:current,1024);
            long start=System.nanoTime();
            for(int z=144;z<160;z++) for(int x=3040;x<3072;x++) e.buriedColumn(x,z);
            if(repeat>0) times[mode]+=(System.nanoTime()-start)/1e6;
        }
        System.out.printf(Locale.ROOT,"Warm analytical 2-chunk mean (not DH): dev1=%.2fms dev2=%.2fms ratio=%.2f.%n",times[0]/3,times[1]/3,times[1]/times[0]);
    }
    private static double average(List<Double> values) {return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);}
    private static double deviation(List<Double> values) {double mean=average(values); return Math.sqrt(values.stream().mapToDouble(v->(v-mean)*(v-mean)).average().orElse(0));}
    private static void require(boolean condition,String message) {if(!condition) throw new AssertionError(message);}
}
