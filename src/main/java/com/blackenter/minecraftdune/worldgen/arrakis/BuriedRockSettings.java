package com.blackenter.minecraftdune.worldgen.arrakis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Profile-6000 controls. No native-root, occupancy-repair, or concealment settings. */
public record BuriedRockSettings(RockSurface rockSurface, Sediment sediment,
        FaultDisplacement faults, Erosion erosion, Talus talus) {
    public static final RockSurface DEFAULT_ROCK = new RockSurface(36, 14, 1200, 4, 280, 1.15, 1, -48, 300);
    public static final Sediment DEFAULT_SEDIMENT = new Sediment(64, 3, 2400, 8);
    public static final FaultDisplacement DEFAULT_FAULTS = new FaultDisplacement(32, 12, 220, 40);
    public static final Erosion DEFAULT_EROSION = new Erosion(true, 18, 18, 6, .42, .58,
            1.35, .58, .28, .34, 18, 6, 4, 1, .7, .32);
    public static final Talus DEFAULT_TALUS = new Talus(true, .4, 4, 16, .75, 8);
    public static final BuriedRockSettings DEFAULT = new BuriedRockSettings(
            DEFAULT_ROCK, DEFAULT_SEDIMENT, DEFAULT_FAULTS, DEFAULT_EROSION, DEFAULT_TALUS);

    public static final Codec<BuriedRockSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            RockSurface.CODEC.optionalFieldOf("rock_surface", DEFAULT_ROCK).forGetter(BuriedRockSettings::rockSurface),
            Sediment.CODEC.optionalFieldOf("sediment", DEFAULT_SEDIMENT).forGetter(BuriedRockSettings::sediment),
            FaultDisplacement.CODEC.optionalFieldOf("fault_displacement", DEFAULT_FAULTS).forGetter(BuriedRockSettings::faults),
            Erosion.CODEC.optionalFieldOf("erosion", DEFAULT_EROSION).forGetter(BuriedRockSettings::erosion),
            Talus.CODEC.optionalFieldOf("talus", DEFAULT_TALUS).forGetter(BuriedRockSettings::talus)
    ).apply(i, BuriedRockSettings::new));

    private static Codec<Double> number(double low, double high) {
        return Codec.DOUBLE.flatXmap(v -> Double.isFinite(v) && v >= low && v <= high
                ? DataResult.success(v) : DataResult.error(() -> "Expected finite value in [" + low + ", " + high + "]: " + v),
                v -> Double.isFinite(v) && v >= low && v <= high
                        ? DataResult.success(v) : DataResult.error(() -> "Invalid buried-rock setting: " + v));
    }

    public record RockSurface(double regionalY, double amplitude, double scale, double detailAmplitude,
            double detailScale, double upliftScale, double otherReliefScale, int minimumY, int maximumY) {
        public static final Codec<RockSurface> CODEC = RecordCodecBuilder.<RockSurface>create(i -> i.group(
                number(-32, 100).optionalFieldOf("regional_y", 36.0).forGetter(RockSurface::regionalY),
                number(0, 48).optionalFieldOf("amplitude", 14.0).forGetter(RockSurface::amplitude),
                number(64, 10000).optionalFieldOf("scale", 1200.0).forGetter(RockSurface::scale),
                number(0, 16).optionalFieldOf("detail_amplitude", 4.0).forGetter(RockSurface::detailAmplitude),
                number(16, 2000).optionalFieldOf("detail_scale", 280.0).forGetter(RockSurface::detailScale),
                number(0, 2).optionalFieldOf("uplift_scale", 1.15).forGetter(RockSurface::upliftScale),
                number(0, 3).optionalFieldOf("other_relief_scale", 1.0).forGetter(RockSurface::otherReliefScale),
                Codec.intRange(-48, 32).optionalFieldOf("minimum_y", -48).forGetter(RockSurface::minimumY),
                Codec.intRange(128, 300).optionalFieldOf("maximum_y", 300).forGetter(RockSurface::maximumY)
        ).apply(i, RockSurface::new));
    }

    public record Sediment(double datum, double relief, double scale, int compactionDepth) {
        public static final Codec<Sediment> CODEC = RecordCodecBuilder.create(i -> i.group(
                number(32, 96).optionalFieldOf("datum", 64.0).forGetter(Sediment::datum),
                number(0, 12).optionalFieldOf("relief", 3.0).forGetter(Sediment::relief),
                number(64, 10000).optionalFieldOf("scale", 2400.0).forGetter(Sediment::scale),
                Codec.intRange(1, 32).optionalFieldOf("compaction_depth", 8).forGetter(Sediment::compactionDepth)
        ).apply(i, Sediment::new));
    }

    public record FaultDisplacement(double maximumThrow, double transitionWidth,
            double influenceWidth, double damageWidth) {
        public static final Codec<FaultDisplacement> CODEC = RecordCodecBuilder.<FaultDisplacement>create(i -> i.group(
                number(0, 96).optionalFieldOf("maximum_throw", 32.0).forGetter(FaultDisplacement::maximumThrow),
                number(1, 64).optionalFieldOf("transition_width", 12.0).forGetter(FaultDisplacement::transitionWidth),
                number(16, 1000).optionalFieldOf("influence_width", 220.0).forGetter(FaultDisplacement::influenceWidth),
                number(1, 128).optionalFieldOf("damage_width", 40.0).forGetter(FaultDisplacement::damageWidth)
        ).apply(i, FaultDisplacement::new)).validate(v -> v.influenceWidth >= Math.max(v.transitionWidth, v.damageWidth)
                ? DataResult.success(v) : DataResult.error(() -> "Fault influence_width must cover transition_width and damage_width"));
    }

    public record Erosion(boolean enabled, double minimumRelief, double probeDistance,
            double maximumRecession, double windStrength, double fractureStrength,
            double softMultiplier, double hardMultiplier, double veryHardMultiplier,
            double surfaceStrength, double coarseScale, double detailScale, int surfaceRetreat,
            double incisionScale, double faultWeakness, double edgeThreshold) {
        public static final Codec<Erosion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Erosion::enabled),
                number(4, 96).optionalFieldOf("minimum_relief", 18.0).forGetter(Erosion::minimumRelief),
                number(6, 32).optionalFieldOf("probe_distance", 18.0).forGetter(Erosion::probeDistance),
                number(0, 16).optionalFieldOf("maximum_recession", 6.0).forGetter(Erosion::maximumRecession),
                number(0, 2).optionalFieldOf("wind_strength", .42).forGetter(Erosion::windStrength),
                number(0, 2).optionalFieldOf("fracture_strength", .58).forGetter(Erosion::fractureStrength),
                number(0, 3).optionalFieldOf("soft_multiplier", 1.35).forGetter(Erosion::softMultiplier),
                number(0, 2).optionalFieldOf("hard_multiplier", .58).forGetter(Erosion::hardMultiplier),
                number(0, 2).optionalFieldOf("very_hard_multiplier", .28).forGetter(Erosion::veryHardMultiplier),
                number(0, 1.5).optionalFieldOf("surface_strength", .34).forGetter(Erosion::surfaceStrength),
                number(6, 256).optionalFieldOf("coarse_scale", 18.0).forGetter(Erosion::coarseScale),
                number(2, 128).optionalFieldOf("detail_scale", 6.0).forGetter(Erosion::detailScale),
                Codec.intRange(0, 8).optionalFieldOf("surface_retreat", 4).forGetter(Erosion::surfaceRetreat),
                number(0, 2).optionalFieldOf("incision_scale", 1.0).forGetter(Erosion::incisionScale),
                number(0, 2).optionalFieldOf("fault_weakness", .7).forGetter(Erosion::faultWeakness),
                number(.05, .9).optionalFieldOf("edge_threshold", .32).forGetter(Erosion::edgeThreshold)
        ).apply(i, Erosion::new));
    }

    public record Talus(boolean enabled, double yield, int maximumThickness, int reach,
            double minimumErosion, double minimumRelief) {
        public static final Codec<Talus> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Talus::enabled),
                number(0, 1).optionalFieldOf("yield", .4).forGetter(Talus::yield),
                Codec.intRange(0, 8).optionalFieldOf("maximum_thickness", 4).forGetter(Talus::maximumThickness),
                Codec.intRange(2, 24).optionalFieldOf("reach", 16).forGetter(Talus::reach),
                number(.01, 16).optionalFieldOf("minimum_erosion", .75).forGetter(Talus::minimumErosion),
                number(2, 48).optionalFieldOf("minimum_relief", 8.0).forGetter(Talus::minimumRelief)
        ).apply(i, Talus::new));
    }
}
