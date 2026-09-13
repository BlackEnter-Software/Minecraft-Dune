package com.blackenter.minecraftdune.worldgen.arrakis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Frozen column fingerprints protect saved algorithm identities from accidental drift. */
final class TerrainAlgorithmValidation {
    static void validate(JsonObject currentJson, ArrakisTerrainSettings current) throws Exception {
        require(current.terrainAlgorithmRevision() == 3, "preset must explicitly select algorithm 3");
        var encoded = ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, current).getOrThrow().getAsJsonObject();
        require(encoded.get("terrain_algorithm_revision").getAsInt() == 3, "saved algorithm identity missing");
        for (int revision : new int[] {-1, 0, 1, 2, 4, 999}) {
            var invalid = currentJson.deepCopy(); invalid.addProperty("terrain_algorithm_revision", revision);
            require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(),
                    "unsupported or contradictory revision accepted: " + revision);
        }
        var ambiguous = currentJson.deepCopy(); ambiguous.remove("terrain_algorithm_revision");
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, ambiguous).error().isPresent(),
                "unversioned dev.2 save silently migrated");
        try {
            new ArrakisTerrainEvaluator(0, current.withAlgorithmRevision(4), 0);
            throw new AssertionError("Direct evaluator construction accepted an unknown algorithm");
        } catch (IllegalArgumentException expected) { /* Also enforce identity outside codec loading. */ }
        var dev1 = load("arrakis_6000_dev1.json");
        require(dev1.terrainAlgorithmRevision() == 1, "unambiguous dev.1 compatibility resolution failed");
        require(ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE,
                ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, dev1).getOrThrow()).getOrThrow().equals(dev1),
                "dev.1 revision does not persist on round trip");
        fingerprint("dev1", dev1, "bc6249591a68032b5712cf37b461a05f7dcc950b1a60e7a39c99e6621a034896");
        fingerprint("dev2", load("arrakis_6000_dev2.json"), "7819b1b4bb61528742bf701dbe686a812f5fe9cf2c513a19b8cdb59acd817696");
        fingerprint("dev2 saved defaults", load("arrakis_6000_dev2_saved.json"),
                "7819b1b4bb61528742bf701dbe686a812f5fe9cf2c513a19b8cdb59acd817696");
        var dev3 = load("arrakis_6000_dev3.json");
        require(dev3.equals(current), "revision-3 preset differs from its frozen fixture");
        fingerprint("dev3", dev3, "6027a1f35b2b7f6f65f46dbed15f6393af38e24147125c6db7cc7a36396f28f6");
    }

    static ArrakisTerrainSettings load(String file) throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/test/resources/terrain", file))).getAsJsonObject();
        if (json.has("dimensions")) json = json.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld")
                .getAsJsonObject("generator").getAsJsonObject("terrain");
        return ArrakisTerrainSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static void fingerprint(String name, ArrakisTerrainSettings settings, String expected) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        for (long seed : new long[] {0, -5640511200611798902L}) {
            var evaluator = new ArrakisTerrainEvaluator(seed, settings, 1024);
            var points = new java.util.ArrayList<int[]>(java.util.List.of(new int[][] {
                    {0,0}, {3057,150}, {3060,150}, {3100,150}, {3400,0},
                    {4096,0}, {3053,190}, {-2960,-589}, {-17,4103}, {-16,4102}, {9000,9000}}));
            if (settings.terrainAlgorithmRevision() == 3) {
                // Freeze occupied cavity columns too, without altering historical coverage.
                var feature = evaluator.cavityFeature(-10,48);
                for (int x=-640;x<-576;x++)for(int z=3072;z<3136;z++)
                    if(feature.column(x,z).volume()>0)points.add(new int[]{x,z});
                if(seed==0)require(feature.volume()>0,"revision-3 fingerprint lost native cavity coverage");
            }
            for (int[] point : points) {
                var column = evaluator.buriedColumn(point[0], point[1]);
                digest.update((Double.toHexString(column.raw().rockTop()) + ":"
                        + Double.toHexString(column.erosion().rockTop()) + ":"
                        + Double.toHexString(column.sediment().surfaceY()) + "\n").getBytes(StandardCharsets.UTF_8));
                for (int y = -64; y < 320; y++) {
                    var cell = column.cellAt(y, -64);
                    digest.update((cell.kind() + ":" + cell.material() + ":" + cell.layers() + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        System.out.println("Frozen " + name + " fingerprint: " + actual);
        require(actual.equals(expected), "Frozen " + name + " terrain changed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
