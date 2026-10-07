package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.PoisonedNeedleItem;

import com.geckolib.renderer.GeoItemRenderer;

public class PoisonedNeedleRenderer
        extends GeoItemRenderer<PoisonedNeedleItem> {

    public PoisonedNeedleRenderer() {

        /*
         * GeckoLib automatically resolves:
         *
         * geckolib/models/item/poisoned_needle.geo.json
         * textures/item/poisoned_needle.png
         */

        super(
                FoxsWeapons
                        .POISONED_NEEDLE
                        .get()
        );
    }
}