package id.dg.jcode.ui.home;

import id.dg.jcode.ui.icons.ModeIcons;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.Palette;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.util.Colors;
import id.dg.jcode.ui.util.Gfx;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Kartu besar di beranda dengan efek hover yang halus:
 * warna border berpindah ke aksen, bayangan muncul, dan panah "Buka" bergeser.
 * Bisa dipakai dengan mouse maupun keyboard (Tab untuk fokus, Enter/Spasi untuk membuka).
 */
public final class ModeCard extends JComponent {

    public enum Kind { PROFESSIONAL, PRACTICE }

    private static final int CARD_W = 344;
    private static final int CARD_H = 240;
    private static final int SHADOW_SPACE = 4;
    private static final int PAD = 24;
    private static final int ICON_BOX = 48;

    private static final Font TITLE_FONT = Fonts.uiSemibold(20f);
    private static final Font DESC_FONT = Fonts.ui(13f);
    private static final Font BADGE_FONT = Fonts.ui(12f);
    private static final Font CTA_FONT = Fonts.uiSemibold(13f);

    private final Kind kind;
    private final String title;
    private final String description;
    private final String badge;
    private final Runnable action;
    private final Timer animation;

    private float hover;
    private float target;
    private boolean pressed;

    public ModeCard(Kind kind, String title, String description, String badge, Runnable action) {
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.badge = badge;
        this.action = action;

        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Aman dipanggil karena getAccessibleContext() di bawah tidak pernah mengembalikan null.
        getAccessibleContext().setAccessibleName(title);
        getAccessibleContext().setAccessibleDescription(description);

        animation = new Timer(16, e -> step());

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                animateTo(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pressed = false;
                animateTo(0f);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    pressed = true;
                    requestFocusInWindow();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasPressed = pressed;
                pressed = false;
                repaint();
                if (wasPressed && contains(e.getPoint()) && SwingUtilities.isLeftMouseButton(e)) {
                    action.run();
                }
            }
        };
        addMouseListener(mouse);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });

        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
        actionMap.put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * Turunan langsung JComponent TIDAK otomatis punya AccessibleContext
     * (nilai bawaannya null). Karena itu kita membuatnya sendiri di sini.
     * Perannya PUSH_BUTTON agar pembaca layar mengenali kartu ini sebagai tombol.
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    private void animateTo(float value) {
        target = value;
        if (!animation.isRunning()) {
            animation.start();
        }
    }

    private void step() {
        float diff = target - hover;
        if (Math.abs(diff) < 0.01f) {
            hover = target;
            animation.stop();
        } else {
            hover += diff * 0.22f;
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CARD_W, CARD_H);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Gfx.create(g, true);
        try {
            Palette p = ThemeManager.palette();
            int arc = ThemeManager.ARC;
            int w = getWidth();
            float cardH = getHeight() - SHADOW_SPACE;

            // 1. Bayangan halus saat hover.
            if (hover > 0.01f) {
                int base = ThemeManager.isDark() ? 70 : 28;
                for (int i = 1; i <= 3; i++) {
                    g2.setColor(new Color(0, 0, 0, Math.round(base * hover / (i + 1))));
                    g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f + i, w - 1f, cardH - 1f, arc, arc));
                }
            }

            // 2. Latar kartu.
            RoundRectangle2D card = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, cardH - 1f, arc, arc);
            Color bg = Colors.mix(p.surface(), p.surfaceHover(), hover);
            if (pressed) {
                bg = Colors.mix(bg, p.accentSoft(), 0.6f);
            }
            g2.setColor(bg);
            g2.fill(card);

            // 3. Border: abu-abu, lalu berubah ke aksen saat hover atau fokus.
            boolean focused = isFocusOwner();
            float accentAmount = Math.max(hover, focused ? 1f : 0f);
            g2.setColor(Colors.mix(p.border(), p.accent(), accentAmount));
            g2.setStroke(new BasicStroke(focused ? 2f : 1f));
            g2.draw(card);
            g2.setStroke(new BasicStroke(1f));

            // 4. Kotak ikon.
            g2.setColor(p.accentSoft());
            g2.fill(new RoundRectangle2D.Float(PAD, PAD, ICON_BOX, ICON_BOX, arc, arc));
            if (kind == Kind.PROFESSIONAL) {
                ModeIcons.paintProfessional(g2, PAD + 8, PAD + 8, 32, p.accent());
            } else {
                ModeIcons.paintPractice(g2, PAD + 8, PAD + 8, 32, p.accent(), p.accentSoft());
            }

            // 5. Badge berbentuk pill di kanan atas.
            g2.setFont(BADGE_FONT);
            FontMetrics bfm = g2.getFontMetrics();
            int bw = bfm.stringWidth(badge) + 16;
            int bh = 24;
            int bx = w - PAD - bw;
            g2.setColor(p.border());
            g2.draw(new RoundRectangle2D.Float(bx + 0.5f, PAD + 0.5f, bw - 1f, bh - 1f, bh, bh));
            g2.setColor(p.textMuted());
            g2.drawString(badge, bx + 8, PAD + (bh - bfm.getHeight()) / 2 + bfm.getAscent());

            // 6. Judul.
            g2.setFont(TITLE_FONT);
            FontMetrics tfm = g2.getFontMetrics();
            int titleBaseline = PAD + ICON_BOX + 24 + tfm.getAscent() - 4;
            g2.setColor(p.text());
            g2.drawString(title, PAD, titleBaseline);

            // 7. Penjelasan satu kalimat (otomatis dibungkus).
            g2.setFont(DESC_FONT);
            FontMetrics dfm = g2.getFontMetrics();
            g2.setColor(p.textMuted());
            Gfx.drawWrapped(g2, description, PAD, titleBaseline + 8 + dfm.getAscent(),
                    w - PAD * 2, dfm.getHeight() + 2);

            // 8. Ajakan "Buka" dengan panah yang bergeser saat hover.
            g2.setFont(CTA_FONT);
            FontMetrics cfm = g2.getFontMetrics();
            int ctaBaseline = Math.round(cardH) - PAD;
            String cta = "Buka";
            g2.setColor(p.accent());
            g2.drawString(cta, PAD, ctaBaseline);
            float ax = PAD + cfm.stringWidth(cta) + 8 + 4 * hover;
            float ay = ctaBaseline - cfm.getAscent() / 2f + 1;
            Path2D arrow = new Path2D.Float();
            arrow.moveTo(ax, ay);
            arrow.lineTo(ax + 10, ay);
            arrow.moveTo(ax + 6, ay - 4);
            arrow.lineTo(ax + 10, ay);
            arrow.lineTo(ax + 6, ay + 4);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arrow);
        } finally {
            g2.dispose();
        }
    }
}
