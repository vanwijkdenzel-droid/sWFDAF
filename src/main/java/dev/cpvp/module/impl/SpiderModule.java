package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import net.minecraft.util.math.Vec3d;

public final class SpiderModule extends Module {
    public SpiderModule() { super("Spider", "Climb walls", Category.MOVEMENT); }

    @Override public void onTick() {
        if (!mc.player.horizontalCollision) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x, 0.2, v.z);
    }
}
