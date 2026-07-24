package lv.editvillager;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

/**
 * Thin NBT helpers. Must call Minecraft APIs directly so Loom remaps them —
 * reflective yarn names like {@code getBoolean(String)} do not exist at runtime.
 */
public final class NbtCompat {

    private NbtCompat() {
    }

    public static boolean getBoolean(NbtCompound nbt, String key, boolean def) {
        if (nbt == null || !nbt.contains(key)) {
            return def;
        }
        //? if 1.21.1 {
        return nbt.getBoolean(key);
        //?} else {
        return nbt.getBoolean(key, def);
        //?}
    }

    public static long getLong(NbtCompound nbt, String key, long def) {
        if (nbt == null || !nbt.contains(key)) {
            return def;
        }
        //? if 1.21.1 {
        return nbt.getLong(key);
        //?} else {
        return nbt.getLong(key, def);
        //?}
    }

    public static int getInt(NbtCompound nbt, String key, int def) {
        if (nbt == null || !nbt.contains(key)) {
            return def;
        }
        //? if 1.21.1 {
        return nbt.getInt(key);
        //?} else {
        return nbt.getInt(key, def);
        //?}
    }

    public static String getString(NbtCompound nbt, String key, String def) {
        if (nbt == null || !nbt.contains(key)) {
            return def;
        }
        //? if 1.21.1 {
        return nbt.getString(key);
        //?} else {
        return nbt.getString(key, def);
        //?}
    }

    public static NbtCompound getCompound(NbtCompound nbt, String key) {
        if (nbt == null || !nbt.contains(key)) {
            return new NbtCompound();
        }
        //? if 1.21.1 {
        return nbt.getCompound(key);
        //?} else {
        return nbt.getCompoundOrEmpty(key);
        //?}
    }

    public static long[] getLongArray(NbtCompound nbt, String key) {
        if (nbt == null || !nbt.contains(key)) {
            return new long[0];
        }
        //? if 1.21.1 {
        return nbt.getLongArray(key);
        //?} else {
        return nbt.getLongArray(key).orElse(new long[0]);
        //?}
    }

    public static NbtCompound getListCompound(NbtList list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return new NbtCompound();
        }
        //? if 1.21.1 {
        return list.getCompound(index);
        //?} else {
        return list.getCompoundOrEmpty(index);
        //?}
    }
}
