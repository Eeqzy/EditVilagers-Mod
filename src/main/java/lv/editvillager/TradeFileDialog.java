package lv.editvillager;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TradeFileDialog {

    private TradeFileDialog() {
    }

    /**
     * Opens the native OS file picker (TinyFileDialogs / modern Windows dialog).
     * Returns absolute path, or {@code null} if cancelled / failed.
     */
    public static String pickTradeFile() {
        try {
            Path tradesDir = TradeFileStorage.getTradesDirectory();
            Files.createDirectories(tradesDir);

            String defaultPath = tradesDir.toAbsolutePath().toString();
            if (!defaultPath.endsWith(File.separator)) {
                defaultPath = defaultPath + File.separator;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(0, stack.UTF8("*.evtrades"));
                String selected = TinyFileDialogs.tinyfd_openFileDialog(
                        LanguageManager.tr("trades.files.dialog.title"),
                        defaultPath,
                        filters,
                        LanguageManager.tr("trades.files.dialog.filter"),
                        false);
                return (selected == null || selected.isEmpty()) ? null : selected;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}
