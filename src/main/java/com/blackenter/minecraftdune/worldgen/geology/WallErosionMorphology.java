package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;

/** Coherent erosion potential in a seam-free circular wall frame, not a prescribed cliff. */
public final class WallErosionMorphology {
    private static final long SECTOR = 0x38BA947DC162E05FL;
    private static final long MESO = 0x91DE36A57C40B82FL;
    private static final long GULLY = 0x672EA4C1B903FD85L;
    private WallErosionMorphology() {}

    public static Sample sample(long seed, double x, double z, double rawTop, double sediment,
            RockFaceExposure.Sample face, MacroGeologyField.Sample geography, ArrakisTerrainSettings terrain) {
        var config = terrain.buriedRock().erosion().morphology();
        // The legacy exposed flag describes a steep cliff, not all externally exposed rock.
        // The roof/sediment comparison and continuous envelope response below also admit slopes.
        if (!config.enabled() || rawTop <= sediment) return Sample.NONE;
        var contact = ScarpMorphologyField.nearestMassifLowSideContact(seed, x, z,
                geography.radiusBlocks(), geography.effectiveRadiusBlocks(), terrain.massif());
        if (!contact.valid()) return Sample.NONE;
        boolean inner = contact.inwardX() * x + contact.inwardZ() * z > 0;
        double referenceRadius = inner ? terrain.massif().startRadius()
                : terrain.massif().outerStartRadius() + terrain.massif().outerScarpWidth();
        var pattern = pattern(seed, x, z, referenceRadius, inner, config);
        // Ownership only selects this erosion style; actual neighboring heights grant exposure.
        double wall = GeologyNoise.smoothStep(.04, .45, geography.physicalMassifWeight());
        double alignment = Math.max(0, -contact.inwardX() * face.outwardNormalX()
                - contact.inwardZ() * face.outwardNormalZ());
        double gate = wall * GeologyNoise.smoothStep(.05, .3, face.steepness())
                * GeologyNoise.smoothStep(4, 18, face.localRelief())
                * GeologyNoise.smoothStep(0, 12, rawTop - sediment)
                * (.4 + .6 * alignment);
        if (gate <= 0) return Sample.NONE;
        return new Sample(pattern.sector() * config.sectorRecession() * gate,
                pattern.mesoscale() * config.mesoscaleRecession() * gate,
                pattern.gully() * config.gullyDepth() * gate, gate);
    }

    /** Public analytical potential for statistical/seam fixtures; no terrain writes or caches. */
    public static Pattern pattern(long seed, double x, double z, double referenceRadius,
            boolean inner, BuriedRockSettings.Morphology config) {
        double radius = Math.hypot(x, z);
        double theta = Math.atan2(z, x);
        double downslope = (radius - referenceRadius) * (inner ? -1 : 1);
        double scale = config.sectorScale();
        double warp = GeologyNoise.value3(seed ^ MESO, x / (scale * 2), downslope / 420, z / (scale * 2));
        double angle = theta + warp * scale * .3 / Math.max(1, referenceRadius);
        double tx = Math.cos(angle) * referenceRadius, tz = Math.sin(angle) * referenceRadius;
        double broad = .65 * GeologyNoise.value3(seed ^ SECTOR, tx / scale, downslope / 600, tz / scale)
                + .35 * GeologyNoise.value3(seed ^ (SECTOR + 1), tx / (scale * .48), downslope / 300, tz / (scale * .48));
        double sector = Math.pow(GeologyNoise.smoothStep(-.4, .45, broad), 1.6);
        double meso = GeologyNoise.smoothStep(-.35, .5,
                GeologyNoise.value3(seed ^ MESO, tx / (scale * .28), downslope / 130, tz / (scale * .28)));

        // Periodic cell identifiers close the atan2 seam. Jittered centers are not a sine comb.
        int count = Math.max(8, (int) Math.round(2 * Math.PI * referenceRadius / config.gullySpacing()));
        double u = theta / (2 * Math.PI) * count;
        int cell = (int) Math.floor(u);
        double spacing = 2 * Math.PI * referenceRadius / count;
        double gully = 0;
        for (int i = cell - 2; i <= cell + 2; i++) {
            long channel = GeologyNoise.cellSeed(seed, Math.floorMod(i, count), inner ? 0 : 1, GULLY);
            double wander = .3 * GeologyNoise.value2(channel, downslope / 90, 1)
                    + .1 * GeologyNoise.value2(channel ^ MESO, downslope / 27, 1);
            double center = i + .2 + .6 * GeologyNoise.unit(channel, 1) + wander;
            double width = spacing * (.10 + .13 * GeologyNoise.unit(channel, 2))
                    * (.8 + .2 * GeologyNoise.value2(channel, downslope / 55, 2));
            double signal = channelShape(Math.abs(u - center) * spacing, width);
            if (GeologyNoise.unit(channel, 3) > .45) {
                double merge = GeologyNoise.signed(channel, 4) * 140;
                double branch = .45 * (1 - GeologyNoise.smoothStep(merge - 70, merge + 20, downslope));
                signal = Math.max(signal, .8 * channelShape(Math.abs(u - center - branch) * spacing, width * .7));
            }
            double continuity = GeologyNoise.smoothStep(-.4, .15,
                    GeologyNoise.value2(channel ^ SECTOR, downslope / 180, 3));
            gully = Math.max(gully, signal * continuity * (.55 + .45 * GeologyNoise.unit(channel, 5)));
        }
        return new Pattern(sector, meso, gully);
    }

    private static double channelShape(double distance, double width) {
        return 1 - GeologyNoise.smoothStep(width * .15, width, distance);
    }

    public record Pattern(double sector, double mesoscale, double gully) {}
    public record Sample(double sectorRecession, double mesoscaleRecession, double gullyDepth, double exposureGate) {
        public static final Sample NONE = new Sample(0, 0, 0, 0);
    }
}
