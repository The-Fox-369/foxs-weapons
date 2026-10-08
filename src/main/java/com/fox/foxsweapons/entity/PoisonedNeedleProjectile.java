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
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
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

    private static final Identifier THATS_A_LOTTA_DAMAGE =
            Identifier.fromNamespaceAndPath(
                    FoxsWeapons.MODID,
                    "thats_a_lotta_damage"
            );

    private static final String THATS_A_LOTTA_DAMAGE_CRITERION =
            "hit_with_poisoned_needle";


    // =========================================================
    // SYNCHED DATA
    // =========================================================

    private static final EntityDataAccessor<Boolean> DATA_IN_GROUND =
            SynchedEntityData.defineId(
                    PoisonedNeedleProjectile.class,
                    EntityDataSerializers.BOOLEAN
            );


    // =========================================================
    // PIERCING
    // =========================================================

    private final Set<Integer> piercedEntityIds =
            new HashSet<>();

    private boolean piercedDuringCurrentTick =
            false;

    private static final int MAX_PIERCES_PER_TICK =
            64;


    // =========================================================
    // EMBEDDED STATE
    // =========================================================

    private BlockPos stuckBlockPos =
            null;

    private int inGroundTime =
            0;

    private static final int PICKUP_DELAY_TICKS =
            5;

    private static final int IN_GROUND_DESPAWN_TICKS =
            1200;


    // =========================================================
    // CONSTRUCTORS
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
    }


    // =========================================================
    // SYNCHED DATA
    // =========================================================

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {

        super.defineSynchedData(
                builder
        );

        builder.define(
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

    public boolean isNeedleInGround() {

        return entityData.get(
                DATA_IN_GROUND
        );
    }


    private void setNeedleInGround(
            boolean value
    ) {

        entityData.set(
                DATA_IN_GROUND,
                value
        );
    }


    // =========================================================
    // TICK
    // =========================================================

    @Override
    public void tick() {

        // -----------------------------------------------------
        // ALREADY EMBEDDED
        // -----------------------------------------------------

        if (isNeedleInGround()) {

            tickInGround();

            return;
        }


        Vec3 tickStart =
                position();

        piercedDuringCurrentTick =
                false;


        /*
         * Vanilla handles gravity, inertia, rotation and
         * the FIRST collision.
         */
        super.tick();


        if (!isAlive()) {

            return;
        }


        if (isNeedleInGround()) {

            return;
        }


        /*
         * No entity collision interrupted movement.
         */
        if (!piercedDuringCurrentTick) {

            return;
        }


        Vec3 fullVelocity =
                getDeltaMovement();


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


        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        // -----------------------------------------------------
        // BLOCK WAS DESTROYED
        // -----------------------------------------------------

        if (
                stuckBlockPos != null
                        && serverLevel
                        .getBlockState(
                                stuckBlockPos
                        )
                        .isAir()
        ) {

            dropAfterBlockRemoved(
                    serverLevel
            );

            return;
        }


        // -----------------------------------------------------
        // PLAYER PICKUP
        // -----------------------------------------------------

        /*
         * Do NOT rely only on vanilla playerTouch().
         *
         * Embedded projectiles are tiny and we want pickup
         * to behave consistently.
         *
         * So every server tick after the tiny pickup delay,
         * check the normal Minecraft pickup area ourselves.
         */
        if (inGroundTime
                >= PICKUP_DELAY_TICKS) {

            for (
                    ServerPlayer player :
                    serverLevel.getEntitiesOfClass(
                            ServerPlayer.class,

                            getBoundingBox()
                                    .inflate(
                                            1.0D,
                                            0.5D,
                                            1.0D
                                    )
                    )
            ) {

                if (player.isSpectator()) {

                    continue;
                }


                if (tryPickup(
                        player
                )) {

                    return;
                }
            }
        }


        // -----------------------------------------------------
        // DESPAWN
        // -----------------------------------------------------

        if (inGroundTime
                >= IN_GROUND_DESPAWN_TICKS) {

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
                        && !isNeedleInGround()
                        && processedHits
                        < MAX_PIERCES_PER_TICK
        ) {

            Vec3 current =
                    position();


            Vec3 remaining =
                    intendedEnd.subtract(
                            current
                    );


            if (remaining.lengthSqr()
                    <= 0.000001D) {

                break;
            }


            /*
             * ProjectileUtil checks through the current
             * delta movement.
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
            // NOTHING ELSE IN PATH
            // -------------------------------------------------

            if (nextHit.getType()
                    == HitResult.Type.MISS) {

                setPos(
                        intendedEnd
                );

                break;
            }


            // -------------------------------------------------
            // MOVE TO COLLISION
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
             * Something actually changed our trajectory.
             */
            if (deflection
                    != ProjectileDeflection.NONE) {

                return;
            }


            if (!isAlive()) {

                return;
            }


            /*
             * Block impact embedded us.
             */
            if (isNeedleInGround()) {

                return;
            }


            // -------------------------------------------------
            // ENTITY HIT
            // -------------------------------------------------

            if (nextHit.getType()
                    == HitResult.Type.ENTITY) {

                processedHits++;


                /*
                 * Safety against repeatedly colliding with
                 * the same target at the same location.
                 */
                if (!piercedDuringCurrentTick) {

                    break;
                }
            }
        }


        /*
         * Restore real flight velocity after temporarily
         * using the remaining-distance vectors.
         */
        if (
                isAlive()
                        && !isNeedleInGround()
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

        if (entity == getOwner()) {

            return false;
        }


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
         * Remember this target on both logical sides so the
         * same Needle cannot repeatedly collide with it.
         */
        if (!piercedEntityIds.add(
                hit.getId()
        )) {

            return;
        }


        piercedDuringCurrentTick =
                true;


        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }


        if (!(hit
                instanceof LivingEntity target)) {

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
         * Still physically pierce an invulnerable/rejected
         * target, but do not apply gameplay effects.
         */
        if (!damaged) {

            return;
        }


        // =====================================================
        // CHALLENGE
        // =====================================================

        if (owner
                instanceof ServerPlayer player) {

            awardThatsALottaDamage(
                    serverLevel,
                    player
            );
        }


        // =====================================================
        // TOXIN
        // =====================================================

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
         * The Needle keeps going.
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
         * At this point updateRotation() has already used the
         * incoming velocity, so xRot/yRot preserve the angle
         * at which the Needle struck the block.
         */


        Vec3 movement =
                getDeltaMovement();


        Vec3 impact =
                hitResult.getLocation();


        /*
         * Keep a tiny portion of the Needle outside the block.
         */
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
         * Freeze projectile WITHOUT changing its xRot/yRot.
         *
         * The renderer will use those stored rotations,
         * meaning the Needle keeps its actual impact angle.
         */
        setDeltaMovement(
                Vec3.ZERO
        );


        setNoGravity(
                true
        );


        setNeedleInGround(
                true
        );


        stuckBlockPos =
                hitResult.getBlockPos();


        inGroundTime =
                0;
    }


    // =========================================================
    // VANILLA PLAYER TOUCH
    // =========================================================

    @Override
    public void playerTouch(
            Player player
    ) {

        /*
         * Keep vanilla proximity pickup as a second path.
         *
         * tickInGround() also actively checks nearby players,
         * so even if this doesn't fire reliably for a tiny
         * projectile, recovery still works.
         */

        if (!isNeedleInGround()) {

            return;
        }


        if (level().isClientSide()) {

            return;
        }


        if (inGroundTime
                < PICKUP_DELAY_TICKS) {

            return;
        }


        tryPickup(
                player
        );
    }


    // =========================================================
    // PICKUP
    // =========================================================

    private boolean tryPickup(
            Player player
    ) {

        if (!isNeedleInGround()) {

            return false;
        }


        if (player.isSpectator()) {

            return false;
        }


        // -----------------------------------------------------
        // CREATIVE
        // -----------------------------------------------------

        /*
         * Creative did not consume the original Needle.
         *
         * Touching the embedded projectile simply removes it.
         */
        if (player
                .getAbilities()
                .instabuild) {

            player.take(
                    this,
                    1
            );


            discard();


            return true;
        }


        // -----------------------------------------------------
        // SURVIVAL
        // -----------------------------------------------------

        ItemStack recoveredNeedle =
                getItem()
                        .copyWithCount(
                                1
                        );


        /*
         * Full inventory?
         *
         * Leave it embedded instead of deleting the Needle.
         */
        if (!player
                .getInventory()
                .add(
                        recoveredNeedle
                )) {

            return false;
        }


        player.take(
                this,
                1
        );


        discard();


        return true;
    }


    // =========================================================
    // SUPPORT BLOCK REMOVED
    // =========================================================

    private void dropAfterBlockRemoved(
            ServerLevel serverLevel
    ) {

        /*
         * If the block holding the Needle disappears,
         * convert it into a normal dropped item.
         *
         * We intentionally don't care whether it was
         * originally thrown in Creative. Creative duplication
         * is irrelevant; Survival must never lose the weapon.
         */
        spawnAtLocation(
                serverLevel,

                getItem()
                        .copyWithCount(
                                1
                        )
        );


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
                isNeedleInGround()
        );


        output.putInt(
                "NeedleInGroundTime",
                inGroundTime
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


        setNeedleInGround(
                loadedInGround
        );


        inGroundTime =
                input.getIntOr(
                        "NeedleInGroundTime",
                        0
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