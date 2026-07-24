package lv.editvillager;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class TradeFilePayloads {

    public static final Identifier OPEN_LOAD_DIALOG_ID = Identifier.of("editvillager", "open_trade_load");
    public static final Identifier TRADE_FILE_SELECTED_ID = Identifier.of("editvillager", "trade_file_selected");
    public static final Identifier TRADE_FILE_APPLY_ID = Identifier.of("editvillager", "trade_file_apply");
    public static final Identifier TRADE_FILE_LOAD_CANCELLED_ID = Identifier.of("editvillager", "trade_file_load_cancelled");

    /** Max compressed .evtrades size accepted from the client. */
    public static final int MAX_FILE_BYTES = 512 * 1024;

    private TradeFilePayloads() {
    }

    public record OpenTradeLoadDialogPayload(UUID villagerId) implements CustomPayload {
        public static final Id<OpenTradeLoadDialogPayload> ID = new Id<>(OPEN_LOAD_DIALOG_ID);
        public static final PacketCodec<RegistryByteBuf, OpenTradeLoadDialogPayload> CODEC =
                PacketCodec.tuple(Uuids.PACKET_CODEC, OpenTradeLoadDialogPayload::villagerId, OpenTradeLoadDialogPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Apply a file already copied into the server/shared trades folder. */
    public record TradeFileApplyPayload(UUID villagerId, String fileName) implements CustomPayload {
        public static final Id<TradeFileApplyPayload> ID = new Id<>(TRADE_FILE_APPLY_ID);
        public static final PacketCodec<RegistryByteBuf, TradeFileApplyPayload> CODEC =
                PacketCodec.tuple(
                        Uuids.PACKET_CODEC, TradeFileApplyPayload::villagerId,
                        PacketCodecs.STRING, TradeFileApplyPayload::fileName,
                        TradeFileApplyPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TradeFileSelectedPayload(UUID villagerId, String fileName, byte[] contents) implements CustomPayload {
        public static final Id<TradeFileSelectedPayload> ID = new Id<>(TRADE_FILE_SELECTED_ID);
        public static final PacketCodec<RegistryByteBuf, TradeFileSelectedPayload> CODEC =
                PacketCodec.tuple(
                        Uuids.PACKET_CODEC, TradeFileSelectedPayload::villagerId,
                        PacketCodecs.STRING, TradeFileSelectedPayload::fileName,
                        PacketCodecs.byteArray(MAX_FILE_BYTES), TradeFileSelectedPayload::contents,
                        TradeFileSelectedPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TradeFileLoadCancelledPayload() implements CustomPayload {
        public static final Id<TradeFileLoadCancelledPayload> ID = new Id<>(TRADE_FILE_LOAD_CANCELLED_ID);
        public static final PacketCodec<RegistryByteBuf, TradeFileLoadCancelledPayload> CODEC =
                PacketCodec.unit(new TradeFileLoadCancelledPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
