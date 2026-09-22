package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.HeavyGreatswordItem;

import com.geckolib.renderer.GeoItemRenderer;

public class HeavyGreatswordRenderer
        extends GeoItemRenderer<HeavyGreatswordItem> {

    public HeavyGreatswordRenderer() {

        /*
         * GeckoLib automatically resolves:
         *
         * geckolib/models/item/heavy_greatsword.geo.json
         * textures/item/heavy_greatsword.png
         */
        super(
                FoxsWeapons.HEAVY_GREATSWORD.get()
        );
    }
}