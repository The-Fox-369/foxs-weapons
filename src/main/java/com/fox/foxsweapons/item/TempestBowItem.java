package com.fox.foxsweapons.item;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.client.renderer.TempestBowRenderer;
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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

import java.util.function.Consumer;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public class TempestBowItem extends BowItem implements GeoItem {

    private static final String TEMPEST_ARROW =
            "foxsweapons_tempest_arrow";

    private static final String TEMPEST_HIT =
            "foxsweapons_tempest_hit";

    private static final RawAnimation DRAW_ANIMATION =
            RawAnimation.begin()
                    .thenPlayAndHold("draw_and_release");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public TempestBowItem(Properties properties) {
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

            private TempestBowRenderer renderer;

            @Override
            public GeoItemRenderer<TempestBowItem>
            getGeoItemRenderer() {

                if (renderer == null) {
                    renderer =
                            new TempestBowRenderer();
                }

                return renderer;
            }
        });
    }

    // =========================================================
    // ANIMATION
    // =========================================================

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        "draw_controller",
                        0,
                        state -> PlayState.STOP
                )
                        .triggerableAnim(
                                "draw",
                                DRAW_ANIMATION
                        )
        );
    }

    // =========================================================
    // START DRAW
    // =========================================================

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        InteractionResult result =
                super.use(
                        level,
                        player,
                        hand
                );

        if (level instanceof ServerLevel serverLevel
                && result != InteractionResult.FAIL) {

            ItemStack stack =
                    player.getItemInHand(
                            hand
                    );

            triggerAnim(
                    player,

                    GeoItem.getOrAssignId(
                            stack,
                            serverLevel
                    ),

                    "draw_controller",
                    "draw"
            );
        }

        return result;
    }

    // =========================================================
    // RELEASE
    // =========================================================

    @Override
    public boolean releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingTime
    ) {
        if (level instanceof ServerLevel serverLevel) {

            stopTriggeredAnim(
                    entity,

                    GeoItem.getOrAssignId(
                            stack,
                            serverLevel
                    ),

                    "draw_controller",
                    "draw"
            );
        }

        return super.releaseUsing(
                stack,
                level,
                entity,
                remainingTime
        );
    }

    // =========================================================
    // 0.25 SECOND DRAW
    // =========================================================

    @SubscribeEvent
    public static void onArrowLoose(
            ArrowLooseEvent event
    ) {
        if (!event.getBow()
                .is(
                        FoxsWeapons.TEMPEST_BOW.get()
                )) {

            return;
        }

        /*
         * Five ticks = 0.25 seconds.
         *
         * At five ticks the Tempest Bow becomes
         * equivalent to a full vanilla bow charge.
         */
        if (event.getCharge()
                >= WeaponStats.TEMPEST_BOW_DRAW_TICKS) {

            event.setCharge(
                    20
            );

        } else {

            event.setCharge(
                    0
            );
        }
    }

    // =========================================================
    // MARK TEMPEST ARROWS
    // =========================================================

    @SubscribeEvent
    public static void onProjectileSpawn(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel()
                .isClientSide()) {

            return;
        }

        if (!(event.getEntity()
                instanceof AbstractArrow arrow)) {

            return;
        }

        if (!(arrow.getOwner()
                instanceof Player player)) {

            return;
        }

        /*
         * YES:
         *
         * Normal Bow in main hand
         * +
         * Tempest Bow in offhand
         *
         * still makes a Tempest arrow.
         *
         * IT'S A FEATURE.
         */
        boolean usingTempest =

                player.getUseItem()
                        .is(
                                FoxsWeapons.TEMPEST_BOW.get()
                        )

                        || player.getMainHandItem()
                        .is(
                                FoxsWeapons.TEMPEST_BOW.get()
                        )

                        || player.getOffhandItem()
                        .is(
                                FoxsWeapons.TEMPEST_BOW.get()
                        );

        if (!usingTempest) {
            return;
        }

        arrow.getPersistentData()
                .putBoolean(
                        TEMPEST_ARROW,
                        true
                );
    }

    // =========================================================
    // TEMPEST DAMAGE
    // =========================================================

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        /*
         * Extra damage only applies when
         * the arrow actually hits a living entity.
         */
        if (!(event.getSource()
                .getDirectEntity()
                instanceof AbstractArrow arrow)) {

            return;
        }

        if (!arrow.getPersistentData()
                .getBooleanOr(
                        TEMPEST_ARROW,
                        false
                )) {

            return;
        }

        /*
         * Add Tempest damage BEFORE armor mitigation.
         *
         * This means:
         *
         * Power still matters.
         * Armor still matters.
         * Other vanilla modifiers still matter.
         */
        event.setAmount(
                event.getAmount()
                        + WeaponStats
                        .TEMPEST_BOW_DAMAGE_BONUS
        );
    }

    // =========================================================
    // LIGHTNING ON ANY IMPACT
    // =========================================================

    @SubscribeEvent
    public static void onProjectileImpact(
            ProjectileImpactEvent event
    ) {
        /*
         * Only arrows.
         */
        if (!(event.getProjectile()
                instanceof AbstractArrow arrow)) {

            return;
        }

        /*
         * Only Tempest arrows.
         */
        if (!arrow.getPersistentData()
                .getBooleanOr(
                        TEMPEST_ARROW,
                        false
                )) {

            return;
        }

        /*
         * Server only.
         */
        if (!(arrow.level()
                instanceof ServerLevel level)) {

            return;
        }

        /*
         * Prevent one arrow from summoning
         * multiple lightning bolts.
         */
        if (arrow.getPersistentData()
                .getBooleanOr(
                        TEMPEST_HIT,
                        false
                )) {

            return;
        }

        arrow.getPersistentData()
                .putBoolean(
                        TEMPEST_HIT,
                        true
                );

        /*
         * EXACT impact position.
         *
         * Entity?
         * Block?
         * Ground?
         * Wall?
         *
         * Doesn't matter.
         */
        Vec3 impact =
                event.getRayTraceResult()
                        .getLocation();

        // =====================================================
        // REAL VANILLA LIGHTNING
        // =====================================================
        //
        // Arrow lands.
        //
        // Lightning happens.
        //
        // That's the entire philosophy.
        //
        // =====================================================

        LightningBolt lightning =
                new LightningBolt(
                        EntityTypes.LIGHTNING_BOLT,
                        level
                );

        lightning.setPos(
                impact.x,
                impact.y,
                impact.z
        );

        level.addFreshEntity(
                lightning
        );
    }

    // =========================================================
    // CACHE
    // =========================================================

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return cache;
    }
}