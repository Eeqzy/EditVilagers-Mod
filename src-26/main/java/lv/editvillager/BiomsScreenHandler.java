package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.npc.villager.VillagerData;

import java.util.Optional;

public class BiomsScreenHandler extends AbstractContainerMenu {

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

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(SIZE);

    private Identifier pendingTypeId;
    private boolean isAuto = false;

    public BiomsScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x3, syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);

        Object typeObj = ReflectionUtils.getType(villager.getVillagerData());
        this.pendingTypeId = resolveToId(typeObj);
        if (this.pendingTypeId == null) {
            this.pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "plains");
        }

        fillMenu();

        for (int i = 0; i < SIZE; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private Identifier resolveToId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Holder<?> re) {
            return re.unwrapKey().map(k -> k.identifier()).orElse(null);
        }
        if (obj instanceof ResourceKey<?> key) {
            return key.identifier();
        }
        try {
            return BuiltInRegistries.VILLAGER_TYPE.getKey((VillagerType) obj);
        } catch (Exception e) {
        }
        return null;
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            menu.setItem(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < SIZE; i++) {
            if (i == 13 || i == 14 || i == 15)
                continue;
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            if (menu.getItem(i).isEmpty()) {
                safeSet(i, glass);
            }
        }

        ItemStack navMain = new ItemStack(Items.IRON_PICKAXE);
        navMain.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.main.settings")));
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
        trades.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("settings.nav.trades")));
        safeSet(SLOT_NAV_TRADES, trades);

        safeSet(SLOT_TYPE_SWAMP, createTypeStack(Items.MUD, LanguageManager.tr("biomes.type.swamp"), "swamp"));
        safeSet(SLOT_TYPE_TAIGA, createTypeStack(Items.PODZOL, LanguageManager.tr("biomes.type.taiga"), "taiga"));

        ItemStack prof = new ItemStack(Items.LECTERN);
        prof.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("settings.nav.professions")));
        safeSet(SLOT_NAV_PROFESSION, prof);

        ItemStack apply = new ItemStack(Items.GOLD_INGOT);
        apply.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.confirm")));
        safeSet(SLOT_APPLY, apply);

        ItemStack back = new ItemStack(McCompat.RED_CONCRETE);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.back")));
        safeSet(SLOT_BACK, back);
    }

    private ItemStack createTypeStack(net.minecraft.world.item.Item item, String name, String idPath) {
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", idPath);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§f§l" + name));
        if (!isAuto && pendingTypeId != null && pendingTypeId.equals(id)) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    private void safeSet(int slot, ItemStack stack) {
        if (slot >= 0 && slot < menu.getContainerSize()) {
            menu.setItem(slot, stack);
        }
    }

    private void refreshVisuals() {
        fillMenu();
        broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (player instanceof ServerPlayer sp) {
                if (slotIndex == SLOT_NAV_MAIN) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.main.settings"))));
                    return;
                }

                if (slotIndex == SLOT_TYPE_DESERT) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "desert");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_JUNGLE) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "jungle");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_PLAINS) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "plains");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SAVANNA) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "savanna");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SNOW) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "snow");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_SWAMP) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "swamp");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }
                if (slotIndex == SLOT_TYPE_TAIGA) {
                    pendingTypeId = Identifier.fromNamespaceAndPath("minecraft", "taiga");
                    isAuto = false;
                    refreshVisuals();
                    return;
                }

                if (slotIndex == SLOT_APPLY) {
                    if (!isAuto && pendingTypeId != null) {
                        try {
                            ResourceKey<VillagerType> key = ResourceKey.create(Registries.VILLAGER_TYPE, pendingTypeId);
                            Holder<VillagerType> typeEntry = BuiltInRegistries.VILLAGER_TYPE.get(key)
                                    .orElseThrow(() -> new RuntimeException("Unknown type: " + pendingTypeId));
                            VillagerData old = villager.getVillagerData();
                            villager.setVillagerData(new VillagerData(typeEntry, old.profession(), old.level()));
                            sp.sendSystemMessage(Component.literal(LanguageManager.tr("biomes.msg.changed")), true);
                        } catch (Exception e) {
                            sp.sendSystemMessage(Component.literal(LanguageManager.tr("biomes.msg.error", e.getMessage())), true);
                        }
                    }
                    return;
                }

                if (slotIndex == SLOT_NAV_TRADES) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.trades.title"))));
                    return;
                }
                if (slotIndex == SLOT_NAV_PROFESSION) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.professions.title"))));
                    return;
                }
                if (slotIndex == SLOT_BACK) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.main.title"))));
                    return;
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

