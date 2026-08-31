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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class WeightedNetItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WeightedNetItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (WeightedNetTetherManager.release(serverPlayer, true)) {
            player.getCooldowns().addCooldown(stack, WeaponStats.WEIGHTED_NET_THROW_COOLDOWN_TICKS);
            player.swing(hand, true);
            return InteractionResult.SUCCESS;
        }

        ItemStack projectileStack = stack.copy();
        projectileStack.setCount(1);

        WeightedNetProjectile projectile =
                new WeightedNetProjectile(serverLevel, serverPlayer, projectileStack);

        projectile.shootFromRotation(
                serverPlayer,
                serverPlayer.getXRot(),
                serverPlayer.getYRot(),
                0.0F,
                1.35F,
                0.5F
        );

        serverLevel.addFreshEntity(projectile);

        serverLevel.playSound(
                null,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.8F,
                0.85F + serverLevel.getRandom().nextFloat() * 0.2F
        );

        player.getCooldowns().addCooldown(stack, WeaponStats.WEIGHTED_NET_THROW_COOLDOWN_TICKS);
        player.swing(hand, true);

        return InteractionResult.SUCCESS;
    }
}
