package lv.editvillager;

import com.mojang.serialization.DynamicOps;
import dev.architectury.platform.Platform;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    /** Copy a picked OS file into the local trades folder (helps singleplayer load). */
    public static Path copyIntoTradesFolder(Path selectedFile) throws IOException {
        ensureDirectory();
        String fileName = selectedFile.getFileName().toString();
        Path dest = resolveTradeFile(fileName);
        Files.copy(selectedFile, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    public static void saveAll(VillagerEntity villager, String fileName) throws IOException {
        ensureDirectory();
        Path file = resolveTradeFile(fileName);
        NbtIo.writeCompressed(serializeVillagerTrades(villager), file);
    }

    public static String loadSelectedFile(VillagerEntity villager, String rawFileName) throws IOException {
        Path file = resolveExistingTradeFile(rawFileName);
        NbtCompound root = NbtIo.readCompressed(file, NbtSizeTracker.ofUnlimitedBytes());
        applyToVillager(villager, root);
        return displayName(file.getFileName().toString());
    }

    public static String loadFromBytes(VillagerEntity villager, String rawFileName, byte[] contents) throws IOException {
        if (contents == null || contents.length == 0) {
            return loadSelectedFile(villager, rawFileName);
        }
        if (contents.length > TradeFilePayloads.MAX_FILE_BYTES) {
            throw new IOException("too_large");
        }
        NbtCompound root = NbtIo.readCompressed(
                new java.io.ByteArrayInputStream(contents),
                NbtSizeTracker.ofUnlimitedBytes());
        applyToVillager(villager, root);

        try {
            ensureDirectory();
            String name = Path.of(rawFileName).getFileName().toString();
            if (!name.toLowerCase().endsWith(FILE_EXT)) {
                name = name + FILE_EXT;
            }
            Files.write(getTradesDirectory().resolve(name), contents);
        } catch (IOException ignored) {
        }

        return displayName(rawFileName);
    }

    private static Path resolveExistingTradeFile(String rawFileName) throws IOException {
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
        return file;
    }

    private static String displayName(String raw) {
        String display = Path.of(raw).getFileName().toString();
        if (display.toLowerCase().endsWith(FILE_EXT)) {
            display = display.substring(0, display.length() - FILE_EXT.length());
        }
        return display;
    }

    public static NbtCompound serializeVillagerTrades(VillagerEntity villager) {
        EvVillagerLock lock = (EvVillagerLock) villager;
        RegistryWrapper.WrapperLookup lookup = villager.getRegistryManager();
        NbtCompound root = new NbtCompound();
        root.putInt("Version", 2);

        // Codec-encoded offers — reliable round-trip without Entity save/load
        DynamicOps<NbtElement> offerOps = ops(lookup);
        TradeOfferList liveOffers = villager.getOffers();
        if (liveOffers != null && !liveOffers.isEmpty()) {
            TradeOfferList.CODEC.encodeStart(offerOps, liveOffers).result()
                    .ifPresent(el -> root.put("OffersVanilla", el));
        }

        NbtCompound levels = new NbtCompound();
        boolean hasAnyLevel = false;
        for (int level = 1; level <= 5; level++) {
            TradeOfferList trades = lock.ev$getCustomLevelTrades(level);
            if (trades != null && !trades.isEmpty()) {
                levels.put(String.valueOf(level), encodeOffers(trades, lookup));
                hasAnyLevel = true;
            }
        }
        if (!hasAnyLevel) {
            TradeOfferList live = villager.getOffers();
            if (live != null && !live.isEmpty()) {
                int level = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
                levels.put(String.valueOf(level), encodeOffers(live, lookup));
            }
        }
        root.put("Levels", levels);
        root.put("Offers", encodeOffers(villager.getOffers(), lookup));
        root.putBoolean("KeepTrades", lock.ev$shouldKeepTrades());
        root.putBoolean("TradesLocked", lock.ev$areTradesLocked());
        return root;
    }

    public static void applyToVillager(VillagerEntity villager, NbtCompound root) {
        if (!(villager instanceof EvVillagerLock lock)) {
            throw new IllegalStateException("Villager mixin missing");
        }
        RegistryWrapper.WrapperLookup lookup = villager.getRegistryManager();
        DynamicOps<NbtElement> offerOps = ops(lookup);

        for (int level = 1; level <= 5; level++) {
            lock.ev$setCustomLevelTrades(level, new TradeOfferList());
        }

        boolean hasLevel = false;
        int highestLevelWithTrades = 0;
        NbtCompound levels = NbtCompat.getCompound(root, "Levels");
        for (String key : levels.getKeys()) {
            try {
                int level = Integer.parseInt(key);
                TradeOfferList offers = decodeLevelOffers(levels, key, offerOps, lookup);
                if (!offers.isEmpty()) {
                    lock.ev$setCustomLevelTrades(level, offers);
                    hasLevel = true;
                    highestLevelWithTrades = Math.max(highestLevelWithTrades, level);
                }
            } catch (NumberFormatException ignored) {
            } catch (Exception e) {
                System.err.println("[EditVillagers] level " + key + ": " + e.getMessage());
            }
        }

        if (root.contains("KeepTrades")) {
            lock.ev$setKeepTrades(NbtCompat.getBoolean(root, "KeepTrades", false));
        }
        if (root.contains("TradesLocked")) {
            lock.ev$setTradesLocked(NbtCompat.getBoolean(root, "TradesLocked", true));
        }

        // Prefer codec / vanilla offers blob
        if (root.contains("OffersVanilla")) {
            TradeOfferList vanilla = VillagerCloneHelper.decodeOffersElement(root.get("OffersVanilla"), offerOps);
            if (!vanilla.isEmpty()) {
                lock.ev$forceSetOffers(vanilla);
                if (!hasLevel) {
                    int level = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
                    lock.ev$setCustomLevelTrades(level, copyOffers(vanilla));
                }
                return;
            }
        }

        TradeOfferList flatOffers = decodeOffers(NbtCompat.getCompound(root, "Offers"), lookup);
        if (flatOffers.isEmpty()) {
            // Offers might itself be a codec list stored under "Offers"
            flatOffers = VillagerCloneHelper.decodeOffersElement(root.get("Offers"), offerOps);
        }
        if (!flatOffers.isEmpty()) {
            lock.ev$forceSetOffers(flatOffers);
        } else if (hasLevel) {
            int upTo = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
            if (highestLevelWithTrades > 0) {
                upTo = Math.max(upTo, highestLevelWithTrades);
            }
            lock.ev$rebuildOffersFromMenu(upTo);
        }

        if (!hasLevel && !flatOffers.isEmpty()) {
            int level = Math.max(1, Math.min(5, ReflectionUtils.getLevel(villager.getVillagerData())));
            lock.ev$setCustomLevelTrades(level, flatOffers);
        }
    }

    private static TradeOfferList decodeLevelOffers(
            NbtCompound levels, String key, DynamicOps<NbtElement> ops, RegistryWrapper.WrapperLookup lookup) {
        NbtElement raw = levels.get(key);
        if (raw == null) {
            return new TradeOfferList();
        }
        TradeOfferList fromCodec = VillagerCloneHelper.decodeOffersElement(raw, ops);
        if (!fromCodec.isEmpty()) {
            return fromCodec;
        }
        if (raw instanceof NbtCompound compound) {
            return decodeOffers(compound, lookup);
        }
        return new TradeOfferList();
    }

    private static TradeOfferList copyOffers(TradeOfferList source) {
        TradeOfferList copy = new TradeOfferList();
        if (source == null) {
            return copy;
        }
        for (TradeOffer offer : source) {
            copy.add(offer.copy());
        }
        return copy;
    }

    private static DynamicOps<NbtElement> ops(RegistryWrapper.WrapperLookup lookup) {
        if (lookup != null) {
            return RegistryOps.of(NbtOps.INSTANCE, lookup);
        }
        return NbtOps.INSTANCE;
    }

    private static NbtCompound encodeOffers(TradeOfferList offers, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound tag = new NbtCompound();
        if (offers == null || offers.isEmpty()) {
            return tag;
        }
        DynamicOps<NbtElement> primary = ops(lookup);
        DynamicOps<NbtElement> fallback = NbtOps.INSTANCE;
        for (int i = 0; i < offers.size(); i++) {
            final int index = i;
            TradeOffer offer = offers.get(i);
            var encoded = TradeOffer.CODEC.encodeStart(primary, offer);
            if (encoded.result().isEmpty()) {
                encoded = TradeOffer.CODEC.encodeStart(fallback, offer);
            }
            encoded
                    .resultOrPartial(err -> System.err.println("[EditVillagers] encode trade " + index + ": " + err))
                    .ifPresent(el -> tag.put("Trade" + index, el));
        }
        return tag;
    }

    private static TradeOfferList decodeOffers(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        Map<Integer, TradeOffer> sorted = new TreeMap<>();
        if (tag == null || tag.isEmpty()) {
            return new TradeOfferList();
        }
        DynamicOps<NbtElement> primary = ops(lookup);
        DynamicOps<NbtElement> fallback = NbtOps.INSTANCE;
        for (String key : tag.getKeys()) {
            if (!key.startsWith("Trade")) {
                continue;
            }
            try {
                int index = Integer.parseInt(key.substring(5));
                NbtElement raw = tag.get(key);
                if (raw == null) {
                    continue;
                }
                var parsed = TradeOffer.CODEC.parse(primary, raw);
                if (parsed.result().isEmpty()) {
                    parsed = TradeOffer.CODEC.parse(fallback, raw);
                }
                parsed
                        .resultOrPartial(err -> System.err.println("[EditVillagers] decode trade " + key + ": " + err))
                        .ifPresent(offer -> sorted.put(index, offer));
            } catch (NumberFormatException ignored) {
            }
        }
        TradeOfferList offers = new TradeOfferList();
        offers.addAll(sorted.values());
        return offers;
    }
}
