package id.dg.jcode.ui.theme;

import java.awt.Font;
import java.util.Locale;

/**
 * Pemilih font dengan fallback.
 * Tidak memakai getAvailableFontFamilyNames() karena lambat saat startup.
 */
public final class Fonts {

    private static final String[] WIDE_FALLBACKS = {
            "Microsoft YaHei UI", "Microsoft JhengHei UI", "Yu Gothic UI", "Malgun Gothic", Font.DIALOG
    };

    private Fonts() {
    }

    public static Font firstAvailable(int style, float size, String... families) {
        for (String family : families) {
            Font font = new Font(family, style, 12);
            if (isLogical(family) || !"Dialog".equalsIgnoreCase(font.getFamily(Locale.ROOT))) {
                return font.deriveFont(style, size);
            }
        }
        return new Font(Font.DIALOG, style, 12).deriveFont(style, size);
    }

    public static Font ui(float size) {
        return firstAvailable(Font.PLAIN, size, "Segoe UI", Font.SANS_SERIF);
    }

    public static Font uiBold(float size) {
        return firstAvailable(Font.BOLD, size, "Segoe UI", Font.SANS_SERIF);
    }

    public static Font uiSemibold(float size) {
        Font font = new Font("Segoe UI Semibold", Font.PLAIN, 12);
        if (!"Dialog".equalsIgnoreCase(font.getFamily(Locale.ROOT))) {
            return font.deriveFont(Font.PLAIN, size);
        }
        return uiBold(size);
    }

    public static Font code(float size) {
        return firstAvailable(Font.PLAIN, size, "JetBrains Mono", "Consolas", Font.MONOSPACED);
    }

    /**
     * Font UI yang dijamin bisa menampilkan seluruh teks.
     * Contoh: Segoe UI tidak punya karakter "乡", maka dipakai Microsoft YaHei UI.
     */
    public static Font forText(String text, int style, float size) {
        Font base = firstAvailable(style, size, "Segoe UI", Font.SANS_SERIF);
        if (base.canDisplayUpTo(text) == -1) {
            return base;
        }
        for (String family : WIDE_FALLBACKS) {
            Font candidate = new Font(family, style, 12).deriveFont(style, size);
            if (candidate.canDisplayUpTo(text) == -1) {
                return candidate;
            }
        }
        return base;
    }

    private static boolean isLogical(String family) {
        return switch (family) {
            case Font.DIALOG, Font.DIALOG_INPUT, Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED -> true;
            default -> false;
        };
    }
}
