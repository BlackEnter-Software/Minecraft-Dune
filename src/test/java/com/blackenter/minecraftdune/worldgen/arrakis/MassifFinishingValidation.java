package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.*;
import java.util.*;

/** Revision-3 physical constraints; historical revision-1/2 fixtures stay independent. */
final class MassifFinishingValidation {
    static void validate(ArrakisTerrainSettings current, ArrakisTerrainSettings dev2) throws Exception {
        talus(current);
        fissures(current);
        cavities(current);
        compositionAndMorphology(current, dev2);
        performance(current, dev2);
    }

    private static void talus(ArrakisTerrainSettings settings) {
        int[] sourceCounts = new int[3]; int oldSource = 0, count = 0;
        for (int x = -160; x < 160; x += 3) for (int z = -160; z < 160; z += 3) {
            for (int band = 0; band < 3; band++) {
                var deposit = new TalusColluviumField.Sample(64, 68, 0, 0, new double[] {.1, .5, .9}[band], LithologyField.Material.BASALT);
                if (StableTalusField.materialAt(0, x, 66, z, deposit, settings.buriedRock().finishing().sourceClastFraction())
                        == LithologyField.Material.BASALT) sourceCounts[band]++;
                if (band == 0 && TalusColluviumField.materialAt(0,x,66,z,deposit) == LithologyField.Material.BASALT) oldSource++;
            }
            count++;
        }
        require(sourceCounts[0] > count * .65 && sourceCounts[0] < count * .99, "proximal talus needs dominant clasts plus matrix");
        require(sourceCounts[0] > sourceCounts[1] && sourceCounts[1] > sourceCounts[2], "distance does not sort debris");
        require(StableTalusField.route(0,0,120,16,0,64,(x,z)->x==8?130:64)==0, "debris passed through a ridge");
        require(StableTalusField.route(0,0,120,16,0,64,(x,z)->64)==1, "clear downhill route was blocked");
        require(StableTalusField.suitability(64,100,64,64,64,1.1)>=1, "cliff toe treated as steep unstable face");
        require(StableTalusField.suitability(110,125,95,110,110,1.1)==0, "vertical cliff retains thick debris");
        require(StableTalusField.suitability(80,81,79,80,80,1.1)>0, "moderate slopes disallowed");
        double minimum = Double.POSITIVE_INFINITY, maximum = 0; int oldMisses = 0;
        for (int offset=0; offset<4; offset++) {
            final int shift=offset;
            TalusColluviumField.SourceLookup source = (x,z) -> new TalusColluviumField.Source(
                    x>=4+shift && x<=5+shift?124:64, x>=4+shift && x<=5+shift?40:0, -1,0,LithologyField.Material.BASALT);
            var old=TalusColluviumField.sample(0,shift,0,64,settings.buriedRock().talus(),source);
            var now=StableTalusField.sample(0,shift,0,64,settings.buriedRock(),source,(x,z)->source.sample(x,z).rockTop());
            if(old.tendency()==0)oldMisses++;
            require(now.tendency()>0,"translated two-block source disappeared");
            minimum=Math.min(minimum,now.tendency()); maximum=Math.max(maximum,now.tendency());
        }
        require(oldMisses>0 && minimum/maximum>.5,"finer source integration did not reduce grid alignment loss");
        System.out.printf(Locale.ROOT,"Talus source-rock shares: old proximal %.1f%%; dev3 proximal/middle/distal %.1f/%.1f/%.1f%%; translated supply min/max %.3f.%n",
                100.0*oldSource/count,100.0*sourceCounts[0]/count,100.0*sourceCounts[1]/count,100.0*sourceCounts[2]/count,minimum/maximum);
    }

    private static void fissures(ArrakisTerrainSettings settings) throws Exception {
        double min=2,max=0;
        for(int along=-2000;along<2000;along++) {
            double depth=MassifFractureField.surfaceDepthMultiplier(123,along,settings.buriedRock().finishing().fissureVariation());
            min=Math.min(min,depth);max=Math.max(max,depth);
            require(Math.abs(depth-MassifFractureField.surfaceDepthMultiplier(123,along+.001,.85))<.01,"fissure expression discontinuity");
        }
        require(max-min>.6 && max>1 && min<.4,"major fissures still have nearly constant depth");
        require(MassifFractureField.branchTaper(0)==1 && MassifFractureField.branchTaper(1)==0
                && MassifFractureField.branchTaper(2)==0 && MassifFractureField.branchTaper(.8)<MassifFractureField.branchTaper(.6),"branch does not terminate/taper");
        for(int x:new int[]{-65,-64,-17,-16,15,16,63,64}) {
            var a=MassifFractureField.surface(0,x+.5,-3100.5,LithologyField.ResistanceClass.MEDIUM,settings.fractures(),.85);
            var b=MassifFractureField.surface(0,x+.50001,-3100.5,LithologyField.ResistanceClass.MEDIUM,settings.fractures(),.85);
            require(Math.abs(a.carveDepth()-b.carveDepth())<.01,"fracture boundary discontinuity");
            require(a.equals(MassifFractureField.surface(0,x+.5,-3100.5,LithologyField.ResistanceClass.MEDIUM,settings.fractures(),.85)),"fracture nondeterminism");
        }
        // Exercise the production finite-segment evaluator, not just its taper helper.
        var accumulator = Class.forName(MassifFractureField.class.getName() + "$Accumulator");
        var constructor = accumulator.getDeclaredConstructor(); constructor.setAccessible(true);
        var segment = Arrays.stream(MassifFractureField.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("considerSegment")).findFirst().orElseThrow();
        segment.setAccessible(true);
        var sample = accumulator.getDeclaredMethod("toSample", double.class, double.class); sample.setAccessible(true);
        double previous = Double.POSITIVE_INFINITY;
        for (double position : new double[] {60, 80, 95, 100, 120}) {
            Object state = constructor.newInstance();
            segment.invoke(null, state, position, 0., 0., 0., 100., 0., 8., 30., .82, 4.1,
                    0., 0., 1., LithologyField.ResistanceClass.MEDIUM, settings.fractures(), .85);
            var result = (MassifFractureField.Sample) sample.invoke(state, 1., 1.);
            require(result.carveDepth() <= previous, "actual branch tip does not taper");
            if (position >= 100) require(result.carveDepth() == 0, "finite branch continues beyond its endpoint");
            previous = result.carveDepth();
        }
        System.out.printf(Locale.ROOT,"Fissure coherent depth multiplier range %.3f..%.3f.%n",min,max);
    }

    private static ExposedCliffCavityField.SurfaceLookup cliff(int cellX,int cellZ,int nx,int nz,LithologyField.ResistanceClass resistance) {
        return new ExposedCliffCavityField.SurfaceLookup() {
            public ExposedCliffCavityField.Surface sample(int x,int z) {
                double side=(x-(cellX*64+32))*nx+(z-(cellZ*64+32))*nz;
                return new ExposedCliffCavityField.Surface(side>=0?150:64,64,1,.3);
            }
            public LithologyField.ResistanceClass resistance(int x,int y,int z){return resistance;}
        };
    }

    private static void cavities(ArrakisTerrainSettings settings) {
        var c=settings.buriedRock().finishing().cliffCavities();
        var config=new BuriedRockSettings.CliffCavities(true,c.maximumPenetration(),c.maximumHeight(),1,c.strength(),c.minimumRoofThickness());
        int volume=0,features=0;
        for(int cell:new int[]{-2,0,1}) for(int[] n:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) for(int seed=0;seed<6;seed++) {
            var terrain=cliff(cell,cell,n[0],n[1],LithologyField.ResistanceClass.SOFT);
            var feature=ExposedCliffCavityField.sample(seed,cell,cell,config,terrain);
            var harder=ExposedCliffCavityField.sample(seed,cell,cell,config,cliff(cell,cell,n[0],n[1],LithologyField.ResistanceClass.VERY_HARD));
            require(feature.volume()>=harder.volume(),"harder unit has more cavity removal");
            if(feature.volume()==0)continue;
            features++;volume+=feature.volume();
            for(int x=cell*64;x<(cell+1)*64;x++)for(int z=cell*64;z<(cell+1)*64;z++)for(int y=feature.baseY();y<feature.baseY()+feature.height();y++) {
                if(!feature.column(x,z).removes(y))continue;
                int steps=0,px=x,pz=z;
                while(y<=terrain.sample(px,pz).external()) {
                    require(feature.column(px,pz).removes(y),"sealed cavity: rock blocks its exterior lane");
                    require(++steps<=config.maximumPenetration(),"cavity exceeds penetration cap");
                    px-=feature.inwardX();pz-=feature.inwardZ();
                }
                require(terrain.sample(x,z).roof()>=feature.baseY()+feature.height()+config.minimumRoofThickness(),"thin cavity roof");
                // A common intact roof plane connects every carved lane to the uncut back.
                int depth=(x-feature.mouthX())*feature.inwardX()+(z-feature.mouthZ())*feature.inwardZ();
                for(int d=depth;d<=feature.length();d++) {
                    int rx=x+(d-depth)*feature.inwardX(),rz=z+(d-depth)*feature.inwardZ();
                    int roofY=feature.baseY()+feature.height();
                    require(terrain.sample(rx,rz).roof()>=roofY+config.minimumRoofThickness()
                            && !feature.column(rx,rz).removes(roofY), "roof lost its connection to the back wall");
                }
            }
        }
        require(features>=12 && volume>0,"synthetic cliffs cannot produce cavities");
        var flat=new ExposedCliffCavityField.SurfaceLookup(){
            public ExposedCliffCavityField.Surface sample(int x,int z){return new ExposedCliffCavityField.Surface(40,64,1,1);}
            public LithologyField.ResistanceClass resistance(int x,int y,int z){return LithologyField.ResistanceClass.SOFT;}
        };
        for(int seed=0;seed<20;seed++)require(ExposedCliffCavityField.sample(seed,0,0,config,flat).volume()==0,"buried spontaneous cavity");
        var plateau=new ExposedCliffCavityField.SurfaceLookup(){
            public ExposedCliffCavityField.Surface sample(int x,int z){return new ExposedCliffCavityField.Surface(200,64,1,1);}
            public LithologyField.ResistanceClass resistance(int x,int y,int z){return LithologyField.ResistanceClass.SOFT;}
        };
        for(int seed=0;seed<20;seed++)require(ExposedCliffCavityField.sample(seed,0,0,config,plateau).volume()==0,"flat summit spontaneous cavity");
        require(!new ExposedCliffCavityField.Surface(80,64,1,0,4).certainlyOpen(84)
                && new ExposedCliffCavityField.Surface(80,64,1,0,4).certainlyOpen(85),"opening ignores possible talus occlusion");
        System.out.printf("Cavities: %d synthetic features, %d connected voxels checked across four normals and negative cells.%n",features,volume);
    }

    private static void performance(ArrakisTerrainSettings current,ArrakisTerrainSettings dev2) {
        double[] times = new double[2]; long checksum = 0;
        for(int round=0;round<5;round++)for(int order=0;order<2;order++) {
            int revision=(order+round)%2;
            long started=System.nanoTime();
            for(int[] origin:new int[][]{{3056,144},{-624,3104}}) {
                var evaluator=new ArrakisTerrainEvaluator(0,revision==0?dev2:current,1024);
                for(int z=origin[1];z<origin[1]+16;z++)for(int x=origin[0];x<origin[0]+16;x++)
                    checksum+=evaluator.buriedColumn(x,z).highestOccupiedY();
            }
            if(round>0)times[revision]+=(System.nanoTime()-started)/4e6;
        }
        require(checksum>0,"benchmark did not evaluate terrain");
        System.out.printf(Locale.ROOT,"Warm analytical 2-chunk mean (not DH/game throughput): dev2=%.2fms dev3=%.2fms ratio=%.2f.%n",
                times[0],times[1],times[1]/times[0]);
    }

    private static void compositionAndMorphology(ArrakisTerrainSettings current,ArrakisTerrainSettings dev2) {
        var old=new ArrakisTerrainEvaluator(0,dev2,1024);
        var now=new ArrakisTerrainEvaluator(0,current,1024);
        double summitMin=99,summitMax=0,removedBefore=0,removedNow=0;int talusBefore=0,talusNow=0,clasts=0,gravel=0;
        long started=System.nanoTime();
        for(int x=3032;x<=3120;x+=8)for(int z=128;z<=192;z+=8) {
            var a=old.buriedColumn(x,z);var b=now.buriedColumn(x,z);
            require(a.raw().equals(b.raw()) && a.sediment().equals(b.sediment()),"dev3 changed raw geology or sediment");
            removedBefore+=a.erosion().removedAmount();removedNow+=b.erosion().removedAmount();
            if(a.talus().active())talusBefore++;if(b.talus().active())talusNow++;
            if(b.talus().active())for(int y=b.talus().bottomY();y<=b.talus().topY();y++) {
                var material=b.cellAt(y,-64).material();
                if(material==b.talus().sourceMaterial())clasts++;
                else if(material==LithologyField.Material.GRAVEL)gravel++;
            }
            require(b.erosion().rockTop()<=b.raw().rockTop(),"summit relief adds decorative piles");
            for(int y=-64;y<320;y++) {
                var cell=b.cellAt(y,-64);
                if(y>-64&&y<=b.rockTopY())require((cell.kind()==BuriedTerrainColumn.Kind.AIR)==b.cavities().removes(y),"unauthorized geological void");
            }
        }
        for(int x=3280;x<3800;x+=32) {
            var b=now.buriedColumn(x,100);
            summitMin=Math.min(summitMin,b.summitRemoval());summitMax=Math.max(summitMax,b.summitRemoval());
            require(b.summitRemoval()>=0&&b.summitRemoval()<=current.buriedRock().finishing().summitRelief(),"unbounded summit relief");
        }
        require(summitMax-summitMin>.5,"summit relief is uniform");
        require(removedNow/removedBefore<1.35,"erosion change is not modest");
        int found=0;
        for(int cx=-10;cx<=10&&found<3;cx++)for(int cz=45;cz<=51&&found<3;cz++) {
            var f=now.cavityFeature(cx,cz);
            if(f.volume()==0)continue;
            found++;
            int checked=0;
            for(int x=cx*64;x<(cx+1)*64&&checked<3;x++)for(int z=cz*64;z<(cz+1)*64&&checked<3;z++) {
                if(f.column(x,z).volume()==0)continue;
                checked++;
                var column=now.buriedColumn(x,z);
                for(int capacity:new int[]{0,1,64}) {
                    var other=new ArrakisTerrainEvaluator(0,current,capacity);other.buriedColumn(0,0);
                    var translated=other.buriedColumn(x,z);
                    for(int y=-64;y<320;y++) require(column.cellAt(y,-64).equals(translated.cellAt(y,-64)),"cavity/cache order mismatch");
                }
                var written=new BuriedTerrainColumn.Cell[384];column.compose(-64,320,(y,cell)->written[y+64]=cell);
                for(int y=-64;y<320;y++)require(written[y+64].equals(column.cellAt(y,-64)),"cavity composer mismatch");
                require(column.baseHeight(-64,320,cell->cell.kind()!=BuriedTerrainColumn.Kind.AIR)==column.highestOccupiedY()+1,"internal cavity changed top height");
            }
            System.out.printf("Southern cavity fixture: cell %d/%d mouth %d/%d baseY %d volume %d.%n",cx,cz,f.mouthX(),f.mouthZ(),f.baseY(),f.volume());
        }
        require(found>0,"no native southern-wall cavities found");
        System.out.printf("Native sampled talus blocks: source clasts=%d gravel matrix=%d.%n",clasts,gravel);
        System.out.printf(Locale.ROOT,"Dev3 sample: erosion ratio %.3f, talus columns %d -> %d, summit removal %.2f..%.2f; finishing checks %.2fs.%n",
                removedNow/removedBefore,talusBefore,talusNow,summitMin,summitMax,(System.nanoTime()-started)/1e9);
    }

    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
