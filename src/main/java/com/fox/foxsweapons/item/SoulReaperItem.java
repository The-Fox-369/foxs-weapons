package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.SoulReaperRenderer;
import com.fox.foxsweapons.config.WeaponStats;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public class SoulReaperItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    /*
     * Player UUID -> target entity ID.
     *
     * CriticalHitEvent tells us FOR CERTAIN that Minecraft
     * classified this attack as a vanilla critical.
     *
     * We remember that until the damage sequence finishes.
     */
    private static final Map<UUID, Integer> PENDING_SOUL_STEALS =
            new HashMap<>();


    public SoulReaperItem(Properties properties) {
        super(properties);

        GeoItem.registerSyncedAnimatable(this);
    }


    // =========================================================
    // GECKOLIB RENDERING
    // =========================================================

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {
        consumer.accept(new GeoRenderProvider() {

            private SoulReaperRenderer renderer;

            @Override
            public GeoItemRenderer<SoulReaperItem> getGeoItemRenderer() {

                if (renderer == null) {
                    renderer = new SoulReaperRenderer();
                }

                return renderer;
            }
        });
    }


    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        /*
         * Soul Reaper itself has no GeckoLib animation.
         *
         * WeaponPlayerAnimations handles the player swing.
         */
        controllers.add(
                new AnimationController<>(
                        "controller",
                        0,
                        state -> PlayState.STOP
                )
        );
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    // =========================================================
    // STEP 1 - DETECT THE ACTUAL CRITICAL
    // =========================================================

    @SubscribeEvent
    public static void onCriticalHit(
            CriticalHitEvent event
    ) {
        Player player =
                event.getEntity();

        /*
         * CriticalHitEvent fires on both sides.
         *
         * Gameplay logic belongs on the server.
         */
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }

        /*
         * Always clear any old unfinished marker.
         *
         * CriticalHitEvent fires for attacks whether or not
         * they end up being critical, so the next attack
         * automatically cleans stale state.
         */
        PENDING_SOUL_STEALS.remove(
                player.getUUID()
        );

        /*
         * ONLY vanilla jump criticals.
         *
         * No guessing with:
         *
         * fallDistance
         * onGround()
         * attackStrength
         *
         * after the attack has already happened.
         */
        if (!event.isVanillaCritical()) {
            return;
        }

        /*
         * Must actually be attacking with Soul Reaper.
         */
        if (!player
                .getMainHandItem()
                .is(FoxsWeapons.SOUL_REAPER.get())) {

            return;
        }

        Entity target =
                event.getTarget();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        if (!livingTarget.isAlive()) {
            return;
        }

        /*
         * Remember:
         *
         * THIS player
         * just crit
         * THIS target
         * with Soul Reaper.
         */
        PENDING_SOUL_STEALS.put(
                player.getUUID(),
                livingTarget.getId()
        );
    }


    // =========================================================
    // STEP 2 - TAKE THE EXTRA HALF HEART
    // =========================================================

    @SubscribeEvent
    public static void onDamagePre(
            LivingDamageEvent.Pre event
    ) {
        Player player =
                getPendingSoulReaperPlayer(
                        event.getSource(),
                        event.getEntity()
                );

        if (player == null) {
            return;
        }

        /*
         * Actual LIFE STEAL damage.
         *
         * Existing crit damage
         * +
         * 1 health point
         *
         * 1 health point = HALF A HEART.
         */
        event.setNewDamage(
                event.getNewDamage()
                        + WeaponStats.SOUL_REAPER_LIFESTEAL
        );
    }


    // =========================================================
    // STEP 3 - GIVE THE STOLEN HEALTH TO PLAYER
    // =========================================================

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        Player player =
                getPendingSoulReaperPlayer(
                        event.getSource(),
                        event.getEntity()
                );

        if (player == null) {
            return;
        }

        /*
         * Damage sequence is finished.
         *
         * Consume the pending steal so it can NEVER leak
         * into another damage event.
         */
        PENDING_SOUL_STEALS.remove(
                player.getUUID()
        );

        /*
         * If the target genuinely lost no health,
         * there is nothing to steal.
         */
        if (event.getHealthDamage() <= 0.0F) {
            return;
        }

        /*
         * Transfer HALF A HEART.
         */
        float stolen =
                Math.min(
                        WeaponStats.SOUL_REAPER_LIFESTEAL,
                        event.getHealthDamage()
                );

        player.heal(stolen);

        /*
         * Visual confirmation that the steal ACTUALLY happened.
         */
        if (player.level() instanceof ServerLevel level) {

            spawnSoulDrain(
                    level,
                    event.getEntity(),
                    player
            );
        }
    }


    // =========================================================
    // MATCH DAMAGE TO THE CRITICAL WE SAVED
    // =========================================================

    private static Player getPendingSoulReaperPlayer(
            DamageSource source,
            LivingEntity target
    ) {
        /*
         * Damage must come from a player.
         */
        if (!(source.getEntity() instanceof Player player)) {
            return null;
        }

        /*
         * It must be direct melee damage from that player.
         *
         * Prevents arrows / projectiles / other damage
         * from accidentally consuming the pending crit.
         */
        if (source.getDirectEntity() != player) {
            return null;
        }

        Integer expectedTarget =
                PENDING_SOUL_STEALS.get(
                        player.getUUID()
                );

        if (expectedTarget == null) {
            return null;
        }

        /*
         * Ensure this is the SAME victim from CriticalHitEvent.
         */
        if (expectedTarget != target.getId()) {
            return null;
        }

        /*
         * Still ensure the player has Soul Reaper.
         */
        if (!player
                .getMainHandItem()
                .is(FoxsWeapons.SOUL_REAPER.get())) {

            PENDING_SOUL_STEALS.remove(
                    player.getUUID()
            );

            return null;
        }

        return player;
    }


    // =========================================================
    // SOUL PARTICLES
    // =========================================================

    private static void spawnSoulDrain(
            ServerLevel level,
            LivingEntity target,
            Player player
    ) {
        Vec3 start =
                target
                        .getEyePosition()
                        .add(
                                0.0,
                                -0.30,
                                0.0
                        );

        Vec3 end =
                player
                        .getEyePosition()
                        .add(
                                0.0,
                                -0.25,
                                0.0
                        );

        Vec3 difference =
                end.subtract(start);


        /*
         * Visible victim -> attacker trail.
         */
        for (int i = 0; i <= 7; i++) {

            double progress =
                    i / 7.0;

            Vec3 point =
                    start.add(
                            difference.scale(progress)
                    );

            level.sendParticles(
                    ParticleTypes.SOUL,

                    point.x,
                    point.y,
                    point.z,

                    2,

                    0.025,
                    0.025,
                    0.025,

                    0.005
            );
        }


        /*
         * Victim burst.
         */
        level.sendParticles(
                ParticleTypes.SOUL,

                start.x,
                start.y,
                start.z,

                8,

                0.18,
                0.24,
                0.18,

                0.025
        );


        /*
         * Player receives the soul.
         */
        level.sendParticles(
                ParticleTypes.SOUL,

                end.x,
                end.y,
                end.z,

                6,

                0.12,
                0.18,
                0.12,

                0.015
        );
    }
}