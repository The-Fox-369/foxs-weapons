package com.fox.foxsweapons.effect;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The target has just been released from a tightly bound Weighted Net.
 *
 * Rope Burns is intentionally a custom effect rather than vanilla Slowness
 * so the player sees the weapon's own status effect name. At level V the
 * -15% per level movement modifier gives the old Tier 1 net's heavy slow.
 */
public final class RopeBurnsEffect extends MobEffect {

    public RopeBurnsEffect() {
        super(MobEffectCategory.HARMFUL, 0xB65A3A);

        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(FoxsWeapons.MODID, "effect.rope_burns"),
                -0.15D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
