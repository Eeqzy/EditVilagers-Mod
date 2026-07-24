package lv.editvillager;

import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NetworkCompat {

    private NetworkCompat() {
    }

    public static void send(ServerPlayerEntity player, Packet<?> packet) {
        //? if fabric {
        player.networkHandler.sendPacket(packet);
        //?} else {
        /*player.networkHandler.send(packet);*/
        //?}
    }
}