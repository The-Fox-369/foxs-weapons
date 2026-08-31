package com.fox.foxsweapons.mechanic;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public final class WeightedNetTetherManager {

    private static final Map<UUID, UUID> TETHERS =
            new HashMap<>();

    private static final DustParticleOptions ROPE_PARTICLE =
            new DustParticleOptions(
                    0x8B5A2B,
                    0.55F
            );

    private WeightedNetTetherManager() {
    }


    // =========================================================
    // ATTACH
    // =========================================================

    public static void attach(
            ServerPlayer owner,
            LivingEntity target
    ) {

        if (target == owner || !target.isAlive()) {
            return;
        }

        UUID oldTarget =
                TETHERS.get(owner.getUUID());

        if (oldTarget != null
                && !oldTarget.equals(target.getUUID())) {

            release(owner, true);
        }

        TETHERS.put(
                owner.getUUID(),
                target.getUUID()
        );

        ServerLevel level =
                (ServerLevel) owner.level();

        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.LEAD_TIED,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }


    // =========================================================
    // RELEASE
    // =========================================================

    public static boolean release(
            ServerPlayer owner,
            boolean applyRopeBurns
    ) {

        UUID targetId =
                TETHERS.remove(owner.getUUID());

        if (targetId == null) {
            return false;
        }

        LivingEntity target =
                resolveTarget(
                        owner,
                        targetId
                );

        if (target != null && applyRopeBurns) {

            target.addEffect(
                    new MobEffectInstance(
                            FoxsWeapons.ROPE_BURNS,
                            WeaponStats.ROPE_BURNS_DURATION_TICKS,
                            0,
                            false,
                            true,
                            false
                    )
            );
        }

        ServerLevel level =
                (ServerLevel) owner.level();

        level.playSound(
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                SoundEvents.LEAD_UNTIED,
                SoundSource.PLAYERS,
                0.9F,
                1.0F
        );

        return true;
    }


    // =========================================================
    // CHECK
    // =========================================================

    public static boolean isTethering(
            ServerPlayer owner
    ) {

        return TETHERS.containsKey(
                owner.getUUID()
        );
    }


    // =========================================================
    // TETHER TICK
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {

        if (!(event.getEntity()
                instanceof ServerPlayer owner)) {

            return;
        }

        UUID targetId =
                TETHERS.get(owner.getUUID());

        if (targetId == null) {
            return;
        }


        // -----------------------------------------------------
        // OWNER INVALID
        // -----------------------------------------------------

        if (!owner.isAlive()
                || owner.isSpectator()) {

            TETHERS.remove(
                    owner.getUUID()
            );

            return;
        }


        // -----------------------------------------------------
        // FIND TARGET
        // -----------------------------------------------------

        LivingEntity target =
                resolveTarget(
                        owner,
                        targetId
                );

        if (target == null
                || !target.isAlive()) {

            TETHERS.remove(
                    owner.getUUID()
            );

            return;
        }


        // -----------------------------------------------------
        // DISTANCE
        // -----------------------------------------------------

        Vec3 delta =
                owner.position()
                        .subtract(
                                target.position()
                        );

        double distance =
                delta.length();


        /*
         * IMPORTANT:
         *
         * There is intentionally NO maximum tether distance.
         *
         * The Weighted Net cannot snap from being stretched.
         *
         * Once a target is caught, the tether remains attached
         * until the player releases it or either entity becomes
         * invalid.
         */


        // -----------------------------------------------------
        // ROPE VISUAL
        // -----------------------------------------------------

        if (owner.tickCount % 4 == 0) {

            spawnRopeParticles(
                    (ServerLevel) owner.level(),
                    owner,
                    target
            );
        }


        // -----------------------------------------------------
        // SLACK AREA
        // -----------------------------------------------------

        if (distance
                <= WeaponStats.WEIGHTED_NET_SLACK_RANGE
                || distance < 0.001) {

            return;
        }


        // -----------------------------------------------------
        // PULL TARGET
        // -----------------------------------------------------

        double excess =
                distance
                        - WeaponStats.WEIGHTED_NET_SLACK_RANGE;

        double strength =
                Math.min(
                        0.35,
                        0.055 * excess
                );

        Vec3 pull =
                delta.normalize()
                        .scale(
                                strength
                        );

        target.push(
                pull.x,
                Math.max(
                        0.015,
                        pull.y * 0.25
                ),
                pull.z
        );
    }


    // =========================================================
    // ROPE PARTICLES
    // =========================================================

    private static void spawnRopeParticles(
            ServerLevel level,
            ServerPlayer owner,
            LivingEntity target
    ) {

        Vec3 start =
                target.position()
                        .add(
                                0.0,
                                target.getBbHeight()
                                        * 0.55,
                                0.0
                        );

        Vec3 end =
                owner.getEyePosition()
                        .add(
                                owner.getLookAngle()
                                        .scale(0.25)
                        )
                        .add(
                                0.0,
                                -0.35,
                                0.0
                        );

        Vec3 line =
                end.subtract(start);

        double length =
                line.length();

        if (length < 0.001) {
            return;
        }

        int steps =
                Math.max(
                        2,
                        (int) Math.ceil(
                                length * 1.5
                        )
                );

        for (int i = 0; i <= steps; i++) {

            double progress =
                    (double) i / steps;

            Vec3 point =
                    start.add(
                            line.scale(progress)
                    );

            level.sendParticles(
                    ROPE_PARTICLE,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }


    // =========================================================
    // TARGET LOOKUP
    // =========================================================

    private static LivingEntity resolveTarget(
            ServerPlayer owner,
            UUID targetId
    ) {

        ServerLevel level =
                (ServerLevel) owner.level();

        Entity entity =
                level.getEntity(targetId);

        if (entity
                instanceof LivingEntity living) {

            return living;
        }

        return null;
    }
}