package com.fox.foxsweapons;

import com.fox.foxsweapons.config.WeaponStats;
import com.fox.foxsweapons.item.BlunderbussItem;
import com.fox.foxsweapons.item.SoulReaperItem;
import com.fox.foxsweapons.item.VolcanoHammerItem;
import com.fox.foxsweapons.item.TempestBowItem;
import com.fox.foxsweapons.network.BlunderbussNetwork;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Weapon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FoxsWeapons.MODID)
public class FoxsWeapons {
    public static final String MODID = "foxsweapons";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<VolcanoHammerItem> VOLCANO_HAMMER = ITEMS.registerItem(
            "volcano_hammer",
            VolcanoHammerItem::new,
            p -> p.durability(WeaponStats.VOLCANO_HAMMER_DURABILITY)
                    .fireResistant()
                    .repairable(Items.MAGMA_CREAM)
                    .enchantable(WeaponStats.VOLCANO_HAMMER_ENCHANTABILITY)
                    .rarity(Rarity.RARE)
                    .attributes(meleeAttributes(
                            WeaponStats.VOLCANO_HAMMER_ATTACK_DAMAGE,
                            WeaponStats.VOLCANO_HAMMER_ATTACK_SPEED))
                    .component(DataComponents.WEAPON, new Weapon(1))
                    .component(DataComponents.SWING_ANIMATION,
                            noVanillaSwing(WeaponStats.VOLCANO_HAMMER_SWING_TICKS))
    );

    public static final DeferredItem<BlunderbussItem> BLUNDERBUSS = ITEMS.registerItem(
            "blunderbuss",
            BlunderbussItem::new,
            p -> p.durability(WeaponStats.BLUNDERBUSS_DURABILITY)
                    .repairable(Items.IRON_INGOT)
                    .rarity(Rarity.UNCOMMON)
                    .component(DataComponents.SWING_ANIMATION,
                            noVanillaSwing(WeaponStats.BLUNDERBUSS_SWING_TICKS))
    );

    public static final DeferredItem<SoulReaperItem> SOUL_REAPER = ITEMS.registerItem(
            "soul_reaper",
            SoulReaperItem::new,
            p -> p.durability(WeaponStats.SOUL_REAPER_DURABILITY)
                    .repairable(Items.DIAMOND)
                    .enchantable(WeaponStats.SOUL_REAPER_ENCHANTABILITY)
                    .rarity(Rarity.EPIC)
                    .attributes(meleeAttributes(
                            WeaponStats.SOUL_REAPER_ATTACK_DAMAGE,
                            WeaponStats.SOUL_REAPER_ATTACK_SPEED))
                    .component(DataComponents.WEAPON, new Weapon(1))
                    .component(DataComponents.SWING_ANIMATION,
                            noVanillaSwing(WeaponStats.SOUL_REAPER_SWING_TICKS))
    );

    public static final DeferredItem<TempestBowItem> TEMPEST_BOW =
            ITEMS.registerItem(
                    "tempest_bow",
                    TempestBowItem::new,
                    p -> p
                            .durability(
                                    WeaponStats.TEMPEST_BOW_DURABILITY
                            )
                            .enchantable(
                                    WeaponStats.TEMPEST_BOW_ENCHANTABILITY
                            )
                            .rarity(Rarity.EPIC)
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS_TAB =
            CREATIVE_MODE_TABS.register("weapons", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.foxsweapons"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(FoxsWeapons::createTabIcon)
                    .displayItems((parameters, output) -> {
                        output.accept(VOLCANO_HAMMER.get());
                        output.accept(BLUNDERBUSS.get());
                        output.accept(SOUL_REAPER.get());
                        output.accept(TEMPEST_BOW.get());
                    })
                    .build());

    public FoxsWeapons(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(BlunderbussNetwork::registerPayloads);
    }

    private static ItemAttributeModifiers meleeAttributes(double damage, double speed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                damage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID,
                                speed,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static SwingAnimation noVanillaSwing(int ticks) {
        return new SwingAnimation(SwingAnimationType.NONE, ticks);
    }

    private static ItemStack createTabIcon() {
        ItemStack icon = VOLCANO_HAMMER.get().getDefaultInstance();
        icon.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(MODID, "fox_icon"));
        return icon;
    }
}
