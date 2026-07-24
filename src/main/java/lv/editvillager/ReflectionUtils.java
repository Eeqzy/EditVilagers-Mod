package lv.editvillager;

import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public static void setAgeLocked(net.minecraft.entity.passive.PassiveEntity entity, boolean locked) {
        if (entity == null) {
            return;
        }
        try {
            Method m = net.minecraft.entity.passive.PassiveEntity.class.getDeclaredMethod("setAgeLocked", boolean.class);
            m.setAccessible(true);
            m.invoke(entity, locked);
        } catch (Exception e) {
            try {
                Field f = net.minecraft.entity.passive.PassiveEntity.class.getDeclaredField("ageLocked");
                f.setAccessible(true);
                f.setBoolean(entity, locked);
            } catch (Exception ignored) {
            }
        }
    }

    private static Field levelField;
    private static Field professionField;
    private static Field typeField;
    private static Field isClientField;

    private static Field tradedItemItemField;
    private static Field tradedItemCountField;

    private static Method tradedItemItemMethod;
    private static Method tradedItemCountMethod;

    private static Method levelMethod;
    private static Method professionMethod;
    private static Method typeMethod;
    private static Method isClientMethod;

    static {
        String[] levelNames = {"getLevel", "level", "method_19194"};
        for (String name : levelNames) {
            try {
                levelMethod = VillagerData.class.getMethod(name);
                levelMethod.setAccessible(true);
                break;
            } catch (Exception ignored) {}
        }
        String[] profNames = {"getProfession", "profession", "method_19192"};
        for (String name : profNames) {
            try {
                professionMethod = VillagerData.class.getMethod(name);
                professionMethod.setAccessible(true);
                break;
            } catch (Exception ignored) {}
        }
        String[] typeNames = {"getType", "type", "method_19193"};
        for (String name : typeNames) {
            try {
                typeMethod = VillagerData.class.getMethod(name);
                typeMethod.setAccessible(true);
                break;
            } catch (Exception ignored) {}
        }

        if (levelMethod == null || professionMethod == null || typeMethod == null) {
            try {
                for (Field f : VillagerData.class.getDeclaredFields()) {
                     if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                     f.setAccessible(true);
                     
                     Class<?> type = f.getType();
                     if (type == int.class && levelField == null) {
                         levelField = f;
                     } else if (type == VillagerProfession.class && professionField == null) {
                         professionField = f;
                     } else if (type == VillagerType.class && typeField == null) {
                         typeField = f;
                     }
                }
                if (levelField == null) {
                    try {
                        levelField = VillagerData.class.getDeclaredField("field_18555");
                        levelField.setAccessible(true);
                    } catch(Exception e) {
                         System.err.println("ReflectionUtils: Failed to find level field via name");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try {
            isClientField = World.class.getDeclaredField("isClient");
            isClientField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                isClientField = World.class.getDeclaredField("field_9236");
                isClientField.setAccessible(true);
            } catch (Exception ex) {
                System.err.println("ReflectionUtils: Failed to find isClient field");
            }
        }
        String[] worldMethodNames = {"isClient", "isClientSide", "method_8530"};
        for (String name : worldMethodNames) {
            try {
                isClientMethod = World.class.getMethod(name);
                isClientMethod.setAccessible(true);
                break;
            } catch (Exception ignored) {}
        }


        try {
            Class<?> tradedItemClass = null;
            for (String cn : new String[] {
                    "net.minecraft.village.TradedItem",
                    "net.minecraft.village.ItemCost",
                    "net.minecraft.class_9306"
            }) {
                try {
                    tradedItemClass = Class.forName(cn);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (tradedItemClass == null) {
                // 1.21.11+ uses ItemCost; convertTradedItem has other fallbacks
            } else {
                Class<?> registryEntryClass = null;
                try {
                    registryEntryClass = Class.forName("net.minecraft.registry.entry.RegistryEntry");
                } catch (Exception ignored) {
                }
                if (registryEntryClass == null) {
                    try {
                        registryEntryClass = Class.forName("net.minecraft.class_6880");
                    } catch (Exception ignored) {
                    }
                }

                String[] itemMethodNames = {"item", "getItem", "method_58286"};
                for (String name : itemMethodNames) {
                    try {
                        tradedItemItemMethod = tradedItemClass.getMethod(name);
                        tradedItemItemMethod.setAccessible(true);
                        break;
                    } catch (Exception ignored) {
                    }
                }
                String[] countMethodNames = {"count", "getCount", "method_58284"};
                for (String name : countMethodNames) {
                    try {
                        tradedItemCountMethod = tradedItemClass.getMethod(name);
                        tradedItemCountMethod.setAccessible(true);
                        break;
                    } catch (Exception ignored) {
                    }
                }

                if (tradedItemItemMethod == null || tradedItemCountMethod == null) {
                    for (Field f : tradedItemClass.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                            continue;
                        f.setAccessible(true);

                        Class<?> type = f.getType();
                        String typeName = type.getSimpleName();
                        String fullName = type.getName();

                        boolean isItem = (type == net.minecraft.item.Item.class);
                        boolean isRegistryEntry = (registryEntryClass != null
                                && registryEntryClass.isAssignableFrom(type))
                                || typeName.contains("RegistryEntry")
                                || fullName.contains("RegistryEntry")
                                || fullName.contains("class_6880");
                        boolean isHolder = typeName.contains("Holder") || fullName.contains("Holder");

                        if ((isItem || isRegistryEntry || isHolder) && tradedItemItemField == null) {
                            tradedItemItemField = f;
                        } else if (type == int.class && tradedItemCountField == null) {
                            tradedItemCountField = f;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static net.minecraft.item.ItemStack convertTradedItem(Object tradedItem) {
        if (tradedItem == null) {
            return net.minecraft.item.ItemStack.EMPTY;
        }
        try {
            net.minecraft.item.Item item = net.minecraft.item.Items.AIR;
            int count = 1;

            Object rawItem = null;
            if (tradedItemItemMethod != null) {
                try {
                    rawItem = tradedItemItemMethod.invoke(tradedItem);
                } catch (Exception e) {
                }
            }
            if (rawItem == null && tradedItemItemField != null) {
                try {
                    rawItem = tradedItemItemField.get(tradedItem);
                } catch (Exception e) {
                }
            }

            if (rawItem instanceof net.minecraft.item.Item i) {
                item = i;
            } else if (rawItem != null) {
                Object unwrapped = getEntryValue(rawItem);
                if (unwrapped instanceof net.minecraft.item.Item i) {
                    item = i;
                } else {
                }
            }

            if (tradedItemCountField != null) {
                count = tradedItemCountField.getInt(tradedItem);
            } else if (tradedItemCountMethod != null) {
                Object c = tradedItemCountMethod.invoke(tradedItem);
                if (c instanceof Integer i) {
                    count = i;
                }
            }

            net.minecraft.item.ItemStack result = new net.minecraft.item.ItemStack(item, count);
            return result;
        } catch (Exception e) {
            System.err.println("ReflectionUtils.convertTradedItem: Exception occurred");
            e.printStackTrace();
            return net.minecraft.item.ItemStack.EMPTY;
        }
    }

    private static Method cachedSaveMethod = null;

    /** Save entity NBT without id (1.21.10+ WriteView / legacy writeNbt). */
    public static NbtCompound saveEntityWithoutId(Entity entity) {
        NbtCompound nbt = new NbtCompound();
        if (entity == null) {
            return nbt;
        }
        RegistryWrapper.WrapperLookup lookup = entity.getRegistryManager();
        //? if 1.21.10 || 1.21.11 {
        try {
            net.minecraft.storage.NbtWriteView view = net.minecraft.storage.NbtWriteView.create(
                    net.minecraft.util.ErrorReporter.EMPTY, lookup);
            boolean saved = false;
            try {
                Method saveSelf = Entity.class.getMethod("saveSelfData", net.minecraft.storage.WriteView.class);
                Object ok = saveSelf.invoke(entity, view);
                saved = !(ok instanceof Boolean b) || b;
            } catch (ReflectiveOperationException ignored) {
            }
            if (!saved) {
                try {
                    Method writeData = Entity.class.getMethod("writeData", net.minecraft.storage.WriteView.class);
                    writeData.invoke(entity, view);
                    saved = true;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (saved) {
                nbt = view.getNbt().copy();
            }
        } catch (Throwable ignored) {
        }
        //?}

        if (nbt.isEmpty()) {
            saveAllDataLegacy(entity, nbt, lookup);
        }

        for (String key : new String[] {
                "UUID", "UUIDLeast", "UUIDMost", "Pos", "Motion", "Rotation",
                "dimension", "WorldUUIDLeast", "WorldUUIDMost", "id"
        }) {
            nbt.remove(key);
        }
        return nbt;
    }

    public static void saveAllData(Entity entity, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (entity == null || nbt == null)
            return;

        NbtCompound saved = saveEntityWithoutId(entity);
        for (String key : saved.getKeys()) {
            net.minecraft.nbt.NbtElement el = saved.get(key);
            if (el != null) {
                nbt.put(key, el.copy());
            }
        }
    }

    private static void saveAllDataLegacy(Entity entity, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        String[] prioritized = {
                "writeNbt", "writeCustomDataToNbt", "saveWithoutId", "addAdditionalSaveData",
                "saveNbt", "saveCustomDataToNbt", "method_5652", "method_5651"
        };

        Class<?> current = entity.getClass();
        while (current != null && current != Object.class) {
            for (String name : prioritized) {
                try {
                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt, lookup);
                        if (!nbt.isEmpty()) {
                            return;
                        }
                    } catch (NoSuchMethodException ignored) {
                    }

                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt);
                        if (!nbt.isEmpty()) {
                            return;
                        }
                    } catch (NoSuchMethodException ignored) {
                    }
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }

        current = entity.getClass();
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length >= 1 && params[0] == NbtCompound.class) {
                    try {
                        m.setAccessible(true);
                        if (params.length == 2 && params[1].isAssignableFrom(lookup.getClass())) {
                            m.invoke(entity, nbt, lookup);
                        } else if (params.length == 1) {
                            m.invoke(entity, nbt);
                        }

                        if (!nbt.isEmpty()) {
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        System.err.println("ReflectionUtils: FAILED TO SAVE NBT FOR " + entity.getClass().getSimpleName());
    }

    private static Method cachedLoadMethod = null;

    public static void loadAllData(Entity entity, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (entity == null || nbt == null)
            return;

        //? if 1.21.10 || 1.21.11 {
        try {
            net.minecraft.storage.ReadView view = net.minecraft.storage.NbtReadView.create(
                    net.minecraft.util.ErrorReporter.EMPTY, lookup, nbt);
            Method readData = Entity.class.getMethod("readData", net.minecraft.storage.ReadView.class);
            readData.invoke(entity, view);
            return;
        } catch (Throwable ignored) {
        }
        //?}

        String[] prioritized = {
                "readNbt", "readCustomDataFromNbt", "load", "readAdditionalSaveData",
                "loadNbt", "loadCustomDataFromNbt", "method_5650", "method_5648"
        };

        Class<?> current = entity.getClass();
        while (current != null && current != Object.class) {
            for (String name : prioritized) {
                try {
                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt, lookup);
                        return;
                    } catch (NoSuchMethodException ignored) {
                    }

                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt);
                        return;
                    } catch (NoSuchMethodException ignored) {
                    }
                } catch (Exception ignored) {
                }
            }
            current = current.getSuperclass();
        }

        current = entity.getClass();
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length >= 1 && params[0] == NbtCompound.class) {
                    try {
                        m.setAccessible(true);
                        String name = m.getName().toLowerCase();
                        if (name.contains("read") || name.contains("load") || name.startsWith("method_")) {
                            if (params.length == 2 && params[1].isAssignableFrom(lookup.getClass())) {
                                m.invoke(entity, nbt, lookup);
                            } else if (params.length == 1) {
                                m.invoke(entity, nbt);
                            }
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            current = current.getSuperclass();
        }
        System.err.println("ReflectionUtils: FAILED TO LOAD NBT FOR " + entity.getClass().getSimpleName());
    }

    public static int getLevel(VillagerData data) {
        if (data == null) {
            return 1;
        }
        try {
            if (levelMethod != null) {
                return ((Number) levelMethod.invoke(data)).intValue();
            }
            if (levelField != null) {
                return levelField.getInt(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static Object getProfession(VillagerData data) {
        if (data == null) {
            return net.minecraft.village.VillagerProfession.NONE;
        }
        try {
            if (professionMethod != null) {
                return professionMethod.invoke(data);
            }
            if (professionField != null) {
                return professionField.get(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return net.minecraft.village.VillagerProfession.NONE;
    }

    public static Object getType(VillagerData data) {
        if (data == null) {
            return net.minecraft.village.VillagerType.PLAINS;
        }
        try {
            if (typeMethod != null) {
                return typeMethod.invoke(data);
            }
            if (typeField != null) {
                return typeField.get(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return net.minecraft.village.VillagerType.PLAINS;
    }

    public static boolean isClient(World world) {
        if (world == null) {
            return false;
        }
        try {
            if (isClientField != null) {
                return isClientField.getBoolean(world);
            }
            if (isClientMethod != null) {
                return (boolean) isClientMethod.invoke(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return !(world instanceof net.minecraft.server.world.ServerWorld);
    }

    public static Object getEntryValue(Object entry) {
        if (entry == null)
            return null;

        if (entry instanceof net.minecraft.item.Item) return entry;

        try {
            String[] methodNames = {"value", "getValue", "get", "method_40220", "v"};
            
            Class<?> current = entry.getClass();
            while (current != null && current != Object.class) {
                for (String name : methodNames) {
                    try {
                        Method m = current.getDeclaredMethod(name);
                        m.setAccessible(true);
                        Object result = m.invoke(entry);
                        if (result != null) {
                            return result;
                        }
                    } catch (NoSuchMethodException ignored) {
                    } catch (Exception e) {
                    }
                }
                current = current.getSuperclass();
            }

            current = entry.getClass();
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    Object val = f.get(entry);
                    if (val != null) {
                        if (val instanceof net.minecraft.item.Item || val.getClass().getName().contains("class_1792")) {
                             return val;
                        }
                    }
                }
                current = current.getSuperclass();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setRewardExp(net.minecraft.village.TradeOffer offer, boolean reward) {
        if (offer == null)
            return;
        try {
            Field expField = null;

            int booleanCount = 0;
            for (Field f : net.minecraft.village.TradeOffer.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                
                if (f.getType() == boolean.class) {
                    booleanCount++;
                    f.setAccessible(true);
                    if (expField == null) expField = f;
                }
            }

            if (expField == null) {
                 try {
                     expField = net.minecraft.village.TradeOffer.class.getDeclaredField("field_18677");
                     expField.setAccessible(true);
                 } catch (Exception e) {}
            }
            
            if (expField == null) {
                 try {
                     expField = net.minecraft.village.TradeOffer.class.getDeclaredField("rewardingPlayerExperience");
                     expField.setAccessible(true);
                 } catch (Exception e) {}
            }

            if (expField != null) {
                expField.setBoolean(offer, reward);
            } else {
                System.err.println("ReflectionUtils.setRewardExp: FAILED to find boolean field for XP Reward!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleQuickMoveSync(net.minecraft.screen.slot.SlotActionType actionType, net.minecraft.entity.player.PlayerEntity player) {
        if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
            forceSyncScreen(player);
        }
    }

    public static void forceSyncScreen(net.minecraft.entity.player.PlayerEntity player) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp && sp.currentScreenHandler != null) {
            try {
                int currentRev = sp.currentScreenHandler.getRevision();
                sp.currentScreenHandler.sendContentUpdates();

                for (int i = 0; i < sp.currentScreenHandler.slots.size(); i++) {
                    NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                            sp.currentScreenHandler.syncId, 
                            currentRev, 
                            i, 
                            sp.currentScreenHandler.slots.get(i).getStack().copy()
                    ));
                }
                NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                        -1, 
                        currentRev, 
                        -1, 
                        sp.currentScreenHandler.getCursorStack().copy()
                ));

                int newRev = currentRev + 1;
                try {
                    java.lang.reflect.Method m = net.minecraft.screen.ScreenHandler.class.getDeclaredMethod("nextRevision");
                    m.setAccessible(true);
                    m.invoke(sp.currentScreenHandler);
                    newRev = sp.currentScreenHandler.getRevision();
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Field f = net.minecraft.screen.ScreenHandler.class.getDeclaredField("revision");
                        f.setAccessible(true);
                        newRev = f.getInt(sp.currentScreenHandler) + 1;
                        f.setInt(sp.currentScreenHandler, newRev);
                    } catch (Exception ex) {
                        try {
                            java.lang.reflect.Field f = net.minecraft.screen.ScreenHandler.class.getDeclaredField("field_29241");
                            f.setAccessible(true);
                            newRev = f.getInt(sp.currentScreenHandler) + 1;
                            f.setInt(sp.currentScreenHandler, newRev);
                        } catch (Exception ex2) {
                            newRev = sp.currentScreenHandler.getRevision() + 1;
                        }
                    }
                }

                net.minecraft.util.collection.DefaultedList<net.minecraft.item.ItemStack> contents = 
                        net.minecraft.util.collection.DefaultedList.ofSize(sp.currentScreenHandler.slots.size(), net.minecraft.item.ItemStack.EMPTY);
                for (int i = 0; i < sp.currentScreenHandler.slots.size(); i++) {
                    contents.set(i, sp.currentScreenHandler.slots.get(i).getStack().copy());
                }

                NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.InventoryS2CPacket(
                        sp.currentScreenHandler.syncId, 
                        newRev, 
                        contents, 
                        sp.currentScreenHandler.getCursorStack().copy()
                ));

                for (int i = 0; i < sp.currentScreenHandler.slots.size(); i++) {
                    NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                            sp.currentScreenHandler.syncId, 
                            newRev, 
                            i, 
                            sp.currentScreenHandler.slots.get(i).getStack().copy()
                    ));
                }
                
                NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                        -1, 
                        newRev, 
                        -1, 
                        sp.currentScreenHandler.getCursorStack().copy()
                ));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isBedNearby(Entity entity) {
        World world = entity.getEntityWorld();
        BlockPos feet = entity.getBlockPos();
        BlockPos center = BlockPos.ofFloored(
                entity.getX(),
                entity.getY() + entity.getHeight() / 2.0,
                entity.getZ());

        BlockPos[] origins = {feet, feet.down(), center};
        for (BlockPos origin : origins) {
            if (scanBedCube(world, origin)) {
                return true;
            }
        }
        return false;
    }

    private static boolean scanBedCube(World world, BlockPos origin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState state = world.getBlockState(origin.add(x, y, z));
                    if (state.isIn(BlockTags.BEDS) || state.getBlock() instanceof BedBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
