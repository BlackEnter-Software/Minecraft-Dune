package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;

/** Broad differential lowering of exposed summit rock, upstream of fissure incision. */
public final class SummitWeatheringField {
    private SummitWeatheringField() {}

    public static double removal(long seed, double x, double z, double raw, double sediment,
            double massifWeight, RockFaceExposure.Sample face, LithologyField.ResistanceClass resistance,
            BuriedRockSettings settings) {
        var config = settings.finishing();
        if (!config.enabled() || raw <= sediment || config.summitRelief() == 0) return 0;
        double gate = GeologyNoise.smoothStep(.45, .85, massifWeight)
                * GeologyNoise.smoothStep(12, 48, raw - sediment)
                * (1 - GeologyNoise.smoothStep(.25, .7, face.steepness()));
        double scale = config.summitScale();
        double bowls = GeologyNoise.smoothStep(-.55, .55,
                GeologyNoise.value2(seed ^ 0x5138C9AE671D240BL, x / scale, z / scale));
        double ridges = .5 + .5 * GeologyNoise.value2(seed ^ 0x1AB5489F7EC03264L,
                (x + z * .25) / (scale * .45), z / (scale * .75));
        double weakness = Math.min(1, RockErosionField.susceptibility(resistance, settings.erosion()));
        return config.summitRelief() * gate * (.75 * bowls + .25 * ridges) * (.35 + .65 * weakness);
    }
}
