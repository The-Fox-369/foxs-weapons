package com.fox.foxsweapons.mechanic;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side ranged-lead state for Weighted Net.
 *
 * One owner may tether one target, and one target may only be tethered by
 * one owner at a time. The state intentionally stays out of the entity NBT:
 * it is a temporary combat restraint, not a permanent leash.
 */
@EventBusSubscriber(modid = FoxsWeapons.MODID)
public final class WeightedNetTetherManager {

    private static final Map<UUID, Tether> BY_OWNER = new HashMap<>();
    private static final Map<UUID, UUID> OWNER_BY_TARGET = new HashMap<>();

    private WeightedNetTetherManager() {}

    public static boolean attach(ServerPlayer owner, LivingEntity target) {
        UUID ownerId = owner.getUUID();
        UUID targetId = target.getUUID();

        UUID existingOwner = OWNER_BY_TARGET.get(targetId);
        if (existingOwner != null && !existingOwner.equals(ownerId)) {
            owner.sendOverlayMessage(
                    Component.translatable("message.foxsweapons.weighted_net.already_tethered")
            );
            return false;
        }

        remove(ownerId, false);

        BY_OWNER.put(ownerId, new Tether(owner, target));
        OWNER_BY_TARGET.put(targetId, ownerId);

        owner.sendOverlayMessage(
                Component.translatable("message.foxsweapons.weighted_net.tethered")
        );

        owner.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.LEASH_KNOT_PLACE,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        return true;
    }

    /**
     * @param applyRopeBurns true only for an intentional player release.
     */
    public static boolean release(ServerPlayer owner, boolean applyRopeBurns) {
        return remove(owner.getUUID(), applyRopeBurns);
    }

    private static boolean remove(UUID ownerId, boolean applyRopeBurns) {
        Tether tether = BY_OWNER.remove(ownerId);
        if (tether == null) {
            return false;
        }

        OWNER_BY_TARGET.remove(tether.target().getUUID());

        LivingEntity target = tether.target();
        ServerPlayer owner = tether.owner();

        if (applyRopeBurns && target.isAlive()) {
            target.addEffect(
                    new MobEffectInstance(
                            FoxsWeapons.ROPE_BURNS,
                            WeaponStats.WEIGHTED_NET_ROPE_BURNS_DURATION_TICKS,
                            WeaponStats.WEIGHTED_NET_ROPE_BURNS_AMPLIFIER
                    )
            );

            owner.sendOverlayMessage(
                    Component.translatable("message.foxsweapons.weighted_net.released")
            );

            owner.level().playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundEvents.LEASH_KNOT_BREAK,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.9F
            );
        }

        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, Tether>> iterator =
                BY_OWNER.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Tether> entry = iterator.next();
            Tether tether = entry.getValue();
            ServerPlayer owner = tether.owner();
            LivingEntity target = tether.target();

            if (owner.isRemoved()
                    || !owner.isAlive()
                    || target.isRemoved()
                    || !target.isAlive()
                    || owner.level() != target.level()) {

                OWNER_BY_TARGET.remove(target.getUUID());
                iterator.remove();
                continue;
            }

            pullTarget(owner, target);
        }
    }

    private static void pullTarget(ServerPlayer owner, LivingEntity target) {
        Vec3 delta = owner.position().subtract(target.position());
        double distance = delta.length();

        if (distance <= WeaponStats.WEIGHTED_NET_TETHER_SOFT_RANGE
                || distance < 0.001D) {
            return;
        }

        double excess = distance - WeaponStats.WEIGHTED_NET_TETHER_SOFT_RANGE;
        double strength;

        if (distance >= WeaponStats.WEIGHTED_NET_TETHER_HARD_RANGE) {
            strength = Math.min(0.48D, 0.18D + excess * 0.035D);
        } else {
            strength = Math.min(0.18D, 0.035D + excess * 0.025D);
        }

        Vec3 pull = delta.normalize().scale(strength);

        target.setDeltaMovement(target.getDeltaMovement().add(pull));
        target.hurtMarked = true;
    }

    private record Tether(ServerPlayer owner, LivingEntity target) {}
}
