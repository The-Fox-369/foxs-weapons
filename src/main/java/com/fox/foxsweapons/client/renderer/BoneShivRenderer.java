package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.BoneShivItem;
import com.geckolib.renderer.GeoItemRenderer;

public class BoneShivRenderer
        extends GeoItemRenderer<BoneShivItem> {

    public BoneShivRenderer() {
        super(
                FoxsWeapons.BONE_SHIV.get()
        );
    }
}