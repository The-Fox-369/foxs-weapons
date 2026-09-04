package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID,
        value = Dist.CLIENT
)
public final class SlingPocketClient {

    private SlingPocketClient() {
    }


    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {

        event.registerEntityRenderer(
                FoxsWeapons
                        .SLING_STONE_PROJECTILE
                        .get(),

                ThrownItemRenderer::new
        );
    }
}