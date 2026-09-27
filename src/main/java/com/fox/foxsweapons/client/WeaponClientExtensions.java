package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.animation.WeaponPlayerAnimations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.util.Mth;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.HumanoidArm;
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

    private WeaponClientExtensions() {
    }


    @SubscribeEvent
    public static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {

        // =====================================================
        // VOLCANO HAMMER
        // =====================================================

        event.registerItem(
                pose(
                        WeaponPlayerAnimations
                                .CUSTOM_WEAPON_POSE
                ),

                FoxsWeapons
                        .VOLCANO_HAMMER
                        .get()
        );


        // =====================================================
        // BLUNDERBUSS
        // =====================================================

        event.registerItem(
                pose(
                        WeaponPlayerAnimations
                                .BLUNDERBUSS_POSE
                ),

                FoxsWeapons
                        .BLUNDERBUSS
                        .get()
        );


        // =====================================================
        // SOUL REAPER
        // =====================================================

        event.registerItem(
                pose(
                        WeaponPlayerAnimations
                                .SOUL_REAPER_POSE
                ),

                FoxsWeapons
                        .SOUL_REAPER
                        .get()
        );


        // =====================================================
        // SLING POCKET
        // =====================================================

        event.registerItem(
                pose(
                        WeaponPlayerAnimations
                                .SLING_POCKET_POSE
                ),

                FoxsWeapons
                        .SLING_POCKET
                        .get()
        );


        // =====================================================
        // BONE SHIV
        // =====================================================

        event.registerItem(
                boneShivExtensions(),

                FoxsWeapons
                        .BONE_SHIV
                        .get()
        );
    }


    // =========================================================
    // NORMAL POSE HELPER
    // =========================================================

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

                return hand
                        == InteractionHand.MAIN_HAND

                        ? pose.getValue()

                        : null;
            }
        };
    }


    // =========================================================
    // BONE SHIV CLIENT EXTENSIONS
    // =========================================================

    private static IClientItemExtensions
    boneShivExtensions() {

        return new IClientItemExtensions() {

            // -------------------------------------------------
            // THIRD-PERSON ARM POSE
            // -------------------------------------------------

            @Override
            public HumanoidModel.ArmPose getArmPose(
                    LivingEntity entity,
                    InteractionHand hand,
                    ItemStack stack
            ) {

                /*
                 * The special dual-shiv pose only exists
                 * while BOTH hands actually contain a Shiv.
                 */

                if (!entity
                        .getMainHandItem()
                        .is(FoxsWeapons.BONE_SHIV.get())) {

                    return null;
                }


                if (!entity
                        .getOffhandItem()
                        .is(FoxsWeapons.BONE_SHIV.get())) {

                    return null;
                }


                /*
                 * BONE_SHIV_POSE affects both arms,
                 * therefore it only needs to be returned
                 * from MAIN_HAND.
                 */

                return hand
                        == InteractionHand.MAIN_HAND

                        ? WeaponPlayerAnimations
                        .BONE_SHIV_POSE
                        .getValue()

                        : null;
            }


            // -------------------------------------------------
            // FIRST-PERSON VISUAL
            // -------------------------------------------------
            //
            // NeoForge calls this immediately before applying
            // the normal first-person hand/item transforms.
            //
            // Returning true tells Minecraft:
            //
            // "We handled the hand transform ourselves."
            //
            // This completely avoids relying on the vanilla
            // LocalPlayer swing state.
            // -------------------------------------------------

            @Override
            public boolean applyForgeHandTransform(
                    PoseStack poseStack,
                    LocalPlayer player,
                    HumanoidArm arm,
                    ItemStack itemInHand,
                    float partialTick,
                    float equipProcess,
                    float swingProcess
            ) {

                float attack =
                        WeaponPlayerAnimations
                                .getBoneShivSwingProgress(
                                        arm
                                );


                int invert =
                        arm == HumanoidArm.RIGHT
                                ? 1
                                : -1;


                // =============================================
                // VANILLA BASE HAND POSITION
                // =============================================

                poseStack.translate(
                        invert * 0.56F,

                        -0.52F
                                + equipProcess
                                * -0.6F,

                        -0.72F
                );


                // =============================================
                // NO ACTIVE BONK
                // =============================================

                if (attack <= 0.0F) {

                    return true;
                }


                // =============================================
                // VANILLA WHACK POSITION
                // =============================================

                float sqrtAttack =
                        Mth.sqrt(
                                attack
                        );


                float xSwingPosition =
                        -0.4F
                                * Mth.sin(
                                sqrtAttack
                                        * (float) Math.PI
                        );


                float ySwingPosition =
                        0.2F
                                * Mth.sin(
                                sqrtAttack
                                        * (float) Math.PI
                                        * 2.0F
                        );


                float zSwingPosition =
                        -0.2F
                                * Mth.sin(
                                attack
                                        * (float) Math.PI
                        );


                poseStack.translate(
                        invert
                                * xSwingPosition,

                        ySwingPosition,

                        zSwingPosition
                );


                // =============================================
                // VANILLA WHACK ROTATION
                // =============================================

                float ySwingRotation =
                        Mth.sin(
                                attack
                                        * attack
                                        * (float) Math.PI
                        );


                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                invert
                                        * (
                                        45.0F
                                                + ySwingRotation
                                                * -20.0F
                                )
                        )
                );


                float xzSwingRotation =
                        Mth.sin(
                                sqrtAttack
                                        * (float) Math.PI
                        );


                poseStack.mulPose(
                        Axis.ZP.rotationDegrees(
                                invert
                                        * xzSwingRotation
                                        * -20.0F
                        )
                );


                poseStack.mulPose(
                        Axis.XP.rotationDegrees(
                                xzSwingRotation
                                        * -80.0F
                        )
                );


                poseStack.mulPose(
                        Axis.YP.rotationDegrees(
                                invert
                                        * -45.0F
                        )
                );


                return true;
            }
        };
    }
}