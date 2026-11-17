package id.dg.jcode.ui.theme;

import java.awt.Color;

/**
 * Palet warna netral dengan satu warna aksen (biru).
 * Dipakai oleh komponen yang digambar sendiri (splash, kartu, logo).
 */
public record Palette(
        Color background,
        Color surface,
        Color surfaceHover,
        Color border,
        Color text,
        Color textMuted,
        Color accent,
        Color accentSoft) {

    public static final Palette LIGHT = new Palette(
            new Color(0xF5F6F8),
            new Color(0xFFFFFF),
            new Color(0xFBFCFD),
            new Color(0xE1E4E8),
            new Color(0x1F2328),
            new Color(0x5F6670),
            new Color(0x2F6FEB),
            new Color(0xE8F0FE));

    public static final Palette DARK = new Palette(
            new Color(0x1B1D21),
            new Color(0x24272C),
            new Color(0x2B2F35),
            new Color(0x363B42),
            new Color(0xE6E8EB),
            new Color(0x9AA1AB),
            new Color(0x4C8BF5),
            new Color(0x22314A));
}
