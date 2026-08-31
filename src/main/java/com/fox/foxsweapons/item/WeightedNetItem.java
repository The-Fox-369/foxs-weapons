package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.WeightedNetRenderer;
import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.entity.WeightedNetProjectile;
import com.fox.foxsweapons.mechanic.WeightedNetTetherManager;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class WeightedNetItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public WeightedNetItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {
        consumer.accept(new GeoRenderProvider() {

            private WeightedNetRenderer renderer;

            @Override
            public GeoItemRenderer<WeightedNetItem> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new WeightedNetRenderer();
                }

                return renderer;
            }
        });
    }

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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack =
                player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        /*
         * Right-click while a target is caught:
         * release the net and inflict Rope Burns.
         */
        if (WeightedNetTetherManager.release(
                serverPlayer,
                true
        )) {
            player.awardStat(
                    Stats.ITEM_USED.get(this)
            );

            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel =
                (ServerLevel) level;

        serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.7F,
                0.8F
                        + serverLevel.getRandom().nextFloat()
                        * 0.2F
        );

        Projectile.spawnProjectileFromRotation(
                WeightedNetProjectile::new,
                serverLevel,
                stack,
                player,
                0.0F,
                WeaponStats.WEIGHTED_NET_THROW_POWER,
                WeaponStats.WEIGHTED_NET_INACCURACY
        );

        player.getCooldowns().addCooldown(
                stack,
                WeaponStats.WEIGHTED_NET_THROW_COOLDOWN
        );

        stack.hurtAndBreak(
                1,
                serverPlayer,
                hand
        );

        player.awardStat(
                Stats.ITEM_USED.get(this)
        );

        return InteractionResult.SUCCESS;
    }
}