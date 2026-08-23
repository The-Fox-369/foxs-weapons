package com.fox.foxsweapons.commands;

import com.fox.foxsweapons.FoxsWeapons;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public final class DummyCommand {

    private static final String DUMMY_TAG =
            "unfortunate_dummy";

    private static final String DUMMY_MARKER =
            "foxsweapons_is_dummy";

    private static final String HEAL_DELAY =
            "foxsweapons_dummy_heal_delay";

    private static final float DUMMY_HEALTH =
            1024.0F;

    private DummyCommand() {
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @SubscribeEvent
    public static void registerCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("dummy")

                        .executes(context ->
                                spawnDummy(
                                        context.getSource()
                                                .getPlayerOrException()
                                )
                        )

                        .then(
                                Commands.literal("remove")
                                        .executes(context ->
                                                removeDummies(
                                                        context.getSource()
                                                                .getPlayerOrException()
                                                )
                                        )
                        )
        );
    }

    // =========================================================
    // SPAWN
    // =========================================================

    private static int spawnDummy(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.level();

        /*
         * Only one loaded dummy at a time.
         */
        removeAllLoadedDummies();

        BlockPos pos =
                player.blockPosition()
                        .relative(
                                player.getDirection(),
                                3
                        );

        IronGolem golem =
                new IronGolem(
                        EntityTypes.IRON_GOLEM,
                        level
                );

        golem.setPos(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );

        configureDummy(
                golem,
                true
        );

        level.addFreshEntity(
                golem
        );

        player.sendSystemMessage(
                Component.literal(
                        "UNFORTUNATE DUMMY has reported for duty."
                )
        );

        return 1;
    }

    // =========================================================
    // CONFIGURE
    // =========================================================

    private static void configureDummy(
            IronGolem golem,
            boolean heal
    ) {
        /*
         * Vanilla scoreboard tag.
         *
         * This survives saving and world reloads.
         */
        golem.addTag(
                DUMMY_TAG
        );

        /*
         * Extra persistent marker.
         *
         * Also survives saving and world reloads.
         */
        golem.getPersistentData()
                .putBoolean(
                        DUMMY_MARKER,
                        true
                );

        golem.setCustomName(
                Component.literal(
                        "UNFORTUNATE DUMMY"
                )
        );

        golem.setCustomNameVisible(
                true
        );

        golem.setPersistenceRequired();

        /*
         * Player-created golem behaviour.
         */
        golem.setPlayerCreated(
                true
        );

        // -----------------------------------------------------
        // HEALTH
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
        // HARMLESS
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

        if (heal) {
            golem.setHealth(
                    golem.getMaxHealth()
            );

            golem.getPersistentData()
                    .putInt(
                            HEAL_DELAY,
                            0
                    );
        }
    }

    // =========================================================
    // WORLD / CHUNK RELOAD
    // =========================================================

    @SubscribeEvent
    public static void onEntityJoinLevel(
            EntityJoinLevelEvent event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel)) {

            return;
        }

        if (!(event.getEntity()
                instanceof IronGolem golem)) {

            return;
        }

        if (!isDummy(golem)) {
            return;
        }

        /*
         * Re-apply everything after world/chunk reload.
         */
        configureDummy(
                golem,
                true
        );
    }

    // =========================================================
    // DAMAGE
    // =========================================================

    @SubscribeEvent
    public static void onDummyDamaged(
            LivingDamageEvent.Post event
    ) {
        if (!(event.getEntity()
                instanceof IronGolem golem)) {

            return;
        }

        if (!isDummy(golem)) {
            return;
        }

        /*
         * Let damage actually exist for two ticks.
         *
         * Weapon damage testing,
         * crits,
         * Soul Reaper,
         * particles,
         * etc.
         *
         * all get to happen first.
         */
        golem.getPersistentData()
                .putInt(
                        HEAL_DELAY,
                        2
                );
    }

    // =========================================================
    // AUTO HEAL
    // =========================================================

    @SubscribeEvent
    public static void onDummyTick(
            EntityTickEvent.Post event
    ) {
        if (!(event.getEntity()
                instanceof IronGolem golem)) {

            return;
        }

        if (!(golem.level()
                instanceof ServerLevel)) {

            return;
        }

        if (!isDummy(golem)) {
            return;
        }

        int ticks =
                golem.getPersistentData()
                        .getInt(
                                HEAL_DELAY
                        )
                        .orElse(0);

        if (ticks <= 0) {
            return;
        }

        ticks--;

        golem.getPersistentData()
                .putInt(
                        HEAL_DELAY,
                        ticks
                );

        if (ticks > 0) {
            return;
        }

        if (golem.isAlive()) {
            golem.setHealth(
                    golem.getMaxHealth()
            );
        }
    }

    // =========================================================
    // IMMORTALITY
    // =========================================================

    @SubscribeEvent
    public static void onDummyDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof IronGolem golem)) {

            return;
        }

        if (!isDummy(golem)) {
            return;
        }

        /*
         * Damage:
         * YES.
         *
         * Dying:
         * NO.
         */
        event.setCanceled(
                true
        );

        configureDummy(
                golem,
                true
        );
    }

    // =========================================================
    // /dummy remove
    // =========================================================

    private static int removeDummies(
            ServerPlayer player
    ) {
        int removed =
                removeAllLoadedDummies();

        if (removed == 0) {

            player.sendSystemMessage(
                    Component.literal(
                            "No UNFORTUNATE DUMMY exists."
                    )
            );

            return 0;
        }

        player.sendSystemMessage(
                Component.literal(
                        removed == 1
                                ? "UNFORTUNATE DUMMY has been released from suffering."
                                : removed
                                + " UNFORTUNATE DUMMIES have been released from suffering."
                )
        );

        return removed;
    }

    // =========================================================
    // FIND
    // =========================================================

    private static List<IronGolem>
    findAllLoadedDummies() {

        List<IronGolem> dummies =
                new ArrayList<>();

        MinecraftServer server =
                ServerLifecycleHooks
                        .getCurrentServer();

        if (server == null) {
            return dummies;
        }

        for (ServerLevel level :
                server.getAllLevels()) {

            for (Entity entity :
                    level.getAllEntities()) {

                if (!(entity
                        instanceof IronGolem golem)) {

                    continue;
                }

                if (!isDummy(golem)) {
                    continue;
                }

                dummies.add(
                        golem
                );
            }
        }

        return dummies;
    }

    // =========================================================
    // REMOVE
    // =========================================================

    private static int removeAllLoadedDummies() {

        List<IronGolem> dummies =
                findAllLoadedDummies();

        for (IronGolem golem : dummies) {

            /*
             * Strip identity first.
             */
            golem.removeTag(
                    DUMMY_TAG
            );

            golem.getPersistentData()
                    .putBoolean(
                            DUMMY_MARKER,
                            false
                    );

            golem.discard();
        }

        return dummies.size();
    }

    // =========================================================
    // IDENTITY
    // =========================================================

    private static boolean isDummy(
            IronGolem golem
    ) {
        /*
         * New dummy:
         * persistent marker identifies him immediately.
         */
        if (golem.getPersistentData()
                .getBooleanOr(
                        DUMMY_MARKER,
                        false
                )) {

            return true;
        }

        /*
         * Compatibility with old dummies
         * already saved in development worlds.
         *
         * Minecraft 26.2 mappings don't expose
         * getTags() here.
         *
         * removeTag returns whether the tag existed.
         */
        boolean hadTag =
                golem.removeTag(
                        DUMMY_TAG
                );

        if (!hadTag) {
            return false;
        }

        /*
         * Put it straight back.
         */
        golem.addTag(
                DUMMY_TAG
        );

        /*
         * Upgrade old dummy.
         */
        golem.getPersistentData()
                .putBoolean(
                        DUMMY_MARKER,
                        true
                );

        return true;
    }
}