package com.blackenter.minecraftdune.worldgen.prototype;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.registry.ModBlocks;
import com.blackenter.minecraftdune.world.level.block.DuneSandLayerBlock;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Runs prototype simulation, chunk loading, and owned block edits without long command ticks. */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class DunePrototypeOperationManager {
    private static final int FIRST_DUNE_Y = DuneSimulation.BASE_SURFACE_Y + 1;
    private static final int MAX_COLUMNS_PER_TICK = 128;
    private static final long MAX_APPLY_NANOS_PER_TICK = 5_000_000L;
    private static final int MAX_IN_FLIGHT_CHUNKS = 2;
    private static final float BACKOFF_TICK_MILLIS = 40.0F;

    private static final Map<MinecraftServer, Operation> ACTIVE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private DunePrototypeOperationManager() {
    }

    static int startGenerate(
            CommandSourceStack source,
            DuneMode mode,
            long seed,
            DuneSimulation.Settings settings,
            DuneSurfaceResolution resolution,
            int minimumX,
            int minimumZ
    ) {
        if (!canStart(source)) {
            return 0;
        }

        CompletableFuture<DuneSimulation.Result> simulation = CompletableFuture.supplyAsync(
                () -> DuneSimulation.simulate(mode, seed, settings, resolution),
                Util.backgroundExecutor()
        );
        Operation operation = new Operation(
                source,
                OperationType.GENERATE,
                new Region(minimumX, minimumZ, settings.regionBlockSize()),
                simulation,
                mode.commandName()
        );
        ACTIVE.put(source.getServer(), operation);
        source.sendSuccess(
                () -> Component.literal(
                        "Started owned " + mode.commandName() + " dune generation in "
                                + operation.region.description()
                                + ". Use /dune dunes operation status or cancel."
                ),
                true
        );
        return 1;
    }

    static int startClear(
            CommandSourceStack source,
            int minimumX,
            int minimumZ,
            int regionBlockSize
    ) {
        if (!canStart(source)) {
            return 0;
        }

        Operation operation = new Operation(
                source,
                OperationType.CLEAR,
                new Region(minimumX, minimumZ, regionBlockSize),
                CompletableFuture.completedFuture(null),
                "clear"
        );
        ACTIVE.put(source.getServer(), operation);
        source.sendSuccess(
                () -> Component.literal(
                        "Started safe prototype clear in " + operation.region.description()
                                + ". Only blocks recorded as prototype-owned are eligible."
                ),
                true
        );
        return 1;
    }

    static int status(CommandSourceStack source) {
        Operation operation = ACTIVE.get(source.getServer());
        if (operation == null) {
            source.sendSuccess(
                    () -> Component.literal("No dune prototype operation is active."),
                    false
            );
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Dune prototype " + operation.label + ": " + operation.stageName()
                                + ", " + operation.nextColumn + "/" + operation.region.columnCount()
                                + " columns, " + operation.changedBlocks + " block changes, "
                                + operation.remainingChunks.size() + " chunks queued and "
                                + operation.inFlightChunks.size() + " in flight."
                ),
                false
        );
        return 1;
    }

    static int cancel(CommandSourceStack source) {
        Operation operation = ACTIVE.remove(source.getServer());
        if (operation == null) {
            source.sendFailure(Component.literal("No dune prototype operation is active."));
            return 0;
        }
        operation.cancel();
        source.sendSuccess(
                () -> Component.literal(
                        "Cancelled dune prototype " + operation.label + " after "
                                + operation.changedBlocks + " block changes."
                ),
                true
        );
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Operation operation = ACTIVE.get(server);
        if (operation == null) {
            return;
        }

        ServerLevel level = server.getLevel(operation.dimension);
        if (level == null) {
            fail(server, operation, "its dimension is no longer loaded");
            return;
        }
        if (operation.failure != null) {
            fail(server, operation, operation.failure);
            return;
        }

        if (!operation.simulation.isDone()) {
            return;
        }
        if (operation.result == null && operation.type == OperationType.GENERATE) {
            try {
                operation.result = operation.simulation.join();
            } catch (RuntimeException exception) {
                MinecraftDune.LOGGER.error("Dune prototype simulation failed", exception);
                fail(server, operation, "the simulation failed");
                return;
            }
        }

        if (server.getCurrentSmoothedTickTime() < BACKOFF_TICK_MILLIS
                && !operation.remainingChunks.isEmpty()
                && operation.inFlightChunks.size() < MAX_IN_FLIGHT_CHUNKS) {
            requestChunk(operation, level, operation.remainingChunks.removeFirst());
        }
        if (!operation.remainingChunks.isEmpty() || !operation.inFlightChunks.isEmpty()) {
            return;
        }

        applyColumns(server, level, operation);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Operation operation = ACTIVE.remove(event.getServer());
        if (operation != null) {
            operation.cancel();
        }
    }

    private static void applyColumns(
            MinecraftServer server,
            ServerLevel level,
            Operation operation
    ) {
        DunePrototypeState state = DunePrototypeState.get(level);
        long startNanos = System.nanoTime();
        int columnsThisTick = 0;
        while (operation.nextColumn < operation.region.columnCount()
                && columnsThisTick < MAX_COLUMNS_PER_TICK) {
            int localX = operation.nextColumn % operation.region.size;
            int localZ = operation.nextColumn / operation.region.size;
            int worldX = operation.region.minimumX + localX;
            int worldZ = operation.region.minimumZ + localZ;
            operation.changedBlocks += operation.type == OperationType.GENERATE
                    ? applyGeneratedColumn(level, state, operation.result, localX, localZ, worldX, worldZ)
                    : clearOwnedColumn(level, state, worldX, worldZ);
            operation.nextColumn++;
            columnsThisTick++;
            if (System.nanoTime() - startNanos >= MAX_APPLY_NANOS_PER_TICK) {
                break;
            }
        }

        if (operation.nextColumn < operation.region.columnCount()) {
            return;
        }

        ACTIVE.remove(server);
        double elapsedSeconds = (System.nanoTime() - operation.startNanoseconds) / 1_000_000_000.0;
        sendFeedback(
                operation,
                Component.literal(String.format(
                        java.util.Locale.ROOT,
                        "Dune prototype %s complete: %s, %d changed blocks, %.2f s.",
                        operation.label,
                        operation.region.description(),
                        operation.changedBlocks,
                        elapsedSeconds
                )),
                false
        );
    }

    private static int applyGeneratedColumn(
            ServerLevel level,
            DunePrototypeState state,
            DuneSimulation.Result result,
            int localX,
            int localZ,
            int worldX,
            int worldZ
    ) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState sand = ModBlocks.SAND.get().defaultBlockState();
        DunePrototypeState.OwnedColumn ownership = state.ownership(worldX, worldZ);
        int fullBlocks = result.fullBlocksAt(localX, localZ);
        int partialLayers = result.partialLayersAt(localX, localZ);
        int changed = 0;

        for (int offset = 31; offset >= fullBlocks; offset--) {
            int y = FIRST_DUNE_Y + offset;
            position.set(worldX, y, worldZ);
            if (ownership.ownsFullBlock(offset)) {
                if (level.getBlockState(position).is(ModBlocks.SAND.get())
                        && level.setBlock(position, air, 2)) {
                    changed++;
                }
                ownership = ownership.withFullBlock(offset, false);
            }
        }

        if (ownership.partialLayers() > 0 && ownership.partialY() != fullBlocks) {
            int y = FIRST_DUNE_Y + ownership.partialY();
            position.set(worldX, y, worldZ);
            if (isExpectedLayer(level.getBlockState(position), ownership.partialLayers())
                    && level.setBlock(position, air, 2)) {
                changed++;
            }
            ownership = ownership.withPartial(-1, 0);
        }

        boolean unobstructed = true;
        for (int offset = 0; offset < fullBlocks; offset++) {
            int y = FIRST_DUNE_Y + offset;
            position.set(worldX, y, worldZ);
            BlockState existing = level.getBlockState(position);
            boolean ownedFull = ownership.ownsFullBlock(offset);
            boolean trackedPartial = ownership.partialY() == offset
                    && ownership.partialLayers() > 0;
            boolean ownedPartial = trackedPartial
                    && isExpectedLayer(existing, ownership.partialLayers());
            if (ownedFull && existing.is(ModBlocks.SAND.get())) {
                continue;
            }
            if (ownedFull) {
                ownership = ownership.withFullBlock(offset, false);
            }
            if (trackedPartial) {
                ownership = ownership.withPartial(-1, 0);
            }
            if (!existing.isAir() && !ownedPartial) {
                unobstructed = false;
                break;
            }
            if (!existing.equals(sand) && level.setBlock(position, sand, 2)) {
                changed++;
            }
            ownership = ownership.withFullBlock(offset, true);
        }

        if (unobstructed && partialLayers > 0) {
            int offset = fullBlocks;
            position.set(worldX, FIRST_DUNE_Y + offset, worldZ);
            BlockState existing = level.getBlockState(position);
            boolean ownedPartial = ownership.partialY() == offset
                    && ownership.partialLayers() > 0
                    && isExpectedLayer(existing, ownership.partialLayers());
            if (existing.isAir() || ownedPartial) {
                BlockState layer = ModBlocks.SAND_LAYER.get().defaultBlockState()
                        .setValue(DuneSandLayerBlock.LAYERS, partialLayers);
                if (!existing.equals(layer) && level.setBlock(position, layer, 2)) {
                    changed++;
                }
                ownership = ownership.withPartial(offset, partialLayers);
            } else if (ownership.partialY() == offset) {
                ownership = ownership.withPartial(-1, 0);
            }
        } else if (ownership.partialY() == fullBlocks) {
            ownership = ownership.withPartial(-1, 0);
        }

        state.ownership(worldX, worldZ, ownership);
        return changed;
    }

    private static int clearOwnedColumn(
            ServerLevel level,
            DunePrototypeState state,
            int worldX,
            int worldZ
    ) {
        DunePrototypeState.OwnedColumn ownership = state.ownership(worldX, worldZ);
        if (ownership.isEmpty()) {
            return 0;
        }

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int changed = 0;
        for (int offset = 0; offset < 32; offset++) {
            if (!ownership.ownsFullBlock(offset)) {
                continue;
            }
            position.set(worldX, FIRST_DUNE_Y + offset, worldZ);
            if (level.getBlockState(position).is(ModBlocks.SAND.get())
                    && level.setBlock(position, air, 2)) {
                changed++;
            }
        }
        if (ownership.partialLayers() > 0) {
            position.set(worldX, FIRST_DUNE_Y + ownership.partialY(), worldZ);
            if (isExpectedLayer(level.getBlockState(position), ownership.partialLayers())
                    && level.setBlock(position, air, 2)) {
                changed++;
            }
        }
        state.ownership(worldX, worldZ, DunePrototypeState.OwnedColumn.EMPTY);
        return changed;
    }

    private static boolean isExpectedLayer(BlockState state, int layers) {
        return state.is(ModBlocks.SAND_LAYER.get())
                && state.getValue(DuneSandLayerBlock.LAYERS) == layers;
    }

    private static void requestChunk(Operation operation, ServerLevel level, ChunkPos chunk) {
        // 1.21.1 getChunkFuture blocks on the server thread; the off-thread entry point
        // marshals the actual request to mainThreadProcessor without managed-blocking.
        var chunkSource = level.getChunkSource();
        CompletableFuture<ChunkResult<ChunkAccess>> request = CompletableFuture
                .supplyAsync(
                        () -> chunkSource.getChunkFuture(
                                chunk.x, chunk.z, ChunkStatus.FULL, true
                        ),
                        Util.backgroundExecutor()
                )
                .thenCompose(Function.identity());
        operation.inFlightChunks.put(chunk.toLong(), request);
        request.whenCompleteAsync(
                (result, error) -> {
                    if (ACTIVE.get(operation.server) != operation) {
                        return;
                    }
                    operation.inFlightChunks.remove(chunk.toLong());
                    if (error != null) {
                        MinecraftDune.LOGGER.error(
                                "Dune prototype chunk preparation failed for {}",
                                chunk,
                                error
                        );
                        operation.failure = "chunk preparation failed at " + chunk;
                    } else if (result == null || !result.isSuccess()) {
                        operation.failure = "chunk preparation failed at " + chunk
                                + ": " + (result == null ? "no result" : result.getError());
                    }
                },
                operation.server
        );
    }

    private static boolean canStart(CommandSourceStack source) {
        if (ACTIVE.containsKey(source.getServer())) {
            source.sendFailure(Component.literal(
                    "A dune prototype operation is already active. Use operation status or cancel."
            ));
            return false;
        }
        return true;
    }

    private static void fail(MinecraftServer server, Operation operation, String reason) {
        ACTIVE.remove(server);
        operation.cancel();
        sendFeedback(
                operation,
                Component.literal("Dune prototype " + operation.label + " stopped because " + reason + "."),
                true
        );
    }

    private static void sendFeedback(Operation operation, Component message, boolean failure) {
        ServerPlayer player = operation.feedbackPlayer == null
                ? null
                : operation.server.getPlayerList().getPlayer(operation.feedbackPlayer);
        if (player != null) {
            player.sendSystemMessage(message);
        } else if (failure) {
            MinecraftDune.LOGGER.warn(message.getString());
        } else {
            MinecraftDune.LOGGER.info(message.getString());
        }
    }

    private enum OperationType {
        GENERATE,
        CLEAR
    }

    private static final class Operation {
        private final MinecraftServer server;
        private final UUID feedbackPlayer;
        private final ResourceKey<Level> dimension;
        private final OperationType type;
        private final Region region;
        private final CompletableFuture<DuneSimulation.Result> simulation;
        private final String label;
        private final ArrayDeque<ChunkPos> remainingChunks;
        private final Map<Long, CompletableFuture<ChunkResult<ChunkAccess>>> inFlightChunks =
                new HashMap<>();
        private final long startNanoseconds = System.nanoTime();

        private DuneSimulation.Result result;
        private String failure;
        private int nextColumn;
        private int changedBlocks;

        private Operation(
                CommandSourceStack source,
                OperationType type,
                Region region,
                CompletableFuture<DuneSimulation.Result> simulation,
                String label
        ) {
            server = source.getServer();
            feedbackPlayer = source.getEntity() instanceof ServerPlayer player
                    ? player.getUUID()
                    : null;
            dimension = source.getLevel().dimension();
            this.type = type;
            this.region = region;
            this.simulation = simulation;
            this.label = label;
            remainingChunks = region.chunks();
        }

        private String stageName() {
            if (!simulation.isDone()) {
                return "simulating off-thread";
            }
            if (!remainingChunks.isEmpty() || !inFlightChunks.isEmpty()) {
                return "preparing chunks";
            }
            return "applying owned columns";
        }

        private void cancel() {
            simulation.cancel(false);
            for (CompletableFuture<?> request : inFlightChunks.values()) {
                request.cancel(false);
            }
            inFlightChunks.clear();
        }
    }

    private record Region(int minimumX, int minimumZ, int size) {
        private int columnCount() {
            return size * size;
        }

        private ArrayDeque<ChunkPos> chunks() {
            ArrayDeque<ChunkPos> chunks = new ArrayDeque<>();
            int maximumX = minimumX + size - 1;
            int maximumZ = minimumZ + size - 1;
            for (int chunkZ = minimumZ >> 4; chunkZ <= maximumZ >> 4; chunkZ++) {
                for (int chunkX = minimumX >> 4; chunkX <= maximumX >> 4; chunkX++) {
                    chunks.addLast(new ChunkPos(chunkX, chunkZ));
                }
            }
            return chunks;
        }

        private String description() {
            return "X=" + minimumX + ".." + (minimumX + size - 1)
                    + ", Z=" + minimumZ + ".." + (minimumZ + size - 1);
        }
    }
}
