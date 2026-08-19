package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.BlunderbussItem;

import com.geckolib.renderer.GeoItemRenderer;

public class BlunderbussRenderer
        extends GeoItemRenderer<BlunderbussItem> {

    public BlunderbussRenderer() {

        super(
                FoxsWeapons
                        .BLUNDERBUSS
                        .get()
        );
    }
}