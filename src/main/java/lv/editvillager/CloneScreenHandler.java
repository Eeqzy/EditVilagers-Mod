package lv.editvillager;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CloneScreenHandler extends ScreenHandler {

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(9);

    public CloneScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
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

        ItemStack cloneTemplate = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        cloneTemplate.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.clone.instant")));
        cloneTemplate.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.clone.lore.instant")))));
        menu.setStack(2, cloneTemplate);

        ItemStack spawnEgg = new ItemStack(Items.VILLAGER_SPAWN_EGG);
        spawnEgg.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("menu.clone.egg")));
        spawnEgg.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("menu.clone.lore.egg")))));
        menu.setStack(6, spawnEgg);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
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

            if (villager.hasCustomName()) {
                newVillager.setCustomName(villager.getCustomName());
                newVillager.setCustomNameVisible(villager.isCustomNameVisible());
            }

            newVillager.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
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

    @SuppressWarnings("unchecked")
    private void giveVillagerEgg(ServerPlayerEntity player) {
        try {
            ItemStack egg = new ItemStack(Items.VILLAGER_SPAWN_EGG);

            NbtCompound nbt = new NbtCompound();
            nbt.putString("id", "minecraft:villager");

            NbtCompound fullNbt = new NbtCompound();
            ReflectionUtils.saveAllData(villager, fullNbt, player.getRegistryManager());

            if (fullNbt.contains("VillagerData"))
                nbt.put("VillagerData", fullNbt.get("VillagerData"));
            if (fullNbt.contains("Health"))
                nbt.put("Health", fullNbt.get("Health"));
            if (fullNbt.contains("AbsorptionAmount"))
                nbt.put("AbsorptionAmount", fullNbt.get("AbsorptionAmount"));
            if (fullNbt.contains("Age"))
                nbt.put("Age", fullNbt.get("Age"));

            if (fullNbt.contains("Attributes"))
                nbt.put("Attributes", fullNbt.get("Attributes"));
            if (fullNbt.contains("attributes"))
                nbt.put("attributes", fullNbt.get("attributes"));

            if (fullNbt.contains("Offers")) {
                net.minecraft.nbt.NbtElement offersEl = fullNbt.get("Offers");
                if (offersEl instanceof NbtCompound) {
                    NbtCompound offers = ((NbtCompound) offersEl).copy();
                    if (offers.contains("Recipes")) {
                        net.minecraft.nbt.NbtElement recipesEl = offers.get("Recipes");
                        if (recipesEl instanceof NbtList) {
                            NbtList recipes = (NbtList) recipesEl;
                            for (int i = 0; i < recipes.size(); i++) {
                                java.util.Optional<NbtCompound> optRecipe = recipes.getCompound(i);
                                if (optRecipe.isPresent()) {
                                    NbtCompound recipe = optRecipe.get();
                                    fixItemNbt(recipe.getCompound("buy").orElse(null));
                                    fixItemNbt(recipe.getCompound("buyB").orElse(null));
                                    fixItemNbt(recipe.getCompound("sell").orElse(null));
                                }
                            }
                        }
                    }
                    nbt.put("Offers", offers);
                }
            }

            String[] flags = {
                    "NoAI", "Silent", "Invulnerable", "Glowing", "PersistenceRequired",
                    "CanPickUpLoot", "CustomNameVisible", "NoGravity", "OnGround",
                    "LeftHanded", "FallFlying"
            };
            for (String flag : flags) {
                if (fullNbt.contains(flag))
                    nbt.put(flag, fullNbt.get(flag));
            }

            if (fullNbt.contains("CustomName"))
                nbt.put("CustomName", fullNbt.get("CustomName"));

            boolean applied = false;
            try {
                Class<?> typedClass = Class.forName("net.minecraft.component.type.TypedEntityData");
                Method createMethod = null;
                for (Method m : typedClass.getMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) &&
                            m.getParameterCount() == 2 &&
                            m.getParameterTypes()[0] == EntityType.class &&
                            m.getParameterTypes()[1] == NbtCompound.class) {
                        createMethod = m;
                        break;
                    }
                }

                if (createMethod != null) {
                    Object typedData = createMethod.invoke(null, EntityType.VILLAGER, nbt);
                    ((ItemStack) (Object) egg)
                            .set((net.minecraft.component.ComponentType) DataComponentTypes.ENTITY_DATA, typedData);
                    applied = true;
                }
            } catch (Exception e) {
            }

            if (!applied) {
                egg.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            }

            if (villager.hasCustomName()) {
                egg.set(DataComponentTypes.CUSTOM_NAME, villager.getCustomName());
            }

            if (!player.getInventory().insertStack(egg)) {
                player.dropItem(egg, false);
            }

            player.sendMessage(Text.literal(LanguageManager.tr("msg.egg_given")), true);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.literal(LanguageManager.tr("msg.error", e.getMessage())), false);
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
