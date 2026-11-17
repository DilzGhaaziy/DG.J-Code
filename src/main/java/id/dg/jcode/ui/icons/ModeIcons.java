package id.dg.jcode.ui.icons;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Ikon garis sederhana dan orisinal untuk kartu beranda. */
public final class ModeIcons {

    private ModeIcons() {
    }

    /** Ikon tata letak IDE: jendela dengan panel kiri dan panel bawah. */
    public static void paintProfessional(Graphics2D g, double x, double y, double s, Color c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(x, y);
            g2.setColor(c);
            g2.setStroke(new BasicStroke((float) (s * 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Double(s * 0.10, s * 0.16, s * 0.80, s * 0.68, s * 0.18, s * 0.18));
            g2.draw(new Line2D.Double(s * 0.10, s * 0.32, s * 0.90, s * 0.32));
            g2.draw(new Line2D.Double(s * 0.36, s * 0.32, s * 0.36, s * 0.84));
            g2.draw(new Line2D.Double(s * 0.36, s * 0.64, s * 0.90, s * 0.64));
        } finally {
            g2.dispose();
        }
    }

    /** Ikon lembar latihan dengan lencana play. cut = warna latar di belakang ikon. */
    public static void paintPractice(Graphics2D g, double x, double y, double s, Color c, Color cut) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(x, y);
            g2.setColor(c);
            g2.setStroke(new BasicStroke((float) (s * 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Double(s * 0.14, s * 0.10, s * 0.54, s * 0.74, s * 0.16, s * 0.16));
            g2.draw(new Line2D.Double(s * 0.28, s * 0.30, s * 0.54, s * 0.30));
            g2.draw(new Line2D.Double(s * 0.28, s * 0.44, s * 0.54, s * 0.44));
            g2.draw(new Line2D.Double(s * 0.28, s * 0.58, s * 0.42, s * 0.58));

            g2.setColor(cut);
            g2.fill(new Ellipse2D.Double(s * 0.48, s * 0.50, s * 0.48, s * 0.48));
            g2.setColor(c);
            g2.fill(new Ellipse2D.Double(s * 0.53, s * 0.55, s * 0.38, s * 0.38));

            Path2D play = new Path2D.Double();
            play.moveTo(s * 0.67, s * 0.65);
            play.lineTo(s * 0.67, s * 0.83);
            play.lineTo(s * 0.82, s * 0.74);
            play.closePath();
            g2.setColor(cut);
            g2.fill(play);
        } finally {
            g2.dispose();
        }
    }
}
