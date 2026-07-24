package lv.editvillager;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class TradeFileClientNetworking {

    private TradeFileClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(
                TradeFilePayloads.OpenTradeLoadDialogPayload.ID,
                (payload, context) -> {
                    UUID villagerId = payload.villagerId();
                    Thread thread = new Thread(() -> {
                        String selectedPath = TradeFileDialog.pickTradeFile();
                        MinecraftClient.getInstance().execute(() -> sendLoadResult(villagerId, selectedPath));
                    }, "ev-trade-file-dialog");
                    thread.setDaemon(false);
                    thread.start();
                });
    }

    private static void sendLoadResult(UUID villagerId, String selectedPath) {
        if (selectedPath == null || selectedPath.isBlank()) {
            if (ClientPlayNetworking.canSend(TradeFilePayloads.TradeFileLoadCancelledPayload.ID)) {
                ClientPlayNetworking.send(new TradeFilePayloads.TradeFileLoadCancelledPayload());
            }
            return;
        }
        try {
            Path path = Path.of(selectedPath);
            if (!Files.isRegularFile(path)) {
                return;
            }
            Path copied = TradeFileStorage.copyIntoTradesFolder(path);
            String fileName = copied.getFileName().toString();

            // Small C2S packet: UUID + filename only (file already in trades/)
            if (ClientPlayNetworking.canSend(TradeFilePayloads.TradeFileApplyPayload.ID)) {
                ClientPlayNetworking.send(new TradeFilePayloads.TradeFileApplyPayload(villagerId, fileName));
                return;
            }
            // Fallback: include bytes
            byte[] contents = Files.readAllBytes(copied);
            if (contents.length > 0 && contents.length <= TradeFilePayloads.MAX_FILE_BYTES
                    && ClientPlayNetworking.canSend(TradeFilePayloads.TradeFileSelectedPayload.ID)) {
                ClientPlayNetworking.send(
                        new TradeFilePayloads.TradeFileSelectedPayload(villagerId, fileName, contents));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (ClientPlayNetworking.canSend(TradeFilePayloads.TradeFileLoadCancelledPayload.ID)) {
                ClientPlayNetworking.send(new TradeFilePayloads.TradeFileLoadCancelledPayload());
            }
        }
    }
}
