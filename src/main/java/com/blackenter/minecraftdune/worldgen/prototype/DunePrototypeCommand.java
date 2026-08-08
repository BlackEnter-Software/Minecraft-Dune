package com.blackenter.minecraftdune.worldgen.prototype;

import com.blackenter.minecraftdune.MinecraftDune;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * Operator-only commands for generating and clearing prototype dunes in Arrakis Dev.
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class DunePrototypeCommand {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;
    private static final int BLOCK_UPDATE_FLAGS = 2;
    private static final int FIRST_DUNE_Y = DuneSimulation.BASE_SURFACE_Y + 1;
    private static final int LAST_DUNE_Y =
            DuneSimulation.BASE_SURFACE_Y + DuneSimulation.MAXIMUM_PROTOTYPE_HEIGHT;

    private DunePrototypeCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("minecraftdune")
                        .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                        .then(Commands.literal("dunes")
                                .then(Commands.literal("generate")
                                        .then(Commands.literal(DuneMode.TRANSVERSE.commandName())
                                                .executes(context -> generate(
                                                        context,
                                                        DuneMode.TRANSVERSE
                                                )))
                                        .then(Commands.literal(DuneMode.BARCHAN.commandName())
                                                .executes(context -> generate(
                                                        context,
                                                        DuneMode.BARCHAN
                                                ))))
                                .then(Commands.literal("clear")
                                        .executes(DunePrototypeCommand::clear))
                                .then(Commands.literal("info")
                                        .executes(DunePrototypeCommand::info)))
        );
    }

    private static int generate(
            CommandContext<CommandSourceStack> context,
            DuneMode mode
    ) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Region region = regionAt(source);
        long seed = regionalSeed(level.getSeed(), region.minimumX(), region.minimumZ(), mode);

        source.sendSuccess(
                () -> Component.literal(
                        "Generating " + mode.commandName() + " dunes in " + region.description()
                                + ". This development command may pause the server briefly."
                ),
                false
        );

        long startNanoseconds = System.nanoTime();
        DuneSimulation.Result result = DuneSimulation.simulate(mode, seed);
        int changedBlocks = applyHeightField(level, region, result);
        double elapsedSeconds = (System.nanoTime() - startNanoseconds) / 1_000_000_000.0;

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Generated %s dunes: %d changed blocks, maximum +%d Y, %.3f sand-mass drift, %.2f s. Seed %s.",
                        mode.commandName(),
                        changedBlocks,
                        result.maximumHeight(),
                        result.massDifference(),
                        elapsedSeconds,
                        Long.toUnsignedString(seed)
                )),
                true
        );

        return changedBlocks;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Region region = regionAt(source);
        int changedBlocks = clearPrototypeSand(level, region);

        source.sendSuccess(
                () -> Component.literal(
                        "Cleared " + changedBlocks + " prototype sand blocks from "
                                + region.description() + "."
                ),
                true
        );
        return changedBlocks;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Region region = regionAt(source);
        source.sendSuccess(
                () -> Component.literal(
                        "Arrakis Dev dune region: " + region.description()
                                + ", base surface Y=" + DuneSimulation.BASE_SURFACE_Y
                                + ", size=" + DuneSimulation.REGION_BLOCK_SIZE + "x"
                                + DuneSimulation.REGION_BLOCK_SIZE
                                + ", wind=24 degrees toward +X/+Z."
                ),
                false
        );
        return 1;
    }

    private static int applyHeightField(
            ServerLevel level,
            Region region,
            DuneSimulation.Result result
    ) {
        preloadRegion(level, region);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int changedBlocks = 0;

        for (int localZ = 0; localZ < DuneSimulation.REGION_BLOCK_SIZE; localZ++) {
            int worldZ = region.minimumZ() + localZ;
            for (int localX = 0; localX < DuneSimulation.REGION_BLOCK_SIZE; localX++) {
                int worldX = region.minimumX() + localX;
                int targetTopY = DuneSimulation.BASE_SURFACE_Y
                        + result.heightAt(localX, localZ);

                for (int y = LAST_DUNE_Y; y > targetTopY; y--) {
                    position.set(worldX, y, worldZ);
                    if (level.getBlockState(position).is(Blocks.SAND)) {
                        if (level.setBlock(position, air, BLOCK_UPDATE_FLAGS)) {
                            changedBlocks++;
                        }
                    }
                }

                for (int y = FIRST_DUNE_Y; y <= targetTopY; y++) {
                    position.set(worldX, y, worldZ);
                    BlockState existing = level.getBlockState(position);
                    if (!existing.isAir() && !existing.is(Blocks.SAND)) {
                        break;
                    }
                    if (!existing.is(Blocks.SAND)
                            && level.setBlock(position, sand, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }
            }
        }

        return changedBlocks;
    }

    private static int clearPrototypeSand(ServerLevel level, Region region) {
        preloadRegion(level, region);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        int changedBlocks = 0;

        for (int worldZ = region.minimumZ(); worldZ <= region.maximumZ(); worldZ++) {
            for (int worldX = region.minimumX(); worldX <= region.maximumX(); worldX++) {
                for (int y = FIRST_DUNE_Y; y <= LAST_DUNE_Y; y++) {
                    position.set(worldX, y, worldZ);
                    if (level.getBlockState(position).is(Blocks.SAND)
                            && level.setBlock(position, air, BLOCK_UPDATE_FLAGS)) {
                        changedBlocks++;
                    }
                }
            }
        }

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
        int minimumX = Math.floorDiv(blockX, DuneSimulation.REGION_BLOCK_SIZE)
                * DuneSimulation.REGION_BLOCK_SIZE;
        int minimumZ = Math.floorDiv(blockZ, DuneSimulation.REGION_BLOCK_SIZE)
                * DuneSimulation.REGION_BLOCK_SIZE;
        return new Region(minimumX, minimumZ);
    }

    private static long regionalSeed(
            long worldSeed,
            int regionX,
            int regionZ,
            DuneMode mode
    ) {
        long value = worldSeed ^ mode.seedSalt();
        value ^= (long) regionX * 0x9E3779B97F4A7C15L;
        value ^= (long) regionZ * 0xC2B2AE3D27D4EB4FL;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private record Region(int minimumX, int minimumZ) {
        int maximumX() {
            return minimumX + DuneSimulation.REGION_BLOCK_SIZE - 1;
        }

        int maximumZ() {
            return minimumZ + DuneSimulation.REGION_BLOCK_SIZE - 1;
        }

        String description() {
            return "X=" + minimumX + ".." + maximumX()
                    + ", Z=" + minimumZ + ".." + maximumZ();
        }
    }
}
