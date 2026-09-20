package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.SpikedClubItem;

import com.geckolib.renderer.GeoItemRenderer;

public class SpikedClubRenderer
        extends GeoItemRenderer<SpikedClubItem> {

    public SpikedClubRenderer() {

        /*
         * GeckoLib automatically resolves:
         *
         * geckolib/models/item/spiked_club.geo.json
         * textures/item/spiked_club.png
         */
        super(
                FoxsWeapons.SPIKED_CLUB.get()
        );
    }
}