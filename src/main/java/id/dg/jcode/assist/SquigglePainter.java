package id.dg.jcode.assist;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;

/** Menggambar garis merah bergelombang di bawah teks yang salah (selalu dalam satu baris). */
final class SquigglePainter implements Highlighter.HighlightPainter {
    private final Color color;

    SquigglePainter(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        try {
            Rectangle a = c.modelToView2D(p0).getBounds();
            Rectangle b = c.modelToView2D(p1).getBounds();
            int x0 = a.x;
            int x1 = (b.y == a.y) ? Math.max(b.x, a.x + 6) : a.x + 6;
            int y = a.y + a.height - 2;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.1f));
            Path2D wave = new Path2D.Float();
            wave.moveTo(x0, y);
            boolean up = true;
            for (int x = x0; x < x1; x += 2) {
                wave.lineTo(x + 2, up ? y - 2 : y);
                up = !up;
            }
            g2.draw(wave);
            g2.dispose();
        } catch (BadLocationException ignored) {
            // teks sudah berubah; akan digambar ulang setelah pemeriksaan berikutnya
        }
    }
}
