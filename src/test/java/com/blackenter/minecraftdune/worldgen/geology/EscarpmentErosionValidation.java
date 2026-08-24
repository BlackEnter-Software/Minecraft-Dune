package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Dependency-free deterministic smoke checks for the analytic 0.5.14 terrain fields. */
public final class EscarpmentErosionValidation {
    private static final int CHUNK_SIZE = 16;
    private static final long[] SEEDS = {
            0L,
            -7_219_451_331L,
            0x6A09E667F3BCC909L
    };

    private EscarpmentErosionValidation() {
    }

    public static void main(String[] args) throws Exception {
        Profile profile = loadProfile();
        ArrakisTerrainSettings settings = profile.settings();
        require(settings.profileVersion() == 51410, "active profile_version must be 51410");
        require(settings.erosion().enabled(), "active preset erosion must be enabled");

        JsonObject legacy5142Profile = profile.json().deepCopy();
        legacy5142Profile.addProperty("profile_version", 5142);
        JsonObject legacyMassif = legacy5142Profile.getAsJsonObject("massif");
        legacyMassif.remove("scarp_morphology_enabled");
        legacyMassif.remove("inner_scarp_width");
        legacyMassif.remove("outer_scarp_width");
        legacyMassif.remove("scarp_warp_scale");
        legacyMassif.remove("scarp_warp_strength");
        legacyMassif.remove("scarp_detail_scale");
        legacyMassif.remove("scarp_detail_strength");
        legacy5142Profile.remove("base_alignment");
        JsonObject legacyTalus = legacy5142Profile
                .getAsJsonObject("lithology")
                .getAsJsonObject("talus");
        legacyTalus.remove("basal_apron_enabled");
        legacyTalus.remove("basal_apron_max_height");
        legacyTalus.remove("basal_apron_spread");
        legacyTalus.remove("basal_apron_inset");
        legacyTalus.remove("basal_apron_sand_start");
        legacy5142Profile.remove("additional_materials");
        legacy5142Profile.getAsJsonObject("faults").remove("morphology");
        legacy5142Profile.getAsJsonObject("erosion").remove("orphan_remnants");
        ArrakisTerrainSettings legacy5142 = ArrakisTerrainSettings.CODEC
                .parse(JsonOps.INSTANCE, legacy5142Profile)
                .getOrThrow();
        require(!legacy5142.massif().scarpMorphologyEnabled(),
                "missing scarp controls must retain 0.5.14.2 massif morphology");
        require(legacy5142.faults().morphology().equals(
                        ArrakisTerrainSettings.FaultMorphologySettings.LEGACY
                ),
                "missing fault morphology must retain the 0.5.14.2 fault transition");
        require(legacy5142.baseAlignment().massifVerticalOffset() == 0.0,
                "missing base_alignment must preserve the old massif elevation");
        require(!legacy5142.lithology().talus().basalApronEnabled(),
                "missing basal apron fields must preserve old talus behavior");
        require(!legacy5142.additionalMaterials().enabled(),
                "missing additional_materials must retain the old lithology palette");
        require(!legacy5142.erosion().orphanRemnants().enabled(),
                "missing orphan_remnants must retain old erosion occupancy");
        require(!BasalTalusApronField.usesActualContact(5148),
                "serialized 0.5.14.8 profiles must retain nominal apron targeting");
        require(BasalTalusApronField.usesActualContact(5149),
                "profile 5149 must use actual surviving-rock contact");
        require(!BasalTalusApronField.usesContactOwnership(5149),
                "serialized 0.5.14.9 profiles must retain source-owned contact classification");
        require(BasalTalusApronField.usesContactOwnership(51410),
                "profile 51410 must use actual contact ownership and wall-relief probing");

        JsonObject oldProfile = legacy5142Profile.deepCopy();
        oldProfile.addProperty("profile_version", 513);
        oldProfile.remove("erosion");
        ArrakisTerrainSettings backward = ArrakisTerrainSettings.CODEC
                .parse(JsonOps.INSTANCE, oldProfile)
                .getOrThrow();
        require(!backward.erosion().enabled(), "missing erosion group must decode disabled");

        validateResistanceOrder(settings);
        validateScarpMorphology(settings);
        validateStructuralFaceCoupling(settings);
        validateScarpRoughness(settings);
        validateBaseAlignment(settings);
        validateBasalContactAndTalus(settings);
        validateAdditionalMaterials(settings);
        validateOrphanRemnantFilter(settings);
        validateExposedFaceGeometry(settings);
        validateBasinAndDunes(settings);
        SeamCounts seams = validateChunkBoundaryOrderIndependence(settings);
        ValidationCounts counts = validateEscarpments(settings);
        validateFaultFloors(settings);

        System.out.printf(
                Locale.ROOT,
                "Arrakis erosion validation passed: candidates=%d, undercut_columns=%d, "
                        + "explicit_undercut_candidates=%d, talus_columns=%d, "
                        + "fracture_comparisons=%d, seam_columns=%d, "
                        + "order_columns=%d, deterministic_hash=%016x, max_retreat=%.2f.%n",
                counts.candidates(),
                counts.undercutColumns(),
                counts.explicitUndercutCandidates(),
                counts.talusColumns(),
                counts.fractureComparisons(),
                seams.seamColumns(),
                seams.orderColumns(),
                counts.hash(),
                counts.maximumRetreat()
        );
        for (int index = 0; index < counts.representatives().length; index++) {
            CandidateCoordinate candidate = counts.representatives()[index];
            boolean alreadyReported = false;
            for (int previous = 0; previous < index; previous++) {
                CandidateCoordinate earlier = counts.representatives()[previous];
                if (candidate.seed() == earlier.seed()
                        && candidate.x() == earlier.x()
                        && candidate.z() == earlier.z()) {
                    alreadyReported = true;
                    break;
                }
            }
            if (alreadyReported) {
                continue;
            }
            System.out.printf(
                    Locale.ROOT,
                    "Representative erosion: seed=%d (0x%016x), x=%.1f, z=%.1f, "
                            + "suggested_y=%d, strength=%.3f, relief=%.1f, retreat=%.2f, "
                            + "removed=%d, undercut_column=%s, undercut=%.3f, "
                            + "fracture=%.3f, talus=%.3f (%d blocks).%n",
                    candidate.seed(),
                    candidate.seed(),
                    candidate.x(),
                    candidate.z(),
                    candidate.suggestedY(),
                    candidate.escarpmentStrength(),
                    candidate.localRelief(),
                    candidate.maximumRetreat(),
                    candidate.removedBlocks(),
                    candidate.undercutColumn(),
                    candidate.undercutPotential(),
                    candidate.fractureErosion(),
                    candidate.talusSuitability(),
                    candidate.talusThickness()
            );
        }
    }

    private static Profile loadProfile() throws IOException {
        Path path = Path.of(
                "src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json"
        );
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject terrain = root.getAsJsonObject("dimensions")
                    .getAsJsonObject("minecraft:overworld")
                    .getAsJsonObject("generator")
                    .getAsJsonObject("terrain");
            ArrakisTerrainSettings settings = ArrakisTerrainSettings.CODEC
                    .parse(JsonOps.INSTANCE, terrain)
                    .getOrThrow();
            return new Profile(terrain, settings);
        }
    }

    private static void validateResistanceOrder(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.ErosionSettings erosion = settings.erosion();
        double soft = EscarpmentErosionField.retreatMultiplier(
                LithologyField.ResistanceClass.SOFT, erosion
        );
        double medium = EscarpmentErosionField.retreatMultiplier(
                LithologyField.ResistanceClass.MEDIUM, erosion
        );
        double hard = EscarpmentErosionField.retreatMultiplier(
                LithologyField.ResistanceClass.HARD, erosion
        );
        double veryHard = EscarpmentErosionField.retreatMultiplier(
                LithologyField.ResistanceClass.VERY_HARD, erosion
        );
        require(soft > medium && medium > hard && hard > veryHard,
                "retreat must decrease monotonically with resistance");
    }

    private static void validateScarpMorphology(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.MassifSettings massif = settings.massif();
        ArrakisTerrainSettings.FaultSettings faults = settings.faults();
        ArrakisTerrainSettings.FaultMorphologySettings faultMorphology = faults.morphology();

        require(massif.scarpMorphologyEnabled(),
                "active 0.5.14.3 profile must enable scarp morphology");
        require(faultMorphology.wallWidth() == 14.0 && faultMorphology.toeDepth() == 4.0,
                "active 0.5.14.3 fault morphology must use the source-profile values");

        double innerBefore = ScarpMorphologyField.massifEnvelope(
                massif.startRadius() - 1.0,
                massif.startRadius() - 1.0,
                0.0,
                massif
        );
        double innerAfter = ScarpMorphologyField.massifEnvelope(
                massif.startRadius() + massif.innerScarpWidth() + 1.0,
                massif.startRadius() + massif.innerScarpWidth() + 1.0,
                1.0,
                massif
        );
        double outerBefore = ScarpMorphologyField.massifEnvelope(
                massif.outerStartRadius() - 1.0,
                massif.outerStartRadius() - 1.0,
                1.0,
                massif
        );
        double outerAfter = ScarpMorphologyField.massifEnvelope(
                massif.outerStartRadius() + massif.outerScarpWidth() + 1.0,
                massif.outerStartRadius() + massif.outerScarpWidth() + 1.0,
                1.0,
                massif
        );

        require(innerBefore < 0.01 && innerAfter > 0.95,
                "inner structural scarp did not reach full massif over configured width");
        require(outerBefore > 0.95 && outerAfter < 0.01,
                "outer structural scarp did not terminate over configured width");

        ScarpMorphologyField.FaultProfile core = ScarpMorphologyField.faultProfile(
                0.0,
                1.0,
                faults,
                true
        );
        ScarpMorphologyField.FaultProfile wallOutside = ScarpMorphologyField.faultProfile(
                faults.coreWidth() + faultMorphology.wallWidth() + 1.0,
                1.0,
                faults,
                true
        );
        ScarpMorphologyField.FaultProfile outside = ScarpMorphologyField.faultProfile(
                faults.outerWidth() + 1.0,
                1.0,
                faults,
                true
        );

        require(core.depthMask() > 0.99,
                "fault core no longer reaches full absolute-depth mask");
        require(wallOutside.depthMask() < 0.02,
                "fault depth still ramps across the complete outer influence zone");
        require(wallOutside.shoulderMask() > 0.0,
                "fault outer influence lost its shallow shoulder");
        require(outside.depthMask() == 0.0 && outside.shoulderMask() == 0.0,
                "fault morphology escaped outer_width");

        require(massif.innerScarpWidth()
                        < massif.fullRadius() - massif.startRadius(),
                "inner physical scarp is not narrower than the broad province ramp");
        require(massif.outerScarpWidth()
                        < massif.outerEndRadius() - massif.outerStartRadius(),
                "outer physical scarp is not narrower than the broad province fade");
    }

    private static void validateStructuralFaceCoupling(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.MassifSettings massif = settings.massif();

        double innerMidRadius = massif.startRadius() + massif.innerScarpWidth() * 0.50;
        double physicalInnerPermission = ScarpMorphologyField.massifErosionPermission(
                innerMidRadius,
                innerMidRadius,
                0.01,
                massif
        );
        require(physicalInnerPermission > 0.45,
                "physical inner scarp did not authorize erosion independently of broad massif weight");

        double outerMidRadius = massif.outerStartRadius() + massif.outerScarpWidth() * 0.50;
        double physicalOuterPermission = ScarpMorphologyField.massifErosionPermission(
                outerMidRadius,
                outerMidRadius,
                0.92,
                massif
        );
        require(physicalOuterPermission > 0.90,
                "outer scarp lost existing broad/physical erosion permission");

        double protectedCore = ScarpMorphologyField.faultErosionPermission(1.0);
        double innerWall = ScarpMorphologyField.faultErosionPermission(0.97);
        double ordinaryWall = ScarpMorphologyField.faultErosionPermission(0.80);
        double noFault = ScarpMorphologyField.faultErosionPermission(0.0);

        require(protectedCore < 0.01,
                "full-depth fault core is no longer protected from erosion");
        require(innerWall > 0.05 && innerWall < 0.95,
                "fault core-to-wall erosion transition is not gradual");
        require(ordinaryWall > 0.99,
                "ordinary physical fault wall is still being protected like the floor");
        require(noFault > 0.999,
                "non-fault terrain lost erosion permission");
    }

    private static void validateScarpRoughness(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.MassifSettings massif = settings.massif();
        ArrakisTerrainSettings.FaultSettings faults = settings.faults();
        ArrakisTerrainSettings.FaultMorphologySettings morphology = faults.morphology();

        require(massif.scarpWarpStrength() > 0.0
                        && massif.scarpDetailStrength() > 0.0,
                "active 0.5.14.5 massif scarp roughness must be enabled");
        require(morphology.wallVariation() > 0.0,
                "active 0.5.14.5 fault wall variation must be enabled");

        double maximumMassifOffset =
                massif.scarpWarpStrength() + massif.scarpDetailStrength();
        double innerA = ScarpMorphologyField.massifBoundaryOffset(
                0L, 3000.0, 0.0, massif, true
        );
        double innerAgain = ScarpMorphologyField.massifBoundaryOffset(
                0L, 3000.0, 0.0, massif, true
        );
        double innerB = ScarpMorphologyField.massifBoundaryOffset(
                0L, 3144.0, 93.0, massif, true
        );
        double outerA = ScarpMorphologyField.massifBoundaryOffset(
                0L, 4000.0, 0.0, massif, false
        );

        require(innerA == innerAgain,
                "massif scarp roughness is not deterministic");
        require(Math.abs(innerA) <= maximumMassifOffset + 1.0e-9
                        && Math.abs(innerB) <= maximumMassifOffset + 1.0e-9
                        && Math.abs(outerA) <= maximumMassifOffset + 1.0e-9,
                "massif scarp roughness exceeded configured bound");
        require(Math.abs(innerA - innerB) > 1.0e-4,
                "massif scarp roughness did not vary spatially");

        double wallA = ScarpMorphologyField.faultWallWidth(0L, 0.0, 0, faults);
        double wallAgain = ScarpMorphologyField.faultWallWidth(0L, 0.0, 0, faults);
        double wallB = ScarpMorphologyField.faultWallWidth(
                0L, morphology.wallVariationScale() * 1.75, 0, faults
        );
        double coreA = ScarpMorphologyField.faultCoreWidth(0L, 0.0, 0, faults);
        double coreB = ScarpMorphologyField.faultCoreWidth(
                0L, morphology.wallVariationScale() * 1.75, 0, faults
        );

        require(wallA == wallAgain,
                "fault wall variation is not deterministic");
        require(Math.abs(wallA - morphology.wallWidth())
                        <= morphology.wallVariation() + 1.0e-9
                        && Math.abs(wallB - morphology.wallWidth())
                        <= morphology.wallVariation() + 1.0e-9,
                "fault wall variation exceeded configured bound");
        require(Math.abs(wallA - wallB) > 1.0e-4,
                "fault wall width did not vary along the fault");
        require(coreA >= faults.coreWidth() && coreB >= faults.coreWidth(),
                "fault roughness narrowed the protected core below core_width");

        double maximumCoreExpansion = Math.min(
                2.0, morphology.wallVariation() * 0.40
        );
        require(coreA <= faults.coreWidth() + maximumCoreExpansion + 1.0e-9
                        && coreB <= faults.coreWidth() + maximumCoreExpansion + 1.0e-9,
                "fault protected-core variation exceeded its safety bound");
    }

    private static void validateBaseAlignment(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.BaseAlignmentSettings alignment =
                settings.baseAlignment();

        require(alignment.massifVerticalOffset() == -4.0,
                "active 0.5.14.7 massif vertical offset must be -4 blocks");
        require(settings.faults().rockyFloorHeight() == 0.0,
                "active 0.5.14.7 regional fault floor must target the sand datum");

        require(MacroGeologyField.applyMassifVerticalOffset(
                        100.0,
                        alignment
                ) == 96.0,
                "massif vertical offset did not lower ordinary massif relief by four blocks");
        require(MacroGeologyField.applyMassifVerticalOffset(
                        3.0,
                        alignment
                ) == 0.0,
                "massif vertical offset did not clamp a low scarp toe to the sand datum");
        require(MacroGeologyField.applyMassifVerticalOffset(
                        0.0,
                        alignment
                ) == 0.0,
                "massif vertical offset raised an empty terrain column");
    }

    private static void validateBasalContactAndTalus(
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.BaseAlignmentSettings alignment =
                settings.baseAlignment();
        ArrakisTerrainSettings.TalusSettings talus =
                settings.lithology().talus();

        require(talus.basalApronEnabled(),
                "active profile must enable the basal talus apron");
        require(talus.basalApronMaxHeight() == 6
                        && talus.basalApronSpread() == 12.0
                        && talus.basalApronInset() == 4.0,
                "active basal talus apron must use the source-profile dimensions");

        double edgeHeight = MacroGeologyField.massifHeightWithBasalContact(
                0.0,
                1.0,
                0.5,
                alignment
        );
        double shallowHeight = MacroGeologyField.massifHeightWithBasalContact(
                0.10,
                0.05,
                0.5,
                alignment
        );
        double deepHeight = MacroGeologyField.massifHeightWithBasalContact(
                1.0,
                1.0,
                0.5,
                alignment
        );

        require(edgeHeight == 0.0,
                "massif basal contact no longer reaches the sand datum");
        require(shallowHeight == 0.0,
                "low physical scarp still retains an artificial basal pedestal");
        require(Math.abs(deepHeight - 146.0) < 1.0e-9,
                "basal-contact gate changed full massif height");

        ArrakisTerrainSettings.MassifSettings massif = settings.massif();
        double innerOffset = ScarpMorphologyField.massifBoundaryOffset(
                0L,
                massif.startRadius(),
                0.0,
                massif,
                true
        );
        double innerContactRadius = massif.startRadius() + innerOffset;
        double innerClearance = MacroGeologyField.massifContactClearance(
                0L,
                innerContactRadius,
                0.0,
                innerContactRadius,
                innerContactRadius,
                settings
        );

        double outerOffset = ScarpMorphologyField.massifBoundaryOffset(
                0L,
                massif.outerStartRadius(),
                0.0,
                massif,
                false
        );
        double outerContactRadius = massif.outerStartRadius()
                + outerOffset
                + massif.outerScarpWidth();
        double outerClearance = MacroGeologyField.massifContactClearance(
                0L,
                outerContactRadius,
                0.0,
                outerContactRadius,
                outerContactRadius,
                settings
        );

        require(innerClearance > 0.999,
                "inner Shield-Wall contact did not clear overlapping non-massif ownership");
        require(outerClearance > 0.999,
                "outer Shield-Wall contact did not clear overlapping non-massif ownership");
        require(MacroGeologyField.applyContactClearance(7.0, 1.0) == 0.0,
                "full basal-contact clearance did not remove non-massif height");
        require(MacroGeologyField.applyContactClearance(7.0, 0.0) == 7.0,
                "zero basal-contact clearance changed terrain outside the contact band");

        require(BasalTalusApronField.heightFromFactors(
                        talus.basalApronMaxHeight(),
                        1.0,
                        1.0,
                        1.0
                ) == talus.basalApronMaxHeight(),
                "full basal talus contact did not reach configured maximum height");
        require(BasalTalusApronField.heightFromFactors(
                        talus.basalApronMaxHeight(),
                        0.0,
                        1.0,
                        1.0
                ) == 0,
                "basal talus apron did not taper to zero at its outer edge");

        BasalTalusApronField.RockColumn massifRock =
                new BasalTalusApronField.RockColumn(true, 160, true, 0.0);
        BasalTalusApronField.SurvivingRockLookup outerMassif = (x, z) -> x <= 0
                ? massifRock
                : BasalTalusApronField.RockColumn.EMPTY;
        BasalTalusApronField.SurvivingRockLookup innerMassif = (x, z) -> x >= 0
                ? massifRock
                : BasalTalusApronField.RockColumn.EMPTY;

        BasalTalusApronField.Sample outerContact =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 1, 0, 0.0, talus, outerMassif
                );
        BasalTalusApronField.Sample innerContact =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, -1, 0, 0.0, talus, innerMassif
                );
        require(outerContact.active()
                        && outerContact.contactKind()
                        == BasalTalusApronField.ContactKind.MASSIF,
                "outer massif apron did not find the surviving rock footprint");
        require(innerContact.active()
                        && innerContact.contactKind()
                        == BasalTalusApronField.ContactKind.MASSIF,
                "inner massif apron did not find the surviving rock footprint");
        require(outerContact.contactDistance() == 1.0
                        && outerContact.outwardDistance() == 0.0,
                "massif apron retained an artificial gap beside surviving rock");
        require(!BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 0, 0, 0.0, talus, outerMassif
                ).active(),
                "basal apron overwrote the surviving rock contact column");

        BasalTalusApronField.RockColumn overlapOwnedRock =
                new BasalTalusApronField.RockColumn(true, 150, false, 0.0);
        BasalTalusApronField.SurvivingRockLookup overlapOwnedContact = (x, z) -> x <= 0
                ? overlapOwnedRock
                : BasalTalusApronField.RockColumn.EMPTY;
        BasalTalusApronField.Sample ownership51410 =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 1, 0, 0.0, 51410, talus, overlapOwnedContact
                );
        BasalTalusApronField.Sample ownership5149 =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 1, 0, 0.0, 5149, talus, overlapOwnedContact
                );
        require(ownership51410.active()
                        && ownership51410.contactKind()
                        == BasalTalusApronField.ContactKind.MASSIF,
                "actual surviving contact owned by an overlap field was rejected in profile 51410");
        require(!ownership5149.active(),
                "profile 5149 no longer retains its source-owned massif contact behavior");

        BasalTalusApronField.RockColumn faultWallRock =
                new BasalTalusApronField.RockColumn(true, 150, false, 0.995);
        BasalTalusApronField.SurvivingRockLookup faultCanyon = (x, z) ->
                Math.abs(x) >= 8
                        ? faultWallRock
                        : BasalTalusApronField.RockColumn.EMPTY;
        BasalTalusApronField.Sample leftFault =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, -7, 0, 1.0, talus, faultCanyon
                );
        BasalTalusApronField.Sample rightFault =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 7, 0, 1.0, talus, faultCanyon
                );
        BasalTalusApronField.Sample protectedCenter =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 0, 0, 1.0, talus, faultCanyon
                );
        require(leftFault.active() && rightFault.active()
                        && leftFault.contactKind() == BasalTalusApronField.ContactKind.FAULT
                        && rightFault.contactKind() == BasalTalusApronField.ContactKind.FAULT,
                "both surviving regional-fault walls must produce basal colluvium");
        require(!protectedCenter.active(),
                "opposing fault aprons bridged or refilled the protected sandy core");

        BasalTalusApronField.RockColumn shallowFaultToe =
                new BasalTalusApronField.RockColumn(true, 68, false, 0.98);
        BasalTalusApronField.RockColumn tallFaultWall =
                new BasalTalusApronField.RockColumn(true, 150, false, 0.80);
        BasalTalusApronField.SurvivingRockLookup shallowToeWithWall = (x, z) -> {
            if (x < 8) {
                return BasalTalusApronField.RockColumn.EMPTY;
            }
            return x < 12 ? shallowFaultToe : tallFaultWall;
        };
        BasalTalusApronField.Sample wallRelief51410 =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 7, 0, 1.0, 51410, talus, shallowToeWithWall
                );
        BasalTalusApronField.Sample wallRelief5149 =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 7, 0, 1.0, 5149, talus, shallowToeWithWall
                );
        require(wallRelief51410.active(),
                "shallow fault toe did not inherit relief from the tall wall behind it");
        require(!wallRelief5149.active(),
                "profile 5149 no longer retains its single-contact relief behavior");

        BasalTalusApronField.SurvivingRockLookup narrowFault = (x, z) ->
                Math.abs(x) >= 2
                        ? faultWallRock
                        : BasalTalusApronField.RockColumn.EMPTY;
        require(BasalTalusApronField.sampleFromSurvivingContact(
                        0L, -1, 0, 1.0, talus, narrowFault
                ).active()
                        && BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 1, 0, 1.0, talus, narrowFault
                ).active(),
                "narrow fault lost its wall-adjacent colluvium");
        require(!BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 0, 0, 1.0, talus, narrowFault
                ).active(),
                "block-grid rounding closed the narrow fault's central sandy channel");

        BasalTalusApronField.Sample outerAgain =
                BasalTalusApronField.sampleFromSurvivingContact(
                        0L, 1, 0, 0.0, talus, outerMassif
                );
        require(outerContact.equals(outerAgain),
                "surviving-contact talus sampling is not deterministic");
        BasalTalusApronField.Sample[] forward = {
                leftFault,
                protectedCenter,
                rightFault
        };
        BasalTalusApronField.Sample[] reverse = new BasalTalusApronField.Sample[3];
        reverse[2] = BasalTalusApronField.sampleFromSurvivingContact(
                0L, 7, 0, 1.0, talus, faultCanyon
        );
        reverse[1] = BasalTalusApronField.sampleFromSurvivingContact(
                0L, 0, 0, 1.0, talus, faultCanyon
        );
        reverse[0] = BasalTalusApronField.sampleFromSurvivingContact(
                0L, -7, 0, 1.0, talus, faultCanyon
        );
        for (int index = 0; index < forward.length; index++) {
            require(forward[index].equals(reverse[index]),
                    "talus result changed with sampling order");
        }
    }

    private static void validateAdditionalMaterials(ArrakisTerrainSettings settings) {
        require(settings.additionalMaterials().enabled(),
                "active 0.5.14.6 profile must enable additional materials");
        require(LithologyField.Material.SMOOTH_BASALT.resistance()
                        == LithologyField.ResistanceClass.HARD,
                "smooth basalt must be HARD");
        require(LithologyField.Material.RED_SANDSTONE.resistance()
                        == LithologyField.ResistanceClass.SOFT,
                "red sandstone must be SOFT");
        require(LithologyField.Material.TERRACOTTA.resistance()
                        == LithologyField.ResistanceClass.MEDIUM,
                "terracotta must be MEDIUM");
    }

    private static void validateOrphanRemnantFilter(ArrakisTerrainSettings settings) {
        ArrakisTerrainSettings.OrphanRemnantSettings orphan =
                settings.erosion().orphanRemnants();
        require(orphan.enabled(),
                "active 0.5.14.6 orphan-remnant filter must be enabled");

        int y = MacroGeologyField.BASE_SURFACE_Y + 24;

        boolean isolated = OrphanRemnantFilter.keeps(
                0,
                y,
                0,
                true,
                80.0,
                1.0,
                0.0,
                orphan,
                (x, testY, z) -> x == 0 && z == 0
        );
        require(!isolated,
                "isolated vertically-supported remnant was not removed");

        boolean directlyAttached = OrphanRemnantFilter.keeps(
                0,
                y,
                0,
                true,
                80.0,
                1.0,
                0.0,
                orphan,
                (x, testY, z) -> z == 0 && (x == 0 || x == -1)
        );
        require(directlyAttached,
                "directly inward-attached cliff rib was incorrectly removed");

        boolean laterallyAttached = OrphanRemnantFilter.keeps(
                0,
                y,
                0,
                true,
                80.0,
                1.0,
                0.0,
                orphan,
                (x, testY, z) ->
                        (x == 0 && z == 0)
                                || (x == 0 && z == 1)
                                || (x == -1 && z == 1)
        );
        require(laterallyAttached,
                "short laterally-connected ledge was incorrectly removed");

        boolean protectedBase = OrphanRemnantFilter.keeps(
                0,
                MacroGeologyField.BASE_SURFACE_Y + 2,
                0,
                true,
                80.0,
                1.0,
                0.0,
                orphan,
                (x, testY, z) -> false
        );
        require(protectedBase,
                "orphan filter modified the protected base layer");

        boolean smallFormation = OrphanRemnantFilter.keeps(
                0,
                y,
                0,
                true,
                orphan.minimumFaceRelief() - 1.0,
                1.0,
                0.0,
                orphan,
                (x, testY, z) -> false
        );
        require(smallFormation,
                "orphan filter escaped its major-face relief gate");
    }

    private static void validateExposedFaceGeometry(ArrakisTerrainSettings settings) {
        // A broad wall remains a cliff even when its owning formation mask would be 1.0. The
        // helper intentionally receives only physical heights, so no mask can suppress it.
        RockFaceExposure.Sample wall = RockFaceExposure.fromHeights(
                220.0,
                80.0,
                220.0,
                220.0,
                220.0,
                80.0,
                220.0,
                220.0,
                220.0,
                5.0,
                18.0,
                settings.erosion().minimumRelief()
        );
        require(wall.exposed() && wall.exposure() > 0.80,
                "140-block synthetic massif wall was not strongly exposed");
        require(wall.lowY() == 80 && wall.highY() == 220,
                "synthetic wall vertical interval is incorrect");
        require(wall.highSide() && wall.faceInset() < 0.01,
                "synthetic wall boundary was not classified on the high side");

        LithologyField.Sample soft = new LithologyField.Sample(
                LithologyField.Material.TUFF,
                LithologyField.ResistanceClass.SOFT,
                false,
                false,
                false,
                false
        );
        LithologyField.Sample veryHard = new LithologyField.Sample(
                LithologyField.Material.BASALT,
                LithologyField.ResistanceClass.VERY_HARD,
                false,
                false,
                true,
                false
        );
        ArrakisTerrainSettings.ErosionSettings erosion = settings.erosion();
        ArrakisTerrainSettings.SurfaceErosionSettings surface = erosion.surface();
        RockFaceExposure.Sample insetWall = new RockFaceExposure.Sample(
                wall.exposed(),
                wall.exposure(),
                wall.localRelief(),
                wall.nearRelief(),
                wall.steepness(),
                wall.outwardNormalX(),
                wall.outwardNormalZ(),
                wall.lowY(),
                wall.highY(),
                wall.signedFaceDistance(),
                2.20,
                wall.highSide(),
                wall.nearProbeDistance(),
                wall.farProbeDistance()
        );
        RockSurfaceErosionField.Column synthetic = new RockSurfaceErosionField.Column(
                true,
                SEEDS[0],
                0.5,
                0.5,
                220,
                220,
                insetWall,
                1.0,
                0.0,
                Math.max(0.52, surface.strength()),
                0.0,
                160.0,
                Math.max(1, surface.maxRetreatBlocks()),
                Math.max(6.0, surface.scale()),
                Math.max(2.0, surface.detailScale()),
                erosion,
                surface,
                MassifFractureField.NONE
        );
        int deepSoftRemoved = 0;
        int deepVeryHardRemoved = 0;
        int distinctBands = 0;
        boolean previousRemoved = false;
        for (int y = wall.lowY() + 5; y <= wall.highY() - 8; y++) {
            boolean softRemoved = !synthetic.occupies(y, soft);
            boolean veryHardRemoved = !synthetic.occupies(y, veryHard);
            if (softRemoved) {
                deepSoftRemoved++;
            }
            if (veryHardRemoved) {
                deepVeryHardRemoved++;
            }
            if (softRemoved && !previousRemoved) {
                distinctBands++;
            }
            previousRemoved = softRemoved;
        }
        require(deepSoftRemoved > 12,
                "ordinary erosion did not operate down the full synthetic wall");
        require(deepSoftRemoved > deepVeryHardRemoved,
                "soft wall did not recede farther than very-hard wall");
        require(distinctBands > 0, "synthetic face recession produced no coherent band");

        int fissureComparisons = 0;
        boolean fissureExtraRemoval = false;
        SurfaceCoordinate wallRepresentative = null;
        SurfaceCoordinate fissureRepresentative = null;
        for (int angleIndex = 0;
                angleIndex < 32
                        && (wallRepresentative == null || !fissureExtraRemoval);
                angleIndex++) {
            double angle = angleIndex * Math.PI * 2.0 / 32.0;
            for (int radius = 1100; radius <= 6800; radius += 3) {
                double x = Math.floor(Math.cos(angle) * radius) + 0.5;
                double z = Math.floor(Math.sin(angle) * radius) + 0.5;
                Evaluation evaluation = evaluate(SEEDS[0], x, z, settings);
                RockSurfaceErosionField.Column withFracture = evaluation.surfaceErosion();
                if (wallRepresentative == null
                        && withFracture.active()
                        && evaluation.face().highSide()
                        && evaluation.face().exposure() > 0.50
                        && evaluation.face().localRelief() > 30.0) {
                    int removed = 0;
                    int firstY = Math.max(
                            evaluation.face().lowY() + 5,
                            MacroGeologyField.BASE_SURFACE_Y + 3
                    );
                    int lastY = Math.min(
                            evaluation.face().highY() - 8,
                            evaluation.fissureTopY()
                    );
                    for (int y = firstY; y <= lastY; y++) {
                        if (!withFracture.occupies(y, productionMaterialAt(evaluation, y))) {
                            removed++;
                        }
                    }
                    if (removed > 2) {
                        wallRepresentative = new SurfaceCoordinate(
                                x,
                                z,
                                Math.min(319, Math.max(80, evaluation.face().highY() + 30)),
                                evaluation.face().exposure(),
                                evaluation.face().localRelief(),
                                removed,
                                withFracture.fractureStrength()
                        );
                    }
                }
                if (!withFracture.active()
                        || withFracture.fractureStrength() <= 0.05
                        || evaluation.fracture().distance()
                        <= evaluation.fracture().halfWidth()) {
                    continue;
                }
                RockSurfaceErosionField.Column withoutFracture =
                        RockSurfaceErosionField.sample(
                                SEEDS[0],
                                x,
                                z,
                                evaluation.originalTopY(),
                                evaluation.fissureTopY(),
                                evaluation.geology(),
                                evaluation.face(),
                                MassifFractureField.NONE,
                                settings
                        );
                int withRemoved = 0;
                int withoutRemoved = 0;
                int bottom = Math.max(
                        MacroGeologyField.BASE_SURFACE_Y + 3,
                        (int) Math.floor(withFracture.fractureBottomY() - 2.0)
                );
                for (int y = bottom; y <= evaluation.fissureTopY(); y++) {
                    if (!withFracture.occupies(y, soft)) {
                        withRemoved++;
                    }
                    if (!withoutFracture.occupies(y, soft)) {
                        withoutRemoved++;
                    }
                }
                if (withFracture.fractureBottomY()
                        >= MacroGeologyField.BASE_SURFACE_Y + 7.0) {
                    int below = (int) Math.floor(withFracture.fractureBottomY() - 4.0);
                    require(withFracture.occupies(below, soft)
                                    == withoutFracture.occupies(below, soft),
                            "fissure surface pass deepened below its authoritative depth");
                }
                fissureComparisons++;
                boolean extraRemoval = withRemoved > withoutRemoved;
                fissureExtraRemoval |= extraRemoval;
                if (extraRemoval && fissureRepresentative == null) {
                    fissureRepresentative = new SurfaceCoordinate(
                            x,
                            z,
                            Math.min(319, Math.max(80, evaluation.originalTopY() + 28)),
                            evaluation.face().exposure(),
                            evaluation.face().localRelief(),
                            withRemoved - withoutRemoved,
                            withFracture.fractureStrength()
                    );
                }
            }
        }
        require(fissureComparisons > 0, "no surface fissure wall was compared");
        require(fissureExtraRemoval, "fissure multiplier produced no extra wall recession");
        require(wallRepresentative != null, "no seed-0 whole-face screenshot site found");
        require(fissureRepresentative != null, "no seed-0 fissure screenshot site found");
        System.out.printf(
                Locale.ROOT,
                "Recommended 0.5.14.2 wall screenshot: seed=0, x=%.1f, z=%.1f, y=%d, "
                        + "exposure=%.3f, relief=%.1f, deep_removed=%d.%n",
                wallRepresentative.x(),
                wallRepresentative.z(),
                wallRepresentative.suggestedY(),
                wallRepresentative.exposure(),
                wallRepresentative.relief(),
                wallRepresentative.removedBlocks()
        );
        System.out.printf(
                Locale.ROOT,
                "Recommended 0.5.14.2 fissure screenshot: seed=0, x=%.1f, z=%.1f, y=%d, "
                        + "fracture=%.3f, extra_removed=%d.%n",
                fissureRepresentative.x(),
                fissureRepresentative.z(),
                fissureRepresentative.suggestedY(),
                fissureRepresentative.fractureStrength(),
                fissureRepresentative.removedBlocks()
        );
    }

    private static void validateBasinAndDunes(ArrakisTerrainSettings settings) {
        double basin = settings.basin().pureSandRadius();
        double[][] basinPoints = {
                {0.5, 0.5},
                {basin * 0.40, basin * 0.20},
                {-basin * 0.55, basin * 0.15}
        };
        for (long seed : SEEDS) {
            for (double[] point : basinPoints) {
                Evaluation evaluation = evaluate(seed, point[0], point[1], settings);
                require(evaluation.geology().baseElevation() == MacroGeologyField.BASE_SURFACE_Y,
                        "pure basin macro height changed");
                require(!evaluation.erosion().candidate(), "erosion activated in pure basin");
                require(evaluation.erosion().talusThickness() == 0,
                        "talus activated in pure basin");
                require(evaluation.erosion().windExposure() == 0.0
                                && evaluation.erosion().fractureErosion() == 0.0
                                && evaluation.erosion().undercutPotential() == 0.0
                                && evaluation.erosion().escarpmentStrength() == 0.0
                                && evaluation.erosion().maximumRetreat() == 0.0
                                && evaluation.erosion().talusSuitability() == 0.0,
                        "inactive basin column retained active erosion metadata");
            }
        }

        int positiveDunes = 0;
        double farRadius = settings.outerTransition().openErgFullRadius() + 512.0;
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2.0 / 48.0;
            double x = Math.cos(angle) * farRadius;
            double z = Math.sin(angle) * farRadius;
            MacroGeologyField.Sample geology = MacroGeologyField.sample(
                    SEEDS[1], x, z, settings
            );
            NativeTransverseDuneField.Sample dune = NativeTransverseDuneField.sample(
                    SEEDS[1], x, z, geology.duneSuitability(), settings.nativeDunes()
            );
            require(dune.heightBlocks() >= 0.0
                            && dune.heightBlocks() <= settings.nativeDunes().maxHeight(),
                    "native dune height escaped configured range");
            if (dune.heightBlocks() > 0.25) {
                positiveDunes++;
            }
            require(!evaluate(SEEDS[1], x, z, settings).erosion().candidate(),
                    "erosion activated in open erg");
        }
        require(positiveDunes > 0, "native dunes disappeared from open erg");
    }

    /**
     * Exercises the analytic production fields through absolute block-center coordinates.
     * The same halo column is addressed from both neighboring chunks, and complete chunks are
     * sampled in opposite orders to catch accidental future chunk-local state or clamping.
     */
    private static SeamCounts validateChunkBoundaryOrderIndependence(
            ArrakisTerrainSettings settings
    ) {
        int massifChunk = Math.floorDiv((int) settings.massif().startRadius(), CHUNK_SIZE);
        int brokenChunk = Math.floorDiv((int) settings.brokenRock().startRadius(), CHUNK_SIZE);
        int diagonalChunk = Math.floorDiv(
                (int) (settings.massif().outerStartRadius() / Math.sqrt(2.0)),
                CHUNK_SIZE
        );
        int[][] anchors = {
                {massifChunk, 0},
                {-massifChunk - 1, 0},
                {diagonalChunk, diagonalChunk},
                {0, brokenChunk}
        };

        int seamColumns = 0;
        int orderColumns = 0;
        for (long seed : SEEDS) {
            for (int[] anchor : anchors) {
                seamColumns += validateEastWestSeam(
                        seed, anchor[0], anchor[1], settings
                );
                seamColumns += validateNorthSouthSeam(
                        seed, anchor[0], anchor[1], settings
                );
                orderColumns += validateChunkPairOrder(
                        seed, anchor[0], anchor[1], 1, 0, settings
                );
                orderColumns += validateChunkPairOrder(
                        seed, anchor[0], anchor[1], 0, 1, settings
                );
            }
        }
        require(seamColumns > 0, "no chunk seam columns were validated");
        require(orderColumns > 0, "no reversed-order chunk columns were validated");
        return new SeamCounts(seamColumns, orderColumns);
    }

    private static int validateEastWestSeam(
            long seed,
            int westChunkX,
            int chunkZ,
            ArrakisTerrainSettings settings
    ) {
        int checked = 0;
        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            requireSameColumn(
                    seed,
                    westChunkX,
                    chunkZ,
                    CHUNK_SIZE,
                    localZ,
                    westChunkX + 1,
                    chunkZ,
                    0,
                    localZ,
                    settings,
                    "east/west boundary"
            );
            requireSameColumn(
                    seed,
                    westChunkX,
                    chunkZ,
                    CHUNK_SIZE - 1,
                    localZ,
                    westChunkX + 1,
                    chunkZ,
                    -1,
                    localZ,
                    settings,
                    "east/west halo"
            );
            checked += 2;
        }
        return checked;
    }

    private static int validateNorthSouthSeam(
            long seed,
            int chunkX,
            int northChunkZ,
            ArrakisTerrainSettings settings
    ) {
        int checked = 0;
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            requireSameColumn(
                    seed,
                    chunkX,
                    northChunkZ,
                    localX,
                    CHUNK_SIZE,
                    chunkX,
                    northChunkZ + 1,
                    localX,
                    0,
                    settings,
                    "north/south boundary"
            );
            requireSameColumn(
                    seed,
                    chunkX,
                    northChunkZ,
                    localX,
                    CHUNK_SIZE - 1,
                    chunkX,
                    northChunkZ + 1,
                    localX,
                    -1,
                    settings,
                    "north/south halo"
            );
            checked += 2;
        }
        return checked;
    }

    private static void requireSameColumn(
            long seed,
            int firstChunkX,
            int firstChunkZ,
            int firstLocalX,
            int firstLocalZ,
            int secondChunkX,
            int secondChunkZ,
            int secondLocalX,
            int secondLocalZ,
            ArrakisTerrainSettings settings,
            String label
    ) {
        long first = chunkColumnSignature(
                seed,
                firstChunkX,
                firstChunkZ,
                firstLocalX,
                firstLocalZ,
                settings
        );
        long second = chunkColumnSignature(
                seed,
                secondChunkX,
                secondChunkZ,
                secondLocalX,
                secondLocalZ,
                settings
        );
        require(first == second, label + " changed at seed " + seed);
    }

    private static int validateChunkPairOrder(
            long seed,
            int firstChunkX,
            int firstChunkZ,
            int secondOffsetX,
            int secondOffsetZ,
            ArrakisTerrainSettings settings
    ) {
        long[] forward = new long[CHUNK_SIZE * CHUNK_SIZE * 2];
        long[] reverse = new long[forward.length];
        fillChunkSignatures(
                seed, firstChunkX, firstChunkZ, settings, forward, 0, false
        );
        fillChunkSignatures(
                seed,
                firstChunkX + secondOffsetX,
                firstChunkZ + secondOffsetZ,
                settings,
                forward,
                CHUNK_SIZE * CHUNK_SIZE,
                false
        );

        // Deliberately visit both the chunks and their local columns in the opposite order.
        fillChunkSignatures(
                seed,
                firstChunkX + secondOffsetX,
                firstChunkZ + secondOffsetZ,
                settings,
                reverse,
                CHUNK_SIZE * CHUNK_SIZE,
                true
        );
        fillChunkSignatures(
                seed, firstChunkX, firstChunkZ, settings, reverse, 0, true
        );

        for (int index = 0; index < forward.length; index++) {
            require(forward[index] == reverse[index],
                    "chunk sampling order changed absolute terrain at seed " + seed);
        }
        return forward.length;
    }

    private static void fillChunkSignatures(
            long seed,
            int chunkX,
            int chunkZ,
            ArrakisTerrainSettings settings,
            long[] output,
            int outputOffset,
            boolean reverse
    ) {
        int columns = CHUNK_SIZE * CHUNK_SIZE;
        for (int visit = 0; visit < columns; visit++) {
            int canonical = reverse ? columns - 1 - visit : visit;
            int localX = canonical % CHUNK_SIZE;
            int localZ = canonical / CHUNK_SIZE;
            output[outputOffset + canonical] = chunkColumnSignature(
                    seed, chunkX, chunkZ, localX, localZ, settings
            );
        }
    }

    private static long chunkColumnSignature(
            long seed,
            int chunkX,
            int chunkZ,
            int localX,
            int localZ,
            ArrakisTerrainSettings settings
    ) {
        double x = (long) chunkX * CHUNK_SIZE + localX + 0.5;
        double z = (long) chunkZ * CHUNK_SIZE + localZ + 0.5;
        return columnSignature(evaluate(seed, x, z, settings));
    }

    private static ValidationCounts validateEscarpments(ArrakisTerrainSettings settings) {
        int candidates = 0;
        int undercutColumns = 0;
        int explicitUndercutCandidates = 0;
        int talusColumns = 0;
        int fractureComparisons = 0;
        double maximumRetreat = 0.0;
        long hash = 0xCBF29CE484222325L;
        CandidateCoordinate[] representatives = new CandidateCoordinate[SEEDS.length];
        CandidateCoordinate undercutRepresentative = null;
        CandidateCoordinate talusRepresentative = null;

        for (int seedIndex = 0; seedIndex < SEEDS.length; seedIndex++) {
            long seed = SEEDS[seedIndex];
            for (int angleIndex = 0; angleIndex < 32; angleIndex++) {
                double angle = angleIndex * Math.PI * 2.0 / 32.0;
                for (int radius = 2800; radius <= 6750; radius += 7) {
                    double x = Math.floor(Math.cos(angle) * radius) + 0.5;
                    double z = Math.floor(Math.sin(angle) * radius) + 0.5;
                    Evaluation first = evaluate(seed, x, z, settings);
                    Evaluation second = evaluate(seed, x, z, settings);
                    long firstHash = evaluationHash(first);
                    require(firstHash == evaluationHash(second),
                            "same seed/coordinate produced different erosion samples");
                    hash = mix(hash, firstHash);

                    EscarpmentErosionField.Column erosion = first.erosion();
                    if (!erosion.candidate()) {
                        continue;
                    }
                    candidates++;
                    maximumRetreat = Math.max(maximumRetreat, erosion.maximumRetreat());
                    if (erosion.undercutPotential() > 0.05) {
                        explicitUndercutCandidates++;
                    }
                    require(erosion.maximumRetreat()
                                    <= settings.erosion().maxUndercutBlocks() + 1.0e-9,
                            "undercut exceeds configured horizontal reach");
                    require(first.fissureTopY() <= first.originalTopY(),
                            "erosion/fissure evaluation raised macro rock");
                    if (erosion.talusThickness() > 0) {
                        talusColumns++;
                        require(erosion.talusThickness()
                                        <= settings.lithology().talus().maximumThickness(),
                                "talus exceeds configured maximum thickness");
                    }

                    boolean rockBelow = false;
                    boolean gapAfterRock = false;
                    boolean rockAboveGap = false;
                    int removedBlocks = 0;
                    for (int y = MacroGeologyField.BASE_SURFACE_Y + 3;
                            y <= first.fissureTopY();
                            y++) {
                        boolean occupied = erosion.occupies(
                                y,
                                productionMaterialAt(first, y)
                        );
                        if (occupied) {
                            rockAboveGap |= gapAfterRock;
                            rockBelow = true;
                        } else if (rockBelow) {
                            gapAfterRock = true;
                        }
                        if (!occupied) {
                            removedBlocks++;
                        }
                    }
                    if (rockAboveGap) {
                        undercutColumns++;
                    }

                    int highest = erosion.highestRockY(
                            first.lithology(),
                            first.fracture()
                    );
                    if (highest > MacroGeologyField.BASE_SURFACE_Y) {
                        require(erosion.occupies(
                                        MacroGeologyField.BASE_SURFACE_Y + 1,
                                        first.lithology().sample(
                                                MacroGeologyField.BASE_SURFACE_Y + 1
                                        )
                                ),
                                "visible rock lost hard-crust connection");
                    }

                    CandidateCoordinate candidate = new CandidateCoordinate(
                            seed,
                            x,
                            z,
                            Math.min(319, Math.max(80, highest + 36)),
                            erosion.escarpmentStrength(),
                            erosion.localRelief(),
                            erosion.maximumRetreat(),
                            removedBlocks,
                            rockAboveGap,
                            erosion.undercutPotential(),
                            erosion.fractureErosion(),
                            erosion.talusSuitability(),
                            erosion.talusThickness()
                    );
                    if (representatives[seedIndex] == null
                            || candidate.score() > representatives[seedIndex].score()) {
                        representatives[seedIndex] = candidate;
                    }
                    if (candidate.undercutPotential() > 0.05
                            && (undercutRepresentative == null
                            || candidate.undercutPotential()
                            > undercutRepresentative.undercutPotential())) {
                        undercutRepresentative = candidate;
                    }
                    if (candidate.talusThickness() > 0
                            && (talusRepresentative == null
                            || candidate.talusThickness()
                            > talusRepresentative.talusThickness()
                            || (candidate.talusThickness()
                            == talusRepresentative.talusThickness()
                            && candidate.talusSuitability()
                            > talusRepresentative.talusSuitability()))) {
                        talusRepresentative = candidate;
                    }

                    if (fractureComparisons < 256
                            && first.fracture().activation() > 0.0
                            && erosion.fractureErosion() > 1.0e-9) {
                        EscarpmentErosionField.Column withoutFracture =
                                EscarpmentErosionField.sample(
                                        seed,
                                        x,
                                        z,
                                        first.originalTopY(),
                                        first.fissureTopY(),
                                        first.geology(),
                                        first.face(),
                                        first.lithology(),
                                        MassifFractureField.NONE,
                                        settings
                                );
                        require(withoutFracture.candidate(),
                                "removing fracture metadata changed escarpment candidacy");
                        require(erosion.fractureErosion()
                                        >= withoutFracture.fractureErosion(),
                                "fracture metadata reduced fracture erosion");
                        require(erosion.maximumRetreat() + 1.0e-9
                                        >= withoutFracture.maximumRetreat(),
                                "fracture metadata reduced maximum retreat");

                        int withFractureRemoved = 0;
                        int withoutFractureRemoved = 0;
                        for (int y = MacroGeologyField.BASE_SURFACE_Y + 3;
                                y <= first.fissureTopY();
                                y++) {
                            LithologyField.Sample material = first.lithology().sample(y);
                            if (!erosion.occupies(y, material)) {
                                withFractureRemoved++;
                            }
                            if (!withoutFracture.occupies(y, material)) {
                                withoutFractureRemoved++;
                            }
                        }
                        require(withFractureRemoved >= withoutFractureRemoved,
                                "fracture influence restored eroded rock");
                        fractureComparisons++;
                    }
                }
            }
        }

        require(candidates > 40, "no meaningful escarpment candidate population found");
        require(undercutColumns > 0, "no bounded rock-air-rock undercut columns found");
        require(talusColumns > 0, "no localized talus columns found");
        require(fractureComparisons > 0,
                "no fracture-influenced escarpment candidates were compared");
        require(undercutRepresentative != null,
                "no explicit lithology-supported undercut candidate found");
        require(talusRepresentative != null,
                "no representative talus coordinate found");
        for (int index = 0; index < representatives.length; index++) {
            require(representatives[index] != null,
                    "no representative erosion coordinate found for seed " + SEEDS[index]);
        }
        CandidateCoordinate[] reported = new CandidateCoordinate[representatives.length + 2];
        System.arraycopy(representatives, 0, reported, 0, representatives.length);
        reported[representatives.length] = undercutRepresentative;
        reported[representatives.length + 1] = talusRepresentative;
        return new ValidationCounts(
                candidates,
                undercutColumns,
                explicitUndercutCandidates,
                talusColumns,
                fractureComparisons,
                hash,
                maximumRetreat,
                reported
        );
    }

    private static void validateFaultFloors(ArrakisTerrainSettings settings) {
        int fullFaultCores = 0;
        for (int angleIndex = 0; angleIndex < 48 && fullFaultCores < 8; angleIndex++) {
            double angle = angleIndex * Math.PI * 2.0 / 48.0;
            for (int radius = 1500; radius <= 5700; radius += 9) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                MacroGeologyField.Sample geology = MacroGeologyField.sample(
                        SEEDS[0], x, z, settings
                );
                if (geology.faultCarveMask() < 0.999) {
                    continue;
                }
                double expected = settings.faults().rockyFloorHeight()
                        * (1.0 - geology.faultSandFloorMask());
                require(geology.addedRockHeight() <= expected + 0.75,
                        "0.5.12 absolute fault floor was raised");
                Evaluation evaluation = evaluate(SEEDS[0], x, z, settings);
                require(!evaluation.erosion().candidate(),
                        "major erosion attempted to bridge a full fault core");
                require(!evaluation.surfaceErosion().active(),
                        "surface erosion attempted to alter a full fault core");
                fullFaultCores++;
            }
        }
        require(fullFaultCores > 0, "validation did not locate a full fault core");
    }

    private static Evaluation evaluate(
            long seed,
            double x,
            double z,
            ArrakisTerrainSettings settings
    ) {
        MacroGeologyField.Sample geology = MacroGeologyField.sample(seed, x, z, settings);
        NativeTransverseDuneField.Sample dune = NativeTransverseDuneField.sample(
                seed,
                x,
                z,
                geology.duneSuitability(),
                settings.nativeDunes()
        );
        int originalTopY = Math.max(
                MacroGeologyField.BASE_SURFACE_Y,
                (int) Math.floor(geology.baseElevation() + 0.5)
        );
        LithologyField.Column lithology = LithologyField.column(
                seed, x, z, settings.lithology()
        );
        LithologyField.Sample surface = lithology.sample(originalTopY);
        MassifFractureField.Sample fracture = MassifFractureField.sample(
                seed,
                x,
                z,
                originalTopY,
                geology,
                surface.resistance(),
                settings.fractures()
        );
        int carveDepth = Math.min(
                (int) Math.floor(fracture.carveDepth()),
                Math.max(0, originalTopY - (MacroGeologyField.BASE_SURFACE_Y + 1))
        );
        int fissureTopY = originalTopY - carveDepth;
        RockFaceExposure.Sample face = RockFaceExposure.sample(
                seed,
                x,
                z,
                originalTopY,
                geology,
                settings
        );
        EscarpmentErosionField.Column erosion = EscarpmentErosionField.sample(
                seed,
                x,
                z,
                originalTopY,
                fissureTopY,
                geology,
                face,
                lithology,
                fracture,
                settings
        );
        RockSurfaceErosionField.Column surfaceErosion = RockSurfaceErosionField.sample(
                seed,
                x,
                z,
                originalTopY,
                fissureTopY,
                geology,
                face,
                fracture,
                settings
        );
        return new Evaluation(
                geology,
                dune,
                lithology,
                fracture,
                face,
                erosion,
                surfaceErosion,
                originalTopY,
                fissureTopY
        );
    }

    private static long evaluationHash(Evaluation evaluation) {
        EscarpmentErosionField.Column erosion = evaluation.erosion();
        long hash = Double.doubleToLongBits(evaluation.geology().baseElevation());
        hash = mix(hash, evaluation.originalTopY());
        hash = mix(hash, evaluation.fissureTopY());
        hash = mix(hash, evaluation.dune().surfaceUnits());
        hash = mix(hash, Double.doubleToLongBits(evaluation.face().exposure()));
        hash = mix(hash, Double.doubleToLongBits(evaluation.face().localRelief()));
        hash = mix(hash, erosion.candidate() ? 1L : 0L);
        hash = mix(hash, Double.doubleToLongBits(erosion.escarpmentStrength()));
        hash = mix(hash, Double.doubleToLongBits(erosion.signedFaceDistance()));
        hash = mix(hash, Double.doubleToLongBits(erosion.windExposure()));
        hash = mix(hash, Double.doubleToLongBits(erosion.fractureErosion()));
        hash = mix(hash, Double.doubleToLongBits(erosion.undercutPotential()));
        hash = mix(hash, Double.doubleToLongBits(erosion.maximumRetreat()));
        hash = mix(hash, erosion.talusThickness());
        return hash;
    }

    /** Compact signature of the same lithology/fracture/occupancy calls used by worldgen. */
    private static long columnSignature(Evaluation evaluation) {
        long hash = evaluationHash(evaluation);
        EscarpmentErosionField.Column erosion = evaluation.erosion();
        RockSurfaceErosionField.Column surfaceErosion = evaluation.surfaceErosion();
        int highest = surfaceErosion.highestRockY(
                evaluation.lithology(),
                evaluation.fracture(),
                erosion
        );
        hash = mix(hash, highest);

        int firstY = MacroGeologyField.BASE_SURFACE_Y + 1;
        int span = Math.max(0, evaluation.fissureTopY() - firstY);
        for (int sampleIndex = 0; sampleIndex <= 8; sampleIndex++) {
            int y = firstY + span * sampleIndex / 8;
            LithologyField.Sample material = productionMaterialAt(evaluation, y);
            hash = mix(hash, y);
            hash = mix(hash, material.material().ordinal());
            hash = mix(hash, material.resistance().ordinal());
            hash = mix(hash, material.limestoneHost() ? 1L : 0L);
            hash = mix(hash, material.calciteVein() ? 1L : 0L);
            hash = mix(hash, erosion.occupies(y, material) ? 1L : 0L);
            hash = mix(hash, surfaceErosion.occupies(y, material) ? 1L : 0L);
        }

        for (int index = 0; index < erosion.talusThickness(); index++) {
            int duneFullTopY = MacroGeologyField.BASE_SURFACE_Y
                    + evaluation.dune().surfaceUnits()
                    / NativeTransverseDuneField.SUBDIVISIONS;
            int talusBaseY = Math.max(
                    Math.max(MacroGeologyField.BASE_SURFACE_Y + 1, highest + 1),
                    duneFullTopY + 1
            );
            int y = talusBaseY + index;
            hash = mix(hash, erosion.talusMaterialAt(y, evaluation.lithology()).ordinal());
        }
        return hash;
    }

    /** Mirrors TerrainColumn.materialSampleAt so validation observes production calcite walls. */
    private static LithologyField.Sample productionMaterialAt(
            Evaluation evaluation,
            int y
    ) {
        LithologyField.Sample sample = evaluation.lithology().sample(y);
        if (!evaluation.fracture().calciteExposure(
                y,
                evaluation.originalTopY(),
                evaluation.fissureTopY()
        )) {
            return sample;
        }
        return new LithologyField.Sample(
                LithologyField.Material.CALCITE,
                LithologyField.ResistanceClass.MEDIUM,
                sample.limestoneHost(),
                false,
                false,
                true
        );
    }

    private static long mix(long hash, long value) {
        long mixed = hash ^ value;
        mixed *= 0x100000001B3L;
        return mixed;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record Profile(JsonObject json, ArrakisTerrainSettings settings) {
    }

    private record SeamCounts(int seamColumns, int orderColumns) {
    }

    private record SurfaceCoordinate(
            double x,
            double z,
            int suggestedY,
            double exposure,
            double relief,
            int removedBlocks,
            double fractureStrength
    ) {
    }

    private record CandidateCoordinate(
            long seed,
            double x,
            double z,
            int suggestedY,
            double escarpmentStrength,
            double localRelief,
            double maximumRetreat,
            int removedBlocks,
            boolean undercutColumn,
            double undercutPotential,
            double fractureErosion,
            double talusSuitability,
            int talusThickness
    ) {
        double score() {
            return (undercutColumn ? 4.0 : 0.0)
                    + (talusThickness > 0 ? 2.0 + talusThickness * 0.25 : 0.0)
                    + removedBlocks * 0.02
                    + escarpmentStrength * 2.0
                    + maximumRetreat * 0.30
                    + undercutPotential * 2.0
                    + fractureErosion * 0.35
                    + talusSuitability;
        }
    }

    private record Evaluation(
            MacroGeologyField.Sample geology,
            NativeTransverseDuneField.Sample dune,
            LithologyField.Column lithology,
            MassifFractureField.Sample fracture,
            RockFaceExposure.Sample face,
            EscarpmentErosionField.Column erosion,
            RockSurfaceErosionField.Column surfaceErosion,
            int originalTopY,
            int fissureTopY
    ) {
    }

    private record ValidationCounts(
            int candidates,
            int undercutColumns,
            int explicitUndercutCandidates,
            int talusColumns,
            int fractureComparisons,
            long hash,
            double maximumRetreat,
            CandidateCoordinate[] representatives
    ) {
    }
}
