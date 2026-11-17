package id.dg.jcode.ui.icons;

import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.util.Gfx;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;

/** Ikon pohon proyek: memakai /icons/tree/<nama>.png jika ada, jika tidak digambar sendiri. */
public final class TreeIcons implements Icon {

    public enum Kind {
        PROJECT("proyek"), SOURCE("source"), PACKAGE("paket"), CLASS("class"), FILE("file");

        final String resource;

        Kind(String resource) {
            this.resource = resource;
        }
    }

    private static final Map<Kind, TreeIcons> CACHE = new EnumMap<>(Kind.class);
    private final Kind kind;

    private TreeIcons(Kind kind) {
        this.kind = kind;
    }

    public static synchronized Icon of(Kind kind) {
        return CACHE.computeIfAbsent(kind, TreeIcons::new);
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
            if (ImageResources.draw(g2, "/icons/tree/" + kind.resource + ".png", x, y, 16)) {
                return;
            }
            g2.translate(x, y);
            switch (kind) {
                case PROJECT -> {
                    g2.setColor(new Color(0x2F6FEB));
                    g2.fill(new RoundRectangle2D.Float(1, 2, 14, 12, 4, 4));
                    g2.setColor(Color.WHITE);
                    g2.fillRect(4, 6, 8, 2);
                    g2.fillRect(4, 9, 5, 2);
                }
                case SOURCE -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(1, 3);
                    p.lineTo(6, 3);
                    p.lineTo(7.5, 4.5);
                    p.lineTo(15, 4.5);
                    p.lineTo(15, 13.5);
                    p.lineTo(1, 13.5);
                    p.closePath();
                    g2.setColor(new Color(0xE3A008));
                    g2.fill(p);
                }
                case PACKAGE -> {
                    g2.setColor(new Color(0xC9852B));
                    g2.fill(new RoundRectangle2D.Float(2, 3, 12, 11, 3, 3));
                    g2.setColor(new Color(255, 255, 255, 170));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawLine(2, 7, 14, 7);
                    g2.drawLine(8, 3, 8, 7);
                }
                case CLASS -> {
                    g2.setColor(new Color(0x2DA44E));
                    g2.fill(new Ellipse2D.Float(1, 1, 14, 14));
                    g2.setColor(Color.WHITE);
                    g2.setFont(Fonts.uiBold(10f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("C", 8 - fm.stringWidth("C") / 2f, 8 + fm.getAscent() / 2f - 1.5f);
                }
                default -> {
                    g2.setColor(Color.GRAY);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(3, 1.5f, 10, 13, 3, 3));
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
