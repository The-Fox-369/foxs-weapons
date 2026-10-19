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
public final class SmokeBombClient {

    private SmokeBombClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                FoxsWeapons.SMOKE_BOMB_PROJECTILE.get(),
                ThrownItemRenderer::new
        );
    }
}
