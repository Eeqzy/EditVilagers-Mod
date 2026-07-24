package lv.editvillager;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public final class TradeFilePayloads {

    public static final Identifier OPEN_LOAD_DIALOG_ID =
            Identifier.fromNamespaceAndPath("editvillager", "open_trade_load");
    public static final Identifier TRADE_FILE_SELECTED_ID =
            Identifier.fromNamespaceAndPath("editvillager", "trade_file_selected");
    public static final Identifier TRADE_FILE_LOAD_CANCELLED_ID =
            Identifier.fromNamespaceAndPath("editvillager", "trade_file_load_cancelled");

    private TradeFilePayloads() {
    }

    public record OpenTradeLoadDialogPayload(UUID villagerId) implements CustomPacketPayload {
        public static final Type<OpenTradeLoadDialogPayload> TYPE =
                new Type<>(OPEN_LOAD_DIALOG_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenTradeLoadDialogPayload> CODEC =
                StreamCodec.composite(
                        UUID_CODEC, OpenTradeLoadDialogPayload::villagerId,
                        OpenTradeLoadDialogPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TradeFileSelectedPayload(UUID villagerId, String fileName) implements CustomPacketPayload {
        public static final Type<TradeFileSelectedPayload> TYPE =
                new Type<>(TRADE_FILE_SELECTED_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeFileSelectedPayload> CODEC =
                StreamCodec.composite(
                        UUID_CODEC, TradeFileSelectedPayload::villagerId,
                        ByteBufCodecs.STRING_UTF8, TradeFileSelectedPayload::fileName,
                        TradeFileSelectedPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TradeFileLoadCancelledPayload() implements CustomPacketPayload {
        public static final Type<TradeFileLoadCancelledPayload> TYPE =
                new Type<>(TRADE_FILE_LOAD_CANCELLED_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeFileLoadCancelledPayload> CODEC =
                StreamCodec.unit(new TradeFileLoadCancelledPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong()));
}
