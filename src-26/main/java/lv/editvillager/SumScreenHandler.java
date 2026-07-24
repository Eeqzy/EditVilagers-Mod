package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.Optional;

import java.util.List;

public class SumScreenHandler extends AbstractContainerMenu {

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(54);

    private int currentPage = 0;
    private final MerchantOffer[] offersBuffer = new MerchantOffer[80];

    public SumScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x6, syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);

        setupDecor();
        setupSlots(playerInventory);

        loadTradesFromVillager();
    }

    private void loadTradesFromVillager() {
        if (villager == null)
            return;

        MerchantOffers offers = villager.getOffers();
        for (int i = 0; i < Math.min(offers.size(), 80); i++) {
            MerchantOffer offer = offers.get(i);
            int maxUses = offer.getMaxUses();
            if (maxUses > 100 && maxUses != Integer.MAX_VALUE) {
                offer = new MerchantOffer(
                        offer.getItemCostA(),
                        offer.getItemCostB(),
                        offer.getResult(),
                        0,
                        100,
                        offer.getXp(),
                        offer.getPriceMultiplier(),
                        offer.getDemand());
                if (offers.get(i) instanceof EvTradeOfferExtension oldExt
                        && offer instanceof EvTradeOfferExtension newExt) {
                    newExt.ev$setDailyRestock(oldExt.ev$isDailyRestock());
                }
            }

            try {
                MerchantOffer copy = offer.copy();
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
            menu.setItem(i, ItemStack.EMPTY);
        }

        menu.setItem(0, new ItemStack(McCompat.LIME_STAINED_GLASS_PANE));
        menu.setItem(9, new ItemStack(McCompat.LIME_STAINED_GLASS_PANE));
        menu.setItem(0, new ItemStack(McCompat.GREEN_STAINED_GLASS_PANE));
        menu.setItem(9, new ItemStack(McCompat.GREEN_STAINED_GLASS_PANE));

        menu.setItem(18, new ItemStack(McCompat.RED_STAINED_GLASS_PANE));

        int[] grayPanes = { 27, 36, 45 };
        for (int idx : grayPanes) {
            ItemStack p = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            p.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(idx, p);
        }

        ItemStack save = new ItemStack(Items.GOLD_INGOT);
        save.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.confirm")));
        menu.setItem(31, save);

        ItemStack back = new ItemStack(McCompat.RED_CONCRETE);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.back")));
        menu.setItem(33, back);
    }

    private void renderPage() {
        for (int row = 0; row < 6; row++) {
            for (int col = 1; col < 9; col++) {
                int idx = row * 9 + col;
                if (idx == 31 || idx == 33)
                    continue;

                if (row == 3) {
                    ItemStack p = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
                    p.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                    menu.setItem(idx, p);
                } else if (row >= 4) {
                    ItemStack p = new ItemStack(McCompat.LIME_STAINED_GLASS_PANE);
                    p.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                    menu.setItem(idx, p);
                } else {
                    menu.setItem(idx, ItemStack.EMPTY);
                }
            }
        }

        refreshArrows();

        int start = currentPage * 8;
        for (int i = 0; i < 8; i++) {
            if (start + i >= offersBuffer.length || offersBuffer[start + i] == null) {
                continue;
            }

            MerchantOffer offer = offersBuffer[start + i];
            if (offer != null) {

                menu.setItem(1 + i, offer.getCostA().copy());

                ItemStack secondItem = offer.getCostB();
                if (!secondItem.isEmpty()) {
                    menu.setItem(10 + i, secondItem.copy());
                }

                menu.setItem(19 + i, offer.getResult().copy());

                int max = offer.getMaxUses();
                String limitText = (max == Integer.MAX_VALUE) ? LanguageManager.tr("sum.limit.infinite")
                        : LanguageManager.tr("sum.limit.value", max);
                ItemLore lore = new ItemLore(List.of(
                        Component.literal(limitText),
                        Component.literal(LanguageManager.tr("sum.lore.lmb")),
                        Component.literal(LanguageManager.tr("sum.lore.rmb"))));

                ItemStack paper = new ItemStack(Items.PAPER);
                paper.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("sum.change")));
                paper.set(DataComponents.LORE, lore);
                menu.setItem(37 + i, paper);

                ItemStack book = new ItemStack(Items.BOOK);
                boolean restock = false;
                if (offer instanceof EvTradeOfferExtension ext) {
                    restock = ext.ev$isDailyRestock();
                }

                if (restock) {
                    book.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("sum.restock.on")));
                    book.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                } else {
                    book.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("sum.restock.off")));
                }
                book.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal(LanguageManager.tr("sum.lore.restock")))));
                menu.setItem(46 + i, book);
            }
        }
    }

    private void refreshArrows() {
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.page.prev", currentPage)));
            menu.setItem(34, prev);
        } else {
            ItemStack pane = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(34, pane);
        }

        if (currentPage < 9) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME,
                    Component.literal(LanguageManager.tr("trades.page.next", currentPage + 2)));
            menu.setItem(35, next);
        } else {
            ItemStack pane = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(35, pane);
        }
    }

    private void setupSlots(Inventory playerInventory) {
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        int row = slotIndex / 9;
        int col = slotIndex % 9;
        boolean isSpecialSlot = (row == 4 && col >= 1 && col <= 8);
        if (actionType == ContainerInput.QUICK_MOVE && !isSpecialSlot) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < 54 && player instanceof ServerPlayer sp) {

            if (slotIndex == 31) {
                saveChanges(sp);
                return;
            }

            if (slotIndex == 33) {
                sp.openMenu(new SimpleMenuProvider(
                        (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                        Component.literal(LanguageManager.tr("menu.settings.title"))));
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

            if (col >= 1 && col <= 8) {
                int tradeOffset = col - 1;
                int start = currentPage * 8;
                int tradeIdx = start + tradeOffset;

                if (tradeIdx < offersBuffer.length && offersBuffer[tradeIdx] != null) {
                    MerchantOffer offer = offersBuffer[tradeIdx];
                    boolean isShift = (actionType == ContainerInput.QUICK_MOVE);

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
                            MerchantOffer newOffer = new MerchantOffer(
                                    offer.getItemCostA(),
                                    offer.getItemCostB(),
                                    offer.getResult(),
                                    0,
                                    newMax,
                                    offer.getXp(),
                                    offer.getPriceMultiplier(),
                                    offer.getDemand());
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
        ReflectionUtils.handleQuickMoveSync(actionType, player);
        super.clicked(slotIndex, button, actionType, player);
    }

    private void saveChanges(ServerPlayer player) {
        if (villager == null)
            return;

        MerchantOffers offers = new MerchantOffers();
        for (MerchantOffer o : offersBuffer) {
            if (o != null) {
                offers.add(o);
            }
        }

        EvVillagerLock lock = (EvVillagerLock) villager;
        lock.ev$forceSetOffers(offers);
        lock.ev$syncCustomLevelTradesFromFlat(offers);

        player.sendSystemMessage(Component.literal(LanguageManager.tr("sum.msg.saved")), true);
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleContainer inventory, int index, int x, int y) {
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

