package com.blackenter.minecraftdune.registry;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.world.level.block.DuneSandLayerBlock;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final int SAND_DUST_COLOR = 0xDBD3A0;

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MinecraftDune.MOD_ID);

    public static final DeferredBlock<ColoredFallingBlock> SAND = BLOCKS.register(
            "sand",
            () -> new ColoredFallingBlock(
                    new ColorRGBA(SAND_DUST_COLOR),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
            )
    );

    public static final DeferredBlock<DuneSandLayerBlock> SAND_LAYER = BLOCKS.register(
            "sand_layer",
            () -> new DuneSandLayerBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).noOcclusion()
            )
    );

    private ModBlocks() {
    }
}
