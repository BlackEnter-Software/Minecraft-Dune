package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.ArrakisProfileValidation;
import com.blackenter.minecraftdune.worldgen.geology.ScarpMorphologyField;
import net.minecraft.world.level.ChunkPos;
import com.mojang.serialization.JsonOps;

import java.util.Locale;
import java.util.function.IntPredicate;

/** Offline analytical measurements, never a build-blocking visual acceptance test. */
public final class ArrakisContactDiagnostics {
    public static void main(String[] args) throws Exception {
        var settings = ArrakisProfileValidation.loadProfile().settings();
        if (java.util.Arrays.asList(args).contains("--front-shell")) {
            summarizeFrontShell(settings);
            return;
        }
        if (java.util.Arrays.asList(args).contains("--basal-tuning")) {
            var old = new ArrakisTerrainEvaluator(0,BasalTuningValidation.previousSettings(settings),1024);
            var now = new ArrakisTerrainEvaluator(0,settings,1024);
            for (int[] p : BasalTuningValidation.POINTS) {
                int x = p[0], z = p[1];
                var c = now.column(x,z);
                System.out.printf("Basal tuning %d/%d pre-talus-top=%d->%d root-Y64=%s->%s rock-Y65=%s->%s "
                        + "contact=%s/%.2f talus=%d->%d skirt-reach=%.2f depth=%d%n",x,z,
                        old.highestFilteredRockY(x,z),now.highestFilteredRockY(x,z),
                        old.nativeFoundationOccupies(x,64,z),now.nativeFoundationOccupies(x,64,z),
                        old.rockOccupies(x,65,z),now.rockOccupies(x,65,z),c.basal().source(),
                        c.basal().actual().signedDistance(),old.column(x,z).basalTalusApron().height(),
                        c.basalTalusApron().height(),c.skirt().outwardReach(),c.skirt().depth());
            }
            return;
        }
        if (java.util.Arrays.asList(args).contains("--ravine-attachment")) {
            int contacts = 0, gaps = 0, north = 0, south = 0;
            long start = System.nanoTime();
            for (long seed : new long[] {0, -5640511200611798902L}) {
                for (int z = 100; z <= 220; z++) {
                    var e = new ArrakisTerrainEvaluator(seed, settings, 1024);
                    for (int x = 3192; x <= 3208; x++) {
                        var c = e.column(x,z); var a = c.basal().actual();
                        if (!c.basal().source().equals("ravine") || !c.basalTalusApron().active() || a.signedDistance() > 0) continue;
                        contacts++;
                        if (seed == 0 && a.z() < z && north++ == 0) System.out.printf("North-wall fixture: %d/%d %s%n", x,z,a);
                        if (seed == 0 && a.z() > z && south++ == 0) System.out.printf("South-wall fixture: %d/%d %s%n", x,z,a);
                        int nx = a.x() + Integer.signum(x-a.x()), nz = a.z() + Integer.signum(z-a.z());
                        if (!e.column(nx,nz).basalTalusApron().active()) {
                            if (++gaps <= 8) System.out.printf("Ravine attachment gap seed=%d X/Z=%d/%d adjacent=%d/%d %s%n",
                                    seed,x,z,nx,nz,a);
                        }
                    }
                }
            }
            System.out.printf("Optional ravine attachment: contacts=%d gaps=%d north=%d south=%d elapsed-ms=%.1f%n",
                    contacts,gaps,north,south,(System.nanoTime()-start)/1_000_000.0);
            profileChunk(settings, 0L, 199, 12, true);
            profileChunk(BasalFinishingValidation.withoutFaultFinishing(settings), 0L, 199, 12, true);
            return;
        }
        if (java.util.Arrays.asList(args).contains("--fault-components")) {
            for (int[] center : new int[][] {{3050,190},{3200,200},{3067,106},{3089,173},{3050,254}}) {
                var e = new ArrakisTerrainEvaluator(0, settings, 1024);
                for (int z = center[1]-3; z <= center[1]+3; z++) for (int x = center[0]-3; x <= center[0]+3; x++) {
                    var c = e.preTalusColumn(x,z);
                    var component = e.componentCleanup(x,z);
                    if (center[0] != 3050 || center[1] != 190) { if (!component.removed()) continue; }
                    System.out.printf("Fault component %d/%d raw-top=%d fault=%.3f face=%s low=%d relief=%.1f surface=%s component=%s%n",
                            x,z,c.rockTopY(),c.geology().faultCarveMask(),c.face().exposed(),c.face().lowY(),
                            c.face().localRelief(),c.surfaceErosion().active(),component);
                }
            }
            return;
        }
        if (java.util.Arrays.asList(args).contains("--fault-edge")) {
            var old = new ArrakisTerrainEvaluator(0, BasalFinishingValidation.withoutFaultFinishing(settings), 1024);
            var current = new ArrakisTerrainEvaluator(0, settings, 1024);
            for (int[] p : new int[][] {{3050,190},{3200,200},{3201,200},{3200,199},{3200,190},
                    {3001,464},{4086,0},{3042,199}}) {
                System.out.printf("Before X/Z=%d/%d top=%d component=%s contact=%s%n", p[0], p[1],
                        old.highestFilteredRockY(p[0],p[1]), old.componentCleanup(p[0],p[1]), old.column(p[0],p[1]).basal());
                System.out.println(ArrakisTerrainCommand.describe(current, 0, settings, p[0], 70, p[1]));
            }
            return;
        }
        if (java.util.Arrays.asList(args).contains("--components")) {
            summarizeComponents(settings, 24, new long[] {0, -5640511200611798902L});
            return;
        }
        describeRimOwnership(settings);
        if (java.util.Arrays.asList(args).contains("--rim-only")) return;
        profileChunk(settings, 0L, 190, 12, false); // JVM warmup; not reported.
        profileChunk(settings, 0L, 190, 12, true);
        profileChunk(settings, -5640511200611798902L, 191, 6, true);
        profileChunk(settings, -5640511200611798902L, 193, 10, true);
        profileChunk(settings, 0L, 41, 206, true);
        profileChunk(settings, 0L, 159, 106, true);
        profileChunk(settings, 0L, 0, 0, true);
        profileChunk(settings, 0L, 409, 0, true);
        profileChunk(settings, 0L, 187, 29, true);
        profileChunk(settings, 0L, 191, -4, true);
        var beforeFinishing = BasalFinishingValidation.withoutFinishing(settings);
        profileChunk(beforeFinishing, -5640511200611798902L, 191, 6, true);
        profileChunk(beforeFinishing, 0L, 187, 29, true);
        profileChunk(beforeFinishing, 0L, 191, -4, true);
        var structuralJson = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
                .getOrThrow().getAsJsonObject();
        structuralJson.getAsJsonObject("lithology").getAsJsonObject("talus").addProperty("actual_contact_enabled", false);
        var structural = ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, structuralJson).getOrThrow();
        profileChunk(structural, -5640511200611798902L, 191, 6, true);
        profileChunk(structural, 0L, 187, 29, true);
        profileChunk(structural, 0L, 191, -4, true);
        transect(settings, 0L, 190, 2920, 3130, false);
        transect(settings, 0L, 3053, 100, 220, true);
        transect(settings, -5640511200611798902L, 173, 2990, 3160, false);
        measureActualAttachment(settings);
        recommendOuterContact(settings);
        var evaluator = new ArrakisTerrainEvaluator(0L, settings, 64);
        System.out.println(ArrakisTerrainCommand.describe(evaluator, 0L, settings, 3053, 65, 190));
        System.out.println(ArrakisTerrainCommand.describe(evaluator, 0L, settings, 3050, 70, 190));
        System.out.println(ArrakisTerrainCommand.describe(evaluator, 0L, settings, 3001, 70, 464));
        if (java.util.Arrays.asList(args).contains("--trace")) trace(settings);
        if (java.util.Arrays.asList(args).contains("--legacy-gap")) measureApronGap(settings, 0L);
        findLowRemnant(settings);
        summarizeComponents(settings, 8, new long[] {0});
    }

    private static void describeRimOwnership(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
        for (int[] p : new int[][] {{2963,615},{3001,464},{2991,464},{2984,464},{2986,464},{2987,464},{2988,464},
                {3043,200},{3050,254},{3050,190},{3053,190},{3002,464},{3005,464},{4086,0}}) {
            var c = evaluator.column(p[0], p[1]);
            if (p[0] == 2988) {
                System.out.printf("Rim context: geology=%s face=%s major=%s surface=%s%n", c.geology(), c.face(),
                        c.erosion().candidate(), c.surfaceErosion().active());
                for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                    System.out.printf("Rim neighbor %d/%d top=%d%n", p[0]+dx,p[1]+dz,
                            evaluator.highestFilteredRockY(p[0]+dx,p[1]+dz));
                }
            }
            for (int y : new int[] {64,65}) {
                boolean nativeWrite = c.hasNativeRock() && y <= c.rockTopY()
                        && evaluator.filteredRockOccupies(p[0],p[1],y,c,c.materialSampleAt(y));
                System.out.printf("Rim: X/Z=%d/%d Y=%d native-write=%s lithology=%s basal=%s local-talus=%s final-top=%d contact=%.1f fault=%.3f face-low=%d owner=%s original=%d mantle=%s%n",
                        p[0],p[1],y,nativeWrite,c.materialSampleAt(y).material(),evaluator.basalMaterialAt(p[0],y,p[1],c),
                        c.talusOccupiesY(y),evaluator.highestFilteredRockY(p[0],p[1]),
                        c.basal().actual().signedDistance(),c.geology().faultCarveMask(),c.face().lowY(),
                        evaluator.preSkirtOwner(p[0],y,p[1]),c.originalRockTopY(),c.skirt().visibleY65Mantle());
            }
        }
    }

    private static void summarizeComponents(ArrakisTerrainSettings settings, int radius, long[] seeds) {
        int examined = 0, candidates = 0, removed = 0;
        for (long seed : seeds) for (int[] center : new int[][] {{3001, 464}, {3043, 200}, {3050, 254}, {2963, 615}, {3067, 106}}) {
            for (int z = center[1] - radius; z <= center[1] + radius; z++) {
                var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
                for (int x = center[0] - radius; x <= center[0] + radius; x++) {
                    examined++;
                    var component = evaluator.componentCleanup(x, z);
                    if (component.candidate()) candidates++;
                    if (!component.removed()) continue;
                    removed++;
                    if (removed <= 12) System.out.printf("Component cleanup seed=%d X/Z=%d/%d fault=%.3f %s%n", seed, x, z,
                            evaluator.preTalusColumn(x,z).geology().faultCarveMask(), component);
                }
            }
        }
        System.out.printf("Optional component survey: columns=%d basal-candidates=%d removed-columns=%d; no population assertion.%n",
                examined, candidates, removed);
    }

    private static void summarizeFrontShell(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(0L, settings, 1024);
        var visited = new java.util.HashSet<Long>();
        int examined = 0, eligible = 0, pass1 = 0, pass2 = 0;
        int reported = 0;
        for (int direction = 0; direction < 128; direction++) {
            double angle = direction * Math.PI / 64.0;
            for (int radius = 2800; radius <= 4250; radius++) {
                int x = (int) Math.floor(Math.cos(angle) * radius);
                int z = (int) Math.floor(Math.sin(angle) * radius);
                long key = ChunkPos.asLong(x, z);
                if (!visited.add(key)) continue;
                examined++;
                var shell = evaluator.frontShellCleanup(x, z);
                if (shell.pass1Eligible()) eligible++;
                if (shell.pass1Removed()) pass1++;
                if (shell.pass2Removed()) pass2++;
                if (shell.removed() && reported++ < 16) {
                    System.out.printf(Locale.ROOT,
                            "Seed-0 front-shell fixture X/Z=%d/%d wall=%s pass=%d top=%d signed=%.2f outward=(%.3f,%.3f)%n",
                            x, z, shell.wall(), shell.pass1Removed() ? 1 : 2, shell.preCleanTopY(),
                            shell.signedStructuralDistance(), shell.outwardNormalX(), shell.outwardNormalZ());
                }
            }
        }
        System.out.printf("Front-shell angular survey: columns=%d eligible=%d pass1=%d pass2=%d removed=%d.%n",
                examined, eligible, pass1, pass2, pass1 + pass2);
        for (int[] point : new int[][] {{3057,150},{3059,150},{3060,150},{3050,190},{4098,0},{4096,0},{4095,0}}) {
            var shell = evaluator.frontShellCleanup(point[0], point[1]);
            System.out.printf("Front-shell fixed probe %d/%d: %s%n", point[0], point[1], shell);
        }
    }

    private static void trace(ArrakisTerrainSettings settings) {
        for (int z : new int[] {464, -52}) {
            var probe = new ArrakisTerrainEvaluator(0L, settings, 1024);
            for (int x = z == 464 ? 2950 : 3028; x <= (z == 464 ? 3010 : 3080); x += 2) {
                var c = probe.column(x, z);
                System.out.printf("Foot probe 0 %d/%d rock=%d apron=%d contact=%s%n", x, z,
                        probe.highestFilteredRockY(x, z), c.basalTalusApron().height(), c.basal().actual());
            }
        }
        var random = new ArrakisTerrainEvaluator(-5640511200611798902L, settings, 1024);
        for (int x = 3050; x <= 3080; x += 2) {
            var c = random.column(x, 173);
            System.out.printf("Foot probe random %d/173 rock=%d apron=%d contact=%s%n", x,
                    random.highestFilteredRockY(x, 173), c.basalTalusApron().height(), c.basal().actual());
        }
    }

    private static void recommendOuterContact(ArrakisTerrainSettings settings) {
        for (int direction = 0; direction < 32; direction++) {
            double angle = direction * Math.PI / 16.0;
            var evaluator = new ArrakisTerrainEvaluator(0L, settings, 64);
            for (int radius = 3600; radius <= 4800; radius += 4) {
                int x = (int) Math.floor(Math.cos(angle) * radius);
                int z = (int) Math.floor(Math.sin(angle) * radius);
                var c = evaluator.column(x, z);
                if (!c.basalTalusApron().active()) continue;
                var g = c.geology();
                var contact = ScarpMorphologyField.nearestMassifLowSideContact(0L, x + 0.5, z + 0.5,
                        g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
                if (contact.valid() && contact.inwardX() * x + contact.inwardZ() * z < 0) {
                    System.out.printf("Seed-0 outer contact diagnostic location: X/Z=%d/%d filtered-top=%d apron=%d contact=%s%n",
                            x, z, evaluator.highestFilteredRockY(x, z), c.basalTalusApron().height(), c.basal().actual());
                    return;
                }
            }
        }
        System.out.println("No outer apron found in the bounded diagnostic scan; not a test failure.");
    }

    private static void profileChunk(ArrakisTerrainSettings settings, long seed,
            int chunkX, int chunkZ, boolean report) {
        var metrics = TerrainGenerationMetrics.evaluation();
        var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024, metrics);
        long start = System.nanoTime();
        int blocks = 0;
        for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
            for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                var c = evaluator.column(x, z);
                if (!c.hasNativeRock()) continue;
                // The current flat profile's hard foundation ends at Y44.
                for (int y = 45; y <= c.rockTopY(); y++) {
                    if (evaluator.filteredRockOccupies(x, z, y, c, c.materialSampleAt(y))) blocks++;
                }
            }
        }
        long elapsed = System.nanoTime() - start;
        if (!report) return;
        TerrainGenerationMetrics.recordChunk(new ChunkPos(chunkX, chunkZ), elapsed, metrics, evaluator.size());
        System.out.printf(Locale.ROOT,
                "Analytical-only chunk seed=%d actual=%s component=%s skirt=%s chunk=%d/%d: %.2f ms evaluations=%d hit=%.2f%% cached=%d bypass=%d rock-writes=%d%n",
                seed, settings.lithology().talus().actualContactEnabled(), settings.erosion().orphanRemnants().basalComponentCleanupEnabled(),
                settings.lithology().talus().basalSandSkirtEnabled(), chunkX, chunkZ, elapsed / 1_000_000.0, metrics.misses(),
                100.0 * metrics.hits() / Math.max(1, metrics.hits() + metrics.misses()),
                evaluator.size(), metrics.bypasses(), blocks);
    }

    private static void transect(ArrakisTerrainSettings settings, long seed,
            int fixed, int start, int end, boolean alongZ) {
        var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
        int n = end - start + 1;
        int[] original = new int[n], raw = new int[n], filtered = new int[n];
        int[] apron = new int[n], scree = new int[n], screeBase = new int[n];
        boolean[] major70 = new boolean[n], surface70 = new boolean[n], filtered70 = new boolean[n];
        boolean[] contact = new boolean[n];
        for (int coordinate = start; coordinate <= end; coordinate++) {
            int i = coordinate - start;
            int x = alongZ ? fixed : coordinate;
            int z = alongZ ? coordinate : fixed;
            var c = evaluator.column(x, z);
            original[i] = c.originalRockTopY();
            raw[i] = c.rockTopY();
            filtered[i] = evaluator.highestFilteredRockY(x, z);
            apron[i] = c.basalTalusApron().height();
            scree[i] = c.erosion().talusThickness();
            screeBase[i] = c.talusBaseY();
            major70[i] = original[i] >= 70 && c.erosion().occupies(70, c.materialSampleAt(70));
            surface70[i] = original[i] >= 70 && c.surfaceErosion().occupies(70, c.materialSampleAt(70));
            filtered70[i] = evaluator.rockOccupies(x, 70, z);
            var g = c.geology();
            var structural = ScarpMorphologyField.nearestMassifLowSideContact(seed, x + 0.5, z + 0.5,
                    g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
            contact[i] = structural.valid() && Math.abs(structural.signedDistance()) <= 1.0;
        }
        System.out.printf("%nContact transect seed=%d %s=%d %s=%d..%d (inclusive intervals)%n",
                seed, alongZ ? "X" : "Z", fixed, alongZ ? "Z" : "X", start, end);
        intervals("structural contact +/-1", start, n, i -> contact[i]);
        intervals("macro rock at Y70", start, n, i -> original[i] >= 70);
        intervals("major-only rock at Y70", start, n, i -> major70[i]);
        intervals("surface-only rock at Y70", start, n, i -> surface70[i]);
        intervals("filtered rock at Y70", start, n, i -> filtered70[i]);
        intervals("raw rock reaches Y84", start, n, i -> raw[i] >= 84);
        intervals("filtered rock reaches Y84", start, n, i -> filtered[i] >= 84);
        intervals("basal apron present", start, n, i -> apron[i] > 0);
        intervals("local scree present", start, n, i -> scree[i] > 0);
        for (int i = 0; i < n; i++) {
            if (apron[i] > 0 || scree[i] > 0) {
                if (i % 4 == 0) System.out.printf(
                        "deposit coordinate=%d original/raw/filtered=%d/%d/%d apron=%d local=%d fromY=%d%n",
                        start + i, original[i], raw[i], filtered[i], apron[i], scree[i], screeBase[i]);
            }
        }
    }

    private static void intervals(String label, int start, int n, IntPredicate included) {
        StringBuilder result = new StringBuilder(label).append(":");
        for (int i = 0; i < n; i++) {
            if (!included.test(i)) continue;
            int first = i;
            while (i + 1 < n && included.test(i + 1)) i++;
            result.append(' ').append(start + first).append("..").append(start + i);
        }
        System.out.println(result);
    }

    private static void findLowRemnant(ArrakisTerrainSettings settings) {
        var evaluator = new ArrakisTerrainEvaluator(-5640511200611798902L, settings, 1024);
        for (int z = 96; z <= 111; z++) for (int x = 3040; x <= 3071; x++) {
            var c = evaluator.preTalusColumn(x, z);
            for (int y = 65; y <= 69; y++) {
                if (evaluator.rawRockOccupies(c, y) && !evaluator.rockOccupies(x, y, z)) {
                    System.out.printf("Basal-only cleanup example seed=-5640511200611798902 XYZ=%d/%d/%d%n", x, y, z);
                    return;
                }
            }
        }
        System.out.println("No low cleanup example in this diagnostic window.");
    }

    private static void measureActualAttachment(ArrakisTerrainSettings settings) {
        int active = 0, adjacent = 0, mismatches = 0;
        for (int z = -100; z <= 500; z += 4) {
            var evaluator = new ArrakisTerrainEvaluator(0, settings, 1024);
            for (int x = 2900; x <= 3120; x += 4) {
                var c = evaluator.column(x, z);
                if (!c.basalTalusApron().active()) continue;
                active++;
                var contact = c.basal().actual();
                if (!contact.found()) { mismatches++; continue; }
                int dx = Integer.compare(contact.x(), x), dz = Integer.compare(contact.z(), z);
                if (contact.signedDistance() > 0) {
                    dx = Math.abs(c.basal().structural().inwardX()) >= Math.abs(c.basal().structural().inwardZ())
                            ? (c.basal().structural().inwardX() >= 0 ? 1 : -1) : 0;
                    dz = dx == 0 ? (c.basal().structural().inwardZ() >= 0 ? 1 : -1) : 0;
                }
                var neighbor = evaluator.column(contact.x() - dx, contact.z() - dz);
                var next = neighbor.basal().actual();
                adjacent++;
                if (!neighbor.basalTalusApron().active() || !next.found() || next.outwardDistance() != 0
                        || next.x() != contact.x() || next.z() != contact.z()) {
                    mismatches++;
                    System.out.printf("Attachment mismatch %d/%d contact=%s adjacent=%s structural=%s%n",
                            x, z, contact, neighbor.basal(), c.basal().structural());
                }
            }
        }
        System.out.printf("Seed-0 actual attachment survey: active=%d adjacent-rechecks=%d mismatches=%d "
                + "(analytical only; not a visual acceptance test).%n", active, adjacent, mismatches);
    }

    /** Historical diagnostic only: the radial Y84/Y70 criterion is not the new contact rule. */
    private static void measureApronGap(ArrakisTerrainSettings settings, long seed) {
        int found = 0, missed = 0, maximumGap = -1;
        String representative = "none";
        for (int z = -100; z <= 300; z += 4) {
            var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
            for (int x = 2900; x <= 3120; x += 4) {
                var c = evaluator.column(x, z);
                if (!c.basalTalusApron().active() || c.basalTalusApron().outwardDistance() > 0.0) continue;
                var g = c.geology();
                var contact = ScarpMorphologyField.nearestMassifLowSideContact(seed, x + 0.5, z + 0.5,
                        g.radiusBlocks(), g.effectiveRadiusBlocks(), settings.massif());
                if (!contact.valid()) continue;
                int lastApron = 0;
                boolean apronEnded = false, reached = false;
                for (int step = 1; step <= 64; step++) {
                    int px = (int) Math.floor(x + 0.5 + contact.inwardX() * step);
                    int pz = (int) Math.floor(z + 0.5 + contact.inwardZ() * step);
                    var next = evaluator.column(px, pz);
                    if (!apronEnded && next.basalTalusApron().active()) lastApron = step;
                    else apronEnded = true;
                    // Diagnostic definition, not a generation rule: a surviving wall that
                    // reaches Y84 and contains solid rock at Y70 on the same inward ray.
                    if (evaluator.highestFilteredRockY(px, pz) >= 84 && evaluator.rockOccupies(px, 70, pz)) {
                        int gap = Math.max(0, step - lastApron - 1);
                        found++;
                        reached = true;
                        if (gap > maximumGap) {
                            maximumGap = gap;
                            representative = String.format(Locale.ROOT,
                                    "apron=%d/%d last-apron-step=%d wall=%d/%d step=%d fault=%.3f",
                                    x, z, lastApron, px, pz, step, next.geology().faultCarveMask());
                        }
                        break;
                    }
                }
                if (!reached) missed++;
            }
        }
        System.out.printf("Seed-%d LEGACY radial Y84/Y70 heuristic (not the actual-contact rule): %d rays reached wall, %d did not within 64; "
                + "largest apron-to-wall separation=%d steps; %s%n", seed, found, missed, maximumGap, representative);
    }
}
