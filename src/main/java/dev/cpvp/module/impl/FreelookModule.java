package dev.cpvp.module.impl;

import com.mojang.authlib.GameProfile;
import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.NumberSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/** Look around freely while your real aim stays where it was. Camera orbits behind you. */
public final class FreelookModule extends Module {
    private final NumberSetting distance = add(new NumberSetting("Distance", 0, 8, 0.5, 3.5));

    private OtherClientPlayerEntity cam;
    private float savedYaw, savedPitch, camYaw, camPitch;

    public FreelookModule() { super("Freelook", "Look around without turning", Category.RENDER); }

    @Override public void onEnable() {
        if (mc.player == null || mc.world == null) { setEnabled(false); return; }
        cam = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "Freelook"));
        savedYaw = camYaw = mc.player.getYaw();
        savedPitch = camPitch = mc.player.getPitch();
        mc.setCameraEntity(cam);
    }

    @Override public void onDisable() {
        if (cam == null) return;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        }
        cam = null;
    }

    @Override public void onFrame() {
        if (cam == null || mc.player == null) return;
        camYaw += mc.player.getYaw() - savedYaw;
        camPitch = MathHelper.clamp(camPitch + mc.player.getPitch() - savedPitch, -90f, 90f);
        mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        mc.player.prevYaw = savedYaw; mc.player.prevPitch = savedPitch;

        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d eye = mc.player.getLerpedPos(td).add(0, mc.player.getStandingEyeHeight(), 0);
        double y = Math.toRadians(camYaw), p = Math.toRadians(camPitch);
        Vec3d fwd = new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3d pos = eye.subtract(fwd.multiply(distance.get()));
        cam.refreshPositionAndAngles(pos.x, pos.y - 1.62, pos.z, camYaw, camPitch);
    }
}
