package id.dg.jcode.assist;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Kotak saran kecil di bawah baris yang salah:
 *   baris 1 = pesan (merah), baris 2 = kode perbaikan (abu-abu miring / "ghost text"),
 *   baris 3 = petunjuk tombol.
 */
final class SuggestionBox extends JComponent {
    private final JLabel message = label();
    private final JLabel code = label();
    private final JLabel hint = label();

    SuggestionBox(Runnable onAccept) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setOpaque(false);
        add(message);
        add(Box.createVerticalStrut(4));
        add(code);
        add(Box.createVerticalStrut(4));
        add(hint);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                e.consume();
                onAccept.run(); // klik = terima saran
            }
        });
    }

    private static JLabel label() {
        JLabel l = new JLabel();
        l.putClientProperty("html.disable", Boolean.TRUE); // "List<String>" jangan dianggap HTML
        return l;
    }

    void setContent(Problem p, Tone tone, Font codeFont, boolean dark) {
        Font ui = UIManager.getFont("Label.font");
        message.setFont(ui.deriveFont(Font.PLAIN, 12f));
        message.setForeground(dark ? new Color(0xFF8A8E) : new Color(0xC62F35));
        message.setText(p.message());

        code.setVisible(p.hasFix());
        if (p.hasFix()) {
            code.setFont(codeFont.deriveFont(Font.ITALIC));
            code.setForeground(dark ? new Color(0x8B929C) : new Color(0x7A808A));
            code.setText(p.fix().preview());
        }

        hint.setFont(ui.deriveFont(Font.PLAIN, 11f));
        hint.setForeground(dark ? new Color(0x9AA1AB) : new Color(0x5F6670));
        if (!p.hasFix()) {
            hint.setText("Esc untuk menutup");
        } else if (tone == Tone.FRIENDLY) {
            hint.setText("Tekan Tab (atau klik) untuk memakai saran · Esc untuk menutup");
        } else {
            hint.setText("Tab / klik: terapkan  ·  Esc: tolak");
        }
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = UIManager.getColor("Panel.background");
        Color border = UIManager.getColor("Component.borderColor");
        g2.setColor(bg != null ? bg : Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16); // sudut 8 px
        g2.setColor(border != null ? border : Color.GRAY);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.setColor(ErrorIcon.RED);
        g2.fillRoundRect(0, 8, 3, getHeight() - 16, 3, 3); // aksen tipis di kiri
        g2.dispose();
    }
}
