package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.BasalTuningValidation;
import java.util.function.IntBinaryOperator;

/** Focused fixtures for the additional two-block support reach and organic deposit profile. */
public final class BasalTuningShapeValidation {
    private BasalTuningShapeValidation() {}

    public static void validate(ArrakisTerrainSettings settings) {
        var old = BasalTuningValidation.previousSettings(settings).erosion().orphanRemnants();
        var now = settings.erosion().orphanRemnants();
        OrphanRemnantFilter.RawRockLookup shortChain = (x,y,z) -> z == 0 && x >= 0 && x <= 8;
        require(OrphanRemnantFilter.keeps(0,80,0,true,100,-1,0,true,old,shortChain), "old support fixture failed");
        require(!OrphanRemnantFilter.keeps(0,80,0,true,100,-1,0,true,now,shortChain), "old short support chain retained");
        var previousTen = new ArrakisTerrainSettings.OrphanRemnantSettings(now.enabled(),10,
                now.lateralSearchRadius(),now.minimumHeightAboveBase(),now.minimumFaceRelief(),
                now.basalComponentCleanupEnabled(),now.faultEdgeCleanupEnabled(),now.componentSearchRadius());
        for (int length : new int[] {10,11}) {
            OrphanRemnantFilter.RawRockLookup chainBeforeGap = (x,y,z) -> z == 0 && x >= 0 && x <= length;
            require(OrphanRemnantFilter.keeps(0,80,0,true,100,-1,0,true,previousTen,chainBeforeGap),
                    "previous ten-block support fixture failed");
            require(!OrphanRemnantFilter.keeps(0,80,0,true,100,-1,0,true,now,chainBeforeGap),
                    "new support check missed gap in the additional two blocks");
        }
        require(OrphanRemnantFilter.keeps(0,80,0,true,100,-1,0,true,now,
                (x,y,z) -> z == 0 && x >= 0 && x <= 12), "continuous inward support removed");
        require(OrphanRemnantFilter.keeps(0,64,0,true,100,-1,0,true,now,shortChain), "orphan stage escaped above-sand scope");

        var chain = fixture((x,z) -> z == 0 && x >= 0 && x <= 3 ? 150 : 65);
        for (int x = 0; x <= 3; x++) {
            require(!BoundedBasalComponentCleanup.sample(x,0,chain,3).removed(), "legacy edge decision changed");
            var result = BoundedBasalComponentCleanup.sample(x,0,chain,5);
            require(result.removed() && result.componentColumns() == 4 && result.removesY(150), "extended small component not removed");
        }
        var broad = fixture((x,z) -> x >= 0 && x <= 5 && z == 0 ? 150 : 65);
        var rib = fixture((x,z) -> x >= 2 || z == 0 && x >= 0 ? 150 : 65);
        require(!BoundedBasalComponentCleanup.sample(0,0,broad,5).removed(), "larger formation removed");
        require(!BoundedBasalComponentCleanup.sample(0,0,rib,5).removed(), "connected rib removed");
        var protectedNeighbor = new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x,int z) { return chain.topY(x,z); }
            public boolean occupied(int x,int y,int z) { return chain.occupied(x,y,z); }
            public boolean cleanupAllowed(int x,int z) { return x != 3; }
        };
        require(!BoundedBasalComponentCleanup.sample(0,0,protectedNeighbor,5).removed(), "protected attachment severed");

        double minHeight = 1, maxHeight = 0, minReach = 24, maxReach = 0;
        for (int z = -64; z <= 64; z += 8) for (int x = -64; x <= 64; x += 8) {
            var n = TalusShapeVariation.sample(0,x+.5,z+.5);
            require(n.equals(TalusShapeVariation.sample(0,x+.5,z+.5)), "noise not repeatable");
            require(n.heightScale() >= .55 && n.heightScale() <= 1
                    && n.spreadScale() >= .85 && n.spreadScale() <= 1.15
                    && n.skirtReach() >= 16 && n.skirtReach() <= 20, "noise escaped bounds");
            require(Math.abs(n.heightScale() - TalusShapeVariation.sample(0,x+.501,z+.5).heightScale()) < .001,
                    "noise discontinuity at neighboring coordinates");
            minHeight = Math.min(minHeight,n.heightScale()); maxHeight = Math.max(maxHeight,n.heightScale());
            minReach = Math.min(minReach,n.skirtReach()); maxReach = Math.max(maxReach,n.skirtReach());
        }
        require(maxHeight-minHeight > .1 && maxReach-minReach > 1, "variation collapsed into uniform apron");
        require(!TalusShapeVariation.sample(0,3001,464).equals(TalusShapeVariation.sample(1,3001,464)), "noise ignored seed");
        for (long seed : new long[] {0,-5640511200611798902L}) {
            var near = BasalTalusApronField.organicShape(seed,3001.5,464.5,settings,0,212);
            require(near.active() && near.height() >= 2 && near.height() <= 4
                    && near.materialAt(near.topY()) == BasalTalusApronField.Material.GRAVEL,
                    "near-wall coarse surface lost");
            int priorHeight = near.height();
            for (int i = 1; i <= 20; i++) {
                var slope = BasalTalusApronField.organicShape(seed,3001.5,464.5,settings,-near.spread()*i/20,212);
                require(slope.height() <= priorHeight, "fixed-noise cross-section not monotonically tapered");
                priorHeight = slope.height();
            }
            require(priorHeight == 0, "distal gravel rail survived cutoff");
            double reach = TalusShapeVariation.sample(seed,3001.5,464.5).skirtReach();
            require(BasalSandSkirt.shape(true,0,false,reach).depth() == 4
                    && BasalSandSkirt.shape(true,4,false,reach).depth() == 4
                    && BasalSandSkirt.shape(true,-reach/2,false,reach).depth() == 2
                    && !BasalSandSkirt.shape(true,-reach,false,reach).active(), "variable skirt taper wrong");
        }
    }

    private static BoundedBasalComponentCleanup.RockLookup fixture(IntBinaryOperator heights) {
        return new BoundedBasalComponentCleanup.RockLookup() {
            public int topY(int x,int z) { return heights.applyAsInt(x,z); }
            public boolean occupied(int x,int y,int z) { return y >= 65 && y <= topY(x,z); }
            public boolean cleanupAllowed(int x,int z) { return true; }
        };
    }
    private static void require(boolean ok,String message) { if (!ok) throw new IllegalStateException(message); }
}
