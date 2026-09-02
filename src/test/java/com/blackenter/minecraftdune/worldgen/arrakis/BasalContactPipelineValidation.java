package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.BasalTalusApronField;
import com.mojang.serialization.JsonOps;

/** Fixed photographic-area regressions and acyclic, cache-independent deposit evaluation. */
public final class BasalContactPipelineValidation {
    public static void validate(ArrakisTerrainSettings settings) {
        fixture(settings, 0, 3001, 464, 3002, 464);
        fixture(settings, 0, 3071, -52, 3072, -52);
        fixture(settings, -5640511200611798902L, 3065, 173, 3066, 173);
        fixture(settings, 0, 3123, 224, 3124, 224);
        fixture(settings, 0, 4086, 0, 4085, 0);

        var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
        var oldFoot = evaluator.column(3028, -52);
        require(BasalTalusApronField.sample(0, 3028.5, -51.5, oldFoot.geology(), settings).active(),
                "fixed historical misplaced apron fixture moved");
        require(!oldFoot.basalTalusApron().active(), "old structural foot still gets detached apron");

        // The new contact calculation must not alter raw rock, dune inputs, or final rock.
        var json = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        json.getAsJsonObject("lithology").getAsJsonObject("talus").addProperty("actual_contact_enabled", false);
        var structuralSettings = ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        var structural = new ArrakisTerrainEvaluator(0, structuralSettings, 64);
        for (int x = 2970; x <= 3010; x++) {
            var a = evaluator.preTalusColumn(x, 464);
            var b = structural.preTalusColumn(x, 464);
            // Lithology columns retain their full settings record, including the unused
            // talus flag. Compare generated values, not settings-container identity.
            require(a.rockTopY() == b.rockTopY() && a.originalRockTopY() == b.originalRockTopY()
                    && a.fissureRockTopY() == b.fissureRockTopY() && a.duneSurfaceUnits() == b.duneSurfaceUnits()
                    && a.geology().equals(b.geology()) && a.fracture().equals(b.fracture()) && a.face().equals(b.face()),
                    "actual contact changed pre-talus inputs");
            for (int y = 65; y <= Math.max(a.rockTopY(), b.rockTopY()); y++) {
                require(a.materialSampleAt(y).equals(b.materialSampleAt(y))
                        && evaluator.rawRockOccupies(a, y) == structural.rawRockOccupies(b, y),
                        "contact changed raw lithology/erosion");
                require(evaluator.rockOccupies(x, y, 464) == structural.rockOccupies(x, y, 464),
                        "contact altered rock occupancy");
            }
            var old = structural.column(x, 464);
            require(old.basalTalusApron().equals(BasalTalusApronField.sample(0, x + 0.5, 464.5,
                    old.geology(), structuralSettings)), "false flag changed historical apron");
        }
        // Fixed full core and a short neighboring strip: deposits may not fill/bridge it.
        for (int z = 196; z <= 204; z++) {
            var c = evaluator.column(3042, z);
            if (c.geology().faultCarveMask() > 0.999) {
                require(c.originalRockTopY() == 64 && evaluator.highestFilteredRockY(3042, z) == 64,
                        "fault core left sand datum");
            }
            if (c.geology().faultCarveMask() > 0.85 || c.geology().sandCorridorMask() > 0.25) {
                require(!c.basalTalusApron().active() && !c.basal().actual().found(), "talus filled suppressed core");
            }
        }
        var core = evaluator.column(3042, 199);
        require(core.geology().faultCarveMask() > 0.999 && !core.basalTalusApron().active(),
                "fixed fault-core regression moved");
        var lowCleanup = new ArrakisTerrainEvaluator(-5640511200611798902L, settings, 64);
        require(lowCleanup.rawRockOccupies(lowCleanup.preTalusColumn(3052, 96), 68)
                && !lowCleanup.rockOccupies(3052, 68, 96), "fixed unsupported basal remnant was not removed");

        // Existing targeted tooth is *not* fixed by basal protection: its face relief is below 24.
        var tooth = evaluator.column(3050, 190);
        require(tooth.face().localRelief() < settings.erosion().orphanRemnants().minimumFaceRelief()
                && evaluator.rockOccupies(3050, 70, 190) && !tooth.talusOccupiesY(70),
                "reported tooth classification changed; inspect before adjusting its policy");
        String report = ArrakisTerrainCommand.describe(evaluator, 0, settings, 3050, 70, 190);
        require(report.contains("protects-through-Y=64") && !report.contains("\r"), "inspector floor/newlines wrong");
    }

    private static void fixture(ArrakisTerrainSettings settings, long seed, int x, int z, int cx, int cz) {
        var full = new ArrakisTerrainEvaluator(seed, settings, 1024);
        var rock = full.preTalusColumn(x, z);
        full.highestFilteredRockY(x, z);
        require(full.completedColumns() == 0, "pre-talus query evaluated deposits recursively");
        var column = full.column(x, z);
        require(full.completedColumns() == 1, "contact search evaluated neighbor deposits recursively");
        var actual = column.basal().actual();
        require(actual.found() && actual.x() == cx && actual.z() == cz && actual.outwardDistance() == 0,
                "fixed wall-adjacent contact failed at " + x + "/" + z);
        require(column.basalTalusApron().active() && column.basalTalusApron().height() > 0,
                "adjacent cell does not receive talus");
        require(column.basalTalusApron().materialAt(column.basalTalusApron().topY())
                == BasalTalusApronField.Material.GRAVEL, "wall-adjacent crest is bare sand");
        require(Math.abs(column.basal().structural().signedDistance()) > 4,
                "fixture no longer distinguishes structural and actual contact");
        int wallTop = full.highestFilteredRockY(cx, cz);
        int dx = Integer.signum(cx - x), dz = Integer.signum(cz - z);
        for (int step = 1; step <= 24; step++) wallTop = Math.max(wallTop,
                full.highestFilteredRockY(cx + step * dx, cz + step * dz));
        require(actual.wallTopY() == wallTop, "wall relief is not based on final pre-talus heights");
        long expected = ArrakisTerrainEvaluatorValidation.fingerprint(full, x, z);
        for (int cap : new int[] {0, 1, 64}) {
            var limited = new ArrakisTerrainEvaluator(seed, settings, cap);
            limited.preTalusColumn(-123, 456);
            limited.preTalusColumn(cx, cz);
            require(limited.column(x, z).basal().equals(column.basal()), "contact depends on cache state/order");
            require(ArrakisTerrainEvaluatorValidation.fingerprint(limited, x, z) == expected,
                    "deposit/material composition depends on cache state");
            require(limited.size() <= cap, "cache limit escaped");
        }
        require(full.preTalusColumn(x, z).equals(rock), "deposit evaluation mutated rock result");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
