package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves serialized material identifiers without linking Minecraft: Dune to optional mods. */
public final class LithologyBlockPalette {
    private final Block background;
    private final Block sandstone;
    private final Block tuff;
    private final Block limestone;
    private final Block calcite;
    private final Block andesite;
    private final Block diorite;
    private final Block basalt;
    private final Block blackstone;
    private final Block talus;

    public LithologyBlockPalette(ArrakisTerrainSettings.MaterialPaletteSettings settings) {
        background = resolve(settings.background(), Blocks.STONE);
        sandstone = resolve(settings.sandstone(), Blocks.SANDSTONE);
        tuff = resolve(settings.tuff(), Blocks.TUFF);
        Block limestoneFallback = resolve(settings.limestoneFallback(), Blocks.SANDSTONE);
        limestone = resolve(settings.limestone(), limestoneFallback);
        calcite = resolve(settings.calcite(), Blocks.CALCITE);
        andesite = resolve(settings.andesite(), Blocks.ANDESITE);
        diorite = resolve(settings.diorite(), Blocks.DIORITE);
        basalt = resolve(settings.basalt(), Blocks.BASALT);
        blackstone = resolve(settings.blackstone(), Blocks.BLACKSTONE);
        talus = resolve(settings.talus(), Blocks.GRAVEL);
    }

    public BlockState state(LithologyField.Material material) {
        return block(material).defaultBlockState();
    }

    public String resolvedId(LithologyField.Material material) {
        return BuiltInRegistries.BLOCK.getKey(block(material)).toString();
    }

    private Block block(LithologyField.Material material) {
        return switch (material) {
            case STONE -> background;
            case SANDSTONE -> sandstone;
            case TUFF -> tuff;
            case LIMESTONE -> limestone;
            case CALCITE -> calcite;
            case ANDESITE -> andesite;
            case DIORITE -> diorite;
            case BASALT -> basalt;
            case BLACKSTONE -> blackstone;
            case GRAVEL -> talus;
        };
    }

    private static Block resolve(String identifier, Block fallback) {
        ResourceLocation location = ResourceLocation.tryParse(identifier);
        if (location == null) {
            return fallback;
        }
        return BuiltInRegistries.BLOCK.getOptional(location).orElse(fallback);
    }
}
