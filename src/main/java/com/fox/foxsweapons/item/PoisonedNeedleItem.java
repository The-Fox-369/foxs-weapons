package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.PoisonedNeedleRenderer;
import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.entity.PoisonedNeedleProjectile;

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


public class PoisonedNeedleItem
        extends Item
        implements GeoItem {

    // =========================================================
    // GECKOLIB
    // =========================================================

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);


    public PoisonedNeedleItem(
            Properties properties
    ) {

        super(properties);

        GeoItem.registerSyncedAnimatable(this);
    }


    // =========================================================
    // RENDERER
    // =========================================================

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {

        consumer.accept(
                new GeoRenderProvider() {

                    private PoisonedNeedleRenderer renderer;


                    @Override
                    public GeoItemRenderer<PoisonedNeedleItem>
                    getGeoItemRenderer() {

                        if (renderer == null) {

                            renderer =
                                    new PoisonedNeedleRenderer();
                        }


                        return renderer;
                    }
                }
        );
    }


    // =========================================================
    // TOOLTIP
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag tooltipFlag
    ) {

        builder.accept(
                Component
                        .translatable(
                                "tooltip.foxsweapons.poisoned_needle.description"
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );


        builder.accept(
                Component
                        .translatable(
                                "tooltip.foxsweapons.poisoned_needle.throw"
                        )
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );


        builder.accept(
                Component
                        .translatable(
                                "tooltip.foxsweapons.poisoned_needle.toxin"
                        )
                        .withStyle(
                                ChatFormatting.DARK_PURPLE
                        )
        );
    }


    // =========================================================
    // THROW
    // =========================================================

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


        ServerLevel serverLevel =
                (ServerLevel) level;


        // -----------------------------------------------------
        // SOUND
        // -----------------------------------------------------

        serverLevel.playSound(
                null,

                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),

                SoundEvents.ARROW_SHOOT,

                SoundSource.PLAYERS,

                0.7F,

                1.35F
                        + serverLevel
                        .getRandom()
                        .nextFloat()
                        * 0.15F
        );


        // -----------------------------------------------------
        // THROW NEEDLE
        // -----------------------------------------------------

        Projectile.spawnProjectileFromRotation(
                PoisonedNeedleProjectile::new,

                serverLevel,

                stack,

                serverPlayer,

                0.0F,

                WeaponStats
                        .POISONED_NEEDLE_THROW_POWER,

                WeaponStats
                        .POISONED_NEEDLE_INACCURACY
        );


        // -----------------------------------------------------
        // CONSUME NEEDLE
        // -----------------------------------------------------

        /*
         * NO COOLDOWN.
         *
         * If the player has 37 Needles and a fast mouse,
         * that is now everybody else's problem.
         */

        if (!serverPlayer
                .getAbilities()
                .instabuild) {

            stack.shrink(
                    1
            );


            serverPlayer
                    .getInventory()
                    .setChanged();
        }


        serverPlayer.awardStat(
                Stats.ITEM_USED.get(this)
        );


        return InteractionResult.SUCCESS;
    }


    // =========================================================
    // NO ITEM ANIMATION
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


    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return cache;
    }
}