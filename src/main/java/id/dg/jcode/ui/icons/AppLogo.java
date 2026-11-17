package id.dg.jcode.ui.icons;

import id.dg.jcode.ui.theme.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Logo aplikasi.
 * Jika file src/main/resources/icons/app-logo.png ada, logo kustom itu yang dipakai
 * di semua tempat (splash, beranda, ikon jendela, taskbar, dan ikon installer).
 * Jika tidak ada, dipakai logo cadangan "J_" yang digambar dengan Java2D.
 */
public final class AppLogo {

    public static final String RESOURCE = "/icons/app-logo.png";

    private static List<Image> windowIcons;

    private AppLogo() {
    }

    public static boolean hasCustomLogo() {
        return ImageResources.exists(RESOURCE);
    }

    /** bg dan fg hanya dipakai oleh logo cadangan. */
    public static void paint(Graphics2D g, double x, double y, double s, Color bg, Color fg) {
        if (ImageResources.draw(g, RESOURCE, x, y, s)) {
            return;
        }
        paintFallback(g, x, y, s, bg, fg);
    }

    public static BufferedImage image(int size) {
        BufferedImage custom = ImageResources.scaled(RESOURCE, size);
        if (custom != null) {
            return custom;
        }
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        paintFallback(g, 0, 0, size, Palette.LIGHT.accent(), Color.WHITE);
        g.dispose();
        return img;
    }

    /** Ikon jendela dalam beberapa ukuran agar tajam di taskbar dan HiDPI. */
    public static synchronized List<Image> windowIcons() {
        if (windowIcons == null) {
            windowIcons = List.<Image>of(image(16), image(20), image(24), image(32),
                    image(48), image(64), image(128), image(256));
        }
        return windowIcons;
    }

    private static void paintFallback(Graphics2D g, double x, double y, double s, Color bg, Color fg) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double(0, 0, s, s, s * 0.32, s * 0.32));
            g2.setColor(fg);
            g2.setStroke(new BasicStroke((float) (s * 0.11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D j = new Path2D.Double();
            j.moveTo(s * 0.38, s * 0.27);
            j.lineTo(s * 0.62, s * 0.27);
            j.moveTo(s * 0.55, s * 0.27);
            j.lineTo(s * 0.55, s * 0.57);
            j.quadTo(s * 0.55, s * 0.74, s * 0.40, s * 0.74);
            j.quadTo(s * 0.29, s * 0.74, s * 0.26, s * 0.63);
            g2.draw(j);
            g2.draw(new Line2D.Double(s * 0.69, s * 0.74, s * 0.77, s * 0.74));
        } finally {
            g2.dispose();
        }
    }
}
