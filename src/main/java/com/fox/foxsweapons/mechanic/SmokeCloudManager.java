package com.fox.foxsweapons.mechanic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class SmokeCloudManager {

    private SmokeCloudManager() {
    }

    // =========================================================
    // CLOUD SETTINGS
    // =========================================================

    public static final int DURATION_TICKS = 160;
    public static final double RADIUS = 5.0D;

    private static final double VERTICAL_RANGE = 3.5D;
    private static final double MOB_SEARCH_RADIUS = 40.0D;

    private static final int BLINDNESS_DURATION = 24;
    private static final int BLINDNESS_REFRESH = 10;
    private static final int PARTICLE_INTERVAL = 2;

    // =========================================================
    // SMOKE COLOURS
    // =========================================================

    private static final DustParticleOptions DARK_SMOKE =
            new DustParticleOptions(0x252529, 3.5F);

    private static final DustParticleOptions GREY_SMOKE =
            new DustParticleOptions(0x4B4B52, 3.0F);

    // =========================================================
    // ACTIVE CLOUDS
    // =========================================================

    private static final Map<ServerLevel, List<SmokeCloud>>
            CLOUDS = new WeakHashMap<>();

    private static final class SmokeCloud {

        private final Vec3 center;
        private int ticksRemaining;

        private SmokeCloud(Vec3 center) {
            this.center = center;
            this.ticksRemaining = DURATION_TICKS;
        }
    }

    // =========================================================
    // DETONATION
    // =========================================================

    public static void deploy(
            ServerLevel level,
            Vec3 position
    ) {
        CLOUDS.computeIfAbsent(
                level,
                unused -> new ArrayList<>()
        ).add(new SmokeCloud(position));

        // Initial smoke explosion.
        level.sendParticles(
                ParticleTypes.POOF,
                position.x,
                position.y + 0.8D,
                position.z,
                100,
                1.5D,
                0.8D,
                1.5D,
                0.10D
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                position.x,
                position.y + 1.2D,
                position.z,
                100,
                1.8D,
                0.9D,
                1.8D,
                0.02D
        );

        level.sendParticles(
                DARK_SMOKE,
                position.x,
                position.y + 1.3D,
                position.z,
                100,
                1.3D,
                0.8D,
                1.3D,
                0.01D
        );

        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                1.0F,
                0.65F
        );

        // Stealth becomes active immediately.
        clearMobAggression(level, position);
        applyVisionObstruction(level, position);
    }

    // =========================================================
    // SERVER TICK
    // =========================================================

    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            List<SmokeCloud> clouds = CLOUDS.get(level);

            if (clouds == null || clouds.isEmpty()) {
                continue;
            }

            Iterator<SmokeCloud> iterator =
                    clouds.iterator();

            while (iterator.hasNext()) {

                SmokeCloud cloud = iterator.next();

                if (cloud.ticksRemaining <= 0) {
                    iterator.remove();
                    continue;
                }

                if (cloud.ticksRemaining
                        % PARTICLE_INTERVAL == 0) {

                    spawnDenseSmoke(level, cloud);
                }

                clearMobAggression(level, cloud.center);

                if (cloud.ticksRemaining
                        % BLINDNESS_REFRESH == 0) {

                    applyVisionObstruction(
                            level,
                            cloud.center
                    );
                }

                cloud.ticksRemaining--;
            }

            if (clouds.isEmpty()) {
                CLOUDS.remove(level);
            }
        }
    }

    // =========================================================
    // DENSE SMOKE PARTICLES
    // =========================================================

    private static void spawnDenseSmoke(
            ServerLevel level,
            SmokeCloud cloud
    ) {
        Vec3 center = cloud.center;

        level.sendParticles(
                DARK_SMOKE,
                center.x,
                center.y + 1.4D,
                center.z,
                65,
                1.4D,
                0.75D,
                1.4D,
                0.005D
        );

        level.sendParticles(
                GREY_SMOKE,
                center.x,
                center.y + 1.3D,
                center.z,
                85,
                2.2D,
                0.9D,
                2.2D,
                0.008D
        );

        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x,
                center.y + 1.2D,
                center.z,
                36,
                2.0D,
                0.85D,
                2.0D,
                0.01D
        );
    }

    // =========================================================
    // CONCEALMENT CHECK
    // =========================================================

    public static boolean isConcealed(
            ServerLevel level,
            LivingEntity entity
    ) {
        if (!entity.isAlive()
                || entity.level() != level) {
            return false;
        }

        List<SmokeCloud> clouds = CLOUDS.get(level);

        if (clouds == null) {
            return false;
        }

        for (SmokeCloud cloud : clouds) {

            if (cloud.ticksRemaining <= 0) {
                continue;
            }

            if (insideCloud(cloud.center, entity)) {
                return true;
            }
        }

        return false;
    }

    private static boolean insideCloud(
            Vec3 center,
            LivingEntity entity
    ) {
        double dx = entity.getX() - center.x;
        double dz = entity.getZ() - center.z;

        double distanceSquared = dx * dx + dz * dz;

        double cloudEyeCenter = center.y + 1.3D;

        double heightDifference =
                Math.abs(entity.getEyeY() - cloudEyeCenter);

        return distanceSquared <= RADIUS * RADIUS
                && heightDifference <= VERTICAL_RANGE;
    }

    // =========================================================
    // PLAYER VISION OBSTRUCTION
    // =========================================================

    private static void applyVisionObstruction(
            ServerLevel level,
            Vec3 center
    ) {
        AABB searchBox = new AABB(
                center.x - RADIUS,
                center.y - VERTICAL_RANGE,
                center.z - RADIUS,
                center.x + RADIUS,
                center.y + VERTICAL_RANGE + 2.0D,
                center.z + RADIUS
        );

        for (ServerPlayer player :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        searchBox
                )) {

            if (player.isSpectator()) {
                continue;
            }

            if (!isConcealed(level, player)) {
                continue;
            }

            // Preserve longer Blindness effects.
            MobEffectInstance existing =
                    player.getEffect(MobEffects.BLINDNESS);

            if (existing != null
                    && existing.getDuration()
                    > BLINDNESS_DURATION) {
                continue;
            }

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.BLINDNESS,
                            BLINDNESS_DURATION,
                            0,
                            false,
                            false,
                            false
                    )
            );
        }
    }

    // =========================================================
    // REMOVE MOB AGGRESSION
    // =========================================================

    private static void clearMobAggression(
            ServerLevel level,
            Vec3 center
    ) {
        AABB searchBox = new AABB(
                center.x - MOB_SEARCH_RADIUS,
                center.y - 16.0D,
                center.z - MOB_SEARCH_RADIUS,
                center.x + MOB_SEARCH_RADIUS,
                center.y + 16.0D,
                center.z + MOB_SEARCH_RADIUS
        );

        for (Mob mob :
                level.getEntitiesOfClass(
                        Mob.class,
                        searchBox
                )) {

            // Normal mob targeting.
            LivingEntity currentTarget = mob.getTarget();

            if (currentTarget != null
                    && isConcealed(level, currentTarget)) {

                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            // ================================================
            // FIX: BRAIN MEMORY REGISTRATION
            //
            // Not every mob registers ATTACK_TARGET.
            // Reading an unregistered memory causes:
            //
            // IllegalStateException:
            // Unregistered memory fetched
            //
            // Check VALUE_PRESENT first.
            // This is false if the memory is missing
            // or unregistered.
            // ================================================

            Brain<?> brain = mob.getBrain();

            if (!brain.checkMemory(
                    MemoryModuleType.ATTACK_TARGET,
                    MemoryStatus.VALUE_PRESENT
            )) {
                continue;
            }

            brain.getMemory(
                    MemoryModuleType.ATTACK_TARGET
            ).ifPresent(brainTarget -> {

                if (isConcealed(level, brainTarget)) {

                    brain.eraseMemory(
                            MemoryModuleType.ATTACK_TARGET
                    );

                    mob.getNavigation().stop();
                }
            });
        }
    }

    // =========================================================
    // BLOCK NEW TARGETS INSIDE SMOKE
    // =========================================================

    public static void onChangeTarget(
            LivingChangeTargetEvent event
    ) {
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }

        if (!(event.getEntity().level()
                instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target =
                event.getNewAboutToBeSetTarget();

        if (target != null
                && isConcealed(level, target)) {

            event.setNewAboutToBeSetTarget(null);
        }
    }

    // =========================================================
    // MAKE CONCEALED ENTITIES HARDER TO DETECT
    // =========================================================

    public static void onVisibility(
            LivingEvent.LivingVisibilityEvent event
    ) {
        if (!(event.getLookingEntity()
                instanceof Mob)) {
            return;
        }

        if (!(event.getEntity().level()
                instanceof ServerLevel level)) {
            return;
        }

        if (isConcealed(level, event.getEntity())) {
            event.modifyVisibility(0.0D);
        }
    }
}
