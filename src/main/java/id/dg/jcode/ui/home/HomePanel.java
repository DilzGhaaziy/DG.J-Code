package id.dg.jcode.ui.home;

import id.dg.jcode.AppInfo;
import id.dg.jcode.ui.icons.AppLogo;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.Palette;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.util.Gfx;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/** Beranda: header, dua kartu mode, "Dukung saya", dan footer. */
public final class HomePanel extends JPanel {

    private final JLabel title = new JLabel(AppInfo.NAME);
    private final JLabel subtitle = new JLabel("Selamat datang! Pilih mode untuk mulai menulis kode Java.");
    private final JLabel tip = new JLabel("Tips: gunakan Tab untuk berpindah kartu, lalu Enter untuk membuka.");
    private final JLabel footerInfo = new JLabel();
    private final JButton themeButton = new JButton();

    public HomePanel(Runnable onProfessional, Runnable onPractice, Runnable onToggleTheme) {
        super(new BorderLayout());
        setOpaque(true);

        JComponent logo = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(48, 48);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Gfx.create(g, false);
                AppLogo.paint(g2, 0, 0, 48, ThemeManager.palette().accent(), Color.WHITE);
                g2.dispose();
            }
        };
        logo.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(title);
        texts.add(Box.createVerticalStrut(4));
        texts.add(subtitle);
        texts.setAlignmentY(Component.CENTER_ALIGNMENT);

        Box header = Box.createHorizontalBox();
        header.add(logo);
        header.add(Box.createHorizontalStrut(16));
        header.add(texts);
        header.add(Box.createHorizontalGlue());
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cards = new JPanel(new GridLayout(1, 2, 24, 0));
        cards.setOpaque(false);
        cards.add(new ModeCard(ModeCard.Kind.PROFESSIONAL, "Professional",
                "Mode IDE lengkap seperti NetBeans: proyek, banyak tab, build, dan run.",
                "IDE lengkap", onProfessional));
        cards.add(new ModeCard(ModeCard.Kind.PRACTICE, "Praktek",
                "Editor sederhana untuk latihan: tulis kode, tekan Run, lalu lihat hasilnya.",
                "Untuk mahasiswa", onPractice));
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);

        tip.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(header);
        column.add(Box.createVerticalStrut(32));
        column.add(cards);
        column.add(Box.createVerticalStrut(24));
        column.add(tip);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(column);
        add(center, BorderLayout.CENTER);

        // Bagian bawah: "Dukung saya" (opsional) di atas baris versi + tombol tema.
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(new EmptyBorder(8, 24, 16, 24));

        if (AppInfo.SHOW_SUPPORT_LINKS) {
            SupportLinks support = new SupportLinks();
            support.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(support);
            south.add(Box.createVerticalStrut(8));
        }

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footerInfo.setText("Versi " + AppInfo.VERSION_FULL + "  \u00B7  Java " + Runtime.version().feature());
        themeButton.setIcon(new ThemeIcon());
        themeButton.setIconTextGap(8);
        themeButton.addActionListener(e -> onToggleTheme.run());
        footer.add(footerInfo, BorderLayout.WEST);
        footer.add(themeButton, BorderLayout.EAST);
        south.add(footer);

        add(south, BorderLayout.SOUTH);

        refreshTheme();
        ThemeManager.addListener(this::refreshTheme);
    }

    private void refreshTheme() {
        Palette p = ThemeManager.palette();
        title.setFont(Fonts.uiSemibold(24f));
        title.setForeground(p.text());
        subtitle.setFont(Fonts.ui(14f));
        subtitle.setForeground(p.textMuted());
        tip.setFont(Fonts.ui(12f));
        tip.setForeground(p.textMuted());
        footerInfo.setFont(Fonts.ui(12f));
        footerInfo.setForeground(p.textMuted());
        themeButton.setText("Tema");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(ThemeManager.palette().background());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Ikon tombol tema: bulan saat tema terang, matahari saat tema gelap. */
    private static final class ThemeIcon implements Icon {

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
                g2.setColor(c.getForeground());
                if (ThemeManager.isDark()) {
                    double cx = x + 8;
                    double cy = y + 8;
                    g2.fill(new Ellipse2D.Double(x + 5, y + 5, 6, 6));
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI / 4 * i;
                        g2.draw(new Line2D.Double(
                                cx + Math.cos(a) * 5.2, cy + Math.sin(a) * 5.2,
                                cx + Math.cos(a) * 7.0, cy + Math.sin(a) * 7.0));
                    }
                } else {
                    Area moon = new Area(new Ellipse2D.Double(x + 2, y + 2, 12, 12));
                    moon.subtract(new Area(new Ellipse2D.Double(x + 6, y, 11, 11)));
                    g2.fill(moon);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
