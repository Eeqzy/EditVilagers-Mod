package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CloneScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(9);

    public CloneScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X1, syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);

        fillMenu();

        for (int i = 0; i < 9; i++) {
            int x = 8 + i * 18;
            int y = 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 48 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 106));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < 9; i++) {
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(i, glass);
        }

        ItemStack cloneNearby = new ItemStack(Items.RED_BED);
        cloneNearby.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.clone.instant")));
        cloneNearby.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.clone.lore.instant")))));
        menu.setStack(2, cloneNearby);

        ItemStack spawnEgg = new ItemStack(Items.VILLAGER_SPAWN_EGG);
        spawnEgg.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.clone.egg")));
        spawnEgg.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.clone.lore.egg")))));
        menu.setStack(6, spawnEgg);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        LanguageManager.bind(player);
        if (actionType == SlotActionType.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (!(player instanceof ServerPlayerEntity sp))
            return;

        if (slotIndex >= 0 && slotIndex < 9) {
            if (slotIndex == 2) {
                cloneVillager(sp);
                sp.closeHandledScreen();
            }
            if (slotIndex == 6) {
                giveVillagerEgg(sp);
                sp.closeHandledScreen();
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

    private void cloneVillager(ServerPlayerEntity player) {
        try {
            VillagerCloneHelper.cloneNearby(player, villager);
            player.sendMessage(Text.literal(LanguageManager.tr("msg.cloned")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("msg.clone_fail", e.getMessage())), false);
        }
    }

    private void giveVillagerEgg(ServerPlayerEntity player) {
        try {
            ItemStack egg = VillagerCloneHelper.createCloneEgg(villager);
            if (!player.getInventory().insertStack(egg)) {
                player.dropItem(egg, false);
            }
            player.sendMessage(Text.literal(LanguageManager.tr("msg.egg_given")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("msg.error", e.getMessage())), false);
        }
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
