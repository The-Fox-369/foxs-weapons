package com.fox.foxsweapons.entity;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.config.WeaponStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public class ChakramProjectile
        extends ThrowableItemProjectile {

    // =========================================================
    // MODE
    // =========================================================

    private enum Mode {
        HUNTING,
        COLLECTING,
        RETURNING
    }

    private enum NavigationResult {
        MOVING,
        UNREACHABLE,
        SEARCHING
    }

    private enum RouteResult {
        REACHABLE,
        UNREACHABLE,
        SEARCH_LIMIT
    }

    private Mode mode =
            Mode.HUNTING;

    // =========================================================
    // MISSION
    // =========================================================

    private Vec3 huntOrigin =
            Vec3.ZERO;

    private boolean initialized =
            false;

    private boolean returnItemToOwner =
            false;

    /**
     * FINITE target snapshot.
     *
     * Things spawned after the Chakram launches do not
     * magically extend its hunting mission forever.
     */
    private final List<UUID> missionTargets =
            new ArrayList<>();

    /**
     * Mission targets that are:
     *
     * - touched
     * - gone
     * - outside the operating area
     * - genuinely unreachable without noclip
     */
    private final Set<UUID> completedTargets =
            new HashSet<>();

    private UUID currentTargetId =
            null;

    /**
     * If a path search reaches the CPU guard rather than
     * proving "no route exists", the target remains pending.
     */
    private int nextSelectionAttemptTick =
            0;

    // =========================================================
    // LOOT
    // =========================================================

    private final List<ItemStack> carriedItems =
            new ArrayList<>();

    private int carriedExperience =
            0;

    private int lootQuietTicks =
            0;

    // =========================================================
    // PATHFINDING
    // =========================================================

    private List<Vec3> currentPath =
            List.of();

    private int waypointIndex =
            0;

    private int lastPathBuildTick =
            Integer.MIN_VALUE;

    // =========================================================
    // ENTITY CONSTRUCTOR
    // =========================================================

    public ChakramProjectile(
            EntityType<? extends ChakramProjectile> type,
            Level level
    ) {

        super(
                type,
                level
        );

        setNoGravity(
                true
        );
    }

    // =========================================================
    // THROWN CONSTRUCTOR
    // =========================================================

    public ChakramProjectile(
            Level level,
            LivingEntity owner,
            ItemStack stack,
            boolean returnItemToOwner
    ) {

        super(
                FoxsWeapons.CHAKRAM_PROJECTILE.get(),
                owner,
                level,
                stack
        );

        this.returnItemToOwner =
                returnItemToOwner;

        setNoGravity(
                true
        );
    }

    // =========================================================
    // DEFAULT ITEM
    // =========================================================

    @Override
    protected Item getDefaultItem() {

        return FoxsWeapons
                .CHAKRAM
                .get();
    }

    // =========================================================
    // TICK
    // =========================================================

    @Override
    public void tick() {

        if (level()
                instanceof ServerLevel serverLevel) {

            initializeIfNeeded(
                    serverLevel
            );

            serverTick(
                    serverLevel
            );
        }

        /*
         * Minecraft performs actual physical movement
         * and collision detection here.
         */
        super.tick();

        if (level()
                instanceof ServerLevel serverLevel) {

            handleNearbyPhysicalContacts(
                    serverLevel
            );
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private void initializeIfNeeded(
            ServerLevel serverLevel
    ) {

        if (initialized) {
            return;
        }

        initialized =
                true;

        huntOrigin =
                position();

        snapshotMissionTargets(
                serverLevel
        );

        /*
         * No five-second thinking pose.
         *
         * Start choosing a route immediately.
         */
        nextSelectionAttemptTick =
                tickCount;

        resetNavigation();
    }

    // =========================================================
    // SNAPSHOT MISSION
    // =========================================================

    private void snapshotMissionTargets(
            ServerLevel serverLevel
    ) {

        double radius =
                WeaponStats
                        .CHAKRAM_HUNT_RADIUS;

        AABB searchArea =
                new AABB(
                        huntOrigin.x - radius,
                        huntOrigin.y - radius,
                        huntOrigin.z - radius,

                        huntOrigin.x + radius,
                        huntOrigin.y + radius,
                        huntOrigin.z + radius
                );

        Entity owner =
                getOwner();

        List<Entity> entities =
                serverLevel.getEntities(
                        this,
                        searchArea,
                        entity -> {

                            if (entity == this
                                    || entity == owner
                                    || entity.isRemoved()) {

                                return false;
                            }

                            return entity
                                    .position()
                                    .distanceToSqr(
                                            huntOrigin
                                    )
                                    <= radius * radius;
                        }
                );

        /*
         * Start with nearby targets.
         *
         * Later target selection still checks actual
         * reachability before committing.
         */
        entities.sort(
                Comparator.comparingDouble(
                        this::distanceToSqr
                )
        );

        for (Entity entity : entities) {

            missionTargets.add(
                    entity.getUUID()
            );
        }
    }

    // =========================================================
    // SERVER STATE MACHINE
    // =========================================================

    private void serverTick(
            ServerLevel serverLevel
    ) {

        switch (mode) {

            case HUNTING ->
                    tickHunting(
                            serverLevel
                    );

            case COLLECTING ->
                    tickCollecting(
                            serverLevel
                    );

            case RETURNING ->
                    tickReturning(
                            serverLevel
                    );
        }
    }

    // =========================================================
    // HUNTING
    // =========================================================

    private void tickHunting(
            ServerLevel serverLevel
    ) {

        Entity target =
                resolveCurrentTarget(
                        serverLevel
                );

        /*
         * Selected target vanished or left the mission area.
         */
        if (currentTargetId != null
                && (
                target == null
                        || !isInsideHuntArea(
                        target.position()
                )
        )) {

            finishCurrentTarget();

            target =
                    null;
        }

        // -----------------------------------------------------
        // SELECT NEXT REACHABLE TARGET
        // -----------------------------------------------------

        if (currentTargetId == null) {

            if (tickCount
                    < nextSelectionAttemptTick) {

                setDeltaMovement(
                        Vec3.ZERO
                );

                return;
            }

            SelectionResult selection =
                    selectNextMissionTarget(
                            serverLevel
                    );

            if (selection
                    == SelectionResult.NONE_LEFT) {

                beginCollecting();

                return;
            }

            if (selection
                    == SelectionResult.WAIT_FOR_SEARCH) {

                nextSelectionAttemptTick =
                        tickCount
                                + WeaponStats
                                .CHAKRAM_PATH_RECALC_INTERVAL_TICKS;

                setDeltaMovement(
                        Vec3.ZERO
                );

                return;
            }

            target =
                    resolveCurrentTarget(
                            serverLevel
                    );
        }

        if (target == null) {
            return;
        }

        // -----------------------------------------------------
        // NAVIGATE
        // -----------------------------------------------------

        NavigationResult navigation =
                navigateTo(
                        serverLevel,

                        target
                                .getBoundingBox()
                                .getCenter()
                );

        if (navigation
                == NavigationResult.UNREACHABLE) {

            /*
             * World changed after target selection.
             *
             * It is no longer physically reachable,
             * therefore it is no longer a valid target.
             */
            finishCurrentTarget();
        }
    }

    // =========================================================
    // TARGET SELECTION
    // =========================================================

    private enum SelectionResult {
        SELECTED,
        NONE_LEFT,
        WAIT_FOR_SEARCH
    }

    private SelectionResult selectNextMissionTarget(
            ServerLevel serverLevel
    ) {

        List<Entity> candidates =
                new ArrayList<>();

        Entity owner =
                getOwner();

        for (UUID uuid : missionTargets) {

            if (completedTargets
                    .contains(uuid)) {

                continue;
            }

            Entity entity =
                    serverLevel.getEntity(
                            uuid
                    );

            if (entity == null
                    || entity.isRemoved()
                    || entity == this
                    || entity == owner
                    || !isInsideHuntArea(
                    entity.position()
            )) {

                completedTargets.add(
                        uuid
                );

                continue;
            }

            candidates.add(
                    entity
            );
        }

        if (candidates.isEmpty()) {

            return SelectionResult.NONE_LEFT;
        }

        candidates.sort(
                Comparator.comparingDouble(
                        this::distanceToSqr
                )
        );

        boolean searchLimitEncountered =
                false;

        for (Entity candidate : candidates) {

            Vec3 destination =
                    candidate
                            .getBoundingBox()
                            .getCenter();

            RouteResult route =
                    prepareRoute(
                            serverLevel,
                            destination
                    );

            if (route
                    == RouteResult.REACHABLE) {

                currentTargetId =
                        candidate.getUUID();

                return SelectionResult.SELECTED;
            }

            if (route
                    == RouteResult.UNREACHABLE) {

                /*
                 * This is the rule:
                 *
                 * if the Chakram cannot get there without
                 * noclip, it simply isn't a target.
                 */
                completedTargets.add(
                        candidate.getUUID()
                );

                continue;
            }

            /*
             * CPU guard was reached.
             *
             * That does NOT prove the target is unreachable.
             */
            searchLimitEncountered =
                    true;
        }

        return searchLimitEncountered
                ? SelectionResult.WAIT_FOR_SEARCH
                : SelectionResult.NONE_LEFT;
    }

    // =========================================================
    // COLLECTION PHASE
    // =========================================================

    private void beginCollecting() {

        mode =
                Mode.COLLECTING;

        currentTargetId =
                null;

        lootQuietTicks =
                0;

        nextSelectionAttemptTick =
                tickCount;

        resetNavigation();
    }

    private void tickCollecting(
            ServerLevel serverLevel
    ) {

        Entity target =
                resolveCurrentTarget(
                        serverLevel
                );

        if (currentTargetId != null
                && (
                target == null
                        || !isCollectible(
                        target
                )
                        || !isInsideHuntArea(
                        target.position()
                )
        )) {

            finishCurrentTarget();

            target =
                    null;
        }

        // -----------------------------------------------------
        // LOOK FOR LOOT
        // -----------------------------------------------------

        if (currentTargetId == null) {

            if (tickCount
                    < nextSelectionAttemptTick) {

                setDeltaMovement(
                        Vec3.ZERO
                );

                return;
            }

            SelectionResult result =
                    selectReachableLoot(
                            serverLevel
                    );

            if (result
                    == SelectionResult.SELECTED) {

                lootQuietTicks =
                        0;

                target =
                        resolveCurrentTarget(
                                serverLevel
                        );

            } else if (result
                    == SelectionResult.WAIT_FOR_SEARCH) {

                nextSelectionAttemptTick =
                        tickCount
                                + WeaponStats
                                .CHAKRAM_PATH_RECALC_INTERVAL_TICKS;

                setDeltaMovement(
                        Vec3.ZERO
                );

                return;

            } else {

                lootQuietTicks++;

                setDeltaMovement(
                        Vec3.ZERO
                );

                if (lootQuietTicks
                        >= WeaponStats
                        .CHAKRAM_LOOT_QUIET_TICKS) {

                    beginReturn();
                }

                return;
            }
        }

        if (target == null) {
            return;
        }

        NavigationResult navigation =
                navigateTo(
                        serverLevel,

                        target
                                .getBoundingBox()
                                .getCenter()
                );

        if (navigation
                == NavigationResult.UNREACHABLE) {

            completedTargets.add(
                    target.getUUID()
            );

            currentTargetId =
                    null;

            resetNavigation();
        }
    }

    // =========================================================
    // LOOT SELECTION
    // =========================================================

    private SelectionResult selectReachableLoot(
            ServerLevel serverLevel
    ) {

        double radius =
                WeaponStats
                        .CHAKRAM_HUNT_RADIUS;

        AABB searchArea =
                new AABB(
                        huntOrigin.x - radius,
                        huntOrigin.y - radius,
                        huntOrigin.z - radius,

                        huntOrigin.x + radius,
                        huntOrigin.y + radius,
                        huntOrigin.z + radius
                );

        List<Entity> loot =
                serverLevel.getEntities(
                        this,
                        searchArea,
                        entity ->
                                isCollectible(entity)
                                        && !entity.isRemoved()
                                        && !completedTargets.contains(
                                        entity.getUUID()
                                )
                                        && isInsideHuntArea(
                                        entity.position()
                                )
                );

        if (loot.isEmpty()) {

            return SelectionResult.NONE_LEFT;
        }

        loot.sort(
                Comparator.comparingDouble(
                        this::distanceToSqr
                )
        );

        boolean searchLimitEncountered =
                false;

        for (Entity entity : loot) {

            RouteResult result =
                    prepareRoute(
                            serverLevel,

                            entity
                                    .getBoundingBox()
                                    .getCenter()
                    );

            if (result
                    == RouteResult.REACHABLE) {

                currentTargetId =
                        entity.getUUID();

                return SelectionResult.SELECTED;
            }

            if (result
                    == RouteResult.UNREACHABLE) {

                completedTargets.add(
                        entity.getUUID()
                );

                continue;
            }

            searchLimitEncountered =
                    true;
        }

        return searchLimitEncountered
                ? SelectionResult.WAIT_FOR_SEARCH
                : SelectionResult.NONE_LEFT;
    }

    // =========================================================
    // RETURN
    // =========================================================

    private void beginReturn() {

        mode =
                Mode.RETURNING;

        currentTargetId =
                null;

        setDeltaMovement(
                Vec3.ZERO
        );

        resetNavigation();
    }

    private void tickReturning(
            ServerLevel serverLevel
    ) {

        Entity owner =
                getOwner();

        if (!(owner
                instanceof ServerPlayer player)
                || owner.isRemoved()) {

            setDeltaMovement(
                    Vec3.ZERO
            );

            return;
        }

        NavigationResult navigation =
                navigateTo(
                        serverLevel,

                        player
                                .getBoundingBox()
                                .getCenter()
                );

        /*
         * Closed house?
         *
         * Fine.
         *
         * The Chakram waits outside and retries the route.
         *
         * The second you emerge:
         *
         * RISE MY ARMY.
         */
        if (navigation
                != NavigationResult.MOVING) {

            setDeltaMovement(
                    Vec3.ZERO
            );
        }
    }

    // =========================================================
    // RESOLVE TARGET
    // =========================================================

    private Entity resolveCurrentTarget(
            ServerLevel serverLevel
    ) {

        if (currentTargetId == null) {
            return null;
        }

        Entity target =
                serverLevel.getEntity(
                        currentTargetId
                );

        if (target == null
                || target.isRemoved()
                || target == this
                || target == getOwner()) {

            return null;
        }

        return target;
    }

    // =========================================================
    // COMPLETE CURRENT TARGET
    // =========================================================

    private void finishCurrentTarget() {

        if (currentTargetId != null) {

            completedTargets.add(
                    currentTargetId
            );
        }

        currentTargetId =
                null;

        nextSelectionAttemptTick =
                tickCount;

        resetNavigation();
    }

    // =========================================================
    // ENTITY HIT
    // =========================================================

    @Override
    protected void onHitEntity(
            EntityHitResult hitResult
    ) {

        if (!(level()
                instanceof ServerLevel serverLevel)) {

            return;
        }

        handlePhysicalContact(
                serverLevel,
                hitResult.getEntity()
        );
    }

    // =========================================================
    // BLOCK HIT
    // =========================================================

    @Override
    protected void onHitBlock(
            BlockHitResult hitResult
    ) {

        /*
         * Walls are not death.
         *
         * Walls mean:
         *
         * "calculate another fucking route."
         */
        setDeltaMovement(
                Vec3.ZERO
        );

        resetNavigation();
    }

    // =========================================================
    // CONTACT CHECK
    // =========================================================

    private void handleNearbyPhysicalContacts(
            ServerLevel serverLevel
    ) {

        if (isRemoved()) {
            return;
        }

        AABB contactArea =
                getBoundingBox()
                        .inflate(
                                0.08D
                        );

        List<Entity> touching =
                serverLevel.getEntities(
                        this,
                        contactArea,
                        entity ->
                                entity != this
                                        && !entity.isRemoved()
                                        && contactArea.intersects(
                                        entity.getBoundingBox()
                                )
                );

        for (Entity entity : touching) {

            handlePhysicalContact(
                    serverLevel,
                    entity
            );

            if (isRemoved()) {
                return;
            }
        }
    }

    // =========================================================
    // PHYSICAL CONTACT
    // =========================================================

    private void handlePhysicalContact(
            ServerLevel serverLevel,
            Entity entity
    ) {

        if (entity == this
                || entity.isRemoved()) {

            return;
        }

        Entity owner =
                getOwner();

        // -----------------------------------------------------
        // RETURN TO OWNER
        // -----------------------------------------------------

        if (mode == Mode.RETURNING
                && entity == owner
                && owner instanceof ServerPlayer player) {

            completeReturn(
                    serverLevel,
                    player
            );

            return;
        }

        /*
         * Thrower is never a combat target.
         */
        if (entity == owner) {
            return;
        }

        UUID uuid =
                entity.getUUID();

        // -----------------------------------------------------
        // ITEM
        // -----------------------------------------------------

        if (entity
                instanceof ItemEntity itemEntity) {

            collectItem(
                    serverLevel,
                    itemEntity
            );

            completedTargets.add(
                    uuid
            );

            if (uuid.equals(
                    currentTargetId
            )) {

                finishCurrentTarget();
            }

            lootQuietTicks =
                    0;

            return;
        }

        // -----------------------------------------------------
        // XP
        // -----------------------------------------------------

        if (entity
                instanceof ExperienceOrb experienceOrb) {

            collectExperience(
                    serverLevel,
                    experienceOrb
            );

            completedTargets.add(
                    uuid
            );

            if (uuid.equals(
                    currentTargetId
            )) {

                finishCurrentTarget();
            }

            lootQuietTicks =
                    0;

            return;
        }

        // -----------------------------------------------------
        // GENERIC ENTITY
        // -----------------------------------------------------

        if (mode != Mode.HUNTING) {
            return;
        }

        /*
         * FINITE MISSION.
         *
         * A newly spawned random entity is ignored unless
         * it was part of the original target snapshot.
         */
        if (!missionTargets.contains(
                uuid
        )) {

            return;
        }

        if (completedTargets.contains(
                uuid
        )) {

            return;
        }

        hitEntity(
                serverLevel,
                entity
        );

        completedTargets.add(
                uuid
        );

        if (uuid.equals(
                currentTargetId
        )) {

            finishCurrentTarget();
        }
    }

    // =========================================================
    // DAMAGE
    // =========================================================

    private void hitEntity(
            ServerLevel serverLevel,
            Entity target
    ) {

        Entity owner =
                getOwner();

        if (!(owner
                instanceof ServerPlayer player)) {

            return;
        }

        boolean damaged =
                target.hurtServer(
                        serverLevel,

                        player
                                .damageSources()
                                .playerAttack(
                                        player
                                ),

                        WeaponStats
                                .CHAKRAM_DAMAGE
                );

        if (!damaged) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.SWEEP_ATTACK,

                target.getX(),

                target.getY()
                        + target.getBbHeight()
                        * 0.5D,

                target.getZ(),

                1,

                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        serverLevel.playSound(
                null,

                target.getX(),
                target.getY(),
                target.getZ(),

                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,

                0.55F,
                1.55F
        );
    }

    // =========================================================
    // ITEM COLLECTION
    // =========================================================

    private void collectItem(
            ServerLevel serverLevel,
            ItemEntity itemEntity
    ) {

        ItemStack stack =
                itemEntity
                        .getItem()
                        .copy();

        if (!stack.isEmpty()) {

            carriedItems.add(
                    stack
            );
        }

        serverLevel.playSound(
                null,

                itemEntity.getX(),
                itemEntity.getY(),
                itemEntity.getZ(),

                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,

                0.35F,
                1.4F
        );

        itemEntity.discard();
    }

    // =========================================================
    // XP COLLECTION
    // =========================================================

    private void collectExperience(
            ServerLevel serverLevel,
            ExperienceOrb orb
    ) {

        carriedExperience +=
                orb.getValue();

        serverLevel.playSound(
                null,

                orb.getX(),
                orb.getY(),
                orb.getZ(),

                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,

                0.25F,
                1.3F
        );

        orb.discard();
    }

    // =========================================================
    // RETURN DELIVERY
    // =========================================================

    private void completeReturn(
            ServerLevel serverLevel,
            ServerPlayer player
    ) {

        setDeltaMovement(
                Vec3.ZERO
        );

        // -----------------------------------------------------
        // ITEMS
        // -----------------------------------------------------

        for (ItemStack stack : carriedItems) {

            if (stack.isEmpty()) {
                continue;
            }

            player
                    .getInventory()
                    .placeItemBackInInventory(
                            stack
                    );
        }

        carriedItems.clear();

        // -----------------------------------------------------
        // EXPERIENCE
        // -----------------------------------------------------

        if (carriedExperience > 0) {

            player.giveExperiencePoints(
                    carriedExperience
            );

            carriedExperience =
                    0;
        }

        // -----------------------------------------------------
        // CHAKRAM ITSELF
        // -----------------------------------------------------

        if (returnItemToOwner) {

            ItemStack returnedChakram =
                    getItem()
                            .copyWithCount(1);

            player
                    .getInventory()
                    .placeItemBackInInventory(
                            returnedChakram
                    );
        }

        serverLevel.playSound(
                null,

                player.getX(),
                player.getY(),
                player.getZ(),

                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,

                0.75F,
                1.1F
        );

        discard();
    }

    // =========================================================
    // COLLECTIBLE
    // =========================================================

    private boolean isCollectible(
            Entity entity
    ) {

        return entity instanceof ItemEntity
                || entity instanceof ExperienceOrb;
    }

    // =========================================================
    // HUNT AREA
    // =========================================================

    private boolean isInsideHuntArea(
            Vec3 position
    ) {

        double radius =
                WeaponStats
                        .CHAKRAM_HUNT_RADIUS;

        return position
                .distanceToSqr(
                        huntOrigin
                )
                <= radius * radius;
    }

    // =========================================================
    // PREPARE ROUTE
    // =========================================================

    private RouteResult prepareRoute(
            ServerLevel serverLevel,
            Vec3 destination
    ) {

        clearPathOnly();

        if (hasClearLine(
                destination
        )) {

            return RouteResult.REACHABLE;
        }

        PathSearchResult search =
                findPath(
                        serverLevel,
                        position(),
                        destination
                );

        lastPathBuildTick =
                tickCount;

        if (!search
                .path()
                .isEmpty()) {

            currentPath =
                    search.path();

            waypointIndex =
                    0;

            return RouteResult.REACHABLE;
        }

        if (search.searchLimitHit()) {

            return RouteResult.SEARCH_LIMIT;
        }

        return RouteResult.UNREACHABLE;
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private NavigationResult navigateTo(
            ServerLevel serverLevel,
            Vec3 destination
    ) {

        // -----------------------------------------------------
        // DIRECT ROUTE
        // -----------------------------------------------------

        if (hasClearLine(
                destination
        )) {

            clearPathOnly();

            moveDirectlyToward(
                    destination
            );

            return NavigationResult.MOVING;
        }

        // -----------------------------------------------------
        // PATH RECALCULATION
        // -----------------------------------------------------

        boolean pathFinished =
                currentPath.isEmpty()
                        || waypointIndex
                        >= currentPath.size();

        boolean stale =
                lastPathBuildTick
                        == Integer.MIN_VALUE
                        || tickCount
                        - lastPathBuildTick
                        >= WeaponStats
                        .CHAKRAM_PATH_RECALC_INTERVAL_TICKS;

        if (pathFinished
                && !stale) {

            setDeltaMovement(
                    Vec3.ZERO
            );

            return NavigationResult.SEARCHING;
        }

        if (stale) {

            PathSearchResult search =
                    findPath(
                            serverLevel,
                            position(),
                            destination
                    );

            lastPathBuildTick =
                    tickCount;

            currentPath =
                    search.path();

            waypointIndex =
                    0;

            if (currentPath.isEmpty()) {

                setDeltaMovement(
                        Vec3.ZERO
                );

                return search.searchLimitHit()
                        ? NavigationResult.SEARCHING
                        : NavigationResult.UNREACHABLE;
            }
        }

        // -----------------------------------------------------
        // FOLLOW PATH
        // -----------------------------------------------------

        while (waypointIndex
                < currentPath.size()) {

            Vec3 waypoint =
                    currentPath.get(
                            waypointIndex
                    );

            double reach =
                    WeaponStats
                            .CHAKRAM_WAYPOINT_REACH;

            if (position()
                    .distanceToSqr(
                            waypoint
                    )
                    <= reach * reach) {

                waypointIndex++;

                continue;
            }

            moveDirectlyToward(
                    waypoint
            );

            return NavigationResult.MOVING;
        }

        setDeltaMovement(
                Vec3.ZERO
        );

        return NavigationResult.SEARCHING;
    }

    // =========================================================
    // DIRECT MOVEMENT
    // =========================================================

    private void moveDirectlyToward(
            Vec3 destination
    ) {

        Vec3 difference =
                destination.subtract(
                        position()
                );

        double distance =
                difference.length();

        if (distance
                <= 0.0001D) {

            setDeltaMovement(
                    Vec3.ZERO
            );

            return;
        }

        double speed =
                Math.min(
                        WeaponStats
                                .CHAKRAM_TRAVEL_SPEED,
                        distance
                );

        setDeltaMovement(
                difference
                        .normalize()
                        .scale(
                                speed
                        )
        );
    }

    // =========================================================
    // LINE OF SIGHT
    // =========================================================

    private boolean hasClearLine(
            Vec3 destination
    ) {

        BlockHitResult result =
                level().clip(
                        new ClipContext(
                                position(),
                                destination,

                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,

                                this
                        )
                );

        return result.getType()
                == HitResult.Type.MISS;
    }

    // =========================================================
    // A*
    // =========================================================

    private PathSearchResult findPath(
            ServerLevel serverLevel,
            Vec3 startPosition,
            Vec3 destination
    ) {

        BlockPos start =
                BlockPos.containing(
                        startPosition.x,
                        startPosition.y,
                        startPosition.z
                );

        BlockPos requestedGoal =
                BlockPos.containing(
                        destination.x,
                        destination.y,
                        destination.z
                );

        BlockPos goal =
                findPassableGoal(
                        serverLevel,
                        requestedGoal
                );

        if (goal == null) {

            return new PathSearchResult(
                    List.of(),
                    false
            );
        }

        if (start.equals(
                goal
        )) {

            return new PathSearchResult(
                    List.of(
                            destination
                    ),
                    false
            );
        }

        PriorityQueue<PathNode> openSet =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                PathNode::score
                        )
                );

        Map<BlockPos, BlockPos> cameFrom =
                new HashMap<>();

        Map<BlockPos, Double> gScore =
                new HashMap<>();

        Set<BlockPos> closedSet =
                new HashSet<>();

        gScore.put(
                start,
                0.0D
        );

        openSet.add(
                new PathNode(
                        start,
                        heuristic(
                                start,
                                goal
                        )
                )
        );

        int visitedNodes =
                0;

        while (!openSet.isEmpty()) {

            if (visitedNodes
                    >= WeaponStats
                    .CHAKRAM_PATH_MAX_NODES) {

                return new PathSearchResult(
                        List.of(),
                        true
                );
            }

            PathNode currentNode =
                    openSet.poll();

            BlockPos current =
                    currentNode.position();

            if (!closedSet.add(
                    current
            )) {

                continue;
            }

            visitedNodes++;

            if (current.equals(
                    goal
            )) {

                return new PathSearchResult(
                        reconstructPath(
                                cameFrom,
                                start,
                                current
                        ),
                        false
                );
            }

            for (Direction direction
                    : Direction.values()) {

                BlockPos neighbour =
                        current.relative(
                                direction
                        );

                if (closedSet.contains(
                        neighbour
                )) {

                    continue;
                }

                if (neighbour.getY()
                        < serverLevel.getMinY()
                        || neighbour.getY()
                        >= serverLevel.getMaxY()) {

                    continue;
                }

                if (!isPassable(
                        serverLevel,
                        neighbour
                )) {

                    continue;
                }

                double tentativeScore =
                        gScore.getOrDefault(
                                current,
                                Double.POSITIVE_INFINITY
                        )
                                + 1.0D;

                double knownScore =
                        gScore.getOrDefault(
                                neighbour,
                                Double.POSITIVE_INFINITY
                        );

                if (tentativeScore
                        >= knownScore) {

                    continue;
                }

                cameFrom.put(
                        neighbour,
                        current
                );

                gScore.put(
                        neighbour,
                        tentativeScore
                );

                double estimatedTotal =
                        tentativeScore
                                + heuristic(
                                neighbour,
                                goal
                        );

                openSet.add(
                        new PathNode(
                                neighbour,
                                estimatedTotal
                        )
                );
            }
        }

        /*
         * Entire searchable connected region was exhausted.
         *
         * THIS actually means unreachable without noclip.
         */
        return new PathSearchResult(
                List.of(),
                false
        );
    }

    // =========================================================
    // PASSABLE GOAL
    // =========================================================

    private BlockPos findPassableGoal(
            ServerLevel serverLevel,
            BlockPos requestedGoal
    ) {

        if (isPassable(
                serverLevel,
                requestedGoal
        )) {

            return requestedGoal;
        }

        for (Direction direction
                : Direction.values()) {

            BlockPos candidate =
                    requestedGoal.relative(
                            direction
                    );

            if (isPassable(
                    serverLevel,
                    candidate
            )) {

                return candidate;
            }
        }

        return null;
    }

    // =========================================================
    // PASSABLE NODE
    // =========================================================

    private boolean isPassable(
            ServerLevel serverLevel,
            BlockPos position
    ) {

        return serverLevel
                .getBlockState(
                        position
                )
                .getCollisionShape(
                        serverLevel,
                        position
                )
                .isEmpty();
    }

    // =========================================================
    // RECONSTRUCT PATH
    // =========================================================

    private List<Vec3> reconstructPath(
            Map<BlockPos, BlockPos> cameFrom,
            BlockPos start,
            BlockPos goal
    ) {

        List<BlockPos> reversePath =
                new ArrayList<>();

        BlockPos cursor =
                goal;

        while (!cursor.equals(
                start
        )) {

            reversePath.add(
                    cursor
            );

            cursor =
                    cameFrom.get(
                            cursor
                    );

            if (cursor == null) {

                return List.of();
            }
        }

        Collections.reverse(
                reversePath
        );

        List<Vec3> result =
                new ArrayList<>(
                        reversePath.size()
                );

        for (BlockPos blockPos
                : reversePath) {

            result.add(
                    Vec3.atCenterOf(
                            blockPos
                    )
            );
        }

        return result;
    }

    // =========================================================
    // HEURISTIC
    // =========================================================

    private static double heuristic(
            BlockPos from,
            BlockPos to
    ) {

        return Math.abs(
                from.getX()
                        - to.getX()
        )
                + Math.abs(
                from.getY()
                        - to.getY()
        )
                + Math.abs(
                from.getZ()
                        - to.getZ()
        );
    }

    // =========================================================
    // NAV RESET
    // =========================================================

    private void resetNavigation() {

        clearPathOnly();

        lastPathBuildTick =
                Integer.MIN_VALUE;
    }

    private void clearPathOnly() {

        currentPath =
                List.of();

        waypointIndex =
                0;
    }

    // =========================================================
    // PATH DATA
    // =========================================================

    private record PathNode(
            BlockPos position,
            double score
    ) {
    }

    private record PathSearchResult(
            List<Vec3> path,
            boolean searchLimitHit
    ) {
    }
}