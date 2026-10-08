package com.fox.foxsweapons.client.renderer;

import com.fox.foxsweapons.entity.PoisonedNeedleProjectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.minecraft.world.item.ItemDisplayContext;


public class PoisonedNeedleProjectileRenderer
        extends EntityRenderer<
        PoisonedNeedleProjectile,
        PoisonedNeedleProjectileRenderer.State
        > {

    private final ItemModelResolver itemModelResolver;


    public PoisonedNeedleProjectileRenderer(
            EntityRendererProvider.Context context
    ) {

        super(
                context
        );


        itemModelResolver =
                context.getItemModelResolver();


        /*
         * Tiny projectile.
         *
         * No giant circular entity shadow required.
         */
        shadowRadius =
                0.0F;
    }


    // =========================================================
    // SUBMIT
    // =========================================================

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {

        poseStack.pushPose();


        /*
         * IMPORTANT:
         *
         * NO camera.orientation.
         *
         * ThrownItemRenderer uses the CAMERA rotation,
         * which is why every embedded Needle turned whenever
         * you moved your head.
         *
         * We rotate from the PROJECTILE'S actual xRot/yRot.
         */


        /*
         * The Blockbench Needle is built lengthwise along
         * its local +Y axis.
         *
         * These two rotations point that +Y axis along the
         * projectile's flight direction.
         */
        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        state.yRot
                )
        );


        poseStack.mulPose(
                Axis.XP.rotationDegrees(
                        90.0F
                                - state.xRot
                )
        );


        state.item.submit(
                poseStack,

                submitNodeCollector,

                state.lightCoords,

                OverlayTexture.NO_OVERLAY,

                state.outlineColor
        );


        poseStack.popPose();


        super.submit(
                state,
                poseStack,
                submitNodeCollector,
                camera
        );
    }


    // =========================================================
    // RENDER STATE
    // =========================================================

    @Override
    public State createRenderState() {

        return new State();
    }


    @Override
    public void extractRenderState(
            PoisonedNeedleProjectile entity,
            State state,
            float partialTicks
    ) {

        super.extractRenderState(
                entity,
                state,
                partialTicks
        );


        /*
         * These rotations stop changing once the projectile
         * embeds because the projectile itself stops moving.
         *
         * Therefore the impact angle naturally becomes the
         * permanent embedded angle.
         */
        state.xRot =
                entity.getXRot(
                        partialTicks
                );


        state.yRot =
                entity.getYRot(
                        partialTicks
                );


        itemModelResolver.updateForNonLiving(
                state.item,

                entity.getItem(),

                ItemDisplayContext.GROUND,

                entity
        );
    }


    // =========================================================
    // STATE
    // =========================================================

    public static class State
            extends EntityRenderState {

        public final ItemStackRenderState item =
                new ItemStackRenderState();


        public float xRot =
                0.0F;


        public float yRot =
                0.0F;
    }
}