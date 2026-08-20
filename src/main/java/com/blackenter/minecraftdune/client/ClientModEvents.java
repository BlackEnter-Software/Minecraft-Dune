package com.blackenter.minecraftdune.client;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.client.model.DesertHareModel;
import com.blackenter.minecraftdune.client.model.MuaddibMouseModel;
import com.blackenter.minecraftdune.client.renderer.DesertHareRenderer;
import com.blackenter.minecraftdune.client.renderer.MuaddibMouseRenderer;
import com.blackenter.minecraftdune.registry.ModEntityTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = MinecraftDune.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        event.registerLayerDefinition(
                DesertHareModel.LAYER_LOCATION,
                DesertHareModel::createBodyLayer
        );
        event.registerLayerDefinition(
                MuaddibMouseModel.LAYER_LOCATION,
                MuaddibMouseModel::createBodyLayer
        );
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntityTypes.DESERT_HARE.get(),
                DesertHareRenderer::new
        );
        event.registerEntityRenderer(
                ModEntityTypes.MUADDIB_MOUSE.get(),
                MuaddibMouseRenderer::new
        );
    }
}
