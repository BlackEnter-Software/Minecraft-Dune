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
        LithologySettings lithology,
        FractureSettings fractures,
        SandPassSettings sandPasses,
        BrokenRockSettings brokenRock,
        OuterTransitionSettings outerTransition,
        NativeDuneSettings nativeDunes
) {
    public static final int CURRENT_PROFILE_VERSION = 513;

    public static final MaterialPaletteSettings DEFAULT_MATERIALS =
            new MaterialPaletteSettings(
                    "minecraft:stone",
                    "minecraft:sandstone",
                    "minecraft:tuff",
                    "create:limestone",
                    "minecraft:sandstone",
                    "minecraft:calcite",
                    "minecraft:andesite",
                    "minecraft:diorite",
                    "minecraft:basalt",
                    "minecraft:blackstone",
                    "minecraft:gravel"
            );

    public static final TalusSettings DEFAULT_TALUS =
            new TalusSettings(false, 0.78, 3, 5.0);

    public static final LithologySettings DEFAULT_LITHOLOGY =
            new LithologySettings(
                    260.0,
                    46.0,
                    28.0,
                    210.0,
                    16.0,
                    190.0,
                    0.58,
                    420.0,
                    0.70,
                    0.86,
                    180.0,
                    2.5,
                    86.0,
                    1.1,
                    DEFAULT_MATERIALS,
                    DEFAULT_TALUS
            );

    public static final FractureSettings DEFAULT_FRACTURES =
            new FractureSettings(
                    true,
                    520.0,
                    0.72,
                    72.0,
                    230.0,
                    0.72,
                    1.0,
                    12.0,
                    5.0,
                    68.0,
                    18.0,
                    0.18,
                    0.24,
                    1.5,
                    0.30,
                    0.22
            );

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
                    9.0,
                    0.25,
                    0.18,
                    1.0
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
                    0.56,
                    4.0
            ),
            DEFAULT_LITHOLOGY,
            DEFAULT_FRACTURES,
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
                    10.0,
                    1.0
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
                    0.16,
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
                    LithologySettings.CODEC.optionalFieldOf(
                                    "lithology",
                                    DEFAULT_LITHOLOGY
                            )
                            .forGetter(ArrakisTerrainSettings::lithology),
                    FractureSettings.CODEC.optionalFieldOf(
                                    "fractures",
                                    DEFAULT_FRACTURES
                            )
                            .forGetter(ArrakisTerrainSettings::fractures),
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
            double microMaxHeight,
            double innerHeightScale,
            double innerThresholdBoost,
            double growthPower
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
                                .forGetter(ForelandSettings::microMaxHeight),
                        Codec.DOUBLE.optionalFieldOf("inner_height_scale", 0.25)
                                .forGetter(ForelandSettings::innerHeightScale),
                        Codec.DOUBLE.optionalFieldOf("inner_threshold_boost", 0.18)
                                .forGetter(ForelandSettings::innerThresholdBoost),
                        Codec.DOUBLE.optionalFieldOf("growth_power", 1.0)
                                .forGetter(ForelandSettings::growthPower)
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
            double sandyFloorThreshold,
            double rockyFloorHeight
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
                                .forGetter(FaultSettings::sandyFloorThreshold),
                        Codec.DOUBLE.optionalFieldOf("rocky_floor_height", 4.0)
                                .forGetter(FaultSettings::rockyFloorHeight)
                ).apply(instance, FaultSettings::new));
    }

    /**
     * Coherent native-rock units. Horizontal/vertical scales control geological bodies,
     * while the dike and vein settings control thin structural features. Block identifiers
     * are data, so optional mod materials never become compile-time dependencies.
     */
    public record LithologySettings(
            double unitHorizontalScale,
            double unitVerticalScale,
            double strataThickness,
            double strataWarpScale,
            double strataWarpStrength,
            double intrusionScale,
            double intrusionThreshold,
            double rareBodyScale,
            double limestoneThreshold,
            double blackstoneThreshold,
            double dikeSpacing,
            double dikeHalfWidth,
            double calciteVeinSpacing,
            double calciteVeinHalfWidth,
            MaterialPaletteSettings materials,
            TalusSettings talus
    ) {
        public static final Codec<LithologySettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.optionalFieldOf("unit_horizontal_scale", 260.0)
                                .forGetter(LithologySettings::unitHorizontalScale),
                        Codec.DOUBLE.optionalFieldOf("unit_vertical_scale", 46.0)
                                .forGetter(LithologySettings::unitVerticalScale),
                        Codec.DOUBLE.optionalFieldOf("strata_thickness", 28.0)
                                .forGetter(LithologySettings::strataThickness),
                        Codec.DOUBLE.optionalFieldOf("strata_warp_scale", 210.0)
                                .forGetter(LithologySettings::strataWarpScale),
                        Codec.DOUBLE.optionalFieldOf("strata_warp_strength", 16.0)
                                .forGetter(LithologySettings::strataWarpStrength),
                        Codec.DOUBLE.optionalFieldOf("intrusion_scale", 190.0)
                                .forGetter(LithologySettings::intrusionScale),
                        Codec.DOUBLE.optionalFieldOf("intrusion_threshold", 0.58)
                                .forGetter(LithologySettings::intrusionThreshold),
                        Codec.DOUBLE.optionalFieldOf("rare_body_scale", 420.0)
                                .forGetter(LithologySettings::rareBodyScale),
                        Codec.DOUBLE.optionalFieldOf("limestone_threshold", 0.70)
                                .forGetter(LithologySettings::limestoneThreshold),
                        Codec.DOUBLE.optionalFieldOf("blackstone_threshold", 0.86)
                                .forGetter(LithologySettings::blackstoneThreshold),
                        Codec.DOUBLE.optionalFieldOf("dike_spacing", 180.0)
                                .forGetter(LithologySettings::dikeSpacing),
                        Codec.DOUBLE.optionalFieldOf("dike_half_width", 2.5)
                                .forGetter(LithologySettings::dikeHalfWidth),
                        Codec.DOUBLE.optionalFieldOf("calcite_vein_spacing", 86.0)
                                .forGetter(LithologySettings::calciteVeinSpacing),
                        Codec.DOUBLE.optionalFieldOf("calcite_vein_half_width", 1.1)
                                .forGetter(LithologySettings::calciteVeinHalfWidth),
                        MaterialPaletteSettings.CODEC.optionalFieldOf(
                                        "materials",
                                        DEFAULT_MATERIALS
                                )
                                .forGetter(LithologySettings::materials),
                        TalusSettings.CODEC.optionalFieldOf("talus", DEFAULT_TALUS)
                                .forGetter(LithologySettings::talus)
                ).apply(instance, LithologySettings::new));
    }

    public record MaterialPaletteSettings(
            String background,
            String sandstone,
            String tuff,
            String limestone,
            String limestoneFallback,
            String calcite,
            String andesite,
            String diorite,
            String basalt,
            String blackstone,
            String talus
    ) {
        public static final Codec<MaterialPaletteSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("background", "minecraft:stone")
                                .forGetter(MaterialPaletteSettings::background),
                        Codec.STRING.optionalFieldOf("sandstone", "minecraft:sandstone")
                                .forGetter(MaterialPaletteSettings::sandstone),
                        Codec.STRING.optionalFieldOf("tuff", "minecraft:tuff")
                                .forGetter(MaterialPaletteSettings::tuff),
                        Codec.STRING.optionalFieldOf("limestone", "create:limestone")
                                .forGetter(MaterialPaletteSettings::limestone),
                        Codec.STRING.optionalFieldOf(
                                        "limestone_fallback",
                                        "minecraft:sandstone"
                                )
                                .forGetter(MaterialPaletteSettings::limestoneFallback),
                        Codec.STRING.optionalFieldOf("calcite", "minecraft:calcite")
                                .forGetter(MaterialPaletteSettings::calcite),
                        Codec.STRING.optionalFieldOf("andesite", "minecraft:andesite")
                                .forGetter(MaterialPaletteSettings::andesite),
                        Codec.STRING.optionalFieldOf("diorite", "minecraft:diorite")
                                .forGetter(MaterialPaletteSettings::diorite),
                        Codec.STRING.optionalFieldOf("basalt", "minecraft:basalt")
                                .forGetter(MaterialPaletteSettings::basalt),
                        Codec.STRING.optionalFieldOf("blackstone", "minecraft:blackstone")
                                .forGetter(MaterialPaletteSettings::blackstone),
                        Codec.STRING.optionalFieldOf("talus", "minecraft:gravel")
                                .forGetter(MaterialPaletteSettings::talus)
                ).apply(instance, MaterialPaletteSettings::new));
    }

    /** Reserved 0.5.14 scree controls; generation stays disabled by default in 0.5.13. */
    public record TalusSettings(
            boolean localScreeEnabled,
            double minimumFractureStrength,
            int maximumThickness,
            double spread
    ) {
        public static final Codec<TalusSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("local_scree_enabled", false)
                                .forGetter(TalusSettings::localScreeEnabled),
                        Codec.DOUBLE.optionalFieldOf("minimum_fracture_strength", 0.78)
                                .forGetter(TalusSettings::minimumFractureStrength),
                        Codec.INT.optionalFieldOf("maximum_thickness", 3)
                                .forGetter(TalusSettings::maximumThickness),
                        Codec.DOUBLE.optionalFieldOf("spread", 5.0)
                                .forGetter(TalusSettings::spread)
                ).apply(instance, TalusSettings::new));
    }

    /**
     * Massif-top fissures, separate from the kilometre-scale regional fault network.
     * `cellSize` is retained as a backwards-compatible JSON name but now represents the
     * spacing of continuous primary traces and their branch-node lattice.
     */
    public record FractureSettings(
            boolean enabled,
            double cellSize,
            double density,
            double minimumLength,
            double maximumLength,
            double branchChance,
            double minimumWidth,
            double maximumWidth,
            double minimumDepth,
            double maximumDepth,
            double minimumRockHeight,
            double minimumMassifWeight,
            double mineralizationChance,
            double calciteWallThickness,
            double resistanceWidthInfluence,
            double resistanceDepthInfluence
    ) {
        public static final Codec<FractureSettings> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("enabled", true)
                                .forGetter(FractureSettings::enabled),
                        Codec.DOUBLE.optionalFieldOf("cell_size", 520.0)
                                .forGetter(FractureSettings::cellSize),
                        Codec.DOUBLE.optionalFieldOf("density", 0.72)
                                .forGetter(FractureSettings::density),
                        Codec.DOUBLE.optionalFieldOf("minimum_length", 72.0)
                                .forGetter(FractureSettings::minimumLength),
                        Codec.DOUBLE.optionalFieldOf("maximum_length", 230.0)
                                .forGetter(FractureSettings::maximumLength),
                        Codec.DOUBLE.optionalFieldOf("branch_chance", 0.72)
                                .forGetter(FractureSettings::branchChance),
                        Codec.DOUBLE.optionalFieldOf("minimum_width", 1.0)
                                .forGetter(FractureSettings::minimumWidth),
                        Codec.DOUBLE.optionalFieldOf("maximum_width", 12.0)
                                .forGetter(FractureSettings::maximumWidth),
                        Codec.DOUBLE.optionalFieldOf("minimum_depth", 5.0)
                                .forGetter(FractureSettings::minimumDepth),
                        Codec.DOUBLE.optionalFieldOf("maximum_depth", 68.0)
                                .forGetter(FractureSettings::maximumDepth),
                        Codec.DOUBLE.optionalFieldOf("minimum_rock_height", 18.0)
                                .forGetter(FractureSettings::minimumRockHeight),
                        Codec.DOUBLE.optionalFieldOf("minimum_massif_weight", 0.18)
                                .forGetter(FractureSettings::minimumMassifWeight),
                        Codec.DOUBLE.optionalFieldOf("mineralization_chance", 0.24)
                                .forGetter(FractureSettings::mineralizationChance),
                        Codec.DOUBLE.optionalFieldOf("calcite_wall_thickness", 1.5)
                                .forGetter(FractureSettings::calciteWallThickness),
                        Codec.DOUBLE.optionalFieldOf("resistance_width_influence", 0.30)
                                .forGetter(FractureSettings::resistanceWidthInfluence),
                        Codec.DOUBLE.optionalFieldOf("resistance_depth_influence", 0.22)
                                .forGetter(FractureSettings::resistanceDepthInfluence)
                ).apply(instance, FractureSettings::new));
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
            double microMaxHeight,
            double sizeDecayPower
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
                                .forGetter(BrokenRockSettings::microMaxHeight),
                        Codec.DOUBLE.optionalFieldOf("size_decay_power", 1.0)
                                .forGetter(BrokenRockSettings::sizeDecayPower)
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
            double forelandWeight,
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
                        Codec.DOUBLE.optionalFieldOf("foreland_weight", 0.16)
                                .forGetter(NativeDuneSettings::forelandWeight),
                        Codec.DOUBLE.fieldOf("broken_rock_weight")
                                .forGetter(NativeDuneSettings::brokenRockWeight),
                        Codec.DOUBLE.fieldOf("transition_weight")
                                .forGetter(NativeDuneSettings::transitionWeight)
                ).apply(instance, NativeDuneSettings::new));
    }
}
