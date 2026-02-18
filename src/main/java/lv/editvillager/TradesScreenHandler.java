package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import net.minecraft.village.TradedItem;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.Optional;

public class TradesScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory tradeInventory = new SimpleInventory(27);
    private final SimpleInventory buttonInventory = new SimpleInventory(27);
    private final PlayerEntity player;
    private final ServerWorld world;

    private int currentPage = 0;
    private final TradeOffer[] offersBuffer = new TradeOffer[80];

    private int viewLevel;
    private boolean vanillaLevelingEnabled;
    private boolean keepTradesEnabled;

    private final String pendingName;

    public TradesScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.villager = villager;
        this.pendingName = null;
        this.player = playerInventory.player;
        this.world = null;

        this.viewLevel = villager != null ? ReflectionUtils.getLevel(villager.getVillagerData()) : 1;

        EvVillagerLock lock = (EvVillagerLock) villager;
        this.vanillaLevelingEnabled = !lock.ev$areTradesLocked();
        this.keepTradesEnabled = lock.ev$shouldKeepTrades();

        setupDecor();
        setupButtons();
        setupSlots(playerInventory);

        loadTradesFromVillager();
    }

    public TradesScreenHandler(int syncId, PlayerInventory playerInventory, String name, VillagerEntity unused,
            ServerWorld world) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.villager = null;
        this.pendingName = name;
        this.player = playerInventory.player;
        this.world = world;

        setupDecor();
        setupButtons();
        setupSlots(playerInventory);
    }

    private void setupDecor() {
        tradeInventory.setStack(0, new ItemStack(Items.GREEN_STAINED_GLASS_PANE));
        tradeInventory.setStack(9, new ItemStack(Items.GREEN_STAINED_GLASS_PANE));
        tradeInventory.setStack(18, new ItemStack(Items.RED_STAINED_GLASS_PANE));
    }

    private void setupButtons() {
        for (int i = 0; i < 27; i++) {
            buttonInventory.setStack(i, new ItemStack(Items.GRAY_STAINED_GLASS_PANE));
        }

        buttonInventory.setStack(0, createNavStack(Items.IRON_PICKAXE, LanguageManager.tr("menu.main.settings")));
        buttonInventory.setStack(9, createNavStack(Items.LECTERN, LanguageManager.tr("menu.main.professions")));
        buttonInventory.setStack(18, createNavStack(Items.SAND, LanguageManager.tr("menu.main.biomes")));

        refreshKeepTradesButton();

        refreshLevelSelectorButton();

        refreshVanillaLevelingButton();

        ItemStack save = new ItemStack(Items.GOLD_INGOT);
        save.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.confirm")));
        buttonInventory.setStack(22, save);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        buttonInventory.setStack(26, back);
    }

    private ItemStack createNavStack(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return stack;
    }

    private void setupSlots(PlayerInventory playerInventory) {
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
        System.out.println("TradesScreenHandler.loadBufferFromStorage: Loading level " + level);
        for (int i = 0; i < offersBuffer.length; i++)
            offersBuffer[i] = null;

        TradeOfferList source = null;
        EvVillagerLock lock = (EvVillagerLock) villager;

        source = lock.ev$getCustomLevelTrades(level);
        System.out.println("  Custom trades for level " + level + ": " + (source != null ? source.size() : "null"));

        boolean hasAnyCustom = false;
        if (villager instanceof EvVillagerLock) {
            EvVillagerLock l = (EvVillagerLock) villager;
             for(int i=1; i<=5; i++) {
                 TradeOfferList t = l.ev$getCustomLevelTrades(i);
                 if(t != null && !t.isEmpty()) {
                     hasAnyCustom = true;
                     break;
                 }
             }
        }

        if ((source == null || source.isEmpty()) 
                && level == ReflectionUtils.getLevel(villager.getVillagerData())
                && !hasAnyCustom) {
            source = villager.getOffers();
            System.out.println(
                    "  Fallback to villager.getOffers() (init import): " + (source != null ? source.size() : "null"));
        }

        if (source != null) {
            for (int i = 0; i < Math.min(source.size(), 80); i++) {
                TradeOffer offer = source.get(i);
                System.out.println(
                        "  Source offer[" + i + "]: " + offer + ", hasSecond=" + offer.getSecondBuyItem().isPresent());
                try {
                    TradeOffer copied = offer.copy();
                    offersBuffer[i] = copied;
                    System.out.println("    -> Copied to buffer[" + i + "]: " + copied + ", hasSecond="
                            + copied.getSecondBuyItem().isPresent());
                } catch (Throwable t) {
                    offersBuffer[i] = offer;
                    System.out.println("    -> Copy failed, using original at buffer[" + i + "]: " + offer);
                }
            }
        }
    }

    private void commitBufferToStorage(int level) {
        if (villager == null)
            return;
        EvVillagerLock lock = (EvVillagerLock) villager;

        TradeOfferList offers = new TradeOfferList();
        for (int i = 0; i < offersBuffer.length; i++) {
            if (offersBuffer[i] != null) {
                offers.add(offersBuffer[i]);
            }
        }


        lock.ev$setCustomLevelTrades(level, offers);

        if (level == ReflectionUtils.getLevel(villager.getVillagerData())) {

            if (keepTradesEnabled) {
                TradeOfferList combined = new TradeOfferList();
                for (int l = 1; l <= level; l++) {
                    TradeOfferList tierUpdates = lock.ev$getCustomLevelTrades(l);
                    for (TradeOffer o : tierUpdates) {
                        combined.add(o.copy());
                    }


                }
                lock.ev$forceSetOffers(combined);


            } else {
                lock.ev$forceSetOffers(offers);


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
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("trades.button.level", lvlName, status)));

        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal(LanguageManager.tr("trades.lore.level.1")),
                Text.literal(LanguageManager.tr("trades.lore.level.2")),
                Text.literal(LanguageManager.tr("trades.lore.level.3")))));

        buttonInventory.setStack(4, stack);
    }

    private void refreshVanillaLevelingButton() {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        String status = vanillaLevelingEnabled ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.button.vanilla", status)));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, vanillaLevelingEnabled);

        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal(LanguageManager.tr("trades.lore.vanilla.1")),
                Text.literal(LanguageManager.tr("trades.lore.vanilla.2")),
                Text.literal(LanguageManager.tr("trades.lore.vanilla.3")),
                Text.literal(LanguageManager.tr("trades.lore.vanilla.4")),
                Text.literal(LanguageManager.tr("trades.lore.vanilla.5")))));

        buttonInventory.setStack(5, stack);
    }

    private void refreshKeepTradesButton() {
        ItemStack stack = new ItemStack(Items.BOOK);
        String status = keepTradesEnabled ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.button.keep", status)));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, keepTradesEnabled);

        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
                Text.literal(LanguageManager.tr("trades.lore.keep.1")),
                Text.literal(LanguageManager.tr("trades.lore.keep.2")),
                Text.literal(LanguageManager.tr("trades.lore.keep.3")),
                Text.literal(LanguageManager.tr("trades.lore.keep.4")))));

        buttonInventory.setStack(3, stack);
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
        System.out.println("TradesScreenHandler.renderPage: Rendering page " + currentPage);
        for (int i = 0; i < 27; i++) {
            if (isEditableTradeSlot(i)) {
                tradeInventory.setStack(i, ItemStack.EMPTY);
            }
        }

        int start = currentPage * 8;
        for (int i = 0; i < 8; i++) {
            if (start + i >= offersBuffer.length)
                break;

            TradeOffer offer = offersBuffer[start + i];
            if (offer != null) {
                System.out.println("  Rendering slot " + i + " from buffer[" + (start + i) + "]: " + offer
                        + ", hasSecond=" + offer.getSecondBuyItem().isPresent());

                ItemStack firstItem = offer.getOriginalFirstBuyItem().copy();
                tradeInventory.setStack(1 + i, firstItem);
                System.out.println("    First item (slot " + (1 + i) + "): " + firstItem);

                ItemStack secondItem = offer.getDisplayedSecondBuyItem();
                if (!secondItem.isEmpty()) {
                    tradeInventory.setStack(10 + i, secondItem.copy());
                    System.out.println("    Second item (slot " + (10 + i) + "): " + secondItem);
                }

                tradeInventory.setStack(19 + i, offer.getSellItem().copy());
                System.out.println("    Sell item (slot " + (19 + i) + "): " + offer.getSellItem());
            }
        }

        refreshArrows();
    }

    private void refreshArrows() {
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.page.prev", currentPage)));
            buttonInventory.setStack(7, prev);
        } else {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            buttonInventory.setStack(7, pane);
        }

        if (currentPage < 9) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(LanguageManager.tr("trades.page.next", currentPage + 2)));
            buttonInventory.setStack(8, next);
        } else {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            buttonInventory.setStack(8, pane);
        }

    }

    private void saveCurrentPageToBuffer() {
        int start = currentPage * 8;
        System.out.println("TradesScreenHandler.saveCurrentPageToBuffer: Saving page " + currentPage + " (slots "
                + start + " to " + (start + 7) + ")");
        for (int i = 0; i < 8; i++) {
            int bufIdx = start + i;
            if (bufIdx >= offersBuffer.length)
                break;

            ItemStack first = tradeInventory.getStack(1 + i);
            ItemStack second = tradeInventory.getStack(10 + i);
            ItemStack sell = tradeInventory.getStack(19 + i);

            System.out.println("  Slot " + i + ": first=" + first + ", second=" + second + ", sell=" + sell);

            if (!first.isEmpty() && !sell.isEmpty()) {
                TradeOffer offer = new TradeOffer(
                        new TradedItem(first.getItem(), first.getCount()),
                        second.isEmpty()
                                ? Optional.empty()
                                : Optional.of(new TradedItem(second.getItem(), second.getCount())),
                        sell.copy(),
                        0,
                        Integer.MAX_VALUE,
                        5,
                        0.05f,
                        0);
                offersBuffer[bufIdx] = offer;
                System.out.println("  -> Created offer at buffer[" + bufIdx + "]: " + offer + ", hasSecond="
                        + offer.getSecondBuyItem().isPresent());
            } else {
                offersBuffer[bufIdx] = null;
                System.out.println("  -> Cleared buffer[" + bufIdx + "]");
            }
        }
    }

    private void saveTrades() {
        saveCurrentPageToBuffer();
        commitBufferToStorage(viewLevel);

        if (villager != null) {
            ((ServerPlayerEntity) player).sendMessage(
                    Text.literal(LanguageManager.tr("trades.msg.saved", getLevelName(viewLevel))),
                    true);
            return;
        }

        TradeOfferList offers = new TradeOfferList();
        for (int i = 0; i < offersBuffer.length; i++) {
            if (offersBuffer[i] != null) {
                offers.add(offersBuffer[i]);
            }
        }

        ServerWorld world = this.world;
        BlockPos pos = player.getBlockPos();

        VillagerEntity v = EntityType.VILLAGER.spawn(world, pos, SpawnReason.COMMAND);
        if (v != null) {
            if (pendingName != null) {
                v.setCustomName(Text.literal(pendingName));
                v.setCustomNameVisible(true);
            }

            EvVillagerLock lock = (EvVillagerLock) v;
            lock.ev$forceSetOffers(offers);
            lock.ev$setTradesLocked(true);

            ((ServerPlayerEntity) player)
                    .sendMessage(Text.literal("§aЖитель создан (торги бесконечные + зафиксированы)"), true);
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity playerEntity) {
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
                lock.ev$setTradesLocked(!vanillaLevelingEnabled);
                refreshVanillaLevelingButton();
                return;
            }

            if (idx == 26) {
                if (playerEntity instanceof ServerPlayerEntity sp) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager != null ? villager
                                    : new VillagerEntity(EntityType.VILLAGER, playerEntity.getEntityWorld())),
                            Text.literal(LanguageManager.tr("menu.main.title"))));
                }
                return;
            }

            if (idx == 0) {
                if (playerEntity instanceof ServerPlayerEntity sp) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.settings.title"))));
                }
                return;
            }

            if (idx == 9) {
                if (playerEntity instanceof ServerPlayerEntity sp) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.professions.title"))));
                }
                return;
            }

            if (idx == 18) {
                if (playerEntity instanceof ServerPlayerEntity sp) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.biomes.title"))));
                }
                return;
            }

            return;
        }

        super.onSlotClick(slotIndex, button, actionType, playerEntity);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        final int TRADE_START = 0;
        final int TRADE_END = 27;

        final int BUTTON_START = 27;
        final int BUTTON_END = 54;

        final int PLAYER_START = 54;
        final int PLAYER_END = this.slots.size();

        if (slotIndex >= BUTTON_START && slotIndex < BUTTON_END) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= TRADE_START && slotIndex < TRADE_END) {
            if (!this.insertItem(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveToEditableTradeSlots(stack)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }

    private boolean moveToEditableTradeSlots(ItemStack stack) {
        for (int i = 0; i < 27 && !stack.isEmpty(); i++) {
            if (!isEditableTradeSlot(i))
                continue;

            Slot s = this.slots.get(i);
            if (!s.canInsert(stack))
                continue;

            ItemStack cur = s.getStack();
            if (cur.isEmpty())
                continue;
            if (!ItemStack.areItemsAndComponentsEqual(cur, stack))
                continue;

            int max = Math.min(cur.getMaxCount(), s.getMaxItemCount(stack));
            int canMove = max - cur.getCount();
            if (canMove <= 0)
                continue;

            int moved = Math.min(canMove, stack.getCount());
            cur.increment(moved);
            stack.decrement(moved);
            s.markDirty();
        }

        for (int i = 0; i < 27 && !stack.isEmpty(); i++) {
            if (!isEditableTradeSlot(i))
                continue;

            Slot s = this.slots.get(i);
            if (!s.canInsert(stack))
                continue;

            if (!s.getStack().isEmpty())
                continue;

            int moved = Math.min(stack.getCount(), s.getMaxItemCount(stack));
            s.setStack(stack.split(moved));
            s.markDirty();
        }

        return stack.isEmpty();
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}
