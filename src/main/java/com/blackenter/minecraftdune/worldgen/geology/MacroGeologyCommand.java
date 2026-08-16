package com.blackenter.minecraftdune.worldgen.geology;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Operator-only macro-geology inspection and materialization commands for Arrakis Dev.
 *
 * <p>The geology field itself is world-scale and coordinate-based. The normal generate and
 * clear commands operate synchronously on one aligned 256 x 256 laboratory tile. Large-area
 * generation is delegated to {@link MacroGeologyGenerationManager} so it can be spread over
 * server ticks instead of freezing one tick for tens of thousands of Minecraft chunks.</p>
 */
public final class MacroGeologyCommand {
    public static final int TEST_REGION_SIZE = 256;

    private static final int BLOCK_UPDATE_FLAGS = 2;
    private static final int FIRST_GEOLOGY_Y = MacroGeologyField.BASE_SURFACE_Y + 1;
    private static final int LAST_GEOLOGY_Y = MacroGeologyField.BASE_SURFACE_Y
            + MacroGeologyField.MAX_ADDED_ROCK_HEIGHT;

    private MacroGeologyCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("geology")
                .then(Commands.literal("info")
                        .executes(MacroGeologyCommand::info))
                .then(Commands.literal("sample")
                        .then(Commands.argument(
                                        "x",
                                        IntegerArgumentType.integer(-30_000_000, 30_000_000)
                                )
                                .then(Commands.argument(
                                                "z",
                                                IntegerArgumentType.integer(-30_000_000, 30_000_000)
                                        )
                                        .executes(MacroGeologyCommand::sampleCoordinates))))
                .then(Commands.literal("generate")
                        .executes(MacroGeologyCommand::generate))
                .then(Commands.literal("generate_initial")
                        .executes(context ->
                                MacroGeologyGenerationManager.startInitial(context.getSource())))
                .then(Commands.literal("generate_nearest")
                        .then(Commands.argument(
                                        "radius",
                                        IntegerArgumentType.integer(1, 12)
                                )
                                .executes(context ->
                                        MacroGeologyGenerationManager.startNearest(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "radius")
                                        ))))
                .then(Commands.literal("generation")
                        .then(Commands.literal("status")
                                .executes(context ->
                                        MacroGeologyGenerationManager.status(context.getSource())))
                        .then(Commands.literal("cancel")
                                .executes(context ->
                                        MacroGeologyGenerationManager.cancel(context.getSource()))))
                .then(Commands.literal("clear")
                        .executes(MacroGeologyCommand::clear));
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        double x = source.getPosition().x;
        double z = source.getPosition().z;
        MacroGeologyField.Sample sample = MacroGeologyField.sample(
                source.getLevel().getSeed(),
                x,
                z
        );
        return sendSample(source, x, z, sample);
    }

    private static int sampleCoordinates(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");
        MacroGeologyField.Sample sample = MacroGeologyField.sample(
                source.getLevel().getSeed(),
                x,
                z
        );
        return sendSample(source, x, z, sample);
    }

    private static int sendSample(
            CommandSourceStack source,
            double x,
            double z,
            MacroGeologyField.Sample sample
    ) {
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Macro geology @ X=%.1f Z=%.1f: radius=%.1f, effective_radius=%.1f, "
                                + "province=%s, target_surface_Y=%.1f.",
                        x,
                        z,
                        sample.radiusBlocks(),
                        sample.effectiveRadiusBlocks(),
                        sample.dominantProvince().commandName(),
                        sample.baseElevation()
                )),
                false
        );
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Province weights: basin=%.3f, transition=%.3f, massif=%.3f, "
                                + "eroded_margin=%.3f, open_desert=%.3f.",
                        sample.centralBasinWeight(),
                        sample.rockTransitionWeight(),
                        sample.massifWeight(),
                        sample.erodedMarginWeight(),
                        sample.openDesertWeight()
                )),
                false
        );
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Rock field: formation_mask=%.3f, added_height=%.1f, boundary_warp=%+.1f blocks.",
                        sample.rockFormationMask(),
                        sample.addedRockHeight(),
                        sample.boundaryWarpBlocks()
                )),
                false
        );
        return 1;
    }

    private static int generate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Region region = regionAt(source);

        source.sendSuccess(
                () -> Component.literal(
                        "Generating crude macro geology in " + region.description()
                                + ". This synchronous development command may pause the server."
                ),
                false
        );

        long startNanoseconds = System.nanoTime();
        GenerationStats stats = materializeRegion(level, region);
        double elapsedSeconds = (System.nanoTime() - startNanoseconds) / 1_000_000_000.0;

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Generated macro geology: %s, %d changed blocks, maximum surface Y=%d, %.2f s.",
                        region.description(),
                        stats.changedBlocks(),
                        stats.maximumTopY(),
                        elapsedSeconds
                )),
                true
        );
        return stats.changedBlocks();
    }

    /**
     * Fast additive materialization used by the tick-spread large-area jobs.
     *
     * <p>This deliberately does not scan from Y=240 downward to erase old prototype stone.
     * Large jobs are intended for freshly generated inspection areas. Avoiding that clearing
     * pass removes tens of thousands of unnecessary block-state checks per Minecraft chunk.
     * The normal single-tile {@code generate} command retains the full cleanup behavior.</p>
     */
    static GenerationStats materializeChunkForJob(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        // Force the vanilla/Arrakis Dev chunk to exist even when the geology sample is flat.
        // This is important for large-area inspection tools such as Distant Horizons.
        level.getChunk(chunkX, chunkZ);

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();
        int changedBlocks = 0;
        int maximumTopY = MacroGeologyField.BASE_SURFACE_Y;

        int minimumX = chunkX << 4;
        int minimumZ = chunkZ << 4;

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minimumZ + localZ;

            for (int localX = 0; localX < 16; localX++) {
                int worldX = minimumX + localX;

                MacroGeologyField.Sample sample = MacroGeologyField.sample(
                        level.getSeed(),
                        worldX + 0.5,
                        worldZ + 0.5
                );

                int targetTopY = Mth.clamp(
                        Mth.floor(sample.baseElevation() + 0.5),
                        MacroGeologyField.BASE_SURFACE_Y,
                        LAST_GEOLOGY_Y
                );
                maximumTopY = Math.max(maximumTopY, targetTopY);

                if (targetTopY <= MacroGeologyField.BASE_SURFACE_Y) {
                    continue;
                }

                for (int y = FIRST_GEOLOGY_Y; y <= targetTopY; y++) {
                    position.set(worldX, y, worldZ);
                    BlockState existing = level.getBlockState(position);

                    if (!existing.isAir() && !existing.is(Blocks.STONE)) {
                        break;
                    }

                    if (!existing.is(Blocks.STONE)
                            && level.setBlock(position, stone, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }
            }
        }

        return new GenerationStats(changedBlocks, maximumTopY);
    }

    private static GenerationStats materializeRegion(ServerLevel level, Region region) {
        preloadRegion(level, region);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int changedBlocks = 0;
        int maximumTopY = MacroGeologyField.BASE_SURFACE_Y;

        for (int worldZ = region.minimumZ(); worldZ <= region.maximumZ(); worldZ++) {
            for (int worldX = region.minimumX(); worldX <= region.maximumX(); worldX++) {
                MacroGeologyField.Sample sample = MacroGeologyField.sample(
                        level.getSeed(),
                        worldX + 0.5,
                        worldZ + 0.5
                );
                int targetTopY = Mth.clamp(
                        Mth.floor(sample.baseElevation() + 0.5),
                        MacroGeologyField.BASE_SURFACE_Y,
                        LAST_GEOLOGY_Y
                );
                maximumTopY = Math.max(maximumTopY, targetTopY);

                for (int y = LAST_GEOLOGY_Y; y > targetTopY; y--) {
                    position.set(worldX, y, worldZ);
                    if (level.getBlockState(position).is(Blocks.STONE)
                            && level.setBlock(position, air, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }

                for (int y = FIRST_GEOLOGY_Y; y <= targetTopY; y++) {
                    position.set(worldX, y, worldZ);
                    BlockState existing = level.getBlockState(position);
                    if (!existing.isAir() && !existing.is(Blocks.STONE)) {
                        break;
                    }
                    if (!existing.is(Blocks.STONE)
                            && level.setBlock(position, stone, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }
            }
        }

        return new GenerationStats(changedBlocks, maximumTopY);
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Region region = regionAt(source);
        preloadRegion(level, region);

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int changedBlocks = 0;

        for (int worldZ = region.minimumZ(); worldZ <= region.maximumZ(); worldZ++) {
            for (int worldX = region.minimumX(); worldX <= region.maximumX(); worldX++) {
                for (int y = FIRST_GEOLOGY_Y; y <= LAST_GEOLOGY_Y; y++) {
                    position.set(worldX, y, worldZ);
                    if (level.getBlockState(position).is(Blocks.STONE)
                            && level.setBlock(position, air, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }
            }
        }

        int finalChangedBlocks = changedBlocks;
        source.sendSuccess(
                () -> Component.literal(
                        "Cleared " + finalChangedBlocks + " macro-geology stone blocks from "
                                + region.description() + "."
                ),
                true
        );
        return changedBlocks;
    }

    private static void preloadRegion(ServerLevel level, Region region) {
        int minimumChunkX = region.minimumX() >> 4;
        int maximumChunkX = region.maximumX() >> 4;
        int minimumChunkZ = region.minimumZ() >> 4;
        int maximumChunkZ = region.maximumZ() >> 4;
        for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static Region regionAt(CommandSourceStack source) {
        int blockX = Mth.floor(source.getPosition().x);
        int blockZ = Mth.floor(source.getPosition().z);
        int minimumX = Math.floorDiv(blockX, TEST_REGION_SIZE) * TEST_REGION_SIZE;
        int minimumZ = Math.floorDiv(blockZ, TEST_REGION_SIZE) * TEST_REGION_SIZE;
        return new Region(minimumX, minimumZ);
    }

    private record Region(int minimumX, int minimumZ) {
        int maximumX() {
            return minimumX + TEST_REGION_SIZE - 1;
        }

        int maximumZ() {
            return minimumZ + TEST_REGION_SIZE - 1;
        }

        String description() {
            return "X=" + minimumX + ".." + maximumX()
                    + ", Z=" + minimumZ + ".." + maximumZ()
                    + " (" + TEST_REGION_SIZE + "x" + TEST_REGION_SIZE + ")";
        }
    }

    record GenerationStats(
            int changedBlocks,
            int maximumTopY
    ) {
    }
}
