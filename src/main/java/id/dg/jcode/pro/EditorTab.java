package id.dg.jcode.pro;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Satu tab editor. */
public final class EditorTab extends JPanel {

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.$]+)\\s*;");
    private static final Pattern PUBLIC_CLASS = Pattern.compile(
            "public\\s+(?:final\\s+|abstract\\s+)*(?:class|record|enum|interface)\\s+([\\w$]+)");
    private static final Pattern MAIN = Pattern.compile("static\\s+void\\s+main\\s*\\(");

    private final RSyntaxTextArea area = new RSyntaxTextArea(25, 80);
    private final RTextScrollPane scroll;
    private final String untitledName;
    private Project project;
    private Path file;
    private boolean dirty;
    private Runnable onChange = () -> { };
    
    // Variabel assist ditambahkan di sini
    private final id.dg.jcode.assist.CodeAssist assist;

    public EditorTab(Path file, Project project, String untitledName, String text, int fontSize) {
        super(new BorderLayout());
        this.file = file;
        this.project = project;
        this.untitledName = untitledName;

        EditorStyle.configure(area);
        area.setText(text);
        area.setCaretPosition(0);
        area.discardAllEdits();

        scroll = new RTextScrollPane(area);
        scroll.setLineNumbersEnabled(true);
        scroll.setFoldIndicatorEnabled(true);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        EditorStyle.apply(area, scroll, fontSize);
        add(scroll, BorderLayout.CENTER);

        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setDirty(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setDirty(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        area.addCaretListener(e -> onChange.run());
        
        // Instalasi CodeAssist ditambahkan di akhir konstruktor
        this.assist = id.dg.jcode.assist.CodeAssist.install(area, () -> this.file, id.dg.jcode.assist.Tone.PROFESSIONAL);
    }

    public static EditorTab fromFile(Path file, Project project, int fontSize) throws IOException {
        return new EditorTab(file, project, null, Files.readString(file, StandardCharsets.UTF_8), fontSize);
    }

    public void save() throws IOException {
        if (file == null) {
            throw new IllegalStateException("File belum punya nama.");
        }
        Files.writeString(file, area.getText(), StandardCharsets.UTF_8);
        setDirty(false);
    }

    public void saveAs(Path target) throws IOException {
        file = target.toAbsolutePath().normalize();
        save();
        onChange.run();
    }

    /** Memuat ulang isi dari disk (dipakai setelah refactor). */
    public void reload() throws IOException {
        int pos = area.getCaretPosition();
        area.setText(Files.readString(file, StandardCharsets.UTF_8));
        area.setCaretPosition(Math.min(pos, area.getDocument().getLength()));
        area.discardAllEdits();
        setDirty(false);
    }

    public void restyle(int fontSize) {
        EditorStyle.apply(area, scroll, fontSize);
    }

    private void setDirty(boolean value) {
        if (dirty != value) {
            dirty = value;
            onChange.run();
        }
    }

    public String simpleClassName() {
        if (file != null) {
            String n = file.getFileName().toString();
            return n.endsWith(".java") ? n.substring(0, n.length() - 5) : n;
        }
        Matcher m = PUBLIC_CLASS.matcher(area.getText());
        return m.find() ? m.group(1) : "Main";
    }

    /** Nama lengkap class (paket + nama) berdasarkan isi kode. */
    public String javaClassName() {
        Matcher m = PACKAGE.matcher(area.getText());
        return m.find() ? m.group(1) + "." + simpleClassName() : simpleClassName();
    }

    public boolean hasMain() {
        return MAIN.matcher(area.getText()).find();
    }

    public String title() {
        return file != null ? String.valueOf(file.getFileName()) : untitledName;
    }

    public void setOnChange(Runnable r) {
        onChange = r;
    }

    public void setFile(Path file) {
        this.file = file;
        onChange.run();
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public RSyntaxTextArea area() {
        return area;
    }

    public Path file() {
        return file;
    }

    public Project project() {
        return project;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int caretLine() {
        return area.getCaretLineNumber() + 1;
    }

    public int caretColumn() {
        return area.getCaretOffsetFromLineStart() + 1;
    }

    /** Method baru untuk membersihkan resource saat tab ditutup */
    public void dispose() {
        if (assist != null) {
            assist.dispose();
        }
    }
}