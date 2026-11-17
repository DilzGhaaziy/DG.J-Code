package id.dg.jcode.ui.theme;

import id.dg.jcode.ui.util.Colors;

import java.awt.Color;
import java.util.Locale;

/**
 * Daftar tema. Angka hex: 6 warna UI (latar, permukaan, border, teks, teks redup, aksen)
 * lalu 12 warna editor (latar, teks, baris aktif, seleksi, gutter, nomor baris,
 * keyword, tipe, string, angka, komentar, anotasi).
 */
public enum ThemeMode {
    NORWAY_TODAY("Norway Today", false, true,
            0xEEF1F4, 0xFFFFFF, 0xD3D9E0, 0x1E2A36, 0x5E6B78, 0x2A6EBB,
            0xFFFFFF, 0x1E2A36, 0xF0F4F9, 0xC9DDF2, 0xF6F8FA, 0x9AA5B1,
            0x0A4F8F, 0x0F7B7B, 0xB54708, 0x7B3FA0, 0x8794A1, 0x956400),
    CITY_LIGHTS("City Lights", true, true,
            0x161C22, 0x1D252C, 0x2C3742, 0xB7C5D3, 0x7F95A8, 0x5EC4FF,
            0x1D252C, 0xB7C5D3, 0x232D36, 0x2F4459, 0x1A2128, 0x4A5A6A,
            0x5EC4FF, 0x33CED8, 0x8BD49C, 0xE27E8D, 0x5E7183, 0xEBBF83),
    FLATLAF_DARK("FlatLaf Dark", true, false,
            0x1B1D21, 0x24272C, 0x363B42, 0xE6E8EB, 0x9AA1AB, 0x4C8BF5,
            0x1E1F22, 0xBCBEC4, 0x26282E, 0x214283, 0x1E1F22, 0x4B5059,
            0xCF8E6D, 0x2AACB8, 0x6AAB73, 0x2AACB8, 0x7A7E85, 0xB3AE60),
    FLATLAF_LIGHT("FlatLaf Light", false, false,
            0xF5F6F8, 0xFFFFFF, 0xE1E4E8, 0x1F2328, 0x5F6670, 0x2F6FEB,
            0xFFFFFF, 0x1F2328, 0xF3F6FA, 0xCCE0FF, 0xFAFBFC, 0xA0A7B0,
            0x0033B3, 0x00627A, 0x067D17, 0x1750EB, 0x8C8C8C, 0x9E880D);

    public final String label;
    public final boolean dark;
    /** true = warna latar/teks FlatLaf ikut diganti; false = warna bawaan FlatLaf. */
    public final boolean customUi;
    public final Palette palette;
    public final Color editorBg, editorText, currentLine, selection, gutterBg, lineNumber;
    public final Color keyword, type, string, number, comment, annotation;

    ThemeMode(String label, boolean dark, boolean customUi, int... c) {
        this.label = label;
        this.dark = dark;
        this.customUi = customUi;
        Color surface = new Color(c[1]);
        Color background = new Color(c[0]);
        Color accent = new Color(c[5]);
        this.palette = new Palette(background, surface,
                dark ? Colors.mix(surface, Color.WHITE, 0.04f) : Colors.mix(surface, background, 0.4f),
                new Color(c[2]), new Color(c[3]), new Color(c[4]), accent,
                Colors.mix(surface, accent, dark ? 0.18f : 0.10f));
        editorBg = new Color(c[6]);
        editorText = new Color(c[7]);
        currentLine = new Color(c[8]);
        selection = new Color(c[9]);
        gutterBg = new Color(c[10]);
        lineNumber = new Color(c[11]);
        keyword = new Color(c[12]);
        type = new Color(c[13]);
        string = new Color(c[14]);
        number = new Color(c[15]);
        comment = new Color(c[16]);
        annotation = new Color(c[17]);
    }

    @Override
    public String toString() {
        return label;
    }

    /** Default: Norway Today. Nilai lama "light"/"dark" dari Tahap 1 tetap dikenali. */
    public static ThemeMode parse(String value) {
        if (value == null || value.isBlank()) {
            return NORWAY_TODAY;
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (v.equals("DARK")) {
            return FLATLAF_DARK;
        }
        try {
            return valueOf(v);
        } catch (IllegalArgumentException e) {
            return NORWAY_TODAY;
        }
    }
}
