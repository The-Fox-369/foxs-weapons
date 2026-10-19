package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.SmokeBombItem;

import com.geckolib.renderer.GeoItemRenderer;

public final class SmokeBombRenderer
        extends GeoItemRenderer<SmokeBombItem> {

    public SmokeBombRenderer() {
        super(FoxsWeapons.SMOKE_BOMB.get());
    }
}
