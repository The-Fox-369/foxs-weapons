package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.VolcanoHammerItem;
import com.geckolib.renderer.GeoItemRenderer;

public class VolcanoHammerRenderer extends GeoItemRenderer<VolcanoHammerItem> {
    public VolcanoHammerRenderer() {
        super(FoxsWeapons.VOLCANO_HAMMER.get());
    }
}
