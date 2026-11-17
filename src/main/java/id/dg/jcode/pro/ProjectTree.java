package id.dg.jcode.pro;

import id.dg.jcode.ui.icons.TreeIcons;
import id.dg.jcode.ui.theme.ThemeManager;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/** Pohon proyek: Project > src > package > class, dengan ikon, label jenis, dan menu klik kanan. */
public final class ProjectTree extends JTree {

    public record Node(TreeIcons.Kind kind, Path path, Project project, String label) {
        @Override
        public String toString() {
            return label;
        }

        public boolean isDefaultPackage() {
            return kind == TreeIcons.Kind.PACKAGE && path.equals(project.srcDir());
        }
    }

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Projects");
    private final DefaultTreeModel model = new DefaultTreeModel(rootNode);
    private final List<Project> projects = new ArrayList<>();
    private final Set<Path> seenProjects = new HashSet<>();
    private final Consumer<Node> onOpen;
    private Function<Node, JPopupMenu> popupProvider = n -> null;
    private Consumer<Node> renameHandler = n -> { };

    public ProjectTree(Consumer<Node> onOpen) {
        this.onOpen = onOpen;
        setModel(model);
        setRootVisible(false);
        setShowsRootHandles(true);
        setRowHeight(24);
        setCellRenderer(new Renderer());
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    openSelected();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybePopup(e);
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openNode");
        getActionMap().put("openNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelected();
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "renameNode");
        getActionMap().put("renameNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Node n = selectedNode();
                if (n != null) {
                    renameHandler.accept(n);
                }
            }
        });
    }

    public void setPopupProvider(Function<Node, JPopupMenu> provider) {
        popupProvider = provider;
    }

    public void setRenameHandler(Consumer<Node> handler) {
        renameHandler = handler;
    }

    private void maybePopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = getRowForLocation(e.getX(), e.getY());
        if (row < 0) {
            return;
        }
        setSelectionRow(row);
        Node n = selectedNode();
        JPopupMenu menu = n == null ? null : popupProvider.apply(n);
        if (menu != null) {
            menu.show(this, e.getX(), e.getY());
        }
    }

    public void addProject(Project project) {
        projects.removeIf(p -> p.root().equals(project.root()));
        projects.add(project);
        refresh();
    }

    public void removeProject(Path root) {
        projects.removeIf(p -> p.root().equals(root));
        refresh();
    }

    public void replaceProject(Path oldRoot, Project project) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).root().equals(oldRoot)) {
                projects.set(i, project);
            }
        }
        refresh();
    }

    public List<Project> projects() {
        return List.copyOf(projects);
    }

    public void refresh() {
        Set<String> expanded = new HashSet<>();
        for (int i = 0; i < getRowCount(); i++) {
            TreePath tp = getPathForRow(i);
            Node n = nodeOf(tp);
            if (n != null && isExpanded(tp)) {
                expanded.add(key(n));
            }
        }
        rootNode.removeAllChildren();
        for (Project p : projects) {
            rootNode.add(buildProject(p));
        }
        model.reload();
        for (int i = 0; i < getRowCount(); i++) {
            Node n = nodeOf(getPathForRow(i));
            if (n == null) {
                continue;
            }
            boolean isNew = !seenProjects.contains(n.project().root());
            if (expanded.contains(key(n)) || (isNew && n.kind() != TreeIcons.Kind.CLASS)) {
                expandRow(i);
            }
        }
        for (Project p : projects) {
            seenProjects.add(p.root());
        }
    }

    private DefaultMutableTreeNode buildProject(Project p) {
        DefaultMutableTreeNode pn = new DefaultMutableTreeNode(new Node(TreeIcons.Kind.PROJECT, p.root(), p, p.name()));
        Path src = p.srcDir();
        DefaultMutableTreeNode sn = new DefaultMutableTreeNode(new Node(TreeIcons.Kind.SOURCE, src, p, "src"));
        if (Files.isDirectory(src)) {
            List<Path> rootFiles = files(src);
            if (!rootFiles.isEmpty()) {
                DefaultMutableTreeNode def = new DefaultMutableTreeNode(
                        new Node(TreeIcons.Kind.PACKAGE, src, p, "<default package>"));
                addFiles(def, rootFiles, p);
                sn.add(def);
            }
            for (Path dir : packageDirs(src)) {
                DefaultMutableTreeNode pk = new DefaultMutableTreeNode(
                        new Node(TreeIcons.Kind.PACKAGE, dir, p, ProjectOps.packageOf(p, dir)));
                addFiles(pk, files(dir), p);
                sn.add(pk);
            }
        }
        pn.add(sn);
        return pn;
    }

    private static void addFiles(DefaultMutableTreeNode parent, List<Path> files, Project p) {
        for (Path f : files) {
            boolean java = f.getFileName().toString().endsWith(".java");
            parent.add(new DefaultMutableTreeNode(new Node(
                    java ? TreeIcons.Kind.CLASS : TreeIcons.Kind.FILE, f, p,
                    java ? ProjectOps.simpleName(f) : f.getFileName().toString())));
        }
    }

    private static List<Path> packageDirs(Path src) {
        try (Stream<Path> s = Files.walk(src)) {
            return s.filter(Files::isDirectory)
                    .filter(d -> !d.equals(src))
                    .filter(d -> !d.getFileName().toString().startsWith("."))
                    .sorted(Comparator.comparing(d -> src.relativize(d).toString().toLowerCase()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<Path> files(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(f -> !f.getFileName().toString().startsWith("."))
                    .sorted(Comparator.comparing(f -> f.getFileName().toString().toLowerCase()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String key(Node n) {
        return n.kind() + "|" + n.path();
    }

    private static Node nodeOf(TreePath tp) {
        if (tp != null && tp.getLastPathComponent() instanceof DefaultMutableTreeNode d
                && d.getUserObject() instanceof Node n) {
            return n;
        }
        return null;
    }

    public Node selectedNode() {
        return nodeOf(getSelectionPath());
    }

    public Project selectedProject() {
        Node n = selectedNode();
        if (n != null) {
            return n.project();
        }
        return projects.size() == 1 ? projects.get(0) : null;
    }

    /** Folder tujuan (untuk class/paket baru atau paste). */
    public static Path dirFor(Node n) {
        return switch (n.kind()) {
            case PROJECT, SOURCE -> n.project().srcDir();
            case PACKAGE -> n.path();
            default -> n.path().getParent();
        };
    }

    public Path selectedDirectory() {
        Node n = selectedNode();
        if (n != null) {
            return dirFor(n);
        }
        Project p = selectedProject();
        return p == null ? null : p.srcDir();
    }

    private void openSelected() {
        Node n = selectedNode();
        if (n != null && (n.kind() == TreeIcons.Kind.CLASS || n.kind() == TreeIcons.Kind.FILE)) {
            onOpen.accept(n);
        }
    }

    private static final class Renderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode d && d.getUserObject() instanceof Node n) {
                setIcon(TreeIcons.of(n.kind()));
                String suffix = switch (n.kind()) {
                    case PROJECT -> "(Project)";
                    case SOURCE -> "(Source)";
                    case PACKAGE -> "(Package)";
                    case CLASS -> "(Class)";
                    default -> "";
                };
                Color muted = sel && getTextSelectionColor() != null
                        ? getTextSelectionColor() : ThemeManager.palette().textMuted();
                String label = n.label().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                setText("<html>" + label + (suffix.isEmpty() ? "" : "&nbsp;<font color='"
                        + ThemeManager.toHex(muted) + "'>" + suffix + "</font>") + "</html>");
                setToolTipText(n.path().toString());
            }
            return this;
        }
    }
}
