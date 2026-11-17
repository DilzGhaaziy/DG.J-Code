package id.dg.jcode.practice;

import id.dg.jcode.theme.EditorTheme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Panel output: println kuning, error merah, kolom input Scanner, tombol Bersihkan. Ikut tema. */
final class PracticeOutput extends JPanel {
    enum Kind { NORMAL, ERROR, INFO, INPUT }

    private record Chunk(String text, Kind kind) { }

    private static final int MAX_CHARS = 500_000;

    private final JTextPane pane = new JTextPane();
    private final JScrollPane scroll = new JScrollPane(pane);
    private final JTextField input = new JTextField();
    private final JButton send = new JButton("Kirim");
    private final List<Chunk> chunks = new ArrayList<>();
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
    private final SimpleAttributeSet error = new SimpleAttributeSet();
    private final SimpleAttributeSet info = new SimpleAttributeSet();
    private final SimpleAttributeSet typed = new SimpleAttributeSet();
    private int totalChars;

    PracticeOutput(Font mono, Consumer<String> onInput) {
        super(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel title = new JLabel("Output");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        JButton clear = new JButton("Bersihkan Output");
        clear.addActionListener(e -> clear());
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(title, BorderLayout.WEST);
        head.add(clear, BorderLayout.EAST);

        pane.setEditable(false);
        pane.setFont(mono);

        input.setFont(mono);
        input.putClientProperty("JTextField.placeholderText", "Ketik input untuk program (Scanner), lalu Enter");
        Runnable doSend = () -> {
            String s = input.getText();
            input.setText("");
            append(s + "\n", Kind.INPUT);
            onInput.accept(s);
        };
        input.addActionListener(e -> doSend.run());
        send.addActionListener(e -> doSend.run());
        JPanel in = new JPanel(new BorderLayout(8, 0));
        in.setOpaque(false);
        in.add(new JLabel("Input:"), BorderLayout.WEST);
        in.add(input, BorderLayout.CENTER);
        in.add(send, BorderLayout.EAST);
        setInputEnabled(false);

        add(head, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(in, BorderLayout.SOUTH);
        restyle();
    }

    /** Dipanggil FlatLaf setiap kali tema diganti. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (pane != null) SwingUtilities.invokeLater(this::restyle); // pane masih null saat konstruktor super
    }

    void restyle() {
        boolean dark = EditorTheme.isDark();
        StyleConstants.setForeground(normal, dark ? new Color(0xFFE066) : new Color(0x8A6500)); // println = kuning
        StyleConstants.setForeground(error, dark ? new Color(0xFF8A8E) : new Color(0xC62F35));
        StyleConstants.setForeground(info, dark ? new Color(0x9AA1AB) : new Color(0x5F6670));
        StyleConstants.setForeground(typed, dark ? new Color(0x6CB6FF) : new Color(0x2F6FEB));

        Color bg = EditorTheme.background();
        pane.setBackground(bg);
        pane.setCaretColor(dark ? Color.WHITE : Color.BLACK);
        scroll.getViewport().setBackground(bg);
        Color border = UIManager.getColor("Component.borderColor");
        scroll.setBorder(BorderFactory.createLineBorder(border != null ? border : Color.GRAY));
        rebuild();
    }

    private AttributeSet attr(Kind kind) {
        return switch (kind) {
            case ERROR -> error;
            case INFO -> info;
            case INPUT -> typed;
            default -> normal;
        };
    }

    /** Boleh dipanggil dari thread mana saja. */
    void append(String text, Kind kind) {
        SwingUtilities.invokeLater(() -> {
            chunks.add(new Chunk(text, kind));
            totalChars += text.length();
            StyledDocument doc = pane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), text, attr(kind));
                int cut = 0;
                while (totalChars > MAX_CHARS && chunks.size() > 1) { // buang output paling lama
                    Chunk old = chunks.remove(0);
                    totalChars -= old.text().length();
                    cut += old.text().length();
                }
                if (cut > 0) doc.remove(0, cut);
                pane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) { }
        });
    }

    private void rebuild() {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            for (Chunk c : chunks) doc.insertString(doc.getLength(), c.text(), attr(c.kind()));
        } catch (BadLocationException ignored) { }
    }

    void clear() {
        chunks.clear();
        totalChars = 0;
        pane.setText("");
    }

    String plainText() {
        return pane.getText();
    }

    void setInputEnabled(boolean on) {
        input.setEnabled(on);
        send.setEnabled(on);
    }

    void focusInput() {
        input.requestFocusInWindow();
    }
}
