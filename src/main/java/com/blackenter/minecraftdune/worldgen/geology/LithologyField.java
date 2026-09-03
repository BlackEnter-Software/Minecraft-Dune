package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Deterministic 3D/stratigraphic native-rock field.
 *
 * <p>Materials are geological roles, not decoration. Soft units are future erosion/cavern
 * hosts, hard intrusions retain structure, very-hard volcanic bodies are intended to survive
 * the later coronal-wind pass, and gravel is loose talus rather than intact bedrock.</p>
 */
public final class LithologyField {
    private static final long STRATA_WARP_SALT = 0x2D65A48B1EF703C9L;
    private static final long HOST_SALT = 0x67FC51B328DA904EL;
    private static final long BAND_SALT = 0x0AA718DB4E39C625L;
    private static final long INTRUSION_SALT = 0x43E975A10CBD82F6L;
    private static final long GRANITE_SELECTOR_SALT = 0x58C16E4B29A7D03FL;
    private static final long HOST_VARIANT_SALT = 0x26A7D14C9B53E80FL;
    private static final long BASEMENT_WARP_SALT = 0x1A4FD72C83E659B0L;
    private static final long RARE_BODY_SALT = 0x75BD094FC30A16E8L;
    private static final long SHEET_SALT = 0x61D38FA2940CE75BL;
    private static final long SHEET_WARP_SALT = 0x54C0B78E129DA36FL;
    private static final long CALCITE_BAND_SALT = 0x36F2C1598AB74D0EL;
    private static final long CALCITE_GATE_SALT = 0x19A6E42D73BF580CL;
    private static final long CONTACT_DETAIL_SALT = 0x4EA8D216B07C953FL;
    private static final long CONTACT_MICRO_SALT = 0x72C1F4093DA65B8EL;

    private LithologyField() {
    }

    public static Column column(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings.LithologySettings settings
    ) {
        return new Column(
                worldSeed,
                worldX,
                worldZ,
                settings,
                ArrakisTerrainSettings.DEFAULT_ADDITIONAL_MATERIALS
        );
    }

    public static Column column(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings.LithologySettings settings,
            ArrakisTerrainSettings.AdditionalMaterialSettings additionalMaterials
    ) {
        return new Column(
                worldSeed,
                worldX,
                worldZ,
                settings,
                additionalMaterials
        );
    }

    /** Strata, sheets and intrusions share the same displaced geological Y coordinate. */
    public static Column column(long seed, double x, double z, ArrakisTerrainSettings.LithologySettings settings,
            ArrakisTerrainSettings.AdditionalMaterialSettings additional, double structuralDisplacement) {
        return new Column(seed, x, z, settings, additional, structuralDisplacement);
    }

    public enum ResistanceClass {
        SOFT("soft", 1.18),
        MEDIUM("medium", 1.0),
        HARD("hard", 0.82),
        VERY_HARD("very_hard", 0.66),
        LOOSE("loose", 1.35);

        private final String commandName;
        private final double fractureFactor;

        ResistanceClass(String commandName, double fractureFactor) {
            this.commandName = commandName;
            this.fractureFactor = fractureFactor;
        }

        public String commandName() {
            return commandName;
        }

        public double fractureFactor() {
            return fractureFactor;
        }
    }

    public enum Material {
        STONE("stone", ResistanceClass.MEDIUM, "background structural host"),
        SANDSTONE("sandstone", ResistanceClass.SOFT, "soft sedimentary unit"),
        TUFF("tuff", ResistanceClass.SOFT, "soft altered volcanic unit"),
        LIMESTONE("limestone", ResistanceClass.SOFT, "rare future cavern host"),
        CALCITE("calcite", ResistanceClass.MEDIUM, "horizontal mineral band/fracture exposure"),
        ANDESITE("andesite", ResistanceClass.HARD, "hard intrusive body"),
        DIORITE("diorite", ResistanceClass.HARD, "hard intrusive body"),
        GRANITE("granite", ResistanceClass.HARD, "coherent hard plutonic intrusion"),
        BASALT("basalt", ResistanceClass.VERY_HARD, "very-hard resistant sheet"),
        SMOOTH_BASALT("smooth_basalt", ResistanceClass.HARD, "hard altered basalt-sheet margin"),
        BLACKSTONE("blackstone", ResistanceClass.VERY_HARD, "rare ancient resistant body"),
        DEEPSLATE("deepslate", ResistanceClass.HARD, "hard ancient basement exposed by deep cuts"),
        RED_SANDSTONE("red_sandstone", ResistanceClass.SOFT, "soft oxidized sedimentary unit"),
        TERRACOTTA("terracotta", ResistanceClass.MEDIUM, "medium clay-rich sedimentary unit"),
        GRAVEL("gravel", ResistanceClass.LOOSE, "loose talus/collapse material");

        private final String commandName;
        private final ResistanceClass resistance;
        private final String geologicalRole;

        Material(String commandName, ResistanceClass resistance, String geologicalRole) {
            this.commandName = commandName;
            this.resistance = resistance;
            this.geologicalRole = geologicalRole;
        }

        public String commandName() {
            return commandName;
        }

        public ResistanceClass resistance() {
            return resistance;
        }

        public String geologicalRole() {
            return geologicalRole;
        }
    }

    public record Sample(
            Material material,
            ResistanceClass resistance,
            boolean limestoneHost,
            boolean intrusive,
            boolean basaltStructure,
            boolean calciteVein
    ) {
    }

    public static final class Column {
        private final long worldSeed;
        private final double worldX;
        private final double worldZ;
        private final ArrakisTerrainSettings.LithologySettings settings;
        private final ArrakisTerrainSettings.AdditionalMaterialSettings additionalMaterials;
        private final double strataWarp;
        private final double sheetWarp;
        private final double sheetGate;
        private final double calciteGate;
        private final double calciteBandOffset;
        private final double structuralDisplacement;

        private Column(
                long worldSeed,
                double worldX,
                double worldZ,
                ArrakisTerrainSettings.LithologySettings settings,
                ArrakisTerrainSettings.AdditionalMaterialSettings additionalMaterials
        ) {
            this(worldSeed, worldX, worldZ, settings, additionalMaterials, 0.0);
        }

        private Column(long worldSeed, double worldX, double worldZ,
                ArrakisTerrainSettings.LithologySettings settings,
                ArrakisTerrainSettings.AdditionalMaterialSettings additionalMaterials, double structuralDisplacement) {
            this.worldSeed = worldSeed;
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.settings = settings;
            this.additionalMaterials = additionalMaterials;
            this.structuralDisplacement = structuralDisplacement;

            double strataWarpScale = Math.max(1.0, settings.strataWarpScale());
            strataWarp = settings.strataWarpStrength() * GeologyNoise.value2(
                    worldSeed ^ STRATA_WARP_SALT,
                    worldX / strataWarpScale,
                    worldZ / strataWarpScale
            );

            // Initial 0.5.13 testing showed that infinite vertical dike/vein planes read as
            // ruler-straight survey lines across broad massif tops. Keep the volcanic role,
            // but express it as laterally discontinuous horizontal resistant sheets until a
            // future volumetric/erosion pass can give vertical intrusions believable contacts.
            sheetWarp = settings.dikeHalfWidth() * 3.2 * GeologyNoise.value2(
                    worldSeed ^ SHEET_WARP_SALT,
                    worldX / Math.max(1.0, settings.unitHorizontalScale() * 0.72),
                    worldZ / Math.max(1.0, settings.unitHorizontalScale() * 0.72)
            );
            sheetGate = GeologyNoise.value2(
                    worldSeed ^ SHEET_SALT,
                    worldX / Math.max(1.0, settings.unitHorizontalScale()),
                    worldZ / Math.max(1.0, settings.unitHorizontalScale())
            );
            calciteGate = GeologyNoise.value2(
                    worldSeed ^ CALCITE_GATE_SALT,
                    worldX / Math.max(24.0, settings.unitHorizontalScale() * 0.55),
                    worldZ / Math.max(24.0, settings.unitHorizontalScale() * 0.55)
            );
            calciteBandOffset = GeologyNoise.unit(worldSeed, CALCITE_BAND_SALT)
                    * Math.max(1.0, settings.calciteVeinSpacing());
        }

        public Sample sample(double worldY) {
            return sampleGeological(geologicalY(worldY));
        }

        public double geologicalY(double worldY) { return worldY - structuralDisplacement; }

        private Sample sampleGeological(double worldY) {
            double horizontalScale = Math.max(1.0, settings.unitHorizontalScale());
            double verticalScale = Math.max(1.0, settings.unitVerticalScale());
            double intrusionScale = Math.max(1.0, settings.intrusionScale());
            double rareScale = Math.max(1.0, settings.rareBodyScale());
            double strataThickness = Math.max(2.0, settings.strataThickness());
            double warpedY = worldY + strataWarp;

            // Boundary-only detail: these coherent 3D fields perturb material selectors and
            // contact elevation, not individual block choice. The 30–35 block detail and
            // ~10 block micro scales break the giant smooth ovals seen in the first 0.5.13
            // screenshots without reverting to decorative per-block speckle.
            double contactHorizontalScale = Math.max(18.0, horizontalScale * 0.13);
            double contactVerticalScale = Math.max(8.0, verticalScale * 0.38);
            double contactDetail = GeologyNoise.value3(
                    worldSeed ^ CONTACT_DETAIL_SALT,
                    worldX / contactHorizontalScale,
                    warpedY / contactVerticalScale,
                    worldZ / contactHorizontalScale
            );
            double contactMicro = GeologyNoise.value3(
                    worldSeed ^ CONTACT_MICRO_SALT,
                    worldX / Math.max(7.0, horizontalScale * 0.045),
                    warpedY / Math.max(4.0, verticalScale * 0.16),
                    worldZ / Math.max(7.0, horizontalScale * 0.045)
            );
            double contactNoise = contactDetail * 0.30 + contactMicro * 0.11;
            double roughContactY = warpedY + contactDetail * 5.0 + contactMicro * 2.0;
            double variantNoise = GeologyNoise.value3(
                    worldSeed ^ HOST_VARIANT_SALT,
                    worldX / Math.max(32.0, horizontalScale * 1.18),
                    warpedY / Math.max(12.0, verticalScale * 1.70),
                    worldZ / Math.max(32.0, horizontalScale * 1.18)
            );

            double hostNoise = GeologyNoise.value3(
                    worldSeed ^ HOST_SALT,
                    worldX / horizontalScale,
                    warpedY / verticalScale,
                    worldZ / horizontalScale
            );
            long band = (long) Math.floor(warpedY / strataThickness);
            double bandBias = GeologyNoise.signed(
                    worldSeed ^ BAND_SALT,
                    band * 0x9E3779B97F4A7C15L
            );
            double unitSignal = hostNoise * 0.64 + bandBias * 0.30 + contactNoise;

            double rareNoise = GeologyNoise.value3(
                    worldSeed ^ RARE_BODY_SALT,
                    worldX / rareScale,
                    warpedY / (verticalScale * 1.35),
                    worldZ / rareScale
            ) + contactNoise * 0.72;
            boolean limestoneHost = rareNoise <= -settings.limestoneThreshold();

            double sheetDistance = GeologyNoise.foldedDistance(
                    roughContactY + sheetWarp,
                    settings.dikeSpacing()
            );
            double localSheetWidth = Math.max(0.0, settings.dikeHalfWidth())
                    * GeologyNoise.smoothStep(-0.20, 0.55, sheetGate + contactDetail * 0.35);
            if (localSheetWidth > 0.35 && sheetDistance <= localSheetWidth) {
                if (additionalMaterials.enabled()
                        && sheetDistance >= localSheetWidth * 0.58) {
                    return material(Material.SMOOTH_BASALT, false, true, false);
                }
                return material(Material.BASALT, false, true, false);
            }

            if (rareNoise >= settings.blackstoneThreshold()) {
                return material(Material.BLACKSTONE, false, false, false);
            }

            ArrakisTerrainSettings.MaterialPaletteSettings palette = settings.materials();
            double basementWarp = palette.deepslateWarpStrength() * GeologyNoise.value2(
                    worldSeed ^ BASEMENT_WARP_SALT,
                    worldX / Math.max(48.0, horizontalScale * 1.15),
                    worldZ / Math.max(48.0, horizontalScale * 1.15)
            );
            double basementTopY = palette.deepslateTopY()
                    + basementWarp
                    + strataWarp * 0.20;
            if (worldY <= basementTopY) {
                return material(Material.DEEPSLATE, false, false, false);
            }

            double intrusionNoise = GeologyNoise.value3(
                    worldSeed ^ INTRUSION_SALT,
                    worldX / intrusionScale,
                    warpedY / (verticalScale * 1.18),
                    worldZ / intrusionScale
            ) + contactNoise * 0.66;
            if (intrusionNoise >= settings.intrusionThreshold()) {
                double graniteSelector = 0.5 + 0.5 * GeologyNoise.value3(
                        worldSeed ^ GRANITE_SELECTOR_SALT,
                        worldX / Math.max(32.0, intrusionScale * 0.82),
                        warpedY / Math.max(10.0, verticalScale * 1.45),
                        worldZ / Math.max(32.0, intrusionScale * 0.82)
                );
                Material intrusion;
                if (graniteSelector < GeologyNoise.clamp(
                        palette.graniteFraction(),
                        0.0,
                        1.0
                )) {
                    intrusion = Material.GRANITE;
                } else {
                    intrusion = unitSignal >= 0.0
                            ? Material.ANDESITE
                            : Material.DIORITE;
                }
                return material(intrusion, false, false, false);
            }

            Material host;
            if (limestoneHost) {
                host = Material.LIMESTONE;
            } else if (unitSignal >= 0.24) {
                host = Material.TUFF;
            } else if (unitSignal <= -0.46) {
                host = additionalMaterials.enabled() && variantNoise >= 0.25
                        ? Material.RED_SANDSTONE
                        : Material.SANDSTONE;
            } else if (additionalMaterials.enabled()
                    && unitSignal >= -0.18
                    && unitSignal <= 0.18
                    && variantNoise <= -0.42) {
                host = Material.TERRACOTTA;
            } else {
                host = Material.STONE;
            }

            double calciteDistance = GeologyNoise.foldedDistance(
                    roughContactY + calciteBandOffset,
                    settings.calciteVeinSpacing()
            );
            double calciteLens = GeologyNoise.smoothStep(
                    0.12,
                    0.62,
                    calciteGate + contactDetail * 0.32
            );
            if (calciteLens > 0.0
                    && calciteDistance <= settings.calciteVeinHalfWidth() * calciteLens) {
                return new Sample(
                        Material.CALCITE,
                        Material.CALCITE.resistance(),
                        limestoneHost,
                        false,
                        false,
                        true
                );
            }

            return material(host, limestoneHost, false, false);
        }

        private static Sample material(
                Material material,
                boolean limestoneHost,
                boolean basaltStructure,
                boolean calciteVein
        ) {
            return new Sample(
                    material,
                    material.resistance(),
                    limestoneHost,
                    material == Material.ANDESITE
                            || material == Material.DIORITE
                            || material == Material.GRANITE,
                    basaltStructure,
                    calciteVein
            );
        }
    }
}
