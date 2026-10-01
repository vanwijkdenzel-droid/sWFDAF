package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.setting.NumberSetting;
import dev.cpvp.util.SlotUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Throw an ender pearl as normal; this then throws a wind charge straight down at your feet
 * after a short delay (the pearl-catch tech), and swaps back to what you were holding.
 */
public final class PearlCatchModule extends Module {
    private final NumberSetting delay   = add(new NumberSetting("Wind Charge Delay (ticks)", 0, 20, 1, 3));
    private final BooleanSetting swapBack = add(new BooleanSetting("Swap Back", true));

    private int countdown = -1;

    public PearlCatchModule() { super("Pearlcatch", "Auto wind charge after you throw a pearl", Category.COMBAT); }

    @Override public void onRightClick() {
        if (mc.player == null || !mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL)) return;
        countdown = delay.getInt();
    }

    @Override public void onTick() {
        if (countdown < 0) return;
        if (countdown > 0) { countdown--; return; }
        countdown = -1;
        fire();
    }

    private void fire() {
        int slot = SlotUtil.find(s -> s.isOf(Items.WIND_CHARGE));
        if (slot < 0) { status("No wind charge in hotbar"); return; }
        int prev = SlotUtil.selected();
        SlotUtil.swap(slot);
        float oldPitch = mc.player.getPitch();
        mc.player.setPitch(90f);                                   // throw straight down
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.setPitch(oldPitch);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (swapBack.get()) SlotUtil.swap(prev);
    }
}
