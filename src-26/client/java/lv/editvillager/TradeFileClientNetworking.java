package lv.editvillager;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public final class TradeFileClientNetworking {

    private TradeFileClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(
                TradeFilePayloads.OpenTradeLoadDialogPayload.TYPE,
                (payload, context) -> {
                    java.util.UUID villagerId = payload.villagerId();
                    Thread thread = new Thread(() -> {
                        String selectedPath = TradeFileDialog.pickTradeFile();
                        Minecraft.getInstance().execute(() -> sendLoadResult(villagerId, selectedPath));
                    }, "ev-trade-file-dialog");
                    thread.setDaemon(false);
                    thread.start();
                });
    }

    private static void sendLoadResult(java.util.UUID villagerId, String selectedPath) {
        if (selectedPath != null && !selectedPath.isBlank()) {
            String fileName = Path.of(selectedPath).getFileName().toString();
            ClientPlayNetworking.send(new TradeFilePayloads.TradeFileSelectedPayload(villagerId, fileName));
        } else {
            ClientPlayNetworking.send(new TradeFilePayloads.TradeFileLoadCancelledPayload());
        }
    }
}
