package id.dg.jcode.pro;

import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.theme.ThemeManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** Kumpulan tab editor. */
public final class EditorTabs extends JTabbedPane {

    private final Settings settings;
    private final Runnable onStateChange;
    private Predicate<EditorTab> saver = tab -> false;
    private int untitledCounter = 1;

    public EditorTabs(Settings settings, Runnable onStateChange) {
        this.settings = settings;
        this.onStateChange = onStateChange;
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
        putClientProperty("JTabbedPane.tabClosable", true);
        putClientProperty("JTabbedPane.tabCloseToolTipText", "Tutup (Ctrl+W)");
        putClientProperty("JTabbedPane.tabCloseCallback",
                (BiConsumer<JTabbedPane, Integer>) (pane, index) -> close(index));
        addChangeListener(e -> onStateChange.run());
        ThemeManager.addListener(this::restyleAll);
    }

    public void setSaver(Predicate<EditorTab> saver) {
        this.saver = saver;
    }

    public EditorTab open(Path file, Project project) throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        for (EditorTab tab : all()) {
            if (normalized.equals(tab.file())) {
                setSelectedComponent(tab);
                return tab;
            }
        }
        EditorTab tab = EditorTab.fromFile(normalized, project, settings.getEditorFontSize());
        add(tab);
        return tab;
    }

    public EditorTab newUntitled(String content) {
        EditorTab tab = new EditorTab(null, null, "Tanpa Judul " + untitledCounter++, content,
                settings.getEditorFontSize());
        add(tab);
        return tab;
    }

    private void add(EditorTab tab) {
        tab.setOnChange(() -> {
            refreshTitle(tab);
            onStateChange.run();
        });
        addTab(tab.title(), tab);
        refreshTitle(tab);
        setSelectedComponent(tab);
        tab.area().requestFocusInWindow();
    }

    public EditorTab current() {
        return getSelectedComponent() instanceof EditorTab t ? t : null;
    }

    public RSyntaxTextArea currentArea() {
        EditorTab t = current();
        return t == null ? null : t.area();
    }

    public List<EditorTab> all() {
        List<EditorTab> list = new ArrayList<>();
        for (int i = 0; i < getTabCount(); i++) {
            if (getComponentAt(i) instanceof EditorTab t) {
                list.add(t);
            }
        }
        return list;
    }

    public void refreshTitle(EditorTab tab) {
        int i = indexOfComponent(tab);
        if (i >= 0) {
            setTitleAt(i, tab.title() + (tab.isDirty() ? " *" : ""));
            setToolTipTextAt(i, tab.file() == null ? tab.title() : tab.file().toString());
        }
    }

    public boolean closeCurrent() {
        int i = getSelectedIndex();
        return i < 0 || close(i);
    }

    public boolean close(int index) {
        if (!(getComponentAt(index) instanceof EditorTab tab)) {
            return false;
        }
        if (tab.isDirty()) {
            setSelectedIndex(index);
            int choice = JOptionPane.showConfirmDialog(this,
                    "File \"" + tab.title() + "\" belum disimpan. Simpan sekarang?",
                    "Tutup file", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
            if (choice == JOptionPane.YES_OPTION && !saver.test(tab)) {
                return false;
            }
        }
        remove(index);
        onStateChange.run();
        return true;
    }

    public boolean confirmCloseAll() {
        for (EditorTab tab : all()) {
            if (tab.isDirty() && !close(indexOfComponent(tab))) {
                return false;
            }
        }
        return true;
    }

    /** Menutup tanpa bertanya semua tab yang filenya ada di dalam path (dipakai setelah delete). */
    public void closeUnder(Path path) {
        for (EditorTab tab : all()) {
            if (tab.file() != null && tab.file().startsWith(path)) {
                remove(indexOfComponent(tab));
            }
        }
        onStateChange.run();
    }

    /** Memperbarui lokasi file tab setelah rename/pindah. */
    public void remap(Path from, Path to) {
        for (EditorTab tab : all()) {
            if (tab.file() != null && tab.file().startsWith(from)) {
                tab.setFile(to.resolve(from.relativize(tab.file())).normalize());
            }
        }
    }

    public void setProject(Path root, Project project) {
        for (EditorTab tab : all()) {
            if (tab.file() != null && tab.file().startsWith(root)) {
                tab.setProject(project);
            }
        }
    }

    /** Memuat ulang semua tab dari disk; tab yang filenya sudah hilang ditutup. */
    public void reloadAllFromDisk() {
        for (EditorTab tab : all()) {
            if (tab.file() == null) {
                continue;
            }
            if (Files.isRegularFile(tab.file())) {
                try {
                    tab.reload();
                } catch (IOException ignored) {
                    // biarkan isi lama
                }
            } else {
                remove(indexOfComponent(tab));
            }
        }
        onStateChange.run();
    }

    public void restyleAll() {
        for (EditorTab tab : all()) {
            tab.restyle(settings.getEditorFontSize());
        }
    }
}
