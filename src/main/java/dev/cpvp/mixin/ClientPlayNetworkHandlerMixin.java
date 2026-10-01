package dev.cpvp.mixin;

import dev.cpvp.module.impl.FakeLagModule;
import dev.cpvp.module.impl.VelocityModule;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void cpvp$sendPacket(Packet<?> packet, CallbackInfo ci) {
        if (FakeLagModule.intercept(packet)) ci.cancel();
    }

    /** TAIL = main-thread pass only (the first, network-thread call throws before reaching here). */
    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void cpvp$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        VelocityModule.apply(packet);
    }
}
