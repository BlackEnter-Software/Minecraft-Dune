package com.blackenter.minecraftdune.worldgen.prototype;

import com.blackenter.minecraftdune.MinecraftDune;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
 * Operator-only commands for generating, tuning, and clearing prototype dunes in Arrakis Dev.
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class DunePrototypeCommand {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;
    private static final int BLOCK_UPDATE_FLAGS = 2;
    private static final int FIRST_DUNE_Y = DuneSimulation.BASE_SURFACE_Y + 1;
    private static final int LAST_DUNE_Y = DuneSimulation.BASE_SURFACE_Y
            + DuneSimulation.Settings.MAXIMUM_ALLOWED_HEIGHT;

    private static DuneSimulation.Settings currentSettings = DuneSimulation.Settings.defaults();

    private DunePrototypeCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> duneRoot = dispatcher.register(
                Commands.literal("dune")
                        .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                        .then(Commands.literal("dunes")
                                .then(Commands.literal("generate")
                                        .then(Commands.literal(DuneMode.TRANSVERSE.commandName())
                                                .executes(context -> generate(context, DuneMode.TRANSVERSE)))
                                        .then(Commands.literal(DuneMode.BARCHAN.commandName())
                                                .executes(context -> generate(context, DuneMode.BARCHAN))))
                                .then(Commands.literal("clear")
                                        .executes(DunePrototypeCommand::clearCurrentRegion)
                                        .then(Commands.argument(
                                                        "cell_size",
                                                        IntegerArgumentType.integer(
                                                                DuneSimulation.Settings.MINIMUM_CELL_SIZE,
                                                                DuneSimulation.Settings.MAXIMUM_CELL_SIZE
                                                        )
                                                )
                                                .executes(DunePrototypeCommand::clearExplicitRegion)))
                                .then(Commands.literal("info")
                                        .executes(DunePrototypeCommand::info))
                                .then(Commands.literal("settings")
                                        .executes(DunePrototypeCommand::showSettings)
                                        .then(Commands.literal("reset")
                                                .executes(DunePrototypeCommand::resetSettings))
                                        .then(Commands.literal("cell_size")
                                                .then(Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        DuneSimulation.Settings.MINIMUM_CELL_SIZE,
                                                                        DuneSimulation.Settings.MAXIMUM_CELL_SIZE
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setCellSize)))
                                        .then(Commands.literal("max_height")
                                                .then(Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        0,
                                                                        DuneSimulation.Settings.MAXIMUM_ALLOWED_HEIGHT
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setMaximumHeight)))
                                        .then(Commands.literal("dune_spacing")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_DUNE_SPACING,
                                                                        DuneSimulation.Settings.MAXIMUM_DUNE_SPACING
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setDuneSpacing)))
                                        .then(Commands.literal("spacing_variation")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_SPACING_VARIATION,
                                                                        DuneSimulation.Settings.MAXIMUM_SPACING_VARIATION
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setSpacingVariation)))
                                        .then(Commands.literal("ridge_sharpness")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_RIDGE_SHARPNESS,
                                                                        DuneSimulation.Settings.MAXIMUM_RIDGE_SHARPNESS
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setRidgeSharpness)))
                                        .then(Commands.literal("valley_cutoff")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_VALLEY_CUTOFF,
                                                                        DuneSimulation.Settings.MAXIMUM_VALLEY_CUTOFF
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setValleyCutoff)))
                                        .then(Commands.literal("repose_angle")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_REPOSE_ANGLE,
                                                                        DuneSimulation.Settings.MAXIMUM_REPOSE_ANGLE
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setReposeAngle)))
                                        .then(Commands.literal("cascade_passes")
                                                .then(Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        0,
                                                                        DuneSimulation.Settings.MAXIMUM_CASCADE_PASSES
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setCascadePasses)))
                                        .then(Commands.literal("iterations")
                                                .then(Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        0,
                                                                        DuneSimulation.Settings.MAXIMUM_TRANSPORT_ITERATIONS
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setTransportIterations)))
                                        .then(Commands.literal("wind_angle")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(-360.0, 360.0)
                                                        )
                                                        .executes(DunePrototypeCommand::setWindAngle)))
                                        .then(Commands.literal("edge_blend")
                                                .then(Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        0,
                                                                        DuneSimulation.Settings.MAXIMUM_EDGE_BLEND_CELLS
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setEdgeBlend)))
                                        .then(Commands.literal("transport_strength")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_TRANSPORT_STRENGTH,
                                                                        DuneSimulation.Settings.MAXIMUM_TRANSPORT_STRENGTH
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setTransportStrength)))))
        );

        dispatcher.register(
                Commands.literal("minecraftdune")
                        .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                        .redirect(duneRoot)
        );
    }

    private static int generate(
            CommandContext<CommandSourceStack> context,
            DuneMode mode
    ) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        DuneSimulation.Settings settings = currentSettings;
        Region region = regionAt(source, settings.regionBlockSize());
        long seed = regionalSeed(level.getSeed(), region.minimumX(), region.minimumZ(), mode);

        source.sendSuccess(
                () -> Component.literal(
                        "Generating " + mode.commandName() + " dunes in " + region.description()
                                + ". This development command may pause the server briefly."
                ),
                false
        );

        long startNanoseconds = System.nanoTime();
        DuneSimulation.Result result = DuneSimulation.simulate(mode, seed, settings);
        int changedBlocks = applyHeightField(level, region, result);
        double elapsedSeconds = (System.nanoTime() - startNanoseconds) / 1_000_000_000.0;

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Generated %s dunes: %dx%d blocks, %d changed blocks, maximum +%d Y, %.3f sand-mass drift, %.2f s. Seed %s.",
                        mode.commandName(),
                        settings.regionBlockSize(),
                        settings.regionBlockSize(),
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

    private static int clearCurrentRegion(CommandContext<CommandSourceStack> context) {
        return clear(context, currentSettings.cellSize());
    }

    private static int clearExplicitRegion(CommandContext<CommandSourceStack> context) {
        int cellSize = IntegerArgumentType.getInteger(context, "cell_size");
        return clear(context, cellSize);
    }

    private static int clear(CommandContext<CommandSourceStack> context, int cellSize) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int regionBlockSize = DuneSimulation.GRID_SIZE * cellSize;
        Region region = regionAt(source, regionBlockSize);
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
        DuneSimulation.Settings settings = currentSettings;
        Region region = regionAt(source, settings.regionBlockSize());

        source.sendSuccess(
                () -> Component.literal(
                        "Arrakis Dev dune region: " + region.description()
                                + ", base surface Y=" + DuneSimulation.BASE_SURFACE_Y
                                + ", wind=" + formatDouble(settings.windAngleDegrees()) + " degrees."
                ),
                false
        );
        return showSettings(context);
    }

    private static int showSettings(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DuneSimulation.Settings settings = currentSettings;
        String maximumHeight = settings.maximumHeightOverride() == 0
                ? "mode default (transverse=" + DuneMode.TRANSVERSE.maximumHeight()
                        + ", barchan=" + DuneMode.BARCHAN.maximumHeight() + ")"
                : Integer.toString(settings.maximumHeightOverride());
        String iterations = settings.transportIterationsOverride() == 0
                ? "mode default (transverse=" + DuneMode.TRANSVERSE.transportIterations()
                        + ", barchan=" + DuneMode.BARCHAN.transportIterations() + ")"
                : Integer.toString(settings.transportIterationsOverride());

        source.sendSuccess(
                () -> Component.literal(
                        "Dune settings: cell_size=" + settings.cellSize()
                                + " -> region=" + settings.regionBlockSize() + "x"
                                + settings.regionBlockSize()
                                + ", max_height=" + maximumHeight
                                + ", dune_spacing=" + formatDouble(settings.duneSpacingBlocks()) + "."
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Transverse shape: spacing_variation=" + formatDouble(settings.spacingVariation())
                                + ", ridge_sharpness=" + formatDouble(settings.ridgeSharpness())
                                + ", valley_cutoff=" + formatDouble(settings.valleyCutoff()) + "."
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Slope/transport: repose_angle=" + formatDouble(settings.reposeAngleDegrees())
                                + " deg, cascade_passes=" + settings.cascadePasses()
                                + ", iterations=" + iterations
                                + ", transport_strength=" + formatDouble(settings.transportStrength()) + "."
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "Wind/boundary: wind_angle=" + formatDouble(settings.windAngleDegrees())
                                + " deg, edge_blend=" + settings.edgeBlendCells() + "."
                ),
                false
        );
        return 1;
    }

    private static int resetSettings(CommandContext<CommandSourceStack> context) {
        currentSettings = DuneSimulation.Settings.defaults();
        context.getSource().sendSuccess(
                () -> Component.literal("Reset Arrakis Dev dune settings to prototype defaults."),
                false
        );
        return showSettings(context);
    }

    private static int setCellSize(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        currentSettings = currentSettings.withCellSize(value);
        return settingChanged(context, "cell_size", Integer.toString(value));
    }

    private static int setMaximumHeight(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        currentSettings = currentSettings.withMaximumHeightOverride(value);
        return settingChanged(context, "max_height", value == 0 ? "mode default" : Integer.toString(value));
    }

    private static int setDuneSpacing(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withDuneSpacingBlocks(value);
        return settingChanged(context, "dune_spacing", formatDouble(value));
    }

    private static int setSpacingVariation(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withSpacingVariation(value);
        return settingChanged(context, "spacing_variation", formatDouble(value));
    }

    private static int setRidgeSharpness(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withRidgeSharpness(value);
        return settingChanged(context, "ridge_sharpness", formatDouble(value));
    }

    private static int setValleyCutoff(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withValleyCutoff(value);
        return settingChanged(context, "valley_cutoff", formatDouble(value));
    }

    private static int setReposeAngle(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withReposeAngleDegrees(value);
        return settingChanged(context, "repose_angle", formatDouble(value));
    }

    private static int setCascadePasses(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        currentSettings = currentSettings.withCascadePasses(value);
        return settingChanged(context, "cascade_passes", Integer.toString(value));
    }

    private static int setTransportIterations(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        currentSettings = currentSettings.withTransportIterationsOverride(value);
        return settingChanged(context, "iterations", value == 0 ? "mode default" : Integer.toString(value));
    }

    private static int setWindAngle(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withWindAngleDegrees(value);
        return settingChanged(context, "wind_angle", formatDouble(currentSettings.windAngleDegrees()));
    }

    private static int setEdgeBlend(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        currentSettings = currentSettings.withEdgeBlendCells(value);
        return settingChanged(context, "edge_blend", Integer.toString(value));
    }

    private static int setTransportStrength(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        currentSettings = currentSettings.withTransportStrength(value);
        return settingChanged(context, "transport_strength", formatDouble(value));
    }

    private static int settingChanged(
            CommandContext<CommandSourceStack> context,
            String name,
            String value
    ) {
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Set dune " + name + "=" + value
                                + ". Regenerate a test region to see the change."
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
        int regionBlockSize = result.settings().regionBlockSize();

        for (int localZ = 0; localZ < regionBlockSize; localZ++) {
            int worldZ = region.minimumZ() + localZ;
            for (int localX = 0; localX < regionBlockSize; localX++) {
                int worldX = region.minimumX() + localX;
                int targetTopY = DuneSimulation.BASE_SURFACE_Y
                        + result.heightAt(localX, localZ);

                // Clear up to the hard prototype ceiling so lowering max_height immediately
                // removes peaks generated by a previous run in the same footprint.
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

    private static Region regionAt(CommandSourceStack source, int regionBlockSize) {
        int blockX = Mth.floor(source.getPosition().x);
        int blockZ = Mth.floor(source.getPosition().z);
        int minimumX = Math.floorDiv(blockX, regionBlockSize) * regionBlockSize;
        int minimumZ = Math.floorDiv(blockZ, regionBlockSize) * regionBlockSize;
        return new Region(minimumX, minimumZ, regionBlockSize);
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

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record Region(int minimumX, int minimumZ, int size) {
        int maximumX() {
            return minimumX + size - 1;
        }

        int maximumZ() {
            return minimumZ + size - 1;
        }

        String description() {
            return "X=" + minimumX + ".." + maximumX()
                    + ", Z=" + minimumZ + ".." + maximumZ()
                    + " (" + size + "x" + size + ")";
        }
    }
}
