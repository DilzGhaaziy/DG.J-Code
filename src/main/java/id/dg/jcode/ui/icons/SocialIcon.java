package id.dg.jcode.ui.icons;

import id.dg.jcode.ui.util.Gfx;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Ikon kecil 16 px untuk tautan media sosial.
 * Jika ada PNG kustom di /icons/social/<nama>.png, PNG itu yang dipakai.
 * Jika tidak, dipakai ikon sederhana yang digambar sendiri (bukan salinan logo resmi).
 */
public final class SocialIcon implements Icon {

    public enum Kind { YOUTUBE, TIKTOK, SAWERIA }

    private static final int SIZE = 16;
    private final Kind kind;

    public SocialIcon(Kind kind) {
        this.kind = kind;
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Gfx.create(g, false);
        try {
            String custom = "/icons/social/" + kind.name().toLowerCase() + ".png";
            if (ImageResources.draw(g2, custom, x, y, SIZE)) {
                return;
            }
            g2.translate(x, y);
            switch (kind) {
                case YOUTUBE -> {
                    g2.setColor(new Color(0xE62117));
                    g2.fill(new RoundRectangle2D.Float(0, 3, 16, 11, 6, 6));
                    Path2D play = new Path2D.Float();
                    play.moveTo(6.5, 5.8);
                    play.lineTo(6.5, 11.2);
                    play.lineTo(11, 8.5);
                    play.closePath();
                    g2.setColor(Color.WHITE);
                    g2.fill(play);
                }
                case TIKTOK -> {
                    g2.setColor(c.getForeground());
                    g2.fill(new Ellipse2D.Float(2, 9, 6.5f, 6.5f));
                    g2.fill(new RoundRectangle2D.Float(7, 1, 2.2f, 12, 2, 2));
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D flag = new Path2D.Float();
                    flag.moveTo(8.5, 1.8);
                    flag.quadTo(9.5, 5, 13.5, 5.2);
                    g2.draw(flag);
                }
                case SAWERIA -> {
                    g2.setColor(new Color(0xF08C00));
                    Path2D heart = new Path2D.Float();
                    heart.moveTo(8, 14);
                    heart.curveTo(1, 9.5, 0.5, 5, 3.5, 3);
                    heart.curveTo(5.5, 1.8, 7.3, 2.8, 8, 4.5);
                    heart.curveTo(8.7, 2.8, 10.5, 1.8, 12.5, 3);
                    heart.curveTo(15.5, 5, 15, 9.5, 8, 14);
                    heart.closePath();
                    g2.fill(heart);
                }
                default -> {
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
