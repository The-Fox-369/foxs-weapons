package com.fox.foxsweapons.client;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(
        modid = FoxsWeapons.MODID,
        value = Dist.CLIENT
)
public final class WeaponTooltips {

    private WeaponTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        ItemStack stack =
                event.getItemStack();

        // =====================================================
        // VOLCANO HAMMER
        // =====================================================

        if (stack.is(FoxsWeapons.VOLCANO_HAMMER.get())) {

            event.getToolTip().add(
                    1,
                    Component.translatable(
                            "tooltip.foxsweapons.brand"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );

            event.getToolTip().add(
                    2,
                    Component.translatable(
                            "tooltip.foxsweapons.volcano_hammer.description"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            event.getToolTip().add(
                    3,
                    Component.translatable(
                            "tooltip.foxsweapons.volcano_hammer.ability"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );

            event.getToolTip().add(
                    4,
                    Component.translatable(
                            "tooltip.foxsweapons.volcano_hammer.passive"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );

            return;
        }

        // =====================================================
        // BLUNDERBUSS
        // =====================================================

        if (stack.is(FoxsWeapons.BLUNDERBUSS.get())) {

            event.getToolTip().add(
                    1,
                    Component.translatable(
                            "tooltip.foxsweapons.brand"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );

            event.getToolTip().add(
                    2,
                    Component.translatable(
                            "tooltip.foxsweapons.blunderbuss.description"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            event.getToolTip().add(
                    3,
                    Component.translatable(
                            "tooltip.foxsweapons.blunderbuss.single"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );

            event.getToolTip().add(
                    4,
                    Component.translatable(
                            "tooltip.foxsweapons.blunderbuss.burst"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );

            event.getToolTip().add(
                    5,
                    Component.translatable(
                            "tooltip.foxsweapons.blunderbuss.ammo"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );

            return;
        }

        // =====================================================
        // SOUL REAPER
        // =====================================================

        if (stack.is(FoxsWeapons.SOUL_REAPER.get())) {

            event.getToolTip().add(
                    1,
                    Component.translatable(
                            "tooltip.foxsweapons.brand"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );

            event.getToolTip().add(
                    2,
                    Component.translatable(
                            "tooltip.foxsweapons.soul_reaper.description"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            event.getToolTip().add(
                    3,
                    Component.translatable(
                            "tooltip.foxsweapons.soul_reaper.ability"
                    ).withStyle(
                            ChatFormatting.DARK_PURPLE
                    )
            );

            event.getToolTip().add(
                    4,
                    Component.translatable(
                            "tooltip.foxsweapons.soul_reaper.recipe_hint"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );

            return;
        }

        // =====================================================
        // TEMPEST BOW
        // =====================================================

        if (stack.is(FoxsWeapons.TEMPEST_BOW.get())) {

            event.getToolTip().add(
                    1,
                    Component.translatable(
                            "tooltip.foxsweapons.brand"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );

            event.getToolTip().add(
                    2,
                    Component.translatable(
                            "tooltip.foxsweapons.tempest_bow.description"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            event.getToolTip().add(
                    3,
                    Component.translatable(
                            "tooltip.foxsweapons.tempest_bow.ability"
                    ).withStyle(
                            ChatFormatting.AQUA
                    )
            );

            event.getToolTip().add(
                    4,
                    Component.translatable(
                            "tooltip.foxsweapons.tempest_bow.damage"
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );

            event.getToolTip().add(
                    5,
                    Component.translatable(
                            "tooltip.foxsweapons.tempest_bow.draw"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );
        }

        // =====================================================
// WEIGHTED NET
// =====================================================

        if (stack.is(FoxsWeapons.WEIGHTED_NET.get())) {

            event.getToolTip().add(
                    1,
                    Component.translatable(
                            "tooltip.foxsweapons.brand"
                    ).withStyle(
                            ChatFormatting.DARK_RED
                    )
            );

            event.getToolTip().add(
                    2,
                    Component.translatable(
                            "tooltip.foxsweapons.weighted_net.description"
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            event.getToolTip().add(
                    3,
                    Component.translatable(
                            "tooltip.foxsweapons.weighted_net.throw"
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );

            event.getToolTip().add(
                    4,
                    Component.translatable(
                            "tooltip.foxsweapons.weighted_net.release"
                    ).withStyle(
                            ChatFormatting.DARK_GRAY
                    )
            );

            return;
        }
    }
}