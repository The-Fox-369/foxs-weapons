package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.IronVanguardShieldRenderer;

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

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.function.Consumer;

public class IronVanguardShieldItem
        extends ShieldItem
        implements GeoItem {

    // =========================================================
    // GECKOLIB CACHE
    // =========================================================

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public IronVanguardShieldItem(
            Properties properties
    ) {
        super(properties);

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

                    private IronVanguardShieldRenderer renderer;

                    @Override
                    public GeoItemRenderer<IronVanguardShieldItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {
                            this.renderer =
                                    new IronVanguardShieldRenderer();
                        }

                        return this.renderer;
                    }
                }
        );
    }

    // =========================================================
    // GECKOLIB ANIMATION CONTROLLER
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
    // OFFHAND PASSIVE
    // =========================================================

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel level,
            Entity owner,
            EquipmentSlot slot
    ) {
        if (!(owner instanceof LivingEntity living)) {
            return;
        }

        if (slot != EquipmentSlot.OFFHAND) {
            return;
        }

        if (owner.tickCount % 10 != 0) {
            return;
        }

        living.addEffect(
                new MobEffectInstance(
                        MobEffects.RESISTANCE,
                        30,
                        0,
                        false,
                        false,
                        false
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