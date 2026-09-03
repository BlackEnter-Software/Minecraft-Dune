package com.blackenter.minecraftdune.worldgen.arrakis;

import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.BasinSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.BrokenRockSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.ErosionSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.FaultMorphologySettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.FaultSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.ForelandSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.FrontShellCleanupSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.FractureSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.LithologySettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.MassifSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.MaterialPaletteSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.NativeDuneSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.OrphanRemnantSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.OuterTransitionSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.SandPassSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.SurfaceErosionSettings;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings.TalusSettings;

/** Semantic profile validation shared by codec encoding and decoding. */
final class ArrakisTerrainSettingsValidator {
    private ArrakisTerrainSettingsValidator() {
    }

    static DataResult<ArrakisTerrainSettings> validate(ArrakisTerrainSettings settings) {
        Validation validation = new Validation();
        validation.integer("profile_version", settings.profileVersion(), 0, 999_999);

        BasinSettings basin = settings.basin();
        validation.radius("basin.pure_sand_radius", basin.pureSandRadius());
        validation.radius("basin.transition_end_radius", basin.transitionEndRadius());
        validation.ordered(
                "basin radii",
                basin.pureSandRadius(),
                basin.transitionEndRadius()
        );

        ForelandSettings foreland = settings.foreland();
        validation.radius("foreland.end_radius", foreland.endRadius());
        validation.scale("foreland.large_scale", foreland.largeScale());
        validation.scale("foreland.detail_scale", foreland.detailScale());
        validation.scale("foreland.micro_scale", foreland.microScale());
        validation.finite("foreland.large_threshold_low", foreland.largeThresholdLow());
        validation.finite("foreland.large_threshold_high", foreland.largeThresholdHigh());
        validation.finite("foreland.micro_threshold_low", foreland.microThresholdLow());
        validation.finite("foreland.micro_threshold_high", foreland.microThresholdHigh());
        validation.ordered("foreland large thresholds",
                foreland.largeThresholdLow(), foreland.largeThresholdHigh());
        validation.ordered("foreland micro thresholds",
                foreland.microThresholdLow(), foreland.microThresholdHigh());
        validation.nonNegative("foreland.large_min_height", foreland.largeMinHeight(), 512.0);
        validation.nonNegative("foreland.large_max_height", foreland.largeMaxHeight(), 512.0);
        validation.nonNegative("foreland.micro_max_height", foreland.microMaxHeight(), 512.0);
        validation.ordered("foreland large heights",
                foreland.largeMinHeight(), foreland.largeMaxHeight());
        validation.unit("foreland.inner_height_scale", foreland.innerHeightScale());
        validation.nonNegative("foreland.inner_threshold_boost", foreland.innerThresholdBoost(), 2.0);
        validation.positive("foreland.growth_power", foreland.growthPower(), 16.0);

        MassifSettings massif = settings.massif();
        validation.ordered("massif radii",
                massif.startRadius(), massif.fullRadius(),
                massif.outerStartRadius(), massif.outerEndRadius());
        validation.radius("massif.start_radius", massif.startRadius());
        validation.radius("massif.full_radius", massif.fullRadius());
        validation.radius("massif.outer_start_radius", massif.outerStartRadius());
        validation.radius("massif.outer_end_radius", massif.outerEndRadius());
        validation.integer("massif.max_added_height", massif.maxAddedHeight(), 0, 512);
        validation.finite("massif.continuity_low", massif.continuityLow());
        validation.finite("massif.continuity_high", massif.continuityHigh());
        validation.finite("massif.shape_low", massif.shapeLow());
        validation.finite("massif.shape_high", massif.shapeHigh());
        validation.ordered("massif continuity thresholds",
                massif.continuityLow(), massif.continuityHigh());
        validation.ordered("massif shape thresholds", massif.shapeLow(), massif.shapeHigh());
        validation.positive("massif.inner_scarp_width", massif.innerScarpWidth(), 2_048.0);
        validation.positive("massif.outer_scarp_width", massif.outerScarpWidth(), 2_048.0);
        validation.scale("massif.scarp_warp_scale", massif.scarpWarpScale());
        validation.nonNegative("massif.scarp_warp_strength", massif.scarpWarpStrength(), 2_048.0);
        validation.scale("massif.scarp_detail_scale", massif.scarpDetailScale());
        validation.nonNegative("massif.scarp_detail_strength", massif.scarpDetailStrength(), 512.0);
        validation.range("base_alignment.massif_vertical_offset",
                settings.baseAlignment().massifVerticalOffset(), -512.0, 512.0);

        FaultSettings faults = settings.faults();
        validation.integer("faults.count", faults.count(), 0, 64);
        validation.ordered("fault radii",
                faults.startRadius(), faults.fullRadius(),
                faults.fadeStartRadius(), faults.endRadius());
        validation.radius("faults.start_radius", faults.startRadius());
        validation.radius("faults.full_radius", faults.fullRadius());
        validation.radius("faults.fade_start_radius", faults.fadeStartRadius());
        validation.radius("faults.end_radius", faults.endRadius());
        validation.nonNegative("faults.core_width", faults.coreWidth(), 2_048.0);
        validation.nonNegative("faults.outer_width", faults.outerWidth(), 4_096.0);
        validation.ordered("fault widths", faults.coreWidth(), faults.outerWidth());
        validation.scale("faults.broad_warp_scale", faults.broadWarpScale());
        validation.nonNegative("faults.broad_warp_strength", faults.broadWarpStrength(), 4_096.0);
        validation.scale("faults.medium_warp_scale", faults.mediumWarpScale());
        validation.nonNegative("faults.medium_warp_strength", faults.mediumWarpStrength(), 4_096.0);
        validation.scale("faults.sine_warp_scale", faults.sineWarpScale());
        validation.nonNegative("faults.sine_warp_strength", faults.sineWarpStrength(), 4_096.0);
        validation.unit("faults.sandy_floor_threshold", faults.sandyFloorThreshold());
        validation.nonNegative("faults.rocky_floor_height", faults.rockyFloorHeight(), 512.0);
        FaultMorphologySettings faultMorphology = faults.morphology();
        validation.nonNegative("faults.morphology.wall_width", faultMorphology.wallWidth(), 1_024.0);
        validation.nonNegative("faults.morphology.toe_depth", faultMorphology.toeDepth(), 1_024.0);
        validation.scale("faults.morphology.wall_variation_scale",
                faultMorphology.wallVariationScale());
        validation.nonNegative("faults.morphology.wall_variation",
                faultMorphology.wallVariation(), 1_024.0);

        LithologySettings lithology = settings.lithology();
        validation.scale("lithology.unit_horizontal_scale", lithology.unitHorizontalScale());
        validation.scale("lithology.unit_vertical_scale", lithology.unitVerticalScale());
        validation.positive("lithology.strata_thickness", lithology.strataThickness(), 2_048.0);
        validation.scale("lithology.strata_warp_scale", lithology.strataWarpScale());
        validation.nonNegative("lithology.strata_warp_strength", lithology.strataWarpStrength(), 2_048.0);
        validation.scale("lithology.intrusion_scale", lithology.intrusionScale());
        validation.unit("lithology.intrusion_threshold", lithology.intrusionThreshold());
        validation.scale("lithology.rare_body_scale", lithology.rareBodyScale());
        validation.unit("lithology.limestone_threshold", lithology.limestoneThreshold());
        validation.unit("lithology.blackstone_threshold", lithology.blackstoneThreshold());
        validation.ordered("lithology rare-body thresholds",
                lithology.limestoneThreshold(), lithology.blackstoneThreshold());
        validation.positive("lithology.dike_spacing", lithology.dikeSpacing(), 65_536.0);
        validation.nonNegative("lithology.dike_half_width", lithology.dikeHalfWidth(), 256.0);
        validation.positive("lithology.calcite_vein_spacing",
                lithology.calciteVeinSpacing(), 65_536.0);
        validation.nonNegative("lithology.calcite_vein_half_width",
                lithology.calciteVeinHalfWidth(), 256.0);
        validation.maximum("lithology.dike_half_width", lithology.dikeHalfWidth(),
                lithology.dikeSpacing() * 0.5);
        validation.maximum("lithology.calcite_vein_half_width", lithology.calciteVeinHalfWidth(),
                lithology.calciteVeinSpacing() * 0.5);
        validation.materials(lithology.materials());
        MaterialPaletteSettings materials = lithology.materials();
        validation.unit("lithology.materials.granite_fraction", materials.graniteFraction());
        validation.range("lithology.materials.deepslate_top_y",
                materials.deepslateTopY(), -64.0, 512.0);
        validation.nonNegative("lithology.materials.deepslate_warp_strength",
                materials.deepslateWarpStrength(), 512.0);

        TalusSettings talus = lithology.talus();
        validation.unit("lithology.talus.minimum_fracture_strength",
                talus.minimumFractureStrength());
        validation.integer("lithology.talus.maximum_thickness", talus.maximumThickness(), 0, 64);
        validation.nonNegative("lithology.talus.spread", talus.spread(), 512.0);
        validation.integer("lithology.talus.basal_apron_max_height",
                talus.basalApronMaxHeight(), 0, 64);
        validation.nonNegative("lithology.talus.basal_apron_spread",
                talus.basalApronSpread(), 512.0);
        validation.nonNegative("lithology.talus.basal_apron_inset",
                talus.basalApronInset(), 512.0);
        validation.maximum("lithology.talus.basal_apron_inset",
                talus.basalApronInset(), talus.basalApronSpread());
        validation.unit("lithology.talus.basal_apron_sand_start",
                talus.basalApronSandStart());
        if (talus.actualContactEnabled()) {
            validation.maximum("lithology.talus.basal_apron_spread (actual contact)", talus.basalApronSpread(), 32.0);
            validation.maximum("lithology.talus.basal_apron_inset (actual contact)", talus.basalApronInset(), 32.0);
        }

        FractureSettings fractures = settings.fractures();
        validation.positive("fractures.cell_size", fractures.cellSize(), 65_536.0);
        validation.unit("fractures.density", fractures.density());
        validation.nonNegative("fractures.minimum_length", fractures.minimumLength(), 65_536.0);
        validation.nonNegative("fractures.maximum_length", fractures.maximumLength(), 65_536.0);
        validation.ordered("fracture lengths", fractures.minimumLength(), fractures.maximumLength());
        validation.unit("fractures.branch_chance", fractures.branchChance());
        validation.nonNegative("fractures.minimum_width", fractures.minimumWidth(), 512.0);
        validation.nonNegative("fractures.maximum_width", fractures.maximumWidth(), 512.0);
        validation.ordered("fracture widths", fractures.minimumWidth(), fractures.maximumWidth());
        validation.nonNegative("fractures.minimum_depth", fractures.minimumDepth(), 512.0);
        validation.nonNegative("fractures.maximum_depth", fractures.maximumDepth(), 512.0);
        validation.ordered("fracture depths", fractures.minimumDepth(), fractures.maximumDepth());
        validation.nonNegative("fractures.minimum_rock_height", fractures.minimumRockHeight(), 512.0);
        validation.unit("fractures.minimum_massif_weight", fractures.minimumMassifWeight());
        validation.unit("fractures.mineralization_chance", fractures.mineralizationChance());
        validation.nonNegative("fractures.calcite_wall_thickness",
                fractures.calciteWallThickness(), 64.0);
        validation.unit("fractures.resistance_width_influence",
                fractures.resistanceWidthInfluence());
        validation.unit("fractures.resistance_depth_influence",
                fractures.resistanceDepthInfluence());

        ErosionSettings erosion = settings.erosion();
        validation.nonNegative("erosion.minimum_relief", erosion.minimumRelief(), 512.0);
        validation.positive("erosion.face_probe_distance", erosion.faceProbeDistance(), 256.0);
        validation.unit("erosion.escarpment_start_strength", erosion.escarpmentStartStrength());
        validation.nonNegative("erosion.vertical_face_bias", erosion.verticalFaceBias(), 4.0);
        validation.nonNegative("erosion.wind_exposure_strength", erosion.windExposureStrength(), 4.0);
        validation.nonNegative("erosion.fracture_erosion_strength",
                erosion.fractureErosionStrength(), 4.0);
        validation.nonNegative("erosion.soft_rock_multiplier", erosion.softRockMultiplier(), 8.0);
        validation.nonNegative("erosion.hard_rock_multiplier", erosion.hardRockMultiplier(), 8.0);
        validation.nonNegative("erosion.very_hard_rock_multiplier",
                erosion.veryHardRockMultiplier(), 8.0);
        validation.ordered("erosion resistance multipliers",
                erosion.veryHardRockMultiplier(),
                erosion.hardRockMultiplier(),
                erosion.softRockMultiplier());
        validation.unit("erosion.undercut_strength", erosion.undercutStrength());
        validation.integer("erosion.max_undercut_blocks", erosion.maxUndercutBlocks(), 0, 64);
        validation.unit("erosion.undercut_frequency", erosion.undercutFrequency());
        validation.nonNegative("erosion.broken_rock_scale", erosion.brokenRockScale(), 4.0);

        OrphanRemnantSettings orphan = erosion.orphanRemnants();
        validation.integer("erosion.orphan_remnants.component_search_radius", orphan.componentSearchRadius(), 3, 5);
        validation.integer("erosion.orphan_remnants.inward_support_depth",
                orphan.inwardSupportDepth(), 1, 16);
        validation.integer("erosion.orphan_remnants.lateral_search_radius",
                orphan.lateralSearchRadius(), 0, 4);
        validation.integer("erosion.orphan_remnants.minimum_height_above_base",
                orphan.minimumHeightAboveBase(), 0, 512);
        validation.nonNegative("erosion.orphan_remnants.minimum_face_relief",
                orphan.minimumFaceRelief(), 512.0);

        FrontShellCleanupSettings frontShell = settings.frontShellCleanup();
        validation.integer("front_shell_cleanup.pass1_depth", frontShell.pass1Depth(), 0, 4);
        validation.integer("front_shell_cleanup.pass2_depth", frontShell.pass2Depth(), 0, 4);
        validation.integer("front_shell_cleanup maximum retreat",
                frontShell.maximumRetreat(), 0, 4);

        SurfaceErosionSettings surface = erosion.surface();
        validation.nonNegative("erosion.surface.strength", surface.strength(), 4.0);
        validation.integer("erosion.surface.basal_erosion_depth", surface.basalErosionDepth(), 0, 10);
        validation.positive("erosion.surface.scale", surface.scale(), 65_536.0);
        validation.positive("erosion.surface.detail_scale", surface.detailScale(), 65_536.0);
        validation.integer("erosion.surface.max_retreat_blocks", surface.maxRetreatBlocks(), 0, 64);
        validation.nonNegative("erosion.surface.fissure_multiplier", surface.fissureMultiplier(), 8.0);
        validation.nonNegative("erosion.surface.small_rock_strength", surface.smallRockStrength(), 4.0);
        validation.nonNegative("erosion.surface.broken_rock_strength", surface.brokenRockStrength(), 4.0);
        validation.nonNegative("erosion.surface.lithology_relief_strength",
                surface.lithologyReliefStrength(), 4.0);

        SandPassSettings sandPasses = settings.sandPasses();
        validation.ordered("sand-pass radii",
                sandPasses.startRadius(), sandPasses.fullRadius(),
                sandPasses.fadeStartRadius(), sandPasses.endRadius());
        validation.radius("sand_passes.start_radius", sandPasses.startRadius());
        validation.radius("sand_passes.full_radius", sandPasses.fullRadius());
        validation.radius("sand_passes.fade_start_radius", sandPasses.fadeStartRadius());
        validation.radius("sand_passes.end_radius", sandPasses.endRadius());
        validation.nonNegative("sand_passes.primary_core_width",
                sandPasses.primaryCoreWidth(), 4_096.0);
        validation.nonNegative("sand_passes.primary_outer_width",
                sandPasses.primaryOuterWidth(), 4_096.0);
        validation.ordered("primary sand-pass widths",
                sandPasses.primaryCoreWidth(), sandPasses.primaryOuterWidth());
        validation.nonNegative("sand_passes.secondary_core_width",
                sandPasses.secondaryCoreWidth(), 4_096.0);
        validation.nonNegative("sand_passes.secondary_outer_width",
                sandPasses.secondaryOuterWidth(), 4_096.0);
        validation.ordered("secondary sand-pass widths",
                sandPasses.secondaryCoreWidth(), sandPasses.secondaryOuterWidth());

        BrokenRockSettings brokenRock = settings.brokenRock();
        validation.ordered("broken-rock radii",
                brokenRock.startRadius(), brokenRock.fullRadius(),
                brokenRock.outerFadeStartRadius(), brokenRock.outerRadius());
        validation.radius("broken_rock.start_radius", brokenRock.startRadius());
        validation.radius("broken_rock.full_radius", brokenRock.fullRadius());
        validation.radius("broken_rock.outer_fade_start_radius", brokenRock.outerFadeStartRadius());
        validation.radius("broken_rock.outer_radius", brokenRock.outerRadius());
        validation.scale("broken_rock.large_scale", brokenRock.largeScale());
        validation.scale("broken_rock.detail_scale", brokenRock.detailScale());
        validation.scale("broken_rock.micro_scale", brokenRock.microScale());
        validation.nonNegative("broken_rock.max_height_inner", brokenRock.maxHeightInner(), 512.0);
        validation.nonNegative("broken_rock.max_height_outer", brokenRock.maxHeightOuter(), 512.0);
        validation.nonNegative("broken_rock.micro_max_height", brokenRock.microMaxHeight(), 512.0);
        validation.positive("broken_rock.size_decay_power", brokenRock.sizeDecayPower(), 16.0);

        OuterTransitionSettings transition = settings.outerTransition();
        validation.ordered("outer-transition radii",
                transition.startRadius(), transition.fullRadius(),
                transition.fadeStartRadius(), transition.outerRadius());
        validation.radius("outer_transition.start_radius", transition.startRadius());
        validation.radius("outer_transition.full_radius", transition.fullRadius());
        validation.radius("outer_transition.fade_start_radius", transition.fadeStartRadius());
        validation.radius("outer_transition.outer_radius", transition.outerRadius());
        validation.radius("outer_transition.open_erg_start_radius", transition.openErgStartRadius());
        validation.radius("outer_transition.open_erg_full_radius", transition.openErgFullRadius());
        validation.ordered("open-erg radii",
                transition.openErgStartRadius(), transition.openErgFullRadius());

        NativeDuneSettings dunes = settings.nativeDunes();
        validation.nonNegative("native_dunes.max_height", dunes.maxHeight(), 256.0);
        validation.positive("native_dunes.spacing", dunes.spacing(), 65_536.0);
        validation.unit("native_dunes.spacing_variation", dunes.spacingVariation());
        validation.positive("native_dunes.ridge_sharpness", dunes.ridgeSharpness(), 32.0);
        validation.unit("native_dunes.valley_cutoff", dunes.valleyCutoff());
        validation.unit("native_dunes.slope_asymmetry", dunes.slopeAsymmetry());
        validation.range("native_dunes.wind_angle_degrees",
                dunes.windAngleDegrees(), -360.0, 360.0);
        validation.unit("native_dunes.foreland_weight", dunes.forelandWeight());
        validation.unit("native_dunes.broken_rock_weight", dunes.brokenRockWeight());
        validation.unit("native_dunes.transition_weight", dunes.transitionWeight());

        return validation.result(settings);
    }

    private static final class Validation {
        private static final double MAX_RADIUS = 30_000_000.0;
        private final List<String> errors = new ArrayList<>();

        private void finite(String name, double value) {
            if (!Double.isFinite(value)) {
                errors.add(name + " must be finite");
            }
        }

        private void range(String name, double value, double minimum, double maximum) {
            if (!Double.isFinite(value) || value < minimum || value > maximum) {
                errors.add(name + " must be in [" + minimum + ", " + maximum + "]");
            }
        }

        private void positive(String name, double value, double maximum) {
            if (!Double.isFinite(value) || value <= 0.0 || value > maximum) {
                errors.add(name + " must be in (0, " + maximum + "]");
            }
        }

        private void nonNegative(String name, double value, double maximum) {
            range(name, value, 0.0, maximum);
        }

        private void unit(String name, double value) {
            range(name, value, 0.0, 1.0);
        }

        private void scale(String name, double value) {
            positive(name, value, 65_536.0);
        }

        private void radius(String name, double value) {
            range(name, value, 0.0, MAX_RADIUS);
        }

        private void integer(String name, int value, int minimum, int maximum) {
            if (value < minimum || value > maximum) {
                errors.add(name + " must be in [" + minimum + ", " + maximum + "]");
            }
        }

        private void maximum(String name, double value, double maximum) {
            if (!Double.isFinite(value) || value > maximum) {
                errors.add(name + " must not exceed " + maximum);
            }
        }

        private void ordered(String name, double... values) {
            for (int index = 1; index < values.length; index++) {
                if (!Double.isFinite(values[index - 1])
                        || !Double.isFinite(values[index])
                        || values[index - 1] > values[index]) {
                    errors.add(name + " must be nondecreasing");
                    return;
                }
            }
        }

        private void materials(MaterialPaletteSettings materials) {
            material("lithology.materials.background", materials.background());
            material("lithology.materials.sandstone", materials.sandstone());
            material("lithology.materials.tuff", materials.tuff());
            material("lithology.materials.limestone", materials.limestone());
            material("lithology.materials.limestone_fallback", materials.limestoneFallback());
            material("lithology.materials.calcite", materials.calcite());
            material("lithology.materials.andesite", materials.andesite());
            material("lithology.materials.diorite", materials.diorite());
            material("lithology.materials.granite", materials.granite());
            material("lithology.materials.basalt", materials.basalt());
            material("lithology.materials.blackstone", materials.blackstone());
            material("lithology.materials.deepslate", materials.deepslate());
            material("lithology.materials.talus", materials.talus());
        }

        private void material(String name, String identifier) {
            if (ResourceLocation.tryParse(identifier) == null) {
                errors.add(name + " is not a valid resource location: " + identifier);
            }
        }

        private <T> DataResult<T> result(T value) {
            return errors.isEmpty()
                    ? DataResult.success(value)
                    : DataResult.error(() -> String.join("; ", errors));
        }
    }

}
