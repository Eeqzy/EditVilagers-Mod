package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

public class TradesScreenHandler extends AbstractContainerMenu {

    private final Villager villager;
    private final SimpleContainer tradeInventory = new SimpleContainer(27);
    private final SimpleContainer buttonInventory = new SimpleContainer(27);
    private final Player player;
    private final ServerLevel world;

    private int currentPage = 0;
    private final MerchantOffer[] offersBuffer = new MerchantOffer[80];

    private int viewLevel;
    private boolean vanillaLevelingEnabled;
    private boolean keepTradesEnabled;

    private final String pendingName;

    public TradesScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x6, syncId);
        this.villager = villager;
        this.pendingName = null;
        this.player = playerInventory.player;
        LanguageManager.bind(this.player);
        this.world = null;

        EvVillagerLock lock = (EvVillagerLock) villager;
        this.vanillaLevelingEnabled = !lock.ev$areTradesLocked();
        this.keepTradesEnabled = lock.ev$shouldKeepTrades();
        this.viewLevel = villager != null
                ? (!this.vanillaLevelingEnabled ? 1 : ReflectionUtils.getLevel(villager.getVillagerData()))
                : 1;

        setupDecor();
        setupButtons();
        setupSlots(playerInventory);

        loadTradesFromVillager();
    }

    public TradesScreenHandler(int syncId, Inventory playerInventory, String name, Villager unused,
            ServerLevel world) {
        super(MenuType.GENERIC_9x6, syncId);
        this.villager = null;
        this.pendingName = name;
        this.player = playerInventory.player;
        LanguageManager.bind(this.player);
        this.world = world;

        setupDecor();
        setupButtons();
        setupSlots(playerInventory);
    }

    private void setupDecor() {
        tradeInventory.setItem(0, new ItemStack(McCompat.GREEN_STAINED_GLASS_PANE));
        tradeInventory.setItem(9, new ItemStack(McCompat.GREEN_STAINED_GLASS_PANE));
        tradeInventory.setItem(18, new ItemStack(McCompat.RED_STAINED_GLASS_PANE));
    }

    private void setupButtons() {
        for (int i = 0; i < 27; i++) {
            buttonInventory.setItem(i, new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE));
        }

        buttonInventory.setItem(0, createNavStack(Items.IRON_PICKAXE, LanguageManager.tr("menu.main.settings")));
        buttonInventory.setItem(9, createNavStack(Items.LECTERN, LanguageManager.tr("menu.main.professions")));
        buttonInventory.setItem(18, createNavStack(Items.SAND, LanguageManager.tr("menu.main.biomes")));

        refreshKeepTradesButton();
        refreshLevelSelectorButton();
        refreshVanillaLevelingButton();

        ItemStack save = new ItemStack(Items.GOLD_INGOT);
        save.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.confirm")));
        buttonInventory.setItem(22, save);

        ItemStack back = new ItemStack(McCompat.RED_CONCRETE);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.back")));
        buttonInventory.setItem(26, back);

        ItemStack tradeFiles = new ItemStack(Items.WRITABLE_BOOK);
        tradeFiles.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.button.files")));
        tradeFiles.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.lore.files")))));
        buttonInventory.setItem(24, tradeFiles);
    }

    private ItemStack createNavStack(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private void setupSlots(Inventory playerInventory) {
        for (int i = 0; i < 27; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;

            if (isEditableTradeSlot(i)) {
                addSlot(new Slot(tradeInventory, i, x, y));
            } else {
                addSlot(new LockedSlot(tradeInventory, i, x, y));
            }
        }

        for (int i = 0; i < 27; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 84 + (i / 9) * 18;
            addSlot(new LockedSlot(buttonInventory, i, x, y));
        }

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 138 + i * 18));

        for (int i = 0; i < 9; i++)
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 156));
    }

    private static boolean isEditableTradeSlot(int i) {
        if (i >= 1 && i <= 8)
            return true;
        if (i >= 10 && i <= 17)
            return true;
        if (i >= 19 && i <= 26)
            return true;
        return false;
    }

    private void loadTradesFromVillager() {
        if (villager == null)
            return;
        loadBufferFromStorage(viewLevel);
        loadPage(0);
    }

    private void loadBufferFromStorage(int level) {
        for (int i = 0; i < offersBuffer.length; i++)
            offersBuffer[i] = null;

        MerchantOffers source = null;
        EvVillagerLock lock = (EvVillagerLock) villager;
        source = lock.ev$getCustomLevelTrades(level);

        boolean hasAnyCustom = false;
        for (int i = 1; i <= 5; i++) {
            MerchantOffers t = lock.ev$getCustomLevelTrades(i);
            if (t != null && !t.isEmpty()) {
                hasAnyCustom = true;
                break;
            }
        }

        if ((source == null || source.isEmpty())
                && level == ReflectionUtils.getLevel(villager.getVillagerData())
                && !hasAnyCustom) {
            source = villager.getOffers();
        }

        if (source != null) {
            for (int i = 0; i < Math.min(source.size(), 80); i++) {
                MerchantOffer offer = source.get(i);
                try {
                    offersBuffer[i] = offer.copy();
                } catch (Throwable t) {
                    offersBuffer[i] = offer;
                }
            }
        }
    }

    private void commitBufferToStorage(int level) {
        if (villager == null)
            return;
        EvVillagerLock lock = (EvVillagerLock) villager;

        MerchantOffers offers = new MerchantOffers();
        for (MerchantOffer o : offersBuffer) {
            if (o != null)
                offers.add(o);
        }

        MerchantOffers liveSnapshot = new MerchantOffers();
        for (MerchantOffer offer : villager.getOffers()) {
            try {
                liveSnapshot.add(offer.copy());
            } catch (Throwable t) {
                liveSnapshot.add(offer);
            }
        }

        lock.ev$setCustomLevelTrades(level, offers);

        if (!this.vanillaLevelingEnabled || level == ReflectionUtils.getLevel(villager.getVillagerData())) {
            if (keepTradesEnabled) {
                lock.ev$rebuildOffersFromMenu(level);
                lock.ev$mergeOfferMetadataFrom(liveSnapshot);
                // Only sync metadata back when the live list is the combined keep-trades list.
                lock.ev$syncCustomLevelTradesFromFlat(villager.getOffers());
            } else {
                MerchantOffers toApply = new MerchantOffers();
                for (MerchantOffer offer : offers) {
                    toApply.add(offer);
                }
                for (int i = 0; i < toApply.size() && i < liveSnapshot.size(); i++) {
                    MerchantOffer base = toApply.get(i);
                    MerchantOffer meta = liveSnapshot.get(i);
                    MerchantOffer merged = new MerchantOffer(
                            base.getItemCostA(),
                            base.getItemCostB(),
                            base.getResult(),
                            meta.getUses(),
                            meta.getMaxUses(),
                            base.getXp(),
                            base.getPriceMultiplier(),
                            base.getDemand());
                    if (merged instanceof EvTradeOfferExtension ext
                            && meta instanceof EvTradeOfferExtension metaExt) {
                        ext.ev$setDailyRestock(metaExt.ev$isDailyRestock());
                    }
                    toApply.set(i, merged);
                }
                lock.ev$forceSetOffers(toApply);
                // Do NOT sync flat→levels here: live offers are only this editor level,
                // and syncing would wipe other stored levels when the list is shorter/empty.
            }

            lock.ev$setTradesLocked(!vanillaLevelingEnabled);
        }
    }

    private void refreshLevelSelectorButton() {
        ItemStack stack = new ItemStack(Items.EMERALD);
        String lvlName = getLevelName(viewLevel);
        String status = (viewLevel == ReflectionUtils.getLevel(villager.getVillagerData()))
                ? LanguageManager.tr("trades.level.current")
                : LanguageManager.tr("trades.level.editor");
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(LanguageManager.tr("trades.button.level", lvlName, status)));

        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.lore.level.1")),
                Component.literal(LanguageManager.tr("trades.lore.level.2")),
                Component.literal(LanguageManager.tr("trades.lore.level.3")))));

        buttonInventory.setItem(4, stack);
    }

    private void refreshVanillaLevelingButton() {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        String status = vanillaLevelingEnabled ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.button.vanilla", status)));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, vanillaLevelingEnabled);

        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.lore.vanilla.1")),
                Component.literal(LanguageManager.tr("trades.lore.vanilla.2")),
                Component.literal(LanguageManager.tr("trades.lore.vanilla.3")),
                Component.literal(LanguageManager.tr("trades.lore.vanilla.4")),
                Component.literal(LanguageManager.tr("trades.lore.vanilla.5")))));

        buttonInventory.setItem(5, stack);
    }

    private void refreshKeepTradesButton() {
        ItemStack stack = new ItemStack(Items.BOOK);
        String status = keepTradesEnabled ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.button.keep", status)));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, keepTradesEnabled);

        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.lore.keep.1")),
                Component.literal(LanguageManager.tr("trades.lore.keep.2")),
                Component.literal(LanguageManager.tr("trades.lore.keep.3")),
                Component.literal(LanguageManager.tr("trades.lore.keep.4")))));

        buttonInventory.setItem(3, stack);
    }

    private String getLevelName(int level) {
        return switch (level) {
            case 1 -> LanguageManager.tr("level.novice");
            case 2 -> LanguageManager.tr("level.apprentice");
            case 3 -> LanguageManager.tr("level.journeyman");
            case 4 -> LanguageManager.tr("level.expert");
            case 5 -> LanguageManager.tr("level.master");
            default -> LanguageManager.tr("level.lvl", level);
        };
    }

    private void loadPage(int page) {
        if (page < 0 || page > 9)
            return;
        this.currentPage = page;
        renderPage();
    }

    private void renderPage() {
        for (int i = 0; i < 27; i++) {
            if (isEditableTradeSlot(i)) {
                tradeInventory.setItem(i, ItemStack.EMPTY);
            }
        }

        int start = currentPage * 8;
        for (int i = 0; i < 8; i++) {
            if (start + i >= offersBuffer.length)
                break;

            MerchantOffer offer = offersBuffer[start + i];
            if (offer != null) {
                // В MC 26.1: getBaseCostA() или offer.getCostA() вместо getFirstBuyItem()
                ItemStack firstItem = offer.getCostA().copy();
                tradeInventory.setItem(1 + i, firstItem);

                // Второй предмет
                if (!offer.getCostB().isEmpty()) {
                    tradeInventory.setItem(10 + i, offer.getCostB().copy());
                }

                // Продаваемый предмет
                tradeInventory.setItem(19 + i, offer.getResult().copy());
            }
        }

        refreshArrows();
    }

    private void refreshArrows() {
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.page.prev", currentPage)));
            buttonInventory.setItem(7, prev);
        } else {
            ItemStack pane = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            buttonInventory.setItem(7, pane);
        }

        if (currentPage < 9) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME,
                    Component.literal(LanguageManager.tr("trades.page.next", currentPage + 2)));
            buttonInventory.setItem(8, next);
        } else {
            ItemStack pane = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            buttonInventory.setItem(8, pane);
        }
    }

    private void saveCurrentPageToBuffer() {
        int start = currentPage * 8;
        for (int i = 0; i < 8; i++) {
            int bufIdx = start + i;
            if (bufIdx >= offersBuffer.length)
                break;

            ItemStack first = tradeInventory.getItem(1 + i);
            ItemStack second = tradeInventory.getItem(10 + i);
            ItemStack sell = tradeInventory.getItem(19 + i);

            if (!first.isEmpty() && !sell.isEmpty()) {
                // В 26.1: конструктор MerchantOffer — используем ItemCost
                MerchantOffer offer = new MerchantOffer(
                        new ItemCost(first.getItem(), first.getCount()),
                        second.isEmpty()
                                ? Optional.empty()
                                : Optional.of(new ItemCost(second.getItem(), second.getCount())),
                        sell.copy(),
                        0,
                        Integer.MAX_VALUE,
                        5,
                        0.05f,
                        0);
                offersBuffer[bufIdx] = offer;
            } else {
                offersBuffer[bufIdx] = null;
            }
        }
    }

    private void saveTrades() {
        saveCurrentPageToBuffer();
        commitBufferToStorage(viewLevel);

        if (villager != null) {
            ((ServerPlayer) player).sendSystemMessage(
                    Component.literal(LanguageManager.tr("trades.msg.saved", getLevelName(viewLevel))),
                    true);
            return;
        }

        MerchantOffers offers = new MerchantOffers();
        for (MerchantOffer o : offersBuffer) {
            if (o != null)
                offers.add(o);
        }

        ServerLevel world = this.world;
        BlockPos pos = player.blockPosition();

        Villager v = McCompat.VILLAGER.spawn(world, pos, EntitySpawnReason.COMMAND);
        if (v != null) {
            if (pendingName != null) {
                v.setCustomName(Component.literal(pendingName));
                v.setCustomNameVisible(true);
            }

            EvVillagerLock lock = (EvVillagerLock) v;
            lock.ev$forceSetOffers(offers);
            lock.ev$setTradesLocked(true);

            ((ServerPlayer) player)
                    .sendSystemMessage(Component.literal(LanguageManager.tr("msg.trades_created_locked")), true);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player playerEntity) {
        LanguageManager.bind(playerEntity);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, playerEntity);
            return;
        }
        if (slotIndex >= 27 && slotIndex < 54) {
            int idx = slotIndex - 27;

            if (idx == 22) {
                saveTrades();
                return;
            }

            if (idx == 7) {
                if (currentPage > 0) {
                    saveCurrentPageToBuffer();
                    currentPage--;
                    renderPage();
                }
                return;
            }

            if (idx == 8) {
                if (currentPage < 9) {
                    saveCurrentPageToBuffer();
                    currentPage++;
                    renderPage();
                }
                return;
            }

            if (idx == 3) {
                keepTradesEnabled = !keepTradesEnabled;
                EvVillagerLock lock = (EvVillagerLock) villager;
                lock.ev$setKeepTrades(keepTradesEnabled);
                refreshKeepTradesButton();
                return;
            }

            if (idx == 4) {
                saveCurrentPageToBuffer();
                commitBufferToStorage(viewLevel);

                viewLevel++;
                if (viewLevel > 5)
                    viewLevel = 1;

                loadBufferFromStorage(viewLevel);
                refreshLevelSelectorButton();
                renderPage();
                return;
            }

            if (idx == 5) {
                vanillaLevelingEnabled = !vanillaLevelingEnabled;
                EvVillagerLock lock = (EvVillagerLock) villager;
                if (vanillaLevelingEnabled) {
                    lock.ev$syncCustomLevelTradesFromFlat(villager.getOffers());
                    viewLevel = ReflectionUtils.getLevel(villager.getVillagerData());
                    loadBufferFromStorage(viewLevel);
                    loadPage(currentPage);
                    refreshLevelSelectorButton();
                }
                lock.ev$setTradesLocked(!vanillaLevelingEnabled);
                refreshVanillaLevelingButton();
                return;
            }

            if (idx == 26) {
                if (playerEntity instanceof ServerPlayer sp) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager != null ? villager
                                    : new Villager(McCompat.VILLAGER, playerEntity.level())),
                            Component.literal(LanguageManager.tr("menu.main.title"))));
                }
                return;
            }

            if (idx == 0) {
                if (playerEntity instanceof ServerPlayer sp) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.settings.title"))));
                }
                return;
            }

            if (idx == 9) {
                if (playerEntity instanceof ServerPlayer sp) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.professions.title"))));
                }
                return;
            }

            if (idx == 18) {
                if (playerEntity instanceof ServerPlayer sp) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.biomes.title"))));
                }
                return;
            }

            if (idx == 24) {
                if (playerEntity instanceof ServerPlayer sp && villager != null) {
                    saveCurrentPageToBuffer();
                    commitBufferToStorage(viewLevel);
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new TradeFilesScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.trade_files.title"))));
                }
                return;
            }

            return;
        }

        super.clicked(slotIndex, button, actionType, playerEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerEntity) {
            return false;
        }
    }
}

