package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Operator-only native Arrakis terrain inspection and chunk-pregeneration commands.
 */
public final class MacroGeologyCommand {
    public static final int TEST_REGION_SIZE = 256;

    private MacroGeologyCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("geology")
                .executes(MacroGeologyCommand::info)
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
        return sendSample(source, x, z);
    }

    private static int sampleCoordinates(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");
        return sendSample(source, x, z);
    }

    private static int sendSample(
            CommandSourceStack source,
            double x,
            double z
    ) {
        long worldSeed = source.getLevel().getSeed();
        MacroGeologyField.Sample sample = MacroGeologyField.sample(
                worldSeed,
                x,
                z
        );
        NativeTransverseDuneField.Sample dune = NativeTransverseDuneField.sample(
                worldSeed,
                x,
                z,
                sample.duneSuitability()
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Arrakis terrain @ X=%.1f Z=%.1f: radius=%.1f, effective_radius=%.1f, "
                                + "province=%s, rock_surface_Y=%.1f.",
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
                        "Province weights: basin=%.2f, foreland=%.2f, massif=%.2f, "
                                + "faulted_margin=%.2f, broken_rock=%.2f, transition=%.2f, erg=%.2f.",
                        sample.centralBasinWeight(),
                        sample.innerForelandWeight(),
                        sample.massifWeight(),
                        sample.faultedMarginWeight(),
                        sample.brokenRockWeight(),
                        sample.sandRockTransitionWeight(),
                        sample.openErgWeight()
                )),
                false
        );
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Rock: +%.1f blocks, small=%.2f, mask=%.2f, fault=%.2f, sand_pass=%.2f, "
                                + "boundary_warp=%+.0f.",
                        sample.addedRockHeight(),
                        sample.smallFormationMask(),
                        sample.rockFormationMask(),
                        sample.faultCarveMask(),
                        sample.sandCorridorMask(),
                        sample.boundaryWarpBlocks()
                )),
                false
        );
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Native transverse dunes: suitability=%.2f, local_height=%.2f, "
                                + "spacing=%.0f, wind=%.0f deg, asymmetry=%.2f.",
                        sample.duneSuitability(),
                        dune.heightBlocks(),
                        NativeTransverseDuneField.DUNE_SPACING_BLOCKS,
                        NativeTransverseDuneField.WIND_ANGLE_DEGREES,
                        NativeTransverseDuneField.SLOPE_ASYMMETRY
                )),
                false
        );
        return 1;
    }

    private static int nativeClearMessage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal(
                "Macro geology and far-erg dunes are native chunk terrain in 0.5.9 and "
                        + "cannot be cleared independently. Use a fresh Arrakis Dev world, "
                        + "or delete/regenerate the affected world region files while the "
                        + "world is closed."
        ));
        return 0;
    }
}
