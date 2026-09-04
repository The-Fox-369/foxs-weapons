package com.fox.foxsweapons.client.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * All custom third-person/player weapon poses and attack animations.
 */
public final class WeaponPlayerAnimations {

    public static final EnumProxy<HumanoidModel.ArmPose> CUSTOM_WEAPON_POSE =
            pose(WeaponPlayerAnimations::applyVolcanoHammerPose);

    public static final EnumProxy<HumanoidModel.ArmPose> BLUNDERBUSS_POSE =
            pose(WeaponPlayerAnimations::applyBlunderbussPose);

    public static final EnumProxy<HumanoidModel.ArmPose> SOUL_REAPER_POSE =
            pose(WeaponPlayerAnimations::applySoulReaperPose);

    public static final EnumProxy<HumanoidModel.ArmPose> SLING_POCKET_POSE =
            pose(WeaponPlayerAnimations::applySlingPocketPose);


    // =========================================================
    // SOUL REAPER KEYFRAMES
    // =========================================================

    /*
     * These are player-arm poses, NOT item-model rotations.
     *
     * The item itself is positioned by soul_reaper.json.
     */

    private static final SoulPose SOUL_IDLE = new SoulPose(
            -1.05F, -0.32F, 0.10F,
            -1.18F, 0.58F, -0.18F,
            0.00F, -0.10F, 0.00F
    );

    /*
     * Pull the weapon behind the body.
     */
    private static final SoulPose SOUL_WINDUP = new SoulPose(
            -1.55F, -0.85F, 0.30F,
            -1.40F, 0.88F, -0.24F,
            -0.06F, -0.28F, 0.10F
    );

    /*
     * Main cutting frame.
     *
     * Main arm travels across the body.
     */
    private static final SoulPose SOUL_REAP = new SoulPose(
            -0.62F, 0.72F, -0.20F,
            -0.78F, -0.06F, 0.12F,
            0.07F, 0.28F, -0.12F
    );

    /*
     * Let the scythe continue past the target instead of
     * instantly snapping back after impact.
     */
    private static final SoulPose SOUL_FOLLOW_THROUGH = new SoulPose(
            -0.78F, 0.92F, -0.12F,
            -0.95F, -0.22F, 0.10F,
            0.04F, 0.18F, -0.08F
    );


    private WeaponPlayerAnimations() {
    }


    private static EnumProxy<HumanoidModel.ArmPose> pose(
            IArmPoseTransformer transformer
    ) {
        return new EnumProxy<>(
                HumanoidModel.ArmPose.class,
                true,
                true,
                transformer
        );
    }


    // =========================================================
    // VOLCANO HAMMER
    // =========================================================

    private static void applyVolcanoHammerPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {
        float t = attackTime(state);

        ModelPart attackArm =
                model.getArm(arm);

        ModelPart supportArm =
                model.getArm(arm.getOpposite());

        float side =
                side(arm);


        // -----------------------------------------------------
        // IDLE
        // -----------------------------------------------------

        if (t <= 0.001F) {

            attackArm.xRot =
                    attackArm.xRot * 0.5F
                            - (float) Math.PI / 10.0F;

            attackArm.yRot =
                    0.0F;

            return;
        }


        float attackBaseX =
                attackArm.xRot;

        float attackBaseY =
                attackArm.yRot;

        float attackBaseZ =
                attackArm.zRot;

        float supportBaseX =
                supportArm.xRot;

        float supportBaseY =
                supportArm.yRot;

        float supportBaseZ =
                supportArm.zRot;


        // -----------------------------------------------------
        // WINDUP
        // -----------------------------------------------------

        if (t < 0.35F) {

            float p =
                    smooth(
                            t / 0.35F
                    );

            setArm(
                    attackArm,

                    Mth.lerp(
                            p,
                            attackBaseX,
                            -2.70F
                    ),

                    Mth.lerp(
                            p,
                            attackBaseY,
                            -0.22F * side
                    ),

                    Mth.lerp(
                            p,
                            attackBaseZ,
                            0.12F * side
                    )
            );

            setArm(
                    supportArm,

                    Mth.lerp(
                            p,
                            supportBaseX,
                            -2.55F
                    ),

                    Mth.lerp(
                            p,
                            supportBaseY,
                            0.82F * side
                    ),

                    Mth.lerp(
                            p,
                            supportBaseZ,
                            -0.28F * side
                    )
            );

            model.body.xRot +=
                    -0.22F * p;

            model.body.yRot +=
                    -0.20F * side * p;

            model.head.xRot +=
                    -0.10F * p;

            model.rightLeg.xRot +=
                    0.08F * p;

            model.leftLeg.xRot +=
                    -0.05F * p;

            return;
        }


        // -----------------------------------------------------
        // SLAM
        // -----------------------------------------------------

        if (t < 0.62F) {

            float p =
                    smooth(
                            (t - 0.35F) / 0.27F
                    );

            setArm(
                    attackArm,

                    Mth.lerp(
                            p,
                            -2.70F,
                            -0.48F
                    ),

                    Mth.lerp(
                            p,
                            -0.22F * side,
                            0.08F * side
                    ),

                    Mth.lerp(
                            p,
                            0.12F * side,
                            -0.06F * side
                    )
            );

            setArm(
                    supportArm,

                    Mth.lerp(
                            p,
                            -2.55F,
                            -0.72F
                    ),

                    Mth.lerp(
                            p,
                            0.82F * side,
                            0.52F * side
                    ),

                    Mth.lerp(
                            p,
                            -0.28F * side,
                            -0.12F * side
                    )
            );

            model.body.xRot +=
                    Mth.lerp(
                            p,
                            -0.22F,
                            0.55F
                    );

            model.body.yRot +=
                    Mth.lerp(
                            p,
                            -0.20F * side,
                            0.12F * side
                    );

            model.head.xRot +=
                    Mth.lerp(
                            p,
                            -0.10F,
                            0.25F
                    );

            model.rightLeg.xRot +=
                    Mth.lerp(
                            p,
                            0.08F,
                            0.18F
                    );

            model.leftLeg.xRot +=
                    Mth.lerp(
                            p,
                            -0.05F,
                            0.10F
                    );

            return;
        }


        // -----------------------------------------------------
        // RECOVERY
        // -----------------------------------------------------

        float p =
                smooth(
                        (t - 0.62F) / 0.38F
                );

        float idleAttackX =
                attackBaseX * 0.5F
                        - (float) Math.PI / 10.0F;

        setArm(
                attackArm,

                Mth.lerp(
                        p,
                        -0.48F,
                        idleAttackX
                ),

                Mth.lerp(
                        p,
                        0.08F * side,
                        0.0F
                ),

                Mth.lerp(
                        p,
                        -0.06F * side,
                        attackBaseZ
                )
        );

        setArm(
                supportArm,

                Mth.lerp(
                        p,
                        -0.72F,
                        supportBaseX
                ),

                Mth.lerp(
                        p,
                        0.52F * side,
                        supportBaseY
                ),

                Mth.lerp(
                        p,
                        -0.12F * side,
                        supportBaseZ
                )
        );

        model.body.xRot +=
                Mth.lerp(
                        p,
                        0.55F,
                        0.0F
                );

        model.body.yRot +=
                Mth.lerp(
                        p,
                        0.12F * side,
                        0.0F
                );

        model.head.xRot +=
                Mth.lerp(
                        p,
                        0.25F,
                        0.0F
                );

        model.rightLeg.xRot +=
                Mth.lerp(
                        p,
                        0.18F,
                        0.0F
                );

        model.leftLeg.xRot +=
                Mth.lerp(
                        p,
                        0.10F,
                        0.0F
                );
    }


    // =========================================================
    // BLUNDERBUSS
    // =========================================================

    private static void applyBlunderbussPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {
        ModelPart triggerArm =
                model.getArm(arm);

        ModelPart supportArm =
                model.getArm(arm.getOpposite());

        float side =
                side(arm);


        // -----------------------------------------------------
        // IDLE AIM
        // -----------------------------------------------------

        setArm(
                triggerArm,
                -1.18F,
                -0.12F * side,
                0.05F * side
        );

        setArm(
                supportArm,
                -1.32F,
                0.72F * side,
                -0.18F * side
        );

        model.body.yRot +=
                -0.10F * side;


        // -----------------------------------------------------
        // RECOIL
        // -----------------------------------------------------

        float t =
                attackTime(state);

        if (t <= 0.001F) {
            return;
        }


        float recoil;

        if (t < 0.18F) {

            recoil =
                    smooth(
                            t / 0.18F
                    );

        } else if (t < 0.62F) {

            recoil =
                    1.0F
                            - smooth(
                            (t - 0.18F) / 0.44F
                    );

        } else {

            recoil =
                    0.0F;
        }


        triggerArm.xRot -=
                0.30F * recoil;

        triggerArm.yRot -=
                0.03F * side * recoil;


        supportArm.xRot -=
                0.34F * recoil;

        supportArm.yRot +=
                0.08F * side * recoil;

        supportArm.zRot -=
                0.03F * side * recoil;


        model.body.xRot -=
                0.16F * recoil;

        model.body.yRot -=
                0.04F * side * recoil;

        model.head.xRot +=
                0.07F * recoil;

        model.rightLeg.xRot +=
                0.035F * recoil;

        model.leftLeg.xRot -=
                0.025F * recoil;
    }


    // =========================================================
    // SLING POCKET
    // =========================================================
    //
    // Main hand holds the launcher forward.
    //
    // Support hand reaches toward the centre,
    // as though the player has just pulled/released
    // the elastic sling.
    //
    // NO charging animation.
    // NO recoil animation.
    // NO swing animation.
    //
    // Rock simply goes BONK.
    // =========================================================

    private static void applySlingPocketPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {

        ModelPart launcherArm =
                model.getArm(arm);

        ModelPart supportArm =
                model.getArm(
                        arm.getOpposite()
                );

        float side =
                side(arm);


        // -----------------------------------------------------
        // LAUNCHER ARM
        //
        // Mostly straight forward.
        // -----------------------------------------------------

        setArm(
                launcherArm,

                -1.48F,

                -0.10F * side,

                0.04F * side
        );


        // -----------------------------------------------------
        // SUPPORT ARM
        //
        // Pulled inward toward the sling.
        // -----------------------------------------------------

        setArm(
                supportArm,

                -1.20F,

                0.48F * side,

                -0.20F * side
        );


        // -----------------------------------------------------
        // BODY
        //
        // Small sideways turn toward the weapon.
        // -----------------------------------------------------

        model.body.yRot +=
                -0.07F * side;


        // Tiny head compensation.
        model.head.yRot +=
                0.03F * side;
    }


    // =========================================================
    // SOUL REAPER
    // =========================================================

    private static void applySoulReaperPose(
            HumanoidModel<?> model,
            HumanoidRenderState state,
            HumanoidArm arm
    ) {
        ModelPart attackArm =
                model.getArm(arm);

        ModelPart supportArm =
                model.getArm(arm.getOpposite());

        float side =
                side(arm);

        float t =
                attackTime(state);


        // -----------------------------------------------------
        // IDLE
        // -----------------------------------------------------

        if (t <= 0.001F) {

            applySoulPose(
                    model,
                    attackArm,
                    supportArm,
                    side,
                    SOUL_IDLE
            );

            return;
        }


        // -----------------------------------------------------
        // WINDUP
        //
        // 0% -> 18%
        //
        // Pull the scythe backward before the cut.
        // -----------------------------------------------------

        if (t < 0.18F) {

            float p =
                    segment(
                            t,
                            0.00F,
                            0.18F
                    );

            applySoulPose(
                    model,
                    attackArm,
                    supportArm,
                    side,

                    lerp(
                            SOUL_IDLE,
                            SOUL_WINDUP,
                            p
                    )
            );

            return;
        }


        // -----------------------------------------------------
        // REAP
        //
        // 18% -> 48%
        //
        // Fastest and largest part of the animation.
        // -----------------------------------------------------

        if (t < 0.48F) {

            float p =
                    fastSegment(
                            t,
                            0.18F,
                            0.48F
                    );

            applySoulPose(
                    model,
                    attackArm,
                    supportArm,
                    side,

                    lerp(
                            SOUL_WINDUP,
                            SOUL_REAP,
                            p
                    )
            );

            return;
        }


        // -----------------------------------------------------
        // FOLLOW THROUGH
        //
        // 48% -> 68%
        //
        // Weapon continues after hitting instead of snapping.
        // -----------------------------------------------------

        if (t < 0.68F) {

            float p =
                    segment(
                            t,
                            0.48F,
                            0.68F
                    );

            applySoulPose(
                    model,
                    attackArm,
                    supportArm,
                    side,

                    lerp(
                            SOUL_REAP,
                            SOUL_FOLLOW_THROUGH,
                            p
                    )
            );

            return;
        }


        // -----------------------------------------------------
        // RECOVERY
        //
        // 68% -> 100%
        //
        // Smoothly return exactly to idle.
        // -----------------------------------------------------

        float p =
                segment(
                        t,
                        0.68F,
                        1.00F
                );

        applySoulPose(
                model,
                attackArm,
                supportArm,
                side,

                lerp(
                        SOUL_FOLLOW_THROUGH,
                        SOUL_IDLE,
                        p
                )
        );
    }


    // =========================================================
    // SOUL REAPER POSE HELPERS
    // =========================================================

    private static void applySoulPose(
            HumanoidModel<?> model,
            ModelPart attackArm,
            ModelPart supportArm,
            float side,
            SoulPose pose
    ) {
        setArm(
                attackArm,
                pose.attackX(),
                pose.attackY() * side,
                pose.attackZ() * side
        );

        setArm(
                supportArm,
                pose.supportX(),
                pose.supportY() * side,
                pose.supportZ() * side
        );

        model.body.xRot +=
                pose.bodyX();

        model.body.yRot +=
                pose.bodyY() * side;

        model.head.yRot +=
                pose.headY() * side;
    }


    private static SoulPose lerp(
            SoulPose from,
            SoulPose to,
            float progress
    ) {
        return new SoulPose(
                Mth.lerp(
                        progress,
                        from.attackX(),
                        to.attackX()
                ),

                Mth.lerp(
                        progress,
                        from.attackY(),
                        to.attackY()
                ),

                Mth.lerp(
                        progress,
                        from.attackZ(),
                        to.attackZ()
                ),

                Mth.lerp(
                        progress,
                        from.supportX(),
                        to.supportX()
                ),

                Mth.lerp(
                        progress,
                        from.supportY(),
                        to.supportY()
                ),

                Mth.lerp(
                        progress,
                        from.supportZ(),
                        to.supportZ()
                ),

                Mth.lerp(
                        progress,
                        from.bodyX(),
                        to.bodyX()
                ),

                Mth.lerp(
                        progress,
                        from.bodyY(),
                        to.bodyY()
                ),

                Mth.lerp(
                        progress,
                        from.headY(),
                        to.headY()
                )
        );
    }


    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private static void setArm(
            ModelPart arm,
            float x,
            float y,
            float z
    ) {
        arm.xRot = x;
        arm.yRot = y;
        arm.zRot = z;
    }


    private static float attackTime(
            HumanoidRenderState state
    ) {
        return Mth.clamp(
                state.attackTime,
                0.0F,
                1.0F
        );
    }


    private static float side(
            HumanoidArm arm
    ) {
        return arm == HumanoidArm.RIGHT
                ? 1.0F
                : -1.0F;
    }


    private static float segment(
            float value,
            float start,
            float end
    ) {
        return smooth(
                (value - start)
                        / (end - start)
        );
    }


    /*
     * Faster acceleration during the actual Soul Reaper cut.
     */
    private static float fastSegment(
            float value,
            float start,
            float end
    ) {
        float p =
                Mth.clamp(
                        (value - start)
                                / (end - start),
                        0.0F,
                        1.0F
                );

        float inverse =
                1.0F - p;

        return 1.0F
                - inverse
                * inverse
                * inverse;
    }


    private static float smooth(
            float value
    ) {
        value =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return value
                * value
                * (
                3.0F
                        - 2.0F * value
        );
    }


    // =========================================================
    // SOUL REAPER POSE DATA
    // =========================================================

    private record SoulPose(
            float attackX,
            float attackY,
            float attackZ,

            float supportX,
            float supportY,
            float supportZ,

            float bodyX,
            float bodyY,
            float headY
    ) {
    }
}