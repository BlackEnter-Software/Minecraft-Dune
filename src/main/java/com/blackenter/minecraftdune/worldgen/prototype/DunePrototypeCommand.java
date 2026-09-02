package com.blackenter.minecraftdune.worldgen.prototype;

import com.blackenter.minecraftdune.MinecraftDune;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Operator-only commands for generating, tuning, and clearing prototype dunes in Arrakis Dev.
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class DunePrototypeCommand {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

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
                                .then(Commands.literal("operation")
                                        .then(Commands.literal("status")
                                                .executes(context -> DunePrototypeOperationManager.status(
                                                        context.getSource()
                                                )))
                                        .then(Commands.literal("cancel")
                                                .executes(context -> DunePrototypeOperationManager.cancel(
                                                        context.getSource()
                                                ))))
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
                                        .then(Commands.literal("surface_resolution")
                                                .then(Commands.literal(DuneSurfaceResolution.WHOLE.commandName())
                                                        .executes(context -> setSurfaceResolution(
                                                                context,
                                                                DuneSurfaceResolution.WHOLE
                                                        )))
                                                .then(Commands.literal(DuneSurfaceResolution.EIGHTH.commandName())
                                                        .executes(context -> setSurfaceResolution(
                                                                context,
                                                                DuneSurfaceResolution.EIGHTH
                                                        )))
                                                .then(Commands.literal(DuneSurfaceResolution.SIXTEENTH.commandName())
                                                        .executes(context -> setSurfaceResolution(
                                                                context,
                                                                DuneSurfaceResolution.SIXTEENTH
                                                        ))))
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
                                        .then(Commands.literal("slope_asymmetry")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_SLOPE_ASYMMETRY,
                                                                        DuneSimulation.Settings.MAXIMUM_SLOPE_ASYMMETRY
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setSlopeAsymmetry)))
                                        .then(Commands.literal("interdune_cleanup")
                                                .then(Commands.argument(
                                                                "value",
                                                                DoubleArgumentType.doubleArg(
                                                                        DuneSimulation.Settings.MINIMUM_INTERDUNE_CLEANUP,
                                                                        DuneSimulation.Settings.MAXIMUM_INTERDUNE_CLEANUP
                                                                )
                                                        )
                                                        .executes(DunePrototypeCommand::setInterduneCleanup)))                                        .then(Commands.literal("repose_angle")
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
        DunePrototypeState state = DunePrototypeState.get(level);
        DuneSimulation.Settings settings = state.settings();
        Region region = regionAt(source, settings.regionBlockSize());
        long seed = regionalSeed(level.getSeed(), region.minimumX(), region.minimumZ(), mode);

        return DunePrototypeOperationManager.startGenerate(
                source,
                mode,
                seed,
                settings,
                state.surfaceResolution(),
                region.minimumX(),
                region.minimumZ()
        );
    }

    private static int clearCurrentRegion(CommandContext<CommandSourceStack> context) {
        return clear(context, state(context).settings().cellSize());
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
        return DunePrototypeOperationManager.startClear(
                source,
                region.minimumX(),
                region.minimumZ(),
                regionBlockSize
        );
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DuneSimulation.Settings settings = state(context).settings();
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
        DunePrototypeState state = state(context);
        DuneSimulation.Settings settings = state.settings();
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
                                + ", surface_resolution=" + state.surfaceResolution().commandName()
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
                        "Transverse morphology: slope_asymmetry=" + formatDouble(settings.slopeAsymmetry())
                                + ", interdune_cleanup=" + formatDouble(settings.interduneCleanup()) + "."
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
        DunePrototypeState state = state(context);
        state.settings(DuneSimulation.Settings.defaults());
        state.surfaceResolution(DuneSurfaceResolution.SIXTEENTH);
        context.getSource().sendSuccess(
                () -> Component.literal("Reset Arrakis Dev dune settings to prototype defaults."),
                false
        );
        return showSettings(context);
    }

    private static int setCellSize(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        updateSettings(context, settings -> settings.withCellSize(value));
        return settingChanged(context, "cell_size", Integer.toString(value));
    }

    private static int setMaximumHeight(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        updateSettings(context, settings -> settings.withMaximumHeightOverride(value));
        return settingChanged(context, "max_height", value == 0 ? "mode default" : Integer.toString(value));
    }

    private static int setSurfaceResolution(
            CommandContext<CommandSourceStack> context,
            DuneSurfaceResolution surfaceResolution
    ) {
        state(context).surfaceResolution(surfaceResolution);
        return settingChanged(context, "surface_resolution", surfaceResolution.commandName());
    }

    private static int setDuneSpacing(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withDuneSpacingBlocks(value));
        return settingChanged(context, "dune_spacing", formatDouble(value));
    }

    private static int setSpacingVariation(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withSpacingVariation(value));
        return settingChanged(context, "spacing_variation", formatDouble(value));
    }

    private static int setRidgeSharpness(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withRidgeSharpness(value));
        return settingChanged(context, "ridge_sharpness", formatDouble(value));
    }

    private static int setValleyCutoff(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withValleyCutoff(value));
        return settingChanged(context, "valley_cutoff", formatDouble(value));
    }

    private static int setSlopeAsymmetry(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withSlopeAsymmetry(value));
        return settingChanged(context, "slope_asymmetry", formatDouble(value));
    }

    private static int setInterduneCleanup(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withInterduneCleanup(value));
        return settingChanged(context, "interdune_cleanup", formatDouble(value));
    }
    private static int setReposeAngle(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withReposeAngleDegrees(value));
        return settingChanged(context, "repose_angle", formatDouble(value));
    }

    private static int setCascadePasses(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        updateSettings(context, settings -> settings.withCascadePasses(value));
        return settingChanged(context, "cascade_passes", Integer.toString(value));
    }

    private static int setTransportIterations(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        updateSettings(context, settings -> settings.withTransportIterationsOverride(value));
        return settingChanged(context, "iterations", value == 0 ? "mode default" : Integer.toString(value));
    }

    private static int setWindAngle(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        DuneSimulation.Settings settings = updateSettings(
                context,
                current -> current.withWindAngleDegrees(value)
        );
        return settingChanged(context, "wind_angle", formatDouble(settings.windAngleDegrees()));
    }

    private static int setEdgeBlend(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        updateSettings(context, settings -> settings.withEdgeBlendCells(value));
        return settingChanged(context, "edge_blend", Integer.toString(value));
    }

    private static int setTransportStrength(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        updateSettings(context, settings -> settings.withTransportStrength(value));
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

    private static DunePrototypeState state(CommandContext<CommandSourceStack> context) {
        return DunePrototypeState.get(context.getSource().getLevel());
    }

    private static DuneSimulation.Settings updateSettings(
            CommandContext<CommandSourceStack> context,
            UnaryOperator<DuneSimulation.Settings> update
    ) {
        DunePrototypeState state = state(context);
        DuneSimulation.Settings settings = update.apply(state.settings());
        state.settings(settings);
        return settings;
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
