package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.mechanic.WeightedNetTetherManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class WeightedNetProjectile extends ThrowableItemProjectile {

    private static final int MAX_LIFETIME_TICKS = 40;

    public WeightedNetProjectile(
            EntityType<? extends WeightedNetProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    public WeightedNetProjectile(
            Level level,
            LivingEntity owner,
            ItemStack stack
    ) {
        super(
                FoxsWeapons.WEIGHTED_NET_PROJECTILE.get(),
                owner,
                level,
                stack
        );
    }

    @Override
    protected Item getDefaultItem() {
        return FoxsWeapons.WEIGHTED_NET.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()
                && tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(
            EntityHitResult hitResult
    ) {
        super.onHitEntity(hitResult);

        if (level().isClientSide()) {
            return;
        }

        Entity owner = getOwner();
        Entity hit = hitResult.getEntity();

        if (owner instanceof ServerPlayer player
                && hit instanceof LivingEntity living
                && living != player
                && living.isAlive()) {

            WeightedNetTetherManager.attach(
                    player,
                    living
            );
        }

        discard();
    }

    @Override
    protected void onHitBlock(
            BlockHitResult hitResult
    ) {
        super.onHitBlock(hitResult);

        if (!level().isClientSide()) {
            discard();
        }
    }
}