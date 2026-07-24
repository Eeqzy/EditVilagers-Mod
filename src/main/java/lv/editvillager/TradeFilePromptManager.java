package lv.editvillager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TradeFilePromptManager {

    public enum PromptType {
        SAVE,
        LOAD
    }

    private record PendingPrompt(PromptType type, UUID villagerId, long expiresAtMs) {
    }

    private static final long PROMPT_TIMEOUT_MS = 120_000L;
    private static final Map<UUID, PendingPrompt> PENDING = new ConcurrentHashMap<>();

    private TradeFilePromptManager() {
    }

    public static void beginSave(ServerPlayerEntity player, VillagerEntity villager) {
        LanguageManager.bind(player);
        PENDING.put(player.getUuid(), new PendingPrompt(
                PromptType.SAVE,
                villager.getUuid(),
                System.currentTimeMillis() + PROMPT_TIMEOUT_MS));
        player.sendMessage(Text.literal(LanguageManager.tr("trades.files.prompt.save")), false);
        player.sendMessage(Text.literal(LanguageManager.tr("trades.files.prompt.no_spaces")), false);
        sendCancelButton(player);
    }

    public static void beginLoad(ServerPlayerEntity player, VillagerEntity villager) {
        LanguageManager.bind(player);
        PENDING.put(player.getUuid(), new PendingPrompt(
                PromptType.LOAD,
                villager.getUuid(),
                System.currentTimeMillis() + PROMPT_TIMEOUT_MS));
        player.sendMessage(Text.literal(LanguageManager.tr("trades.files.prompt.load")), false);
        sendCancelButton(player);
        TradeFileNetworking.sendOpenLoadDialog(player, villager.getUuid());
    }

    public static int cancel(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);
        if (!PENDING.containsKey(player.getUuid())) {
            source.sendError(Text.literal(LanguageManager.tr("trades.files.msg.nothing_to_cancel")));
            return 0;
        }
        cancel(player);
        source.sendFeedback(() -> Text.literal(LanguageManager.tr("trades.files.msg.cancelled")), false);
        return 1;
    }

    public static void cancel(ServerPlayerEntity player) {
        LanguageManager.bind(player);
        PENDING.remove(player.getUuid());
    }

    public static boolean hasPending(ServerPlayerEntity player) {
        return PENDING.containsKey(player.getUuid());
    }

    public static boolean handleChat(ServerPlayerEntity player, String message) {
        PendingPrompt prompt = PENDING.get(player.getUuid());
        if (prompt == null || prompt.type() != PromptType.SAVE) {
            return false;
        }
        if (System.currentTimeMillis() > prompt.expiresAtMs()) {
            PENDING.remove(player.getUuid());
            return false;
        }

        LanguageManager.bind(player);
        String text = message.trim();
        if (text.isEmpty()) {
            return false;
        }

        VillagerEntity villager = findVillager(player, prompt.villagerId());
        if (villager == null) {
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.no_villager")), false);
            return true;
        }

        try {
            String fileName = TradeFileStorage.sanitizeFileName(text);
            TradeFileStorage.saveAll(villager, fileName);
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.saved", fileName)), false);
        } catch (IllegalArgumentException e) {
            String key = "spaces".equals(e.getMessage())
                    ? "trades.files.msg.no_spaces"
                    : "trades.files.msg.invalid_name";
            player.sendMessage(Text.literal(LanguageManager.tr(key)), false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
            return true;
        }

        PENDING.remove(player.getUuid());
        return true;
    }

    public static int applyFromCommand(ServerCommandSource source, String fileName) {
        try {
            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                source.sendError(Text.literal(LanguageManager.tr("command.error.player_only")));
                return 0;
            }
            PendingPrompt prompt = PENDING.get(player.getUuid());
            if (prompt == null || prompt.type() != PromptType.LOAD) {
                source.sendError(Text.literal(LanguageManager.tr("trades.files.msg.nothing_to_cancel")));
                return 0;
            }
            return finishLoad(player, prompt.villagerId(), fileName, null) ? 1 : 0;
        } catch (Exception e) {
            e.printStackTrace();
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
            }
            return 0;
        }
    }

    public static void handleFileSelected(ServerPlayerEntity player, UUID villagerId, String fileName, byte[] contents) {
        try {
            finishLoad(player, villagerId, fileName, contents);
        } catch (Exception e) {
            e.printStackTrace();
            LanguageManager.bind(player);
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
        }
    }

    private static boolean finishLoad(ServerPlayerEntity player, UUID villagerId, String fileName, byte[] contents) {
        PendingPrompt prompt = PENDING.get(player.getUuid());
        if (prompt == null || prompt.type() != PromptType.LOAD) {
            return false;
        }
        if (!prompt.villagerId().equals(villagerId)) {
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.no_villager")), false);
            return false;
        }
        if (System.currentTimeMillis() > prompt.expiresAtMs()) {
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.cancelled")), false);
            return false;
        }

        LanguageManager.bind(player);
        VillagerEntity villager = findVillager(player, villagerId);
        if (villager == null) {
            PENDING.remove(player.getUuid());
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.no_villager")), false);
            return false;
        }

        try {
            String loadedName = contents != null && contents.length > 0
                    ? TradeFileStorage.loadFromBytes(villager, fileName, contents)
                    : TradeFileStorage.loadSelectedFile(villager, fileName);
            int offerCount = villager.getOffers() != null ? villager.getOffers().size() : 0;
            if (offerCount == 0) {
                player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
                PENDING.remove(player.getUuid());
                return false;
            }
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.loaded", loadedName)), false);
            PENDING.remove(player.getUuid());
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.invalid_name")), false);
        } catch (IOException e) {
            if ("missing".equals(e.getMessage())) {
                player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.not_found")), false);
            } else {
                e.printStackTrace();
                player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("trades.files.msg.error")), false);
        }
        return false;
    }

    private static void sendCancelButton(ServerPlayerEntity player) {
        Text cancel = Text.literal(LanguageManager.tr("trades.files.button.cancel"))
                .styled(style -> style
                        .withColor(Formatting.RED)
                        .withBold(true)
                        //? if 1.21.1 {
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ev tradefiles cancel"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal(LanguageManager.tr("trades.files.button.cancel.hover"))))
                        //?} else {
                        .withClickEvent(new ClickEvent.RunCommand("/ev tradefiles cancel"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal(LanguageManager.tr("trades.files.button.cancel.hover"))))
                        //?}
                );
        player.sendMessage(cancel, false);
    }

    private static VillagerEntity findVillager(ServerPlayerEntity player, UUID villagerId) {
        var server = player.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(villagerId);
            //? if !(1.21.1) {
            if (entity == null) {
                entity = world.getEntityAnyDimension(villagerId);
            }
            //?}
            if (entity instanceof VillagerEntity villager) {
                return villager;
            }
        }
        return null;
    }
}
