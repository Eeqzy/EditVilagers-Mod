package lv.editvillager;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Loader-agnostic bridge so common code can open the trade-file load dialog
 * without depending on Fabric/NeoForge networking APIs.
 */
public final class TradeFileNetworking {

    private static BiConsumer<ServerPlayer, UUID> openLoadDialogSender = (player, villagerId) -> {
    };

    private TradeFileNetworking() {
    }

    public static void setOpenLoadDialogSender(BiConsumer<ServerPlayer, UUID> sender) {
        openLoadDialogSender = sender != null ? sender : (player, villagerId) -> {
        };
    }

    public static void sendOpenLoadDialog(ServerPlayer player, UUID villagerId) {
        openLoadDialogSender.accept(player, villagerId);
    }
}
