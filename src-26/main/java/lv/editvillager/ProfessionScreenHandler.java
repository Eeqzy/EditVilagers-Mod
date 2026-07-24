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
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ProfessionScreenHandler extends AbstractContainerMenu {

    private static final int SIZE = 45;
    private static final int SLOT_CONFIRM = 40;
    private static final int SLOT_BACK = 44;

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(SIZE);

    private Identifier pendingProfessionId = null;

    private static final Map<Integer, Identifier> SLOT_TO_PROF = new HashMap<>();
    static {
        SLOT_TO_PROF.put(2, Identifier.fromNamespaceAndPath("minecraft", "toolsmith"));
        SLOT_TO_PROF.put(21, Identifier.fromNamespaceAndPath("minecraft", "armorer"));
        SLOT_TO_PROF.put(4, Identifier.fromNamespaceAndPath("minecraft", "farmer"));
        SLOT_TO_PROF.put(23, Identifier.fromNamespaceAndPath("minecraft", "shepherd"));
        SLOT_TO_PROF.put(6, Identifier.fromNamespaceAndPath("minecraft", "leatherworker"));
        SLOT_TO_PROF.put(5, Identifier.fromNamespaceAndPath("minecraft", "fisherman"));
        SLOT_TO_PROF.put(20, Identifier.fromNamespaceAndPath("minecraft", "butcher"));
        SLOT_TO_PROF.put(14, Identifier.fromNamespaceAndPath("minecraft", "weaponsmith"));
        SLOT_TO_PROF.put(13, Identifier.fromNamespaceAndPath("minecraft", "fletcher"));
        SLOT_TO_PROF.put(22, Identifier.fromNamespaceAndPath("minecraft", "cartographer"));
        SLOT_TO_PROF.put(12, Identifier.fromNamespaceAndPath("minecraft", "librarian"));
        SLOT_TO_PROF.put(3, Identifier.fromNamespaceAndPath("minecraft", "cleric"));
        SLOT_TO_PROF.put(11, Identifier.fromNamespaceAndPath("minecraft", "mason"));
        SLOT_TO_PROF.put(24, Identifier.fromNamespaceAndPath("minecraft", "none"));
        SLOT_TO_PROF.put(15, Identifier.fromNamespaceAndPath("minecraft", "nitwit"));
    }

    public ProfessionScreenHandler(int syncId, Inventory inv, Villager villager) {
        super(MenuType.GENERIC_9x5, syncId);
        this.villager = villager;
        LanguageManager.bind(inv.player);
        fillMenu();
        for (int i = 0; i < SIZE; i++) {
            addSlot(new LockedSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, i * 9 + j + 9, 8 + j * 18, 120 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 178));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(i, glass);
        }

        putButton(2, Items.SMITHING_TABLE, LanguageManager.tr("profession.toolsmith"));
        putButton(21, Items.BLAST_FURNACE, LanguageManager.tr("profession.armorer"));
        putButton(4, Items.COMPOSTER, LanguageManager.tr("profession.farmer"));
        putButton(23, Items.LOOM, LanguageManager.tr("profession.shepherd"));
        putButton(6, Items.CAULDRON, LanguageManager.tr("profession.leatherworker"));
        putButton(5, Items.BARREL, LanguageManager.tr("profession.fisherman"));
        putButton(20, Items.SMOKER, LanguageManager.tr("profession.butcher"));
        putButton(14, Items.GRINDSTONE, LanguageManager.tr("profession.weaponsmith"));
        putButton(13, Items.FLETCHING_TABLE, LanguageManager.tr("profession.fletcher"));
        putButton(22, Items.CARTOGRAPHY_TABLE, LanguageManager.tr("profession.cartographer"));
        putButton(12, Items.LECTERN, LanguageManager.tr("profession.librarian"));
        putButton(3, Items.BREWING_STAND, LanguageManager.tr("profession.cleric"));
        putButton(11, Items.STONECUTTER, LanguageManager.tr("profession.mason"));
        putButton(15, Items.STRUCTURE_VOID, LanguageManager.tr("profession.nitwit"));
        putButton(24, Items.BARRIER, LanguageManager.tr("profession.none"));

        putButton(18, Items.IRON_PICKAXE, LanguageManager.tr("menu.main.settings"));
        putButton(27, Items.EMERALD, LanguageManager.tr("settings.nav.trades"));
        putButton(36, Items.SAND, LanguageManager.tr("settings.nav.biomes"));
        putButton(SLOT_CONFIRM, Items.GOLD_INGOT, LanguageManager.tr("button.confirm"));
        putButton(SLOT_BACK, McCompat.RED_CONCRETE, LanguageManager.tr("button.back"));
    }

    private void putButton(int slot, net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));

        Identifier profId = SLOT_TO_PROF.get(slot);
        if (profId != null && pendingProfessionId != null && profId.equals(pendingProfessionId)) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        if (slot >= 0 && slot < menu.getContainerSize())
            menu.setItem(slot, stack);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (!(player instanceof ServerPlayer sp) || slotIndex < 0 || slotIndex >= SIZE)
            return;

        if (slotIndex == SLOT_BACK) {
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                    Component.literal(LanguageManager.tr("menu.main.title"))));
            return;
        }

        if (slotIndex == 18) {
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                    Component.literal(LanguageManager.tr("menu.main.settings"))));
            return;
        }

        if (slotIndex == 27) {
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                    Component.literal(LanguageManager.tr("menu.trades.title"))));
            return;
        }

        if (slotIndex == 36) {
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                    Component.literal(LanguageManager.tr("menu.biomes.title"))));
            return;
        }

        if (SLOT_TO_PROF.containsKey(slotIndex)) {
            pendingProfessionId = SLOT_TO_PROF.get(slotIndex);
            fillMenu();
            sp.sendSystemMessage(Component.literal(LanguageManager.tr("profession.msg.selected", pendingProfessionId.toString())), true);
            return;
        }

        if (slotIndex == SLOT_CONFIRM) {
            if (pendingProfessionId == null) {
                sp.sendSystemMessage(Component.literal(LanguageManager.tr("profession.msg.select_first")), true);
                return;
            }
            applyProfessionKeepTrades(pendingProfessionId);
            pendingProfessionId = null;
            fillMenu();
            sp.sendSystemMessage(Component.literal(LanguageManager.tr("profession.msg.applied")), true);
            return;
        }
    }

    private void applyProfessionKeepTrades(Identifier id) {
        MerchantOffers savedOffers = deepCopyOffers(villager.getOffers());
        VillagerData old = villager.getVillagerData();
        int level = ReflectionUtils.getLevel(old);

        EvVillagerLock lock = (EvVillagerLock) villager;
        lock.ev$setProfessionLocked(false);

        try {
            ResourceKey<VillagerProfession> key = ResourceKey.create(Registries.VILLAGER_PROFESSION, id);
            Holder<VillagerProfession> profEntry = BuiltInRegistries.VILLAGER_PROFESSION.get(key)
                    .orElseThrow(() -> new RuntimeException("Unknown profession: " + id));

            if (level < 1)
                level = 1;

            villager.setVillagerData(new VillagerData(old.type(), profEntry, level));
            lock.ev$setProfessionLocked(true);

            if (villager.getVillagerXp() <= 0) {
                villager.setVillagerXp(2);
            }
            villager.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_TRADE, 1.0f, 1.0f);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        lock.ev$forceSetOffers(savedOffers);
        lock.ev$setTradesLocked(true);
    }

    private static MerchantOffers deepCopyOffers(MerchantOffers offers) {
        MerchantOffers copy = new MerchantOffers();
        for (MerchantOffer o : offers) {
            try {
                copy.add(o.copy());
            } catch (Throwable t) {
                copy.add(o);
            }
        }
        return copy;
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

