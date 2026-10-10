package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.EnderStaffRenderer;
import com.fox.foxsweapons.config.WeaponStats;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * A stick with a judgmental green cube.
 * Right-click: blink exactly five blocks along the player's look vector,
 * provided the entire route has room for the player.
 */
public final class EnderStaffItem extends Item implements GeoItem {
    private static final double CHECK_STEP = 0.25D;
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public EnderStaffItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private EnderStaffRenderer renderer;

            @Override
            public GeoItemRenderer<EnderStaffItem> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new EnderStaffRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Absolutely no animations. The green cube simply judges.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable(
                "tooltip.foxsweapons.ender_staff.description"
        ).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(
                "tooltip.foxsweapons.ender_staff.ability"
        ).withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Vec3 start = serverPlayer.position();
        Vec3 facing = serverPlayer.getLookAngle().normalize();
        double distance = WeaponStats.ENDER_STAFF_TELEPORT_DISTANCE;
        Vec3 offset = facing.scale(distance);

        // Never blink through walls or into blocks. Test the entire player's
        // collision box along the route; reject the blink if it ever collides.
        if (!isClearRoute(serverLevel, serverPlayer, start, facing, distance)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.foxsweapons.ender_staff.blocked"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        Vec3 destination = start.add(offset);

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                start.x, start.y + 1.0D, start.z,
                24, 0.3D, 0.65D, 0.3D, 0.06D);
        serverLevel.playSound(null,
                start.x, start.y, start.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);

        serverPlayer.teleport(new TeleportTransition(
                serverLevel,
                destination,
                Vec3.ZERO,
                serverPlayer.getYRot(),
                serverPlayer.getXRot(),
                TeleportTransition.DO_NOTHING
        ));
        serverPlayer.fallDistance = 0.0F;

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + 1.0D, destination.z,
                24, 0.3D, 0.65D, 0.3D, 0.06D);
        serverLevel.playSound(null,
                destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);

        serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    private static boolean isClearRoute(
            ServerLevel level, ServerPlayer player, Vec3 start,
            Vec3 facing, double distance
    ) {
        AABB originalBox = player.getBoundingBox();
        for (double traveled = CHECK_STEP;
             traveled <= distance + 1.0E-6D;
             traveled += CHECK_STEP) {
            Vec3 offset = facing.scale(Math.min(traveled, distance));
            Vec3 candidate = start.add(offset);

            // Never force-load chunks for a teleport.
            if (!level.hasChunkAt(BlockPos.containing(candidate))) {
                return false;
            }
            if (!level.noCollision(player, originalBox.move(offset))) {
                return false;
            }
        }
        return true;
    }
}
