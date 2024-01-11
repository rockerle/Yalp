package org.rockerle.airprinter.airprinter.client.mixins;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {

    @Inject(at=@At("HEAD"),method="sendPacket")
    public void onSendPacket(Packet<?> packet, CallbackInfo ci){
        if(packet instanceof PlayerInteractBlockC2SPacket){
            System.out.println("Sending playerInteractBlockPacket");
        }
    }
}