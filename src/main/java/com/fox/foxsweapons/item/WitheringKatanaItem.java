package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.WitheringKatanaRenderer;
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
public class WitheringKatanaItem
        extends Item
        implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public WitheringKatanaItem(
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

                    private WitheringKatanaRenderer renderer;

                    @Override
                    public GeoItemRenderer<WitheringKatanaItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {
                            this.renderer =
                                    new WitheringKatanaRenderer();
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
        /*
         * The katana itself is static.
         *
         * The reverse-grip idle stance and attack motion
         * will be handled by the player pose code later.
         */
        controllers.add(
                new AnimationController<>(
                        "controller",
                        0,
                        state -> PlayState.STOP
                )
        );
    }

    // =========================================================
    // WITHERING STRIKE
    // =========================================================

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        LivingEntity target =
                event.getEntity();

        /*
         * Gameplay effects belong on the server.
         */
        if (!(target.level() instanceof ServerLevel)) {
            return;
        }

        /*
         * The hit must actually deal health damage.
         *
         * No applying Wither through a completely blocked hit.
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
         * Direct melee hit only.
         *
         * Prevent arrows or other indirect damage from
         * inheriting the katana's Wither effect.
         */
        if (event.getSource().getDirectEntity() != player) {
            return;
        }

        /*
         * Player must actually be holding the katana
         * in the main hand when the hit lands.
         */
        if (!player
                .getMainHandItem()
                .is(FoxsWeapons.WITHERING_KATANA.get())) {

            return;
        }

        if (!target.isAlive()) {
            return;
        }

        /*
         * Wither II for approximately four seconds.
         *
         * Amplifier:
         * 0 = Wither I
         * 1 = Wither II
         */
        target.addEffect(
                new MobEffectInstance(
                        MobEffects.WITHER,
                        WeaponStats.WITHERING_KATANA_WITHER_DURATION_TICKS,
                        WeaponStats.WITHERING_KATANA_WITHER_AMPLIFIER
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