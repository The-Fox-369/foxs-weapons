package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.VolcanoHammerRenderer;
import com.fox.foxsweapons.config.WeaponStats;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class VolcanoHammerItem extends Item implements GeoItem {

    /*
     * GeckoLib animation cache.
     */
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public VolcanoHammerItem(Properties properties) {
        super(properties);

        GeoItem.registerSyncedAnimatable(this);
    }

    /*
     * =========================================================
     * GECKOLIB RENDERER
     * =========================================================
     */

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer) {

        consumer.accept(new GeoRenderProvider() {

            private VolcanoHammerRenderer renderer;

            @Override
            public GeoItemRenderer<VolcanoHammerItem>
            getGeoItemRenderer() {

                if (this.renderer == null) {
                    this.renderer =
                            new VolcanoHammerRenderer();
                }

                return this.renderer;
            }
        });
    }

    /*
     * =========================================================
     * GECKOLIB ANIMATION CONTROLLER
     * =========================================================
     *
     * GeckoLib renders the hammer.
     *
     * The custom player animation system handles
     * the actual full-body swing.
     */

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(
                new AnimationController<>(
                        "controller",
                        0,
                        state -> PlayState.STOP
                )
        );
    }

    /*
     * =========================================================
     * PASSIVE EFFECTS WHILE HELD
     * =========================================================
     *
     * - Fire Resistance
     * - Lava particles
     *
     * These only activate while the hammer
     * is in the main hand or off hand.
     */

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel level,
            Entity owner,
            EquipmentSlot slot) {

        /*
         * Only living entities can hold the hammer.
         */
        if (!(owner instanceof LivingEntity living)) {
            return;
        }

        /*
         * Do nothing if the hammer is merely
         * sitting somewhere in the inventory.
         */
        if (slot != EquipmentSlot.MAINHAND
                && slot != EquipmentSlot.OFFHAND) {

            return;
        }

        /*
         * =====================================================
         * FIRE RESISTANCE
         * =====================================================
         *
         * Refresh every 10 ticks.
         *
         * Duration = 30 ticks.
         *
         * This means:
         *
         * Hammer held:
         * effect constantly refreshed
         *
         * Hammer unequipped:
         * effect disappears shortly afterwards
         *
         * The final three false values hide:
         *
         * - ambient appearance
         * - particles
         * - HUD icon
         */

        if (owner.tickCount % 10 == 0) {

            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.FIRE_RESISTANCE,
                            30,
                            0,
                            false,
                            false,
                            false
                    )
            );
        }

        /*
         * =====================================================
         * PASSIVE LAVA PARTICLES
         * =====================================================
         *
         * Spawn roughly once every 6 ticks.
         *
         * Around 3 particles per second.
         */

        if (owner.tickCount % 6 != 0) {
            return;
        }

        spawnHeldLavaParticle(
                level,
                living,
                slot
        );
    }

    /*
     * =========================================================
     * HELD HAMMER LAVA PARTICLES
     * =========================================================
     *
     * IMPORTANT:
     *
     * We use the player's BODY rotation here,
     * not where their camera is looking.
     *
     * The old version used getEyePosition()
     * and getLookAngle(), which made the lava
     * appear to leak from the player's body.
     */

    private void spawnHeldLavaParticle(
        ServerLevel level,
        LivingEntity living,
        EquipmentSlot slot) {

    /*
     * Which arm is holding the hammer?
     */
    HumanoidArm arm =
            slot == EquipmentSlot.MAINHAND
                    ? living.getMainArm()
                    : living.getMainArm().getOpposite();

    double side =
            arm == HumanoidArm.RIGHT
                    ? 1.0
                    : -1.0;

    /*
     * Player BODY rotation.
     */
    double yaw =
            Math.toRadians(
                    living.getYRot()
            );

    /*
     * Player forward direction.
     */
    Vec3 forward =
            new Vec3(
                    -Math.sin(yaw),
                    0.0,
                    Math.cos(yaw)
            );

    /*
     * Player RIGHT direction.
     *
     * IMPORTANT:
     * The previous version had this backwards,
     * which is why right-hand particles appeared
     * on the LEFT side of your torso.
     */
    Vec3 right =
            new Vec3(
                    -Math.cos(yaw),
                    0.0,
                    -Math.sin(yaw)
            );

    /*
     * Approximate hammer position.
     *
     * Smaller sideways offset than before:
     *
     * old = 0.55
     * new = 0.38
     */
    Vec3 particlePos =
            living.position()
                    .add(
                            0.0,
                            1.00,
                            0.0
                    )
                    .add(
                            right.scale(
                                    0.38 * side
                            )
                    )
                    .add(
                            forward.scale(
                                    0.10
                            )
                    );

    /*
     * Regular molten drip.
     */
    level.sendParticles(
            ParticleTypes.DRIPPING_LAVA,

            particlePos.x,
            particlePos.y,
            particlePos.z,

            1,

            0.025,
            0.04,
            0.025,

            0.01
    );

    /*
     * Occasional brighter lava pop.
     */
    if (living.tickCount % 24 == 0) {

        level.sendParticles(
                ParticleTypes.LAVA,

                particlePos.x,
                particlePos.y,
                particlePos.z,

                1,

                0.02,
                0.025,
                0.02,

                0.01
        );
    }
}

    /*
     * =========================================================
     * RIGHT-CLICK VOLCANO SMASH
     * =========================================================
     *
     * Right-click the TOP of a block.
     *
     * Creates:
     *
     * F F F
     * F . F
     * F F F
     *
     * F = fire
     * . = safe center
     */

    @Override
    public InteractionResult useOn(
            UseOnContext context) {

        Level level =
                context.getLevel();

        Player player =
                context.getPlayer();

        /*
         * No player = no ability.
         */
        if (player == null) {
            return InteractionResult.PASS;
        }

        /*
         * Only activate when clicking
         * the TOP of a block.
         */
        if (context.getClickedFace()
                != Direction.UP) {

            return InteractionResult.PASS;
        }

        ItemStack stack =
                context.getItemInHand();

        /*
         * Prevent spam while cooling down.
         */
        if (player
                .getCooldowns()
                .isOnCooldown(stack)) {

            return InteractionResult.FAIL;
        }

        /*
         * World changes happen server-side.
         *
         * Client only needs to know the interaction
         * was successful.
         */
        if (!(level instanceof ServerLevel serverLevel)) {

            return InteractionResult.SUCCESS;
        }

        /*
         * The clicked block is the floor.
         *
         * Fire is placed one block above it.
         */
        BlockPos center =
                context
                        .getClickedPos()
                        .above();

        /*
         * Create the 3x3 fire ring.
         */
        int firesPlaced =
                createFireRing(
                        serverLevel,
                        center,
                        context.getHorizontalDirection()
                );

        /*
         * If no valid fire locations existed,
         * don't waste durability or cooldown.
         */
        if (firesPlaced == 0) {

            return InteractionResult.FAIL;
        }

        /*
         * Trigger the player's custom
         * hammer swing animation.
         */
        player.swing(
                context.getHand(),
                true
        );

        /*
         * Heavy impact sound.
         */
        serverLevel.playSound(
                null,

                center,

                SoundEvents.MACE_SMASH_GROUND_HEAVY,

                SoundSource.PLAYERS,

                1.35F,

                0.80F
        );

        /*
         * Volcano impact particles.
         */
        spawnSmashParticles(
                serverLevel,
                center
        );

        /*
         * Six-second cooldown.
         */
        player
                .getCooldowns()
                .addCooldown(
                        stack,
                        WeaponStats.VOLCANO_HAMMER_SMASH_COOLDOWN_TICKS
                );

        /*
         * Smash costs 3 durability.
         */
        stack.hurtAndBreak(
                WeaponStats.VOLCANO_HAMMER_SMASH_DURABILITY_COST,
                player,
                context.getHand()
        );

        return InteractionResult.SUCCESS;
    }

    /*
     * =========================================================
     * CREATE 3x3 FIRE RING
     * =========================================================
     */

    private int createFireRing(
            ServerLevel level,
            BlockPos center,
            Direction playerDirection) {

        int placed = 0;

        /*
         * Loop through:
         *
         * x = -1, 0, 1
         * z = -1, 0, 1
         *
         * This creates a 3x3 area.
         */
        for (int x = -1; x <= 1; x++) {

            for (int z = -1; z <= 1; z++) {

                /*
                 * Skip the center block.
                 *
                 * F F F
                 * F . F
                 * F F F
                 */
                if (x == 0 && z == 0) {
                    continue;
                }

                BlockPos firePos =
                        center.offset(
                                x,
                                0,
                                z
                        );

                BlockState currentState =
                        level.getBlockState(
                                firePos
                        );

                /*
                 * Remove replaceable stuff such as:
                 *
                 * tall grass
                 * flowers
                 * etc.
                 *
                 * so the fire can actually spawn.
                 */
                if (!currentState.isAir()
                        && currentState.canBeReplaced()) {

                    level.setBlock(
                            firePos,
                            Blocks.AIR.defaultBlockState(),
                            11
                    );
                }

                /*
                 * Check Minecraft's normal
                 * fire placement rules.
                 */
                if (!BaseFireBlock.canBePlacedAt(
                        level,
                        firePos,
                        playerDirection)) {

                    continue;
                }

                /*
                 * Place the fire.
                 */
                level.setBlock(
                        firePos,
                        BaseFireBlock.getState(
                                level,
                                firePos
                        ),
                        11
                );

                placed++;

                /*
                 * Small flame burst on each
                 * newly-created fire block.
                 */
                level.sendParticles(
                        ParticleTypes.FLAME,

                        firePos.getX() + 0.5,
                        firePos.getY() + 0.15,
                        firePos.getZ() + 0.5,

                        4,

                        0.18,
                        0.08,
                        0.18,

                        0.02
                );
            }
        }

        return placed;
    }

    /*
     * =========================================================
     * VOLCANO SMASH PARTICLES
     * =========================================================
     */

    private void spawnSmashParticles(
            ServerLevel level,
            BlockPos center) {

        double x =
                center.getX() + 0.5;

        double y =
                center.getY() + 0.1;

        double z =
                center.getZ() + 0.5;

        /*
         * =====================================================
         * LAVA CHUNKS
         * =====================================================
         */
        level.sendParticles(
                ParticleTypes.LAVA,

                x,
                y,
                z,

                14,

                0.95,
                0.15,
                0.95,

                0.12
        );

        /*
         * =====================================================
         * FLAMES
         * =====================================================
         */
        level.sendParticles(
                ParticleTypes.FLAME,

                x,
                y,
                z,

                32,

                1.10,
                0.22,
                1.10,

                0.08
        );

        /*
         * =====================================================
         * HEAVY SMOKE
         * =====================================================
         */
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,

                x,
                y + 0.15,
                z,

                14,

                0.90,
                0.18,
                0.90,

                0.05
        );
    }

    /*
     * =========================================================
     * GECKOLIB CACHE
     * =========================================================
     */

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return this.cache;
    }
}
