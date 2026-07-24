package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class CloneScreenHandler extends AbstractContainerMenu {

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(9);

    public CloneScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x1, syncId);
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
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(i, glass);
        }

        ItemStack bed = new ItemStack(McCompat.RED_BED);
        bed.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.clone.instant")));
        bed.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.clone.lore.instant")))));
        menu.setItem(2, bed);

        ItemStack spawnEgg = new ItemStack(Items.VILLAGER_SPAWN_EGG);
        spawnEgg.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("menu.clone.egg")));
        spawnEgg.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                java.util.List.of(Component.literal(LanguageManager.tr("menu.clone.lore.egg")))));
        menu.setItem(6, spawnEgg);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (!(player instanceof ServerPlayer sp))
            return;

        if (slotIndex >= 0 && slotIndex < 9) {
            if (slotIndex == 2) {
                cloneVillager(sp);
                sp.closeContainer();
            }
            if (slotIndex == 6) {
                giveVillagerEgg(sp);
                sp.closeContainer();
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

    private void cloneVillager(ServerPlayer player) {
        try {
            ServerLevel world = (ServerLevel) player.level();
            Villager newVillager = new Villager(McCompat.VILLAGER, world);

            newVillager.setVillagerData(villager.getVillagerData());
            newVillager.setVillagerXp(villager.getVillagerXp());
            newVillager.setBaby(villager.isBaby());
            ReflectionUtils.setAgeLocked(newVillager, villager.isBaby());

            MerchantOffers newOffers = new MerchantOffers();
            for (MerchantOffer offer : villager.getOffers()) {
                try {
                    newOffers.add(offer.copy());
                } catch(Throwable t) {
                    newOffers.add(offer);
                }
            }
            if (newVillager instanceof EvVillagerLock lock) {
                lock.ev$forceSetOffers(newOffers);
            }

            if (villager instanceof EvVillagerLock oldLock && newVillager instanceof EvVillagerLock newLock) {
                newLock.ev$setTradesLocked(oldLock.ev$areTradesLocked());
                newLock.ev$setProfessionLocked(oldLock.ev$isProfessionLocked());
                newLock.ev$setAlwaysLookAtPlayer(oldLock.ev$shouldAlwaysLookAtPlayer());
                newLock.ev$setPriceLock(oldLock.ev$isPriceLocked());
                newLock.ev$setKeepTrades(oldLock.ev$shouldKeepTrades());
                newLock.ev$setXpDropEnabled(oldLock.ev$isXpDropEnabled());
                newLock.ev$setContinuousEffect(oldLock.ev$hasContinuousEffect());
                newLock.ev$setContinuousParticle(oldLock.ev$getContinuousParticle());
                newLock.ev$setContinuousParticleCount(oldLock.ev$getContinuousParticleCount());
                for (int i = 1; i <= 5; i++) {
                    newLock.ev$setCustomLevelTrades(i, oldLock.ev$getCustomLevelTrades(i));
                }
            }

            newVillager.setNoAi(villager.isNoAi());
            newVillager.setSilent(villager.isSilent());
            newVillager.setInvulnerable(villager.isInvulnerable());
            newVillager.setGlowingTag(villager.isCurrentlyGlowing());
            newVillager.setNoGravity(villager.isNoGravity());
            newVillager.setPersistenceRequired();

            if (villager.hasCustomName()) {
                newVillager.setCustomName(villager.getCustomName());
                newVillager.setCustomNameVisible(villager.isCustomNameVisible());
            }

            newVillager.absSnapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
            newVillager.setYHeadRot(player.getYRot());
            newVillager.setYBodyRot(player.getYRot());
            world.addFreshEntity(newVillager);
            newVillager.setVillagerData(newVillager.getVillagerData());

            player.sendSystemMessage(Component.literal(LanguageManager.tr("msg.cloned")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendSystemMessage(Component.literal(LanguageManager.tr("msg.clone_fail", e.getMessage())), false);
        }
    }

    private void giveVillagerEgg(ServerPlayer player) {
        try {
            ItemStack egg = new ItemStack(Items.VILLAGER_SPAWN_EGG);

            CompoundTag nbt = ReflectionUtils.saveEntityWithoutId(villager);
            // Не тащим старый UUID/позицию — иначе спавн из яйца ломается
            nbt.remove("UUID");
            nbt.remove("UUIDLeast");
            nbt.remove("UUIDMost");
            nbt.remove("Pos");
            nbt.remove("Motion");
            nbt.remove("Rotation");
            nbt.remove("dimension");
            nbt.remove("WorldUUIDLeast");
            nbt.remove("WorldUUIDMost");

            egg.set(DataComponents.ENTITY_DATA, TypedEntityData.of(McCompat.VILLAGER, nbt));

            if (villager.hasCustomName()) {
                egg.set(DataComponents.CUSTOM_NAME, villager.getCustomName());
            }

            if (!player.getInventory().add(egg)) {
                player.drop(egg, false);
            }

            player.sendSystemMessage(Component.literal(LanguageManager.tr("msg.egg_given")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendSystemMessage(Component.literal(LanguageManager.tr("msg.error", e.getMessage())), false);
        }
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

