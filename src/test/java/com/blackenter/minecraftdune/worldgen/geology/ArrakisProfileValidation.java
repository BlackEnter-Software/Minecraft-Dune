package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Codec compatibility and semantic profile invariants; always build-blocking. */
public final class ArrakisProfileValidation {
    private ArrakisProfileValidation() {}
    public static Profile validate() throws IOException {
        Profile profile = loadProfile();
        ArrakisTerrainSettings settings = profile.settings();
        require(settings.profileVersion() == 5148, "active profile_version must be 5148");
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
        legacy5142Profile.getAsJsonObject("erosion")
                .getAsJsonObject("surface")
                .remove("base_anchored_erosion");
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
        require(!legacy5142.erosion().surface().baseAnchoredErosion(),
                "missing base-anchored face erosion must retain the old erosion floor");

        JsonObject oldProfile = legacy5142Profile.deepCopy();
        oldProfile.addProperty("profile_version", 513);
        oldProfile.remove("erosion");
        ArrakisTerrainSettings backward = ArrakisTerrainSettings.CODEC
                .parse(JsonOps.INSTANCE, oldProfile)
                .getOrThrow();
        require(!backward.erosion().enabled(), "missing erosion group must decode disabled");
        validateRejectedProfiles(profile.json());

        JsonObject encoded = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                .getOrThrow().getAsJsonObject();
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(settings),
                "current terrain profile did not round-trip");
        return profile;
    }

    public static Profile loadProfile() throws IOException {
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

    private static void validateRejectedProfiles(JsonObject validProfile) {
        JsonObject reversedBasin = validProfile.deepCopy();
        reversedBasin.getAsJsonObject("basin")
                .addProperty("pure_sand_radius", 2_500.0);
        require(
                ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, reversedBasin)
                        .error()
                        .isPresent(),
                "reversed basin radii must be rejected"
        );

        JsonObject unboundedOrphanSearch = validProfile.deepCopy();
        unboundedOrphanSearch.getAsJsonObject("erosion")
                .getAsJsonObject("orphan_remnants")
                .addProperty("lateral_search_radius", 10_000);
        require(
                ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, unboundedOrphanSearch)
                        .error()
                        .isPresent(),
                "pathological orphan search radius must be rejected"
        );

        JsonObject invalidMaterial = validProfile.deepCopy();
        invalidMaterial.getAsJsonObject("lithology")
                .getAsJsonObject("materials")
                .addProperty("background", "not a resource location");
        require(
                ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalidMaterial)
                        .error()
                        .isPresent(),
                "invalid material identifiers must be rejected"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    public record Profile(JsonObject json, ArrakisTerrainSettings settings) {}
}
