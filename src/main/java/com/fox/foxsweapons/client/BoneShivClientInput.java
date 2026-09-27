package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.animation.WeaponPlayerAnimations;
import com.fox.foxsweapons.network.BoneShivNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.HumanoidArm;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID,
        value = Dist.CLIENT
)
public final class BoneShivClientInput {

    private BoneShivClientInput() {
    }


    @SubscribeEvent
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();


        LocalPlayer player =
                minecraft.player;


        if (player == null) {
            return;
        }


        ItemStack mainHand =
                player.getMainHandItem();


        ItemStack offHand =
                player.getOffhandItem();


        // =====================================================
        // DUAL SHIV MODE ONLY
        // =====================================================

        if (!mainHand.is(
                FoxsWeapons.BONE_SHIV.get()
        )) {
            return;
        }


        if (!offHand.is(
                FoxsWeapons.BONE_SHIV.get()
        )) {
            return;
        }


        // =====================================================
        // ENTITY TARGET ONLY
        // =====================================================

        if (!(minecraft.hitResult
                instanceof EntityHitResult entityHit)) {
            return;
        }


        if (minecraft.hitResult.getType()
                != HitResult.Type.ENTITY) {
            return;
        }


        int targetEntityId =
                entityHit
                        .getEntity()
                        .getId();


        // =====================================================
        // LEFT CLICK
        // =====================================================
        //
        // LEFT CLICK =
        // PHYSICAL LEFT ARM
        // =====================================================

        if (event.isAttack()) {

            event.setCanceled(
                    true
            );


            /*
             * Do not allow Minecraft to make its own
             * MAIN_HAND swing.
             *
             * We animate the PHYSICAL arm ourselves.
             */
            event.setSwingHand(
                    false
            );


            WeaponPlayerAnimations
                    .startBoneShivSwing(
                            HumanoidArm.LEFT
                    );


            ClientPacketDistributor
                    .sendToServer(
                            new BoneShivNetwork
                                    .AttackPayload(
                                    targetEntityId,
                                    true
                            )
                    );


            return;
        }


        // =====================================================
        // RIGHT CLICK
        // =====================================================
        //
        // RIGHT CLICK =
        // PHYSICAL RIGHT ARM
        // =====================================================

        if (event.isUseItem()) {

            event.setCanceled(
                    true
            );


            event.setSwingHand(
                    false
            );


            /*
             * Use-item input can be checked for both
             * inventory hands.
             *
             * Only process the first/main-hand event so
             * one click produces one attack.
             */
            if (event.getHand()
                    != InteractionHand.MAIN_HAND) {
                return;
            }


            WeaponPlayerAnimations
                    .startBoneShivSwing(
                            HumanoidArm.RIGHT
                    );


            ClientPacketDistributor
                    .sendToServer(
                            new BoneShivNetwork
                                    .AttackPayload(
                                    targetEntityId,
                                    false
                            )
                    );
        }
    }
}