package id.dg.jcode.pro;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Operasi file proyek: buat, salin, pindah, hapus, rename + refactor sederhana. */
public final class ProjectOps {

    public static final String IDENT = "[A-Za-z_$][A-Za-z0-9_$]*";
    public static final String PKG = IDENT + "(\\." + IDENT + ")*";
    private static final Pattern PACKAGE_LINE =
            Pattern.compile("(?m)^[ \\t]*package[ \\t]+[\\w.$]+[ \\t]*;[ \\t]*(\\r?\\n)?");

    private ProjectOps() {
    }

    public static String packageOf(Project p, Path dir) {
        Path rel = p.srcDir().relativize(dir.toAbsolutePath().normalize());
        return rel.toString().replace('\\', '.').replace('/', '.');
    }

    public static String simpleName(Path file) {
        String n = file.getFileName().toString();
        return n.endsWith(".java") ? n.substring(0, n.length() - 5) : n;
    }

    public static String fqName(Project p, Path javaFile) {
        String pkg = packageOf(p, javaFile.getParent());
        return pkg.isEmpty() ? simpleName(javaFile) : pkg + "." + simpleName(javaFile);
    }

    public static Path dirOfPackage(Project p, String pkg) {
        Path d = p.srcDir();
        if (!pkg.isBlank()) {
            for (String part : pkg.split("\\.")) {
                d = d.resolve(part);
            }
        }
        return d;
    }

    public static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".java")).toList();
        }
    }

    public static Path createClass(Project p, Path dir, String name) throws IOException {
        Path target = dir.resolve(name + ".java");
        if (Files.exists(target)) {
            throw new IOException(target.getFileName() + " sudah ada.");
        }
        Files.createDirectories(dir);
        Files.writeString(target, Project.classTemplate(packageOf(p, dir), name), StandardCharsets.UTF_8);
        return target;
    }

    public static Path createPackage(Project p, String pkg) throws IOException {
        Path d = dirOfPackage(p, pkg);
        Files.createDirectories(d);
        return d;
    }

    public static void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> s = Files.walk(path)) {
            for (Path f : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(f);
            }
        }
    }

    public static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> s = Files.walk(from)) {
            for (Path f : s.toList()) {
                Path t = to.resolve(from.relativize(f));
                if (Files.isDirectory(f)) {
                    Files.createDirectories(t);
                } else {
                    Files.createDirectories(t.getParent());
                    Files.copy(f, t);
                }
            }
        }
    }

    public static String withPackage(String text, String pkg) {
        Matcher m = PACKAGE_LINE.matcher(text);
        if (m.find()) {
            String repl = pkg.isEmpty() ? "" : "package " + pkg + ";\n";
            return text.substring(0, m.start()) + repl + text.substring(m.end());
        }
        return pkg.isEmpty() ? text : "package " + pkg + ";\n\n" + text;
    }

    /** Menyesuaikan baris "package" semua file di dalam dir dengan lokasi foldernya. */
    public static void fixPackages(Project p, Path dir) throws IOException {
        for (Path f : javaFiles(dir)) {
            String t = read(f);
            String n = withPackage(t, packageOf(p, f.getParent()));
            if (!n.equals(t)) {
                write(f, n);
            }
        }
    }

    public static Path pasteClass(Project target, Path file, Path targetDir, boolean move) throws IOException {
        Path dest = targetDir.resolve(file.getFileName());
        if (dest.equals(file)) {
            throw new IOException("File sudah berada di paket ini.");
        }
        if (Files.exists(dest)) {
            throw new IOException(dest.getFileName() + " sudah ada di paket tujuan.");
        }
        if (move) {
            Files.move(file, dest);
        } else {
            Files.copy(file, dest);
        }
        write(dest, withPackage(read(dest), packageOf(target, targetDir)));
        return dest;
    }

    public static Path pastePackage(Project target, Path dir, Path targetParent, boolean move) throws IOException {
        Path dest = targetParent.resolve(dir.getFileName().toString());
        if (dest.startsWith(dir)) {
            throw new IOException("Paket tidak bisa ditempel ke dalam dirinya sendiri.");
        }
        if (Files.exists(dest)) {
            throw new IOException("Paket \"" + dest.getFileName() + "\" sudah ada di tujuan.");
        }
        if (move) {
            Files.move(dir, dest);
        } else {
            copyTree(dir, dest);
        }
        fixPackages(target, dest);
        return dest;
    }

    /** Rename class + ganti semua referensi nama class di proyek. */
    public static Path renameClass(Project p, Path file, String newName) throws IOException {
        String old = simpleName(file);
        Path dest = file.resolveSibling(newName + ".java");
        boolean caseOnly = dest.toString().equalsIgnoreCase(file.toString());
        if (Files.exists(dest) && !caseOnly) {
            throw new IOException(dest.getFileName() + " sudah ada.");
        }
        Pattern word = Pattern.compile("(?<![\\w$])" + Pattern.quote(old) + "(?![\\w$])");
        for (Path f : javaFiles(p.srcDir())) {
            replaceAll(f, word, newName);
        }
        Files.move(file, dest);
        return dest;
    }

    /** Rename paket + ganti baris package/import yang memakainya. */
    public static Path renamePackage(Project p, Path dir, String newPkg) throws IOException {
        String old = packageOf(p, dir);
        Path dest = dirOfPackage(p, newPkg);
        if (Files.exists(dest)) {
            throw new IOException("Paket \"" + newPkg + "\" sudah ada.");
        }
        if (dest.startsWith(dir)) {
            throw new IOException("Nama paket baru tidak boleh berada di dalam paket lama.");
        }
        Files.createDirectories(dest.getParent());
        Files.move(dir, dest);
        Pattern ref = Pattern.compile("(?<![\\w$.])" + Pattern.quote(old) + "(?=[.;\\s])");
        for (Path f : javaFiles(p.srcDir())) {
            replaceAll(f, ref, newPkg);
        }
        Path d = dir.getParent();
        while (d != null && d.startsWith(p.srcDir()) && !d.equals(p.srcDir()) && isEmptyDir(d)) {
            Files.delete(d);
            d = d.getParent();
        }
        return dest;
    }

    public static Project copyProject(Project p, Path newRoot, String newName) throws IOException {
        if (Files.exists(newRoot)) {
            throw new IOException("Folder \"" + newRoot + "\" sudah ada.");
        }
        copyTree(p.root(), newRoot);
        Project np = new Project(newName, newRoot, p.mainClass());
        np.save();
        return np;
    }

    public static void openLocation(Path path) throws IOException {
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (win) {
            new ProcessBuilder("explorer.exe", "/select," + path.toAbsolutePath()).start();
        } else {
            Desktop.getDesktop().open((Files.isDirectory(path) ? path : path.getParent()).toFile());
        }
    }

    private static boolean isEmptyDir(Path d) throws IOException {
        try (Stream<Path> s = Files.list(d)) {
            return s.findAny().isEmpty();
        }
    }

    private static void replaceAll(Path f, Pattern pattern, String replacement) throws IOException {
        String t = read(f);
        String n = pattern.matcher(t).replaceAll(Matcher.quoteReplacement(replacement));
        if (!n.equals(t)) {
            write(f, n);
        }
    }

    private static String read(Path f) throws IOException {
        return Files.readString(f, StandardCharsets.UTF_8);
    }

    private static void write(Path f, String s) throws IOException {
        Files.writeString(f, s, StandardCharsets.UTF_8);
    }
}
