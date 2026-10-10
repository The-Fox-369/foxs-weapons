
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

public final class EnderStaffItem extends Item implements GeoItem {

    // =========================================================
    // CONFIGURATION
    // =========================================================

    private static final double CHECK_STEP = 0.025D;

    private static final double COLLISION_TOLERANCE = 0.10D;

    private static final double MIN_TELEPORT_DISTANCE = 0.025D;

    private static final double POSITION_EPSILON = 0.0001D;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EnderStaffItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    // =========================================================
    // GECKOLIB RENDERER
    // =========================================================

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {
        consumer.accept(new GeoRenderProvider() {

            private EnderStaffRenderer renderer;

            @Override
            public GeoItemRenderer<EnderStaffItem>
            getGeoItemRenderer() {

                if (renderer == null) {
                    renderer = new EnderStaffRenderer();
                }

                return renderer;
            }
        });
    }

    // =========================================================
    // ANIMATION
    //
    // The green cube does not blink.
    // The green cube does not rotate.
    // The green cube judges.
    // =========================================================

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        // No animations.
    }

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // TOOLTIP
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        tooltipAdder.accept(
                Component.translatable(
                        "tooltip.foxsweapons.ender_staff.description"
                ).withStyle(ChatFormatting.GRAY)
        );

        tooltipAdder.accept(
                Component.translatable(
                        "tooltip.foxsweapons.ender_staff.ability"
                ).withStyle(ChatFormatting.DARK_PURPLE)
        );
    }

    // =========================================================
    // RIGHT-CLICK TELEPORTATION
    //
    // FULL THREE-DIMENSIONAL MOVEMENT
    //
    // Looking forward -> Forward
    // Looking upward  -> Upward
    // Looking downward -> Downward
    // Looking diagonally -> Diagonal
    //
    // Maximum distance: 5 blocks.
    // =========================================================

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {

            return InteractionResult.SUCCESS;
        }

        Vec3 start = serverPlayer.position();

        // =====================================================
        // FULL 3D DIRECTION
        //
        // IMPORTANT:
        // Do not remove the Y component.
        // =====================================================

        Vec3 direction =
                serverPlayer.getLookAngle().normalize();

        double maxDistance =
                WeaponStats.ENDER_STAFF_TELEPORT_DISTANCE;

        Vec3 destination = findTeleportDestination(
                serverLevel,
                serverPlayer,
                start,
                direction,
                maxDistance
        );

        // =====================================================
        // NO VALID MOVEMENT
        //
        // No denial.
        // No chat message.
        // No sound.
        // =====================================================

        if (destination.distanceToSqr(start) <
                MIN_TELEPORT_DISTANCE * MIN_TELEPORT_DISTANCE) {

            return InteractionResult.SUCCESS;
        }

        // =====================================================
        // DEPARTURE EFFECTS
        // =====================================================

        playTeleportEffects(
                serverLevel,
                start
        );

        // =====================================================
        // TELEPORT
        // =====================================================

        serverPlayer.teleport(
                new TeleportTransition(
                        serverLevel,
                        destination,
                        Vec3.ZERO,
                        serverPlayer.getYRot(),
                        serverPlayer.getXRot(),
                        TeleportTransition.DO_NOTHING
                )
        );

        // Prevent fall damage accumulated before teleport.
        serverPlayer.fallDistance = 0.0F;

        // =====================================================
        // ARRIVAL EFFECTS
        // =====================================================

        playTeleportEffects(
                serverLevel,
                serverPlayer.position()
        );

        serverPlayer.awardStat(
                Stats.ITEM_USED.get(this)
        );

        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // CALCULATE MAXIMUM COLLISION SPAN
    //
    // We must account for the player being more than
    // one block tall.
    //
    // A one-block roof can overlap the player's
    // bounding box for longer than a one-block wall.
    //
    // This function estimates how far a one-block
    // obstruction can extend along the look vector.
    // =========================================================

    private static double calculateMaximumCollisionSpan(
            Player player,
            Vec3 direction
    ) {

        double absX = Math.abs(direction.x);
        double absY = Math.abs(direction.y);
        double absZ = Math.abs(direction.z);

        // The player's collision-box dimensions.

        double playerWidth = player.getBbWidth();
        double playerHeight = player.getBbHeight();

        // Project the player's bounding box onto
        // the direction of movement.

        double playerProjection =
                playerWidth * absX
                        + playerHeight * absY
                        + playerWidth * absZ;

        // Conservative estimate of one voxel's
        // thickness along this travel direction.

        double majorAxis = Math.max(
                absX,
                Math.max(absY, absZ)
        );

        double singleBlockSpan =
                1.0D / Math.max(majorAxis, 0.000001D);

        return playerProjection
                + singleBlockSpan
                + COLLISION_TOLERANCE;
    }

    // =========================================================
    // FIND TELEPORT DESTINATION
    //
    // 1. Travel along the full 3D look vector.
    //
    // 2. Skip a thin obstruction if clear space
    //    is found on the other side.
    //
    // 3. Stop before a thick obstruction.
    //
    // 4. Never end inside solid blocks.
    //
    // 5. Ignore grass and other non-colliding blocks.
    //
    // 6. Do not display denial messages.
    // =========================================================

    private static Vec3 findTeleportDestination(
            ServerLevel level,
            ServerPlayer player,
            Vec3 start,
            Vec3 direction,
            double maxDistance
    ) {

        AABB originalBox = player.getBoundingBox();

        double lastSafeDistance = 0.0D;

        double obstructionStart = -1.0D;

        double safeBeforeObstruction = 0.0D;

        double maxCollisionSpan =
                calculateMaximumCollisionSpan(
                        player,
                        direction
                );

        int steps = (int) Math.ceil(
                maxDistance / CHECK_STEP
        );

        // =====================================================
        // TRAVEL ALONG THE FULL THREE-DIMENSIONAL PATH
        // =====================================================

        for (int i = 1; i <= steps; i++) {

            double traveled = Math.min(
                    i * CHECK_STEP,
                    maxDistance
            );

            // The Y coordinate is preserved here.
            //
            // This allows upward and downward
            // teleportation.

            Vec3 offset =
                    direction.scale(traveled);

            AABB movedBox =
                    originalBox.move(offset);

            // =================================================
            // CHECK LOADED AREA
            // =================================================

            if (!isAreaLoaded(level, movedBox)) {

                double fallback =
                        obstructionStart >= 0.0D
                                ? safeBeforeObstruction
                                : lastSafeDistance;

                return start.add(
                        direction.scale(fallback)
                );
            }

            // =================================================
            // COLLISION CHECK
            //
            // Tall grass has no solid collision shape.
            //
            // Solid walls, ceilings and floors do.
            // =================================================

            boolean clear =
                    level.noCollision(player, movedBox);

            // =================================================
            // OBSTRUCTION
            // =================================================

            if (!clear) {

                if (obstructionStart < 0.0D) {

                    obstructionStart = traveled;

                    safeBeforeObstruction =
                            lastSafeDistance;
                }

                double obstructionSpan =
                        traveled - obstructionStart
                                + CHECK_STEP;

                // =================================================
                // TOO THICK TO PASS
                //
                // Stop immediately before the obstruction.
                // =================================================

                if (obstructionSpan > maxCollisionSpan) {

                    return start.add(
                            direction.scale(
                                    safeBeforeObstruction
                            )
                    );
                }

                continue;
            }

            // =================================================
            // CLEAR SPACE FOUND AFTER AN OBSTRUCTION
            // =================================================

            if (obstructionStart >= 0.0D) {

                double obstructionSpan =
                        traveled - obstructionStart;

                // =================================================
                // THIN OBSTRUCTION
                //
                // Continue teleporting through it.
                // =================================================

                if (obstructionSpan <= maxCollisionSpan) {

                    obstructionStart = -1.0D;

                } else {

                    return start.add(
                            direction.scale(
                                    safeBeforeObstruction
                            )
                    );
                }
            }

            // Remember the latest valid destination.

            lastSafeDistance = traveled;
        }

        // =====================================================
        // END OF RANGE
        //
        // If the destination would be inside a block,
        // stop before entering it.
        // =====================================================

        if (obstructionStart >= 0.0D) {

            return start.add(
                    direction.scale(
                            safeBeforeObstruction
                    )
            );
        }

        // Full five-block teleport if clear.

        return start.add(
                direction.scale(lastSafeDistance)
        );
    }

    // =========================================================
    // LOADED CHUNK SAFETY
    // =========================================================

    private static boolean isAreaLoaded(
            ServerLevel level,
            AABB box
    ) {

        double minX = box.minX + POSITION_EPSILON;
        double minY = box.minY + POSITION_EPSILON;
        double minZ = box.minZ + POSITION_EPSILON;

        double maxX = box.maxX - POSITION_EPSILON;
        double maxY = box.maxY - POSITION_EPSILON;
        double maxZ = box.maxZ - POSITION_EPSILON;

        BlockPos corner1 = BlockPos.containing(
                minX,
                minY,
                minZ
        );

        BlockPos corner2 = BlockPos.containing(
                maxX,
                maxY,
                maxZ
        );

        BlockPos corner3 = BlockPos.containing(
                minX,
                minY,
                maxZ
        );

        BlockPos corner4 = BlockPos.containing(
                maxX,
                minY,
                minZ
        );

        return level.hasChunkAt(corner1)
                && level.hasChunkAt(corner2)
                && level.hasChunkAt(corner3)
                && level.hasChunkAt(corner4);
    }

    // =========================================================
    // ENDER PARTICLES AND TELEPORT SOUND
    // =========================================================

    private static void playTeleportEffects(
            ServerLevel level,
            Vec3 position
    ) {

        level.sendParticles(
                ParticleTypes.PORTAL,
                position.x,
                position.y + 1.0D,
                position.z,
                32,
                0.35D,
                0.75D,
                0.35D,
                0.08D
        );

        level.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.85F,
                1.1F
        );
    }
}
