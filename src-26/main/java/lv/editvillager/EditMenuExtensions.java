package lv.editvillager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

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
        void onClick(ServerPlayer player, Villager villager);
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

    public static MenuType<?> getMenuType() {
        return switch (extraRows) {
            case 0 -> MenuType.GENERIC_9x1;
            case 1 -> MenuType.GENERIC_9x2;
            case 2 -> MenuType.GENERIC_9x3;
            case 3 -> MenuType.GENERIC_9x4;
            case 4 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
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

    static void applyButtons(SimpleContainer menu) {
        for (RegisteredButton button : BUTTONS) {
            if (button.slotIndex >= 0 && button.slotIndex < menu.getContainerSize()) {
                ItemStack stack = button.icon.get();
                if (stack != null && !stack.isEmpty()) {
                    menu.setItem(button.slotIndex, stack);
                }
            }
        }
    }

    /** @return true, если клик обработан аддоном */
    static boolean handleClick(int slotIndex, ServerPlayer player, Villager villager) {
        for (RegisteredButton button : BUTTONS) {
            if (button.slotIndex == slotIndex) {
                button.action.onClick(player, villager);
                return true;
            }
        }
        return false;
    }

    /** Только для тестов / сброса. */
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
