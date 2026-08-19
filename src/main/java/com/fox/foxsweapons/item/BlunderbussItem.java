package com.fox.foxsweapons.item;

import com.fox.foxsweapons.client.renderer.BlunderbussRenderer;
import com.fox.foxsweapons.config.WeaponStats;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;

import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.level.ClipContext;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class BlunderbussItem
        extends Item
        implements GeoItem {

    /*
     * =========================================================
     * GECKOLIB
     * =========================================================
     */

    private static final RawAnimation SHOOT_ANIMATION =
            RawAnimation
                    .begin()
                    .thenPlay("shoot");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public BlunderbussItem(Properties properties) {

        super(properties);

        /*
         * Required because we trigger "shoot"
         * from the server.
         */
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

        consumer.accept(
                new GeoRenderProvider() {

                    private BlunderbussRenderer renderer;

                    @Override
                    public GeoItemRenderer<BlunderbussItem>
                    getGeoItemRenderer() {

                        if (this.renderer == null) {

                            this.renderer =
                                    new BlunderbussRenderer();
                        }

                        return this.renderer;
                    }
                }
        );
    }

    /*
     * =========================================================
     * GECKOLIB SHOOT ANIMATION
     * =========================================================
     */

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(
                new AnimationController<>(
                        "shoot_controller",
                        0,
                        state -> PlayState.STOP
                )

                        .triggerableAnim(
                                "shoot",
                                SHOOT_ANIMATION
                        )
        );
    }

    /*
     * =========================================================
     * FIRE
     * =========================================================
     *
     * burst = false:
     * LEFT CLICK
     * 1 accurate pellet
     *
     * burst = true:
     * RIGHT CLICK
     * 4 spread pellets
     */

    public static void fire(
            ServerPlayer player,
            boolean burst) {

        ItemStack stack =
                player.getMainHandItem();

        /*
         * SECURITY CHECK:
         *
         * Never trust the packet.
         *
         * The server confirms the player really
         * has the Blunderbuss in their main hand.
         */
        if (!(stack.getItem()
                instanceof BlunderbussItem blunderbuss)) {

            return;
        }

        /*
         * Don't fire during cooldown.
         */
        if (player
                .getCooldowns()
                .isOnCooldown(stack)) {

            return;
        }

        /*
         * One trigger pull consumes ONE Iron Nugget.
         *
         * Single:
         * 1 nugget -> 1 pellet
         *
         * Burst:
         * 1 nugget -> 4 pieces of shot
         */
        if (!consumeIronNugget(player)) {

            player.sendOverlayMessage(
                    Component.literal(
                            "Need an Iron Nugget"
                    )
            );

            return;
        }

        ServerLevel level =
                (ServerLevel) player.level();

        /*
         * Trigger your Blockbench animation.
         */
        blunderbuss.triggerAnim(
                player,

                GeoItem.getOrAssignId(
                        stack,
                        level
                ),

                "shoot_controller",
                "shoot"
        );

        /*
         * IMPORTANT:
         *
         * This is NOT the vanilla attack click.
         *
         * The client event already cancelled that.
         *
         * We manually trigger this swing ONLY so
         * state.attackTime drives our firearm
         * player recoil pose.
         *
         * The item has SwingAnimationType.NONE,
         * so the vanilla punch stays dead.
         */
        player.swing(
                InteractionHand.MAIN_HAND,
                true
        );

        /*
         * BOOM.
         */
        playFireEffects(
                level,
                player,
                burst
        );

        /*
         * Fire the procedural pellet rays.
         */
        if (burst) {

            firePellets(
                    level,
                    player,
                    4,
                    WeaponStats.BLUNDERBUSS_BURST_SPREAD,
                    WeaponStats.BLUNDERBUSS_BURST_PELLET_DAMAGE
            );

        } else {

            firePellets(
                    level,
                    player,
                    1,
                    WeaponStats.BLUNDERBUSS_SINGLE_SPREAD,
                    WeaponStats.BLUNDERBUSS_SINGLE_DAMAGE
            );
        }

        /*
         * Cooldown.
         */
        player
                .getCooldowns()
                .addCooldown(
                        stack,

                        burst
                                ? WeaponStats.BLUNDERBUSS_BURST_COOLDOWN
                                : WeaponStats.BLUNDERBUSS_SINGLE_COOLDOWN
                );

        /*
         * One durability per trigger pull.
         */
        stack.hurtAndBreak(
                1,
                player,
                InteractionHand.MAIN_HAND
        );
    }

    /*
     * =========================================================
     * AMMO
     * =========================================================
     */

    private static boolean consumeIronNugget(
            ServerPlayer player) {

        /*
         * Creative mode = infinite ammo.
         */
        if (player
                .getAbilities()
                .instabuild) {

            return true;
        }

        for (
                int slot = 0;
                slot < player
                        .getInventory()
                        .getContainerSize();
                slot++
        ) {

            ItemStack ammo =
                    player
                            .getInventory()
                            .getItem(slot);

            if (!ammo.is(
                    Items.IRON_NUGGET)) {

                continue;
            }

            ammo.shrink(1);

            player
                    .getInventory()
                    .setChanged();

            return true;
        }

        return false;
    }

    /*
     * =========================================================
     * PROCEDURAL PELLETS
     * =========================================================
     *
     * NO ENTITY.
     * NO MODEL.
     * NO TEXTURE.
     *
     * Each pellet is simply:
     *
     * direction
     * -> block ray
     * -> entity intersection
     * -> particle tracer
     * -> damage
     */

    private static void firePellets(
            ServerLevel level,
            ServerPlayer player,
            int pelletCount,
            double spread,
            float pelletDamage) {

        Vec3 eye =
                player.getEyePosition();

        Vec3 look =
                player
                        .getLookAngle()
                        .normalize();

        /*
         * We collect hits first.
         *
         * This matters for the 4-shot.
         *
         * Minecraft's damage invulnerability
         * would otherwise cause multiple pellets
         * hitting on the exact same tick to fight
         * each other.
         *
         * So:
         *
         * 3 pellets hit zombie
         *
         * becomes:
         *
         * 3 x pelletDamage
         *
         * in ONE damage call.
         */
        Map<LivingEntity, Integer> hits =
                new HashMap<>();

        for (
                int pellet = 0;
                pellet < pelletCount;
                pellet++
        ) {

            Vec3 direction =
                    addSpread(
                            level,
                            look,
                            spread
                    );

            Vec3 intendedEnd =
                    eye.add(
                            direction.scale(
                                    WeaponStats.BLUNDERBUSS_RANGE
                            )
                    );

            /*
             * Stop at blocks first.
             */
            BlockHitResult blockHit =
                    level.clip(
                            new ClipContext(
                                    eye,
                                    intendedEnd,
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    player
                            )
                    );

            Vec3 rayEnd =
                    blockHit.getType()
                            == HitResult.Type.MISS

                            ? intendedEnd

                            : blockHit
                            .getLocation();

            /*
             * Find closest living entity before
             * that block.
             */
            PelletHit entityHit =
                    findEntityHit(
                            level,
                            player,
                            eye,
                            rayEnd
                    );

            if (entityHit != null) {

                rayEnd =
                        entityHit.location();

                hits.merge(
                        entityHit.entity(),
                        1,
                        Integer::sum
                );

                spawnEntityImpact(
                        level,
                        rayEnd
                );

            } else if (
                    blockHit.getType()
                            != HitResult.Type.MISS
            ) {

                spawnBlockImpact(
                        level,
                        rayEnd
                );
            }

            /*
             * Visible procedural pellet trail.
             */
            spawnTracer(
                    level,
                    eye,
                    rayEnd
            );
        }

        /*
         * Apply aggregated damage.
         */
        for (
                Map.Entry<LivingEntity, Integer>
                        entry : hits.entrySet()
        ) {

            LivingEntity target =
                    entry.getKey();

            int pelletsHit =
                    entry.getValue();

            float damage =
                    pelletDamage
                            * pelletsHit;

            boolean damaged =
                    target.hurtServer(
                            level,

                            player
                                    .damageSources()
                                    .playerAttack(
                                            player
                                    ),

                            damage
                    );

            if (!damaged) {
                continue;
            }

            /*
             * Shotgun-ish knockback.
             *
             * More pellets hitting =
             * stronger push.
             */
            Vec3 pushDirection =
                    target
                            .position()
                            .subtract(
                                    player.position()
                            );

            if (pushDirection
                    .horizontalDistanceSqr()
                    > 0.0001) {

                pushDirection =
                        pushDirection
                                .normalize();

                double strength =
                        0.18
                                + 0.10
                                * pelletsHit;

                target.push(
                        pushDirection.x
                                * strength,

                        0.05
                                + 0.025
                                * pelletsHit,

                        pushDirection.z
                                * strength
                );
            }
        }
    }

    /*
     * =========================================================
     * RANDOM SPREAD
     * =========================================================
     */

    private static Vec3 addSpread(
            ServerLevel level,
            Vec3 look,
            double spread) {

        if (spread <= 0.0) {
            return look;
        }

        double x =
                (
                        level.getRandom().nextDouble()
                                - 0.5
                )
                        * spread;

        double y =
                (
                        level.getRandom().nextDouble()
                                - 0.5
                )
                        * spread;

        double z =
                (
                        level.getRandom().nextDouble()
                                - 0.5
                )
                        * spread;

        return look
                .add(
                        x,
                        y,
                        z
                )
                .normalize();
    }

    /*
     * =========================================================
     * ENTITY RAYCAST
     * =========================================================
     */

    private static PelletHit findEntityHit(
            ServerLevel level,
            ServerPlayer shooter,
            Vec3 start,
            Vec3 end) {

        AABB searchBox =
                new AABB(
                        start,
                        end
                )
                        .inflate(1.0);

        LivingEntity closest =
                null;

        Vec3 closestLocation =
                null;

        double closestDistance =
                start.distanceToSqr(
                        end
                );

        for (
                Entity entity :
                level.getEntities(
                        shooter,
                        searchBox,

                        candidate ->
                                candidate
                                        instanceof LivingEntity

                                        && candidate
                                        .isPickable()

                                        && !candidate
                                        .isSpectator()
                )
        ) {

            LivingEntity living =
                    (LivingEntity) entity;

            if (!living.isAlive()) {
                continue;
            }

            AABB hitBox =
                    living
                            .getBoundingBox()
                            .inflate(0.20);

            Optional<Vec3> intersection =
                    hitBox.clip(
                            start,
                            end
                    );

            if (intersection.isEmpty()) {
                continue;
            }

            Vec3 hitLocation =
                    intersection.get();

            double distance =
                    start.distanceToSqr(
                            hitLocation
                    );

            if (distance >= closestDistance) {
                continue;
            }

            closestDistance =
                    distance;

            closest =
                    living;

            closestLocation =
                    hitLocation;
        }

        if (closest == null) {
            return null;
        }

        return new PelletHit(
                closest,
                closestLocation
        );
    }

    /*
     * =========================================================
     * TRACER
     * =========================================================
     */

    private static void spawnTracer(
            ServerLevel level,
            Vec3 start,
            Vec3 end) {

        Vec3 delta =
                end.subtract(start);

        double distance =
                delta.length();

        if (distance <= 0.001) {
            return;
        }

        Vec3 direction =
                delta.normalize();

        /*
         * One tiny smoke particle roughly
         * every 1.25 blocks.
         */
        for (
                double travelled = 0.75;
                travelled < distance;
                travelled += 1.25
        ) {

            Vec3 point =
                    start.add(
                            direction.scale(
                                    travelled
                            )
                    );

            level.sendParticles(
                    ParticleTypes.SMOKE,

                    point.x,
                    point.y,
                    point.z,

                    1,

                    0.0,
                    0.0,
                    0.0,

                    0.0
            );
        }
    }

    /*
     * =========================================================
     * MUZZLE EFFECTS
     * =========================================================
     */

    private static void playFireEffects(
            ServerLevel level,
            ServerPlayer player,
            boolean burst) {

        Vec3 look =
                player
                        .getLookAngle()
                        .normalize();

        /*
         * Approximate muzzle position.
         *
         * Later we can move this if the model
         * visually needs it.
         */
        Vec3 muzzle =
                player
                        .getEyePosition()
                        .add(
                                look.scale(
                                        0.85
                                )
                        )
                        .add(
                                0.0,
                                -0.18,
                                0.0
                        );

        level.playSound(
                null,

                player.blockPosition(),

                SoundEvents.GENERIC_EXPLODE.value(),

                SoundSource.PLAYERS,

                burst
                        ? 1.45F
                        : 1.15F,

                burst
                        ? 0.78F
                        : 0.90F
        );

        /*
         * Bright muzzle flash.
         */
        level.sendParticles(
                ParticleTypes.FLAME,

                muzzle.x,
                muzzle.y,
                muzzle.z,

                burst
                        ? 9
                        : 5,

                0.08,
                0.08,
                0.08,

                0.04
        );

        /*
         * FLINTLOCK SMOKE™
         */
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,

                muzzle.x,
                muzzle.y,
                muzzle.z,

                burst
                        ? 12
                        : 7,

                0.16,
                0.12,
                0.16,

                0.025
        );
    }

    private static void spawnEntityImpact(
            ServerLevel level,
            Vec3 position) {

        level.sendParticles(
                ParticleTypes.CRIT,

                position.x,
                position.y,
                position.z,

                4,

                0.08,
                0.08,
                0.08,

                0.10
        );
    }

    private static void spawnBlockImpact(
            ServerLevel level,
            Vec3 position) {

        level.sendParticles(
                ParticleTypes.POOF,

                position.x,
                position.y,
                position.z,

                3,

                0.06,
                0.06,
                0.06,

                0.02
        );
    }

    /*
     * Tiny internal result object.
     */
    private record PelletHit(
            LivingEntity entity,
            Vec3 location) {
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