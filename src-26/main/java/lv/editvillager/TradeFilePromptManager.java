package lv.editvillager;

import lv.editvillager.TradeFileNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

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

    public static void beginSave(ServerPlayer player, Villager villager) {
        LanguageManager.bind(player);
        PENDING.put(player.getUUID(), new PendingPrompt(
                PromptType.SAVE,
                villager.getUUID(),
                System.currentTimeMillis() + PROMPT_TIMEOUT_MS));
        player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.prompt.save")), false);
        player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.prompt.no_spaces")), false);
        sendCancelButton(player);
    }

    public static void beginLoad(ServerPlayer player, Villager villager) {
        LanguageManager.bind(player);
        PENDING.put(player.getUUID(), new PendingPrompt(
                PromptType.LOAD,
                villager.getUUID(),
                System.currentTimeMillis() + PROMPT_TIMEOUT_MS));
        player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.prompt.load")), false);
        sendCancelButton(player);
        TradeFileNetworking.sendOpenLoadDialog(player, villager.getUUID());
    }

    public static int cancel(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);
        if (!PENDING.containsKey(player.getUUID())) {
            source.sendFailure(Component.literal(LanguageManager.tr("trades.files.msg.nothing_to_cancel")));
            return 0;
        }
        cancel(player);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("trades.files.msg.cancelled")), false);
        return 1;
    }

    public static void cancel(ServerPlayer player) {
        LanguageManager.bind(player);
        PENDING.remove(player.getUUID());
    }

    public static boolean hasPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    public static boolean handleChat(ServerPlayer player, String message) {
        PendingPrompt prompt = PENDING.get(player.getUUID());
        if (prompt == null || prompt.type() != PromptType.SAVE) {
            return false;
        }
        if (System.currentTimeMillis() > prompt.expiresAtMs()) {
            PENDING.remove(player.getUUID());
            return false;
        }

        LanguageManager.bind(player);
        String text = message.trim();
        if (text.isEmpty()) {
            return false;
        }

        Villager villager = findVillager(player, prompt.villagerId());
        if (villager == null) {
            PENDING.remove(player.getUUID());
            player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.no_villager")), false);
            return true;
        }

        try {
            String fileName = TradeFileStorage.sanitizeFileName(text);
            TradeFileStorage.saveAll(villager, fileName);
            player.sendSystemMessage(
                    Component.literal(LanguageManager.tr("trades.files.msg.saved", fileName)), false);
        } catch (IllegalArgumentException e) {
            String key = "spaces".equals(e.getMessage())
                    ? "trades.files.msg.no_spaces"
                    : "trades.files.msg.invalid_name";
            player.sendSystemMessage(Component.literal(LanguageManager.tr(key)), false);
            return true;
        } catch (IOException e) {
            player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.error")), false);
            return true;
        }

        PENDING.remove(player.getUUID());
        return true;
    }

    public static void handleFileSelected(ServerPlayer player, UUID villagerId, String fileName) {
        PendingPrompt prompt = PENDING.get(player.getUUID());
        if (prompt == null || prompt.type() != PromptType.LOAD || !prompt.villagerId().equals(villagerId)) {
            return;
        }
        if (System.currentTimeMillis() > prompt.expiresAtMs()) {
            PENDING.remove(player.getUUID());
            return;
        }

        LanguageManager.bind(player);
        Villager villager = findVillager(player, villagerId);
        if (villager == null) {
            PENDING.remove(player.getUUID());
            player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.no_villager")), false);
            return;
        }

        try {
            String loadedName = TradeFileStorage.loadSelectedFile(villager, fileName);
            player.sendSystemMessage(
                    Component.literal(LanguageManager.tr("trades.files.msg.loaded", loadedName)), false);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.invalid_name")), false);
        } catch (IOException e) {
            if ("missing".equals(e.getMessage())) {
                player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.not_found")), false);
            } else {
                player.sendSystemMessage(Component.literal(LanguageManager.tr("trades.files.msg.error")), false);
            }
        }

        PENDING.remove(player.getUUID());
    }

    private static void sendCancelButton(ServerPlayer player) {
        Component cancel = Component.literal(LanguageManager.tr("trades.files.button.cancel"))
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/ev tradefiles cancel"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal(LanguageManager.tr("trades.files.button.cancel.hover")))));
        player.sendSystemMessage(cancel, false);
    }

    private static Villager findVillager(ServerPlayer player, UUID villagerId) {
        for (var level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(villagerId);
            if (entity instanceof Villager villager) {
                return villager;
            }
        }
        return null;
    }
}
