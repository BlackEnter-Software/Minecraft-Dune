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
        return shape(worldSeed, worldX, worldZ, settings, signedDistance, high);
    }

    private static Sample shape(long worldSeed, double worldX, double worldZ,
            ArrakisTerrainSettings settings, double signedDistance, double high) {
        if (settings.lithology().talus().organicApronEnabled()) {
            return organicShape(worldSeed, worldX, worldZ, settings, signedDistance, high);
        }
        var talus = settings.lithology().talus();
        double spread = Math.max(1.0, talus.basalApronSpread());
        double inset = Math.max(0.0, talus.basalApronInset());
        if (signedDistance < -spread || signedDistance > inset) return Sample.NONE;
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

    static Sample organicShape(long seed, double x, double z, ArrakisTerrainSettings settings,
            double signed, double high) {
        var talus = settings.lithology().talus();
        var variation = TalusShapeVariation.sample(seed, x, z);
        double spread = Math.min(CONTACT_SEARCH_LIMIT, Math.max(1, talus.basalApronSpread() * variation.spreadScale()));
        double inset = Math.max(0, talus.basalApronInset());
        if (!Double.isFinite(signed) || signed <= -spread || signed > inset) return Sample.NONE;
        double outward = Math.max(0, -signed);
        // Steeper against the source wall, progressively gentler towards the desert.
        // No independent per-block randomness: neighboring columns share smooth patches.
        double curve = Math.pow(1 - outward / spread, 1.8);
        if (signed > 0) curve *= Math.max(0.45, 1 - GeologyNoise.smoothStep(0, Math.max(1, inset), signed));
        double relief = GeologyNoise.smoothStep(12, 42, high - MacroGeologyField.BASE_SURFACE_Y);
        double height = Math.max(0, Math.min(12, talus.basalApronMaxHeight())) * variation.heightScale() * curve * relief;
        // Let the very shallow distal tail disappear instead of rounding every trace up
        // into a long, uniform one-block sand rail. Preserve a qualified near-wall deposit.
        if (height <= 0 || height < 0.45 && outward > 1) return Sample.NONE;
        return new Sample(true, Math.max(1, (int) Math.floor(height + 0.5)), outward, spread,
                GeologyNoise.clamp(talus.basalApronSandStart() + variation.sandStartOffset(), 0, 1), seed, x, z);
    }

    // Search is local to the candidate deposit cell, not the obsolete structural foot.
    public static final int CONTACT_SEARCH_LIMIT = 32;
    public static final int WALL_PROBE_LIMIT = 24;

    public static Evaluation evaluate(long seed, int x, int z, MacroGeologyField.Sample geology,
            ArrakisTerrainSettings settings, RockLookup rock) {
        Evaluation massif = evaluateMassif(seed, x, z, geology, settings, rock);
        var talus = settings.lithology().talus();
        // Preserve every qualified existing contact. Only fault shoulders which lack one
        // may use the local wall fallback; desert, full cores and passes stay excluded.
        if (!talus.actualContactEnabled() || !talus.ravineContactEnabled() || !talus.basalApronEnabled()
                || massif.actual().reason().equals("found")
                || geology.faultCarveMask() <= 0 || geology.faultCarveMask() > 0.85
                || geology.sandCorridorMask() > 0.25 || geology.physicalMassifWeight() <= 0.18
                || !rock.allowed(x, z)) return massif;
        ActualContact actual = findRavineContact(x, z, talus.basalApronInset(), rock);
        return new Evaluation(actual.found()
                ? shape(seed, x + 0.5, z + 0.5, settings, actual.signedDistance(), actual.wallTopY())
                : Sample.NONE, massif.structural(), actual, "ravine");
    }

    private static Evaluation evaluateMassif(long seed, int x, int z, MacroGeologyField.Sample geology,
            ArrakisTerrainSettings settings, RockLookup rock) {
        var talus = settings.lithology().talus();
        var structural = ScarpMorphologyField.nearestMassifLowSideContact(seed, x + 0.5, z + 0.5,
                geology.radiusBlocks(), geology.effectiveRadiusBlocks(), settings.massif());
        if (!talus.actualContactEnabled()) {
            return new Evaluation(sample(seed, x + 0.5, z + 0.5, geology, settings),
                    structural, ActualContact.none(false, "legacy-structural"));
        }
        String exclusion = !talus.basalApronEnabled() ? "apron-disabled"
                : geology.sandCorridorMask() > 0.25 ? "sand-corridor"
                : geology.faultCarveMask() > 0.85 ? "fault-core"
                : !structural.valid() ? "no-structural-side"
                // The candidate halo is wider than the canonical source band. Otherwise
                // a distal cell can find a wall whose adjacent cell is outside the gate.
                : Math.abs(structural.signedDistance()) > structural.scarpWidth() + 3 * CONTACT_SEARCH_LIMIT
                        ? "outside-scarp-search-band" : null;
        if (exclusion != null) {
            return new Evaluation(Sample.NONE, structural, ActualContact.none(true, exclusion));
        }
        ActualContact actual = findContact(x, z, structural, talus.basalApronInset(), rock);
        boolean sourceAllowed = actual.found() && rock.sourceAllowed(actual.x(), actual.z());
        if (actual.found() && !sourceAllowed) actual = actual.withReason("outside-source-scarp-band");
        Sample apron = sourceAllowed && actual.wallRelief() > 12
                ? shape(seed, x + 0.5, z + 0.5, settings, actual.signedDistance(), actual.wallTopY())
                : Sample.NONE;
        return new Evaluation(apron, structural, actual);
    }

    /** A cardinal raster ray guarantees that the zero-distance exterior cell is face-adjacent. */
    static ActualContact findContact(int x, int z, ScarpMorphologyField.LowSideContact structural,
            double inset, RockLookup rock) {
        int dx = 0, dz = 0;
        if (Math.abs(structural.inwardX()) >= Math.abs(structural.inwardZ())) {
            dx = structural.inwardX() >= 0 ? 1 : -1;
        } else {
            dz = structural.inwardZ() >= 0 ? 1 : -1;
        }
        return findContact(x, z, dx, dz, inset, rock);
    }

    /** Four bounded raster rays, never a search across the protected fault channel. */
    static ActualContact findRavineContact(int x, int z, double inset, RockLookup rock) {
        if (!rock.allowed(x, z)) return ActualContact.missing(0, "suppressed-path");
        ActualContact best = null;
        int searched = 0;
        for (int direction = 0; direction < 4; direction++) {
            int dx = direction == 0 ? 1 : direction == 1 ? -1 : 0;
            int dz = direction == 2 ? 1 : direction == 3 ? -1 : 0;
            ActualContact candidate = findContact(x, z, dx, dz, inset, rock);
            searched += candidate.searchedBlocks();
            if (!candidate.found() || !candidate.reason().equals("found")
                    || !rock.ravineSourceAllowed(candidate.x(), candidate.z())) continue;
            // Nearest actual foot wins; world-coordinate tie break is stable across caches,
            // ray enumeration and chunk boundaries. Relief is never pooled across walls.
            if (best == null || Math.abs(candidate.signedDistance()) < Math.abs(best.signedDistance())
                    || Math.abs(candidate.signedDistance()) == Math.abs(best.signedDistance())
                        && (candidate.x() < best.x() || candidate.x() == best.x() && candidate.z() < best.z())) {
                best = candidate;
            }
        }
        return best == null ? ActualContact.missing(searched, "no-qualified-ravine-foot") : best;
    }

    private static ActualContact findContact(int x, int z, int dx, int dz,
            double inset, RockLookup rock) {
        boolean inside = rock.footPresent(x, z);
        int direction = inside ? -1 : 1;
        int limit = inside ? Math.min(CONTACT_SEARCH_LIMIT, (int) Math.ceil(inset) + 1)
                : CONTACT_SEARCH_LIMIT;
        for (int step = 1; step <= limit; step++) {
            int px = x + dx * step * direction, pz = z + dz * step * direction;
            if (!rock.allowed(px, pz)) {
                return ActualContact.missing(step, "suppressed-path");
            }
            if (rock.footPresent(px, pz) == inside) continue;
            int cx = inside ? px + dx : px, cz = inside ? pz + dz : pz;
            int contactTop = rock.topY(cx, cz);
            int wallTop = contactTop, probed = 0;
            // Do not inherit relief across an air gap, a sand corridor or an opposing fault wall.
            for (int probe = 1; probe <= WALL_PROBE_LIMIT; probe++) {
                int rx = cx + dx * probe, rz = cz + dz * probe;
                probed++;
                if (!rock.allowed(rx, rz) || !rock.footPresent(rx, rz)) break;
                wallTop = Math.max(wallTop, rock.topY(rx, rz));
            }
            return new ActualContact(true, step, true, inside ? step : 1.0 - step,
                    cx, cz, contactTop, wallTop, probed,
                    wallTop > MacroGeologyField.BASE_SURFACE_Y + 12 ? "found" : "insufficient-connected-relief");
        }
        return ActualContact.missing(limit, inside ? "inside-rock-beyond-inset" : "no-foot-within-bound");
    }

    public interface RockLookup {
        boolean footPresent(int x, int z);
        int topY(int x, int z);
        boolean allowed(int x, int z);
        default boolean sourceAllowed(int x, int z) { return true; }
        default boolean ravineSourceAllowed(int x, int z) { return false; }
    }

    public record Evaluation(Sample apron, ScarpMorphologyField.LowSideContact structural,
            ActualContact actual, String source) {
        public Evaluation(Sample apron, ScarpMorphologyField.LowSideContact structural, ActualContact actual) {
            this(apron, structural, actual, actual.enabled() ? "massif" : "legacy-structural");
        }
    }

    public record ActualContact(boolean enabled, int searchedBlocks, boolean found,
            double signedDistance, int x, int z, int rockTopY, int wallTopY,
            int wallProbeBlocks, String reason) {
        static ActualContact none(boolean enabled, String reason) {
            return new ActualContact(enabled, 0, false, Double.POSITIVE_INFINITY,
                    0, 0, MacroGeologyField.BASE_SURFACE_Y, MacroGeologyField.BASE_SURFACE_Y, 0, reason);
        }
        static ActualContact missing(int searched, String reason) {
            return new ActualContact(true, searched, false, Double.POSITIVE_INFINITY,
                    0, 0, MacroGeologyField.BASE_SURFACE_Y, MacroGeologyField.BASE_SURFACE_Y, 0, reason);
        }
        public double outwardDistance() { return found ? Math.max(0, -signedDistance) : Double.POSITIVE_INFINITY; }
        public int wallRelief() { return wallTopY - MacroGeologyField.BASE_SURFACE_Y; }
        ActualContact withReason(String reason) {
            return new ActualContact(enabled, searchedBlocks, found, signedDistance,
                    x, z, rockTopY, wallTopY, wallProbeBlocks, reason);
        }
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
