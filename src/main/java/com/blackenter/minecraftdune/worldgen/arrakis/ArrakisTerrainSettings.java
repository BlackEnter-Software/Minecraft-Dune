package com.blackenter.minecraftdune.worldgen.arrakis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Serialized native Arrakis terrain profile.
 *
 * <p>The profile is part of the chunk-generator codec, so a new world stores the terrain
 * parameters that created it instead of depending on process-global Java constants.</p>
 */
public record ArrakisTerrainSettings(
        int profileVersion,
        BasinSettings basin,
        ForelandSettings foreland,
        MassifSettings massif,
        FaultSettings faults,
        SandPassSettings sandPasses,
        BrokenRockSettings brokenRock,
        OuterTransitionSettings outerTransition,
        NativeDuneSettings nativeDunes
) {
    public static final int CURRENT_PROFILE_VERSION = 510;

    public static final ArrakisTerrainSettings DEFAULT = new ArrakisTerrainSettings(
            CURRENT_PROFILE_VERSION,
            new BasinSettings(800.0, 970.0),
            new ForelandSettings(
                    1150.0,
                    145.0,
                    62.0,
                    30.0,
                    0.08,
                    0.40,
                    0.15,
                    0.42,
                    4.0,
                    28.0,
                    9.0
            ),
            new MassifSettings(
                    1000.0,
                    1250.0,
                    2920.0,
                    3020.0,
                    176,
                    -0.38,
                    -0.02,
                    0.07,
                    0.58
            ),
            new FaultSettings(
                    4,
                    1050.0,
                    1350.0,
                    3400.0,
                    3850.0,
                    30.0,
                    105.0,
                    1150.0,
                    240.0,
                    360.0,
                    90.0,
                    780.0,
                    75.0,
                    0.56
            ),
            new SandPassSettings(
                    1000.0,
                    1320.0,
                    4450.0,
                    4950.0,
                    105.0,
                    225.0,
                    135.0,
                    285.0
            ),
            new BrokenRockSettings(
                    2920.0,
                    3150.0,
                    5200.0,
                    5650.0,
                    430.0,
                    180.0,
                    72.0,
                    67.0,
                    18.0,
                    10.0
            ),
            new OuterTransitionSettings(
                    4450.0,
                    4800.0,
                    6100.0,
                    6500.0,
                    5850.0,
                    6700.0
            ),
            new NativeDuneSettings(
                    30.0,
                    525.0,
                    0.18,
                    3.0,
                    0.20,
                    0.82,
                    24.0,
                    0.12,
                    0.68
            )
    );

    public static final Codec<ArrakisTerrainSettings> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("profile_version")
                            .forGetter(ArrakisTerrainSettings::profileVersion),
                    BasinSettings.CODEC.fieldOf("basin")
                            .forGetter(ArrakisTerrainSettings::basin),
                    ForelandSettings.CODEC.fieldOf("foreland")
                            .forGetter(ArrakisTerrainSettings::foreland),
                    MassifSettings.CODEC.fieldOf("massif")
                            .forGetter(ArrakisTerrainSettings::massif),
                    FaultSettings.CODEC.fieldOf("faults")
                            .forGetter(ArrakisTerrainSettings::faults),
                    SandPassSettings.CODEC.fieldOf("sand_passes")
                            .forGetter(ArrakisTerrainSettings::sandPasses),
                    BrokenRockSettings.CODEC.fieldOf("broken_rock")
                            .forGetter(ArrakisTerrainSettings::brokenRock),
                    OuterTransitionSettings.CODEC.fieldOf("outer_transition")
                            .forGetter(ArrakisTerrainSettings::outerTransition),
                    NativeDuneSettings.CODEC.fieldOf("native_dunes")
                            .forGetter(ArrakisTerrainSettings::nativeDunes)
            ).apply(instance, ArrakisTerrainSettings::new));

    public record BasinSettings(
            double pureSandRadius,
            double transitionEndRadius
    ) {
        public static final Codec<BasinSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("pure_sand_radius")
                                .forGetter(BasinSettings::pureSandRadius),
                        Codec.DOUBLE.fieldOf("transition_end_radius")
                                .forGetter(BasinSettings::transitionEndRadius)
                ).apply(instance, BasinSettings::new));
    }

    public record ForelandSettings(
            double endRadius,
            double largeScale,
            double detailScale,
            double microScale,
            double largeThresholdLow,
            double largeThresholdHigh,
            double microThresholdLow,
            double microThresholdHigh,
            double largeMinHeight,
            double largeMaxHeight,
            double microMaxHeight
    ) {
        public static final Codec<ForelandSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("end_radius").forGetter(ForelandSettings::endRadius),
                        Codec.DOUBLE.fieldOf("large_scale").forGetter(ForelandSettings::largeScale),
                        Codec.DOUBLE.fieldOf("detail_scale").forGetter(ForelandSettings::detailScale),
                        Codec.DOUBLE.fieldOf("micro_scale").forGetter(ForelandSettings::microScale),
                        Codec.DOUBLE.fieldOf("large_threshold_low")
                                .forGetter(ForelandSettings::largeThresholdLow),
                        Codec.DOUBLE.fieldOf("large_threshold_high")
                                .forGetter(ForelandSettings::largeThresholdHigh),
                        Codec.DOUBLE.fieldOf("micro_threshold_low")
                                .forGetter(ForelandSettings::microThresholdLow),
                        Codec.DOUBLE.fieldOf("micro_threshold_high")
                                .forGetter(ForelandSettings::microThresholdHigh),
                        Codec.DOUBLE.fieldOf("large_min_height")
                                .forGetter(ForelandSettings::largeMinHeight),
                        Codec.DOUBLE.fieldOf("large_max_height")
                                .forGetter(ForelandSettings::largeMaxHeight),
                        Codec.DOUBLE.fieldOf("micro_max_height")
                                .forGetter(ForelandSettings::microMaxHeight)
                ).apply(instance, ForelandSettings::new));
    }

    public record MassifSettings(
            double startRadius,
            double fullRadius,
            double outerStartRadius,
            double outerEndRadius,
            int maxAddedHeight,
            double continuityLow,
            double continuityHigh,
            double shapeLow,
            double shapeHigh
    ) {
        public static final Codec<MassifSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("start_radius").forGetter(MassifSettings::startRadius),
                        Codec.DOUBLE.fieldOf("full_radius").forGetter(MassifSettings::fullRadius),
                        Codec.DOUBLE.fieldOf("outer_start_radius")
                                .forGetter(MassifSettings::outerStartRadius),
                        Codec.DOUBLE.fieldOf("outer_end_radius")
                                .forGetter(MassifSettings::outerEndRadius),
                        Codec.INT.fieldOf("max_added_height").forGetter(MassifSettings::maxAddedHeight),
                        Codec.DOUBLE.fieldOf("continuity_low")
                                .forGetter(MassifSettings::continuityLow),
                        Codec.DOUBLE.fieldOf("continuity_high")
                                .forGetter(MassifSettings::continuityHigh),
                        Codec.DOUBLE.fieldOf("shape_low").forGetter(MassifSettings::shapeLow),
                        Codec.DOUBLE.fieldOf("shape_high").forGetter(MassifSettings::shapeHigh)
                ).apply(instance, MassifSettings::new));
    }

    public record FaultSettings(
            int count,
            double startRadius,
            double fullRadius,
            double fadeStartRadius,
            double endRadius,
            double coreWidth,
            double outerWidth,
            double broadWarpScale,
            double broadWarpStrength,
            double mediumWarpScale,
            double mediumWarpStrength,
            double sineWarpScale,
            double sineWarpStrength,
            double sandyFloorThreshold
    ) {
        public static final Codec<FaultSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("count").forGetter(FaultSettings::count),
                        Codec.DOUBLE.fieldOf("start_radius").forGetter(FaultSettings::startRadius),
                        Codec.DOUBLE.fieldOf("full_radius").forGetter(FaultSettings::fullRadius),
                        Codec.DOUBLE.fieldOf("fade_start_radius")
                                .forGetter(FaultSettings::fadeStartRadius),
                        Codec.DOUBLE.fieldOf("end_radius").forGetter(FaultSettings::endRadius),
                        Codec.DOUBLE.fieldOf("core_width").forGetter(FaultSettings::coreWidth),
                        Codec.DOUBLE.fieldOf("outer_width").forGetter(FaultSettings::outerWidth),
                        Codec.DOUBLE.fieldOf("broad_warp_scale")
                                .forGetter(FaultSettings::broadWarpScale),
                        Codec.DOUBLE.fieldOf("broad_warp_strength")
                                .forGetter(FaultSettings::broadWarpStrength),
                        Codec.DOUBLE.fieldOf("medium_warp_scale")
                                .forGetter(FaultSettings::mediumWarpScale),
                        Codec.DOUBLE.fieldOf("medium_warp_strength")
                                .forGetter(FaultSettings::mediumWarpStrength),
                        Codec.DOUBLE.fieldOf("sine_warp_scale")
                                .forGetter(FaultSettings::sineWarpScale),
                        Codec.DOUBLE.fieldOf("sine_warp_strength")
                                .forGetter(FaultSettings::sineWarpStrength),
                        Codec.DOUBLE.fieldOf("sandy_floor_threshold")
                                .forGetter(FaultSettings::sandyFloorThreshold)
                ).apply(instance, FaultSettings::new));
    }

    public record SandPassSettings(
            double startRadius,
            double fullRadius,
            double fadeStartRadius,
            double endRadius,
            double primaryCoreWidth,
            double primaryOuterWidth,
            double secondaryCoreWidth,
            double secondaryOuterWidth
    ) {
        public static final Codec<SandPassSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("start_radius").forGetter(SandPassSettings::startRadius),
                        Codec.DOUBLE.fieldOf("full_radius").forGetter(SandPassSettings::fullRadius),
                        Codec.DOUBLE.fieldOf("fade_start_radius")
                                .forGetter(SandPassSettings::fadeStartRadius),
                        Codec.DOUBLE.fieldOf("end_radius").forGetter(SandPassSettings::endRadius),
                        Codec.DOUBLE.fieldOf("primary_core_width")
                                .forGetter(SandPassSettings::primaryCoreWidth),
                        Codec.DOUBLE.fieldOf("primary_outer_width")
                                .forGetter(SandPassSettings::primaryOuterWidth),
                        Codec.DOUBLE.fieldOf("secondary_core_width")
                                .forGetter(SandPassSettings::secondaryCoreWidth),
                        Codec.DOUBLE.fieldOf("secondary_outer_width")
                                .forGetter(SandPassSettings::secondaryOuterWidth)
                ).apply(instance, SandPassSettings::new));
    }

    public record BrokenRockSettings(
            double startRadius,
            double fullRadius,
            double outerFadeStartRadius,
            double outerRadius,
            double largeScale,
            double detailScale,
            double microScale,
            double maxHeightInner,
            double maxHeightOuter,
            double microMaxHeight
    ) {
        public static final Codec<BrokenRockSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("start_radius").forGetter(BrokenRockSettings::startRadius),
                        Codec.DOUBLE.fieldOf("full_radius").forGetter(BrokenRockSettings::fullRadius),
                        Codec.DOUBLE.fieldOf("outer_fade_start_radius")
                                .forGetter(BrokenRockSettings::outerFadeStartRadius),
                        Codec.DOUBLE.fieldOf("outer_radius").forGetter(BrokenRockSettings::outerRadius),
                        Codec.DOUBLE.fieldOf("large_scale").forGetter(BrokenRockSettings::largeScale),
                        Codec.DOUBLE.fieldOf("detail_scale").forGetter(BrokenRockSettings::detailScale),
                        Codec.DOUBLE.fieldOf("micro_scale").forGetter(BrokenRockSettings::microScale),
                        Codec.DOUBLE.fieldOf("max_height_inner")
                                .forGetter(BrokenRockSettings::maxHeightInner),
                        Codec.DOUBLE.fieldOf("max_height_outer")
                                .forGetter(BrokenRockSettings::maxHeightOuter),
                        Codec.DOUBLE.fieldOf("micro_max_height")
                                .forGetter(BrokenRockSettings::microMaxHeight)
                ).apply(instance, BrokenRockSettings::new));
    }

    public record OuterTransitionSettings(
            double startRadius,
            double fullRadius,
            double fadeStartRadius,
            double outerRadius,
            double openErgStartRadius,
            double openErgFullRadius
    ) {
        public static final Codec<OuterTransitionSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("start_radius")
                                .forGetter(OuterTransitionSettings::startRadius),
                        Codec.DOUBLE.fieldOf("full_radius")
                                .forGetter(OuterTransitionSettings::fullRadius),
                        Codec.DOUBLE.fieldOf("fade_start_radius")
                                .forGetter(OuterTransitionSettings::fadeStartRadius),
                        Codec.DOUBLE.fieldOf("outer_radius")
                                .forGetter(OuterTransitionSettings::outerRadius),
                        Codec.DOUBLE.fieldOf("open_erg_start_radius")
                                .forGetter(OuterTransitionSettings::openErgStartRadius),
                        Codec.DOUBLE.fieldOf("open_erg_full_radius")
                                .forGetter(OuterTransitionSettings::openErgFullRadius)
                ).apply(instance, OuterTransitionSettings::new));
    }

    public record NativeDuneSettings(
            double maxHeight,
            double spacing,
            double spacingVariation,
            double ridgeSharpness,
            double valleyCutoff,
            double slopeAsymmetry,
            double windAngleDegrees,
            double brokenRockWeight,
            double transitionWeight
    ) {
        public static final Codec<NativeDuneSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("max_height").forGetter(NativeDuneSettings::maxHeight),
                        Codec.DOUBLE.fieldOf("spacing").forGetter(NativeDuneSettings::spacing),
                        Codec.DOUBLE.fieldOf("spacing_variation")
                                .forGetter(NativeDuneSettings::spacingVariation),
                        Codec.DOUBLE.fieldOf("ridge_sharpness")
                                .forGetter(NativeDuneSettings::ridgeSharpness),
                        Codec.DOUBLE.fieldOf("valley_cutoff")
                                .forGetter(NativeDuneSettings::valleyCutoff),
                        Codec.DOUBLE.fieldOf("slope_asymmetry")
                                .forGetter(NativeDuneSettings::slopeAsymmetry),
                        Codec.DOUBLE.fieldOf("wind_angle_degrees")
                                .forGetter(NativeDuneSettings::windAngleDegrees),
                        Codec.DOUBLE.fieldOf("broken_rock_weight")
                                .forGetter(NativeDuneSettings::brokenRockWeight),
                        Codec.DOUBLE.fieldOf("transition_weight")
                                .forGetter(NativeDuneSettings::transitionWeight)
                ).apply(instance, NativeDuneSettings::new));
    }
}
