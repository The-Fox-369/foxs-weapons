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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

import java.util.function.Consumer;

@EventBusSubscriber(modid = FoxsWeapons.MODID)
public class TempestBowItem extends BowItem implements GeoItem {

    private static final String TEMPEST_ARROW =
            "foxsweapons_tempest_arrow";

    private static final String TEMPEST_HIT =
            "foxsweapons_tempest_hit";

    /*
     * This MUST match the animation name inside:
     *
     * tempest_bow.animation.json
     *
     * "animations": {
     *     "draw_and_release": {
     */
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
                    renderer = new TempestBowRenderer();
                }

                return renderer;
            }
        });
    }

    // =========================================================
    // GECKOLIB ANIMATION
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
                super.use(level, player, hand);

        if (level instanceof ServerLevel serverLevel
                && result != InteractionResult.FAIL) {

            ItemStack stack =
                    player.getItemInHand(hand);

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
    // RELEASE BOW
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
    // FAST NINJA DRAW
    // =========================================================

    @SubscribeEvent
    public static void onArrowLoose(
            ArrowLooseEvent event
    ) {
        if (!event.getBow()
                .is(FoxsWeapons.TEMPEST_BOW.get())) {

            return;
        }

        /*
         * 5 ticks
         * =
         * 0.25 seconds.
         *
         * Once the Tempest Bow reaches five ticks,
         * treat it as a completely charged vanilla bow.
         */
        if (event.getCharge()
                >= WeaponStats.TEMPEST_BOW_DRAW_TICKS) {

            event.setCharge(20);

        } else {

            /*
             * Released before the ninja draw completed.
             *
             * No shot.
             */
            event.setCharge(0);
        }
    }

    // =========================================================
    // MARK TEMPEST ARROWS
    // =========================================================

    @SubscribeEvent
    public static void onProjectileSpawn(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide()) {
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
         * Detect whether this arrow came from a player
         * currently holding / using the Tempest Bow.
         */
        boolean usingTempest =
                player.getUseItem()
                        .is(FoxsWeapons.TEMPEST_BOW.get())

                        || player.getMainHandItem()
                        .is(FoxsWeapons.TEMPEST_BOW.get())

                        || player.getOffhandItem()
                        .is(FoxsWeapons.TEMPEST_BOW.get());

        if (!usingTempest) {
            return;
        }

        /*
         * Mark THIS arrow as a Tempest projectile.
         */
        arrow.getPersistentData()
                .putBoolean(
                        TEMPEST_ARROW,
                        true
                );
    }

    // =========================================================
    // TEMPEST DAMAGE + REAL LIGHTNING
    // =========================================================

    @SubscribeEvent
    public static void onArrowDamage(
            LivingDamageEvent.Pre event
    ) {
        /*
         * Damage must come directly from an arrow.
         */
        if (!(event.getSource()
                .getDirectEntity()
                instanceof AbstractArrow arrow)) {

            return;
        }

        /*
         * Ignore normal arrows.
         */
        if (!arrow.getPersistentData()
                .getBooleanOr(
                        TEMPEST_ARROW,
                        false
                )) {

            return;
        }

        /*
         * The Tempest Bow arrow deals:
         *
         * 20 HP
         * =
         * 10 hearts.
         */
        event.setNewDamage(
                WeaponStats.TEMPEST_BOW_ARROW_DAMAGE
        );

        if (!(event.getEntity().level()
                instanceof ServerLevel level)) {

            return;
        }

        /*
         * Prevent the same arrow from summoning
         * multiple lightning bolts on one hit.
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

        LivingEntity target =
                event.getEntity();

        // =====================================================
        // ACTUAL LIGHTNING
        // =====================================================
        //
        // NOT visual-only.
        //
        // This is a genuine vanilla LightningBolt entity.
        //
        // Creepers can charge.
        // Fire can happen.
        // Vanilla lightning transformations can happen.
        // The universe may regret this bow.
        // =====================================================

        LightningBolt lightning =
                new LightningBolt(
                        EntityTypes.LIGHTNING_BOLT,
                        level
                );

        lightning.setPos(
                target.getX(),
                target.getY(),
                target.getZ()
        );

        level.addFreshEntity(
                lightning
        );
    }

    // =========================================================
    // GECKOLIB CACHE
    // =========================================================

    @Override
    public AnimatableInstanceCache
    getAnimatableInstanceCache() {

        return cache;
    }
}