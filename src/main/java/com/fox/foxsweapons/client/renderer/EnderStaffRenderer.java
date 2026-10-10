package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.EnderStaffItem;
import com.geckolib.renderer.GeoItemRenderer;

/**
 * Resolves existing ender_staff.geo.json and ender_staff.png via GeckoLib.
 */
public final class EnderStaffRenderer extends GeoItemRenderer<EnderStaffItem> {
    public EnderStaffRenderer() {
        super(FoxsWeapons.ENDER_STAFF.get());
    }
}
