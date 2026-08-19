package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.animation.WeaponPlayerAnimations;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID,
        value = Dist.CLIENT
)
public final class WeaponClientExtensions {

    private WeaponClientExtensions() {}

    @SubscribeEvent
    public static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {
        event.registerItem(
                pose(WeaponPlayerAnimations.CUSTOM_WEAPON_POSE),
                FoxsWeapons.VOLCANO_HAMMER.get()
        );

        event.registerItem(
                pose(WeaponPlayerAnimations.BLUNDERBUSS_POSE),
                FoxsWeapons.BLUNDERBUSS.get()
        );

        event.registerItem(
                pose(WeaponPlayerAnimations.SOUL_REAPER_POSE),
                FoxsWeapons.SOUL_REAPER.get()
        );
    }

    private static IClientItemExtensions pose(
            EnumProxy<HumanoidModel.ArmPose> pose
    ) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(
                    LivingEntity entity,
                    InteractionHand hand,
                    ItemStack stack
            ) {
                return hand == InteractionHand.MAIN_HAND
                        ? pose.getValue()
                        : null;
            }
        };
    }
}