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
    private static final long RARE_BODY_SALT = 0x75BD094FC30A16E8L;
    private static final long DIKE_ANGLE_SALT = 0x19A6E42D73BF580CL;
    private static final long DIKE_WARP_SALT = 0x54C0B78E129DA36FL;
    private static final long SHEET_SALT = 0x61D38FA2940CE75BL;
    private static final long CALCITE_ANGLE_SALT = 0x36F2C1598AB74D0EL;
    private static final double TWO_PI = Math.PI * 2.0;

    private LithologyField() {
    }

    public static Column column(
            long worldSeed,
            double worldX,
            double worldZ,
            ArrakisTerrainSettings.LithologySettings settings
    ) {
        return new Column(worldSeed, worldX, worldZ, settings);
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
        CALCITE("calcite", ResistanceClass.MEDIUM, "mineralized vein/fill"),
        ANDESITE("andesite", ResistanceClass.HARD, "hard intrusive body"),
        DIORITE("diorite", ResistanceClass.HARD, "hard intrusive body"),
        BASALT("basalt", ResistanceClass.VERY_HARD, "very-hard dike/sheet"),
        BLACKSTONE("blackstone", ResistanceClass.VERY_HARD, "rare ancient resistant body"),
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
        private final double strataWarp;
        private final double dikeDistance;
        private final double sheetGate;
        private final double calciteCoordinate;

        private Column(
                long worldSeed,
                double worldX,
                double worldZ,
                ArrakisTerrainSettings.LithologySettings settings
        ) {
            this.worldSeed = worldSeed;
            this.worldX = worldX;
            this.worldZ = worldZ;
            this.settings = settings;

            double strataWarpScale = Math.max(1.0, settings.strataWarpScale());
            strataWarp = settings.strataWarpStrength() * GeologyNoise.value2(
                    worldSeed ^ STRATA_WARP_SALT,
                    worldX / strataWarpScale,
                    worldZ / strataWarpScale
            );

            double dikeAngle = GeologyNoise.unit(worldSeed, DIKE_ANGLE_SALT) * TWO_PI;
            double dikeProjection = worldX * Math.cos(dikeAngle)
                    + worldZ * Math.sin(dikeAngle);
            double dikeWarp = settings.dikeSpacing() * 0.22 * GeologyNoise.value2(
                    worldSeed ^ DIKE_WARP_SALT,
                    worldX / Math.max(1.0, settings.dikeSpacing() * 2.4),
                    worldZ / Math.max(1.0, settings.dikeSpacing() * 2.4)
            );
            dikeDistance = GeologyNoise.foldedDistance(
                    dikeProjection + dikeWarp,
                    settings.dikeSpacing()
            );
            sheetGate = GeologyNoise.value2(
                    worldSeed ^ SHEET_SALT,
                    worldX / Math.max(1.0, settings.unitHorizontalScale()),
                    worldZ / Math.max(1.0, settings.unitHorizontalScale())
            );

            double calciteAngle = GeologyNoise.unit(worldSeed, CALCITE_ANGLE_SALT) * TWO_PI;
            calciteCoordinate = worldX * Math.cos(calciteAngle)
                    + worldZ * Math.sin(calciteAngle);
        }

        public Sample sample(double worldY) {
            double horizontalScale = Math.max(1.0, settings.unitHorizontalScale());
            double verticalScale = Math.max(1.0, settings.unitVerticalScale());
            double intrusionScale = Math.max(1.0, settings.intrusionScale());
            double rareScale = Math.max(1.0, settings.rareBodyScale());
            double strataThickness = Math.max(2.0, settings.strataThickness());
            double warpedY = worldY + strataWarp;

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
            double unitSignal = hostNoise * 0.68 + bandBias * 0.32;

            double rareNoise = GeologyNoise.value3(
                    worldSeed ^ RARE_BODY_SALT,
                    worldX / rareScale,
                    warpedY / (verticalScale * 1.35),
                    worldZ / rareScale
            );
            boolean limestoneHost = rareNoise <= -settings.limestoneThreshold();

            double dikeWidth = Math.max(0.0, settings.dikeHalfWidth())
                    * (0.82 + 0.18 * Math.sin(worldY * 0.11 + strataWarp * 0.07));
            if (dikeDistance <= dikeWidth) {
                return material(Material.BASALT, false, true, false);
            }

            double sheetDistance = GeologyNoise.foldedDistance(warpedY, strataThickness);
            double sheetBand = GeologyNoise.signed(
                    worldSeed ^ SHEET_SALT,
                    band * 0xD1B54A32D192ED03L
            );
            if (sheetBand > 0.72
                    && sheetGate > 0.05
                    && sheetDistance <= settings.dikeHalfWidth() * 0.70) {
                return material(Material.BASALT, false, true, false);
            }

            if (rareNoise >= settings.blackstoneThreshold()) {
                return material(Material.BLACKSTONE, false, false, false);
            }

            double intrusionNoise = GeologyNoise.value3(
                    worldSeed ^ INTRUSION_SALT,
                    worldX / intrusionScale,
                    warpedY / (verticalScale * 1.18),
                    worldZ / intrusionScale
            );
            if (intrusionNoise >= settings.intrusionThreshold()) {
                Material intrusion = unitSignal >= 0.0
                        ? Material.ANDESITE
                        : Material.DIORITE;
                return material(intrusion, false, false, false);
            }

            Material host;
            if (limestoneHost) {
                host = Material.LIMESTONE;
            } else if (unitSignal >= 0.24) {
                host = Material.TUFF;
            } else if (unitSignal <= -0.46) {
                host = Material.SANDSTONE;
            } else {
                host = Material.STONE;
            }

            double calciteDistance = GeologyNoise.foldedDistance(
                    calciteCoordinate + warpedY * 0.31,
                    settings.calciteVeinSpacing()
            );
            if (calciteDistance <= settings.calciteVeinHalfWidth()) {
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
                    material == Material.ANDESITE || material == Material.DIORITE,
                    basaltStructure,
                    calciteVein
            );
        }
    }
}
