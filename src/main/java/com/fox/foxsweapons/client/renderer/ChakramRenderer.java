package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.ChakramItem;

import com.geckolib.renderer.GeoItemRenderer;

public class ChakramRenderer
        extends GeoItemRenderer<ChakramItem> {

    public ChakramRenderer() {

        /*
         * GeckoLib automatically resolves:
         *
         * geckolib/models/item/chakram.geo.json
         * textures/item/chakram.png
         */
        super(
                FoxsWeapons.CHAKRAM.get()
        );
    }
}