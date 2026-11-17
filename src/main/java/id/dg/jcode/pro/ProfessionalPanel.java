package id.dg.jcode.pro;

import id.dg.jcode.AppInfo;
import id.dg.jcode.run.JavaCompilerService;
import id.dg.jcode.run.ProgramRunner;
import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.icons.LineIcon;
import id.dg.jcode.ui.icons.TreeIcons;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.ThemeDialog;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Mode Professional: tata letak mirip IDE desktop klasik + build & run. */
public final class ProfessionalPanel extends JPanel {

    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
    private static final int ALT = InputEvent.ALT_DOWN_MASK;
    private static final String CARD_EMPTY = "empty";
    private static final String CARD_TABS = "tabs";
    private static final Locale ID = Locale.forLanguageTag("id");

    /** Isi "clipboard" panel Projects (Copy/Cut). */
    private record Clip(ProjectTree.Node node, boolean cut) {
    }

    /** Data satu kali build/run. mainClass == null berarti hanya build. */
    private record Launch(String title, List<Path> sources, Path outDir, String mainClass, Path workDir) {
    }

    private final Settings settings;
    private final Runnable onHome;
    private final EditorTabs tabs;
    private final ProjectTree projectTree;
    private final FindReplaceBar findBar;
    private final OutputPanel output;
    private final EmptyState emptyState;
    private final StatusBar status = new StatusBar();
    private final CardLayout editorLayout = new CardLayout();
    private final JPanel editorCards = new JPanel(editorLayout);
    private final JPanel projectsPanel;
    private final JSplitPane horizontal;
    private final JSplitPane vertical;
    private final JMenuBar menuBar;
    private final ProgramRunner runner = new ProgramRunner();

    private JButton buildButton;
    private JButton runButton;
    private JButton stopButton;
    private JButton runThisButton;
    private volatile boolean busy;
    private volatile boolean stopRequested;
    private Clip clip;
    private int savedProjectsDivider = 260;
    private int savedOutputDivider = -1;

    public ProfessionalPanel(Settings settings, Runnable onHome) {
        super(new BorderLayout());
        this.settings = settings;
        this.onHome = onHome;

        tabs = new EditorTabs(settings, this::updateStatus);
        tabs.setSaver(this::saveTab);
        projectTree = new ProjectTree(n -> openFile(n.path(), n.project()));
        projectTree.setPopupProvider(this::popupFor);
        projectTree.setRenameHandler(this::rename);
        findBar = new FindReplaceBar(tabs::currentArea);
        output = new OutputPanel(this::toggleOutput, this::sendInput);
        emptyState = new EmptyState(this::newProject, this::openProject, this::newFile);

        projectsPanel = new JPanel(new BorderLayout());
        JLabel projectsTitle = new JLabel("PROJECTS");
        projectsTitle.setFont(Fonts.uiSemibold(11f));
        projectsTitle.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 8));
        JScrollPane treeScroll = new JScrollPane(projectTree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        projectsPanel.add(projectsTitle, BorderLayout.NORTH);
        projectsPanel.add(treeScroll, BorderLayout.CENTER);

        editorCards.add(emptyState, CARD_EMPTY);
        editorCards.add(tabs, CARD_TABS);
        JPanel editorArea = new JPanel(new BorderLayout());
        editorArea.add(findBar, BorderLayout.NORTH);
        editorArea.add(editorCards, BorderLayout.CENTER);

        horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectsPanel, editorArea);
        horizontal.setDividerLocation(260);
        horizontal.setResizeWeight(0);
        horizontal.setBorder(BorderFactory.createEmptyBorder());

        vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontal, output);
        vertical.setResizeWeight(1);
        vertical.setBorder(BorderFactory.createEmptyBorder());

        add(createToolBar(), BorderLayout.NORTH);
        add(vertical, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        menuBar = createMenuBar();
        updateStatus();
        output.info("Selamat datang di mode Professional.");
        output.info("Buat proyek lewat File > New Project (Ctrl+Shift+N) atau buka proyek (Ctrl+Shift+O).");
    }

    public JMenuBar menuBar() {
        return menuBar;
    }

    public boolean confirmCloseAll() {
        return tabs.confirmCloseAll();
    }

    /** Menghentikan program yang masih berjalan saat aplikasi ditutup. */
    public void shutdown() {
        stopRequested = true;
        runner.stop();
    }

    // ===================== Menu & toolbar =====================

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(item("New Project...", ks(KeyEvent.VK_N, CTRL | SHIFT), this::newProject));
        file.add(item("New File...", ks(KeyEvent.VK_N, CTRL), this::newFile));
        file.addSeparator();
        file.add(item("Open Project...", ks(KeyEvent.VK_O, CTRL | SHIFT), this::openProject));
        file.add(item("Open File...", ks(KeyEvent.VK_O, CTRL), this::openFileDialog));
        file.addSeparator();
        file.add(item("Save", ks(KeyEvent.VK_S, CTRL), this::save));
        file.add(item("Save As...", null, this::saveAs));
        file.add(item("Save All", ks(KeyEvent.VK_S, CTRL | SHIFT), this::saveAll));
        file.add(item("Close Tab", ks(KeyEvent.VK_W, CTRL), tabs::closeCurrent));
        file.addSeparator();
        file.add(item("Kembali ke Beranda", null, onHome));
        file.add(item("Exit", null, this::exit));

        JMenu edit = new JMenu("Edit");
        edit.add(item("Undo", ks(KeyEvent.VK_Z, CTRL), () -> withArea(RSyntaxTextArea::undoLastAction)));
        edit.add(item("Redo", ks(KeyEvent.VK_Y, CTRL), () -> withArea(RSyntaxTextArea::redoLastAction)));
        edit.addSeparator();
        edit.add(item("Cut", ks(KeyEvent.VK_X, CTRL), () -> withArea(RSyntaxTextArea::cut)));
        edit.add(item("Copy", ks(KeyEvent.VK_C, CTRL), () -> withArea(RSyntaxTextArea::copy)));
        edit.add(item("Paste", ks(KeyEvent.VK_V, CTRL), () -> withArea(RSyntaxTextArea::paste)));
        edit.add(item("Select All", ks(KeyEvent.VK_A, CTRL), () -> withArea(RSyntaxTextArea::selectAll)));
        edit.addSeparator();
        edit.add(item("Find...", ks(KeyEvent.VK_F, CTRL), () -> findBar.open(false)));
        edit.add(item("Replace...", ks(KeyEvent.VK_H, CTRL), () -> findBar.open(true)));

        JMenu view = new JMenu("View");
        view.add(item("Tampilkan/Sembunyikan Projects", ks(KeyEvent.VK_1, CTRL), this::toggleProjects));
        view.add(item("Tampilkan/Sembunyikan Output", ks(KeyEvent.VK_4, CTRL), this::toggleOutput));
        view.add(item("Pilih Tema...", null, this::openThemeDialog));

        JMenu navigate = new JMenu("Navigate");
        navigate.add(item("Go to Line...", ks(KeyEvent.VK_G, CTRL), this::goToLine));

        JMenu source = new JMenu("Source");
        source.add(item("Format", ks(KeyEvent.VK_F, ALT | SHIFT), () -> withArea(CodeFormatter::formatArea)));
        source.add(item("Toggle Comment", ks(KeyEvent.VK_SLASH, CTRL), this::toggleComment));

        JMenu refactor = new JMenu("Refactor");
        refactor.add(item("Rename (item terpilih di Projects)", null, () -> {
            ProjectTree.Node n = projectTree.selectedNode();
            if (n == null) {
                info("Pilih proyek, paket, atau class di panel Projects terlebih dahulu.");
            } else {
                rename(n);
            }
        }));

        JMenu run = new JMenu("Run");
        run.add(item("Run Project", ks(KeyEvent.VK_F6, 0), this::runProject));
        run.add(item("Run This (class yang dibuka)", ks(KeyEvent.VK_F6, SHIFT), this::runCurrent));
        run.add(item("Stop", ks(KeyEvent.VK_F2, CTRL), this::stop));

        JMenu build = new JMenu("Build");
        build.add(item("Build Project", ks(KeyEvent.VK_F11, 0), () -> buildProject(false)));
        build.add(item("Clean and Build Project", ks(KeyEvent.VK_F11, SHIFT), () -> buildProject(true)));

        JMenu tools = new JMenu("Tools");
        tools.add(item("Settings...", null, this::openSettings));
        tools.add(item("Tema...", null, this::openThemeDialog));

        JMenu window = new JMenu("Window");
        window.add(item("Reset Layout", null, this::resetLayout));

        JMenu help = new JMenu("Help");
        help.add(item("Daftar Shortcut", null, this::showShortcuts));
        help.add(item("Tentang " + AppInfo.NAME, null, this::showAbout));

        for (JMenu m : new JMenu[] {file, edit, view, navigate, source, refactor, run, build, tools, window, help}) {
            bar.add(m);
        }
        return bar;
    }

    private JToolBar createToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bar.add(tool(LineIcon.Kind.HOME, "Beranda", onHome));
        bar.addSeparator();
        bar.add(tool(LineIcon.Kind.NEW_PROJECT, "Proyek baru (Ctrl+Shift+N)", this::newProject));
        bar.add(tool(LineIcon.Kind.OPEN, "Buka proyek (Ctrl+Shift+O)", this::openProject));
        bar.add(tool(LineIcon.Kind.SAVE, "Simpan (Ctrl+S)", this::save));
        bar.addSeparator();
        bar.add(tool(LineIcon.Kind.UNDO, "Undo (Ctrl+Z)", () -> withArea(RSyntaxTextArea::undoLastAction)));
        bar.add(tool(LineIcon.Kind.REDO, "Redo (Ctrl+Y)", () -> withArea(RSyntaxTextArea::redoLastAction)));
        bar.addSeparator();

        buildButton = tool(LineIcon.Kind.BUILD, "Build proyek (F11)", () -> buildProject(false));
        runButton = tool(LineIcon.Kind.RUN, "Jalankan proyek (F6)", this::runProject);
        runButton.setText("Run");
        stopButton = tool(LineIcon.Kind.STOP, "Hentikan program (Ctrl+F2)", this::stop);
        stopButton.setEnabled(false);
        runThisButton = tool(LineIcon.Kind.RUN, "Jalankan class yang sedang dibuka (Shift+F6)", this::runCurrent);
        runThisButton.setText("Run This");

        bar.add(buildButton);
        bar.add(runButton);
        bar.add(stopButton);
        bar.add(runThisButton);

        // Dorong tombol Tema ke pojok kanan.
        bar.add(Box.createHorizontalGlue());
        JButton theme = new JButton("Tema");
        theme.setToolTipText("Pilih tema tampilan");
        theme.setFocusable(false);
        theme.addActionListener(e -> openThemeDialog());
        bar.add(theme);
        return bar;
    }

    private JButton tool(LineIcon.Kind kind, String tip, Runnable action) {
        JButton b = new JButton(new LineIcon(kind));
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JMenuItem item(String text, KeyStroke key, Runnable action) {
        JMenuItem i = new JMenuItem(text);
        if (key != null) {
            i.setAccelerator(key);
        }
        i.addActionListener(e -> action.run());
        return i;
    }

    private static JMenuItem item(String text, Runnable action, boolean enabled) {
        JMenuItem i = item(text, null, action);
        i.setEnabled(enabled);
        return i;
    }

    private static KeyStroke ks(int key, int modifiers) {
        return KeyStroke.getKeyStroke(key, modifiers);
    }

    // ===================== Menu klik kanan Projects =====================

    private JPopupMenu popupFor(ProjectTree.Node n) {
        JPopupMenu m = new JPopupMenu();
        switch (n.kind()) {
            case PROJECT -> {
                Project p = n.project();
                m.add(newMenu(n));
                m.addSeparator();
                m.add(item("Run", () -> runProject(p), true));
                m.add(item("Rename...", () -> renameProject(p), true));
                m.add(item("Delete", () -> deleteProject(p), true));
                m.add(item("Open File Location", () -> openLocation(p.root()), true));
                m.addSeparator();
                m.add(item("Save", () -> saveProjectFiles(p), true));
                m.add(item("Save As...", () -> saveProjectAs(p), true));
            }
            case SOURCE -> m.add(newMenu(n));
            case PACKAGE -> {
                boolean normal = !n.isDefaultPackage();
                m.add(newMenu(n));
                m.addSeparator();
                m.add(item("Copy", () -> clip = new Clip(n, false), normal));
                m.add(item("Cut", () -> clip = new Clip(n, true), normal));
                m.add(item("Paste", () -> paste(n), clip != null));
                m.addSeparator();
                m.add(item("Delete", () -> deleteNode(n), normal));
                m.add(refactorMenu(n, normal));
            }
            case CLASS -> {
                m.add(item("Open", () -> openFile(n.path(), n.project()), true));
                m.addSeparator();
                m.add(item("Cut", () -> clip = new Clip(n, true), true));
                m.add(item("Copy", () -> clip = new Clip(n, false), true));
                m.add(item("Paste", () -> paste(n), clip != null));
                m.addSeparator();
                m.add(item("Run File", () -> runFile(n), true));
                m.add(item("Delete", () -> deleteNode(n), true));
                m.add(refactorMenu(n, true));
            }
            default -> {
                m.add(item("Open", () -> openFile(n.path(), n.project()), true));
                m.add(item("Delete", () -> deleteNode(n), true));
            }
        }
        return m;
    }

    private JMenu newMenu(ProjectTree.Node n) {
        JMenu menu = new JMenu("New");
        menu.add(item("Java Class...", () -> newClassIn(n), true));
        menu.add(item("Java Package...", () -> newPackageIn(n), true));
        return menu;
    }

    private JMenu refactorMenu(ProjectTree.Node n, boolean enabled) {
        JMenu menu = new JMenu("Refactor");
        JMenuItem rename = item("Rename...", () -> rename(n), enabled);
        rename.setAccelerator(ks(KeyEvent.VK_F2, 0));
        menu.add(rename);
        menu.setEnabled(enabled);
        return menu;
    }

    // ===================== Proyek & file =====================

    private void newProject() {
        NewProjectWizard.show(window()).ifPresent(p -> {
            projectTree.addProject(p);
            openFile(p.mainFile(), p);
            output.info("Proyek \"" + p.name() + "\" dibuat di " + p.root());
            updateStatus();
        });
    }

    private void openProject() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Buka folder proyek");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Project p = Project.load(chooser.getSelectedFile().toPath());
            projectTree.addProject(p);
            if (Files.isRegularFile(p.mainFile())) {
                openFile(p.mainFile(), p);
            }
            updateStatus();
        } catch (IOException ex) {
            error("Proyek gagal dibuka", ex);
        }
    }

    /** Ctrl+N atau "File Baru" di tampilan kosong. */
    private void newFile() {
        ProjectTree.Node selected = projectTree.selectedNode();
        if (selected != null) {
            newClassIn(selected);
            return;
        }
        List<Project> projects = projectTree.projects();
        if (projects.isEmpty()) {
            tabs.newUntitled("public class Main {\n\n    public static void main(String[] args) {\n"
                    + "        System.out.println(\"Halo!\");\n    }\n}\n");
            return;
        }
        EditorTab current = tabs.current();
        Project p = current != null && current.project() != null ? current.project() : projects.get(0);
        Path dir = p.mainFile().getParent();
        if (dir != null && Files.isDirectory(dir) && !dir.equals(p.srcDir())) {
            newClassIn(new ProjectTree.Node(TreeIcons.Kind.PACKAGE, dir, p, ProjectOps.packageOf(p, dir)));
        } else {
            newClassIn(new ProjectTree.Node(TreeIcons.Kind.SOURCE, p.srcDir(), p, "src"));
        }
    }

    private void newClassIn(ProjectTree.Node n) {
        Project p = n.project();
        Path dir = ProjectTree.dirFor(n);
        String pkg = ProjectOps.packageOf(p, dir);
        String name = ask("Java Class Baru",
                "Nama class" + (pkg.isEmpty() ? "" : " di paket " + pkg) + " (contoh: Mahasiswa):", "");
        if (name == null) {
            return;
        }
        if (!name.matches(ProjectOps.IDENT)) {
            info("Nama class harus diawali huruf dan tidak boleh memakai spasi.");
            return;
        }
        try {
            Path f = ProjectOps.createClass(p, dir, name);
            projectTree.refresh();
            openFile(f, p);
        } catch (IOException ex) {
            error("Class gagal dibuat", ex);
        }
    }

    private void newPackageIn(ProjectTree.Node n) {
        Project p = n.project();
        String base = n.kind() == TreeIcons.Kind.PACKAGE && !n.isDefaultPackage()
                ? ProjectOps.packageOf(p, n.path()) + "." : "";
        String name = ask("Java Package Baru", "Nama paket (huruf kecil, pisahkan dengan titik):", base);
        if (name == null) {
            return;
        }
        if (!name.matches(ProjectOps.PKG)) {
            info("Nama paket tidak valid. Contoh yang benar: belajar.dasar");
            return;
        }
        try {
            ProjectOps.createPackage(p, name);
            projectTree.refresh();
        } catch (IOException ex) {
            error("Paket gagal dibuat", ex);
        }
    }

    private void paste(ProjectTree.Node target) {
        Clip c = clip;
        if (c == null) {
            return;
        }
        Path src = c.node().path();
        if (!Files.exists(src)) {
            info("Item yang disalin sudah tidak ada.");
            clip = null;
            return;
        }
        if (!saveAll()) {
            return;
        }
        Project tp = target.project();
        Path targetDir = ProjectTree.dirFor(target);
        try {
            Path dest = c.node().kind() == TreeIcons.Kind.PACKAGE
                    ? ProjectOps.pastePackage(tp, src, targetDir, c.cut())
                    : ProjectOps.pasteClass(tp, src, targetDir, c.cut());
            if (c.cut()) {
                tabs.remap(src, dest);
                tabs.setProject(tp.root(), tp);
                clip = null;
            }
            tabs.reloadAllFromDisk();
            projectTree.refresh();
            output.info((c.cut() ? "Dipindahkan: " : "Disalin: ") + src.getFileName() + " -> " + dest);
        } catch (IOException ex) {
            error("Paste gagal", ex);
        }
    }

    private void deleteNode(ProjectTree.Node n) {
        String what = n.kind() == TreeIcons.Kind.PACKAGE
                ? "paket \"" + n.label() + "\" beserta seluruh isinya (termasuk sub-paket)"
                : "\"" + n.path().getFileName() + "\"";
        if (!confirm("Hapus " + what + "?\nTindakan ini tidak bisa dibatalkan.", "Delete")) {
            return;
        }
        try {
            tabs.closeUnder(n.path());
            ProjectOps.delete(n.path());
            projectTree.refresh();
            updateStatus();
        } catch (IOException ex) {
            error("Gagal menghapus", ex);
        }
    }

    private void deleteProject(Project p) {
        Object[] options = {"Hapus dari Disk", "Tutup Saja", "Batal"};
        int choice = JOptionPane.showOptionDialog(this,
                "Proyek \"" + p.name() + "\"\n\n"
                        + "Tutup Saja: proyek hanya dikeluarkan dari panel Projects.\n"
                        + "Hapus dari Disk: folder proyek dihapus permanen.",
                "Delete Project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (choice == 0) {
            if (!checkIdle()) {
                return;
            }
            if (!confirm("Yakin menghapus folder ini secara permanen?\n" + p.root(), "Hapus dari Disk")) {
                return;
            }
            try {
                tabs.closeUnder(p.root());
                ProjectOps.delete(p.root());
                projectTree.removeProject(p.root());
                updateStatus();
            } catch (IOException ex) {
                error("Proyek gagal dihapus", ex);
            }
        } else if (choice == 1) {
            for (EditorTab tab : tabs.all()) {
                if (tab.file() != null && tab.file().startsWith(p.root())
                        && !tabs.close(tabs.indexOfComponent(tab))) {
                    return;
                }
            }
            projectTree.removeProject(p.root());
            updateStatus();
        }
    }

    private void rename(ProjectTree.Node n) {
        switch (n.kind()) {
            case PROJECT -> renameProject(n.project());
            case PACKAGE -> {
                if (!n.isDefaultPackage()) {
                    renamePackage(n);
                }
            }
            case CLASS -> renameClass(n);
            default -> {
            }
        }
    }

    private void renameProject(Project p) {
        if (!checkIdle()) {
            return;
        }
        String name = ask("Rename Project", "Nama baru proyek (juga dipakai sebagai nama folder):", p.name());
        if (name == null || name.equals(p.name())) {
            return;
        }
        if (!name.matches("[A-Za-z0-9_\\- ]+")) {
            info("Nama proyek hanya boleh huruf, angka, spasi, - dan _.");
            return;
        }
        if (!saveAll()) {
            return;
        }
        Path newRoot = p.root().resolveSibling(name);
        try {
            if (!newRoot.toString().equalsIgnoreCase(p.root().toString())) {
                if (Files.exists(newRoot)) {
                    throw new IOException("Folder \"" + newRoot + "\" sudah ada.");
                }
                Files.move(p.root(), newRoot);
            } else {
                newRoot = p.root();
            }
            Project np = new Project(name, newRoot, p.mainClass());
            np.save();
            tabs.remap(p.root(), newRoot);
            tabs.setProject(newRoot, np);
            projectTree.replaceProject(p.root(), np);
            updateStatus();
        } catch (IOException ex) {
            error("Rename proyek gagal (tutup Explorer/program lain yang memakai folder ini)", ex);
        }
    }

    private void renameClass(ProjectTree.Node n) {
        Project p = n.project();
        Path f = n.path();
        String old = ProjectOps.simpleName(f);
        String name = ask("Rename Class (Refactor)",
                "Nama baru untuk class \"" + old + "\".\nSemua pemakaian nama ini di proyek ikut diganti:", old);
        if (name == null || name.equals(old)) {
            return;
        }
        if (!name.matches(ProjectOps.IDENT)) {
            info("Nama class harus diawali huruf dan tidak boleh memakai spasi.");
            return;
        }
        if (!saveAll()) {
            return;
        }
        String oldFq = ProjectOps.fqName(p, f);
        try {
            Path dest = ProjectOps.renameClass(p, f, name);
            tabs.remap(f, dest);
            String newFq = ProjectOps.fqName(p, dest);
            if (p.mainClass().equals(oldFq)) {
                updateMainClass(p, newFq);
            }
            tabs.reloadAllFromDisk();
            projectTree.refresh();
            output.info("Refactor: " + oldFq + " -> " + newFq);
        } catch (IOException ex) {
            error("Rename class gagal", ex);
        }
    }

    private void renamePackage(ProjectTree.Node n) {
        Project p = n.project();
        String old = ProjectOps.packageOf(p, n.path());
        String name = ask("Rename Package (Refactor)",
                "Nama baru untuk paket \"" + old + "\".\nBaris package/import di proyek ikut diganti:", old);
        if (name == null || name.equals(old)) {
            return;
        }
        if (!name.matches(ProjectOps.PKG)) {
            info("Nama paket tidak valid. Contoh yang benar: belajar.dasar");
            return;
        }
        if (!saveAll()) {
            return;
        }
        try {
            Path dest = ProjectOps.renamePackage(p, n.path(), name);
            tabs.remap(n.path(), dest);
            if (p.mainClass().startsWith(old + ".")) {
                updateMainClass(p, name + p.mainClass().substring(old.length()));
            }
            tabs.reloadAllFromDisk();
            projectTree.refresh();
            output.info("Refactor paket: " + old + " -> " + name);
        } catch (IOException ex) {
            error("Rename paket gagal", ex);
        }
    }

    private void updateMainClass(Project p, String newMain) throws IOException {
        Project np = new Project(p.name(), p.root(), newMain);
        np.save();
        projectTree.replaceProject(p.root(), np);
        tabs.setProject(p.root(), np);
    }

    private void openLocation(Path path) {
        try {
            ProjectOps.openLocation(path);
        } catch (IOException | RuntimeException ex) {
            info("Folder tidak bisa dibuka otomatis:\n" + path);
        }
    }

    private void saveProjectFiles(Project p) {
        for (EditorTab tab : tabs.all()) {
            if (tab.file() != null && tab.file().startsWith(p.root()) && tab.isDirty() && !saveTab(tab)) {
                return;
            }
        }
        output.info("Semua file proyek \"" + p.name() + "\" sudah disimpan.");
    }

    private void saveProjectAs(Project p) {
        saveProjectFiles(p);
        JFileChooser chooser = new JFileChooser(p.root().getParent() == null ? null : p.root().getParent().toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Pilih folder tujuan untuk salinan proyek");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String name = ask("Save Project As", "Nama proyek baru:", p.name() + "_salinan");
        if (name == null) {
            return;
        }
        if (!name.matches("[A-Za-z0-9_\\- ]+")) {
            info("Nama proyek hanya boleh huruf, angka, spasi, - dan _.");
            return;
        }
        try {
            Path newRoot = chooser.getSelectedFile().toPath().resolve(name).toAbsolutePath().normalize();
            Project np = ProjectOps.copyProject(p, newRoot, name);
            projectTree.addProject(np);
            output.info("Proyek disalin ke " + newRoot);
        } catch (IOException ex) {
            error("Save As proyek gagal", ex);
        }
    }

    private void openFileDialog() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileFilter(new FileNameExtensionFilter("File Java (*.java)", "java"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path f = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            Project owner = null;
            for (Project p : projectTree.projects()) {
                if (f.startsWith(p.root())) {
                    owner = p;
                }
            }
            openFile(f, owner);
        }
    }

    private void openFile(Path file, Project project) {
        try {
            tabs.open(file, project);
            updateStatus();
        } catch (IOException ex) {
            error("File gagal dibuka", ex);
        }
    }

    private void save() {
        EditorTab tab = tabs.current();
        if (tab != null) {
            saveTab(tab);
        }
    }

    private boolean saveTab(EditorTab tab) {
        if (tab.file() == null) {
            return saveTabAs(tab);
        }
        try {
            tab.save();
            return true;
        } catch (IOException ex) {
            error("File gagal disimpan", ex);
            return false;
        }
    }

    private void saveAs() {
        EditorTab tab = tabs.current();
        if (tab != null) {
            saveTabAs(tab);
        }
    }

    private boolean saveTabAs(EditorTab tab) {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileFilter(new FileNameExtensionFilter("File Java (*.java)", "java"));
        chooser.setSelectedFile(new File(tab.simpleClassName() + ".java"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().contains(".")) {
            target = target.resolveSibling(target.getFileName() + ".java");
        }
        try {
            tab.saveAs(target);
            tabs.refreshTitle(tab);
            projectTree.refresh();
            return true;
        } catch (IOException ex) {
            error("File gagal disimpan", ex);
            return false;
        }
    }

    private boolean saveAll() {
        for (EditorTab tab : tabs.all()) {
            if (tab.isDirty() && !saveTab(tab)) {
                return false;
            }
        }
        return true;
    }

    private void exit() {
        Window w = window();
        if (w != null) {
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }
    }

    // ===================== Editor =====================

    private void withArea(Consumer<RSyntaxTextArea> action) {
        RSyntaxTextArea area = tabs.currentArea();
        if (area != null) {
            action.accept(area);
            area.requestFocusInWindow();
        }
    }

    private void toggleComment() {
        withArea(area -> new RSyntaxTextAreaEditorKit.ToggleCommentAction()
                .actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, "toggleComment")));
    }

    private void goToLine() {
        RSyntaxTextArea area = tabs.currentArea();
        if (area == null) {
            return;
        }
        String input = ask("Go to Line", "Nomor baris (1 - " + area.getLineCount() + "):", "");
        if (input == null) {
            return;
        }
        try {
            int line = Math.max(1, Math.min(area.getLineCount(), Integer.parseInt(input)));
            area.setCaretPosition(area.getLineStartOffset(line - 1));
            area.requestFocusInWindow();
        } catch (NumberFormatException | BadLocationException ex) {
            info("Masukkan angka yang benar.");
        }
    }

    // ===================== Build & Run =====================

    private Project projectForRun() {
        EditorTab t = tabs.current();
        if (t != null && t.project() != null) {
            return t.project();
        }
        return projectTree.selectedProject();
    }

    /** F6: menjalankan main class proyek. */
    private void runProject() {
        Project p = projectForRun();
        if (p == null) {
            if (tabs.current() != null) {
                runCurrent();
            } else {
                info("Belum ada proyek. Buat proyek baru dulu (Ctrl+Shift+N).");
            }
            return;
        }
        runProject(p);
    }

    private void runProject(Project p) {
        if (!checkIdle() || !saveAll()) {
            return;
        }
        try {
            List<Path> sources = ProjectOps.javaFiles(p.srcDir());
            if (sources.isEmpty()) {
                info("Proyek \"" + p.name() + "\" belum punya file .java.");
                return;
            }
            String main = p.mainClass();
            if (!Files.isRegularFile(p.mainFile())) {
                EditorTab t = tabs.current();
                if (t != null && t.project() != null && t.project().root().equals(p.root()) && t.hasMain()) {
                    main = t.javaClassName();
                } else {
                    info("Main class \"" + main + "\" tidak ditemukan.\n"
                            + "Buka class yang punya method main lalu tekan \"Run This\".");
                    return;
                }
            }
            launch(new Launch(p.name(), sources, p.buildDir(), main, p.root()));
        } catch (IOException ex) {
            error("Gagal menyiapkan build", ex);
        }
    }

    private void buildProject(boolean clean) {
        Project p = projectForRun();
        if (p == null) {
            info("Belum ada proyek untuk di-build.");
            return;
        }
        if (!checkIdle() || !saveAll()) {
            return;
        }
        try {
            if (clean) {
                ProjectOps.delete(p.buildDir());
            }
            List<Path> sources = ProjectOps.javaFiles(p.srcDir());
            if (sources.isEmpty()) {
                info("Proyek \"" + p.name() + "\" belum punya file .java.");
                return;
            }
            launch(new Launch(p.name(), sources, p.buildDir(), null, p.root()));
        } catch (IOException ex) {
            error("Build gagal", ex);
        }
    }

    /** Tombol "Run This": hanya menjalankan class yang sedang ditampilkan. */
    private void runCurrent() {
        EditorTab t = tabs.current();
        if (t == null) {
            info("Buka sebuah class terlebih dahulu.");
            return;
        }
        runTab(t);
    }

    private void runFile(ProjectTree.Node n) {
        openFile(n.path(), n.project());
        EditorTab t = tabs.current();
        if (t != null) {
            runTab(t);
        }
    }

    private void runTab(EditorTab t) {
        if (!checkIdle()) {
            return;
        }
        if (!t.hasMain()) {
            info("Class \"" + t.simpleClassName() + "\" tidak punya method main, jadi tidak bisa dijalankan.\n"
                    + "Tambahkan:  public static void main(String[] args) { ... }");
            return;
        }
        try {
            if (t.project() != null && t.file() != null) {
                if (!saveAll()) {
                    return;
                }
                Project p = t.project();
                launch(new Launch(t.title(), ProjectOps.javaFiles(p.srcDir()), p.buildDir(),
                        t.javaClassName(), p.root()));
            } else if (t.file() != null) {
                if (!saveTab(t)) {
                    return;
                }
                Path tmp = Files.createTempDirectory("dgjcode-");
                launch(new Launch(t.title(), List.of(t.file()), tmp, t.javaClassName(), t.file().getParent()));
            } else {
                Path tmp = Files.createTempDirectory("dgjcode-");
                Path f = tmp.resolve(t.simpleClassName() + ".java");
                Files.writeString(f, t.area().getText(), StandardCharsets.UTF_8);
                launch(new Launch(t.title(), List.of(f), tmp.resolve("classes"), t.javaClassName(), tmp));
            }
        } catch (IOException ex) {
            error("Gagal menyiapkan run", ex);
        }
    }

    private boolean checkIdle() {
        if (busy || runner.isRunning()) {
            info("Program masih berjalan. Tekan Stop terlebih dahulu.");
            return false;
        }
        return true;
    }

    private void launch(Launch l) {
        busy = true;
        stopRequested = false;
        setRunUi(true);
        showOutputHalf();
        output.clear();
        output.setTitle("Output \u2014 " + l.title());
        output.info("Mengompilasi " + l.sources().size() + " file...");
        status.startProgress((l.mainClass() == null ? "Build: " : "Mengompilasi: ") + l.title());
        Thread worker = new Thread(() -> doLaunch(l), "dgjcode-build");
        worker.setDaemon(true);
        worker.start();
    }

    /** Berjalan di thread latar belakang. */
    private void doLaunch(Launch l) {
        try {
            JavaCompilerService.Result r = JavaCompilerService.compile(l.sources(), l.outDir());
            printDiagnostics(r);
            if (!r.success()) {
                output.error("BUILD GAGAL. Perbaiki error di atas, lalu coba lagi.");
                finish();
                return;
            }
            output.success("BUILD BERHASIL (" + seconds(r.millis()) + ")");
            if (l.mainClass() == null) {
                finish();
                return;
            }
            if (stopRequested) {
                output.info("Dibatalkan.");
                finish();
                return;
            }
            output.info("Menjalankan " + l.mainClass() + " ...");
            output.print("\n");
            status.startProgress("Sedang berjalan: " + l.mainClass());
            output.setRunning(true);
            runner.start(l.outDir(), l.mainClass(), l.workDir(), new ProgramRunner.Listener() {
                @Override
                public void out(String text) {
                    output.print(text);
                }

                @Override
                public void err(String text) {
                    output.printErr(text);
                }

                @Override
                public void exited(int code, long millis) {
                    output.print("\n");
                    if (stopRequested) {
                        output.info("Program dihentikan oleh pengguna.");
                    } else if (code == 0) {
                        output.success("Program selesai (" + seconds(millis) + ").");
                    } else {
                        output.error("Program berhenti dengan kode keluar " + code + ".");
                    }
                    finish();
                }
            });
        } catch (IOException | RuntimeException ex) {
            output.error("Gagal: " + ex.getMessage());
            finish();
        }
    }

    private void printDiagnostics(JavaCompilerService.Result r) {
        for (Diagnostic<? extends JavaFileObject> d : r.diagnostics()) {
            String file = d.getSource() == null ? "" : new File(d.getSource().getName()).getName();
            String loc = file.isEmpty() ? "" : file + ":" + d.getLineNumber() + ": ";
            String msg = d.getMessage(Locale.ROOT);
            switch (d.getKind()) {
                case ERROR -> output.error(loc + "error: " + msg);
                case WARNING, MANDATORY_WARNING -> output.info(loc + "peringatan: " + msg);
                default -> output.info(loc + msg);
            }
        }
    }

    private void finish() {
        busy = false;
        status.stopProgress();
        output.setRunning(false);
        SwingUtilities.invokeLater(() -> setRunUi(false));
    }

    private void stop() {
        if (!busy && !runner.isRunning()) {
            output.info("Tidak ada program yang sedang berjalan.");
            return;
        }
        stopRequested = true;
        runner.stop();
    }

    private void sendInput(String line) {
        if (!runner.isRunning()) {
            return;
        }
        output.echoInput(line);
        runner.send(line);
    }

    private void setRunUi(boolean running) {
        buildButton.setEnabled(!running);
        runButton.setEnabled(!running);
        runThisButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }

    private static String seconds(long millis) {
        return String.format(ID, "%.1f dtk", millis / 1000.0);
    }

    // ===================== Tampilan =====================

    /** Saat Run: panel Output naik sampai setengah tinggi area kerja. */
    private void showOutputHalf() {
        output.setVisible(true);
        vertical.setDividerLocation(0.5);
        vertical.revalidate();
    }

    private void toggleProjects() {
        if (projectsPanel.isVisible()) {
            savedProjectsDivider = horizontal.getDividerLocation();
            projectsPanel.setVisible(false);
        } else {
            projectsPanel.setVisible(true);
            horizontal.setDividerLocation(savedProjectsDivider);
        }
        horizontal.revalidate();
    }

    private void toggleOutput() {
        if (output.isVisible()) {
            savedOutputDivider = vertical.getDividerLocation();
            output.setVisible(false);
        } else {
            output.setVisible(true);
            if (savedOutputDivider > 0) {
                vertical.setDividerLocation(savedOutputDivider);
            } else {
                vertical.resetToPreferredSizes();
            }
        }
        vertical.revalidate();
    }

    private void resetLayout() {
        projectsPanel.setVisible(true);
        output.setVisible(true);
        horizontal.setDividerLocation(260);
        SwingUtilities.invokeLater(() -> vertical.setDividerLocation(Math.max(200, vertical.getHeight() - 220)));
    }

    private void openThemeDialog() {
        new ThemeDialog(window(), settings).setVisible(true);
    }

    private void openSettings() {
        new SettingsDialog(window(), settings, tabs::restyleAll).setVisible(true);
    }

    private void showShortcuts() {
        JOptionPane.showMessageDialog(this, "<html><table cellpadding='3'>"
                + "<tr><td><b>Ctrl+S</b></td><td>Simpan</td></tr>"
                + "<tr><td><b>Ctrl+Z / Ctrl+Y</b></td><td>Undo / Redo</td></tr>"
                + "<tr><td><b>Ctrl+F / Ctrl+H</b></td><td>Cari / Ganti</td></tr>"
                + "<tr><td><b>Alt+Shift+F</b></td><td>Rapikan kode</td></tr>"
                + "<tr><td><b>Ctrl+/</b></td><td>Komentar baris</td></tr>"
                + "<tr><td><b>Ctrl+G</b></td><td>Pergi ke baris</td></tr>"
                + "<tr><td><b>F6</b></td><td>Run proyek</td></tr>"
                + "<tr><td><b>Shift+F6</b></td><td>Run This (class yang dibuka)</td></tr>"
                + "<tr><td><b>Ctrl+F2</b></td><td>Stop</td></tr>"
                + "<tr><td><b>F11</b></td><td>Build proyek</td></tr>"
                + "<tr><td><b>F2</b></td><td>Rename item di Projects (dengan refactor)</td></tr>"
                + "<tr><td><b>Ctrl+Shift+N</b></td><td>Proyek baru</td></tr>"
                + "</table></html>", "Daftar Shortcut", JOptionPane.PLAIN_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                AppInfo.NAME + " " + AppInfo.VERSION_FULL + "\n" + AppInfo.FULL_NAME
                        + "\nOleh " + AppInfo.VENDOR + "\nJava " + Runtime.version(),
                "Tentang", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStatus() {
        if (tabs == null) {
            return;
        }
        status.update(tabs.current());
        if (emptyState != null && projectTree != null) {
            emptyState.setNewFileVisible(!projectTree.projects().isEmpty());
        }
        editorLayout.show(editorCards, tabs.getTabCount() == 0 ? CARD_EMPTY : CARD_TABS);
    }

    // ===================== Dialog bantu =====================

    private String ask(String title, String message, String initial) {
        Object v = JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE,
                null, null, initial);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        if (s.endsWith(".java")) {
            s = s.substring(0, s.length() - 5);
        }
        return s.isEmpty() ? null : s;
    }

    private boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, AppInfo.NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String title, Exception ex) {
        JOptionPane.showMessageDialog(this, title + ":\n" + ex.getMessage(), AppInfo.NAME,
                JOptionPane.ERROR_MESSAGE);
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }
}
