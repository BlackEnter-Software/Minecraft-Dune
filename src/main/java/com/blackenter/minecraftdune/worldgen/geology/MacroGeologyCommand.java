package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisChunkGenerator;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import com.blackenter.minecraftdune.worldgen.dune.NativeTransverseDuneField;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;

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
                .then(Commands.literal("profile")
                        .executes(MacroGeologyCommand::profile))
                .then(Commands.literal("sample")
                        .then(Commands.argument(
                                        "x",
                                        IntegerArgumentType.integer(
                                                -30_000_000,
                                                30_000_000
                                        )
                                )
                                .then(Commands.argument(
                                                "z",
                                                IntegerArgumentType.integer(
                                                        -30_000_000,
                                                        30_000_000
                                                )
                                        )
                                        .executes(
                                                MacroGeologyCommand::sampleCoordinates
                                        ))))
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

    private static int info(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        return sendSample(
                source,
                source.getPosition().x,
                source.getPosition().z
        );
    }

    private static int sampleCoordinates(
            CommandContext<CommandSourceStack> context
    ) {
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
        ArrakisTerrainSettings settings =
                activeTerrainSettings(source);

        MacroGeologyField.Sample sample =
                MacroGeologyField.sample(
                        worldSeed,
                        x,
                        z,
                        settings
                );
        NativeTransverseDuneField.Sample dune =
                NativeTransverseDuneField.sample(
                        worldSeed,
                        x,
                        z,
                        sample.duneSuitability(),
                        settings.nativeDunes()
                );
        int originalRockTopY = (int) Math.floor(sample.baseElevation() + 0.5);
        LithologyField.Column lithologyColumn = LithologyField.column(
                worldSeed,
                x,
                z,
                settings.lithology()
        );
        LithologyField.Sample lithology = lithologyColumn.sample(originalRockTopY);
        MassifFractureField.Sample fracture = MassifFractureField.sample(
                worldSeed,
                x,
                z,
                originalRockTopY,
                sample,
                lithology.resistance(),
                settings.fractures()
        );
        int fractureRockTopY = originalRockTopY - Math.min(
                (int) Math.floor(fracture.carveDepth()),
                Math.max(
                        0,
                        originalRockTopY - (MacroGeologyField.BASE_SURFACE_Y + 1)
                )
        );
        LithologyBlockPalette palette = new LithologyBlockPalette(
                settings.lithology().materials()
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Arrakis terrain @ X=%.1f Z=%.1f: radius=%.1f, "
                                + "effective_radius=%.1f, province=%s, "
                                + "macro_rock_Y=%.1f, fissure_rock_Y=%d.",
                        x,
                        z,
                        sample.radiusBlocks(),
                        sample.effectiveRadiusBlocks(),
                        sample.dominantProvince().commandName(),
                        sample.baseElevation(),
                        fractureRockTopY
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Lithology: %s (%s, block=%s), limestone_host=%s, "
                                + "intrusive=%s, basalt_structure=%s, calcite_vein=%s.",
                        lithology.material().commandName(),
                        lithology.resistance().commandName(),
                        palette.resolvedId(lithology.material()),
                        lithology.limestoneHost(),
                        lithology.intrusive(),
                        lithology.basaltStructure(),
                        lithology.calciteVein()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Massif fissure: strength=%.2f, width=%.1f, carve_depth=%.1f, "
                                + "activation=%.2f, calcite_indicator=%.2f, mineralized=%s.",
                        fracture.strength(),
                        fracture.width(),
                        fracture.carveDepth(),
                        fracture.activation(),
                        fracture.mineralization(),
                        fracture.mineralized()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Province weights: basin=%.2f, foreland=%.2f, massif=%.2f, "
                                + "faulted_margin=%.2f, broken_rock=%.2f, "
                                + "transition=%.2f, erg=%.2f.",
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
                        "Rock: +%.1f blocks, small=%.2f, mask=%.2f, "
                                + "fault_depth=%.2f, fault_sand=%.2f, sand_pass=%.2f, "
                                + "boundary_warp=%+.0f.",
                        sample.addedRockHeight(),
                        sample.smallFormationMask(),
                        sample.rockFormationMask(),
                        sample.faultCarveMask(),
                        sample.faultSandFloorMask(),
                        sample.sandCorridorMask(),
                        sample.boundaryWarpBlocks()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Native transverse dunes: suitability=%.2f, local_height=%.2f, "
                                + "spacing=%.0f, wind=%.0f deg, asymmetry=%.2f. "
                                + "Profile=%d.",
                        sample.duneSuitability(),
                        dune.heightBlocks(),
                        settings.nativeDunes().spacing(),
                        settings.nativeDunes().windAngleDegrees(),
                        settings.nativeDunes().slopeAsymmetry(),
                        settings.profileVersion()
                )),
                false
        );

        return 1;
    }

    private static int profile(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ArrakisTerrainSettings settings =
                activeTerrainSettings(source);

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Arrakis terrain profile %d: basin=0-%.0f, foreland_end=%.0f, "
                                + "massif_outer=%.0f..%.0f.",
                        settings.profileVersion(),
                        settings.basin().pureSandRadius(),
                        settings.foreland().endRadius(),
                        settings.massif().outerStartRadius(),
                        settings.massif().outerEndRadius()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Foreland growth: inner_height_scale=%.2f, threshold_boost=%.2f, "
                                + "growth_power=%.2f. Broken-rock size_decay_power=%.2f.",
                        settings.foreland().innerHeightScale(),
                        settings.foreland().innerThresholdBoost(),
                        settings.foreland().growthPower(),
                        settings.brokenRock().sizeDecayPower()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Broken rock: %.0f..%.0f (fade after %.0f); transition=%.0f..%.0f; "
                                + "open_erg=%.0f..%.0f.",
                        settings.brokenRock().startRadius(),
                        settings.brokenRock().outerRadius(),
                        settings.brokenRock().outerFadeStartRadius(),
                        settings.outerTransition().startRadius(),
                        settings.outerTransition().outerRadius(),
                        settings.outerTransition().openErgStartRadius(),
                        settings.outerTransition().openErgFullRadius()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Faults: count=%d, width=%.0f..%.0f, rocky_floor=Y+%.1f, "
                                + "broad_warp=%.0f @ %.0f scale, medium_warp=%.0f @ %.0f scale.",
                        settings.faults().count(),
                        settings.faults().coreWidth(),
                        settings.faults().outerWidth(),
                        settings.faults().rockyFloorHeight(),
                        settings.faults().broadWarpStrength(),
                        settings.faults().broadWarpScale(),
                        settings.faults().mediumWarpStrength(),
                        settings.faults().mediumWarpScale()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Lithology: units=%.0f x %.0f scale, strata=%.0f, "
                                + "intrusions >= %.2f, limestone <= -%.2f, "
                                + "blackstone >= %.2f; dikes=%.1f half-width / %.0f spacing.",
                        settings.lithology().unitHorizontalScale(),
                        settings.lithology().unitVerticalScale(),
                        settings.lithology().strataThickness(),
                        settings.lithology().intrusionThreshold(),
                        settings.lithology().limestoneThreshold(),
                        settings.lithology().blackstoneThreshold(),
                        settings.lithology().dikeHalfWidth(),
                        settings.lithology().dikeSpacing()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Fissures: enabled=%s, trace_spacing=%.0f, active_lines=%.2f, "
                                + "branch_length=%.0f..%.0f, width=%.1f..%.1f, depth=%.0f..%.0f, "
                                + "branch=%.2f, mineralized=%.2f.",
                        settings.fractures().enabled(),
                        settings.fractures().cellSize(),
                        settings.fractures().density(),
                        settings.fractures().minimumLength(),
                        settings.fractures().maximumLength(),
                        settings.fractures().minimumWidth(),
                        settings.fractures().maximumWidth(),
                        settings.fractures().minimumDepth(),
                        settings.fractures().maximumDepth(),
                        settings.fractures().branchChance(),
                        settings.fractures().mineralizationChance()
                )),
                false
        );

        LithologyBlockPalette profilePalette = new LithologyBlockPalette(
                settings.lithology().materials()
        );
        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Material resolution: limestone=%s (configured %s; fallback %s), "
                                + "talus=%s; local_scree=%s (0.5.14 hook).",
                        profilePalette.resolvedId(LithologyField.Material.LIMESTONE),
                        settings.lithology().materials().limestone(),
                        settings.lithology().materials().limestoneFallback(),
                        profilePalette.resolvedId(LithologyField.Material.GRAVEL),
                        settings.lithology().talus().localScreeEnabled()
                )),
                false
        );

        source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "Native dunes: spacing=%.0f, max_height=%.0f, variation=%.2f, "
                                + "sharpness=%.2f, cutoff=%.2f, asymmetry=%.2f, wind=%.0f deg; "
                                + "weights foreland=%.2f broken=%.2f transition=%.2f.",
                        settings.nativeDunes().spacing(),
                        settings.nativeDunes().maxHeight(),
                        settings.nativeDunes().spacingVariation(),
                        settings.nativeDunes().ridgeSharpness(),
                        settings.nativeDunes().valleyCutoff(),
                        settings.nativeDunes().slopeAsymmetry(),
                        settings.nativeDunes().windAngleDegrees(),
                        settings.nativeDunes().forelandWeight(),
                        settings.nativeDunes().brokenRockWeight(),
                        settings.nativeDunes().transitionWeight()
                )),
                false
        );

        return 1;
    }

    private static ArrakisTerrainSettings activeTerrainSettings(
            CommandSourceStack source
    ) {
        ChunkGenerator generator =
                source.getLevel().getChunkSource().getGenerator();

        if (generator instanceof ArrakisChunkGenerator arrakis) {
            return arrakis.terrainSettings();
        }

        return ArrakisTerrainSettings.DEFAULT;
    }

    private static int nativeClearMessage(
            CommandContext<CommandSourceStack> context
    ) {
        context.getSource().sendFailure(Component.literal(
                "Macro geology, lithology, fissures and native dunes are chunk terrain in "
                        + "0.5.13 and cannot "
                        + "be cleared independently. Use a fresh Arrakis Dev world, or "
                        + "delete/regenerate affected region files while the world is closed."
        ));
        return 0;
    }
}
