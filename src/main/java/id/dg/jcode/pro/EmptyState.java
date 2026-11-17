package id.dg.jcode.pro;

import id.dg.jcode.AppInfo;
import id.dg.jcode.ui.icons.AppLogo;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.Palette;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.util.Gfx;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.util.List;

/** Tampilan saat belum ada file terbuka: logo + "DG J-Code" + tautan cepat. */
public final class EmptyState extends JPanel {

    private final JLabel name = new JLabel(AppInfo.NAME);
    private final JButton newProject;
    private final JButton openProject;
    private final JButton newFile;

    public EmptyState(Runnable onNewProject, Runnable onOpenProject, Runnable onNewFile) {
        super(new GridBagLayout());
        JComponent logo = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(96, 96);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Gfx.create(g, false);
                AppLogo.paint(g2, 0, 0, 96, ThemeManager.palette().accent(), Color.WHITE);
                g2.dispose();
            }
        };
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(logo);
        left.add(Box.createVerticalStrut(8));
        left.add(name);

        newProject = link("Proyek Baru", onNewProject);
        openProject = link("Buka Proyek", onOpenProject);
        newFile = link("File Baru", onNewFile);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(newProject);
        right.add(openProject);
        right.add(newFile);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setMaximumSize(new Dimension(1, 96));
        sep.setPreferredSize(new Dimension(1, 96));

        Box row = Box.createHorizontalBox();
        row.add(left);
        row.add(Box.createHorizontalStrut(32));
        row.add(sep);
        row.add(Box.createHorizontalStrut(24));
        row.add(right);
        add(row);

        refreshTheme();
        ThemeManager.addListener(this::refreshTheme);
    }

    public void setNewFileVisible(boolean visible) {
        newFile.setVisible(visible);
    }

    private static JButton link(String text, Runnable action) {
        JButton b = new JButton(text);
        b.putClientProperty("JButton.buttonType", "borderless");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void refreshTheme() {
        Palette p = ThemeManager.palette();
        name.setFont(Fonts.uiSemibold(18f));
        name.setForeground(p.text());
        for (JButton b : List.of(newProject, openProject, newFile)) {
            b.setFont(Fonts.ui(15f));
            b.setForeground(p.accent());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(ThemeManager.current().editorBg);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
