package id.dg.jcode.assist;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Ikon bulat merah dengan tanda seru, untuk gutter. Digambar sendiri (orisinal). */
final class ErrorIcon implements Icon {
    static final ErrorIcon INSTANCE = new ErrorIcon();
    static final Color RED = new Color(0xE5484D);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(RED);
        g2.fillOval(x + 1, y + 1, 12, 12);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x + 6, y + 3, 2, 6, 2, 2);
        g2.fillOval(x + 6, y + 10, 2, 2);
        g2.dispose();
    }

    @Override public int getIconWidth() { return 14; }
    @Override public int getIconHeight() { return 14; }
}
