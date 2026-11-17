package id.dg.jcode.pro;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Proyek Java sederhana:
 *   NamaProyek/
 *     project.properties   (nama + main class)
 *     src/paket/Main.java
 *     build/classes/       (diisi saat build di Tahap 3)
 */
public record Project(String name, Path root, String mainClass) {

    public static final String FILE = "project.properties";

    public Path srcDir() {
        return root.resolve("src");
    }

    public Path buildDir() {
        return root.resolve("build").resolve("classes");
    }

    public Path mainFile() {
        return srcDir().resolve(mainClass.replace('.', '/') + ".java");
    }

    public static Project load(Path root) throws IOException {
        Path dir = root.toAbsolutePath().normalize();
        Properties p = new Properties();
        Path file = dir.resolve(FILE);
        if (Files.isRegularFile(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        String name = p.getProperty("name", String.valueOf(dir.getFileName()));
        Project project = new Project(name, dir, p.getProperty("main.class", "Main"));
        if (!Files.isRegularFile(file)) {
            Files.createDirectories(project.srcDir());
            project.save();
        }
        return project;
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("name", name);
        p.setProperty("main.class", mainClass);
        try (Writer w = Files.newBufferedWriter(root.resolve(FILE), StandardCharsets.UTF_8)) {
            p.store(w, "DG J-Code project");
        }
    }

    /** Membuat proyek baru beserta file main class. */
    public static Project create(Path parent, String name, String pkg, String mainName) throws IOException {
        Path root = parent.resolve(name).toAbsolutePath().normalize();
        if (Files.exists(root)) {
            try (Stream<Path> s = Files.list(root)) {
                if (s.findAny().isPresent()) {
                    throw new IOException("Folder \"" + root + "\" sudah ada dan tidak kosong.");
                }
            }
        }
        String fq = pkg.isBlank() ? mainName : pkg + "." + mainName;
        Project project = new Project(name, root, fq);
        Files.createDirectories(project.mainFile().getParent());
        Files.writeString(project.mainFile(), mainTemplate(pkg, mainName, name), StandardCharsets.UTF_8);
        project.save();
        return project;
    }

    public static String classTemplate(String pkg, String className) {
        String header = pkg.isBlank() ? "" : "package " + pkg + ";\n\n";
        return header + "public class " + className + " {\n\n}\n";
    }

    private static String mainTemplate(String pkg, String className, String projectName) {
        String header = pkg.isBlank() ? "" : "package " + pkg + ";\n\n";
        return header
                + "public class " + className + " {\n"
                + "\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Halo dari " + projectName.replace("\"", "") + "!\");\n"
                + "    }\n"
                + "}\n";
    }
}
