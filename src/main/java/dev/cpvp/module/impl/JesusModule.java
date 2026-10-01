package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import net.minecraft.util.math.Vec3d;

public final class JesusModule extends Module {
    public JesusModule() { super("Jesus", "Float on water", Category.MOVEMENT); }

    @Override public void onTick() {
        if (mc.player.isSneaking() || !mc.player.isTouchingWater()) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, 0.1, v.z);
    }
}
