package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.BooleanSetting;
import dev.cpvp.setting.NumberSetting;
import dev.cpvp.util.PacketUtil;
import dev.cpvp.util.SlotUtil;
import dev.cpvp.util.TargetUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Places crystals on obsidian/bedrock near the target and detonates them.
 * Damage is an approximation (no armor / line-of-sight exposure), so tune Min/Max values.
 */
public final class CrystalAuraModule extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", 3, 12, 0.5, 9));
    private final NumberSetting placeRange  = add(new NumberSetting("Place Range", 1, 6, 0.1, 4.5));
    private final NumberSetting breakRange  = add(new NumberSetting("Break Range", 1, 6, 0.1, 4.5));
    private final NumberSetting placeDelay  = add(new NumberSetting("Place Delay (ticks)", 0, 10, 1, 0));
    private final NumberSetting breakDelay  = add(new NumberSetting("Break Delay (ticks)", 0, 10, 1, 0));
    private final NumberSetting minDamage   = add(new NumberSetting("Min Damage", 1, 36, 1, 6));
    private final NumberSetting maxSelf     = add(new NumberSetting("Max Self Damage", 1, 36, 1, 8));
    private final BooleanSetting place      = add(new BooleanSetting("Place", true));
    private final BooleanSetting breakIt    = add(new BooleanSetting("Break", true));
    private final BooleanSetting autoSwap   = add(new BooleanSetting("Auto Swap", true));

    private int placeTimer, breakTimer;

    public CrystalAuraModule() { super("Crystal Aura", "Auto place and break crystals on a target", Category.COMBAT); }

    @Override public void onTick() {
        if (mc.currentScreen != null || mc.interactionManager == null) return;
        PlayerEntity t = TargetUtil.nearest(targetRange.get());
        if (t == null) return;
        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;
        if (breakIt.get() && breakTimer == 0) doBreak(t);
        if (place.get() && placeTimer == 0) doPlace(t);
    }

    private double hp() { return mc.player.getHealth() + mc.player.getAbsorptionAmount(); }

    private static double damage(Vec3d crystal, LivingEntity e) {
        double w = Math.sqrt(e.getBoundingBox().getCenter().squaredDistanceTo(crystal)) / 12.0;
        if (w > 1) return 0;
        double a = 1.0 - w;
        return (int) ((a * a + a) / 2.0 * 7.0 * 12.0 + 1.0);
    }

    private void doBreak(PlayerEntity t) {
        EndCrystalEntity best = null; double bestScore = -1e9;
        double r = breakRange.get();
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                mc.player.getBoundingBox().expand(r + 1), Entity -> Entity.isAlive())) {
            if (mc.player.distanceTo(c) > r) continue;
            Vec3d pos = c.getPos();
            double td = damage(pos, t), sd = damage(pos, mc.player);
            if (td < minDamage.get() || sd > maxSelf.get() || sd >= hp() - 1) continue;
            if (td - sd > bestScore) { bestScore = td - sd; best = c; }
        }
        if (best == null) return;
        PacketUtil.attack(best);
        PacketUtil.swing();
        breakTimer = breakDelay.getInt();
    }

    private void doPlace(PlayerEntity t) {
        boolean offhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        int slot = SlotUtil.find(s -> s.isOf(Items.END_CRYSTAL));
        if (!offhand && slot < 0) { status("No end crystals in hotbar/offhand"); return; }

        BlockPos tp = t.getBlockPos(), bestBase = null;
        double bestScore = -1e9;
        Vec3d eye = mc.player.getEyePos();
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -3; dy <= 2; dy++) {
            BlockPos base = tp.add(dx, dy, dz);
            var st = mc.world.getBlockState(base);
            if (!st.isOf(Blocks.OBSIDIAN) && !st.isOf(Blocks.BEDROCK)) continue;
            Vec3d crystal = new Vec3d(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
            if (eye.distanceTo(crystal) > placeRange.get()) continue;
            if (!mc.world.getBlockState(base.up()).isAir() || !mc.world.getBlockState(base.up(2)).isAir()) continue;
            Box box = new Box(base.getX(), base.getY() + 1, base.getZ(), base.getX() + 1, base.getY() + 3, base.getZ() + 1);
            if (!mc.world.getOtherEntities(null, box).isEmpty()) continue;
            double td = damage(crystal, t), sd = damage(crystal, mc.player);
            if (td < minDamage.get() || sd > maxSelf.get() || sd >= hp() - 1) continue;
            if (td - sd > bestScore) { bestScore = td - sd; bestBase = base; }
        }
        if (bestBase == null) return;

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        int prev = SlotUtil.selected();
        if (!offhand) {
            if (!autoSwap.get() && prev != slot) return;
            SlotUtil.swap(slot);
        }
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(bestBase).add(0, 0.5, 0), Direction.UP, bestBase, false);
        if (mc.interactionManager.interactBlock(mc.player, hand, hit).isAccepted()) mc.player.swingHand(hand);
        if (!offhand) SlotUtil.swap(prev);
        placeTimer = placeDelay.getInt();
    }
}
