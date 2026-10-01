package dev.cpvp.module.impl;

import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.NumberSetting;

public final class SpeedModule extends Module {
    private final NumberSetting mult = add(new NumberSetting("Multiplier", 1.0, 3.0, 0.05, 1.3));

    public SpeedModule() { super("Speed", "Faster ground/air movement", Category.MOVEMENT); }

    @Override public void onTick() {
        float f = mc.player.input.movementForward, s = mc.player.input.movementSideways;
        if (f == 0 && s == 0) return;
        double yaw = Math.toRadians(mc.player.getYaw());
        double dx = -Math.sin(yaw) * f + Math.cos(yaw) * s, dz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.sqrt(dx * dx + dz * dz);
        double sp = 0.2873 * mult.get();
        mc.player.setVelocity(dx / len * sp, mc.player.getVelocity().y, dz / len * sp);
    }
}
