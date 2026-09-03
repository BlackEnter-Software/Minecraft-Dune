package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.OrphanRemnantFilter;
import com.blackenter.minecraftdune.worldgen.geology.BasalSandSkirt;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Read-only inspection of the exact analytical evaluator used during generation. */
public final class ArrakisTerrainCommand {
    private ArrakisTerrainCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("terrain")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .executes(context -> inspect(context.getSource(),
                                BlockPos.containing(context.getSource().getPosition())))
                        .then(Commands.argument("position", BlockPosArgument.blockPos())
                                .executes(context -> inspect(context.getSource(),
                                        BlockPosArgument.getBlockPos(context, "position")))));
    }

    private static int inspect(CommandSourceStack source, BlockPos position) {
        var level = source.getLevel();
        if (!(level.getChunkSource().getGenerator() instanceof ArrakisChunkGenerator generator)) {
            source.sendFailure(Component.literal("Terrain inspection requires an Arrakis native generator."));
            return 0;
        }
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        if (Math.abs((long) x) > 30_000_000L || Math.abs((long) z) > 30_000_000L) {
            source.sendFailure(Component.literal("Inspection position is outside the world coordinate limit."));
            return 0;
        }
        var evaluator = new ArrakisTerrainEvaluator(level.getSeed(), generator.terrainSettings(),
                ArrakisTerrainEvaluator.QUERY_CACHE_LIMIT);
        String analytical = describe(evaluator, level.getSeed(), generator.terrainSettings(), x, y, z);
        // Never force-load a chunk for diagnostics. This is an observed, post-generation
        // hard layer, not a reconstruction of the flat substrate before native rock writes.
        var loaded = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        String observed = loaded == null ? "not loaded (no chunk requested)"
                : Integer.toString(ArrakisChunkGenerator.findFoundationTopY(
                        loaded, new BlockPos.MutableBlockPos(), x, z, level.getMinBuildHeight()));
        String report = analytical + "\nObserved hard layer at/below Y64: " + observed
                + "\nAnalytical prediction only; existing chunks/player edits may differ.";
        source.sendSuccess(() -> Component.literal(report), false);
        source.sendSuccess(() -> Component.literal("[Copy terrain report]")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, report))), false);
        return 1;
    }

    /** Shared with offline diagnostics; this formats results, never reimplements terrain math. */
    static String describe(ArrakisTerrainEvaluator evaluator, long seed,
            ArrakisTerrainSettings settings, int x, int y, int z) {
        var c = evaluator.column(x, z);
        var g = c.geology();
        var face = c.face();
        var major = c.erosion();
        var surface = c.surfaceErosion();
        var apron = c.basalTalusApron();
        var structural = c.basal().structural();
        var contact = c.basal().actual();
        var orphan = settings.erosion().orphanRemnants();
        var component = evaluator.componentCleanup(x, z);
        var skirt = c.skirt();
        int filteredTop = evaluator.highestFilteredRockY(x, z);
        var material = c.materialSampleAt(y);
        String surfaceMaterial = filteredTop <= 64 ? "none"
                : c.materialSampleAt(filteredTop).material().name();
        return String.format(Locale.ROOT,
                "Arrakis seed=%d profile=%d XYZ=%d/%d/%d base-anchored=%s%n"
                + "%s radius=%.2f effective=%.2f massif=%.3f physical=%.3f rock-mask=%.3f%n"
                + "Macro elevation=%.2f added=%.2f corridor=%.3f fault=%.3f sand-floor=%.3f configured-rocky-floor=%.2f%n"
                + "Rock tops: original=%d fissure=%d pre-filter=%d filtered=%d%n"
                + "Major: candidate=%s strength=%.3f relief=%.2f retreat=%.2f%n"
                + "Surface: active=%s retreat=%d face-strength=%.3f; fracture activation=%.3f carve=%.2f%n"
                + "Face: exposed=%s high-side=%s Y=%d..%d relief=%.2f inset=%.2f normal=(%.3f,%.3f)%n"
                + "Lithology at Y%d: %s/%s; filtered surface=%s%n"
                + "Orphan: enabled=%s inward=%d lateral=%d protects-through-Y=%d min-relief=%.1f; raw=%s kept=%s%n"
                + "Component cleanup: candidate=%s removed=%s search-radius=%d component-columns=%d reaches-support=%s reaches-search-boundary=%s reason=%s fault-edge-enabled=%s%n"
                + "Structural: valid=%s signed=%.2f inward-normal=(%.3f,%.3f)%n"
                + "Actual contact: enabled=%s searched=%d found=%s signed=%.2f X/Z=%d/%d rock-top=%d reason=%s source=%s%n"
                + "Wall: top=%d relief=%d probe-blocks=%d query-band=Y%d..%d%n"
                + "Basal talus: active=%s height=%d outward=%.2f spread=%.2f; local talus=%d blocks from Y%d%n"
                + "At queried Y: basal-material=%s local-talus=%s%n"
                + "Basal erosion: floor=Y%d depth=%d native-root-at-Y64=%s rock-at-Y65=%s; organic-talus=%s%n"
                + "Sand skirt: active=%s actual-contact-distance=%.2f inward-overlap=%d outward-reach=%.2f local-depth=%d visible-Y65-mantle=%s material-at-query=%s%n"
                + "Pre-skirt ownership: Y64=%s Y65=%s queried-Y=%s%n"
                + "Dune units=%d dune-top=%d combined analytical top=%d cached-columns=%d",
                seed, settings.profileVersion(), x, y, z, surface.settings().baseAnchoredErosion(),
                g.dominantProvince().commandName(), g.radiusBlocks(), g.effectiveRadiusBlocks(),
                g.massifWeight(), g.physicalMassifWeight(), g.rockFormationMask(),
                g.baseElevation(), g.addedRockHeight(), g.sandCorridorMask(), g.faultCarveMask(),
                g.faultSandFloorMask(), settings.faults().rockyFloorHeight(),
                c.originalRockTopY(), c.fissureRockTopY(), c.rockTopY(), filteredTop,
                major.candidate(), major.escarpmentStrength(), major.localRelief(), major.maximumRetreat(),
                surface.active(), surface.maximumRetreat(), surface.faceErosionStrength(),
                c.fracture().activation(), c.fracture().carveDepth(),
                face.exposed(), face.highSide(), face.lowY(), face.highY(), face.localRelief(),
                face.faceInset(), face.outwardNormalX(), face.outwardNormalZ(),
                y, material.material(), material.resistance(), surfaceMaterial,
                orphan.enabled(), orphan.inwardSupportDepth(), orphan.lateralSearchRadius(),
                OrphanRemnantFilter.protectedThroughY(surface.settings().baseAnchoredErosion(), orphan), orphan.minimumFaceRelief(),
                evaluator.rawRockOccupies(c, y), evaluator.rockOccupies(x, y, z),
                component.candidate(), component.removed(), orphan.componentSearchRadius(),
                component.componentColumns(), component.reachesSupport(), component.reachesSearchBoundary(), component.reason(),
                orphan.faultEdgeCleanupEnabled(),
                structural.valid(), structural.signedDistance(), structural.inwardX(), structural.inwardZ(),
                contact.enabled(), contact.searchedBlocks(), contact.found(), contact.signedDistance(),
                contact.x(), contact.z(), contact.rockTopY(), contact.reason(), c.basal().source(),
                contact.wallTopY(), contact.wallRelief(), contact.wallProbeBlocks(),
                evaluator.talusWallQueryMinY(), evaluator.talusWallQueryMaxY(),
                apron.active(), apron.height(), apron.outwardDistance(), apron.spread(),
                c.localTalusThickness(), c.talusBaseY(), evaluator.basalMaterialAt(x, y, z, c), c.talusOccupiesY(y),
                surface.settings().erosionFloorY(), surface.settings().basalErosionDepth(),
                evaluator.nativeFoundationOccupies(x,64,z), evaluator.rockOccupies(x,65,z),
                settings.lithology().talus().organicApronEnabled(),
                skirt.active(), skirt.signedDistance(), BasalSandSkirt.INWARD_OVERLAP, skirt.outwardReach(),
                skirt.depth(), skirt.visibleY65Mantle(), skirt.materialAt(y),
                evaluator.preSkirtOwner(x, 64, z), evaluator.preSkirtOwner(x, 65, z), evaluator.preSkirtOwner(x, y, z),
                c.duneSurfaceUnits(), c.highestDuneY(),
                evaluator.highestOccupiedY(x, z), evaluator.size()).replace("\r\n", "\n");
    }
}
