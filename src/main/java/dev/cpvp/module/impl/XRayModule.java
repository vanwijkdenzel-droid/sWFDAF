package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.BooleanSetting;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Hides ordinary blocks and shows ores/storage (via BlockMixin). Does not work with Sodium installed. */
public final class XRayModule extends Module {
    public static boolean ACTIVE;
    private final BooleanSetting fullbright = add(new BooleanSetting("Fullbright", true));
    private boolean addedNv;

    public XRayModule() { super("X-Ray", "See ores through walls", Category.RENDER); }

    public static boolean isTarget(BlockState s) {
        return s.isIn(ConventionalBlockTags.ORES) || s.isOf(Blocks.ANCIENT_DEBRIS) || s.isOf(Blocks.CHEST)
                || s.isOf(Blocks.ENDER_CHEST) || s.isOf(Blocks.SPAWNER) || s.isOf(Blocks.TRAPPED_CHEST)
                || s.isOf(Blocks.END_PORTAL_FRAME) || s.isOf(Blocks.NETHER_PORTAL);
    }

    @Override public void onEnable()  { ACTIVE = true;  mc.worldRenderer.reload(); }
    @Override public void onDisable() {
        ACTIVE = false;
        if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
        if (addedNv && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        addedNv = false;
    }

    @Override public void onTick() {
        if (!fullbright.get() || mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) return;
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false));
        addedNv = true;
    }
}
