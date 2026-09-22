package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.HeavyGreatswordRenderer;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public class HeavyGreatswordItem
        extends Item
        implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    /*
     * Secondary cleave damage also fires LivingDamageEvent.Post.
     *
     * Without this guard, a cleave hit could trigger another cleave,
     * which could trigger another cleave, which could trigger another...
     *
     * We are making a greatsword, not a nuclear chain reaction.
     */
    private static final ThreadLocal<Boolean> APPLYING_CLEAVE =
            ThreadLocal.withInitial(() -> false);

    public HeavyGreatswordItem(
            Properties properties
    ) {
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
        consumer.accept(
                new GeoRenderProvider() {

                    private HeavyGreatswordRenderer renderer;

                    @Override
                    public GeoItemRenderer<HeavyGreatswordItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {
                            this.renderer =
                                    new HeavyGreatswordRenderer();
                        }

                        return this.renderer;
                    }
                }
        );
    }

    // =========================================================
    // STATIC ITEM MODEL
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
    // HEAVY CLEAVE
    // =========================================================

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        /*
         * Ignore the secondary damage created by our own cleave.
         */
        if (APPLYING_CLEAVE.get()) {
            return;
        }

        LivingEntity target =
                event.getEntity();

        /*
         * Gameplay belongs on the logical server.
         */
        if (!(target.level()
                instanceof ServerLevel serverLevel)) {

            return;
        }

        /*
         * The primary attack must actually remove health.
         */
        if (event.getHealthDamage() <= 0.0F) {
            return;
        }

        /*
         * Damage must come from a player.
         */
        if (!(event.getSource().getEntity()
                instanceof Player player)) {

            return;
        }

        /*
         * Direct melee attack only.
         *
         * Arrows, projectiles and indirect damage do not suddenly
         * become giant sword cleaves because the attacker happens
         * to be holding the greatsword.
         */
        if (event.getSource().getDirectEntity() != player) {
            return;
        }

        /*
         * The weapon must actually be in the player's main hand.
         */
        if (!player
                .getMainHandItem()
                .is(FoxsWeapons.HEAVY_GREATSWORD.get())) {

            return;
        }

        /*
         * Find nearby living entities around the entity that was
         * struck directly.
         */
        List<LivingEntity> nearbyTargets =
                serverLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        target
                                .getBoundingBox()
                                .inflate(
                                        WeaponStats
                                                .HEAVY_GREATSWORD_CLEAVE_RADIUS
                                ),
                        entity ->
                                entity != target
                                        && entity != player
                                        && entity.isAlive()
                                        && !player.isAlliedTo(entity)
                );

        if (nearbyTargets.isEmpty()) {
            return;
        }

        /*
         * Closest targets are cleaved first.
         */
        nearbyTargets.sort(
                Comparator.comparingDouble(
                        target::distanceToSqr
                )
        );

        float cleaveDamage =
                event.getHealthDamage()
                        * WeaponStats
                        .HEAVY_GREATSWORD_CLEAVE_DAMAGE_MULTIPLIER;

        if (cleaveDamage <= 0.0F) {
            return;
        }

        int cleavedTargets = 0;

        APPLYING_CLEAVE.set(true);

        try {
            for (LivingEntity nearbyTarget : nearbyTargets) {

                if (cleavedTargets >=
                        WeaponStats
                                .HEAVY_GREATSWORD_CLEAVE_MAX_TARGETS) {

                    break;
                }

                boolean damaged =
                        nearbyTarget.hurtServer(
                                serverLevel,
                                event.getSource(),
                                cleaveDamage
                        );

                if (damaged) {
                    cleavedTargets++;
                }
            }
        } finally {
            APPLYING_CLEAVE.remove();
        }

        /*
         * Give the cleave some visual/audio feedback.
         */
        if (cleavedTargets > 0) {

            serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    target.getX(),
                    target.getY()
                            + target.getBbHeight() * 0.5,
                    target.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );

            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.75F
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