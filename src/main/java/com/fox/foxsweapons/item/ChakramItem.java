package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.ChakramRenderer;
import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.entity.ChakramProjectile;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class ChakramItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public ChakramItem(Properties properties) {
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

                    private ChakramRenderer renderer;

                    @Override
                    public GeoItemRenderer<ChakramItem>
                    getGeoItemRenderer() {

                        if (renderer == null) {
                            renderer =
                                    new ChakramRenderer();
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
    // ENCHANTMENTS
    // =========================================================

    /**
     * EXACT Chakram enchantment whitelist.
     *
     * Allowed:
     * - Sharpness
     * - Smite
     * - Bane of Arthropods
     * - Looting
     *
     * Nothing else.
     */
    private static boolean isAllowedEnchantment(
            Holder<Enchantment> enchantment
    ) {
        return enchantment
                .unwrapKey()
                .map(key ->
                        key.equals(
                                Enchantments.SHARPNESS
                        )
                                || key.equals(
                                Enchantments.SMITE
                        )
                                || key.equals(
                                Enchantments.BANE_OF_ARTHROPODS
                        )
                                || key.equals(
                                Enchantments.LOOTING
                        )
                )
                .orElse(false);
    }

    /**
     * Controls which enchantments may be applied through
     * anvils and other normal application mechanics.
     */
    @Override
    public boolean supportsEnchantment(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        return isAllowedEnchantment(
                enchantment
        );
    }

    /**
     * Controls which enchantments the enchanting table
     * may offer for the Chakram.
     */
    @Override
    public boolean isPrimaryItemFor(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        return isAllowedEnchantment(
                enchantment
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
        ItemStack heldStack =
                player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel =
                (ServerLevel) level;

        boolean creative =
                serverPlayer
                        .getAbilities()
                        .instabuild;

        /*
         * Carry the EXACT stack.
         *
         * This preserves:
         *
         * - damage
         * - enchantments
         * - components
         * - custom data
         *
         * The projectile therefore literally carries
         * the enchanted Chakram through the world.
         */
        ItemStack projectileStack =
                heldStack.copyWithCount(1);

        // -----------------------------------------------------
        // DURABILITY
        // -----------------------------------------------------

        if (!creative) {

            int nextDamage =
                    projectileStack.getDamageValue()
                            + 1;

            /*
             * Final durability point:
             *
             * break instead of deploying a Chakram whose
             * damage value has reached max durability.
             */
            if (nextDamage >=
                    projectileStack.getMaxDamage()) {

                heldStack.hurtAndBreak(
                        1,
                        serverPlayer,
                        hand
                );

                serverPlayer.awardStat(
                        Stats.ITEM_USED.get(this)
                );

                return InteractionResult.SUCCESS;
            }

            /*
             * EXACTLY ONE durability per deployment.
             *
             * Unbreaking is deliberately not allowed,
             * so this can never randomly become zero.
             */
            projectileStack.setDamageValue(
                    nextDamage
            );
        }

        // -----------------------------------------------------
        // PROJECTILE
        // -----------------------------------------------------

        /*
         * Survival:
         * remove the physical item and return it later.
         *
         * Creative:
         * player keeps their item, so the projectile must
         * NOT create another copy when it returns.
         */
        boolean returnItemToOwner =
                !creative;

        ChakramProjectile projectile =
                new ChakramProjectile(
                        serverLevel,
                        serverPlayer,
                        projectileStack,
                        returnItemToOwner
                );

        projectile.setPos(
                serverPlayer.getX(),
                serverPlayer.getEyeY()
                        - 0.15D,
                serverPlayer.getZ()
        );

        Vec3 launchVelocity =
                serverPlayer
                        .getLookAngle()
                        .scale(
                                WeaponStats
                                        .CHAKRAM_LAUNCH_SPEED
                        );

        projectile.setDeltaMovement(
                launchVelocity
        );

        // -----------------------------------------------------
        // SPAWN
        // -----------------------------------------------------

        if (!serverLevel.addFreshEntity(
                projectile
        )) {
            return InteractionResult.FAIL;
        }

        if (!creative) {
            heldStack.shrink(1);
        }

        // -----------------------------------------------------
        // SOUND
        // -----------------------------------------------------

        serverLevel.playSound(
                null,

                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),

                SoundEvents.SNOWBALL_THROW,

                SoundSource.PLAYERS,

                0.8F,

                1.1F
                        + serverLevel
                        .getRandom()
                        .nextFloat()
                        * 0.2F
        );

        serverPlayer.awardStat(
                Stats.ITEM_USED.get(this)
        );

        return InteractionResult.SUCCESS;
    }
}