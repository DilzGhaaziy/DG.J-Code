package id.dg.jcode.pro;

import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.theme.ThemeMode;
import id.dg.jcode.ui.util.Colors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/** Panel Output: teks berwarna kontras sesuai tema + kolom input untuk Scanner. */
public final class OutputPanel extends JPanel {

    private final JTextPane text = new JTextPane();
    private final JLabel title = new JLabel("Output");
    private final JTextField input = new JTextField();
    private final JButton send = new JButton("Kirim");
    private final JScrollPane scroll;

    public OutputPanel(Runnable onHide, Consumer<String> onInput) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 200));
        text.setEditable(false);

        JButton clear = new JButton("Bersihkan");
        JButton hide = new JButton("Sembunyikan");
        clear.putClientProperty("JButton.buttonType", "borderless");
        hide.putClientProperty("JButton.buttonType", "borderless");
        clear.addActionListener(e -> clear());
        hide.addActionListener(e -> onHide.run());

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        title.setFont(Fonts.uiSemibold(12f));
        header.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(clear);
        actions.add(hide);
        header.add(actions, BorderLayout.EAST);

        scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));

        input.putClientProperty("JTextField.placeholderText",
                "Ketik input untuk program (Scanner) lalu tekan Enter...");
        ActionListener submit = e -> {
            String s = input.getText();
            input.setText("");
            onInput.accept(s);
        };
        input.addActionListener(submit);
        send.addActionListener(submit);
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(send, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(inputRow, BorderLayout.SOUTH);
        setRunning(false);
        refreshColors();
        ThemeManager.addListener(this::refreshColors);
    }

    private void refreshColors() {
        ThemeMode m = ThemeManager.current();
        text.setBackground(m.editorBg);
        text.setForeground(m.editorText);
        text.setCaretColor(m.editorText);
        text.setFont(Fonts.code(13f));
        scroll.getViewport().setBackground(m.editorBg);
        text.repaint();
    }

    public void setTitle(String value) {
        onEdt(() -> title.setText(value));
    }

    public void setRunning(boolean running) {
        onEdt(() -> {
            input.setEnabled(running);
            send.setEnabled(running);
            if (running) {
                input.requestFocusInWindow();
            }
        });
    }

    public void clear() {
        onEdt(() -> text.setText(""));
    }

    /** Output program biasa: memakai warna teks tema (kontras dengan latar). */
    public void print(String s) {
        append(s, null);
    }

    public void printErr(String s) {
        append(s, red());
    }

    public void info(String s) {
        ThemeMode m = ThemeManager.current();
        append(s + "\n", Colors.mix(m.editorText, m.editorBg, 0.35f));
    }

    public void success(String s) {
        append(s + "\n", ThemeManager.isDark() ? new Color(0x56D364) : new Color(0x1A7F37));
    }

    public void error(String s) {
        append(s + "\n", red());
    }

    public void echoInput(String s) {
        append(s + "\n", ThemeManager.palette().accent());
    }

    private static Color red() {
        return ThemeManager.isDark() ? new Color(0xFF7B72) : new Color(0xCF222E);
    }

    private void append(String s, Color color) {
        String clean = s.replace("\r", "");
        onEdt(() -> {
            StyledDocument doc = text.getStyledDocument();
            SimpleAttributeSet attr = new SimpleAttributeSet();
            if (color != null) {
                StyleConstants.setForeground(attr, color);
            }
            try {
                doc.insertString(doc.getLength(), clean, attr);
                text.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {
                // posisi selalu di akhir dokumen
            }
        });
    }

    private static void onEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}
