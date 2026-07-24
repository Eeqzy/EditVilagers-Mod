package lv.editvillager;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class McCompat {

    public static final Item GRAY_STAINED_GLASS_PANE;
    public static final Item LIGHT_GRAY_STAINED_GLASS_PANE;
    public static final Item WHITE_STAINED_GLASS_PANE;
    public static final Item LIME_STAINED_GLASS_PANE;
    public static final Item GREEN_STAINED_GLASS_PANE;
    public static final Item RED_STAINED_GLASS_PANE;
    /** Радужный порядок для анимации доп. строк меню. */
    public static final Item[] RAINBOW_STAINED_GLASS_PANES;
    public static final Item RED_CONCRETE;
    public static final Item RED_BED;
    public static final Item WHITE_DYE;
    public static final Item LIGHTNING_ROD;
    public static final EntityType<Villager> VILLAGER;

    static {
        GRAY_STAINED_GLASS_PANE = resolveColoredItem("GRAY_STAINED_GLASS_PANE", "gray", "STAINED_GLASS_PANE");
        LIGHT_GRAY_STAINED_GLASS_PANE = resolveColoredItem(
                "LIGHT_GRAY_STAINED_GLASS_PANE", "lightGray", "STAINED_GLASS_PANE");
        WHITE_STAINED_GLASS_PANE = resolveColoredItem(
                "WHITE_STAINED_GLASS_PANE", "white", "STAINED_GLASS_PANE");
        LIME_STAINED_GLASS_PANE = resolveColoredItem("LIME_STAINED_GLASS_PANE", "lime", "STAINED_GLASS_PANE");
        GREEN_STAINED_GLASS_PANE = resolveColoredItem("GREEN_STAINED_GLASS_PANE", "green", "STAINED_GLASS_PANE");
        RED_STAINED_GLASS_PANE = resolveColoredItem("RED_STAINED_GLASS_PANE", "red", "STAINED_GLASS_PANE");
        RAINBOW_STAINED_GLASS_PANES = new Item[] {
                resolveColoredItem("RED_STAINED_GLASS_PANE", "red", "STAINED_GLASS_PANE"),
                resolveColoredItem("ORANGE_STAINED_GLASS_PANE", "orange", "STAINED_GLASS_PANE"),
                resolveColoredItem("YELLOW_STAINED_GLASS_PANE", "yellow", "STAINED_GLASS_PANE"),
                resolveColoredItem("LIME_STAINED_GLASS_PANE", "lime", "STAINED_GLASS_PANE"),
                resolveColoredItem("LIGHT_BLUE_STAINED_GLASS_PANE", "lightBlue", "STAINED_GLASS_PANE"),
                resolveColoredItem("BLUE_STAINED_GLASS_PANE", "blue", "STAINED_GLASS_PANE"),
                resolveColoredItem("PURPLE_STAINED_GLASS_PANE", "purple", "STAINED_GLASS_PANE"),
                resolveColoredItem("MAGENTA_STAINED_GLASS_PANE", "magenta", "STAINED_GLASS_PANE"),
                resolveColoredItem("PINK_STAINED_GLASS_PANE", "pink", "STAINED_GLASS_PANE")
        };
        RED_CONCRETE = resolveColoredItem("RED_CONCRETE", "red", "CONCRETE");
        RED_BED = resolveColoredItem("RED_BED", "red", "BED");
        WHITE_DYE = resolveColoredItem("WHITE_DYE", "white", "DYE");
        LIGHTNING_ROD = resolveLightningRod();
        VILLAGER = resolveVillagerType();
    }

    private McCompat() {
    }

    private static Item resolveColoredItem(String legacyField, String colorMethod, String colorCollectionField) {
        try {
            Field field = Items.class.getField(legacyField);
            return (Item) field.get(null);
        } catch (Exception ignored) {
        }
        try {
            Field collectionField = Items.class.getField(colorCollectionField);
            Object collection = collectionField.get(null);
            Method method = collection.getClass().getMethod(colorMethod);
            return (Item) method.invoke(collection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve item: " + legacyField, e);
        }
    }

    private static Item resolveLightningRod() {
        try {
            Object rod = Items.class.getField("LIGHTNING_ROD").get(null);
            if (rod instanceof Item item) {
                return item;
            }
            Method weathering = rod.getClass().getMethod("weathering");
            Object weathered = weathering.invoke(rod);
            Method unaffected = weathered.getClass().getMethod("unaffected");
            return (Item) unaffected.invoke(weathered);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve lightning rod item", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Villager> resolveVillagerType() {
        try {
            Field field = EntityType.class.getField("VILLAGER");
            return (EntityType<Villager>) field.get(null);
        } catch (Exception ignored) {
        }
        try {
            Class<?> entityTypes = Class.forName("net.minecraft.world.entity.EntityTypes");
            Field field = entityTypes.getField("VILLAGER");
            return (EntityType<Villager>) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve villager entity type", e);
        }
    }
}
