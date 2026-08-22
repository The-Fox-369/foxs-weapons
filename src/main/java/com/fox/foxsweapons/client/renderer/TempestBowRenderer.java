package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.TempestBowItem;

import com.geckolib.renderer.GeoItemRenderer;

public class TempestBowRenderer
        extends GeoItemRenderer<TempestBowItem> {

    public TempestBowRenderer() {
        super(
                FoxsWeapons
                        .TEMPEST_BOW
                        .get()
        );
    }
}