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

import java.util.List;

public class TradeFilesScreenHandler extends ScreenHandler {

    private static final int SLOT_SAVE = 11;
    private static final int SLOT_LOAD = 15;
    private static final int SLOT_BACK = 22;

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(27);

    public TradeFilesScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
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
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(i, glass);
        }

        ItemStack save = new ItemStack(Items.WRITABLE_BOOK);
        save.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.files.save")));
        save.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(LanguageManager.tr("trades.files.save.lore")))));
        menu.setStack(SLOT_SAVE, save);

        ItemStack load = new ItemStack(Items.ENCHANTED_BOOK);
        load.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.files.load")));
        load.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(LanguageManager.tr("trades.files.load.lore")))));
        menu.setStack(SLOT_LOAD, load);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        menu.setStack(SLOT_BACK, back);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        LanguageManager.bind(player);
        if (actionType == SlotActionType.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (!(player instanceof ServerPlayerEntity sp)) {
            return;
        }

        if (slotIndex >= 0 && slotIndex < 27) {
            if (slotIndex == SLOT_SAVE) {
                sp.closeHandledScreen();
                TradeFilePromptManager.beginSave(sp, villager);
                return;
            }
            if (slotIndex == SLOT_LOAD) {
                sp.closeHandledScreen();
                TradeFilePromptManager.beginLoad(sp, villager);
                return;
            }
            if (slotIndex == SLOT_BACK) {
                sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                        Text.literal(LanguageManager.tr("menu.trades.title"))));
                return;
            }
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
