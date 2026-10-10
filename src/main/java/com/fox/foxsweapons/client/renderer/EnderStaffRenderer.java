
package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.EnderStaffItem;

import com.geckolib.renderer.GeoItemRenderer;

public final class EnderStaffRenderer
        extends GeoItemRenderer<EnderStaffItem> {

    public EnderStaffRenderer() {
        super(FoxsWeapons.ENDER_STAFF.get());
    }
}
