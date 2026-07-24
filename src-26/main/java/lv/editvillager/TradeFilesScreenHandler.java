package lv.editvillager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeFilesScreenHandler extends AbstractContainerMenu {

    private static final int SLOT_SAVE = 11;
    private static final int SLOT_LOAD = 15;
    private static final int SLOT_BACK = 22;

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(27);

    public TradeFilesScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x3, syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);
        fillMenu();

        for (int i = 0; i < 27; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < 27; i++) {
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(i, glass);
        }

        ItemStack save = new ItemStack(Items.WRITABLE_BOOK);
        save.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.files.save")));
        save.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.files.save.lore")))));
        menu.setItem(SLOT_SAVE, save);

        ItemStack load = new ItemStack(Items.ENCHANTED_BOOK);
        load.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("trades.files.load")));
        load.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.literal(LanguageManager.tr("trades.files.load.lore")))));
        menu.setItem(SLOT_LOAD, load);

        ItemStack back = new ItemStack(McCompat.RED_CONCRETE);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.back")));
        menu.setItem(SLOT_BACK, back);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        if (slotIndex >= 0 && slotIndex < 27) {
            if (slotIndex == SLOT_SAVE) {
                sp.closeContainer();
                TradeFilePromptManager.beginSave(sp, villager);
                return;
            }
            if (slotIndex == SLOT_LOAD) {
                sp.closeContainer();
                TradeFilePromptManager.beginLoad(sp, villager);
                return;
            }
            if (slotIndex == SLOT_BACK) {
                sp.openMenu(new SimpleMenuProvider(
                        (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                        Component.literal(LanguageManager.tr("menu.trades.title"))));
                return;
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
}
