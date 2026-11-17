package id.dg.jcode.assist;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Pengatur koreksi kode untuk satu editor.
 * Pemakaian: {@code CodeAssist.install(area, () -> path, Tone.PROFESSIONAL);}
 */
public final class CodeAssist {
    private static final int DELAY_MS = 500;

    /** Satu thread latar belakang untuk SEMUA editor, supaya laptop tidak berat. */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dgjcode-assist");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static JavacChecker checker; // hanya disentuh oleh thread WORKER

    private final RSyntaxTextArea area;
    private final Supplier<Path> file;
    private final Tone tone;
    private final Timer timer;
    private final SquigglePainter painter = new SquigglePainter(ErrorIcon.RED);
    private final SuggestionBox box;
    private final List<Object> squiggles = new ArrayList<>();
    private final List<Object> lineMarks = new ArrayList<>();
    private final List<GutterIconInfo> icons = new ArrayList<>();
    private final Set<String> dismissed = new HashSet<>();
    private final DocumentListener docListener;
    private final CaretListener caretListener;

    private List<Problem> problems = List.of();
    private Problem shown;
    private int version;
    private int checkedVersion = -1;
    private boolean disposed;

    public static CodeAssist install(RSyntaxTextArea area, Supplier<Path> file, Tone tone) {
        return new CodeAssist(area, file, tone);
    }

    private CodeAssist(RSyntaxTextArea area, Supplier<Path> file, Tone tone) {
        this.area = area;
        this.file = file;
        this.tone = tone;
        this.box = new SuggestionBox(this::acceptShown);
        box.setVisible(false);
        area.add(box); // anak dari editor -> ikut tergulung bersama barisnya

        // Debounce: setiap ketikan me-restart timer; cek baru jalan 500 ms setelah berhenti.
        timer = new Timer(DELAY_MS, e -> runCheck());
        timer.setRepeats(false);

        docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { /* hanya gaya, abaikan */ }
        };
        area.getDocument().addDocumentListener(docListener);

        caretListener = e -> {
            if (version == checkedVersion) updateBox();
        };
        area.addCaretListener(caretListener);

        installKeys();
        timer.restart(); // cek pertama setelah file dibuka
    }

    // ------------------------------------------------------------------ tombol Tab & Esc

    private void installKeys() {
        InputMap im = area.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = area.getActionMap();
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "dg.assist.tab", this::acceptShown);
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dg.assist.esc", this::dismissShown);
    }

    /** Pasang aksi baru; kalau tidak ada saran, teruskan ke aksi lama (Tab tetap menyisipkan tab). */
    private void bind(InputMap im, ActionMap am, KeyStroke ks, String name, java.util.function.BooleanSupplier handler) {
        Object oldKey = im.get(ks);
        Action old = oldKey == null ? null : am.get(oldKey);
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (handler.getAsBoolean()) return;
                if (old != null) old.actionPerformed(e);
                else if (ks.getKeyCode() == KeyEvent.VK_TAB) area.replaceSelection("\t");
            }
        });
    }

    // ------------------------------------------------------------------ alur pemeriksaan

    private void changed() {
        version++;
        hideBox();          // posisi saran lama sudah tidak valid
        timer.restart();
    }

    private void runCheck() {
        if (disposed) return;
        final String text = area.getText();
        final int v = version;
        final Path f = file == null ? null : file.get();
        WORKER.submit(() -> {
            List<Problem> result;
            try {
                if (checker == null) checker = new JavacChecker();
                List<JavacChecker.Diag> diags = checker.check(text, f);
                result = SuggestionEngine.build(text, diags, tone, f == null ? null : JavacChecker.baseName(f));
            } catch (Throwable t) {
                result = List.of();
            }
            final List<Problem> res = result;
            SwingUtilities.invokeLater(() -> apply(res, v));
        });
    }

    private void apply(List<Problem> res, int v) {
        if (disposed || v != version) return; // kode sudah berubah lagi -> hasil basi, buang
        clearMarks();
        problems = res;
        checkedVersion = v;

        RTextScrollPane sp = (RTextScrollPane) SwingUtilities.getAncestorOfClass(RTextScrollPane.class, area);
        if (sp != null && !sp.isIconRowHeaderEnabled()) sp.setIconRowHeaderEnabled(true);
        Gutter gutter = RSyntaxUtilities.getGutter(area);
        Highlighter h = area.getHighlighter();
        Color lineBg = isDark() ? new Color(229, 72, 77, 38) : new Color(229, 72, 77, 26);

        for (Problem p : problems) {
            try {
                squiggles.add(h.addHighlight(p.start(), p.end(), painter));     // 1. garis bergelombang
            } catch (BadLocationException ignored) { }
            try {
                lineMarks.add(area.addLineHighlight(p.line(), lineBg));         // 2. latar merah tipis
            } catch (BadLocationException ignored) { }
            if (gutter != null) {
                try {
                    icons.add(gutter.addLineTrackingIcon(p.line(), ErrorIcon.INSTANCE, p.message())); // 3. ikon
                } catch (BadLocationException ignored) { }
            }
        }
        area.repaint();
        updateBox();
    }

    private void clearMarks() {
        Highlighter h = area.getHighlighter();
        for (Object tag : squiggles) h.removeHighlight(tag);
        for (Object tag : lineMarks) area.removeLineHighlight(tag);
        Gutter gutter = RSyntaxUtilities.getGutter(area);
        if (gutter != null) for (GutterIconInfo info : icons) gutter.removeTrackingIcon(info);
        squiggles.clear();
        lineMarks.clear();
        icons.clear();
    }

    // ------------------------------------------------------------------ kotak saran

    private void updateBox() {
        Problem pick = null;
        int caretLine = area.getCaretLineNumber();
        for (Problem p : problems) {
            if (dismissed.contains(key(p))) continue;
            if (p.line() == caretLine) { pick = p; break; }
            if (pick == null) pick = p; // cadangan: kesalahan pertama
        }
        if (pick == null) { hideBox(); return; }
        try {
            Rectangle r = area.modelToView2D(area.getLineStartOffset(pick.line())).getBounds();
            box.setContent(pick, tone, area.getFont(), isDark());
            Dimension d = box.getPreferredSize();
            int x = Math.max(r.x, 4);
            box.setBounds(x, r.y + r.height + 2, d.width, d.height);
            box.setVisible(true);
            shown = pick;
            area.repaint();
        } catch (BadLocationException ex) {
            hideBox();
        }
    }

    private void hideBox() {
        if (box.isVisible()) {
            box.setVisible(false);
            area.repaint();
        }
        shown = null;
    }

    /** Terapkan saran. Seluruh perubahan = SATU langkah undo (Ctrl+Z membatalkan sekaligus). */
    private boolean acceptShown() {
        Problem p = shown;
        if (p == null || !box.isVisible() || !p.hasFix() || version != checkedVersion) return false;
        List<Edit> edits = new ArrayList<>(p.fix().edits());
        edits.sort(Comparator.comparingInt(Edit::start).reversed()); // dari belakang agar offset tidak bergeser
        Document doc = area.getDocument();
        area.beginAtomicEdit();
        try {
            for (Edit ed : edits) {
                if (ed.end() > ed.start()) doc.remove(ed.start(), ed.end() - ed.start());
                if (!ed.text().isEmpty()) doc.insertString(ed.start(), ed.text(), null);
            }
            if (edits.size() == 1) {
                Edit ed = edits.get(0);
                area.setCaretPosition(Math.min(doc.getLength(), ed.start() + ed.text().length()));
            }
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
        } finally {
            area.endAtomicEdit();
        }
        return true;
    }

    private boolean dismissShown() {
        if (shown == null || !box.isVisible()) return false;
        dismissed.add(key(shown));
        hideBox();
        return true;
    }

    private String key(Problem p) {
        try {
            int s = area.getLineStartOffset(p.line());
            int e = area.getLineEndOffset(p.line());
            return p.message() + "|" + area.getText(s, e - s).strip();
        } catch (BadLocationException ex) {
            return p.message();
        }
    }

    private boolean isDark() {
        Color bg = area.getBackground();
        return (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000 < 128;
    }

    /** Panggil saat tab editor ditutup. */
    public void dispose() {
        disposed = true;
        timer.stop();
        area.getDocument().removeDocumentListener(docListener);
        area.removeCaretListener(caretListener);
        clearMarks();
        area.remove(box);
    }
}
