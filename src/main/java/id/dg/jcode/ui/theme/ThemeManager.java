package id.dg.jcode.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mengatur tema. ARC = 16 berarti radius sudut 8 px (Java2D memakai diameter). */
public final class ThemeManager {

    public static final int ARC = 16;
    public static final int SPACE = 8;

    private static volatile ThemeMode current = ThemeMode.NORWAY_TODAY;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private ThemeManager() {
    }

    public static void install(ThemeMode mode) {
        Palette p = mode.palette;
        Map<String, String> extra = new HashMap<>();
        extra.put("@accentColor", toHex(p.accent()));
        if (mode.customUi) {
            extra.put("@background", toHex(p.background()));
            extra.put("@foreground", toHex(p.text()));
        }
        FlatLaf.setGlobalExtraDefaults(extra);
        if (mode.dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        customize();
        current = mode;
    }

    public static void switchTo(ThemeMode mode) {
        if (mode == current) {
            return;
        }
        install(mode);
        FlatLaf.updateUI();
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static ThemeMode current() {
        return current;
    }

    public static boolean isDark() {
        return current.dark;
    }

    public static Palette palette() {
        return current.palette;
    }

    public static Palette palette(ThemeMode mode) {
        return mode.palette;
    }

    private static void customize() {
        UIManager.put("defaultFont", Fonts.ui(13f));
        UIManager.put("Component.arc", ARC);
        UIManager.put("Button.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("ProgressBar.arc", ARC);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    public static String toHex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }
}
