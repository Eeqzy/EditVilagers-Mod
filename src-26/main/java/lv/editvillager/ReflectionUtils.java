package lv.editvillager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.lang.reflect.Method;

/**
 * ReflectionUtils — утилиты для работы с внутренними данными.
 *
 * В MC 26.1 код не обфусцирован, используем Mojang mappings.
 * Большинство reflection-хаков заменены прямыми вызовами.
 */
public class ReflectionUtils {

    /**
     * Получить уровень жителя из VillagerData.
     * VillagerData — record, accessor называется level().
     */
    public static int getLevel(VillagerData data) {
        if (data == null) return 1;
        return data.level();
    }

    /**
     * Получить профессию жителя из VillagerData (возвращает RegistryEntry).
     */
    public static Object getProfession(VillagerData data) {
        if (data == null) return VillagerProfession.NONE;
        return data.profession();
    }

    /**
     * Получить тип жителя из VillagerData.
     */
    public static Object getType(VillagerData data) {
        if (data == null) return VillagerType.PLAINS;
        return data.type();
    }

    /**
     * Проверить, является ли мир клиентским.
     */
    public static boolean isClient(Level world) {
        if (world == null) return false;
        return world.isClientSide();
    }

    /**
     * Конвертировать ItemCost (TradedItem) в ItemStack.
     * В 26.1 класс называется ItemCost (Mojang mappings).
     */
    public static ItemStack convertTradedItem(ItemCost tradedItem) {
        if (tradedItem == null) return ItemStack.EMPTY;
        return new ItemStack(tradedItem.item(), tradedItem.count());
    }

    /**
     * Распаковать значение из RegistryEntry (для профессий, типов и т.д.).
     */
    public static Object getEntryValue(Object entry) {
        if (entry == null) return null;
        if (entry instanceof net.minecraft.core.Holder<?> holder) {
            return holder.value();
        }
        return entry;
    }

    /**
     * Заблокировать возраст (малыш не вырастет). setAgeLocked protected в AgeableMob.
     */
    public static void setAgeLocked(net.minecraft.world.entity.AgeableMob mob, boolean locked) {
        if (mob == null) {
            return;
        }
        try {
            Method m = net.minecraft.world.entity.AgeableMob.class.getDeclaredMethod("setAgeLocked", boolean.class);
            m.setAccessible(true);
            m.invoke(mob, locked);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Установить флаг rewardingPlayerExperience у TradeOffer.
     * В 26.1 — используем прямой метод если он есть, иначе reflection.
     */
    public static void setRewardExp(net.minecraft.world.item.trading.MerchantOffer offer, boolean reward) {
        if (offer == null) return;
        try {
            // Пытаемся найти поле rewardingPlayerExperience через reflection
            java.lang.reflect.Field[] fields = net.minecraft.world.item.trading.MerchantOffer.class.getDeclaredFields();
            for (java.lang.reflect.Field f : fields) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    // Проверяем по имени (в 26.1 код не обфусцирован)
                    String name = f.getName();
                    if (name.contains("reward") || name.contains("experience") || name.contains("xp") || name.contains("Experience")) {
                        f.setBoolean(offer, reward);
                        return;
                    }
                }
            }
            // Fallback: устанавливаем первое boolean поле
            for (java.lang.reflect.Field f : fields) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    f.setBoolean(offer, reward);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Обработать QUICK_MOVE — синхронизировать инвентарь.
     */
    public static void handleQuickMoveSync(net.minecraft.world.inventory.ContainerInput actionType,
                                           net.minecraft.world.entity.player.Player player) {
        if (actionType == net.minecraft.world.inventory.ContainerInput.QUICK_MOVE) {
            forceSyncScreen(player);
        }
    }

    /**
     * Принудительная синхронизация экрана с клиентом.
     * Отправляет пакеты для обновления всех слотов.
     */
    public static void forceSyncScreen(net.minecraft.world.entity.player.Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.containerMenu != null) {
            try {
                int currentRev = sp.containerMenu.incrementStateId();
                sp.containerMenu.broadcastFullState();

                // Отправляем обновление всех слотов
                var menu = sp.containerMenu;
                for (int i = 0; i < menu.slots.size(); i++) {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                            menu.containerId,
                            currentRev,
                            i,
                            menu.slots.get(i).getItem().copy()
                    ));
                }
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                        -1,
                        currentRev,
                        -1,
                        menu.getCarried().copy()
                ));

                int newRev = menu.incrementStateId();

                // Отправляем полный снимок инвентаря
                java.util.List<ItemStack> contents = new java.util.ArrayList<>();
                for (int i = 0; i < menu.slots.size(); i++) {
                    contents.add(menu.slots.get(i).getItem().copy());
                }

                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket(
                        menu.containerId,
                        newRev,
                        contents,
                        menu.getCarried().copy()
                ));

                // Ещё раз слоты с новым ревизионом
                for (int i = 0; i < menu.slots.size(); i++) {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                            menu.containerId,
                            newRev,
                            i,
                            menu.slots.get(i).getItem().copy()
                    ));
                }
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                        -1,
                        newRev,
                        -1,
                        menu.getCarried().copy()
                ));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Сохранить сущность без id через ValueOutput (MC 26.x).
     * Старый addAdditionalSaveData(CompoundTag) больше не существует.
     */
    public static CompoundTag saveEntityWithoutId(Entity entity) {
        if (entity == null) {
            return new CompoundTag();
        }
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    /** @deprecated используй {@link #saveEntityWithoutId(Entity)} — в 26.x NBT пишется через ValueOutput. */
    @Deprecated
    public static void saveAllData(Entity entity, CompoundTag nbt,
                                   net.minecraft.core.RegistryAccess registries) {
        if (entity == null || nbt == null) {
            return;
        }
        CompoundTag saved = saveEntityWithoutId(entity);
        for (String key : saved.keySet()) {
            Tag tag = saved.get(key);
            if (tag != null) {
                nbt.put(key, tag.copy());
            }
        }
    }

    /**
     * Загрузить данные сущности из NBT через ValueInput (MC 26.x).
     */
    public static void loadAllData(Entity entity, CompoundTag nbt,
                                   net.minecraft.core.RegistryAccess registries) {
        if (entity == null || nbt == null) {
            return;
        }
        try {
            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), nbt);
            entity.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String[] names,
                                                       Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (String name : names) {
                try {
                    java.lang.reflect.Method m = current.getDeclaredMethod(name, paramTypes);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /** 26.1+: day/night clock ticks (replaces 1.21.x {@code World#getTimeOfDay}). */
    public static long getWorldDayTime(Level level) {
        return level.getDefaultClockTime();
    }

    public static boolean isBedNearby(Entity entity) {
        Level world = entity.level();
        net.minecraft.core.BlockPos feet = entity.blockPosition();
        net.minecraft.core.BlockPos center = net.minecraft.core.BlockPos.containing(
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2.0,
                entity.getZ());

        net.minecraft.core.BlockPos[] origins = {feet, feet.below(), center};
        for (net.minecraft.core.BlockPos origin : origins) {
            if (scanBedCube(world, origin)) {
                return true;
            }
        }
        return false;
    }

    private static boolean scanBedCube(Level world, net.minecraft.core.BlockPos origin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    net.minecraft.world.level.block.state.BlockState state =
                            world.getBlockState(origin.offset(x, y, z));
                    if (state.is(net.minecraft.tags.BlockTags.BEDS)
                            || state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
