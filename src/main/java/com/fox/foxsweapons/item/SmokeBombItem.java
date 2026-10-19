
package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.SmokeBombRenderer;
import com.fox.foxsweapons.entity.SmokeBombProjectile;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class SmokeBombItem extends Item implements GeoItem {

    private static final int THROW_COOLDOWN = 12;
    private static final float THROW_SPEED = 1.35F;
    private static final float THROW_INACCURACY = 0.75F;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SmokeBombItem(Properties properties) {
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
        consumer.accept(new GeoRenderProvider() {

            private SmokeBombRenderer renderer;

            @Override
            public GeoItemRenderer<SmokeBombItem>
            getGeoItemRenderer() {

                if (renderer == null) {
                    renderer = new SmokeBombRenderer();
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
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // TOOLTIP
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.brand"
                ).withStyle(ChatFormatting.DARK_RED)
        );

        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.smoke_bomb.description"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.smoke_bomb.ability"
                ).withStyle(ChatFormatting.GOLD)
        );

        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.smoke_bomb.stealth"
                ).withStyle(ChatFormatting.GREEN)
        );

        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.smoke_bomb.stats"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );

        tooltip.accept(
                Component.translatable(
                        "tooltip.foxsweapons.smoke_bomb.damage"
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    // =========================================================
    // THROW SMOKE BOMB
    // =========================================================

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        // Throw sound.
        serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS,
                0.8F,
                0.85F
        );

        // Launch a copy so consuming the original stack
        // does not interfere with the flying projectile.
        ItemStack thrownBomb = stack.copyWithCount(1);

        Projectile.spawnProjectileFromRotation(
                SmokeBombProjectile::new,
                serverLevel,
                thrownBomb,
                serverPlayer,
                0.0F,
                THROW_SPEED,
                THROW_INACCURACY
        );

        player.getCooldowns().addCooldown(
                stack,
                THROW_COOLDOWN
        );

        // Survival consumes one bomb.
        // Creative mode keeps its bomb.
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.getInventory().setChanged();
        }

        player.awardStat(
                Stats.ITEM_USED.get(this)
        );

        return InteractionResult.SUCCESS;
    }
}
