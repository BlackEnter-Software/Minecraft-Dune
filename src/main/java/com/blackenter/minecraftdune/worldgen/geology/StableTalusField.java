package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;
import com.blackenter.minecraftdune.worldgen.geology.TalusColluviumField.Sample;
import com.blackenter.minecraftdune.worldgen.geology.TalusColluviumField.SourceLookup;

/** Revision-3 deposition from the eroded analytical envelope; no deposit cleanup. */
public final class StableTalusField {
    private StableTalusField() {}
    @FunctionalInterface public interface HeightLookup { double top(int x, int z); }

    public static double suitability(double center, double east, double west, double north, double south, double maximumSlope) {
        // Uphill rock beside a toe is shelter, not an unstable downhill recipient slope.
        double downhill = Math.max(0, center - Math.min(Math.min(east, west), Math.min(north, south))) / 2;
        double stable = 1 - GeologyNoise.smoothStep(maximumSlope * .45, maximumSlope * 1.5, downhill);
        double concavity = GeologyNoise.clamp((east + west + north + south - 4 * center) / 24, 0, 1);
        return stable * (1 + .2 * concavity);
    }

    public static double route(int sx, int sz, double sourceTop, int x, int z, double top, HeightLookup terrain) {
        int steps = (int) Math.ceil(Math.hypot(x - sx, z - sz) / 2);
        double obstruction = 0;
        double lowest = sourceTop;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(sx + (x - sx) * t), pz = (int) Math.round(sz + (z - sz) * t);
            double surface = terrain.top(px, pz);
            obstruction = Math.max(obstruction, surface - lowest);
            lowest = Math.min(lowest, surface);
            if (obstruction >= 2) return 0;
        }
        return 1 - GeologyNoise.smoothStep(.5, 2, Math.max(obstruction, top - lowest));
    }

    public static Sample sample(long seed, int x, int z, double top, BuriedRockSettings settings,
            SourceLookup sources, HeightLookup terrain) {
        var config = settings.talus();
        if (!config.enabled() || config.yield() == 0 || config.maximumThickness() == 0) return Sample.NONE;
        double stable = suitability(top, terrain.top(x + 2, z), terrain.top(x - 2, z),
                terrain.top(x, z + 2), terrain.top(x, z - 2), settings.finishing().maximumStableSlope());
        if (stable == 0) return Sample.NONE;
        double sum = 0, distanceSum = 0, best = 0;
        int sourceX = 0, sourceZ = 0, reach = config.reach();
        var material = LithologyField.Material.STONE;
        // Two-block quadrature captures two-block-wide gullies at every integer translation.
        // The area normalization is four times the old four-block stencil, not extra supply.
        for (int gx = Math.floorDiv(x - reach, 2); gx <= Math.floorDiv(x + reach, 2); gx++) {
            for (int gz = Math.floorDiv(z - reach, 2); gz <= Math.floorDiv(z + reach, 2); gz++) {
                int sx = gx * 2, sz = gz * 2;
                double distance = Math.hypot(x - sx, z - sz);
                if (distance < .001 || distance >= reach) continue;
                var source = sources.sample(sx, sz);
                double relief = GeologyNoise.smoothStep(config.minimumRelief(), config.minimumRelief() + 12, source.rockTop() - top);
                double supply = GeologyNoise.smoothStep(config.minimumErosion(), config.minimumErosion() * 2, source.removedAmount());
                if (relief * supply == 0) continue;
                double alignment = (source.outwardX() * (x - sx) + source.outwardZ() * (z - sz)) / distance;
                double contribution = Math.min(config.maximumThickness(), source.removedAmount() * config.yield())
                        * relief * supply * GeologyNoise.smoothStep(.15, .8, alignment)
                        * Math.pow(1 - distance / reach, 2);
                if (contribution < .001) continue;
                contribution *= route(sx, sz, source.rockTop(), x, z, top, terrain);
                sum += contribution;
                distanceSum += contribution * distance;
                if (contribution > best) { best = contribution; sourceX = sx; sourceZ = sz; material = source.material(); }
            }
        }
        double patch = .35 + .65 * GeologyNoise.smoothStep(-.5, .5,
                GeologyNoise.value2(seed ^ 0x1E6B94D235A78FC0L, (x + .5) / 42, (z + .5) / 42));
        double tendency = Math.min(config.maximumThickness(), sum * stable * patch / 12);
        int thickness = (int) Math.floor(tendency + .5);
        if (thickness == 0) return new Sample(0, Integer.MIN_VALUE, sourceX, sourceZ, 0, material, tendency);
        return new Sample((int) Math.floor(top) + 1, (int) Math.floor(top) + thickness,
                sourceX, sourceZ, distanceSum / Math.max(sum, 1e-9) / reach, material, tendency);
    }

    public static LithologyField.Material materialAt(long seed, int x, int y, int z, Sample deposit, double proximalFraction) {
        double clasts = proximalFraction - .4 * GeologyNoise.smoothStep(.2, .9, deposit.distalFraction());
        double patch = GeologyNoise.smoothStep(-.55, .55,
                GeologyNoise.value3(seed ^ 0x65A2E819C4F037BDL, (x + .5) / 9, y / 5.0, (z + .5) / 9));
        return patch < clasts ? deposit.sourceMaterial() : LithologyField.Material.GRAVEL;
    }
}
