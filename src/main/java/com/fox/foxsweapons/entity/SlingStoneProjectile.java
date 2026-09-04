package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class SlingStoneProjectile
        extends ThrowableItemProjectile {

    /*
     * Three seconds maximum lifetime.
     *
     * Prevents forgotten projectile entities
     * flying around forever.
     */
    private static final int MAX_LIFETIME_TICKS =
            60;


    // =========================================================
    // ENTITY CONSTRUCTOR
    // =========================================================

    public SlingStoneProjectile(
            EntityType<? extends SlingStoneProjectile> type,
            Level level
    ) {

        super(
                type,
                level
        );
    }


    // =========================================================
    // FIRED PROJECTILE CONSTRUCTOR
    // =========================================================

    public SlingStoneProjectile(
            Level level,
            LivingEntity owner,
            ItemStack ammo
    ) {

        super(
                FoxsWeapons.SLING_STONE_PROJECTILE.get(),
                owner,
                level,
                ammo
        );
    }


    // =========================================================
    // FALLBACK PROJECTILE ITEM
    // =========================================================

    @Override
    protected Item getDefaultItem() {

        return Items.COBBLESTONE;
    }


    // =========================================================
    // TICK
    // =========================================================

    @Override
    public void tick() {

        super.tick();


        if (!level().isClientSide()
                && tickCount
                > MAX_LIFETIME_TICKS) {

            discard();
        }
    }


    // =========================================================
    // ENTITY HIT
    // =========================================================

    @Override
    protected void onHitEntity(
            EntityHitResult hitResult
    ) {

        super.onHitEntity(
                hitResult
        );


        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        Entity owner =
                getOwner();


        Entity hit =
                hitResult.getEntity();


        /*
         * Sling Pocket is player-fired.
         *
         * Using the same server damage style already
         * used by Blunderbuss keeps this compatible
         * with the current project API.
         */
        if (owner instanceof ServerPlayer player
                && hit instanceof LivingEntity target
                && target != player
                && target.isAlive()) {


            boolean damaged =
                    target.hurtServer(
                            serverLevel,

                            player
                                    .damageSources()
                                    .playerAttack(
                                            player
                                    ),

                            WeaponStats.SLING_POCKET_DAMAGE
                    );


            if (damaged) {

                applyKnockback(
                        target
                );
            }
        }


        discard();
    }


    // =========================================================
    // BLOCK HIT
    // =========================================================

    @Override
    protected void onHitBlock(
            BlockHitResult hitResult
    ) {

        super.onHitBlock(
                hitResult
        );


        if (!level().isClientSide()) {

            discard();
        }
    }


    // =========================================================
    // KNOCKBACK
    // =========================================================

    private void applyKnockback(
            LivingEntity target
    ) {

        Vec3 movement =
                getDeltaMovement();


        /*
         * Only use horizontal projectile direction.
         */
        Vec3 horizontal =
                new Vec3(
                        movement.x,
                        0.0,
                        movement.z
                );


        if (horizontal.lengthSqr()
                <= 0.0001) {

            return;
        }


        Vec3 direction =
                horizontal.normalize();


        target.push(
                direction.x
                        * WeaponStats.SLING_POCKET_KNOCKBACK,

                0.18,

                direction.z
                        * WeaponStats.SLING_POCKET_KNOCKBACK
        );
    }
}