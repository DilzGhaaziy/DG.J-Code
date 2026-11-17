package id.dg.jcode.theme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.util.prefs.Preferences;

/** Mengganti tema FlatLaf untuk SELURUH aplikasi dan mengingat pilihan terakhir. */
public final class AppTheme {
    public enum Choice {
        LIGHT("Terang (FlatLaf Light)"),
        DARK("Gelap (FlatLaf Dark)"),
        INTELLIJ("IntelliJ"),
        DARCULA("Darcula");

        private final String label;
        Choice(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppTheme.class);
    private static final String KEY = "theme";

    private AppTheme() { }

    /** Panggil di main() SEBELUM jendela dibuat, menggantikan FlatLightLaf.setup() yang lama. */
    public static void setupSaved() {
        Choice c;
        try {
            c = Choice.valueOf(PREFS.get(KEY, Choice.LIGHT.name()));
        } catch (IllegalArgumentException e) {
            c = Choice.LIGHT;
        }
        setup(c);
    }

    /** Ganti tema saat aplikasi berjalan. Semua jendela + editor ikut berubah. */
    public static void change(Choice c) {
        setup(c);
        FlatLaf.updateUI();
        PREFS.put(KEY, c.name());
    }

    /** Tema yang sedang aktif (dibaca dari Look and Feel, bukan dari Preferences). */
    public static Choice current() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf instanceof FlatDarculaLaf) return Choice.DARCULA;   // cek subclass dulu
        if (laf instanceof FlatDarkLaf) return Choice.DARK;
        if (laf instanceof FlatIntelliJLaf) return Choice.INTELLIJ;
        return Choice.LIGHT;
    }

    private static void setup(Choice c) {
        switch (c) {
            case DARK -> FlatDarkLaf.setup();
            case INTELLIJ -> FlatIntelliJLaf.setup();
            case DARCULA -> FlatDarculaLaf.setup();
            default -> FlatLightLaf.setup();
        }
    }
}
