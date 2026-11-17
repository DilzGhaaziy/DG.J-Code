package id.dg.jcode.ui;

import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.Palette;
import id.dg.jcode.ui.theme.ThemeManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;

/** Halaman sementara untuk mode yang belum dibuat (diganti di tahap berikutnya). */
public final class PlaceholderPanel extends JPanel {

    private final JLabel title;
    private final JLabel message;

    public PlaceholderPanel(String titleText, String messageText, Runnable onBack) {
        super(new GridBagLayout());
        setOpaque(true);
        title = new JLabel(titleText);
        message = new JLabel(messageText);
        JButton back = new JButton("Kembali ke Beranda");
        back.addActionListener(e -> onBack.run());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[] {title, message, back}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        column.add(title);
        column.add(Box.createVerticalStrut(8));
        column.add(message);
        column.add(Box.createVerticalStrut(24));
        column.add(back);
        add(column);

        refreshTheme();
        ThemeManager.addListener(this::refreshTheme);
    }

    private void refreshTheme() {
        Palette p = ThemeManager.palette();
        title.setFont(Fonts.uiSemibold(22f));
        title.setForeground(p.text());
        message.setFont(Fonts.ui(14f));
        message.setForeground(p.textMuted());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(ThemeManager.palette().background());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
