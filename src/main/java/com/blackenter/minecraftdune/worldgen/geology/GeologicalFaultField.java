package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/** Localized up/down displacement and damage on the existing deterministic fault traces. */
public final class GeologicalFaultField {
    private static final long THROW_SALT = 0x3D8B971AE2065FC4L;
    private GeologicalFaultField() {}

    public static Sample sample(long seed, double x, double z, ArrakisTerrainSettings terrain) {
        var geometry = terrain.faults();
        var settings = terrain.buriedRock().faults();
        double radius = Math.hypot(x, z);
        double gate = GeologyNoise.smoothStep(geometry.startRadius(), geometry.fullRadius(), radius)
                * (1 - GeologyNoise.smoothStep(geometry.fadeStartRadius(), geometry.endRadius(), radius));
        if (gate <= 0 || geometry.count() == 0) return Sample.NONE;
        double displacement = 0, damage = 0, strongest = 0, signed = Double.POSITIVE_INFINITY, strike = 0;
        int dominant = -1;
        for (int index = 0; index < geometry.count(); index++) {
            var trace = MacroGeologyField.faultTrace(seed, x, z, index, geometry);
            double distance = Math.abs(trace.signedDistance());
            double footprint = gate * (1 - GeologyNoise.smoothStep(
                    settings.influenceWidth() * .5, settings.influenceWidth(), distance));
            double throwAmount = settings.maximumThrow() * (.65 + .35 * GeologyNoise.unit(seed, THROW_SALT + index));
            // A signed, smooth tectonic offset; neither side targets a sediment/erosion datum.
            displacement += .5 * throwAmount * Math.tanh(trace.signedDistance() / settings.transitionWidth()) * footprint;
            damage = Math.max(damage, gate * (1 - GeologyNoise.smoothStep(0, settings.damageWidth(), distance)));
            double score = gate * Math.max(0, 1 - distance / settings.influenceWidth());
            if (score > strongest) {
                strongest = score;
                signed = trace.signedDistance();
                strike = trace.strike();
                dominant = index;
            }
        }
        return new Sample(GeologyNoise.clamp(displacement, -settings.maximumThrow(), settings.maximumThrow()),
                damage, signed, dominant < 0 ? 0 : (int) Math.signum(signed), strike, dominant);
    }

    public record Sample(double displacement, double damage, double signedDistance, int side,
            double strikeRadians, int faultIndex) {
        public static final Sample NONE = new Sample(0, 0, Double.POSITIVE_INFINITY, 0, 0, -1);
    }
}
