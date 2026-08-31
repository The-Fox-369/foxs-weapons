package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.mechanic.WeightedNetTetherManager;

import net.minecraft.server.level.ServerLevel;
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

/**
 * The bundled Weighted Net in flight. It deals zero damage.
 */
public class WeightedNetProjectile extends ThrowableItemProjectile {

    public WeightedNetProjectile(
            EntityType<? extends WeightedNetProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    public WeightedNetProjectile(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack stack
    ) {
        super(FoxsWeapons.WEIGHTED_NET_PROJECTILE.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return FoxsWeapons.WEIGHTED_NET.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);

        if (!(level() instanceof ServerLevel)) {
            return;
        }

        Entity owner = getOwner();
        Entity hit = hitResult.getEntity();

        if (owner instanceof ServerPlayer serverPlayer
                && hit instanceof LivingEntity livingTarget
                && livingTarget != owner
                && livingTarget.isAlive()) {

            WeightedNetTetherManager.attach(serverPlayer, livingTarget);
        }

        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);

        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        Entity owner = getOwner();

        if (owner == null
                || tickCount > 30
                || distanceToSqr(owner)
                > WeaponStats.WEIGHTED_NET_THROW_RANGE
                * WeaponStats.WEIGHTED_NET_THROW_RANGE) {
            discard();
        }
    }
}
