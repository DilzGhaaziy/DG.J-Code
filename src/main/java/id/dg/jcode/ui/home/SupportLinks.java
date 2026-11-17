package id.dg.jcode.ui.home;

import id.dg.jcode.AppInfo;
import id.dg.jcode.ui.icons.SocialIcon;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.util.Browser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * Bagian "Dukung saya" di pojok kiri bawah beranda.
 * Aktif/nonaktif diatur lewat AppInfo.SHOW_SUPPORT_LINKS.
 */
public final class SupportLinks extends JPanel {

    private final JLabel heading = new JLabel("Dukung saya :)");

    public SupportLinks() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(heading);
        add(Box.createVerticalStrut(4));

        JButton youtube = link(SocialIcon.Kind.YOUTUBE, "Dilz Ghaaziy \u4E61", AppInfo.YOUTUBE_URL);
        youtube.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(youtube);

        Box row = Box.createHorizontalBox();
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(link(SocialIcon.Kind.TIKTOK, "Dilz Ghaaziy", AppInfo.TIKTOK_URL));
        row.add(Box.createHorizontalStrut(8));
        row.add(link(SocialIcon.Kind.SAWERIA, "Donasi", AppInfo.SAWERIA_URL));
        add(row);

        refreshTheme();
        ThemeManager.addListener(this::refreshTheme);
    }

    private JButton link(SocialIcon.Kind kind, String text, String url) {
        JButton b = new JButton(text, new SocialIcon(kind));
        b.putClientProperty("JButton.buttonType", "borderless");
        b.setFont(Fonts.forText(text, Font.PLAIN, 13f));
        b.setIconTextGap(8);
        // Margin kiri 0 agar ikon sejajar dengan teks "Dukung saya".
        b.setMargin(new Insets(4, 0, 4, 8));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(url);
        b.addActionListener(e -> Browser.open(b, url));
        return b;
    }

    private void refreshTheme() {
        heading.setFont(Fonts.ui(12f));
        heading.setForeground(ThemeManager.palette().textMuted());
    }
}
