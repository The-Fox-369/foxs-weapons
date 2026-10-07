package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.advancements.AdvancementHolder;

import net.minecraft.core.BlockPos;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import net.minecraft.world.entity.projectile.throwableitemprojectile
        .ThrowableItemProjectile;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;


public class PoisonedNeedleProjectile
        extends ThrowableItemProjectile {

    // =========================================================
    // ADVANCEMENT
    // =========================================================

    private static final Identifier
            THATS_A_LOTTA_DAMAGE =

            Identifier.fromNamespaceAndPath(
                    FoxsWeapons.MODID,
                    "thats_a_lotta_damage"
            );


    private static final String
            THATS_A_LOTTA_DAMAGE_CRITERION =

            "hit_with_poisoned_needle";


    // =========================================================
    // SYNCHED DATA
    // =========================================================

    /**
     * We need the client to know when the projectile has
     * embedded itself in terrain so it stops simulating
     * movement too.
     */
    private static final EntityDataAccessor<Boolean>
            DATA_IN_GROUND =

            SynchedEntityData.defineId(
                    PoisonedNeedleProjectile.class,
                    EntityDataSerializers.BOOLEAN
            );


    // =========================================================
    // PIERCING
    // =========================================================

    /**
     * Every entity may only be struck once by this exact
     * Needle.
     */
    private final Set<Integer> piercedEntityIds =
            new HashSet<>();


    /**
     * Vanilla ThrowableProjectile ends the current tick's
     * movement at the first entity collision.
     *
     * When this becomes true, our custom continuation logic
     * travels through the rest of that tick.
     */
    private boolean piercedDuringCurrentTick =
            false;


    /**
     * Safety cap ONLY for one game tick.
     *
     * This does not limit total lifetime piercing.
     */
    private static final int MAX_PIERCES_PER_TICK =
            64;


    // =========================================================
    // EMBEDDED STATE
    // =========================================================

    private BlockPos stuckBlockPos =
            null;


    private int inGroundTime =
            0;


    /**
     * Prevent immediate pickup on the exact frame the Needle
     * enters the ground.
     */
    private static final int PICKUP_DELAY_TICKS =
            5;


    /**
     * Same general idea as vanilla arrows:
     *
     * an abandoned embedded projectile should not live
     * forever and eventually fill the world with entities.
     *
     * 1200 ticks = 60 seconds.
     */
    private static final int IN_GROUND_DESPAWN_TICKS =
            1200;


    // =========================================================
    // RECOVERY
    // =========================================================

    /**
     * Survival:
     * true
     *
     * Creative:
     * false
     *
     * Creative retains the original Needle, so picking
     * the embedded projectile back up would duplicate it.
     */
    private boolean recoverable =
            false;


    // =========================================================
    // ENTITY CONSTRUCTOR
    // =========================================================

    public PoisonedNeedleProjectile(
            EntityType<? extends PoisonedNeedleProjectile> type,
            Level level
    ) {

        super(
                type,
                level
        );
    }


    // =========================================================
    // THROWN CONSTRUCTOR
    // =========================================================

    public PoisonedNeedleProjectile(
            Level level,
            LivingEntity owner,
            ItemStack stack
    ) {

        super(
                FoxsWeapons
                        .POISONED_NEEDLE_PROJECTILE
                        .get(),

                owner,

                level,

                stack
        );


        recoverable =
                !(owner instanceof Player player)
                        || !player
                        .getAbilities()
                        .instabuild;
    }


    // =========================================================
    // SYNCHED DATA
    // =========================================================

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder entityData
    ) {

        super.defineSynchedData(
                entityData
        );


        entityData.define(
                DATA_IN_GROUND,
                false
        );
    }


    // =========================================================
    // DEFAULT ITEM
    // =========================================================

    @Override
    protected Item getDefaultItem() {

        return FoxsWeapons
                .POISONED_NEEDLE
                .get();
    }


    // =========================================================
    // EMBEDDED STATE
    // =========================================================

    private boolean isInGround() {

        return entityData.get(
                DATA_IN_GROUND
        );
    }


    private void setInGround(
            boolean inGround
    ) {

        entityData.set(
                DATA_IN_GROUND,
                inGround
        );
    }


    // =========================================================
    // TICK
    // =========================================================

    @Override
    public void tick() {

        // -----------------------------------------------------
        // ALREADY STUCK IN TERRAIN
        // -----------------------------------------------------

        if (isInGround()) {

            tickInGround();

            return;
        }


        /*
         * Remember where the vanilla movement step began.
         */
        Vec3 tickStart =
                position();


        piercedDuringCurrentTick =
                false;


        /*
         * Vanilla handles:
         *
         * - gravity
         * - inertia
         * - first collision
         * - rotation
         * - world effects
         */
        super.tick();


        if (!isAlive()) {

            return;
        }


        /*
         * We may have just struck terrain.
         */
        if (isInGround()) {

            return;
        }


        /*
         * No entity was pierced during this tick.
         *
         * Vanilla already completed the complete movement.
         */
        if (!piercedDuringCurrentTick) {

            return;
        }


        /*
         * Velocity after vanilla's gravity and inertia.
         */
        Vec3 fullVelocity =
                getDeltaMovement();


        /*
         * Where the Needle WOULD have ended if the first
         * entity collision had not interrupted movement.
         */
        Vec3 intendedEnd =
                tickStart.add(
                        fullVelocity
                );


        continuePiercingFlight(
                intendedEnd,
                fullVelocity
        );
    }


    // =========================================================
    // IN-GROUND TICK
    // =========================================================

    private void tickInGround() {

        inGroundTime++;


        /*
         * If somebody destroys the block containing the
         * Needle, convert the projectile into an actual
         * dropped Needle instead of leaving it floating.
         */
        if (
                !level().isClientSide()
                        && stuckBlockPos != null
                        && level()
                        .getBlockState(
                                stuckBlockPos
                        )
                        .isAir()
        ) {

            dropAfterBlockRemoved();

            return;
        }


        /*
         * Don't leave thousands of forgotten Needle
         * entities around forever.
         */
        if (
                !level().isClientSide()
                        && inGroundTime
                        >= IN_GROUND_DESPAWN_TICKS
        ) {

            discard();
        }
    }


    // =========================================================
    // CONTINUE THROUGH TARGETS
    // =========================================================

    private void continuePiercingFlight(
            Vec3 intendedEnd,
            Vec3 fullVelocity
    ) {

        int processedHits =
                0;


        while (
                isAlive()
                        && !isInGround()
                        && processedHits
                        < MAX_PIERCES_PER_TICK
        ) {

            Vec3 current =
                    position();


            Vec3 remaining =
                    intendedEnd.subtract(
                            current
                    );


            /*
             * Reached the original end of the tick.
             */
            if (remaining.lengthSqr()
                    <= 0.000001D) {

                break;
            }


            /*
             * ProjectileUtil traces through the current
             * delta movement, so temporarily make the
             * delta equal the untravelled part.
             */
            setDeltaMovement(
                    remaining
            );


            HitResult nextHit =
                    ProjectileUtil
                            .getHitResultOnMoveVector(
                                    this,
                                    this::canHitEntity
                            );


            // -------------------------------------------------
            // NOTHING ELSE HIT
            // -------------------------------------------------

            if (nextHit.getType()
                    == HitResult.Type.MISS) {

                setPos(
                        intendedEnd
                );

                break;
            }


            // -------------------------------------------------
            // MOVE TO COLLISION POINT
            // -------------------------------------------------

            setPos(
                    nextHit.getLocation()
            );


            piercedDuringCurrentTick =
                    false;


            ProjectileDeflection deflection =
                    hitTargetOrDeflectSelf(
                            nextHit
                    );


            /*
             * Deflection has replaced our trajectory.
             *
             * Do not restore the old forward velocity.
             */
            if (deflection
                    != ProjectileDeflection.NONE) {

                return;
            }


            if (!isAlive()) {

                return;
            }


            /*
             * We reached terrain.
             *
             * onHitBlock() has now embedded us.
             */
            if (isInGround()) {

                return;
            }


            // -------------------------------------------------
            // ENTITY HIT
            // -------------------------------------------------

            if (nextHit.getType()
                    == HitResult.Type.ENTITY) {

                processedHits++;


                /*
                 * If the hit failed to register as pierced,
                 * stop rather than repeatedly colliding with
                 * the exact same entity forever.
                 */
                if (!piercedDuringCurrentTick) {

                    break;
                }
            }
        }


        /*
         * Collision scanning temporarily changed velocity
         * to smaller remainder vectors.
         *
         * Restore real flight velocity for next tick.
         */
        if (
                isAlive()
                        && !isInGround()
        ) {

            setDeltaMovement(
                    fullVelocity
            );
        }
    }


    // =========================================================
    // ENTITY COLLISION FILTER
    // =========================================================

    @Override
    protected boolean canHitEntity(
            Entity entity
    ) {

        /*
         * Never stab the thrower.
         */
        if (entity == getOwner()) {

            return false;
        }


        /*
         * Never strike the same entity twice with the same
         * projectile.
         */
        if (piercedEntityIds.contains(
                entity.getId()
        )) {

            return false;
        }


        return super.canHitEntity(
                entity
        );
    }


    // =========================================================
    // ENTITY HIT
    // =========================================================

    @Override
    protected void onHitEntity(
            EntityHitResult hitResult
    ) {

        super.onHitEntity(
                hitResult
        );


        Entity hit =
                hitResult.getEntity();


        /*
         * Store collision on both client and server.
         *
         * Both sides need to ignore this entity after
         * the Needle has passed through.
         */
        if (!piercedEntityIds.add(
                hit.getId()
        )) {

            return;
        }


        piercedDuringCurrentTick =
                true;


        /*
         * Damage/effects only belong on the server.
         */
        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        if (!(hit instanceof LivingEntity target)) {

            return;
        }


        if (!target.isAlive()) {

            return;
        }


        Entity owner =
                getOwner();


        // =====================================================
        // DIRECT DAMAGE
        // =====================================================

        boolean damaged =
                target.hurtServer(
                        serverLevel,

                        damageSources().thrown(
                                this,
                                owner
                        ),

                        WeaponStats
                                .POISONED_NEEDLE_DIRECT_DAMAGE
                );


        /*
         * The projectile still physically pierces an entity
         * even when damage is rejected.
         *
         * But no damage means:
         *
         * - no advancement
         * - no Toxin
         */
        if (!damaged) {

            return;
        }


        // =====================================================
        // CHALLENGE
        // =====================================================

        if (owner instanceof ServerPlayer player) {

            awardThatsALottaDamage(
                    serverLevel,
                    player
            );
        }


        // =====================================================
        // TOXIN
        // =====================================================

        /*
         * Direct damage could already have killed it.
         */
        if (!target.isAlive()) {

            return;
        }


        target.addEffect(
                new MobEffectInstance(
                        FoxsWeapons.TOXIN,

                        MobEffectInstance
                                .INFINITE_DURATION,

                        0,

                        false,

                        true,

                        false
                ),

                owner
        );


        /*
         * NO discard().
         *
         * Continue through the victim.
         */
    }


    // =========================================================
    // CHALLENGE
    // =========================================================

    private static void awardThatsALottaDamage(
            ServerLevel level,
            ServerPlayer player
    ) {

        AdvancementHolder advancement =
                level
                        .getServer()
                        .getAdvancements()
                        .get(
                                THATS_A_LOTTA_DAMAGE
                        );


        if (advancement == null) {

            return;
        }


        player
                .getAdvancements()
                .award(
                        advancement,
                        THATS_A_LOTTA_DAMAGE_CRITERION
                );
    }


    // =========================================================
    // BLOCK HIT
    // =========================================================

    @Override
    protected void onHitBlock(
            BlockHitResult hitResult
    ) {

        super.onHitBlock(
                hitResult
        );


        /*
         * Keep the Needle just OUTSIDE the collision surface
         * rather than burying its model completely inside
         * the block.
         */
        Vec3 movement =
                getDeltaMovement();


        Vec3 impact =
                hitResult.getLocation();


        if (movement.lengthSqr()
                > 0.000001D) {

            setPos(
                    impact.subtract(
                            movement
                                    .normalize()
                                    .scale(
                                            0.05D
                                    )
                    )
            );

        } else {

            setPos(
                    impact
            );
        }


        /*
         * THIS IS THE IMPORTANT PART:
         *
         * do not discard
         * do not spawn an ItemEntity
         *
         * physically remain embedded in terrain.
         */
        setDeltaMovement(
                Vec3.ZERO
        );


        setNoGravity(
                true
        );


        setInGround(
                true
        );


        stuckBlockPos =
                hitResult.getBlockPos();


        inGroundTime =
                0;
    }


    // =========================================================
    // PLAYER RECOVERY
    // =========================================================

    @Override
    public void playerTouch(
            Player player
    ) {

        /*
         * Only embedded Needles are recoverable.
         */
        if (!isInGround()) {

            return;
        }


        if (level().isClientSide()) {

            return;
        }


        if (inGroundTime
                < PICKUP_DELAY_TICKS) {

            return;
        }


        /*
         * Creative throwers kept their original item.
         *
         * Don't create a duplicate.
         */
        if (!recoverable) {

            return;
        }


        ItemStack recoveredNeedle =
                getItem()
                        .copyWithCount(
                                1
                        );


        /*
         * Only remove the projectile if the player's
         * inventory successfully accepted the Needle.
         */
        if (player
                .getInventory()
                .add(
                        recoveredNeedle
                )) {

            player.take(
                    this,
                    1
            );


            discard();
        }
    }


    // =========================================================
    // SUPPORT BLOCK REMOVED
    // =========================================================

    private void dropAfterBlockRemoved() {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        if (recoverable) {

            spawnAtLocation(
                    serverLevel,
                    getItem()
                            .copyWithCount(
                                    1
                            )
            );
        }


        discard();
    }


    // =========================================================
    // SAVE DATA
    // =========================================================

    @Override
    protected void addAdditionalSaveData(
            ValueOutput output
    ) {

        super.addAdditionalSaveData(
                output
        );


        output.putBoolean(
                "NeedleInGround",
                isInGround()
        );


        output.putInt(
                "NeedleInGroundTime",
                inGroundTime
        );


        output.putBoolean(
                "NeedleRecoverable",
                recoverable
        );


        if (stuckBlockPos != null) {

            output.putBoolean(
                    "NeedleHasStuckBlock",
                    true
            );


            output.putInt(
                    "NeedleStuckX",
                    stuckBlockPos.getX()
            );


            output.putInt(
                    "NeedleStuckY",
                    stuckBlockPos.getY()
            );


            output.putInt(
                    "NeedleStuckZ",
                    stuckBlockPos.getZ()
            );

        } else {

            output.putBoolean(
                    "NeedleHasStuckBlock",
                    false
            );
        }
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    @Override
    protected void readAdditionalSaveData(
            ValueInput input
    ) {

        super.readAdditionalSaveData(
                input
        );


        boolean loadedInGround =
                input.getBooleanOr(
                        "NeedleInGround",
                        false
                );


        setInGround(
                loadedInGround
        );


        inGroundTime =
                input.getIntOr(
                        "NeedleInGroundTime",
                        0
                );


        recoverable =
                input.getBooleanOr(
                        "NeedleRecoverable",
                        false
                );


        boolean hasStuckBlock =
                input.getBooleanOr(
                        "NeedleHasStuckBlock",
                        false
                );


        if (hasStuckBlock) {

            stuckBlockPos =
                    new BlockPos(
                            input.getIntOr(
                                    "NeedleStuckX",
                                    0
                            ),

                            input.getIntOr(
                                    "NeedleStuckY",
                                    0
                            ),

                            input.getIntOr(
                                    "NeedleStuckZ",
                                    0
                            )
                    );

        } else {

            stuckBlockPos =
                    null;
        }


        if (loadedInGround) {

            setDeltaMovement(
                    Vec3.ZERO
            );


            setNoGravity(
                    true
            );
        }
    }
}