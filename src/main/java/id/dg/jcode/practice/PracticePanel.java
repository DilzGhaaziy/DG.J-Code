package id.dg.jcode.practice;

import id.dg.jcode.assist.CodeAssist;
import id.dg.jcode.assist.Tone;
import id.dg.jcode.theme.AppTheme;
import id.dg.jcode.theme.EditorTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Halaman Praktek: editor + Run + output terpisah + Simpan + Cetak + pilihan tema. */
public final class PracticePanel extends JPanel {
    private static final String TEMPLATE = """
            import java.util.Scanner;

            public class Main {
                public static void main(String[] args) {
                    System.out.println("Halo, selamat belajar Java!");
                }
            }
            """;
    private static final Pattern PUBLIC_CLASS =
            Pattern.compile("\\bpublic\\s+(?:(?:final|abstract)\\s+)*class\\s+([A-Za-z_$][\\w$]*)");
    private static final Preferences PREFS = Preferences.userNodeForPackage(PracticePanel.class);

    private final RSyntaxTextArea area = new RSyntaxTextArea(TEMPLATE, 20, 60);
    private final RTextScrollPane editorScroll;
    private final PracticeRunner runner = new PracticeRunner();
    private final PracticeOutput output;
    private final JSplitPane split;
    private final RunPillButton run = new RunPillButton("Run");
    private final JButton stop = new JButton("Stop");
    private final JButton toggle = new JButton("Sembunyikan Output");
    private final JButton saveBtn = new JButton("Simpan", PracticeIcons.save());
    private final JButton saveAsBtn = new JButton("Simpan Sebagai", PracticeIcons.saveAs());
    private final JComboBox<AppTheme.Choice> themeBox = new JComboBox<>(AppTheme.Choice.values());
    private final JLabel fileLabel = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final Timer statusTimer = new Timer(2500, e -> status.setText(" "));

    private Path currentFile;
    private boolean dirty;
    private boolean syncingTheme;
    private int lastDivider = -1;

    public PracticePanel(Runnable onBack) {
        super(new BorderLayout());
        Font mono = codeFont(15f);

        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        area.setFont(mono);
        area.setCodeFoldingEnabled(false);
        area.setAntiAliasingEnabled(true);
        area.setTabSize(4);
        editorScroll = new RTextScrollPane(area);
        editorScroll.setLineNumbersEnabled(true);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        EditorTheme.install(area, editorScroll);                    // warna ikut tema (SETELAH setFont)
        CodeAssist.install(area, () -> currentFile, Tone.FRIENDLY); // koreksi kode, bahasa ramah

        output = new PracticeOutput(mono.deriveFont(14f), runner::sendInput);

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, output);
        split.setResizeWeight(0.7);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());

        add(toolbar(onBack), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.65));

        stop.setEnabled(false);
        statusTimer.setRepeats(false);
        run.addActionListener(e -> runCode());
        stop.addActionListener(e -> runner.stop());
        toggle.addActionListener(e -> toggleOutput());
        saveBtn.addActionListener(e -> save());
        saveAsBtn.addActionListener(e -> saveAs());

        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { markDirty(); }
            @Override public void removeUpdate(DocumentEvent e) { markDirty(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "dg.run", this::runCode);
        bindKey(ctrlEnter, "dg.run", this::runCode);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "dg.save", () -> save());
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "dg.saveAs", () -> saveAs());
        area.getInputMap().put(ctrlEnter, "dg.run");          // Ctrl+Enter di dalam editor juga
        area.getActionMap().put("dg.run", getActionMap().get("dg.run"));

        // Kalau tema diganti dari tempat lain (misalnya menu MainFrame), pilihan di sini ikut berubah.
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) SwingUtilities.invokeLater(this::syncThemeBox);
        });
        updateFileLabel();
    }

    private JComponent toolbar(Runnable onBack) {
        JButton back = new JButton("← Beranda");
        back.addActionListener(e -> { runner.stop(); onBack.run(); });
        JButton print = new JButton("Cetak");
        print.addActionListener(e -> PrintDialog.show(this, area.getText(), output.plainText()));
        JLabel title = new JLabel("Praktek");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        run.setToolTipText("Jalankan (F5 / Ctrl+Enter)");
        saveBtn.setToolTipText("Simpan (Ctrl+S)");
        saveAsBtn.setToolTipText("Simpan Sebagai (Ctrl+Shift+S)");
        saveBtn.setIconTextGap(6);
        saveAsBtn.setIconTextGap(6);

        themeBox.setToolTipText("Tema aplikasi");
        themeBox.setSelectedItem(AppTheme.current());
        themeBox.addActionListener(e -> {
            if (syncingTheme) return;
            AppTheme.Choice c = (AppTheme.Choice) themeBox.getSelectedItem();
            if (c != null && c != AppTheme.current()) SwingUtilities.invokeLater(() -> AppTheme.change(c));
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(back);
        left.add(title);
        left.add(fileLabel);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(status);
        right.add(themeBox);
        right.add(toggle);
        right.add(saveBtn);
        right.add(saveAsBtn);
        right.add(print);
        right.add(stop);
        right.add(run);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void bindKey(KeyStroke ks, String name, Runnable r) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ks, name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        });
    }

    // ------------------------------------------------------------------ Simpan

    private boolean save() {
        return currentFile == null ? saveAs() : writeTo(currentFile);
    }

    private boolean saveAs() {
        String lastDir = PREFS.get("lastDir", null);
        JFileChooser fc = new JFileChooser(lastDir != null && new File(lastDir).isDirectory() ? lastDir : null);
        fc.setDialogTitle("Simpan Sebagai");
        fc.setFileFilter(new FileNameExtensionFilter("File Java (*.java)", "java"));
        String suggested = currentFile != null ? currentFile.getFileName().toString() : suggestedName();
        fc.setSelectedFile(new File(fc.getCurrentDirectory(), suggested));

        while (true) {
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".java")) f = new File(f.getPath() + ".java");
            boolean sameFile = currentFile != null && f.toPath().toAbsolutePath().equals(currentFile.toAbsolutePath());
            if (f.exists() && !sameFile) {
                int r = JOptionPane.showConfirmDialog(this,
                        "File \"" + f.getName() + "\" sudah ada. Timpa?", "Simpan Sebagai",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.NO_OPTION) continue;        // pilih nama lain
                if (r != JOptionPane.YES_OPTION) return false;   // batal
            }
            return writeTo(f.toPath());
        }
    }

    private boolean writeTo(Path p) {
        try {
            Files.writeString(p, area.getText(), StandardCharsets.UTF_8);
            currentFile = p;
            dirty = false;
            updateFileLabel();
            Path parent = p.toAbsolutePath().getParent();
            if (parent != null) PREFS.put("lastDir", parent.toString());
            flashStatus("Tersimpan");
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan:\n" + ex.getMessage(), "Simpan",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String suggestedName() {
        Matcher m = PUBLIC_CLASS.matcher(area.getText());
        return (m.find() ? m.group(1) : "Main") + ".java"; // nama file = nama class public
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            updateFileLabel();
        }
    }

    private void updateFileLabel() {
        String name = currentFile == null ? "Belum disimpan" : currentFile.getFileName().toString();
        fileLabel.setText(name + (dirty ? " •" : ""));
        fileLabel.setToolTipText(currentFile == null ? null : currentFile.toAbsolutePath().toString());
    }

    private void flashStatus(String text) {
        status.setText(text);
        statusTimer.restart();
    }

    /** Untuk MainFrame: panggil sebelum aplikasi ditutup. false = batal menutup. */
    public boolean canClose() {
        if (!dirty) return true;
        int r = JOptionPane.showConfirmDialog(this,
                "Kode di halaman Praktek belum disimpan.\nSimpan sekarang?", "Simpan Perubahan",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) return save();
        return r == JOptionPane.NO_OPTION;
    }

    private void syncThemeBox() {
        syncingTheme = true;
        try {
            themeBox.setSelectedItem(AppTheme.current());
        } finally {
            syncingTheme = false;
        }
    }

    // ------------------------------------------------------------------ Run

    private void runCode() {
        if (runner.isRunning()) return;
        if (!output.isVisible()) toggleOutput();
        output.clear();
        run.setEnabled(false);
        stop.setEnabled(true);
        statusTimer.stop();
        status.setText("Sedang berjalan...");
        output.setInputEnabled(true);
        String code = area.getText();
        if (code.contains("Scanner")) output.focusInput();

        Thread t = new Thread(() -> runner.compileAndRun(code, new PracticeRunner.Listener() {
            @Override public void out(String s) { output.append(s, PracticeOutput.Kind.NORMAL); }
            @Override public void err(String s) { output.append(s, PracticeOutput.Kind.ERROR); }
            @Override public void info(String s) { output.append(s, PracticeOutput.Kind.INFO); }
            @Override public void finished() {
                SwingUtilities.invokeLater(() -> {
                    run.setEnabled(true);
                    stop.setEnabled(false);
                    output.setInputEnabled(false);
                    status.setText(" ");
                    area.requestFocusInWindow();
                });
            }
        }), "dgjcode-praktek-run");
        t.setDaemon(true);
        t.start();
    }

    private void toggleOutput() {
        if (output.isVisible()) {
            lastDivider = split.getDividerLocation();
            output.setVisible(false);
            toggle.setText("Tampilkan Output");
        } else {
            output.setVisible(true);
            split.setDividerLocation(lastDivider > 0 ? lastDivider : (int) (split.getHeight() * 0.65));
            toggle.setText("Sembunyikan Output");
        }
    }

    public void shutdown() {
        runner.stop();
    }

    private static Font codeFont(float size) {
        for (String n : new String[]{"JetBrains Mono", "Consolas"}) {
            Font f = new Font(n, Font.PLAIN, (int) size);
            if (f.getFamily().equals(n)) return f;
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
    }
}
