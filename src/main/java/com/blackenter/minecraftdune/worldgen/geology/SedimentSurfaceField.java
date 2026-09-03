package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;

/** Absolute depositional envelope. Buried rock height is deliberately not an input. */
public final class SedimentSurfaceField {
    private static final long SEDIMENT_SALT = 0x617FA3D02B8E549CL;
    private SedimentSurfaceField() {}

    public static Sample sample(long seed, double x, double z, MacroGeologyField.Sample geography,
            ArrakisTerrainSettings terrain) {
        var settings = terrain.buriedRock().sediment();
        var dunes = terrain.nativeDunes();
        double grading = 1 - GeologyNoise.smoothStep(terrain.basin().pureSandRadius(),
                terrain.basin().transitionEndRadius(), Math.hypot(x, z));
        double suitability = (1 - grading) * Math.max(dunes.forelandWeight() * geography.innerForelandWeight(),
                Math.max(dunes.brokenRockWeight() * geography.brokenRockWeight(),
                        Math.max(dunes.transitionWeight() * geography.sandRockTransitionWeight(), geography.openErgWeight())));
        var dune = NativeTransverseDuneField.sample(seed, x, z, suitability, dunes);
        double regional = settings.datum() + (1 - grading) * settings.relief()
                * GeologyNoise.value2(seed ^ SEDIMENT_SALT, x / settings.scale(), z / settings.scale());
        int units = (int) Math.round(regional * 16) + dune.surfaceUnits();
        return new Sample(units, grading, dune.surfaceUnits());
    }

    public record Sample(int surfaceUnits, double basinGrading, int duneUnits) {
        public double surfaceY() { return surfaceUnits / 16.0; }
        public int fullTopY() { return Math.floorDiv(surfaceUnits, 16); }
        public int partialLayers() { return Math.floorMod(surfaceUnits, 16); }
        public int highestY() { return fullTopY() + (partialLayers() > 0 ? 1 : 0); }
    }
}
