package lv.editvillager;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.function.BiConsumer;

public final class TradeFileNetworking {

    private static BiConsumer<ServerPlayerEntity, UUID> openLoadDialogSender = (player, villagerId) -> {
    };

    private TradeFileNetworking() {
    }

    public static void setOpenLoadDialogSender(BiConsumer<ServerPlayerEntity, UUID> sender) {
        openLoadDialogSender = sender != null ? sender : (player, villagerId) -> {
        };
    }

    public static void sendOpenLoadDialog(ServerPlayerEntity player, UUID villagerId) {
        openLoadDialogSender.accept(player, villagerId);
    }
}
