package com.blackenter.minecraftdune.client.renderer;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.client.model.MuaddibMouseModel;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class MuaddibMouseRenderer
        extends MobRenderer<MuaddibMouseEntity, MuaddibMouseModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    MinecraftDune.MOD_ID,
                    "textures/entity/muaddib_mouse.png"
            );

    public MuaddibMouseRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new MuaddibMouseModel(
                        context.bakeLayer(MuaddibMouseModel.LAYER_LOCATION)
                ),
                0.12F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(MuaddibMouseEntity entity) {
        return TEXTURE;
    }
}
