package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class EditScreenHandler extends AbstractContainerMenu {

    private static final int ADDON_ROW_FIRST = 9;
    private static final int ADDON_ROW_LAST = 17;
    /** Радужный пробег (~0.5 с), как раньше. */
    private static final int RAINBOW_TICKS = 10;
    /** Затухание чуть медленнее радуги. */
    private static final int FADE_TICKS = 8;
    private static final int TOTAL_ANIM_TICKS = RAINBOW_TICKS + FADE_TICKS;

    private final Villager villager;
    private final SimpleContainer menu;
    /** -1 = анимация закончена; иначе тик анимации доп. строки. */
    private int addonRowAnimTick = -1;

    public EditScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(EditMenuExtensions.getMenuType(), syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);
        this.menu = new SimpleContainer(EditMenuExtensions.getMenuSize());

        fillMenu();
        EditMenuExtensions.applyButtons(menu);

        int rows = 1 + EditMenuExtensions.getExtraRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                this.addSlot(new LockedSlot(menu, index, 8 + col * 18, 18 + row * 18));
            }
        }

        // Как у ванильного сундука: смещение инвентаря игрока от числа строк меню
        int invOffset = (rows - 4) * 18;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 103 + i * 18 + invOffset));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 161 + invOffset));
        }

        if (EditMenuExtensions.getExtraRows() > 0 && menu.getContainerSize() > ADDON_ROW_FIRST) {
            addonRowAnimTick = 0;
            paintAddonRowFrame(0);
            broadcastChanges();
        }
    }

    /** Вызывается каждый серверный тик, пока открыто главное меню. */
    public void tickAddonRowAnimation() {
        if (addonRowAnimTick < 0 || EditMenuExtensions.getExtraRows() <= 0) {
            return;
        }
        addonRowAnimTick++;
        if (addonRowAnimTick >= TOTAL_ANIM_TICKS) {
            settleAddonRow();
            addonRowAnimTick = -1;
            return;
        }
        paintAddonRowFrame(addonRowAnimTick);
    }

    private boolean isAddonButtonSlot(int slot) {
        return EditMenuExtensions.registeredSlots().contains(slot);
    }

    private ItemStack namedPane(Item paneItem) {
        Item safe = paneItem != null ? paneItem : McCompat.GRAY_STAINED_GLASS_PANE;
        ItemStack pane = new ItemStack(safe);
        pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return pane;
    }

    /**
     * Радуга крутится непрерывно; серая «шторка» едет поверх в ту же сторону.
     * Без белых/пустых слотов; кнопка аддона не трогается.
     */
    private void paintAddonRowFrame(int tick) {
        Item[] rainbow = McCompat.RAINBOW_STAINED_GLASS_PANES;
        int last = Math.min(ADDON_ROW_LAST, menu.getContainerSize() - 1);

        boolean changed = false;
        for (int slot = ADDON_ROW_FIRST; slot <= last; slot++) {
            if (isAddonButtonSlot(slot)) {
                continue;
            }
            int col = slot - ADDON_ROW_FIRST;
            // Радуга не останавливается — поток на всём протяжении анимации
            int flow = Math.floorMod(col - (tick / 2), rainbow.length);
            Item paneItem = rainbow[flow];

            // Серое затухание поверх радуги (волна в ту же сторону)
            if (tick >= RAINBOW_TICKS) {
                float fadeProgress = (tick - RAINBOW_TICKS) / (float) Math.max(1, FADE_TICKS - 1);
                float cover = fadeProgress - col * 0.08f;
                if (cover >= 0f) {
                    paneItem = McCompat.GRAY_STAINED_GLASS_PANE;
                }
            }

            ItemStack current = menu.getItem(slot);
            if (current.isEmpty() || !current.is(paneItem)) {
                menu.setItem(slot, namedPane(paneItem));
                changed = true;
            }
        }
        EditMenuExtensions.applyButtons(menu);
        if (changed) {
            broadcastChanges();
        }
    }

    private void settleAddonRow() {
        int last = Math.min(ADDON_ROW_LAST, menu.getContainerSize() - 1);
        for (int slot = ADDON_ROW_FIRST; slot <= last; slot++) {
            if (isAddonButtonSlot(slot)) {
                continue;
            }
            menu.setItem(slot, namedPane(McCompat.GRAY_STAINED_GLASS_PANE));
        }
        EditMenuExtensions.applyButtons(menu);
        broadcastChanges();
    }

    private void fillMenu() {
        for (int i = 0; i < menu.getContainerSize(); i++) {
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(i, glass);
        }

        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.settings")));
        pick.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.main.lore.settings")))));
        menu.setItem(1, pick);

        ItemStack emerald = new ItemStack(Items.EMERALD);
        emerald.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.trades")));
        emerald.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.main.lore.trades")))));
        menu.setItem(2, emerald);

        ItemStack sand = new ItemStack(Items.SAND);
        sand.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.biomes")));
        sand.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.main.lore.biomes")))));
        menu.setItem(4, sand);

        ItemStack lectern = new ItemStack(Items.LECTERN);
        lectern.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.professions")));
        lectern.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.main.lore.professions")))));
        menu.setItem(5, lectern);

        // Paper + item_model: выглядит как кузнечный шаблон, без ванильного «Применяется к…»
        ItemStack clone = new ItemStack(Items.PAPER);
        clone.set(DataComponents.ITEM_MODEL,
                Identifier.withDefaultNamespace("netherite_upgrade_smithing_template"));
        clone.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.clone")));
        clone.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.main.lore.clone")))));
        menu.setItem(7, clone);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < menu.getContainerSize()) {
            if (player instanceof ServerPlayer sp) {
                if (EditMenuExtensions.handleClick(slotIndex, sp, villager)) {
                    return;
                }

                if (slotIndex == 1) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.settings.title"))));

                } else if (slotIndex == 2) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.trades.title"))));

                } else if (slotIndex == 4) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.biomes.title"))));

                } else if (slotIndex == 5) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.professions.title"))));

                } else if (slotIndex == 7) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new CloneScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.clone.title"))));
                }
            }

            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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
