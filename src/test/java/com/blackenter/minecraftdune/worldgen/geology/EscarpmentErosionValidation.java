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
        require(settings.profileVersion() == 514, "active profile_version must be 514");
        require(settings.erosion().enabled(), "active preset erosion must be enabled");

        JsonObject oldProfile = profile.json().deepCopy();
        oldProfile.addProperty("profile_version", 513);
        oldProfile.remove("erosion");
        ArrakisTerrainSettings backward = ArrakisTerrainSettings.CODEC
                .parse(JsonOps.INSTANCE, oldProfile)
                .getOrThrow();
        require(!backward.erosion().enabled(), "missing erosion group must decode disabled");

        validateResistanceOrder(settings);
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
                require(!evaluate(SEEDS[0], x, z, settings).erosion().candidate(),
                        "erosion attempted to bridge a full fault core");
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
        EscarpmentErosionField.Column erosion = EscarpmentErosionField.sample(
                seed,
                x,
                z,
                originalTopY,
                fissureTopY,
                geology,
                lithology,
                fracture,
                settings
        );
        return new Evaluation(
                geology,
                dune,
                lithology,
                fracture,
                erosion,
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
        int highest = erosion.highestRockY(
                evaluation.lithology(),
                evaluation.fracture()
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
            EscarpmentErosionField.Column erosion,
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
