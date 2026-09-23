package com.fox.foxsweapons.mechanic;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.entity.ChakramProjectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID
)
public final class ChakramCombatEvents {

    private ChakramCombatEvents() {
    }

    // =========================================================
    // DAMAGE ENCHANTMENTS
    // =========================================================

    /**
     * Makes the thrown Chakram's actual carried ItemStack
     * participate in vanilla enchantment damage calculation.
     * <p>
     * This gives us the real vanilla behavior for:
     * <p>
     * - Sharpness
     * - Smite
     * - Bane of Arthropods
     * <p>
     * instead of manually recreating their damage formulas.
     */
    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        ChakramProjectile chakram =
                getChakram(
                        event.getSource()
                );

        if (chakram == null) {
            return;
        }

        if (!(event
                .getEntity()
                .level()
                instanceof ServerLevel serverLevel)) {

            return;
        }

        ItemStack chakramStack =
                chakram.getItem();

        if (chakramStack.isEmpty()
                || !chakramStack.is(
                FoxsWeapons.CHAKRAM.get()
        )) {

            return;
        }

        float enchantedDamage =
                EnchantmentHelper.modifyDamage(
                        serverLevel,

                        chakramStack,

                        event.getEntity(),

                        event.getSource(),

                        event.getAmount()
                );

        event.setAmount(
                enchantedDamage
        );
    }

    // =========================================================
    // BANE POST-ATTACK EFFECT
    // =========================================================

    /**
     * Bane of Arthropods also has a post-hit Slowness effect.
     * <p>
     * A Chakram hit is technically an indirect projectile-style
     * DamageSource because:
     * <p>
     * direct entity = Chakram
     * causing entity = player
     * <p>
     * Vanilla Bane's post-attack condition expects a direct
     * attack, so for THIS enchantment-effect pass we provide
     * the owner's direct player-attack DamageSource.
     * <p>
     * The actual damage itself still uses the real Chakram
     * DamageSource.
     */
    @SubscribeEvent
    public static void onPostDamage(
            LivingDamageEvent.Post event
    ) {
        if (event.getHealthDamage()
                <= 0.0F) {

            return;
        }

        ChakramProjectile chakram =
                getChakram(
                        event.getSource()
                );

        if (chakram == null) {
            return;
        }

        if (!(event
                .getEntity()
                .level()
                instanceof ServerLevel serverLevel)) {

            return;
        }

        ItemStack chakramStack =
                chakram.getItem();

        if (chakramStack.isEmpty()
                || !chakramStack.is(
                FoxsWeapons.CHAKRAM.get()
        )) {

            return;
        }

        /*
         * No Bane enchantment?
         *
         * Then there is no post-attack effect we need
         * from our allowed enchantment set.
         */
        var enchantmentLookup =
                serverLevel
                        .registryAccess()
                        .lookupOrThrow(
                                net.minecraft.core.registries
                                        .Registries.ENCHANTMENT
                        );

        int baneLevel =
                chakramStack
                        .getEnchantmentLevel(
                                enchantmentLookup
                                        .getOrThrow(
                                                Enchantments
                                                        .BANE_OF_ARTHROPODS
                                        )
                        );

        if (baneLevel <= 0) {
            return;
        }

        Entity owner =
                chakram.getOwner();

        if (!(owner instanceof Player player)) {
            return;
        }

        /*
         * Synthetic DIRECT player source used ONLY while
         * evaluating Bane's post-attack effect.
         */
        DamageSource directOwnerSource =
                serverLevel
                        .damageSources()
                        .playerAttack(
                                player
                        );

        EnchantmentHelper
                .doPostAttackEffectsWithItemSource(
                        serverLevel,

                        event.getEntity(),

                        directOwnerSource,

                        chakramStack
                );
    }

    // =========================================================
    // LOOTING
    // =========================================================

    /**
     * Vanilla entity loot tables normally inspect the
     * attacking LivingEntity's equipped weapon.
     * <p>
     * Our player is NOT holding the Chakram while it is flying.
     * <p>
     * NeoForge exposes EnchantedEntityLootEvent specifically
     * when entity loot asks:
     * <p>
     * "what level of this enchantment applies?"
     * <p>
     * We answer with the level stored on the ACTUAL flying
     * Chakram ItemStack.
     */
    @SubscribeEvent
    public static void onEnchantedEntityLoot(
            EnchantedEntityLootEvent event
    ) {
        DamageSource damageSource =
                event.getDamageSource();

        if (damageSource == null) {
            return;
        }

        ChakramProjectile chakram =
                getChakram(
                        damageSource
                );

        if (chakram == null) {
            return;
        }

        /*
         * ONLY intercept Looting.
         *
         * We do not fake or inject any other enchantment.
         */
        if (!event
                .getEnchantment()
                .unwrapKey()
                .map(
                        key ->
                                key.equals(
                                        Enchantments.LOOTING
                                )
                )
                .orElse(false)) {

            return;
        }

        ItemStack chakramStack =
                chakram.getItem();

        if (chakramStack.isEmpty()
                || !chakramStack.is(
                FoxsWeapons.CHAKRAM.get()
        )) {

            return;
        }

        int projectileLootingLevel =
                chakramStack
                        .getEnchantmentLevel(
                                event.getEnchantment()
                        );

        if (projectileLootingLevel
                > event.getEnchantmentLevel()) {

            event.setEnchantmentLevel(
                    projectileLootingLevel
            );
        }
    }

    // =========================================================
    // SOURCE HELPER
    // =========================================================

    private static ChakramProjectile getChakram(
            DamageSource source
    ) {
        if (source.getDirectEntity()
                instanceof ChakramProjectile chakram) {

            return chakram;
        }

        return null;
    }
}