package com.blackenter.minecraftdune.worldgen.arrakis;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Opt-in development validation, executed on the integrated server thread in an isolated save. */
public final class ArrakisRuntimeValidation {
    private ArrakisRuntimeValidation() {}

    public static void run(MinecraftServer server, Path output, boolean reload, String folder) throws Exception {
        var level = server.overworld();
        if (!(level.getChunkSource().getGenerator() instanceof ArrakisChunkGenerator generator)) {
            throw new IllegalStateException("Validation world is not using the Arrakis generator");
        }
        var settings = generator.terrainSettings();
        Path actualSave = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (!actualSave.getFileName().toString().equals(folder) || level.getSeed() != 0
                || !settings.isBuriedRock() || settings.terrainAlgorithmRevision() != 2) {
            throw new IllegalStateException("Validation world identity mismatch: " + actualSave);
        }
        JsonObject report = new JsonObject();
        report.addProperty("seed", level.getSeed());
        report.addProperty("save", actualSave.toString());
        report.addProperty("profile", settings.profileVersion());
        report.addProperty("algorithm", settings.terrainAlgorithmRevision());
        report.add("settings", ArrakisTerrainSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow());
        var hashes = new JsonObject();
        var random = level.getChunkSource().randomState();
        var pos = new BlockPos.MutableBlockPos();
        int columns = 0, blocks = 0, heights = 0;
        // Basin, inner wall/gully/shoulder, plateau, outer wall, negative coordinates and far erg.
        int[][] chunks = {{0,0}, {191,9}, {193,9}, {190,11}, {212,0}, {256,0}, {-185,-37}, {-1,256}, {562,562}};
        for (int[] coordinate : chunks) {
            if (reload && level.getChunkSource().chunkMap.read(new net.minecraft.world.level.ChunkPos(
                    coordinate[0], coordinate[1])).join().isEmpty()) {
                throw new IllegalStateException("Reload target was not saved to disk: " + coordinate[0] + "," + coordinate[1]);
            }
            var chunk = level.getChunk(coordinate[0], coordinate[1]);
            var digest = MessageDigest.getInstance("SHA-256");
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                int x = coordinate[0] * 16 + dx, z = coordinate[1] * 16 + dz;
                var expected = generator.getBaseColumn(x, z, level, random);
                columns++;
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    var actual = chunk.getBlockState(pos.set(x, y, z));
                    if (!actual.equals(expected.getBlock(y))) {
                        throw new IllegalStateException("Chunk/base-column mismatch at " + pos + ": " + actual
                                + " vs " + expected.getBlock(y));
                    }
                    digest.update((actual.toString() + "\n").getBytes(StandardCharsets.UTF_8));
                    blocks++;
                }
                for (var type : new Heightmap.Types[] {Heightmap.Types.WORLD_SURFACE,
                        Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.MOTION_BLOCKING}) {
                    int expectedHeight = generator.getBaseHeight(x, z, type, level, random);
                    int actualHeight = chunk.getHeight(type, dx, dz) + 1;
                    if (actualHeight != expectedHeight) throw new IllegalStateException(
                            "Heightmap mismatch " + type + " at " + x + "/" + z + ": " + actualHeight + " vs " + expectedHeight);
                    digest.update((type + ":" + actualHeight + "\n").getBytes(StandardCharsets.UTF_8));
                    heights++;
                }
            }
            hashes.addProperty(coordinate[0] + "," + coordinate[1], HexFormat.of().formatHex(digest.digest()));
        }
        report.add("chunk_hashes", hashes);
        report.addProperty("columns_checked", columns);
        report.addProperty("blocks_checked", blocks);
        report.addProperty("heightmaps_checked", heights);
        Files.createDirectories(output);
        if (reload) {
            var before = JsonParser.parseString(Files.readString(output.resolve("generated.json"))).getAsJsonObject();
            if (!before.equals(report)) throw new IllegalStateException("Saved/reloaded terrain or world identity differs");
        }
        server.saveEverything(true, true, true);
        Files.writeString(output.resolve(reload ? "reloaded.json" : "generated.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
