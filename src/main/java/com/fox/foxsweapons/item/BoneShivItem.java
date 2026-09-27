package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.BoneShivRenderer;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;

import net.minecraft.core.component.DataComponents;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class BoneShivItem
        extends Item
        implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);


    public BoneShivItem(
            Properties properties
    ) {

        /*
         * Minecraft 26.2 uses the SWING_ANIMATION
         * data component to determine the type and
         * duration of the held-item attack animation.
         *
         * WHACK = normal vanilla melee swing.
         *
         * Six ticks is the vanilla-style default
         * swing duration.
         */

        super(
                properties.component(
                        DataComponents.SWING_ANIMATION,

                        new SwingAnimation(
                                SwingAnimationType.WHACK,
                                6
                        )
                )
        );


        GeoItem.registerSyncedAnimatable(this);
    }


    // =========================================================
    // GECKOLIB RENDERER
    // =========================================================

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {

        consumer.accept(
                new GeoRenderProvider() {

                    private BoneShivRenderer renderer;


                    @Override
                    public GeoItemRenderer<BoneShivItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {

                            this.renderer =
                                    new BoneShivRenderer();
                        }

                        return this.renderer;
                    }
                }
        );
    }


    // =========================================================
    // TOOLTIP
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {

        tooltipAdder.accept(
                Component.translatable(
                        "tooltip.foxsweapons.bone_shiv.description"
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );


        tooltipAdder.accept(
                Component.translatable(
                        "tooltip.foxsweapons.bone_shiv.left_attack"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );


        tooltipAdder.accept(
                Component.translatable(
                        "tooltip.foxsweapons.bone_shiv.right_attack"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );


        super.appendHoverText(
                stack,
                context,
                display,
                tooltipAdder,
                flag
        );
    }


    // =========================================================
    // GECKOLIB CONTROLLER
    // =========================================================

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {

        controllers.add(
                new AnimationController<>(
                        "controller",
                        0,
                        state -> PlayState.STOP
                )
        );
    }


    // =========================================================
    // DUAL-WIELD CHECK
    // =========================================================

    public static boolean isDualWielding(
            Entity entity
    ) {

        if (!(entity instanceof ServerPlayer player)) {

            return false;
        }


        return player
                .getMainHandItem()
                .is(FoxsWeapons.BONE_SHIV.get())

                && player
                .getOffhandItem()
                .is(FoxsWeapons.BONE_SHIV.get());
    }


    // =========================================================
    // PHYSICAL HAND LOOKUP
    // =========================================================

    private static InteractionHand getPhysicalHand(
            ServerPlayer player,
            boolean physicalLeft
    ) {

        HumanoidArm wantedArm =
                physicalLeft
                        ? HumanoidArm.LEFT
                        : HumanoidArm.RIGHT;


        return player.getMainArm()
                == wantedArm

                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
    }


    // =========================================================
    // DUAL SHIV ATTACK
    // =========================================================

    public static void attack(
            ServerPlayer player,
            int targetEntityId,
            boolean physicalLeft
    ) {

        // =====================================================
        // SERVER VALIDATION
        // =====================================================

        if (!isDualWielding(player)) {

            return;
        }


        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }


        Entity target =
                level.getEntity(
                        targetEntityId
                );


        if (target == null) {

            return;
        }


        if (target == player) {

            return;
        }


        if (!target.isAlive()) {

            return;
        }


        if (!target.isPickable()) {

            return;
        }


        // =====================================================
        // DETERMINE PHYSICAL ATTACK HAND
        // =====================================================

        InteractionHand attackHand =
                getPhysicalHand(
                        player,
                        physicalLeft
                );


        ItemStack attackStack =
                player.getItemInHand(
                        attackHand
                );


        if (!attackStack.is(
                FoxsWeapons.BONE_SHIV.get()
        )) {

            return;
        }


        // =====================================================
        // ATTACK RANGE
        // =====================================================

        if (!player.isWithinAttackRange(
                attackStack,
                target.getBoundingBox(),
                0.0D
        )) {

            return;
        }


        // =====================================================
        // LINE OF SIGHT
        // =====================================================

        if (!player.hasLineOfSight(
                target
        )) {

            return;
        }


        // =====================================================
        // MAIN-HAND ATTACK
        // =====================================================

        if (attackHand
                == InteractionHand.MAIN_HAND) {

            player.attack(
                    target
            );

            return;
        }


        // =====================================================
        // OFF-HAND ATTACK
        // =====================================================
        //
        // Player.attack() calculates the weapon from
        // MAIN_HAND.
        //
        // Temporarily swap the stacks server-side,
        // perform the vanilla attack using the actual
        // off-hand Shiv, then restore them.
        // =====================================================

        ItemStack originalMain =
                player.getMainHandItem();

        ItemStack originalOff =
                player.getOffhandItem();


        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                originalOff
        );


        player.setItemInHand(
                InteractionHand.OFF_HAND,
                originalMain
        );


        try {

            player.attack(
                    target
            );

        } finally {

            ItemStack attackedShiv =
                    player.getMainHandItem();

            ItemStack restingShiv =
                    player.getOffhandItem();


            player.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    restingShiv
            );


            player.setItemInHand(
                    InteractionHand.OFF_HAND,
                    attackedShiv
            );
        }
    }


    // =========================================================
    // GECKOLIB CACHE
    // =========================================================

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return this.cache;
    }
}