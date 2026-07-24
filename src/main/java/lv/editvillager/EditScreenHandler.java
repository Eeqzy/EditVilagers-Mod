package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EditScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory menu;

    public EditScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(EditMenuExtensions.getMenuType(), syncId);
        this.villager = villager;
        this.menu = new SimpleInventory(EditMenuExtensions.getMenuSize());

        fillMenu();
        EditMenuExtensions.applyButtons(menu);

        int rows = 1 + EditMenuExtensions.getExtraRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                this.addSlot(new LockedSlot(menu, index, 8 + col * 18, 18 + row * 18));
            }
        }

        int invOffset = (rows - 4) * 18;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 103 + i * 18 + invOffset));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 161 + invOffset));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < menu.size(); i++) {
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(i, glass);
        }

        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.settings")));
        pick.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.settings")))));
        menu.setStack(1, pick);

        ItemStack emerald = new ItemStack(Items.EMERALD);
        emerald.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.trades")));
        emerald.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.trades")))));
        menu.setStack(2, emerald);

        ItemStack sand = new ItemStack(Items.SAND);
        sand.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.biomes")));
        sand.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.biomes")))));
        menu.setStack(4, sand);

        ItemStack lectern = new ItemStack(Items.LECTERN);
        lectern.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.professions")));
        lectern.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.professions")))));
        menu.setStack(5, lectern);

        //? if 1.21.1 {
        ItemStack clone = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        //?} else {
        // Paper + item_model: looks like smithing template without vanilla "Applies to…" lore
        ItemStack clone = new ItemStack(Items.PAPER);
        clone.set(DataComponentTypes.ITEM_MODEL,
                net.minecraft.util.Identifier.ofVanilla("netherite_upgrade_smithing_template"));
        //?}
        clone.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.clone")));
        clone.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.clone")))));
        menu.setStack(7, clone);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        LanguageManager.bind(player);
        if (actionType == SlotActionType.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < menu.size()) {

            if (player instanceof ServerPlayerEntity sp) {
                if (EditMenuExtensions.handleClick(slotIndex, sp, villager)) {
                    return;
                }

                if (slotIndex == 1) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.settings.title"))));

                } else if (slotIndex == 2) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.trades.title"))));

                } else if (slotIndex == 4) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.biomes.title"))));

                } else if (slotIndex == 5) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.professions.title"))));

                } else if (slotIndex == 7) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new CloneScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.clone.title"))));
                }
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
