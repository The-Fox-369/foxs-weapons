package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.SlingPocketItem;

import com.geckolib.renderer.GeoItemRenderer;

public class SlingPocketRenderer
        extends GeoItemRenderer<SlingPocketItem> {

    public SlingPocketRenderer() {

        super(
                FoxsWeapons.SLING_POCKET.get()
        );
    }
}