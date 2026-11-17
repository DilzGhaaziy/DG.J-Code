package id.dg.jcode.ui.util;

import java.awt.Color;

/** Utilitas warna kecil. */
public final class Colors {

    private Colors() {
    }

    /** Campur dua warna: t = 0 menghasilkan a, t = 1 menghasilkan b. */
    public static Color mix(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * k);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k);
        int al = Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k);
        return new Color(r, g, bl, al);
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
