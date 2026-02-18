package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerType;

import java.util.Optional;

public class BiomsScreenHandler extends ScreenHandler {

    private static final int SIZE = 27;

    private static final int SLOT_NAV_MAIN = 0;
    private static final int SLOT_TYPE_DESERT = 2;
    private static final int SLOT_TYPE_JUNGLE = 3;
    private static final int SLOT_TYPE_PLAINS = 4;
    private static final int SLOT_TYPE_SAVANNA = 5;
    private static final int SLOT_TYPE_SNOW = 6;

    private static final int SLOT_NAV_TRADES = 9;
    private static final int SLOT_TYPE_SWAMP = 11;
    private static final int SLOT_TYPE_TAIGA = 12;

    private static final int SLOT_NAV_PROFESSION = 18;
    private static final int SLOT_APPLY = 22;
    private static final int SLOT_BACK = 26;

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(SIZE);

    private Identifier pendingTypeId;
    private boolean isAuto = false;

    public BiomsScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.villager = villager;

        Object typeObj = ReflectionUtils.getType(villager.getVillagerData());
        this.pendingTypeId = resolveToId(typeObj);
        if (this.pendingTypeId == null) {
            this.pendingTypeId = Identifier.of("minecraft", "plains");
        }

        fillMenu();

        for (int i = 0; i < SIZE; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }
    }

    private Identifier resolveToId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof RegistryEntry<?> re) {
            return re.getKey().map(k -> ((RegistryKey<?>) k).getValue()).orElse(null);
        }
        if (obj instanceof RegistryKey<?> key) {
            return key.getValue();
        }
        try {
            return net.minecraft.registry.Registries.VILLAGER_TYPE.getId((net.minecraft.village.VillagerType) obj);
        } catch (Exception e) {
        }
        return null;
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            menu.setStack(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < SIZE; i++) {
            if (i == 13 || i == 14 || i == 15)
                continue;
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            if (menu.getStack(i).isEmpty()) {
                safeSet(i, glass);
            }
        }

        ItemStack navMain = new ItemStack(Items.IRON_PICKAXE);
        navMain.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.settings")));
        safeSet(SLOT_NAV_MAIN, navMain);

        safeSet(SLOT_TYPE_DESERT, createTypeStack(Items.SAND, LanguageManager.tr("biomes.type.desert"), "desert"));
        safeSet(SLOT_TYPE_JUNGLE,
                createTypeStack(Items.JUNGLE_LEAVES, LanguageManager.tr("biomes.type.jungle"), "jungle"));
        safeSet(SLOT_TYPE_PLAINS,
                createTypeStack(Items.GRASS_BLOCK, LanguageManager.tr("biomes.type.plains"), "plains"));
        safeSet(SLOT_TYPE_SAVANNA,
                createTypeStack(Items.ACACIA_LOG, LanguageManager.tr("biomes.type.savanna"), "savanna"));
        safeSet(SLOT_TYPE_SNOW, createTypeStack(Items.SNOW_BLOCK, LanguageManager.tr("biomes.type.snow"), "snow"));

        ItemStack trades = new ItemStack(Items.EMERALD);
        trades.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.nav.trades")));
        safeSet(SLOT_NAV_TRADES, trades);

        safeSet(SLOT_TYPE_SWAMP, createTypeStack(Items.MUD, LanguageManager.tr("biomes.type.swamp"), "swamp"));
        safeSet(SLOT_TYPE_TAIGA, createTypeStack(Items.PODZOL, LanguageManager.tr("biomes.type.taiga"), "taiga"));

        ItemStack prof = new ItemStack(Items.LECTERN);
        prof.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.nav.professions")));
        safeSet(SLOT_NAV_PROFESSION, prof);

        ItemStack apply = new ItemStack(Items.GOLD_INGOT);
        apply.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.confirm")));
        safeSet(SLOT_APPLY, apply);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        safeSet(SLOT_BACK, back);
    }

    private ItemStack createTypeStack(net.minecraft.item.Item item, String name, String idPath) {
        Identifier id = Identifier.of("minecraft", idPath);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§f§l" + name));
        if (!isAuto && pendingTypeId != null && pendingTypeId.equals(id)) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private void safeSet(int slot, ItemStack stack) {
        if (slot >= 0 && slot < menu.size()) {
            menu.setStack(slot, stack);
        }
    }

    private void refreshVisuals() {
        fillMenu();
        sendContentUpdates();
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (player instanceof ServerPlayerEntity sp) {
                if (slotIndex == SLOT_NAV_MAIN) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.main.settings"))));
                    return;
                }

                if (slotIndex == SLOT_TYPE_DESERT) {
                    pendingTypeId = Identifier.of("minecraft", "desert");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_JUNGLE) {
                    pendingTypeId = Identifier.of("minecraft", "jungle");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_PLAINS) {
                    pendingTypeId = Identifier.of("minecraft", "plains");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SAVANNA) {
                    pendingTypeId = Identifier.of("minecraft", "savanna");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SNOW) {
                    pendingTypeId = Identifier.of("minecraft", "snow");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SWAMP) {
                    pendingTypeId = Identifier.of("minecraft", "swamp");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_TAIGA) {
                    pendingTypeId = Identifier.of("minecraft", "taiga");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }

                if (slotIndex == SLOT_APPLY) {
                    if (!isAuto && pendingTypeId != null) {
                        try {
                            RegistryEntry<VillagerType> typeEntry = Registries.VILLAGER_TYPE.getEntry(pendingTypeId)
                                    .orElseThrow(() -> new RuntimeException("Unknown type: " + pendingTypeId));
                            villager.setVillagerData(villager.getVillagerData().withType(typeEntry));
                            sp.sendMessage(Text.literal(LanguageManager.tr("biomes.msg.changed")), true);
                        } catch (Exception e) {
                            sp.sendMessage(Text.literal(LanguageManager.tr("biomes.msg.error", e.getMessage())), true);
                        }
                    }
                    return;
                }

                if (slotIndex == SLOT_NAV_TRADES) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.trades.title"))));
                    return;
                }
                if (slotIndex == SLOT_NAV_PROFESSION) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.professions.title"))));
                    return;
                }
                if (slotIndex == SLOT_BACK) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.main.title"))));
                    return;
                }
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
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
