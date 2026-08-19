package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.SoulReaperItem;

import com.geckolib.renderer.GeoItemRenderer;

public class SoulReaperRenderer
        extends GeoItemRenderer<SoulReaperItem> {

    public SoulReaperRenderer() {

        /*
         * GeckoLib 5 automatically resolves:
         *
         * geckolib/models/item/soul_reaper.geo.json
         * textures/item/soul_reaper.png
         *
         * from the registered item ID.
         */

        super(
                FoxsWeapons.SOUL_REAPER.get()
        );
    }
}