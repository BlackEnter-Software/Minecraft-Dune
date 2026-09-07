package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;

/** Finite downslope transport stencil over eroded surfaces; no structural-contact search. */
public final class TalusColluviumField {
    private static final long PATCH_SALT = 0x1E6B94D235A78FC0L;
    private static final long MATERIAL_SALT = 0x65A2E819C4F037BDL;
    private static final int[][] DIRECTIONS = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    private TalusColluviumField() {}

    public static Sample sample(long seed, int x, int z, double externalTop, BuriedRockSettings.Talus settings,
            SourceLookup sources) {
        if (!settings.enabled() || settings.maximumThickness() == 0 || settings.yield() == 0) return Sample.NONE;
        if (settings.coherentSources()) return coherent(seed, x, z, externalTop, settings, sources);
        double best = 0, bestDistance = 0;
        int sourceX = x, sourceZ = z;
        LithologyField.Material material = LithologyField.Material.STONE;
        double patch = .72 + .28 * (.5 + .5 * GeologyNoise.value2(seed ^ PATCH_SALT, (x + .5) / 42, (z + .5) / 42));
        for (int[] direction : DIRECTIONS) {
            for (int step = 2; step <= settings.reach(); step += 2) {
                double distance = step * Math.hypot(direction[0], direction[1]);
                if (distance > settings.reach()) break;
                int sx = x + direction[0] * step, sz = z + direction[1] * step;
                Source source = sources.sample(sx, sz);
                double relief = source.rockTop() - externalTop;
                if (source.removedAmount() < settings.minimumErosion() || relief < settings.minimumRelief()) continue;
                double downhill = (source.outwardX() * (x - sx) + source.outwardZ() * (z - sz)) / distance;
                if (downhill <= .35) continue;
                double falloff = Math.pow(1 - distance / (settings.reach() + 1.0), 1.6);
                double supply = Math.min(settings.maximumThickness(), source.removedAmount() * settings.yield());
                double height = supply * falloff * downhill * patch
                        * GeologyNoise.smoothStep(settings.minimumRelief(), settings.minimumRelief() + 12, relief);
                if (height > best) {
                    best = height; bestDistance = distance; sourceX = sx; sourceZ = sz; material = source.material();
                }
            }
        }
        int thickness = Math.min(settings.maximumThickness(), (int) Math.floor(best + .5));
        if (thickness == 0) return Sample.NONE;
        int bottom = (int) Math.floor(externalTop) + 1;
        return new Sample(bottom, (int) Math.ceil(externalTop) + thickness, sourceX, sourceZ,
                bestDistance / settings.reach(), material);
    }

    public static LithologyField.Material materialAt(long seed, int x, int y, int z, Sample deposit) {
        double noise = GeologyNoise.value3(seed ^ MATERIAL_SALT, (x + .5) / 14, y / 7.0, (z + .5) / 14);
        return noise < -.24 ? deposit.sourceMaterial() : LithologyField.Material.GRAVEL;
    }

    /** Fixed world-space source nodes with compact smooth kernels. Neighbor recipients see
     * the same sources rather than shifting eight rays and selecting unrelated maxima. */
    private static Sample coherent(long seed, int x, int z, double externalTop, BuriedRockSettings.Talus settings,
            SourceLookup sources) {
        int spacing = 4, reach = settings.reach();
        double sum = 0, weightedDistance = 0, best = 0;
        int sourceX = 0, sourceZ = 0;
        LithologyField.Material material = LithologyField.Material.STONE;
        for (int gx = Math.floorDiv(x - reach, spacing); gx <= Math.floorDiv(x + reach, spacing); gx++) {
            for (int gz = Math.floorDiv(z - reach, spacing); gz <= Math.floorDiv(z + reach, spacing); gz++) {
                int sx = gx * spacing, sz = gz * spacing;
                double distance = Math.hypot(x - sx, z - sz);
                if (distance >= reach || distance < .001) continue;
                var source = sources.sample(sx, sz);
                double relief = source.rockTop() - externalTop;
                double reliefGate = GeologyNoise.smoothStep(settings.minimumRelief(), settings.minimumRelief() + 12, relief);
                double supplyGate = GeologyNoise.smoothStep(settings.minimumErosion(), settings.minimumErosion() * 2, source.removedAmount());
                double downhill = (source.outwardX() * (x - sx) + source.outwardZ() * (z - sz)) / distance;
                double direction = GeologyNoise.smoothStep(.15, .8, downhill);
                double kernel = Math.pow(1 - distance / reach, 2);
                double supply = Math.min(settings.maximumThickness(), source.removedAmount() * settings.yield());
                double contribution = supply * supplyGate * reliefGate * direction * kernel;
                sum += contribution;
                weightedDistance += contribution * distance;
                // The dominant source supplies clast identity only, never deposit height.
                if (contribution > best) { best = contribution; sourceX = sx; sourceZ = sz; material = source.material(); }
            }
        }
        double patch = .72 + .28 * (.5 + .5 * GeologyNoise.value2(seed ^ PATCH_SALT, (x + .5) / 42, (z + .5) / 42));
        // Normalize over a small source region; one sample cannot create a tall isolated post.
        double tendency = Math.min(settings.maximumThickness(), sum * patch / 3);
        int thickness = (int) Math.floor(tendency + .5);
        if (thickness == 0) return new Sample(0, Integer.MIN_VALUE, sourceX, sourceZ, 0, material, tendency);
        return new Sample((int) Math.floor(externalTop) + 1, (int) Math.ceil(externalTop) + thickness,
                sourceX, sourceZ, weightedDistance / Math.max(1e-9, sum) / reach, material, tendency);
    }

    public static boolean isDistalSand(long seed, int x, int y, int z, Sample deposit) {
        return deposit.distalFraction() > .68
                && GeologyNoise.value3(seed ^ MATERIAL_SALT, (x + .5) / 14, y / 7.0, (z + .5) / 14) < .1;
    }

    @FunctionalInterface public interface SourceLookup { Source sample(int x, int z); }
    public record Source(double rockTop, double removedAmount, double outwardX, double outwardZ,
            LithologyField.Material material) {}
    public record Sample(int bottomY, int topY, int sourceX, int sourceZ, double distalFraction,
            LithologyField.Material sourceMaterial, double tendency) {
        public Sample(int bottomY, int topY, int sourceX, int sourceZ, double distalFraction,
                LithologyField.Material sourceMaterial) {
            this(bottomY, topY, sourceX, sourceZ, distalFraction, sourceMaterial, Math.max(0, (double) topY - bottomY + 1));
        }
        public static final Sample NONE = new Sample(0, Integer.MIN_VALUE, 0, 0, 0, LithologyField.Material.STONE);
        public boolean active() { return topY >= bottomY; }
    }
}
