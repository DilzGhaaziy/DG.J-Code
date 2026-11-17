package id.dg.jcode.ui.util;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Map;

/** Bantuan menggambar dengan anti-aliasing. */
public final class Gfx {

    private Gfx() {
    }

    /**
     * Membuat salinan Graphics2D dengan anti-aliasing aktif.
     * desktopTextHints = true memakai pengaturan teks Windows (ClearType);
     * false memakai grayscale (aman untuk jendela transparan seperti splash).
     */
    public static Graphics2D create(Graphics g, boolean desktopTextHints) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Object hints = desktopTextHints
                ? Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")
                : null;
        if (hints instanceof Map<?, ?> map) {
            g2.addRenderingHints(map);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        return g2;
    }

    /** Menggambar teks yang dibungkus per kata. Mengembalikan baseline baris terakhir. */
    public static int drawWrapped(Graphics2D g, String text, int x, int baseline,
                                  int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int y = baseline;
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, y);
        }
        return y;
    }
}
