package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/** Continuous geological roof above bottom bedrock, including under the graded central basin. */
public final class RawRockSurfaceField {
    private static final long REGIONAL_SALT = 0x429C6FEBD0137A85L;
    private static final long DETAIL_SALT = 0x138DBA725E40C6F9L;
    private RawRockSurfaceField() {}

    public static Sample sample(long seed, double x, double z, ArrakisTerrainSettings terrain) {
        var settings = terrain.buriedRock().rockSurface();
        var structure = MacroGeologyField.structure(seed, x, z, terrain);
        var fault = GeologicalFaultField.sample(seed, x, z, terrain);
        double regional = settings.regionalY()
                + settings.amplitude() * GeologyNoise.value2(seed ^ REGIONAL_SALT, x / settings.scale(), z / settings.scale())
                + settings.detailAmplitude() * GeologyNoise.value2(seed ^ DETAIL_SALT,
                        x / settings.detailScale(), z / settings.detailScale());
        double uplift = structure.shieldWallUplift() * settings.upliftScale();
        double other = structure.otherUplift() * settings.otherReliefScale();
        double displacement = Math.max(uplift, other) + fault.displacement();
        return new Sample(regional, uplift, other, fault, displacement,
                GeologyNoise.clamp(regional + displacement, settings.minimumY(), settings.maximumY()),
                structure.geography());
    }

    public record Sample(double regionalRockTop, double shieldWallUplift, double otherUplift,
            GeologicalFaultField.Sample fault, double structuralDisplacement, double rockTop,
            MacroGeologyField.Sample geography) {}
}
