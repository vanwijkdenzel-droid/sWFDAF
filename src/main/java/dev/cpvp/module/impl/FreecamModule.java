package dev.cpvp.module.impl;

import com.mojang.authlib.GameProfile;
import dev.cpvp.module.Category;
import dev.cpvp.module.Module;
import dev.cpvp.setting.NumberSetting;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Detached camera. A client-only dummy entity becomes the camera; your real player's input is
 * blanked and its rotation is pinned, so the server sees you standing still.
 */
public final class FreecamModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.5, 8, 0.5, 2.0));

    private OtherClientPlayerEntity cam;
    private Input savedInput;
    private float savedYaw, savedPitch, camYaw, camPitch;
    private Vec3d pos = Vec3d.ZERO;
    private long last;

    public FreecamModule() { super("Freecam", "Detached camera that does not move you", Category.RENDER); }

    @Override public void onEnable() {
        if (mc.player == null || mc.world == null) { setEnabled(false); return; }
        cam = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "Freecam"));
        savedYaw = camYaw = mc.player.getYaw();
        savedPitch = camPitch = mc.player.getPitch();
        pos = mc.player.getEyePos();
        place();
        mc.setCameraEntity(cam);
        savedInput = mc.player.input;
        mc.player.input = new Input();
        last = System.nanoTime();
    }

    @Override public void onDisable() {
        if (cam == null) return;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            if (savedInput != null) mc.player.input = savedInput;
            mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        }
        cam = null;
    }

    private void place() { cam.refreshPositionAndAngles(pos.x, pos.y - 1.62, pos.z, camYaw, camPitch); }

    @Override public void onFrame() {
        if (cam == null || mc.player == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        // The mouse turned the real player; move that rotation onto the camera and pin the player.
        camYaw += mc.player.getYaw() - savedYaw;
        camPitch = MathHelper.clamp(camPitch + mc.player.getPitch() - savedPitch, -90f, 90f);
        mc.player.setYaw(savedYaw); mc.player.setPitch(savedPitch);
        mc.player.prevYaw = savedYaw; mc.player.prevPitch = savedPitch;

        double y = Math.toRadians(camYaw), p = Math.toRadians(camPitch);
        Vec3d fwd = new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3d left = new Vec3d(Math.cos(y), 0, Math.sin(y));
        Vec3d mv = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) mv = mv.add(fwd);
        if (mc.options.backKey.isPressed())    mv = mv.subtract(fwd);
        if (mc.options.leftKey.isPressed())    mv = mv.add(left);
        if (mc.options.rightKey.isPressed())   mv = mv.subtract(left);
        if (mc.options.jumpKey.isPressed())    mv = mv.add(0, 1, 0);
        if (mc.options.sneakKey.isPressed())   mv = mv.add(0, -1, 0);
        if (mv.lengthSquared() > 0) pos = pos.add(mv.normalize().multiply(speed.get() * dt * 8.0));
        place();
    }
}
