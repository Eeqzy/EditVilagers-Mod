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
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerData;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ProfessionScreenHandler extends ScreenHandler {

    private static final int SIZE = 45;
    private static final int SLOT_CONFIRM = 40;
    private static final int SLOT_BACK = 44;

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(SIZE);

    private Identifier pendingProfessionId = null;

    private static final Map<Integer, Identifier> SLOT_TO_PROF = new HashMap<>();
    static {
        SLOT_TO_PROF.put(2, Identifier.of("minecraft", "toolsmith"));
        SLOT_TO_PROF.put(21, Identifier.of("minecraft", "armorer"));
        SLOT_TO_PROF.put(4, Identifier.of("minecraft", "farmer"));
        SLOT_TO_PROF.put(23, Identifier.of("minecraft", "shepherd"));
        SLOT_TO_PROF.put(6, Identifier.of("minecraft", "leatherworker"));
        SLOT_TO_PROF.put(5, Identifier.of("minecraft", "fisherman"));
        SLOT_TO_PROF.put(20, Identifier.of("minecraft", "butcher"));
        SLOT_TO_PROF.put(14, Identifier.of("minecraft", "weaponsmith"));
        SLOT_TO_PROF.put(13, Identifier.of("minecraft", "fletcher"));
        SLOT_TO_PROF.put(22, Identifier.of("minecraft", "cartographer"));
        SLOT_TO_PROF.put(12, Identifier.of("minecraft", "librarian"));
        SLOT_TO_PROF.put(3, Identifier.of("minecraft", "cleric"));
        SLOT_TO_PROF.put(11, Identifier.of("minecraft", "mason"));
        SLOT_TO_PROF.put(24, Identifier.of("minecraft", "none"));
        SLOT_TO_PROF.put(15, Identifier.of("minecraft", "nitwit"));
    }

    public ProfessionScreenHandler(int syncId, PlayerInventory inv, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X5, syncId);
        this.villager = villager;
        fillMenu();
        for (int i = 0; i < SIZE; i++) {
            addSlot(new LockedSlot(menu, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(i, glass);
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
        putButton(SLOT_BACK, Items.RED_CONCRETE, LanguageManager.tr("button.back"));
    }

    private void putButton(int slot, net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));

        Identifier profId = SLOT_TO_PROF.get(slot);
        if (profId != null && pendingProfessionId != null && profId.equals(pendingProfessionId)) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        if (slot >= 0 && slot < menu.size())
            menu.setStack(slot, stack);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp) || slotIndex < 0 || slotIndex >= SIZE)
            return;

        if (slotIndex == SLOT_BACK) {
            sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                    Text.literal(LanguageManager.tr("menu.main.title"))));
            return;
        }

        if (slotIndex == 18) {
            sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                    Text.literal(LanguageManager.tr("menu.main.settings"))));
            return;
        }

        if (slotIndex == 27) {
            sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                    Text.literal(LanguageManager.tr("menu.trades.title"))));
            return;
        }

        if (slotIndex == 36) {
            sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                    Text.literal(LanguageManager.tr("menu.biomes.title"))));
            return;
        }

        if (SLOT_TO_PROF.containsKey(slotIndex)) {
            pendingProfessionId = SLOT_TO_PROF.get(slotIndex);
            fillMenu();
            sp.sendMessage(Text.literal(LanguageManager.tr("profession.msg.selected", pendingProfessionId.toString())),
                    true);
            return;
        }

        if (slotIndex == SLOT_CONFIRM) {
            if (pendingProfessionId == null) {
                sp.sendMessage(Text.literal(LanguageManager.tr("profession.msg.select_first")), true);
                return;
            }
            applyProfessionKeepTrades(pendingProfessionId);
            pendingProfessionId = null;
            fillMenu();
            sp.sendMessage(Text.literal(LanguageManager.tr("profession.msg.applied")), true);
            return;
        }
    }

    private void applyProfessionKeepTrades(Identifier id) {
        TradeOfferList savedOffers = deepCopyOffers(villager.getOffers());
        VillagerData old = villager.getVillagerData();
        int level = ReflectionUtils.getLevel(old);

        EvVillagerLock lock = (EvVillagerLock) villager;
        lock.ev$setProfessionLocked(false);

        try {
            RegistryEntry<VillagerProfession> profEntry = Registries.VILLAGER_PROFESSION.getEntry(id)
                    .orElseThrow(() -> new RuntimeException("Unknown profession: " + id));

            if (level < 1)
                level = 1;

            villager.setVillagerData(old.withProfession(profEntry).withLevel(level));
            lock.ev$setProfessionLocked(true);

            if (villager.getExperience() <= 0) {
                villager.setExperience(2);
            }
            villager.playSound(net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        lock.ev$forceSetOffers(savedOffers);
        lock.ev$setTradesLocked(true);
    }

    private static TradeOfferList deepCopyOffers(TradeOfferList offers) {
        TradeOfferList copy = new TradeOfferList();
        for (TradeOffer o : offers) {
            try {
                copy.add(o.copy());
            } catch (Throwable t) {
                copy.add(o);
            }
        }
        return copy;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
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
