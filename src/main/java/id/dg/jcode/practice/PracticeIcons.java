package id.dg.jcode.practice;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Ikon tombol Praktek.
 *
 * IKON CUSTOM: letakkan file PNG transparan di
 *     src/main/resources/icons/practice/save.png
 *     src/main/resources/icons/practice/save-as.png
 * Disarankan ukuran 32x32 px (ditampilkan 16x16, tetap tajam di layar 200%).
 * Kalau file tidak ada, ikon bawaan (digambar kode) yang dipakai.
 */
final class PracticeIcons {
    static final int SIZE = 16;
    static final String CUSTOM_DIR = "/icons/practice/";

    private PracticeIcons() { }

    static Icon save() { return load("save.png", new SaveIcon(false)); }
    static Icon saveAs() { return load("save-as.png", new SaveIcon(true)); }

    private static Icon load(String file, Icon fallback) {
        try (InputStream in = PracticeIcons.class.getResourceAsStream(CUSTOM_DIR + file)) {
            if (in == null) return fallback;
            BufferedImage img = ImageIO.read(in);
            return img == null ? fallback : new ScaledIcon(img);
        } catch (IOException e) {
            return fallback;
        }
    }

    /** Gambar custom yang selalu digambar 16x16 dengan interpolasi halus. */
    private record ScaledIcon(BufferedImage img) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(img, x, y, SIZE, SIZE, null);
            g2.dispose();
        }
        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }

    /** Ikon disket bawaan. Warnanya mengikuti tema (dibaca saat digambar). */
    private static final class SaveIcon implements Icon {
        private final boolean saveAs;

        SaveIcon(boolean saveAs) { this.saveAs = saveAs; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);

            Color fg = UIManager.getColor(c != null && !c.isEnabled() ? "Label.disabledForeground" : "Label.foreground");
            g2.setColor(fg != null ? fg : Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D body = new Path2D.Float();           // badan disket, sudut kanan atas terpotong
            body.moveTo(2.5, 2.5);
            body.lineTo(11, 2.5);
            body.lineTo(13.5, 5);
            body.lineTo(13.5, 13.5);
            body.lineTo(2.5, 13.5);
            body.closePath();
            g2.draw(body);
            g2.draw(new Rectangle2D.Float(5f, 2.5f, 5f, 3.5f));   // penutup logam
            g2.draw(new Rectangle2D.Float(4.5f, 8.5f, 7f, 5f));   // label

            if (saveAs) {                                // tanda + biru di pojok kanan bawah
                Color bg = UIManager.getColor("Button.background");
                Color accent = UIManager.getColor("Component.accentColor");
                g2.setColor(bg != null ? bg : Color.WHITE);
                g2.fill(new Ellipse2D.Float(8f, 8f, 8f, 8f));
                g2.setColor(accent != null ? accent : new Color(0x2F6FEB));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(12f, 9.5f, 12f, 14.5f));
                g2.draw(new Line2D.Float(9.5f, 12f, 14.5f, 12f));
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
}
