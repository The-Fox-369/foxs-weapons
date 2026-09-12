package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.WitheringKatanaItem;

import com.geckolib.renderer.GeoItemRenderer;

public class WitheringKatanaRenderer
        extends GeoItemRenderer<WitheringKatanaItem> {

    public WitheringKatanaRenderer() {

        /*
         * GeckoLib automatically resolves:
         *
         * geckolib/models/item/withering_katana.geo.json
         * textures/item/withering_katana.png
         */
        super(
                FoxsWeapons.WITHERING_KATANA.get()
        );
    }
}