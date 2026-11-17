package id.dg.jcode.settings;

import id.dg.jcode.AppInfo;
import id.dg.jcode.ui.theme.ThemeMode;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Menyimpan pengaturan pengguna di file lokal:
 * Windows: %APPDATA%\DG J-Code\settings.properties
 * Lainnya: ~/.config/DG J-Code/settings.properties
 */
public final class Settings {

    public static final int MIN_FONT_SIZE = 10;
    public static final int MAX_FONT_SIZE = 28;
    public static final int DEFAULT_FONT_SIZE = 14;

    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT_SIZE = "editor.fontSize";

    private final Path file;
    private final Properties props = new Properties();

    private Settings(Path file) {
        this.file = file;
    }

    public static Settings load() {
        Settings settings = new Settings(configDir().resolve("settings.properties"));
        if (Files.isRegularFile(settings.file)) {
            try (Reader reader = Files.newBufferedReader(settings.file, StandardCharsets.UTF_8)) {
                settings.props.load(reader);
            } catch (IOException e) {
                System.err.println("Gagal membaca pengaturan: " + e.getMessage());
            }
        }
        return settings;
    }

    public static Path configDir() {
        String appData = System.getenv("APPDATA");
        Path base = (appData != null && !appData.isBlank())
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve(AppInfo.NAME);
    }

    public ThemeMode getTheme() {
        return ThemeMode.parse(props.getProperty(KEY_THEME));
    }

    public void setTheme(ThemeMode mode) {
        props.setProperty(KEY_THEME, mode.name().toLowerCase(Locale.ROOT));
    }

    public int getEditorFontSize() {
        String raw = props.getProperty(KEY_FONT_SIZE, String.valueOf(DEFAULT_FONT_SIZE));
        try {
            return clampFont(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_FONT_SIZE;
        }
    }

    public void setEditorFontSize(int size) {
        props.setProperty(KEY_FONT_SIZE, String.valueOf(clampFont(size)));
    }

    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(writer, AppInfo.NAME + " - pengaturan pengguna");
            }
        } catch (IOException e) {
            System.err.println("Gagal menyimpan pengaturan: " + e.getMessage());
        }
    }

    private static int clampFont(int size) {
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
    }
}
