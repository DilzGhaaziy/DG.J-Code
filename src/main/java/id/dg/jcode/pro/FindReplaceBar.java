package id.dg.jcode.pro;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/** Bilah Cari & Ganti di atas editor (Ctrl+F / Ctrl+H, Esc untuk menutup). */
public final class FindReplaceBar extends JPanel {

    private final Supplier<RSyntaxTextArea> target;
    private final JTextField findField = new JTextField(22);
    private final JTextField replaceField = new JTextField(22);
    private final JCheckBox matchCase = new JCheckBox("Huruf besar/kecil");
    private final JCheckBox wholeWord = new JCheckBox("Kata utuh");
    private final JLabel info = new JLabel(" ");
    private final JPanel replaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

    public FindReplaceBar(Supplier<RSyntaxTextArea> target) {
        this.target = target;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        findField.putClientProperty("JTextField.placeholderText", "Cari...");
        replaceField.putClientProperty("JTextField.placeholderText", "Ganti dengan...");

        JButton prev = new JButton("Sebelumnya");
        JButton next = new JButton("Berikutnya");
        JButton close = new JButton("Tutup");
        prev.addActionListener(e -> search(false));
        next.addActionListener(e -> search(true));
        close.addActionListener(e -> close());

        JPanel findRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        findRow.add(new JLabel("Cari:"));
        findRow.add(findField);
        findRow.add(prev);
        findRow.add(next);
        findRow.add(matchCase);
        findRow.add(wholeWord);
        findRow.add(info);
        findRow.add(close);

        JButton replaceOne = new JButton("Ganti");
        JButton replaceAll = new JButton("Ganti Semua");
        replaceOne.addActionListener(e -> replace(false));
        replaceAll.addActionListener(e -> replace(true));
        replaceRow.add(new JLabel("Ganti:"));
        replaceRow.add(replaceField);
        replaceRow.add(replaceOne);
        replaceRow.add(replaceAll);

        add(findRow);
        add(replaceRow);

        findField.addActionListener(e -> search(true));
        findField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "prev");
        findField.getActionMap().put("prev", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search(false);
            }
        });
        replaceField.addActionListener(e -> replace(false));

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeBar");
        getActionMap().put("closeBar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        setVisible(false);
    }

    public void open(boolean replaceMode) {
        RSyntaxTextArea area = target.get();
        if (area != null && area.getSelectedText() != null && !area.getSelectedText().contains("\n")) {
            findField.setText(area.getSelectedText());
        }
        replaceRow.setVisible(replaceMode);
        setVisible(true);
        revalidate();
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    public void close() {
        RSyntaxTextArea area = target.get();
        setVisible(false);
        if (area != null) {
            area.clearMarkAllHighlights();
            area.requestFocusInWindow();
        }
    }

    private SearchContext context(boolean forward) {
        SearchContext ctx = new SearchContext(findField.getText());
        ctx.setReplaceWith(replaceField.getText());
        ctx.setMatchCase(matchCase.isSelected());
        ctx.setWholeWord(wholeWord.isSelected());
        ctx.setRegularExpression(false);
        ctx.setSearchForward(forward);
        return ctx;
    }

    private void search(boolean forward) {
        RSyntaxTextArea area = target.get();
        if (area == null || findField.getText().isEmpty()) {
            return;
        }
        SearchResult r = SearchEngine.find(area, context(forward));
        if (!r.wasFound()) {
            // Mulai lagi dari awal/akhir dokumen (wrap around).
            area.setCaretPosition(forward ? 0 : area.getDocument().getLength());
            r = SearchEngine.find(area, context(forward));
        }
        info.setText(r.wasFound() ? " " : "Tidak ditemukan");
    }

    private void replace(boolean all) {
        RSyntaxTextArea area = target.get();
        if (area == null || findField.getText().isEmpty()) {
            return;
        }
        if (all) {
            SearchResult r = SearchEngine.replaceAll(area, context(true));
            info.setText(r.getCount() + " diganti");
        } else {
            SearchResult r = SearchEngine.replace(area, context(true));
            info.setText(r.wasFound() ? " " : "Tidak ditemukan");
        }
    }
}
