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
public final class WeightedNetTooltip {

    private WeightedNetTooltip() {
    }

    @SubscribeEvent
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        ItemStack stack =
                event.getItemStack();

        if (!stack.is(FoxsWeapons.WEIGHTED_NET.get())) {
            return;
        }

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
    }
}