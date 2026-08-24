package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Gravity-driven debris apron at the low-side contact of the inner and outer Shield Wall.
 *
 * <p>This is deliberately not aeolian deposition. It only masks the immediate rock/sand
 * contact with a short colluvial wedge after the structural rock height has been corrected.
 * Wind-blown accumulation remains reserved for a later sand-system pass.</p>
 */
public final class BasalTalusApronField {
    private static final long HEIGHT_SALT = 0x6D53A91C27F84BE2L;
    private static final long MATERIAL_SALT = 0xB8E2417D5A39C60FL;

    private BasalTalusApronField() {
    }

    public static Sample sample(
            long worldSeed,
            double worldX,
            double worldZ,
            MacroGeologyField.Sample geology,
            ArrakisTerrainSettings settings
    ) {
        ArrakisTerrainSettings.TalusSettings talus = settings.lithology().talus();
        if (!talus.basalApronEnabled()
                || geology.sandCorridorMask() > 0.25
                || geology.faultCarveMask() > 0.85) {
            return Sample.NONE;
        }

        double spread = Math.max(1.0, talus.basalApronSpread());
        double inset = Math.max(0.0, talus.basalApronInset());
        ScarpMorphologyField.LowSideContact contact =
                ScarpMorphologyField.nearestMassifLowSideContact(
                        worldSeed,
                        worldX,
                        worldZ,
                        geology.radiusBlocks(),
                        geology.effectiveRadiusBlocks(),
                        settings.massif()
                );
        if (!contact.valid()) {
            return Sample.NONE;
        }

        double signedDistance = contact.signedDistance();
        if (signedDistance < -spread || signedDistance > inset) {
            return Sample.NONE;
        }

        double outwardDistance = Math.max(0.0, -signedDistance);
        double outwardFalloff = 1.0 - GeologyNoise.smoothStep(
                0.0,
                spread,
                outwardDistance
        );
        double inwardFalloff = 1.0 - GeologyNoise.smoothStep(
                0.0,
                Math.max(1.0, inset),
                Math.max(0.0, signedDistance)
        );
        double contactFalloff = signedDistance >= 0.0
                ? Math.max(0.45, inwardFalloff)
                : outwardFalloff;

        double probe = Math.max(
                8.0,
                Math.min(28.0, contact.scarpWidth() * 0.55)
        );
        double probeX = worldX + contact.inwardX() * probe;
        double probeZ = worldZ + contact.inwardZ() * probe;
        double high = MacroGeologyField.sample(
                worldSeed,
                probeX,
                probeZ,
                settings
        ).baseElevation();
        double relief = Math.max(
                0.0,
                high - MacroGeologyField.BASE_SURFACE_Y
        );
        double reliefGate = GeologyNoise.smoothStep(12.0, 42.0, relief);
        if (reliefGate <= 0.0) {
            return Sample.NONE;
        }

        double patch = 0.82 + 0.18 * (
                0.5 + 0.5 * GeologyNoise.value2(
                        worldSeed ^ HEIGHT_SALT,
                        worldX / 74.0,
                        worldZ / 74.0
                )
        );
        int maxHeight = Math.max(0, Math.min(12, talus.basalApronMaxHeight()));
        int height = heightFromFactors(
                maxHeight,
                contactFalloff,
                reliefGate,
                patch
        );
        if (height <= 0) {
            return Sample.NONE;
        }

        return new Sample(
                true,
                height,
                outwardDistance,
                spread,
                GeologyNoise.clamp(talus.basalApronSandStart(), 0.0, 1.0),
                worldSeed,
                worldX,
                worldZ
        );
    }

    static int heightFromFactors(
            int maximumHeight,
            double contactFalloff,
            double reliefGate,
            double patch
    ) {
        if (maximumHeight <= 0) {
            return 0;
        }
        double strength = GeologyNoise.clamp(
                contactFalloff * reliefGate * patch,
                0.0,
                1.0
        );
        return strength <= 0.03
                ? 0
                : Math.max(
                        1,
                        (int) Math.ceil(
                                maximumHeight * Math.pow(strength, 1.12)
                        )
                );
    }

    public record Sample(
            boolean active,
            int height,
            double outwardDistance,
            double spread,
            double sandStart,
            long worldSeed,
            double worldX,
            double worldZ
    ) {
        public static final Sample NONE = new Sample(
                false,
                0,
                Double.POSITIVE_INFINITY,
                1.0,
                1.0,
                0L,
                0.0,
                0.0
        );

        public boolean occupiesY(int worldY) {
            return active
                    && worldY >= MacroGeologyField.BASE_SURFACE_Y + 1
                    && worldY <= topY();
        }

        public int topY() {
            return active
                    ? MacroGeologyField.BASE_SURFACE_Y + height
                    : MacroGeologyField.BASE_SURFACE_Y;
        }

        public Material materialAt(int worldY) {
            if (!occupiesY(worldY)) {
                return Material.NONE;
            }

            double distanceFraction = GeologyNoise.clamp(
                    outwardDistance / Math.max(1.0, spread),
                    0.0,
                    1.0
            );
            double verticalFraction = GeologyNoise.clamp(
                    (worldY - MacroGeologyField.BASE_SURFACE_Y - 1.0)
                            / Math.max(1.0, height - 1.0),
                    0.0,
                    1.0
            );
            double materialNoise = GeologyNoise.value3(
                    worldSeed ^ MATERIAL_SALT,
                    worldX / 11.0,
                    worldY / 5.0,
                    worldZ / 11.0
            );

            double sandBias = Math.max(
                    distanceFraction,
                    1.0 - verticalFraction
            );
            if (sandBias >= sandStart
                    || (sandBias >= sandStart - 0.12 && materialNoise > 0.42)) {
                return Material.SAND;
            }
            return Material.GRAVEL;
        }
    }

    public enum Material {
        NONE,
        GRAVEL,
        SAND
    }
}
