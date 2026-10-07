package com.fox.foxsweapons.effect;

import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import net.minecraft.world.entity.LivingEntity;

public class ToxinEffect
        extends MobEffect {

    public ToxinEffect() {

        /*
         * Purple toxin.
         *
         * This colour also drives the default
         * status-effect particles.
         */
        super(
                MobEffectCategory.HARMFUL,
                0x7B2CBF
        );
    }


    // =========================================================
    // TICK INTERVAL
    // =========================================================

    @Override
    public boolean shouldApplyEffectTickThisTick(
            int tickCount,
            int amplifier
    ) {

        /*
         * MobEffectInstance uses the entity tick count
         * for infinite-duration effects.
         *
         * Therefore this stays valid even though the
         * effect duration itself is -1 forever.
         */

        return tickCount
                % WeaponStats.TOXIN_INTERVAL_TICKS
                == 0;
    }


    // =========================================================
    // TOXIN DAMAGE
    // =========================================================

    @Override
    public boolean applyEffectTick(
            ServerLevel level,
            LivingEntity target,
            int amplifier
    ) {

        if (!target.isAlive()) {

            /*
             * Returning true prevents Minecraft from
             * interpreting this as "remove the effect".
             */
            return true;
        }


        /*
         * Unlike vanilla Poison:
         *
         * - this CAN kill
         * - it works on undead
         * - it does not stop at half a heart
         *
         * We deliberately use magic damage rather than
         * vanilla POISON mechanics.
         */

        target.hurtServer(
                level,
                target.damageSources().magic(),
                WeaponStats.TOXIN_DAMAGE
        );


        /*
         * IMPORTANT:
         *
         * applyEffectTick returning false tells Minecraft
         * to remove the effect.
         *
         * Toxin must remain forever until something such
         * as milk explicitly removes it.
         */

        return true;
    }
}