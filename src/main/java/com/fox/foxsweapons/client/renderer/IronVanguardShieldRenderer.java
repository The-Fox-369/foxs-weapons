package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.IronVanguardShieldItem;

import com.geckolib.renderer.GeoItemRenderer;

public class IronVanguardShieldRenderer
        extends GeoItemRenderer<IronVanguardShieldItem> {

    public IronVanguardShieldRenderer() {
        super(
                FoxsWeapons.IRON_VANGUARD_SHIELD.get()
        );
    }
}