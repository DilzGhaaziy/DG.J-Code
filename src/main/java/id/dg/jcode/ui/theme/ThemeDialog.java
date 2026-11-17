package id.dg.jcode.ui.theme;

import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.util.Gfx;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Dialog pemilihan tema dengan pratinjau langsung. */
public final class ThemeDialog extends JDialog {

    private final JList<ThemeMode> list = new JList<>(ThemeMode.values());
    private final Settings settings;

    public ThemeDialog(Window owner, Settings settings) {
        super(owner, "Pilih Tema", ModalityType.APPLICATION_MODAL);
        this.settings = settings;

        Preview preview = new Preview();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(ThemeManager.current(), true);
        list.setFixedCellHeight(36);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.addListSelectionListener(e -> preview.repaint());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    apply();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(180, 280));

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        center.add(listScroll, BorderLayout.WEST);
        center.add(preview, BorderLayout.CENTER);

        JButton cancel = new JButton("Batal");
        JButton ok = new JButton("Terapkan");
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> apply());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        buttons.add(cancel);
        buttons.add(ok);

        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void apply() {
        ThemeMode mode = list.getSelectedValue();
        if (mode == null) {
            return;
        }
        settings.setTheme(mode);
        settings.save();
        dispose();
        ThemeManager.switchTo(mode);
    }

    private record Seg(Color color, String text) {
    }

    private final class Preview extends JComponent {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(460, 280);
        }

        @Override
        protected void paintComponent(Graphics g) {
            ThemeMode m = list.getSelectedValue();
            if (m == null) {
                return;
            }
            Graphics2D g2 = Gfx.create(g, true);
            try {
                Palette p = m.palette;
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1f, h - 1f, ThemeManager.ARC, ThemeManager.ARC);
                g2.clip(shape);
                g2.setColor(p.background());
                g2.fillRect(0, 0, w, h);

                // Toolbar
                g2.setColor(p.surface());
                g2.fillRect(0, 0, w, 28);
                g2.setFont(Fonts.ui(11f));
                g2.setColor(p.textMuted());
                g2.drawString("DG J-Code  \u00B7  " + m.label, 12, 18);
                g2.setColor(p.accent());
                g2.fillRoundRect(w - 60, 7, 48, 14, 14, 14);

                // Panel Projects
                int side = 120;
                int outTop = h - 64;
                g2.setColor(p.surface());
                g2.fillRect(0, 28, side, outTop - 28);
                g2.setColor(p.text());
                g2.drawString("\u25BE HaloDunia", 10, 50);
                g2.drawString("    \u25BE src", 10, 68);
                g2.setColor(p.accent());
                g2.drawString("        Main", 10, 86);

                // Editor
                g2.setColor(m.editorBg);
                g2.fillRect(side, 28, w - side, outTop - 28);
                g2.setColor(m.gutterBg);
                g2.fillRect(side, 28, 30, outTop - 28);
                Font code = Fonts.code(12f);
                g2.setFont(code);
                FontMetrics fm = g2.getFontMetrics();
                int lh = fm.getHeight() + 2;
                Seg[][] lines = {
                        {new Seg(m.keyword, "public class "), new Seg(m.type, "Main"), new Seg(m.editorText, " {")},
                        {new Seg(m.comment, "    // Program pertama")},
                        {new Seg(m.keyword, "    public static void "), new Seg(m.editorText, "main("),
                                new Seg(m.type, "String"), new Seg(m.editorText, "[] args) {")},
                        {new Seg(m.editorText, "        "), new Seg(m.type, "System"),
                                new Seg(m.editorText, ".out.println("), new Seg(m.string, "\"Halo!\""),
                                new Seg(m.editorText, ");")},
                        {new Seg(m.keyword, "        int "), new Seg(m.editorText, "nilai = "),
                                new Seg(m.number, "42"), new Seg(m.editorText, ";")},
                        {new Seg(m.editorText, "    }")},
                        {new Seg(m.editorText, "}")}
                };
                int y = 34;
                for (int i = 0; i < lines.length; i++) {
                    if (i == 3) {
                        g2.setColor(m.currentLine);
                        g2.fillRect(side + 30, y, w - side - 30, lh);
                    }
                    int base = y + fm.getAscent() + 1;
                    g2.setColor(m.lineNumber);
                    g2.drawString(String.valueOf(i + 1), side + 10, base);
                    int x = side + 38;
                    for (Seg s : lines[i]) {
                        g2.setColor(s.color());
                        g2.drawString(s.text(), x, base);
                        x += fm.stringWidth(s.text());
                    }
                    y += lh;
                }

                // Output
                g2.setColor(p.border());
                g2.fillRect(0, outTop, w, 1);
                g2.setColor(m.editorBg);
                g2.fillRect(0, outTop + 1, w, h - outTop);
                g2.setFont(Fonts.ui(11f));
                g2.setColor(p.textMuted());
                g2.drawString("Output", 12, outTop + 18);
                g2.setFont(code);
                g2.setColor(m.editorText);
                g2.drawString("Halo!", 12, outTop + 40);

                g2.setClip(null);
                g2.setColor(p.border());
                g2.draw(shape);
            } finally {
                g2.dispose();
            }
        }
    }
}
