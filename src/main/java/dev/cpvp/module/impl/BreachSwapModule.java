package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.util.SlotUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Attribute swap: when you left-click a player while holding a sword, the hit is made with your
 * Breach mace (armor-ignoring), then you are swapped straight back to the sword.
 * The swap happens on the raw click, vanilla performs the attack next tick with the mace in hand,
 * and the swap back is sent at the end of that same tick.
 */
public final class BreachSwapModule extends Module {
    private final BooleanSetting onlySword     = add(new BooleanSetting("Only When Holding Sword", true));
    private final BooleanSetting requireBreach = add(new BooleanSetting("Require Breach Enchant", true));

    private int returnSlot = -1;

    public BreachSwapModule() {
        super("Breach Swap", "Hit with Breach mace, swap back to sword", Category.COMBAT);
    }

    @Override public void onDisable() { restore(); }

    @Override public void onLeftClick() {
        if (mc.player == null || returnSlot >= 0) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof PlayerEntity)) return;
        if (onlySword.get() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) return;

        int mace = SlotUtil.find(s -> s.isOf(Items.MACE) && (!requireBreach.get() || hasBreach(s)));
        if (mace < 0) { status(requireBreach.get() ? "No Breach mace in hotbar" : "No mace in hotbar"); return; }
        if (mace == SlotUtil.selected()) return;

        returnSlot = SlotUtil.selected();
        SlotUtil.swap(mace);
    }

    @Override public void onTick() { restore(); } // runs after vanilla's attack for this tick

    private void restore() {
        if (returnSlot >= 0 && mc.player != null) SlotUtil.swap(returnSlot);
        returnSlot = -1;
    }

    private static boolean hasBreach(ItemStack stack) {
        ItemEnchantmentsComponent e = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> en : e.getEnchantments()) if (en.matchesKey(Enchantments.BREACH)) return true;
        return false;
    }
}
