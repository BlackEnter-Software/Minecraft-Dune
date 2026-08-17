package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisChunkGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tick-spread pregeneration for native Arrakis geology.
 *
 * <p>The job only asks Minecraft to generate/load chunks to FULL status. The registered
 * {@link ArrakisChunkGenerator} creates the geology during normal chunk generation. There
 * is no second pass of ServerLevel#setBlock calls.</p>
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class MacroGeologyGenerationManager {
    public static final int INITIAL_RADIUS_CHUNKS = 100;
    public static final int INITIAL_RADIUS_BLOCKS = INITIAL_RADIUS_CHUNKS * 16;

    /**
     * One 256 x 256 geology test tile contains 16 x 16 normal Minecraft chunks.
     */
    public static final int CHUNKS_PER_TILE = MacroGeologyCommand.TEST_REGION_SIZE / 16;

    /**
     * Conservative development limits. Native generation is much cheaper than the 0.5.7
     * post-placement pass, but one unusually expensive chunk is still allowed to consume
     * the tick by itself.
     */
    private static final int MAX_CHUNKS_PER_TICK = 8;
    private static final long MAX_JOB_NANOS_PER_TICK = 30_000_000L;

    private static GenerationJob activeJob;

    private MacroGeologyGenerationManager() {
    }

    public static int startCurrentTile(CommandSourceStack source) {
        if (!canStart(source)) {
            return 0;
        }

        int blockX = (int) Math.floor(source.getPosition().x);
        int blockZ = (int) Math.floor(source.getPosition().z);
        int centerTileX = Math.floorDiv(blockX, MacroGeologyCommand.TEST_REGION_SIZE);
        int centerTileZ = Math.floorDiv(blockZ, MacroGeologyCommand.TEST_REGION_SIZE);

        return startTileRadius(
                source,
                centerTileX,
                centerTileZ,
                0,
                "current 256x256 geology tile"
        );
    }

    public static int startInitial(CommandSourceStack source) {
        if (!canStart(source)) {
            return 0;
        }

        List<ChunkPos> chunks = buildOriginRadiusChunks();
        chunks.sort(Comparator.comparingLong(
                chunk -> squaredDistanceFromOriginToChunkCenter(chunk.x, chunk.z)
        ));

        activeJob = new GenerationJob(
                source,
                source.getLevel().dimension(),
                "initial radius 100 Minecraft chunks from (0,0)",
                chunks
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Started native Arrakis pregeneration: 100 Minecraft-chunk radius "
                                + "(1600 blocks) around absolute (0,0), "
                                + chunks.size() + " chunks queued. Geology will be created "
                                + "by normal chunk generation. Use "
                                + "/dune geology generation status."
                ),
                true
        );
        return 1;
    }

    public static int startNearest(CommandSourceStack source, int tileRadius) {
        if (!canStart(source)) {
            return 0;
        }

        int blockX = (int) Math.floor(source.getPosition().x);
        int blockZ = (int) Math.floor(source.getPosition().z);
        int centerTileX = Math.floorDiv(blockX, MacroGeologyCommand.TEST_REGION_SIZE);
        int centerTileZ = Math.floorDiv(blockZ, MacroGeologyCommand.TEST_REGION_SIZE);

        return startTileRadius(
                source,
                centerTileX,
                centerTileZ,
                tileRadius,
                "player-centered geology tile radius " + tileRadius
        );
    }

    public static int status(CommandSourceStack source) {
        GenerationJob job = activeJob;
        if (job == null || job.server() != source.getServer()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "No native Arrakis pregeneration job is active."
                    ),
                    false
            );
            return 1;
        }

        double percent = job.totalChunks == 0
                ? 100.0
                : (100.0 * job.processedChunks / job.totalChunks);
        double elapsedSeconds =
                (System.nanoTime() - job.startNanoseconds) / 1_000_000_000.0;

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Native Arrakis pregeneration: %s. %d/%d chunks (%.1f%%), "
                                + "%d remaining, %.1f s elapsed.",
                        job.description,
                        job.processedChunks,
                        job.totalChunks,
                        percent,
                        job.remaining.size(),
                        elapsedSeconds
                )),
                false
        );
        return 1;
    }

    public static int cancel(CommandSourceStack source) {
        GenerationJob job = activeJob;
        if (job == null || job.server() != source.getServer()) {
            source.sendFailure(Component.literal(
                    "No native Arrakis pregeneration job is active."
            ));
            return 0;
        }

        int processed = job.processedChunks;
        int total = job.totalChunks;
        activeJob = null;

        source.sendSuccess(
                () -> Component.literal(
                        "Cancelled native Arrakis pregeneration after "
                                + processed + "/" + total + " chunks."
                ),
                true
        );
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        GenerationJob job = activeJob;
        if (job == null) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (job.server() != server) {
            activeJob = null;
            return;
        }

        ServerLevel level = server.getLevel(job.dimension);
        if (level == null) {
            job.source.sendFailure(Component.literal(
                    "Native Arrakis pregeneration stopped because its dimension "
                            + "is no longer loaded."
            ));
            activeJob = null;
            return;
        }

        long tickStart = System.nanoTime();
        int processedThisTick = 0;

        while (!job.remaining.isEmpty()
                && processedThisTick < MAX_CHUNKS_PER_TICK) {
            ChunkPos chunk = job.remaining.removeFirst();

            // FULL status runs the ordinary chunk-generation pipeline. For an Arrakis Dev
            // world that pipeline now invokes ArrakisChunkGenerator.fillFromNoise(), where
            // the macro geology is written directly into ChunkAccess.
            level.getChunkSource().getChunk(
                    chunk.x,
                    chunk.z,
                    ChunkStatus.FULL,
                    true
            );

            job.processedChunks++;
            processedThisTick++;

            if (System.nanoTime() - tickStart >= MAX_JOB_NANOS_PER_TICK) {
                break;
            }
        }

        if (!job.remaining.isEmpty()) {
            return;
        }

        double elapsedSeconds =
                (System.nanoTime() - job.startNanoseconds) / 1_000_000_000.0;
        GenerationJob completed = job;
        activeJob = null;

        completed.source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Native Arrakis pregeneration complete: %s; %d chunks, %.1f s.",
                        completed.description,
                        completed.totalChunks,
                        elapsedSeconds
                )),
                true
        );
    }

    private static int startTileRadius(
            CommandSourceStack source,
            int centerTileX,
            int centerTileZ,
            int tileRadius,
            String description
    ) {
        List<ChunkPos> chunks = buildTileSquareChunks(
                centerTileX,
                centerTileZ,
                tileRadius
        );

        final double sourceX = source.getPosition().x;
        final double sourceZ = source.getPosition().z;
        chunks.sort(Comparator.comparingDouble(
                chunk -> squaredDistanceToChunkCenter(
                        sourceX,
                        sourceZ,
                        chunk.x,
                        chunk.z
                )
        ));

        int tilesWide = tileRadius * 2 + 1;
        int tileCount = tilesWide * tilesWide;

        activeJob = new GenerationJob(
                source,
                source.getLevel().dimension(),
                description,
                chunks
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Started native Arrakis pregeneration: "
                                + tilesWide + "x" + tilesWide
                                + " geology tiles (" + tileCount + " total), "
                                + chunks.size() + " Minecraft chunks queued. "
                                + "Geology will be created by native chunk generation."
                ),
                true
        );
        return 1;
    }

    private static boolean canStart(CommandSourceStack source) {
        if (!(source.getLevel().getChunkSource().getGenerator()
                instanceof ArrakisChunkGenerator)) {
            source.sendFailure(Component.literal(
                    "This command requires an Arrakis Dev world using the 0.5.8 "
                            + "native Arrakis chunk generator. Existing 0.5.7 worlds "
                            + "keep their old generator; create a new Arrakis Dev world."
            ));
            return false;
        }

        if (activeJob != null && activeJob.server() == source.getServer()) {
            source.sendFailure(Component.literal(
                    "A native Arrakis pregeneration job is already active. Use "
                            + "/dune geology generation status or "
                            + "/dune geology generation cancel."
            ));
            return false;
        }

        // The integrated server may be stopped and restarted in the same client JVM.
        if (activeJob != null) {
            activeJob = null;
        }
        return true;
    }

    private static List<ChunkPos> buildOriginRadiusChunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        int searchRadius = INITIAL_RADIUS_CHUNKS + 1;
        long radiusSquared =
                (long) INITIAL_RADIUS_BLOCKS * INITIAL_RADIUS_BLOCKS;

        for (int chunkZ = -searchRadius;
                chunkZ <= searchRadius;
                chunkZ++) {
            for (int chunkX = -searchRadius;
                    chunkX <= searchRadius;
                    chunkX++) {
                if (minimumSquaredDistanceFromOriginToChunk(
                        chunkX,
                        chunkZ
                ) <= radiusSquared) {
                    chunks.add(new ChunkPos(chunkX, chunkZ));
                }
            }
        }

        return chunks;
    }

    private static List<ChunkPos> buildTileSquareChunks(
            int centerTileX,
            int centerTileZ,
            int tileRadius
    ) {
        int tileCountWide = tileRadius * 2 + 1;
        int estimatedChunkCount =
                tileCountWide
                        * tileCountWide
                        * CHUNKS_PER_TILE
                        * CHUNKS_PER_TILE;
        List<ChunkPos> chunks = new ArrayList<>(estimatedChunkCount);

        for (int tileZ = centerTileZ - tileRadius;
                tileZ <= centerTileZ + tileRadius;
                tileZ++) {
            for (int tileX = centerTileX - tileRadius;
                    tileX <= centerTileX + tileRadius;
                    tileX++) {
                int minimumChunkX = tileX * CHUNKS_PER_TILE;
                int minimumChunkZ = tileZ * CHUNKS_PER_TILE;

                for (int localChunkZ = 0;
                        localChunkZ < CHUNKS_PER_TILE;
                        localChunkZ++) {
                    for (int localChunkX = 0;
                            localChunkX < CHUNKS_PER_TILE;
                            localChunkX++) {
                        chunks.add(new ChunkPos(
                                minimumChunkX + localChunkX,
                                minimumChunkZ + localChunkZ
                        ));
                    }
                }
            }
        }

        return chunks;
    }

    private static long minimumSquaredDistanceFromOriginToChunk(
            int chunkX,
            int chunkZ
    ) {
        int minimumX = chunkX << 4;
        int maximumX = minimumX + 15;
        int minimumZ = chunkZ << 4;
        int maximumZ = minimumZ + 15;

        long nearestX = nearestCoordinateToZero(minimumX, maximumX);
        long nearestZ = nearestCoordinateToZero(minimumZ, maximumZ);
        return nearestX * nearestX + nearestZ * nearestZ;
    }

    private static long nearestCoordinateToZero(int minimum, int maximum) {
        if (minimum > 0) {
            return minimum;
        }
        if (maximum < 0) {
            return maximum;
        }
        return 0L;
    }

    private static long squaredDistanceFromOriginToChunkCenter(
            int chunkX,
            int chunkZ
    ) {
        long centerX = ((long) chunkX << 4) + 8L;
        long centerZ = ((long) chunkZ << 4) + 8L;
        return centerX * centerX + centerZ * centerZ;
    }

    private static double squaredDistanceToChunkCenter(
            double worldX,
            double worldZ,
            int chunkX,
            int chunkZ
    ) {
        double centerX = (chunkX << 4) + 8.0;
        double centerZ = (chunkZ << 4) + 8.0;
        double deltaX = centerX - worldX;
        double deltaZ = centerZ - worldZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static final class GenerationJob {
        private final CommandSourceStack source;
        private final ResourceKey<Level> dimension;
        private final String description;
        private final ArrayDeque<ChunkPos> remaining;
        private final int totalChunks;
        private final long startNanoseconds;

        private int processedChunks;

        private GenerationJob(
                CommandSourceStack source,
                ResourceKey<Level> dimension,
                String description,
                List<ChunkPos> chunks
        ) {
            this.source = source;
            this.dimension = dimension;
            this.description = description;
            this.remaining = new ArrayDeque<>(chunks);
            this.totalChunks = chunks.size();
            this.startNanoseconds = System.nanoTime();
        }

        private MinecraftServer server() {
            return source.getServer();
        }
    }
}
