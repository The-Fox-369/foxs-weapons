package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.WeightedNetItem;

import com.geckolib.renderer.GeoItemRenderer;

public class WeightedNetRenderer
        extends GeoItemRenderer<WeightedNetItem> {

    public WeightedNetRenderer() {
        super(
                FoxsWeapons.WEIGHTED_NET.get()
        );
    }
}