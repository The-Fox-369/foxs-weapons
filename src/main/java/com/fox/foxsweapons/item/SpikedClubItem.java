package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.SpikedClubRenderer;
import com.fox.foxsweapons.config.WeaponStats;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.function.Consumer;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public class SpikedClubItem
        extends Item
        implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SpikedClubItem(
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

                    private SpikedClubRenderer renderer;

                    @Override
                    public GeoItemRenderer<SpikedClubItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {
                            this.renderer =
                                    new SpikedClubRenderer();
                        }

                        return this.renderer;
                    }
                }
        );
    }

    // =========================================================
    // NO ITEM-BONE ANIMATION
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
    // CONCUSSIVE STRIKE
    // =========================================================

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        LivingEntity target =
                event.getEntity();

        /*
         * Gameplay effects belong on the logical server.
         */
        if (!(target.level() instanceof ServerLevel)) {
            return;
        }

        /*
         * The hit must actually remove health.
         *
         * Completely blocked or zero-damage hits
         * do not apply Nausea.
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
         * Only a direct melee strike qualifies.
         *
         * This prevents arrows, projectiles or other indirect
         * damage from inheriting the club's effect merely
         * because the attacker is holding it.
         */
        if (event.getSource().getDirectEntity() != player) {
            return;
        }

        /*
         * The player must actually have the Spiked Club
         * in the main hand when the hit lands.
         */
        if (!player
                .getMainHandItem()
                .is(FoxsWeapons.SPIKED_CLUB.get())) {

            return;
        }

        /*
         * If the impact killed the target, there is
         * no surviving entity that needs the effect.
         */
        if (!target.isAlive()) {
            return;
        }

        /*
         * Nausea 255 for exactly 2.55 seconds.
         *
         * 51 ticks / 20 ticks per second = 2.55 seconds.
         *
         * Amplifiers are zero-based:
         * 254 = effect level 255.
         */
        target.addEffect(
                new MobEffectInstance(
                        MobEffects.NAUSEA,
                        WeaponStats.SPIKED_CLUB_NAUSEA_DURATION_TICKS,
                        WeaponStats.SPIKED_CLUB_NAUSEA_AMPLIFIER
                )
        );
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