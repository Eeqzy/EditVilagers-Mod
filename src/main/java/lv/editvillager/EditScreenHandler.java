package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EntityType;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.component.type.NbtComponent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public class EditScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(9);

    public EditScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X1, syncId);
        this.villager = villager;

        fillMenu();

        for (int i = 0; i < 9; i++) {
            int x = 8 + i * 18;
            int y = 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < 9; i++) {
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

        ItemStack clone = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        clone.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.main.clone")));
        clone.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.main.lore.clone")))));
        menu.setStack(7, clone);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < 9) {

            if (player instanceof ServerPlayerEntity sp) {

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
                    cloneVillager(sp);
                    sp.closeHandledScreen();
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

    private void cloneVillager(ServerPlayerEntity player) {
        try {
            ServerWorld world = (ServerWorld) player.getEntityWorld();

            VillagerEntity newVillager = new VillagerEntity(EntityType.VILLAGER, world);

            newVillager.setVillagerData(villager.getVillagerData());
            newVillager.setExperience(villager.getExperience());
            newVillager.setBaby(villager.isBaby());

            TradeOfferList newOffers = new TradeOfferList();
            for (TradeOffer offer : villager.getOffers()) {
                newOffers.add(offer.copy());
            }
            if (newVillager instanceof EvVillagerLock lock) {
                lock.ev$forceSetOffers(newOffers);
            } else {
                newVillager.setOffers(newOffers);
            }

            if (villager instanceof EvVillagerLock oldLock && newVillager instanceof EvVillagerLock newLock) {
                newLock.ev$setTradesLocked(oldLock.ev$areTradesLocked());
                newLock.ev$setProfessionLocked(oldLock.ev$isProfessionLocked());
                newLock.ev$setAlwaysLookAtPlayer(oldLock.ev$shouldAlwaysLookAtPlayer());
                newLock.ev$setPriceLock(oldLock.ev$isPriceLocked());
                newLock.ev$setKeepTrades(oldLock.ev$shouldKeepTrades());
                newLock.ev$setXpDropEnabled(oldLock.ev$isXpDropEnabled());
                for (int i = 1; i <= 5; i++) {
                    newLock.ev$setCustomLevelTrades(i, oldLock.ev$getCustomLevelTrades(i));
                }
            }

            newVillager.setAiDisabled(villager.isAiDisabled());
            newVillager.setSilent(villager.isSilent());
            newVillager.setInvulnerable(villager.isInvulnerable());
            newVillager.setGlowing(villager.isGlowing());
            newVillager.setNoGravity(villager.hasNoGravity());
            newVillager.setPersistent();

            if (villager.hasCustomName()) {
                newVillager.setCustomName(villager.getCustomName());
                newVillager.setCustomNameVisible(villager.isCustomNameVisible());
            }

            newVillager.refreshPositionAndAngles(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    0.0f);
            newVillager.setHeadYaw(player.getYaw());
            newVillager.setBodyYaw(player.getYaw());

            world.spawnEntity(newVillager);

            newVillager.setVillagerData(newVillager.getVillagerData());

            player.sendMessage(Text.literal(LanguageManager.tr("msg.cloned")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("msg.clone_fail", e.getMessage())), false);
        }
    }

    private void fixItemNbt(NbtCompound itemNbt) {
        if (itemNbt != null && itemNbt.contains("Count")) {
            net.minecraft.nbt.NbtElement element = itemNbt.get("Count");
            if (element != null) {
                itemNbt.remove("Count");
                itemNbt.put("count", element);
            }
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
