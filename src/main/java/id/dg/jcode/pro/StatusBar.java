package id.dg.jcode.pro;

import id.dg.jcode.ui.theme.Fonts;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Status bar paling bawah: file, bar proses Run, dan posisi kursor. */
public final class StatusBar extends JPanel {

    private final JLabel left = new JLabel(" ");
    private final JLabel right = new JLabel(" ");
    private final JLabel progressLabel = new JLabel();
    private final JProgressBar progress = new JProgressBar();
    private final JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)));
        left.setFont(Fonts.ui(12f));
        right.setFont(Fonts.ui(12f));
        progressLabel.setFont(Fonts.ui(12f));
        progress.setPreferredSize(new Dimension(180, 8));
        progressPanel.setOpaque(false);
        progressPanel.add(progressLabel);
        progressPanel.add(progress);
        progressPanel.setVisible(false);
        add(left, BorderLayout.WEST);
        add(progressPanel, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void update(EditorTab tab) {
        if (tab == null) {
            left.setText("Tidak ada file yang dibuka");
            right.setText(" ");
            return;
        }
        String project = tab.project() != null ? tab.project().name() + "  \u00B7  " : "";
        left.setText(project + tab.title() + (tab.isDirty() ? "  (belum disimpan)" : ""));
        right.setText("   Baris " + tab.caretLine() + ", Kolom " + tab.caretColumn() + "   \u00B7   UTF-8");
    }

    public void startProgress(String text) {
        SwingUtilities.invokeLater(() -> {
            progressLabel.setText(text);
            progress.setIndeterminate(true);
            progressPanel.setVisible(true);
            revalidate();
        });
    }

    public void stopProgress() {
        SwingUtilities.invokeLater(() -> {
            progress.setIndeterminate(false);
            progressPanel.setVisible(false);
            revalidate();
        });
    }
}
