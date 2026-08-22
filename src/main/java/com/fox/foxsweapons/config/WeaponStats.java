package com.fox.foxsweapons.config;

/**
 * Central balance values for every weapon.
 */
public final class WeaponStats {

    private WeaponStats() {}

    // =========================================================
    // VOLCANO HAMMER
    // =========================================================

    public static final int VOLCANO_HAMMER_DURABILITY = 1200;
    public static final int VOLCANO_HAMMER_ENCHANTABILITY = 15;

    public static final double VOLCANO_HAMMER_ATTACK_DAMAGE = 8.0;
    public static final double VOLCANO_HAMMER_ATTACK_SPEED = -3.2;

    public static final int VOLCANO_HAMMER_SWING_TICKS = 15;
    public static final int VOLCANO_HAMMER_SMASH_COOLDOWN_TICKS = 120;
    public static final int VOLCANO_HAMMER_SMASH_DURABILITY_COST = 3;

    // =========================================================
    // BLUNDERBUSS
    // =========================================================

    public static final int BLUNDERBUSS_DURABILITY = 450;
    public static final int BLUNDERBUSS_ENCHANTABILITY = 15;

    public static final int BLUNDERBUSS_SWING_TICKS = 10;
    public static final double BLUNDERBUSS_RANGE = 32.0;

    public static final double BLUNDERBUSS_SINGLE_SPREAD = 0.008;
    public static final double BLUNDERBUSS_BURST_SPREAD = 0.095;

    public static final float BLUNDERBUSS_SINGLE_DAMAGE = 8.0F;
    public static final float BLUNDERBUSS_BURST_PELLET_DAMAGE = 5.0F;

    public static final int BLUNDERBUSS_SINGLE_COOLDOWN = 12;
    public static final int BLUNDERBUSS_BURST_COOLDOWN = 24;

    // =========================================================
    // SOUL REAPER
    // =========================================================

    public static final int SOUL_REAPER_DURABILITY = 1450;
    public static final int SOUL_REAPER_ENCHANTABILITY = 15;

    public static final double SOUL_REAPER_ATTACK_DAMAGE = 20.0;
    public static final double SOUL_REAPER_ATTACK_SPEED = -3.1;

    public static final int SOUL_REAPER_SWING_TICKS = 16;

    /** 1 health point = half a heart. */
    public static final float SOUL_REAPER_LIFESTEAL = 5.0F;

    // =========================================================
    // TEMPEST BOW
    // =========================================================

    public static final int TEMPEST_BOW_DURABILITY = 1000;
    public static final int TEMPEST_BOW_ENCHANTABILITY = 15;

    /** Five ticks = 0.25 seconds. */
    public static final int TEMPEST_BOW_DRAW_TICKS = 5;

    /**
     * Added before armor reduction.
     *
     * Normal fully drawn arrow damage + 14
     * gives roughly 20+ raw damage while still
     * allowing Power and armor to matter.
     */
    public static final float TEMPEST_BOW_DAMAGE_BONUS = 14.0F;
}