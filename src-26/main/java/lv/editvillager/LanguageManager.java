package lv.editvillager;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {

    private static final String DEFAULT_LANG = "ru";
    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();
    private static final Map<UUID, String> PLAYER_LANG = new ConcurrentHashMap<>();
    private static final ThreadLocal<UUID> ACTIVE_PLAYER = new ThreadLocal<>();

    static {
        loadLanguages();
    }

    /** Привязать язык текущего игрока для последующих {@link #tr(String)}. */
    public static void bind(Player player) {
        if (player != null) {
            ACTIVE_PLAYER.set(player.getUUID());
        }
    }

    public static void unbind() {
        ACTIVE_PLAYER.remove();
    }

    public static boolean isSupported(String lang) {
        return LANGUAGES.containsKey(lang);
    }

    public static void setLanguage(Player player, String lang) {
        if (player == null || !isSupported(lang)) {
            return;
        }
        PLAYER_LANG.put(player.getUUID(), lang);
        ACTIVE_PLAYER.set(player.getUUID());
    }

    public static String getLanguage(Player player) {
        if (player == null) {
            return DEFAULT_LANG;
        }
        return PLAYER_LANG.getOrDefault(player.getUUID(), DEFAULT_LANG);
    }

    public static String getLanguage(UUID playerId) {
        if (playerId == null) {
            return DEFAULT_LANG;
        }
        return PLAYER_LANG.getOrDefault(playerId, DEFAULT_LANG);
    }

    /** Загрузка из NBT игрока (mixin). */
    public static void loadSavedLanguage(UUID playerId, String lang) {
        if (playerId == null) {
            return;
        }
        if (isSupported(lang)) {
            PLAYER_LANG.put(playerId, lang);
        } else {
            PLAYER_LANG.put(playerId, DEFAULT_LANG);
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            PLAYER_LANG.remove(playerId);
        }
    }

    /** @deprecated глобальный язык больше не используется — см. {@link #setLanguage(Player, String)} */
    @Deprecated
    public static void setLanguage(String lang) {
        UUID id = ACTIVE_PLAYER.get();
        if (id != null && isSupported(lang)) {
            PLAYER_LANG.put(id, lang);
        }
    }

    public static String getCurrentLang() {
        UUID id = ACTIVE_PLAYER.get();
        if (id != null) {
            return PLAYER_LANG.getOrDefault(id, DEFAULT_LANG);
        }
        return DEFAULT_LANG;
    }

    public static String tr(String key) {
        String lang = getCurrentLang();
        Map<String, String> langMap = LANGUAGES.getOrDefault(lang, LANGUAGES.get("en"));
        return langMap.getOrDefault(key, key);
    }

    public static String tr(String key, Object... args) {
        String template = tr(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    public static String tr(Player player, String key) {
        bind(player);
        return tr(key);
    }

    public static String tr(Player player, String key, Object... args) {
        bind(player);
        return tr(key, args);
    }

    private static void loadLanguages() {
        Map<String, String> ru = new HashMap<>();
        Map<String, String> en = new HashMap<>();

        ru.put("command.help.title", "§6§lEditVillagers §7v1.2.0");
        ru.put("command.help.list", "§7Список доступных команд:");
        ru.put("command.help.create", "§e/ev create <имя> §7- Создать нового жителя");
        ru.put("command.help.edit", "§e/ev edit §7- Открыть основное меню");
        ru.put("command.help.trades", "§e/ev trades §7- Редактировать торги");
        ru.put("command.help.name", "§e/ev name <имя> §7- Изменить имя жителю");
        ru.put("command.help.carry", "§e/ev carry §7- Переместить жителя взглядом (отпустить - shift)");
        ru.put("command.help.lang", "§e/ev lang <lang> §7- Сменить язык (ru/en)");

        ru.put("command.error.player_only", "Только игроки могут использовать эту команду");
        ru.put("command.error.no_villager", "Житель не найден (No resident found in sight)");
        ru.put("command.error.spawn_failed", "Не удалось создать жителя");
        ru.put("command.error.invalid_lang", "§cНеверный язык. Используй: ru, en");

        ru.put("command.success.name_changed", "§a[EditVillagers] §fИмя жителя изменено на: %s");
        ru.put("command.success.lang_changed", "§a[EditVillagers] §fЯзык изменен на: %s");
        ru.put("command.success.villager_created", "§aЖитель создан с именем: %s");

        ru.put("carry.picked_up", "§aВы взяли жителя. Нажмите Shift чтобы отпустить.");
        ru.put("carry.released", "§eЖитель отпущен.");

        ru.put("msg.trades_restocked", "§e[EditVillagers] §aТорги обновлены!");
        ru.put("msg.effects_saved", "§aНастройки эффектов сохранены!");
        ru.put("msg.trades_created_locked", "§aЖитель создан (торги бесконечные + зафиксированы)");

        ru.put("effects.continuous.toggle", "§eПостоянный эффект: %s");
        ru.put("effects.continuous.lore.1", "§7Житель будет постоянно");
        ru.put("effects.continuous.lore.2", "§7излучать выбранные частицы.");
        ru.put("effects.continuous.particle", "§eПостоянные частицы: §b%s");
        ru.put("effects.continuous.cycle", "§7ЛКМ - Назад, ПКМ - Вперед");
        ru.put("effects.continuous.count", "§eКол-во частиц: §a%d");
        ru.put("effects.continuous.count.lore.1", "§7ЛКМ: +1 | Shift+ЛКМ: +10");
        ru.put("effects.continuous.count.lore.2", "§7ПКМ: -1 | Shift+ПКМ: -10");
        ru.put("effects.particle.none", "Без частиц");

        ru.put("menu.main.title", "EditVillagers by Eeqzy");
        ru.put("menu.edit_trades.title", "Edit Villager Trades");
        ru.put("menu.settings.title", "Настройки");
        ru.put("menu.trades.title", "Торги");
        ru.put("menu.professions.title", "Профессии");
        ru.put("menu.biomes.title", "Биомы");
        ru.put("menu.effects.title", "Эффекты");
        ru.put("menu.clone.title", "Клонирование жителя");
        ru.put("menu.sum.title", "Колличество торгов");
        ru.put("menu.trade_files.title", "Файлы торгов");

        ru.put("menu.main.settings", "§f§lНастройки");
        ru.put("menu.main.lore.settings", "§7Основные настройки жителя");
        ru.put("menu.main.trades", "§a§lТорги");
        ru.put("menu.main.lore.trades", "§7Редактировать товары и цены");
        ru.put("menu.main.biomes", "§e§lБиомы");
        ru.put("menu.main.lore.biomes", "§7Изменить биом жителя");
        ru.put("menu.main.professions", "§b§lПрофессии");
        ru.put("menu.main.lore.professions", "§7Выбрать профессию жителя");
        ru.put("menu.main.clone", "§d§lКлонирование Жителя");
        ru.put("menu.main.lore.clone", "§7Выбрать способ клонирования");

        ru.put("menu.clone.instant", "§aСоздать рядом");
        ru.put("menu.clone.lore.instant", "§7Спавн копии жителя у игрока");
        ru.put("menu.clone.egg", "§eЯйцо призыва");
        ru.put("menu.clone.lore.egg", "§7Получить яйцо с данными жителя");

        ru.put("msg.cloned", "§a[EditVillagers] §fЖитель склонирован со всеми данными!");
        ru.put("msg.egg_given", "§a[EditVillagers] §fЯйцо с данными жителя выдано!");
        ru.put("msg.clone_fail", "§c[Error] Clone failed: %s");

        ru.put("button.back", "§c§lНазад");
        ru.put("button.confirm", "§e§lПодтвердить изменения");
        ru.put("button.unlimited", "§aБеск.");

        ru.put("trades.level.current", " (Текущий)");
        ru.put("trades.level.editor", " (Редактор)");
        ru.put("trades.button.level", "§aУровень: %s%s");
        ru.put("trades.lore.level.1", "§7Нажмите для выбора уровня");
        ru.put("trades.lore.level.2", "§7торговли, который хотите");
        ru.put("trades.lore.level.3", "§7отредактировать.");

        ru.put("trades.button.effects", "§bЭффекты жителя");

        ru.put("trades.button.vanilla", "§aВанильная прокачка: %s");
        ru.put("trades.lore.vanilla.1", "§7Если §aВключено§7: Житель повышает");
        ru.put("trades.lore.vanilla.2", "§7уровень и открывает новые торги,");
        ru.put("trades.lore.vanilla.3", "§7как в обычной игре.");
        ru.put("trades.lore.vanilla.4", "§7Если §cВыключено§7: Уровень и торги");
        ru.put("trades.lore.vanilla.5", "§7зафиксированы.");

        ru.put("trades.button.keep", "§aСохранение торгов: %s");
        ru.put("trades.lore.keep.1", "§7Если §aВключено§7: Торги с предыдущих");
        ru.put("trades.lore.keep.2", "§7уровней сохраняются при повышении.");
        ru.put("trades.lore.keep.3", "§7Если §cВыключено§7: На каждом уровне");
        ru.put("trades.lore.keep.4", "§7будут только свои торги.");

        ru.put("trades.page.prev", "§a<- Стр. %d");
        ru.put("trades.page.next", "§aСтр. %d ->");
        ru.put("trades.msg.saved", "§aТорги сохранены (Уровень: %s)");

        ru.put("trades.button.files", "§eФайлы торгов");
        ru.put("trades.lore.files", "§7Сохранить или загрузить торги из файла");
        ru.put("trades.files.save", "§aСохранить всё");
        ru.put("trades.files.save.lore", "§7Сохранить все торги в файл");
        ru.put("trades.files.load", "§bЗагрузить торг");
        ru.put("trades.files.load.lore", "§7Загрузить торги из файла");
        ru.put("trades.files.prompt.save", "§e[EditVillagers] §fНапишите в чат §aназвание файла§f для сохранения торгов.");
        ru.put("trades.files.prompt.load", "§e[EditVillagers] §fВыберите файл торгов и нажмите §aВыбрать файл§f.");
        ru.put("trades.files.prompt.no_spaces", "§7Название §cбез пробелов§7 — только буквы, цифры и _");
        ru.put("trades.files.button.cancel", "§c§l[ Отмена ]");
        ru.put("trades.files.button.cancel.hover", "§7Нажмите, чтобы отменить");
        ru.put("trades.files.dialog.title", "Выберите файл торгов");
        ru.put("trades.files.dialog.select", "Выбрать файл");
        ru.put("trades.files.dialog.filter", "Файлы торгов EditVillagers (*.evtrades)");
        ru.put("trades.files.msg.saved", "§a[EditVillagers] §fТорги сохранены в файл: §e%s.evtrades");
        ru.put("trades.files.msg.loaded", "§a[EditVillagers] §fТорги загружены из файла: §e%s.evtrades");
        ru.put("trades.files.msg.cancelled", "§e[EditVillagers] §7Сохранение/загрузка отменена.");
        ru.put("trades.files.msg.nothing_to_cancel", "§e[EditVillagers] §7Нечего отменять.");
        ru.put("trades.files.msg.invalid_name", "§c[EditVillagers] §fНедопустимое название файла.");
        ru.put("trades.files.msg.no_spaces", "§c[EditVillagers] §fНазвание не должно содержать пробелы.");
        ru.put("trades.files.msg.not_found", "§c[EditVillagers] §fФайл не найден в папке trades.");
        ru.put("trades.files.msg.error", "§c[EditVillagers] §fНе удалось выполнить операцию с файлом.");
        ru.put("trades.files.msg.no_villager", "§c[EditVillagers] §fЖитель не найден.");

        ru.put("sum.limit.infinite", "§aЛимит: Бесконечно");
        ru.put("sum.limit.value", "§eЛимит: %d");
        ru.put("sum.change", "§bИзменить лимит");
        ru.put("sum.lore.lmb", "§7ЛКМ: +1 | Shift+ЛКМ: +10");
        ru.put("sum.lore.rmb", "§7ПКМ: -1 | Shift+ПКМ: -10");
        ru.put("sum.restock.on", "§aЕжедневное обновление: Вкл");
        ru.put("sum.restock.off", "§cЕжедневное обновление: Выкл");
        ru.put("sum.lore.restock", "§7Обновляется, если рядом с жителем стоит кровать");
        ru.put("sum.msg.saved", "§aЛимиты торгов сохранены!");

        ru.put("settings.status.on", "§aВкл");
        ru.put("settings.status.off", "§cВыкл");
        ru.put("settings.status.yes", "§aДа");
        ru.put("settings.status.no", "§cНет");
        ru.put("settings.status.enabled", "§aВключено");
        ru.put("settings.status.disabled", "§cОтключено");

        ru.put("settings.button.level", "§fУровень жителя: %s");
        ru.put("settings.lore.level", "§7Установить уровень жителя");

        ru.put("settings.button.immortality", "§fБессмертие: %s");
        ru.put("settings.lore.immortality", "§7Включить неуязвимость жителя");

        ru.put("settings.button.nametag", "§fИмя: %s");
        ru.put("settings.lore.nametag", "§7Показывать имя жителя над головой");

        ru.put("settings.button.lookat", "§fНаблюдать: %s");
        ru.put("settings.lore.lookat", "§7Житель будет всегда смотреть на игрока");

        ru.put("settings.button.glowing", "§fПодсветка: %s");
        ru.put("settings.lore.glowing", "§7Включить эффект свечение жителя");

        ru.put("settings.button.noai", "§eNoAI: %s");
        ru.put("settings.lore.noai", "§7Отключить Искуственный Интелект жителя");

        ru.put("settings.button.silent", "§fЗвуки: %s");
        ru.put("settings.lore.silent", "§7Отключить все звуки жителя");

        ru.put("settings.button.align", "§fВыровнять: %s");
        ru.put("settings.lore.align", "§7Поставить жителя по центру блока");

        ru.put("settings.button.rotate", "§fПовернуть: %s");
        ru.put("settings.lore.rotate", "§7Изменить направление взгляда жителя");

        ru.put("settings.button.resetxp", "§fСбросить: %s");
        ru.put("settings.lore.resetxp", "§7Сбросить уровень и опыт");

        ru.put("settings.button.pricelock", "§fЦены: %s");
        ru.put("settings.lore.pricelock", "§7Отключить изменение цен");

        ru.put("settings.button.noxp", "§fОпыт: %s");
        ru.put("settings.lore.noxp", "§7Отключить опыт при торговли с жителем");

        ru.put("settings.button.baby", "§fРазмер: %s");
        ru.put("settings.lore.baby", "§7Изменить размер жителя");
        ru.put("settings.status.baby", "§eМаленький");
        ru.put("settings.status.adult", "§aБольшой");

        ru.put("settings.button.sum", "§eНастроить количество торгов");

        ru.put("settings.nav.trades", "§aТорги");
        ru.put("settings.nav.professions", "§bПрофессии");
        ru.put("settings.nav.biomes", "§eБиомы");

        ru.put("settings.msg.level_default", "§a[EditVillagers] §fУровень: По умолчанию (Ванильная Прокачка)");
        ru.put("settings.msg.level_fixed", "§a[EditVillagers] §fУровень зафиксирован на: %s");
        ru.put("settings.msg.xp_reset", "§a[EditVillagers] §fОпыт и уровень сброшены до Новичка");
        ru.put("settings.msg.aligned", "§a[EditVillagers] §fЖитель выровнен по центру блока!");
        ru.put("settings.msg.direction", "§a[EditVillagers] §fНаправление: %s");

        ru.put("level.novice", "§7Новичок");
        ru.put("level.apprentice", "§eПодмастерье");
        ru.put("level.journeyman", "§6Ремесленник");
        ru.put("level.expert", "§bЭксперт");
        ru.put("level.master", "§aМастер");
        ru.put("level.default", "§dПо умолчанию");
        ru.put("level.lvl", "§7Lvl %d");

        ru.put("biomes.type.desert", "§eПустыня");
        ru.put("biomes.type.jungle", "§2Джунгли");
        ru.put("biomes.type.plains", "§aРавнины");
        ru.put("biomes.type.savanna", "§6Саванна");
        ru.put("biomes.type.snow", "Снега");
        ru.put("biomes.type.swamp", "§9Болото");
        ru.put("biomes.type.taiga", "§3Тайга");
        ru.put("biomes.msg.changed", "§a[EditVillagers] §fТип жителя изменён");
        ru.put("biomes.msg.error", "§cОшибка: %s");

        ru.put("profession.toolsmith", "§fИнструментальщик");
        ru.put("profession.armorer", "§7Бронник");
        ru.put("profession.farmer", "§aФермер");
        ru.put("profession.shepherd", "§bПастух");
        ru.put("profession.leatherworker", "§6Кожевник");
        ru.put("profession.fisherman", "§3Рыбак");
        ru.put("profession.butcher", "§cМясник");
        ru.put("profession.weaponsmith", "§fОружейник");
        ru.put("profession.fletcher", "§eЛучник");
        ru.put("profession.cartographer", "§fКартограф");
        ru.put("profession.librarian", "§dБиблиотекарь");
        ru.put("profession.cleric", "§5Священник");
        ru.put("profession.mason", "§8Каменщик");
        ru.put("profession.nitwit", "§2Нищий");
        ru.put("profession.none", "§7Безработный");

        ru.put("profession.msg.selected", "§eВыбрано: §f%s");
        ru.put("profession.msg.select_first", "§cСначала выбери профессию!");
        ru.put("profession.msg.applied", "§aПрофессия применена, торги сохранены.");

        ru.put("dir.south", "Юг");
        ru.put("dir.southwest", "Юго-Запад");
        ru.put("dir.west", "Запад");
        ru.put("dir.northwest", "Северо-Запад");
        ru.put("dir.north", "Север");
        ru.put("dir.northeast", "Северо-Восток");
        ru.put("dir.east", "Восток");
        ru.put("dir.southeast", "Юго-Восток");

        en.put("command.help.title", "§6§lEditVillagers §7v1.2.0");
        en.put("command.help.list", "§7Available commands:");
        en.put("command.help.create", "§e/ev create <name> §7- Create a new villager");
        en.put("command.help.edit", "§e/ev edit §7- Open main menu");
        en.put("command.help.trades", "§e/ev trades §7- Edit trades");
        en.put("command.help.name", "§e/ev name <name> §7- Change villager name");
        en.put("command.help.carry", "§e/ev carry §7- Carry villager (release with shift)");
        en.put("command.help.lang", "§e/ev lang <lang> §7- Change language (ru/en)");

        en.put("command.error.player_only", "Only players can use this command");
        en.put("command.error.no_villager", "No villager found in sight");
        en.put("command.error.spawn_failed", "Failed to spawn villager");
        en.put("command.error.invalid_lang", "§cInvalid language. Use: ru, en");

        en.put("command.success.name_changed", "§a[EditVillagers] §fName changed to: %s");
        en.put("command.success.lang_changed", "§a[EditVillagers] §fLanguage changed to: %s");
        en.put("command.success.villager_created", "§aVillager created with name: %s");

        en.put("carry.picked_up", "§aYou picked up the villager. Press Shift to release.");
        en.put("carry.released", "§eVillager released.");

        en.put("msg.trades_restocked", "§e[EditVillagers] §aTrades restocked!");
        en.put("msg.effects_saved", "§aEffect settings saved!");
        en.put("msg.trades_created_locked", "§aVillager created (infinite + locked trades)");

        en.put("effects.continuous.toggle", "§eContinuous effect: %s");
        en.put("effects.continuous.lore.1", "§7The villager will continuously");
        en.put("effects.continuous.lore.2", "§7emit the selected particles.");
        en.put("effects.continuous.particle", "§eContinuous particles: §b%s");
        en.put("effects.continuous.cycle", "§7LMB - Previous, RMB - Next");
        en.put("effects.continuous.count", "§eParticle count: §a%d");
        en.put("effects.continuous.count.lore.1", "§7LMB: +1 | Shift+LMB: +10");
        en.put("effects.continuous.count.lore.2", "§7RMB: -1 | Shift+RMB: -10");
        en.put("effects.particle.none", "None");

        en.put("menu.main.title", "EditVillagers by Eeqzy");
        en.put("menu.edit_trades.title", "Edit Villager Trades");
        en.put("menu.settings.title", "Settings");
        en.put("menu.trades.title", "Trades");
        en.put("menu.professions.title", "Professions");
        en.put("menu.biomes.title", "Biomes");
        en.put("menu.effects.title", "Effects");
        en.put("menu.clone.title", "Clone Villager");
        en.put("menu.sum.title", "Number of Trades");
        en.put("menu.trade_files.title", "Trade Files");

        en.put("menu.main.settings", "§fSettings");
        en.put("menu.main.lore.settings", "§7Main villager settings");
        en.put("menu.main.trades", "§a§lTrades");
        en.put("menu.main.lore.trades", "§7Edit trades and deals");
        en.put("menu.main.biomes", "§e§lBiomes");
        en.put("menu.main.lore.biomes", "§7Change villager biome");
        en.put("menu.main.professions", "§b§lProfessions");
        en.put("menu.main.lore.professions", "§7Select villager profession");
        en.put("menu.main.clone", "§d§lClone Villager");
        en.put("menu.main.lore.clone", "§7Choose how to clone");

        en.put("menu.clone.instant", "§aSpawn Nearby");
        en.put("menu.clone.lore.instant", "§7Spawn a copy at the player");
        en.put("menu.clone.egg", "§eSpawn Egg");
        en.put("menu.clone.lore.egg", "§7Get spawn egg with villager data");

        en.put("msg.cloned", "§a[EditVillagers] §fVillager cloned successfully!");
        en.put("msg.egg_given", "§a[EditVillagers] §fVillager spawn egg given!");
        en.put("msg.clone_fail", "§c[Error] Clone failed: %s");

        en.put("button.back", "§c§lBack");
        en.put("button.confirm", "§e§lConfirm Changes");
        en.put("button.unlimited", "§aInf.");

        en.put("trades.level.current", " (Current)");
        en.put("trades.level.editor", " (Editor)");
        en.put("trades.button.level", "§aLevel: %s%s");
        en.put("trades.lore.level.1", "§7Click to select the trading");
        en.put("trades.lore.level.2", "§7level you want to edit.");
        en.put("trades.lore.level.3", "");

        en.put("trades.button.effects", "§bVillager Effects");

        en.put("trades.button.vanilla", "§aVanilla Leveling: %s");
        en.put("trades.lore.vanilla.1", "§7If §aON§7: Villager levels up");
        en.put("trades.lore.vanilla.2", "§7and unlocks new trades normally.");
        en.put("trades.lore.vanilla.3", "§7If §cOFF§7: Level and trades");
        en.put("trades.lore.vanilla.4", "§7are locked.");
        en.put("trades.lore.vanilla.5", "");

        en.put("trades.button.keep", "§aKeep Trades: %s");
        en.put("trades.lore.keep.1", "§7If §aON§7: Trades from previous");
        en.put("trades.lore.keep.2", "§7levels are kept when leveling up.");
        en.put("trades.lore.keep.3", "§7If §cOFF§7: Each level will");
        en.put("trades.lore.keep.4", "§7have only its own trades.");

        en.put("trades.page.prev", "§a<- Page %d");
        en.put("trades.page.next", "§aPage %d ->");
        en.put("trades.msg.saved", "§aTrades saved (Level: %s)");

        en.put("trades.button.files", "§eTrade Files");
        en.put("trades.lore.files", "§7Save or load trades from a file");
        en.put("trades.files.save", "§aSave All");
        en.put("trades.files.save.lore", "§7Save all trades to a file");
        en.put("trades.files.load", "§bLoad Trade");
        en.put("trades.files.load.lore", "§7Load trades from a file");
        en.put("trades.files.prompt.save", "§e[EditVillagers] §fType a §afile name§f in chat to save trades.");
        en.put("trades.files.prompt.load", "§e[EditVillagers] §fSelect a trade file and press §aSelect file§f.");
        en.put("trades.files.prompt.no_spaces", "§7Name must have §cno spaces§7 — letters, numbers and _ only");
        en.put("trades.files.button.cancel", "§c§l[ Cancel ]");
        en.put("trades.files.button.cancel.hover", "§7Click to cancel");
        en.put("trades.files.dialog.title", "Select trade file");
        en.put("trades.files.dialog.select", "Select file");
        en.put("trades.files.dialog.filter", "EditVillagers trade files (*.evtrades)");
        en.put("trades.files.msg.saved", "§a[EditVillagers] §fTrades saved to: §e%s.evtrades");
        en.put("trades.files.msg.loaded", "§a[EditVillagers] §fTrades loaded from: §e%s.evtrades");
        en.put("trades.files.msg.cancelled", "§e[EditVillagers] §7Save/load cancelled.");
        en.put("trades.files.msg.nothing_to_cancel", "§e[EditVillagers] §7Nothing to cancel.");
        en.put("trades.files.msg.invalid_name", "§c[EditVillagers] §fInvalid file name.");
        en.put("trades.files.msg.no_spaces", "§c[EditVillagers] §fFile name cannot contain spaces.");
        en.put("trades.files.msg.not_found", "§c[EditVillagers] §fFile not found in trades folder.");
        en.put("trades.files.msg.error", "§c[EditVillagers] §fCould not read or write trade file.");
        en.put("trades.files.msg.no_villager", "§c[EditVillagers] §fVillager not found.");

        en.put("sum.limit.infinite", "§aLimit: Infinite");
        en.put("sum.limit.value", "§eLimit: %d");
        en.put("sum.change", "§bChange Limit");
        en.put("sum.lore.lmb", "§7LMB: +1 | Shift+LMB: +10");
        en.put("sum.lore.rmb", "§7RMB: -1 | Shift+RMB: -10");
        en.put("sum.restock.on", "§aDaily Restock: ON");
        en.put("sum.restock.off", "§cDaily Restock: OFF");
        en.put("sum.lore.restock", "§7Restocks if a bed is nearby");
        en.put("sum.msg.saved", "§aTrade limits saved!");

        en.put("settings.status.on", "§aON");
        en.put("settings.status.off", "§cOFF");
        en.put("settings.status.yes", "§aYes");
        en.put("settings.status.no", "§cNo");
        en.put("settings.status.enabled", "§aEnabled");
        en.put("settings.status.disabled", "§cDisabled");

        en.put("settings.button.level", "§fVillager Level: %s");
        en.put("settings.lore.level", "§7Set villager level");

        en.put("settings.button.immortality", "§fImmortality: %s");
        en.put("settings.lore.immortality", "§7Toggle villager invulnerability");

        en.put("settings.button.nametag", "§fName: %s");
        en.put("settings.lore.nametag", "§7Show name tag above head");

        en.put("settings.button.lookat", "§fWatch Player: %s");
        en.put("settings.lore.lookat", "§7Villager will always face the player");

        en.put("settings.button.glowing", "§fGlowing: %s");
        en.put("settings.lore.glowing", "§7Toggle glowing effect");

        en.put("settings.button.noai", "§eNoAI: %s");
        en.put("settings.lore.noai", "§7Disable Villager AI");

        en.put("settings.button.silent", "§fSounds: %s");
        en.put("settings.lore.silent", "§7Disable all villager sounds");

        en.put("settings.button.align", "§fAlign: %s");
        en.put("settings.lore.align", "§7Center villager on the block");

        en.put("settings.button.rotate", "§fRotate: %s");
        en.put("settings.lore.rotate", "§7Change looking direction");

        en.put("settings.button.resetxp", "§fReset: %s");
        en.put("settings.lore.resetxp", "§7Reset level and XP");

        en.put("settings.button.pricelock", "§fDynamic Prices: %s");
        en.put("settings.lore.pricelock", "§7Toggle dynamic price changes (Lock)");

        en.put("settings.button.noxp", "§fXP Drop: %s");
        en.put("settings.lore.noxp", "§7Disable experience when trading with a villager");

        en.put("settings.button.baby", "§fSize: %s");
        en.put("settings.lore.baby", "§7Change villager size");
        en.put("settings.status.baby", "§eBaby");
        en.put("settings.status.adult", "§aAdult");

        en.put("settings.button.sum", "§eTrade Amount Settings");

        en.put("settings.nav.trades", "§aTrades");
        en.put("settings.nav.professions", "§bProfessions");
        en.put("settings.nav.biomes", "§eBiomes");

        en.put("settings.msg.level_default", "§a[EditVillagers] §fLevel: Default (Vanilla Leveling)");
        en.put("settings.msg.level_fixed", "§a[EditVillagers] §fLevel locked at: %s");
        en.put("settings.msg.xp_reset", "§a[EditVillagers] §fXP and Level reset to Novice");
        en.put("settings.msg.aligned", "§a[EditVillagers] §fVillager aligned to block center!");
        en.put("settings.msg.direction", "§a[EditVillagers] §fDirection: %s");

        en.put("level.novice", "§7Novice");
        en.put("level.apprentice", "§eApprentice");
        en.put("level.journeyman", "§6Journeyman");
        en.put("level.expert", "§bExpert");
        en.put("level.master", "§aMaster");
        en.put("level.default", "§dDefault");
        en.put("level.lvl", "§7Lvl %d");

        en.put("biomes.type.desert", "§eDesert");
        en.put("biomes.type.jungle", "§2Jungle");
        en.put("biomes.type.plains", "§aPlains");
        en.put("biomes.type.savanna", "§6Savanna");
        en.put("biomes.type.snow", "Snow");
        en.put("biomes.type.swamp", "§9Swamp");
        en.put("biomes.type.taiga", "§3Taiga");
        en.put("biomes.msg.changed", "§a[EditVillagers] §fVillager type changed");
        en.put("biomes.msg.error", "§cError: %s");

        en.put("profession.toolsmith", "§fToolsmith");
        en.put("profession.armorer", "§7Armorer");
        en.put("profession.farmer", "§aFarmer");
        en.put("profession.shepherd", "§bShepherd");
        en.put("profession.leatherworker", "§6Leatherworker");
        en.put("profession.fisherman", "§3Fisherman");
        en.put("profession.butcher", "§cButcher");
        en.put("profession.weaponsmith", "§fWeaponsmith");
        en.put("profession.fletcher", "§eFletcher");
        en.put("profession.cartographer", "§fCartographer");
        en.put("profession.librarian", "§dLibrarian");
        en.put("profession.cleric", "§5Cleric");
        en.put("profession.mason", "§8Mason");
        en.put("profession.nitwit", "§2Nitwit");
        en.put("profession.none", "§7Unemployed");

        en.put("profession.msg.selected", "§eSelected: §f%s");
        en.put("profession.msg.select_first", "§cSelect a profession first!");
        en.put("profession.msg.applied", "§aProfession applied, trades saved.");

        en.put("dir.south", "South");
        en.put("dir.southwest", "South-West");
        en.put("dir.west", "West");
        en.put("dir.northwest", "North-West");
        en.put("dir.north", "North");
        en.put("dir.northeast", "North-East");
        en.put("dir.east", "East");
        en.put("dir.southeast", "South-East");

        LANGUAGES.put("ru", ru);
        LANGUAGES.put("en", en);
    }
}
