package lv.editvillager;

import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

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
                System.out.println("ReflectionUtils: Found VillagerData level method: " + name);
                break;
            } catch (Exception ignored) {}
        }
        String[] profNames = {"getProfession", "profession", "method_19192"};
        for (String name : profNames) {
            try {
                professionMethod = VillagerData.class.getMethod(name);
                professionMethod.setAccessible(true);
                System.out.println("ReflectionUtils: Found VillagerData profession method: " + name);
                break;
            } catch (Exception ignored) {}
        }
        String[] typeNames = {"getType", "type", "method_19193"};
        for (String name : typeNames) {
            try {
                typeMethod = VillagerData.class.getMethod(name);
                typeMethod.setAccessible(true);
                System.out.println("ReflectionUtils: Found VillagerData type method: " + name);
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
                         System.out.println("ReflectionUtils: Found level field by type (int): " + f.getName());
                     } else if (type == VillagerProfession.class && professionField == null) {
                         professionField = f;
                         System.out.println("ReflectionUtils: Found profession field by type: " + f.getName());
                     } else if (type == VillagerType.class && typeField == null) {
                         typeField = f;
                         System.out.println("ReflectionUtils: Found type field by type: " + f.getName());
                     }
                }
                if (levelField == null) {
                    try {
                        levelField = VillagerData.class.getDeclaredField("field_18555");
                        levelField.setAccessible(true);
                        System.out.println("ReflectionUtils: Found level field via name: field_18555");
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
            System.out.println("ReflectionUtils: Found isClient field via 'isClient'");
        } catch (NoSuchFieldException e) {
            try {
                isClientField = World.class.getDeclaredField("field_9236");
                isClientField.setAccessible(true);
                System.out.println("ReflectionUtils: Found isClient field via usage of 'field_9236'");
            } catch (Exception ex) {
                System.err.println("ReflectionUtils: Failed to find isClient field");
            }
        }
        String[] worldMethodNames = {"isClient", "isClientSide", "method_8530"};
        for (String name : worldMethodNames) {
            try {
                isClientMethod = World.class.getMethod(name);
                isClientMethod.setAccessible(true);
                System.out.println("ReflectionUtils: Found World isClient method: " + name);
                break;
            } catch (Exception ignored) {}
        }


        try {
            Class<?> tradedItemClass = Class.forName("net.minecraft.village.TradedItem");
            Class<?> registryEntryClass = null;
            try { registryEntryClass = Class.forName("net.minecraft.registry.entry.RegistryEntry"); } catch(Exception e) {}
            if (registryEntryClass == null) {
                try { registryEntryClass = Class.forName("net.minecraft.class_6880"); } catch(Exception e) {}
            }

            String[] itemMethodNames = {"item", "getItem", "method_58286"};
            for (String name : itemMethodNames) {
                try {
                    tradedItemItemMethod = tradedItemClass.getMethod(name);
                    tradedItemItemMethod.setAccessible(true);
                    System.out.println("ReflectionUtils: Found TradedItem item method: " + name);
                    break;
                } catch (Exception ignored) {}
            }
            String[] countMethodNames = {"count", "getCount", "method_58284"};
            for (String name : countMethodNames) {
                try {
                    tradedItemCountMethod = tradedItemClass.getMethod(name);
                    tradedItemCountMethod.setAccessible(true);
                    System.out.println("ReflectionUtils: Found TradedItem count method: " + name);
                    break;
                } catch (Exception ignored) {}
            }

            if (tradedItemItemMethod == null || tradedItemCountMethod == null) {
                for (Field f : tradedItemClass.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);

                    Class<?> type = f.getType();
                    String typeName = type.getSimpleName();
                    String fullName = type.getName();

                    boolean isItem = (type == net.minecraft.item.Item.class);
                    boolean isRegistryEntry = (registryEntryClass != null && registryEntryClass.isAssignableFrom(type)) || 
                                              typeName.contains("RegistryEntry") || 
                                              fullName.contains("RegistryEntry") ||
                                              fullName.contains("class_6880");
                    boolean isHolder = typeName.contains("Holder") || fullName.contains("Holder");

                    if ((isItem || isRegistryEntry || isHolder) && tradedItemItemField == null) {
                        tradedItemItemField = f;
                        System.out.println("ReflectionUtils: Found TradedItem item field: " + f.getName() + " (" + fullName + ")");
                    } else if (type == int.class && tradedItemCountField == null) {
                        tradedItemCountField = f;
                        System.out.println("ReflectionUtils: Found TradedItem count field: " + f.getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static net.minecraft.item.ItemStack convertTradedItem(Object tradedItem) {
        if (tradedItem == null) {
            System.out.println("ReflectionUtils.convertTradedItem: tradedItem is null");
            return net.minecraft.item.ItemStack.EMPTY;
        }
        try {
            net.minecraft.item.Item item = net.minecraft.item.Items.AIR;
            int count = 1;

            Object rawItem = null;
            if (tradedItemItemMethod != null) {
                try {
                    rawItem = tradedItemItemMethod.invoke(tradedItem);
                    System.out.println("ReflectionUtils.convertTradedItem: Got rawItem via method: " + rawItem);
                } catch (Exception e) {
                    System.out.println("ReflectionUtils.convertTradedItem: Method access failed: " + e.getMessage());
                }
            }
            if (rawItem == null && tradedItemItemField != null) {
                try {
                    rawItem = tradedItemItemField.get(tradedItem);
                    System.out.println("ReflectionUtils.convertTradedItem: Got rawItem via field: " + rawItem);
                } catch (Exception e) {
                    System.out.println("ReflectionUtils.convertTradedItem: Field access failed: " + e.getMessage());
                }
            }

            if (rawItem instanceof net.minecraft.item.Item i) {
                item = i;
                System.out.println("ReflectionUtils.convertTradedItem: Direct Item: " + item);
            } else if (rawItem != null) {
                Object unwrapped = getEntryValue(rawItem);
                if (unwrapped instanceof net.minecraft.item.Item i) {
                    item = i;
                    System.out.println("ReflectionUtils.convertTradedItem: Unwrapped Item: " + item);
                } else {
                    System.out.println("ReflectionUtils.convertTradedItem: Failed to unwrap, got: " + unwrapped);
                }
            }

            if (tradedItemCountField != null) {
                count = tradedItemCountField.getInt(tradedItem);
                System.out.println("ReflectionUtils.convertTradedItem: Count via field: " + count);
            } else if (tradedItemCountMethod != null) {
                Object c = tradedItemCountMethod.invoke(tradedItem);
                if (c instanceof Integer i) {
                    count = i;
                    System.out.println("ReflectionUtils.convertTradedItem: Count via method: " + count);
                }
            }

            net.minecraft.item.ItemStack result = new net.minecraft.item.ItemStack(item, count);
            System.out.println("ReflectionUtils.convertTradedItem: Final result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("ReflectionUtils.convertTradedItem: Exception occurred");
            e.printStackTrace();
            return net.minecraft.item.ItemStack.EMPTY;
        }
    }

    private static Method cachedSaveMethod = null;

    public static void saveAllData(Entity entity, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (entity == null || nbt == null)
            return;

        String[] prioritized = { "writeNbt", "writeCustomDataToNbt", "saveNbt", "saveCustomDataToNbt", "method_5652",
                "method_5651" };

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
                            System.out.println("ReflectionUtils: Saved via " + name + " in " + current.getSimpleName());
                            return;
                        }
                    } catch (NoSuchMethodException ignored) {
                    }

                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt);
                        if (!nbt.isEmpty()) {
                            System.out.println("ReflectionUtils: Saved via " + name + " in " + current.getSimpleName());
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
                            System.out.println("ReflectionUtils: Saved via search: " + m.getName() + " in "
                                    + current.getSimpleName());
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

        String[] prioritized = { "readNbt", "readCustomDataFromNbt", "loadNbt", "loadCustomDataFromNbt", "method_5650",
                "method_5648" };

        Class<?> current = entity.getClass();
        while (current != null && current != Object.class) {
            for (String name : prioritized) {
                try {
                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class,
                                net.minecraft.registry.RegistryWrapper.WrapperLookup.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt, lookup);
                        System.out.println("ReflectionUtils: Loaded via " + name + " in " + current.getSimpleName());
                        return;
                    } catch (NoSuchMethodException ignored) {
                    }

                    try {
                        Method m = current.getDeclaredMethod(name, NbtCompound.class);
                        m.setAccessible(true);
                        m.invoke(entity, nbt);
                        System.out.println("ReflectionUtils: Loaded via " + name + " in " + current.getSimpleName());
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
                            System.out.println("ReflectionUtils: Loaded via search: " + m.getName() + " in "
                                    + current.getSimpleName());
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
        try {
            if (levelField != null)
                return levelField.getInt(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static Object getProfession(VillagerData data) {
        try {
            if (professionField != null) {
                return professionField.get(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return net.minecraft.village.VillagerProfession.NONE;
    }

    public static Object getType(VillagerData data) {
        try {
            if (typeField != null) {
                return typeField.get(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return net.minecraft.village.VillagerType.PLAINS;
    }

    public static boolean isClient(World world) {
        try {
            if (isClientField != null)
                return isClientField.getBoolean(world);

            try {
                Method m = World.class.getMethod("isClient");
                return (boolean) m.invoke(world);
            } catch(Exception ex) {}
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

            System.out.println("ReflectionUtils.getEntryValue: No extraction method worked for: " + entry + " (Class: " + entry.getClass().getName() + ")");
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
                    System.out.println("ReflectionUtils.setRewardExp: Found boolean field: " + f.getName());
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
}
