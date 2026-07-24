package lv.editvillager;

import com.mojang.serialization.DynamicOps;
import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

public final class TradeFileStorage {

    private static final String FOLDER_NAME = "trades";
    private static final String FILE_EXT = ".evtrades";

    private TradeFileStorage() {
    }

    public static Path getTradesDirectory() {
        return Platform.getGameFolder().resolve(FOLDER_NAME);
    }

    public static void ensureDirectory() throws IOException {
        Files.createDirectories(getTradesDirectory());
    }

    public static void openTradesFolder() throws IOException {
        Path dir = getTradesDirectory();
        Files.createDirectories(dir);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(dir.toFile());
        } else {
            throw new IOException("Desktop not supported");
        }
    }

    public static Path resolveTradeFile(String name) {
        String sanitized = sanitizeFileName(name);
        if (!sanitized.endsWith(FILE_EXT)) {
            sanitized = sanitized + FILE_EXT;
        }
        return getTradesDirectory().resolve(sanitized);
    }

    public static String sanitizeFileName(String raw) {
        String name = raw.trim();
        if (name.toLowerCase().endsWith(FILE_EXT)) {
            name = name.substring(0, name.length() - FILE_EXT.length());
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("spaces");
        }
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.isBlank()) {
            throw new IllegalArgumentException("empty");
        }
        return name;
    }

    public static void saveAll(Villager villager, String fileName) throws IOException {
        ensureDirectory();
        Path file = resolveTradeFile(fileName);
        NbtIo.writeCompressed(serializeVillagerTrades(villager), file);
    }

    public static void loadAll(Villager villager, String fileName) throws IOException {
        Path file = resolveTradeFile(fileName).normalize();
        if (!file.startsWith(getTradesDirectory().toAbsolutePath().normalize())) {
            throw new IOException("invalid");
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("missing");
        }
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        applyToVillager(villager, root);
    }

    /** Load by exact selected file name from the OS dialog (no space rejection). */
    public static String loadSelectedFile(Villager villager, String rawFileName) throws IOException {
        String name = Path.of(rawFileName).getFileName().toString().trim();
        if (name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("invalid");
        }
        if (!name.toLowerCase().endsWith(FILE_EXT)) {
            name = name + FILE_EXT;
        }
        Path file = getTradesDirectory().resolve(name).toAbsolutePath().normalize();
        if (!file.startsWith(getTradesDirectory().toAbsolutePath().normalize())) {
            throw new IOException("invalid");
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("missing");
        }
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        applyToVillager(villager, root);
        String display = name;
        if (display.toLowerCase().endsWith(FILE_EXT)) {
            display = display.substring(0, display.length() - FILE_EXT.length());
        }
        return display;
    }

    public static CompoundTag serializeVillagerTrades(Villager villager) {
        EvVillagerLock lock = (EvVillagerLock) villager;
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 1);

        CompoundTag levels = new CompoundTag();
        boolean hasAnyLevel = false;
        for (int level = 1; level <= 5; level++) {
            MerchantOffers trades = lock.ev$getCustomLevelTrades(level);
            if (trades != null && !trades.isEmpty()) {
                levels.put(String.valueOf(level), encodeOffers(trades));
                hasAnyLevel = true;
            }
        }
        // If per-level storage is empty, seed the current career level from live offers
        // so reload keeps trades when switching levels in the editor.
        if (!hasAnyLevel) {
            MerchantOffers live = villager.getOffers();
            if (live != null && !live.isEmpty()) {
                int level = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
                levels.put(String.valueOf(level), encodeOffers(live));
            }
        }
        root.put("Levels", levels);
        root.put("Offers", encodeOffers(villager.getOffers()));
        root.putBoolean("KeepTrades", lock.ev$shouldKeepTrades());
        root.putBoolean("TradesLocked", lock.ev$areTradesLocked());
        return root;
    }

    public static void applyToVillager(Villager villager, CompoundTag root) {
        EvVillagerLock lock = (EvVillagerLock) villager;

        // Clear existing per-level trades before applying the file.
        for (int level = 1; level <= 5; level++) {
            lock.ev$setCustomLevelTrades(level, new MerchantOffers());
        }

        boolean[] hasLevel = { false };
        root.getCompound("Levels").ifPresent(levels -> {
            for (String key : levels.keySet()) {
                try {
                    int level = Integer.parseInt(key);
                    levels.getCompound(key).ifPresent(levelNbt -> {
                        MerchantOffers offers = decodeOffers(levelNbt);
                        if (!offers.isEmpty()) {
                            lock.ev$setCustomLevelTrades(level, offers);
                            hasLevel[0] = true;
                        }
                    });
                } catch (NumberFormatException ignored) {
                }
            }
        });

        MerchantOffers flatOffers = new MerchantOffers();
        root.getCompound("Offers").ifPresent(offersNbt -> {
            flatOffers.addAll(decodeOffers(offersNbt));
        });

        if (!flatOffers.isEmpty()) {
            lock.ev$forceSetOffers(flatOffers);
        }

        // Older/partial files may only store Offers — put them into the current level.
        if (!hasLevel[0] && !flatOffers.isEmpty()) {
            int level = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
            lock.ev$setCustomLevelTrades(level, flatOffers);
        }

        root.getBoolean("KeepTrades").ifPresent(lock::ev$setKeepTrades);
        root.getBoolean("TradesLocked").ifPresent(lock::ev$setTradesLocked);
        // Do not sync flat→levels after load: that redistributes/wipes stored tiers.
    }

    private static CompoundTag encodeOffers(MerchantOffers offers) {
        CompoundTag tag = new CompoundTag();
        DynamicOps<net.minecraft.nbt.Tag> ops = NbtOps.INSTANCE;
        for (int i = 0; i < offers.size(); i++) {
            final int index = i;
            MerchantOffer offer = offers.get(i);
            MerchantOffer.CODEC.encodeStart(ops, offer)
                    .resultOrPartial(err -> {
                    })
                    .ifPresent(encoded -> tag.put("Trade" + index, encoded));
        }
        return tag;
    }

    private static MerchantOffers decodeOffers(CompoundTag tag) {
        Map<Integer, MerchantOffer> sorted = new TreeMap<>();
        DynamicOps<net.minecraft.nbt.Tag> ops = NbtOps.INSTANCE;
        for (String key : tag.keySet()) {
            if (!key.startsWith("Trade")) {
                continue;
            }
            try {
                int index = Integer.parseInt(key.substring(5));
                tag.getCompound(key).ifPresent(tradeNbt -> MerchantOffer.CODEC.parse(ops, tradeNbt)
                        .resultOrPartial(err -> {
                        })
                        .ifPresent(offer -> sorted.put(index, offer)));
            } catch (NumberFormatException ignored) {
            }
        }
        MerchantOffers offers = new MerchantOffers();
        offers.addAll(sorted.values());
        return offers;
    }
}
