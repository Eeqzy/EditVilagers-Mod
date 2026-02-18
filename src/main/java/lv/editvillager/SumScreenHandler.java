package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.village.TradedItem;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.List;

public class SumScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(54);

    private int currentPage = 0;
    private final TradeOffer[] offersBuffer = new TradeOffer[80];

    public SumScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.villager = villager;

        setupDecor();
        setupSlots(playerInventory);

        loadTradesFromVillager();
    }

    private void loadTradesFromVillager() {
        if (villager == null)
            return;

        TradeOfferList offers = villager.getOffers();
        for (int i = 0; i < Math.min(offers.size(), 80); i++) {
            TradeOffer offer = offers.get(i);
            int maxUses = offer.getMaxUses();
            if (maxUses > 100 && maxUses != Integer.MAX_VALUE) {
                offer = new TradeOffer(
                        offer.getFirstBuyItem(),
                        offer.getSecondBuyItem(),
                        offer.getSellItem(),
                        0,
                        100,
                        offer.getMerchantExperience(),
                        offer.getPriceMultiplier(),
                        offer.getDemandBonus());
                if (offers.get(i) instanceof EvTradeOfferExtension oldExt
                        && offer instanceof EvTradeOfferExtension newExt) {
                    newExt.ev$setDailyRestock(oldExt.ev$isDailyRestock());
                }
            }

            try {
                TradeOffer copy = offer.copy();
                if (offer instanceof EvTradeOfferExtension oldExt && copy instanceof EvTradeOfferExtension newExt) {
                    boolean oldVal = oldExt.ev$isDailyRestock();
                    newExt.ev$setDailyRestock(oldVal);
                }
                offersBuffer[i] = copy;
            } catch (Throwable t) {
                offersBuffer[i] = offer;
            }
        }
        renderPage();
    }

    private void setupDecor() {
        for (int i = 0; i < 54; i++) {
            menu.setStack(i, ItemStack.EMPTY);
        }

        menu.setStack(0, new ItemStack(Items.LIME_STAINED_GLASS_PANE));
        menu.setStack(9, new ItemStack(Items.LIME_STAINED_GLASS_PANE));
        menu.setStack(0, new ItemStack(Items.GREEN_STAINED_GLASS_PANE));
        menu.setStack(9, new ItemStack(Items.GREEN_STAINED_GLASS_PANE));

        menu.setStack(18, new ItemStack(Items.RED_STAINED_GLASS_PANE));

        int[] grayPanes = { 27, 36, 45 };
        for (int idx : grayPanes) {
            ItemStack p = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            p.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(idx, p);
        }

        ItemStack save = new ItemStack(Items.GOLD_INGOT);
        save.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.confirm")));
        menu.setStack(31, save);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        menu.setStack(33, back);
    }

    private void renderPage() {
        for (int row = 0; row < 6; row++) {
            for (int col = 1; col < 9; col++) {
                int idx = row * 9 + col;
                if (idx == 31 || idx == 33)
                    continue;

                if (row == 3) {
                    ItemStack p = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                    p.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
                    menu.setStack(idx, p);
                } else if (row >= 4) {
                    ItemStack p = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
                    p.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
                    menu.setStack(idx, p);
                } else {
                    menu.setStack(idx, ItemStack.EMPTY);
                }
            }
        }

        refreshArrows();

        int start = currentPage * 8;
        for (int i = 0; i < 8; i++) {
            if (start + i >= offersBuffer.length || offersBuffer[start + i] == null) {
                continue;
            }

            TradeOffer offer = offersBuffer[start + i];
            if (offer != null) {

                menu.setStack(1 + i, offer.getOriginalFirstBuyItem().copy());

                ItemStack secondItem = offer.getDisplayedSecondBuyItem();
                if (!secondItem.isEmpty()) {
                    menu.setStack(10 + i, secondItem.copy());
                }

                menu.setStack(19 + i, offer.getSellItem().copy());

                int max = offer.getMaxUses();
                String limitText = (max == Integer.MAX_VALUE) ? LanguageManager.tr("sum.limit.infinite")
                        : LanguageManager.tr("sum.limit.value", max);
                LoreComponent lore = new LoreComponent(List.of(
                        Text.literal(limitText),
                        Text.literal(LanguageManager.tr("sum.lore.lmb")),
                        Text.literal(LanguageManager.tr("sum.lore.rmb"))));

                ItemStack paper = new ItemStack(Items.PAPER);
                paper.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("sum.change")));
                paper.set(DataComponentTypes.LORE, lore);
                menu.setStack(37 + i, paper);

                ItemStack book = new ItemStack(Items.BOOK);
                boolean restock = false;
                if (offer instanceof EvTradeOfferExtension ext) {
                    restock = ext.ev$isDailyRestock();
                }

                if (restock) {
                    book.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("sum.restock.on")));
                    book.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
                } else {
                    book.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("sum.restock.off")));
                }
                book.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                        Text.literal(LanguageManager.tr("sum.lore.restock")))));
                menu.setStack(46 + i, book);
            }
        }
    }

    private void refreshArrows() {
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.page.prev", currentPage)));
            menu.setStack(34, prev);
        } else {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(34, pane);
        }

        if (currentPage < 9) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(LanguageManager.tr("trades.page.next", currentPage + 2)));
            menu.setStack(35, next);
        } else {
            ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(35, pane);
        }
    }

    private void setupSlots(PlayerInventory playerInventory) {
        for (int i = 0; i < 54; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 140 + i * 18));

        for (int i = 0; i < 9; i++)
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 54 && player instanceof ServerPlayerEntity sp) {

            if (slotIndex == 31) {
                saveChanges(sp);
                return;
            }

            if (slotIndex == 33) {
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                        Text.literal(LanguageManager.tr("menu.settings.title"))));
                return;
            }

            if (slotIndex == 34) {
                if (currentPage > 0) {
                    currentPage--;
                    renderPage();
                }
                return;
            }
            if (slotIndex == 35) {
                if (currentPage < 9) {
                    currentPage++;
                    renderPage();
                }
                return;
            }

            int row = slotIndex / 9;
            int col = slotIndex % 9;

            if (col >= 1 && col <= 8) {
                int tradeOffset = col - 1;
                int start = currentPage * 8;
                int tradeIdx = start + tradeOffset;

                if (tradeIdx < offersBuffer.length && offersBuffer[tradeIdx] != null) {
                    TradeOffer offer = offersBuffer[tradeIdx];
                    boolean isShift = (actionType == SlotActionType.QUICK_MOVE);

                    if (row == 4) {
                        int currentMax = offer.getMaxUses();
                        int newMax = currentMax;
                        boolean changed = false;

                        if (button == 0) {
                            if (currentMax == Integer.MAX_VALUE) {
                                newMax = 1;
                                changed = true;
                            } else {
                                int add = isShift ? 10 : 1;
                                newMax += add;
                                if (newMax > 100)
                                    newMax = 100;
                                changed = true;
                            }
                        } else if (button == 1) {
                            if (currentMax <= 1) {
                                newMax = Integer.MAX_VALUE;
                                changed = true;
                            } else if (currentMax == Integer.MAX_VALUE) {
                            } else {
                                int sub = isShift ? 10 : 1;
                                newMax -= sub;
                                if (newMax < 1)
                                    newMax = 1;
                                changed = true;
                            }
                        }

                        if (changed && newMax != currentMax) {
                            TradeOffer newOffer = new TradeOffer(
                                    offer.getFirstBuyItem(),
                                    offer.getSecondBuyItem(),
                                    offer.getSellItem(),
                                    0,
                                    newMax,
                                    offer.getMerchantExperience(),
                                    offer.getPriceMultiplier(),
                                    offer.getDemandBonus());
                            if (offer instanceof EvTradeOfferExtension oldExt
                                    && newOffer instanceof EvTradeOfferExtension newExt) {
                                newExt.ev$setDailyRestock(oldExt.ev$isDailyRestock());
                            }
                            offersBuffer[tradeIdx] = newOffer;
                            renderPage();
                        }
                    } else if (row == 5) {
                        if (offer instanceof EvTradeOfferExtension ext) {
                            ext.ev$setDailyRestock(!ext.ev$isDailyRestock());
                            renderPage();
                        }
                    }
                }
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    private void saveChanges(ServerPlayerEntity player) {
        if (villager == null)
            return;

        TradeOfferList offers = new TradeOfferList();
        for (TradeOffer o : offersBuffer) {
            if (o != null) {
                offers.add(o);
            }
        }

        EvVillagerLock lock = (EvVillagerLock) villager;
        lock.ev$forceSetOffers(offers);

        player.sendMessage(Text.literal(LanguageManager.tr("sum.msg.saved")), true);
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleInventory inventory, int index, int x, int y) {
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
