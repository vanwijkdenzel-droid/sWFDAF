package dev.cpvp.module;

import dev.cpvp.module.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public void init() {
        // Combat
        for (Module m : new Module[] {
                new AimAssistModule(), new AutoClickerModule(), new VelocityModule(), new KillAuraModule(),
                new RightClickerModule(), new HitSelectModule(), new WTapModule(), new TriggerBotModule(),
                new CrystalAuraModule(), new AutoAnchorModule(), new SwapStunModule(), new BreachSwapModule(),
                new HoldExplodeModule(), new PearlCatchModule(), new HitboxesModule(), new FakeLagModule(),
                // Movement
                new SpeedModule(), new ScaffoldModule(), new SafeWalkModule(), new JesusModule(),
                new InvMoveModule(), new SpiderModule(), new LongJumpModule(),
                // Render
                new EspModule("ESP", true, true, false), new ChamsModule(), new TracersModule(),
                new EspModule("StorageESP", false, false, true), new XRayModule(), new FullbrightModule(),
                new FreecamModule(), new FreelookModule(), new FovModule(), new HudModule(),
                // Utility
                new ChestStealModule(), new AutoToolModule(), new FastPlaceModule(), new RefillModule(),
                new AutoArmorModule(), new AutoFishModule(), new FastXpModule(), new InvTotemModule(),
                // Player
                new AntiFireballModule(), new BlockInModule(), new NoFallModule(), new BlinkModule(), new FastBowModule(),
                // World
                new AntiDebuffModule(), new AutoBreachModule(),
                // Settings window
                new InterfaceModule() }) modules.add(m);
    }

    public List<Module> all() { return modules; }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    private static boolean inWorld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.world != null;
    }

    public void onTick()  { if (inWorld()) for (Module m : modules) if (m.isEnabled()) m.onTick(); }
    public void onFrame() { if (inWorld()) for (Module m : modules) if (m.isEnabled()) m.onFrame(); }

    public void onHud(DrawContext ctx) {
        for (Module m : modules) if (m.isEnabled()) m.onHudRender(ctx);
    }

    public void onWorldRender(WorldRenderContext ctx) {
        if (inWorld()) for (Module m : modules) if (m.isEnabled()) m.onWorldRender(ctx);
    }

    public void onKey(int key, int action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || action != GLFW.GLFW_PRESS || key < 0) return;
        for (Module m : modules) if (m.getKey() == key) m.toggle();
    }

    public void onMouseClick(int button) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!inWorld() || mc.currentScreen != null) return;
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) m.onRightClick();
            else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) m.onLeftClick();
        }
    }
}
