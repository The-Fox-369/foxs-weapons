
package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.mechanic.SmokeCloudManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class SmokeBombProjectile extends ThrowableItemProjectile {

    private static final int MAX_FLIGHT_TICKS = 100;

    // =========================================================
    // ENTITY REGISTRY CONSTRUCTOR
    //
    // This is what registerEntityType needs!
    // =========================================================

    public SmokeBombProjectile(
            EntityType<? extends SmokeBombProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    // =========================================================
    // THROWING CONSTRUCTOR
    // =========================================================

    public SmokeBombProjectile(
            Level level,
            LivingEntity owner,
            ItemStack stack
    ) {
        super(
                FoxsWeapons.SMOKE_BOMB_PROJECTILE.get(),
                owner,
                level,
                stack
        );
    }

    // =========================================================
    // DEFAULT ITEM
    // =========================================================

    @Override
    protected Item getDefaultItem() {
        return FoxsWeapons.SMOKE_BOMB.get();
    }

    // =========================================================
    // DETONATE ON IMPACT
    // =========================================================

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level() instanceof ServerLevel serverLevel) {

            SmokeCloudManager.deploy(
                    serverLevel,
                    result.getLocation()
            );

            discard();
        }
    }

    // =========================================================
    // PROJECTILE TIMEOUT
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel
                && tickCount > MAX_FLIGHT_TICKS) {

            discard();
        }
    }
}
