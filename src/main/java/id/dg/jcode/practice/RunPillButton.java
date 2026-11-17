package id.dg.jcode.practice;

import javax.swing.JButton;
import java.awt.*;
import java.awt.geom.Path2D;

/** Tombol pill hijau dengan ikon play (digambar sendiri). */
final class RunPillButton extends JButton {
    private static final Color GREEN = new Color(0x1F9D55);
    private static final Color GREEN_HOVER = new Color(0x1A8A4A);

    RunPillButton(String text) {
        super(text);
        setForeground(Color.WHITE);
        setFont(getFont().deriveFont(Font.BOLD, 13f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
        setRolloverEnabled(true);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(16 + 12 + 8 + fm.stringWidth(getText()) + 16, 32);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color bg = !isEnabled() ? new Color(0x9CC9AE) : getModel().isRollover() ? GREEN_HOVER : GREEN;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); // pill
        int cy = getHeight() / 2;
        Path2D play = new Path2D.Float();
        play.moveTo(16, cy - 6);
        play.lineTo(27, cy);
        play.lineTo(16, cy + 6);
        play.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(play);
        FontMetrics fm = g2.getFontMetrics(getFont());
        g2.setFont(getFont());
        g2.drawString(getText(), 16 + 12 + 8, cy + (fm.getAscent() - fm.getDescent()) / 2);
        g2.dispose();
    }
}
