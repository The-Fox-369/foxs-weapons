package com.fox.foxsweapons.effect;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class RopeBurnsEffect extends MobEffect {

    public RopeBurnsEffect() {
        super(MobEffectCategory.HARMFUL, 0xA86D3F);

        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(
                        FoxsWeapons.MODID,
                        "effect.rope_burns"
                ),
                -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}