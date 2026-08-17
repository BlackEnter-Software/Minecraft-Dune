package com.blackenter.minecraftdune.worldgen.geology;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Operator-only macro-geology inspection and native Arrakis chunk-pregeneration commands.
 *
 * <p>Since 0.5.8 geology is part of Arrakis chunk generation itself. These commands no
 * longer place or remove cliff blocks after a chunk has been generated.</p>
 */
public final class MacroGeologyCommand {
    public static final int TEST_REGION_SIZE = 256;

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
                        .executes(context ->
                                MacroGeologyGenerationManager.startCurrentTile(
                                        context.getSource()
                                )))
                .then(Commands.literal("generate_initial")
                        .executes(context ->
                                MacroGeologyGenerationManager.startInitial(
                                        context.getSource()
                                )))
                .then(Commands.literal("generate_nearest")
                        .then(Commands.argument(
                                        "radius",
                                        IntegerArgumentType.integer(1, 12)
                                )
                                .executes(context ->
                                        MacroGeologyGenerationManager.startNearest(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(
                                                        context,
                                                        "radius"
                                                )
                                        ))))
                .then(Commands.literal("generation")
                        .then(Commands.literal("status")
                                .executes(context ->
                                        MacroGeologyGenerationManager.status(
                                                context.getSource()
                                        )))
                        .then(Commands.literal("cancel")
                                .executes(context ->
                                        MacroGeologyGenerationManager.cancel(
                                                context.getSource()
                                        ))))
                .then(Commands.literal("clear")
                        .executes(MacroGeologyCommand::nativeClearMessage));
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

    private static int nativeClearMessage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal(
                "In 0.5.8 macro geology is native chunk terrain and cannot be cleared "
                        + "independently. Use a fresh Arrakis Dev world, or delete/regenerate "
                        + "the affected world region files while the world is closed."
        ));
        return 0;
    }
}
