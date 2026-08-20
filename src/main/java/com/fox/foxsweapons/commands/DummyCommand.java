package com.fox.foxsweapons.commands;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public final class DummyCommand {

    private static final String DUMMY_TAG = "unfortunate_dummy";
    private static final float DUMMY_HEALTH = 1024.0F;

    private static UUID dummyId;
    private static ResourceKey<Level> dummyDimension;
    private static BlockPos dummySpawnPos;

    private DummyCommand() {
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dummy")

                        .executes(context ->
                                spawnDummy(
                                        context.getSource().getPlayerOrException()
                                )
                        )

                        .then(
                                Commands.literal("heal")
                                        .executes(context ->
                                                healDummy(
                                                        context.getSource().getPlayerOrException()
                                                )
                                        )
                        )

                        .then(
                                Commands.literal("remove")
                                        .executes(context ->
                                                removeDummy(
                                                        context.getSource().getPlayerOrException()
                                                )
                                        )
                        )
        );
    }

    // =========================================================
    // SPAWN
    // =========================================================

    private static int spawnDummy(ServerPlayer player) {
        ServerLevel level = player.level();

        BlockPos pos = player.blockPosition()
                .relative(player.getDirection(), 3);

        removeTrackedDummy();

        /*
         * Minecraft 26.2:
         *
         * EntityType constants moved to EntityTypes.
         * IronGolem moved to animal.golem.
         */
        IronGolem golem = new IronGolem(
                EntityTypes.IRON_GOLEM,
                level
        );

        golem.setPos(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );

        configureDummy(golem);

        level.addFreshEntity(golem);

        dummyId = golem.getUUID();
        dummyDimension = level.dimension();
        dummySpawnPos = pos;

        player.sendSystemMessage(
                Component.literal(
                        "UNFORTUNATE DUMMY has reported for duty."
                )
        );

        return 1;
    }

    // =========================================================
    // CONFIGURE DUMMY
    // =========================================================

    private static void configureDummy(IronGolem golem) {

        golem.addTag(DUMMY_TAG);

        golem.setCustomName(
                Component.literal(
                        "UNFORTUNATE DUMMY"
                )
        );

        golem.setCustomNameVisible(true);

        /*
         * Keep normal AI.
         * Don't despawn.
         */
        golem.setPersistenceRequired();

        /*
         * Makes him behave as a player-created golem.
         */
        golem.setPlayerCreated(true);

        // -----------------------------------------------------
        // 1024 HP = 512 hearts
        // -----------------------------------------------------

        AttributeInstance maxHealth =
                golem.getAttribute(
                        Attributes.MAX_HEALTH
                );

        if (maxHealth != null) {
            maxHealth.setBaseValue(
                    DUMMY_HEALTH
            );
        }

        // -----------------------------------------------------
        // Harmless
        // -----------------------------------------------------

        AttributeInstance attackDamage =
                golem.getAttribute(
                        Attributes.ATTACK_DAMAGE
                );

        if (attackDamage != null) {
            attackDamage.setBaseValue(
                    0.0
            );
        }

        AttributeInstance attackKnockback =
                golem.getAttribute(
                        Attributes.ATTACK_KNOCKBACK
                );

        if (attackKnockback != null) {
            attackKnockback.setBaseValue(
                    0.0
            );
        }

        golem.setHealth(
                DUMMY_HEALTH
        );
    }

    // =========================================================
    // AUTO HEAL / AUTO RESPAWN
    // =========================================================

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {

        if (dummyId == null
                || dummyDimension == null
                || dummySpawnPos == null) {

            return;
        }

        ServerLevel level =
                event.getServer()
                        .getLevel(dummyDimension);

        if (level == null) {
            return;
        }

        Entity entity =
                level.getEntity(dummyId);

        /*
         * Somehow died?
         *
         * Spawn another unfortunate employee.
         */
        if (!(entity instanceof IronGolem golem)
                || !golem.isAlive()) {

            if (entity != null) {
                entity.discard();
            }

            respawnDummy(level);
            return;
        }

        /*
         * Damage gets to happen normally first.
         *
         * Soul Reaper can:
         * - damage him
         * - detect crit
         * - steal HP
         * - spawn particles
         *
         * THEN the dummy heals on the server tick.
         */
        if (golem.getHealth() < golem.getMaxHealth()) {
            golem.setHealth(
                    golem.getMaxHealth()
            );
        }
    }

    // =========================================================
    // RESPAWN
    // =========================================================

    private static void respawnDummy(ServerLevel level) {

        if (dummySpawnPos == null) {
            return;
        }

        IronGolem golem = new IronGolem(
                EntityTypes.IRON_GOLEM,
                level
        );

        golem.setPos(
                dummySpawnPos.getX() + 0.5,
                dummySpawnPos.getY(),
                dummySpawnPos.getZ() + 0.5
        );

        configureDummy(golem);

        level.addFreshEntity(golem);

        dummyId = golem.getUUID();
    }

    // =========================================================
    // /dummy heal
    // =========================================================

    private static int healDummy(ServerPlayer player) {

        IronGolem golem =
                getDummy();

        if (golem == null) {

            player.sendSystemMessage(
                    Component.literal(
                            "No UNFORTUNATE DUMMY exists."
                    )
            );

            return 0;
        }

        golem.setHealth(
                golem.getMaxHealth()
        );

        player.sendSystemMessage(
                Component.literal(
                        "UNFORTUNATE DUMMY has been repaired."
                )
        );

        return 1;
    }

    // =========================================================
    // /dummy remove
    // =========================================================

    private static int removeDummy(ServerPlayer player) {

        IronGolem golem =
                getDummy();

        if (golem != null) {
            golem.discard();
        }

        dummyId = null;
        dummyDimension = null;
        dummySpawnPos = null;

        player.sendSystemMessage(
                Component.literal(
                        "UNFORTUNATE DUMMY has been released from suffering."
                )
        );

        return 1;
    }

    // =========================================================
    // FIND CURRENT DUMMY
    // =========================================================

    private static IronGolem getDummy() {

        if (dummyId == null
                || dummyDimension == null) {

            return null;
        }

        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }

        ServerLevel level =
                ServerLifecycleHooks
                        .getCurrentServer()
                        .getLevel(dummyDimension);

        if (level == null) {
            return null;
        }

        Entity entity =
                level.getEntity(dummyId);

        return entity instanceof IronGolem golem
                ? golem
                : null;
    }

    // =========================================================
    // REMOVE PREVIOUS DUMMY
    // =========================================================

    private static void removeTrackedDummy() {

        IronGolem oldDummy =
                getDummy();

        if (oldDummy != null) {
            oldDummy.discard();
        }

        dummyId = null;
    }
}