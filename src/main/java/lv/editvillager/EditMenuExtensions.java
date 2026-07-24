package lv.editvillager;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * API для аддонов: доп. строки и кнопки в главном меню жителя (Shift+ПКМ).
 * Без зарегистрированных расширений меню остаётся 1×9.
 */
public final class EditMenuExtensions {

    @FunctionalInterface
    public interface SlotAction {
        void onClick(ServerPlayerEntity player, VillagerEntity villager);
    }

    private static final class RegisteredButton {
        final int slotIndex;
        final Supplier<ItemStack> icon;
        final SlotAction action;

        RegisteredButton(int slotIndex, Supplier<ItemStack> icon, SlotAction action) {
            this.slotIndex = slotIndex;
            this.icon = icon;
            this.action = action;
        }
    }

    private static int extraRows = 0;
    private static final List<RegisteredButton> BUTTONS = new CopyOnWriteArrayList<>();

    private EditMenuExtensions() {}

    /** Запросить дополнительные строки под первой (макс. 5). Вызывать при инициализации аддона. */
    public static void requestExtraRows(int rows) {
        extraRows = Math.max(extraRows, Math.max(0, Math.min(5, rows)));
    }

    public static int getExtraRows() {
        return extraRows;
    }

    public static int getMenuSize() {
        return 9 * (1 + extraRows);
    }

    public static ScreenHandlerType<?> getMenuType() {
        return switch (extraRows) {
            case 0 -> ScreenHandlerType.GENERIC_9X1;
            case 1 -> ScreenHandlerType.GENERIC_9X2;
            case 2 -> ScreenHandlerType.GENERIC_9X3;
            case 3 -> ScreenHandlerType.GENERIC_9X4;
            case 4 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    /**
     * Зарегистрировать кнопку. Индексы: 0–8 первая строка, 9–17 вторая и т.д.
     * Слот 9 = 10-й слот (первая ячейка второй строки).
     */
    public static void registerButton(int slotIndex, Supplier<ItemStack> icon, SlotAction action) {
        if (slotIndex < 0 || icon == null || action == null) {
            return;
        }
        BUTTONS.removeIf(b -> b.slotIndex == slotIndex);
        BUTTONS.add(new RegisteredButton(slotIndex, icon, action));
    }

    static void applyButtons(SimpleInventory menu) {
        for (RegisteredButton button : BUTTONS) {
            if (button.slotIndex >= 0 && button.slotIndex < menu.size()) {
                ItemStack stack = button.icon.get();
                if (stack != null && !stack.isEmpty()) {
                    menu.setStack(button.slotIndex, stack);
                }
            }
        }
    }

    /** @return true, если клик обработан аддоном */
    static boolean handleClick(int slotIndex, ServerPlayerEntity player, VillagerEntity villager) {
        for (RegisteredButton button : BUTTONS) {
            if (button.slotIndex == slotIndex) {
                button.action.onClick(player, villager);
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        extraRows = 0;
        BUTTONS.clear();
    }

    static List<Integer> registeredSlots() {
        List<Integer> list = new ArrayList<>();
        for (RegisteredButton button : BUTTONS) {
            list.add(button.slotIndex);
        }
        return list;
    }
}
