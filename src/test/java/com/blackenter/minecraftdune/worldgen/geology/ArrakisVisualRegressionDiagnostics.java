package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import static com.blackenter.minecraftdune.worldgen.geology.EscarpmentErosionValidation.*;

/** Exploratory location/population searches. Not a dependency of check/build. */
public final class ArrakisVisualRegressionDiagnostics {
    private ArrakisVisualRegressionDiagnostics() {}
    public static void main(String[] args) throws Exception {
        ArrakisTerrainSettings settings = ArrakisProfileValidation.loadProfile().settings();
        diagnoseSurfaceSites(settings);
        diagnoseRemnantRegion(settings);
        ValidationCounts counts = validateEscarpments(settings);
        for (int index = 0; index < counts.representatives().length; index++) {
            CandidateCoordinate candidate = counts.representatives()[index];
            if (candidate == null) continue;
            boolean alreadyReported = false;
            for (int previous = 0; previous < index; previous++) {
                CandidateCoordinate earlier = counts.representatives()[previous];
                if (earlier != null && candidate.seed() == earlier.seed()
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

    private static void diagnoseSurfaceSites(ArrakisTerrainSettings settings) {
        LithologyField.Sample soft = new LithologyField.Sample(
                LithologyField.Material.SANDSTONE, LithologyField.ResistanceClass.SOFT,
                false, false, false, false);
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
        if (wallRepresentative == null || fissureRepresentative == null) {
            System.out.println("No representative surface wall/fissure found in this diagnostic sweep.");
            return;
        }
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

    private static void diagnoseRemnantRegion(ArrakisTerrainSettings settings) {
        long seed = -5_640_511_200_611_798_902L;
        Map<Long, Evaluation> cache = new HashMap<>();
        int surfaceOnlyColumns = 0;
        int removedSurfaceOnlyBlocks = 0;
        for (int blockZ = 48; blockZ <= 160; blockZ += 2) {
            for (int blockX = 3_032; blockX <= 3_080; blockX++) {
                Evaluation column = evaluationAt(seed, blockX, blockZ, settings, cache);
                if (column.erosion().candidate() || !column.surfaceErosion().active()) {
                    continue;
                }
                boolean removedInColumn = false;
                int lastY = Math.min(160, column.fissureTopY());
                for (int y = MacroGeologyField.BASE_SURFACE_Y + 6; y <= lastY; y++) {
                    if (!rawRockOccupies(column, y)) {
                        continue;
                    }
                    boolean kept = OrphanRemnantFilter.keeps(
                            blockX,
                            y,
                            blockZ,
                            column.erosion(),
                            column.surfaceErosion(),
                            settings.erosion().orphanRemnants(),
                            (supportX, supportY, supportZ) -> rawRockOccupies(
                                    evaluationAt(
                                            seed,
                                            supportX,
                                            supportZ,
                                            settings,
                                            cache
                                    ),
                                    supportY
                            )
                    );
                    if (!kept) {
                        removedSurfaceOnlyBlocks++;
                        removedInColumn = true;
                    }
                }
                if (removedInColumn) {
                    surfaceOnlyColumns++;
                }
            }
        }
        System.out.printf(
                Locale.ROOT,
                "Reported remnant regression: %d surface-only columns, %d orphan blocks removed.%n",
                surfaceOnlyColumns,
                removedSurfaceOnlyBlocks
        );
    }
}
