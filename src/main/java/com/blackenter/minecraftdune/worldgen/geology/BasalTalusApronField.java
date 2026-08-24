package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Gravity-driven debris aprons beside actual surviving massif and fault-wall contacts.
 *
 * <p>This is deliberately not aeolian deposition. It places a short colluvial wedge after
 * the final pre-talus rock footprint has been resolved. Wind-blown accumulation remains
 * reserved for a later sand-system pass.</p>
 */
public final class BasalTalusApronField {
    public static final int ACTUAL_CONTACT_PROFILE_VERSION = 5149;
    public static final int CONTACT_OWNERSHIP_PROFILE_VERSION = 51410;
    private static final long HEIGHT_SALT = 0x6D53A91C27F84BE2L;
    private static final long MATERIAL_SALT = 0xB8E2417D5A39C60FL;
    private static final int[][] CONTACT_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    private BasalTalusApronField() {
    }

    public static boolean usesActualContact(int profileVersion) {
        return profileVersion >= ACTUAL_CONTACT_PROFILE_VERSION;
    }

    public static boolean usesContactOwnership(int profileVersion) {
        return profileVersion >= CONTACT_OWNERSHIP_PROFILE_VERSION;
    }

    /**
     * Targets colluvium from the final deterministic rock footprint before talus or dunes.
     * The lookup must include both erosion fields and orphan-remnant filtering, but must never
     * include this apron. That strict stage boundary prevents recursion and chunk-order state.
     */
    public static Sample sample(
            long worldSeed,
            int worldX,
            int worldZ,
            MacroGeologyField.Sample geology,
            ArrakisTerrainSettings settings,
            SurvivingRockLookup survivingRock
    ) {
        ArrakisTerrainSettings.TalusSettings talus = settings.lithology().talus();
        if (!talus.basalApronEnabled()
                || (geology.sandCorridorMask() > 0.25
                && geology.faultCarveMask() <= 0.12)) {
            return Sample.NONE;
        }

        boolean faultSearch = geology.faultCarveMask() > 0.12;
        if (!faultSearch) {
            // The structural contact is only a cheap province-level search gate. It never
            // supplies placement distance; the final surviving-rock lookup below does that.
            ScarpMorphologyField.LowSideContact structuralContact =
                    ScarpMorphologyField.nearestMassifLowSideContact(
                            worldSeed,
                            worldX + 0.5,
                            worldZ + 0.5,
                            geology.radiusBlocks(),
                            geology.effectiveRadiusBlocks(),
                            settings.massif()
                    );
            double searchBand = Math.max(1.0, talus.basalApronSpread())
                    + Math.max(0.0, talus.basalApronInset())
                    + 8.0;
            if (!structuralContact.valid()
                    || Math.abs(structuralContact.signedDistance()) > searchBand) {
                return Sample.NONE;
            }
        }

        return sampleFromSurvivingContact(
                worldSeed,
                worldX,
                worldZ,
                geology.faultCarveMask(),
                settings.profileVersion(),
                talus,
                survivingRock
        );
    }

    static Sample sampleFromSurvivingContact(
            long worldSeed,
            int worldX,
            int worldZ,
            double targetFaultCarveMask,
            ArrakisTerrainSettings.TalusSettings talus,
            SurvivingRockLookup survivingRock
    ) {
        return sampleFromSurvivingContact(
                worldSeed,
                worldX,
                worldZ,
                targetFaultCarveMask,
                CONTACT_OWNERSHIP_PROFILE_VERSION,
                talus,
                survivingRock
        );
    }

    static Sample sampleFromSurvivingContact(
            long worldSeed,
            int worldX,
            int worldZ,
            double targetFaultCarveMask,
            int profileVersion,
            ArrakisTerrainSettings.TalusSettings talus,
            SurvivingRockLookup survivingRock
    ) {
        if (!talus.basalApronEnabled()) {
            return Sample.NONE;
        }

        RockColumn target = survivingRock.sample(worldX, worldZ);
        if (target.occupiedAtContact()) {
            // Basal colluvium is deposited beside the final footprint. It never overwrites a
            // surviving foundation-connected rock column merely to imitate an inward inset.
            return Sample.NONE;
        }

        double spread = Math.max(1.0, talus.basalApronSpread());
        Contact contact = nearestContact(
                worldX,
                worldZ,
                targetFaultCarveMask,
                spread,
                usesContactOwnership(profileVersion),
                survivingRock
        );
        if (!contact.valid()) {
            return Sample.NONE;
        }

        double effectiveSpread = spread;
        if (contact.kind() == ContactKind.FAULT) {
            double oppositeDistance = oppositeFaultWallDistance(
                    worldX,
                    worldZ,
                    contact,
                    spread,
                    survivingRock
            );
            if (Double.isFinite(oppositeDistance)) {
                double wallSeparation = contact.distance() + oppositeDistance;
                // Reserve both wall-adjacent floor cells and at least one central cell before
                // dividing the remaining interior between the opposing aprons. This accounts
                // for the contact column's distance-one -> outward-distance-zero mapping.
                double distributableInterior = wallSeparation - 3.0;
                if (distributableInterior <= 0.0) {
                    return Sample.NONE;
                }
                effectiveSpread = Math.min(
                        spread,
                        Math.max(1.0, distributableInterior * 0.43)
                );
            }
        }

        // Distance one is the first low-side column touching rock: its outward distance is
        // zero, so the apron begins with no artificial sand strip.
        double outwardDistance = Math.max(0.0, contact.distance() - 1.0);
        if (outwardDistance >= effectiveSpread) {
            return Sample.NONE;
        }
        double contactFalloff = 1.0 - GeologyNoise.smoothStep(
                0.0,
                effectiveSpread,
                outwardDistance
        );

        int representativeTopY = contact.rock().topY();
        if (contact.kind() == ContactKind.FAULT
                && usesContactOwnership(profileVersion)) {
            representativeTopY = representativeFaultWallTopY(
                    worldX,
                    worldZ,
                    contact,
                    spread,
                    survivingRock
            );
        }
        double relief = Math.max(
                0.0,
                representativeTopY - MacroGeologyField.BASE_SURFACE_Y
        );
        double reliefGate = GeologyNoise.smoothStep(12.0, 42.0, relief);
        if (reliefGate <= 0.0) {
            return Sample.NONE;
        }

        double sampleX = worldX + 0.5;
        double sampleZ = worldZ + 0.5;
        double patch = 0.82 + 0.18 * (
                0.5 + 0.5 * GeologyNoise.value2(
                        worldSeed ^ HEIGHT_SALT,
                        sampleX / 74.0,
                        sampleZ / 74.0
                )
        );
        int maxHeight = Math.max(0, Math.min(12, talus.basalApronMaxHeight()));
        int height = heightFromFactors(maxHeight, contactFalloff, reliefGate, patch);
        if (height <= 0) {
            return Sample.NONE;
        }

        return new Sample(
                true,
                height,
                outwardDistance,
                effectiveSpread,
                GeologyNoise.clamp(talus.basalApronSandStart(), 0.0, 1.0),
                worldSeed,
                sampleX,
                sampleZ,
                contact.kind(),
                contact.distance(),
                contact.offsetX(),
                contact.offsetZ()
        );
    }

    /** Retains the pushed 0.5.14.8 nominal-scarp behavior for serialized older profiles. */
    public static Sample sampleLegacy(
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
                worldZ,
                ContactKind.LEGACY_MASSIF,
                Math.abs(signedDistance),
                contact.inwardX(),
                contact.inwardZ()
        );
    }

    private static Contact nearestContact(
            int worldX,
            int worldZ,
            double targetFaultCarveMask,
            double spread,
            boolean acceptAnyMassifContact,
            SurvivingRockLookup survivingRock
    ) {
        Contact best = Contact.NONE;
        int maximumStep = Math.max(1, (int) Math.ceil(spread + 1.0));
        boolean faultFloor = targetFaultCarveMask > 0.12;

        for (int[] direction : CONTACT_DIRECTIONS) {
            double length = Math.hypot(direction[0], direction[1]);
            double unitX = direction[0] / length;
            double unitZ = direction[1] / length;
            int previousX = Integer.MIN_VALUE;
            int previousZ = Integer.MIN_VALUE;
            for (int step = 1; step <= maximumStep; step++) {
                int offsetX = (int) Math.round(unitX * step);
                int offsetZ = (int) Math.round(unitZ * step);
                if (offsetX == previousX && offsetZ == previousZ) {
                    continue;
                }
                previousX = offsetX;
                previousZ = offsetZ;
                double distance = Math.hypot(offsetX, offsetZ);
                if (distance > spread + 1.0 || distance >= best.distance()) {
                    continue;
                }

                RockColumn rock = survivingRock.sample(
                        worldX + offsetX,
                        worldZ + offsetZ
                );
                if (!rock.occupiedAtContact()) {
                    continue;
                }

                ContactKind kind = ContactKind.NONE;
                if (faultFloor) {
                    // Near the absolute floor, adjacent wall and floor carve masks may differ
                    // by only a few thousandths. The empty target's fault context identifies
                    // the canyon; actual occupancy identifies the surviving wall contact.
                    kind = ContactKind.FAULT;
                } else if (acceptAnyMassifContact || rock.massifSource()) {
                    // Profile 51410 treats actual surviving Y=65 contact as authoritative
                    // inside the already-bounded physical Shield-Wall search band. This lets
                    // foreland/broken-rock-owned basal overlap connect to the massif apron.
                    kind = ContactKind.MASSIF;
                }
                if (kind != ContactKind.NONE) {
                    best = new Contact(
                            true,
                            kind,
                            distance,
                            offsetX,
                            offsetZ,
                            rock
                    );
                    if (distance <= 1.0 + 1.0e-9) {
                        return best;
                    }
                }
            }
        }
        return best;
    }

    static int representativeFaultWallTopY(
            int worldX,
            int worldZ,
            Contact contact,
            double spread,
            SurvivingRockLookup survivingRock
    ) {
        int highest = contact.rock().topY();
        double contactDistance = Math.max(1.0, contact.distance());
        double unitX = contact.offsetX() / contactDistance;
        double unitZ = contact.offsetZ() / contactDistance;
        int probeLength = (int) Math.round(
                Math.max(16.0, Math.min(24.0, spread + 8.0))
        );

        int previousX = Integer.MIN_VALUE;
        int previousZ = Integer.MIN_VALUE;
        for (int step = 0; step <= probeLength; step++) {
            int offsetX = (int) Math.round(contact.offsetX() + unitX * step);
            int offsetZ = (int) Math.round(contact.offsetZ() + unitZ * step);
            if (offsetX == previousX && offsetZ == previousZ) {
                continue;
            }
            previousX = offsetX;
            previousZ = offsetZ;

            RockColumn rock = survivingRock.sample(
                    worldX + offsetX,
                    worldZ + offsetZ
            );
            if (rock.occupiedAtContact()) {
                highest = Math.max(highest, rock.topY());
            }
        }
        return highest;
    }

    private static double oppositeFaultWallDistance(
            int worldX,
            int worldZ,
            Contact contact,
            double spread,
            SurvivingRockLookup survivingRock
    ) {
        double unitX = contact.offsetX() / contact.distance();
        double unitZ = contact.offsetZ() / contact.distance();
        int maximumStep = Math.max(2, (int) Math.ceil(spread * 2.0 + 2.0));
        int previousX = Integer.MIN_VALUE;
        int previousZ = Integer.MIN_VALUE;
        for (int step = 1; step <= maximumStep; step++) {
            int offsetX = (int) Math.round(-unitX * step);
            int offsetZ = (int) Math.round(-unitZ * step);
            if (offsetX == previousX && offsetZ == previousZ) {
                continue;
            }
            previousX = offsetX;
            previousZ = offsetZ;
            RockColumn opposite = survivingRock.sample(
                    worldX + offsetX,
                    worldZ + offsetZ
            );
            if (opposite.occupiedAtContact()) {
                return Math.hypot(offsetX, offsetZ);
            }
        }
        return Double.POSITIVE_INFINITY;
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
            double worldZ,
            ContactKind contactKind,
            double contactDistance,
            double contactOffsetX,
            double contactOffsetZ
    ) {
        public static final Sample NONE = new Sample(
                false,
                0,
                Double.POSITIVE_INFINITY,
                1.0,
                1.0,
                0L,
                0.0,
                0.0,
                ContactKind.NONE,
                Double.POSITIVE_INFINITY,
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

    public record RockColumn(
            boolean occupiedAtContact,
            int topY,
            boolean massifSource,
            double faultCarveMask
    ) {
        public static final RockColumn EMPTY = new RockColumn(
                false,
                MacroGeologyField.BASE_SURFACE_Y,
                false,
                0.0
        );
    }

    private record Contact(
            boolean valid,
            ContactKind kind,
            double distance,
            int offsetX,
            int offsetZ,
            RockColumn rock
    ) {
        private static final Contact NONE = new Contact(
                false,
                ContactKind.NONE,
                Double.POSITIVE_INFINITY,
                0,
                0,
                RockColumn.EMPTY
        );
    }

    @FunctionalInterface
    public interface SurvivingRockLookup {
        RockColumn sample(int worldX, int worldZ);
    }

    public enum ContactKind {
        NONE,
        MASSIF,
        FAULT,
        LEGACY_MASSIF
    }

    public enum Material {
        NONE,
        GRAVEL,
        SAND
    }
}
