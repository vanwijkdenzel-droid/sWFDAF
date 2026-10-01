package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

public final class LongJumpModule extends Module {
    private final NumberSetting boost = add(new NumberSetting("Boost", 0.1, 1.5, 0.05, 0.6));

    public LongJumpModule() { super("LongJump", "Extra distance on jumps", Category.MOVEMENT); }

    @Override public void onTick() {
        if (!mc.options.jumpKey.isPressed() || !mc.player.isOnGround()) return;
        double yaw = Math.toRadians(mc.player.getYaw());
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x - Math.sin(yaw) * boost.get(), v.y, v.z + Math.cos(yaw) * boost.get());
    }
}
