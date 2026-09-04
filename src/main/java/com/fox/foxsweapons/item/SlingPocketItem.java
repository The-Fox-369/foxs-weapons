package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.SlingPocketRenderer;
import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.entity.SlingStoneProjectile;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class SlingPocketItem
        extends Item
        implements GeoItem {

    // =========================================================
    // AMMO TAG
    // =========================================================

    public static final TagKey<Item> SLING_AMMO =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            FoxsWeapons.MODID,
                            "sling_ammo"
                    )
            );


    // =========================================================
    // GECKOLIB
    // =========================================================

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);


    public SlingPocketItem(
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

                    private SlingPocketRenderer renderer;

                    @Override
                    public GeoItemRenderer<SlingPocketItem>
                    getGeoItemRenderer() {

                        if (renderer == null) {

                            renderer =
                                    new SlingPocketRenderer();
                        }

                        return renderer;
                    }
                }
        );
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


    // =========================================================
    // FIRE
    // =========================================================

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack sling =
                player.getItemInHand(hand);


        if (!(player instanceof ServerPlayer serverPlayer)) {

            return InteractionResult.SUCCESS;
        }


        // -----------------------------------------------------
        // COOLDOWN
        // -----------------------------------------------------

        if (serverPlayer
                .getCooldowns()
                .isOnCooldown(sling)) {

            return InteractionResult.FAIL;
        }


        // -----------------------------------------------------
        // FIND AMMO
        // -----------------------------------------------------

        ItemStack ammo =
                findAmmo(
                        serverPlayer
                );


        if (ammo.isEmpty()
                && !serverPlayer
                .getAbilities()
                .instabuild) {

            serverPlayer.sendOverlayMessage(
                    Component.literal(
                            "Need stone ammo"
                    )
            );

            return InteractionResult.FAIL;
        }


        // -----------------------------------------------------
        // PROJECTILE APPEARANCE
        // -----------------------------------------------------

        ItemStack projectileStack;

        if (ammo.isEmpty()) {

            /*
             * Creative mode fallback.
             */
            projectileStack =
                    new ItemStack(
                            Items.COBBLESTONE
                    );

        } else {

            /*
             * Projectile visually becomes the exact
             * stone ammunition that was consumed.
             */
            projectileStack =
                    ammo.copyWithCount(1);
        }


        ServerLevel serverLevel =
                (ServerLevel) level;


        // -----------------------------------------------------
        // SHOOT SOUND
        // -----------------------------------------------------

        serverLevel.playSound(
                null,

                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),

                SoundEvents.SNOWBALL_THROW,

                SoundSource.PLAYERS,

                0.8F,

                1.25F
                        + serverLevel
                        .getRandom()
                        .nextFloat()
                        * 0.15F
        );


        // -----------------------------------------------------
        // ROCK GO BONK
        // -----------------------------------------------------

        Projectile.spawnProjectileFromRotation(
                SlingStoneProjectile::new,

                serverLevel,

                projectileStack,

                serverPlayer,

                0.0F,

                WeaponStats.SLING_POCKET_POWER,

                WeaponStats.SLING_POCKET_INACCURACY
        );


        // -----------------------------------------------------
        // CONSUME AMMO
        // -----------------------------------------------------

        if (!serverPlayer
                .getAbilities()
                .instabuild
                && !ammo.isEmpty()) {

            ammo.shrink(1);

            serverPlayer
                    .getInventory()
                    .setChanged();
        }


        // -----------------------------------------------------
        // COOLDOWN
        // -----------------------------------------------------

        serverPlayer
                .getCooldowns()
                .addCooldown(
                        sling,
                        WeaponStats.SLING_POCKET_COOLDOWN
                );


        serverPlayer.awardStat(
                Stats.ITEM_USED.get(this)
        );


        return InteractionResult.SUCCESS;
    }


    // =========================================================
    // FIND AMMO
    // =========================================================

    private static ItemStack findAmmo(
            ServerPlayer player
    ) {

        if (player
                .getAbilities()
                .instabuild) {

            return ItemStack.EMPTY;
        }


        for (
                int slot = 0;
                slot < player
                        .getInventory()
                        .getContainerSize();
                slot++
        ) {

            ItemStack stack =
                    player
                            .getInventory()
                            .getItem(slot);


            if (stack.isEmpty()) {

                continue;
            }


            if (isStoneAmmo(stack)) {

                return stack;
            }
        }


        return ItemStack.EMPTY;
    }


    // =========================================================
    // STONE AMMO CHECK
    // =========================================================

    private static boolean isStoneAmmo(
            ItemStack stack
    ) {

        /*
         * First allow anything added through our
         * custom data tag.
         */
        if (stack.is(SLING_AMMO)) {

            return true;
        }


        /*
         * Hard fallback.
         *
         * This guarantees common stone blocks work
         * even if the custom tag fails to load for
         * some stupid datapack/resource reason.
         */

        return stack.is(Items.STONE)
                || stack.is(Items.COBBLESTONE)
                || stack.is(Items.MOSSY_COBBLESTONE)
                || stack.is(Items.SMOOTH_STONE)

                || stack.is(Items.GRANITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.ANDESITE)

                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)

                || stack.is(Items.TUFF)
                || stack.is(Items.CALCITE)

                || stack.is(Items.BLACKSTONE)

                || stack.is(Items.END_STONE);
    }
}