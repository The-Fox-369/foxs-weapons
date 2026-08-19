package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.network.BlunderbussNetwork;

import net.minecraft.client.Minecraft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID,
        value = Dist.CLIENT
)
public final class BlunderbussClientInput {

    private BlunderbussClientInput() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ItemStack stack =
                minecraft.player
                        .getMainHandItem();

        /*
         * Only hijack clicks while holding
         * the Blunderbuss.
         */
        if (!stack.is(
                FoxsWeapons.BLUNDERBUSS.get())) {

            return;
        }

        /*
         * =====================================================
         * LEFT CLICK
         * =====================================================
         *
         * Cancel Minecraft's normal attack.
         *
         * NO:
         * player punch
         * vanilla melee
         * vanilla swing
         *
         * Then ask the server for one pellet.
         */

        if (event.isAttack()) {

            event.setCanceled(true);

            event.setSwingHand(
                    false
            );

            /*
             * Don't spam packets while the
             * client already knows we're cooling down.
             */
            if (minecraft.player
                    .getCooldowns()
                    .isOnCooldown(stack)) {

                return;
            }

            ClientPacketDistributor
                    .sendToServer(
                            new BlunderbussNetwork
                                    .SingleFirePayload()
                    );

            return;
        }

        /*
         * =====================================================
         * RIGHT CLICK
         * =====================================================
         *
         * Right-click use events may be evaluated
         * for both hands.
         *
         * Cancel both, but only send ONE packet
         * from MAIN_HAND.
         */

        if (event.isUseItem()) {

            event.setCanceled(true);

            event.setSwingHand(
                    false
            );

            if (event.getHand()
                    != InteractionHand.MAIN_HAND) {

                return;
            }

            if (minecraft.player
                    .getCooldowns()
                    .isOnCooldown(stack)) {

                return;
            }

            ClientPacketDistributor
                    .sendToServer(
                            new BlunderbussNetwork
                                    .BurstFirePayload()
                    );
        }
    }
}