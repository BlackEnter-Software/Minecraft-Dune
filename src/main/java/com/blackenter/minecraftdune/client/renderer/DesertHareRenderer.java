package com.blackenter.minecraftdune.client.renderer;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.client.model.DesertHareModel;
import com.blackenter.minecraftdune.entity.DesertHareEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class DesertHareRenderer
        extends MobRenderer<DesertHareEntity, DesertHareModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MinecraftDune.MOD_ID,
                    "textures/entity/desert_hare.png"
            );

    public DesertHareRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new DesertHareModel(
                        context.bakeLayer(DesertHareModel.LAYER_LOCATION)
                ),
                0.18F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(
            DesertHareEntity entity
    ) {
        return TEXTURE;
    }
}
