package id.dg.jcode.ui.icons;

import id.dg.jcode.ui.util.Gfx;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Ikon garis 16 px orisinal untuk toolbar. Mengikuti warna teks tema. */
public final class LineIcon implements Icon {

    public enum Kind { HOME, NEW_PROJECT, OPEN, SAVE, UNDO, REDO, BUILD, RUN, STOP }

    private static final Color GREEN = new Color(0x2DA44E);
    private static final Color RED = new Color(0xCF222E);

    private final Kind kind;

    public LineIcon(Kind kind) {
        this.kind = kind;
    }

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Gfx.create(g, false);
        try {
            g2.translate(x, y);
            Color fg = c.isEnabled() ? c.getForeground() : UIManager.getColor("Label.disabledForeground");
            if (fg == null) {
                fg = Color.GRAY;
            }
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case HOME -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(2, 8);
                    p.lineTo(8, 2.5);
                    p.lineTo(14, 8);
                    g2.draw(p);
                    g2.draw(new Rectangle2D.Float(4, 7.5f, 8, 6.5f));
                }
                case NEW_PROJECT -> {
                    g2.draw(new RoundRectangle2D.Float(2, 2, 12, 12, 4, 4));
                    g2.draw(new Line2D.Float(8, 5, 8, 11));
                    g2.draw(new Line2D.Float(5, 8, 11, 8));
                }
                case OPEN -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(1.5, 4);
                    p.lineTo(6, 4);
                    p.lineTo(7.5, 5.5);
                    p.lineTo(14.5, 5.5);
                    p.lineTo(14.5, 13);
                    p.lineTo(1.5, 13);
                    p.closePath();
                    g2.draw(p);
                }
                case SAVE -> {
                    g2.draw(new RoundRectangle2D.Float(2, 2, 12, 12, 3, 3));
                    g2.draw(new Rectangle2D.Float(5, 2, 6, 4));
                    g2.draw(new Rectangle2D.Float(5, 9, 6, 5));
                }
                case UNDO, REDO -> {
                    if (kind == Kind.REDO) {
                        g2.translate(16, 0);
                        g2.scale(-1, 1);
                    }
                    Path2D p = new Path2D.Float();
                    p.moveTo(5, 4);
                    p.lineTo(2, 7);
                    p.lineTo(5, 10);
                    p.moveTo(2, 7);
                    p.lineTo(10, 7);
                    p.quadTo(14, 7, 14, 10.5);
                    p.quadTo(14, 14, 10, 14);
                    p.lineTo(7, 14);
                    g2.draw(p);
                }
                case BUILD -> {
                    g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, 11, 11, 3, 3));
                    g2.draw(new Line2D.Float(2.5f, 6.5f, 13.5f, 6.5f));
                    g2.draw(new Line2D.Float(6.5f, 6.5f, 6.5f, 13.5f));
                }
                case RUN -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(4, 2.5);
                    p.lineTo(13.5, 8);
                    p.lineTo(4, 13.5);
                    p.closePath();
                    g2.setColor(c.isEnabled() ? GREEN : fg);
                    g2.fill(p);
                }
                case STOP -> {
                    g2.setColor(c.isEnabled() ? RED : fg);
                    g2.fill(new RoundRectangle2D.Float(3, 3, 10, 10, 3, 3));
                }
                default -> {
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
