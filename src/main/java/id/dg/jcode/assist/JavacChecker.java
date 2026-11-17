package id.dg.jcode.assist;

import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Menjalankan javac pada teks editor (langsung dari memori, tanpa menyimpan file)
 * dan mengembalikan daftar error.
 *
 * Hanya dipanggil dari SATU thread latar belakang (lihat CodeAssist), jadi
 * file manager boleh dipakai ulang tanpa sinkronisasi.
 */
final class JavacChecker {

    /** Error mentah dari javac. Posisi = offset karakter di dalam teks. */
    record Diag(String code, String message, int pos, int start, int end) {
    }

    static final Pattern PACKAGE =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:(?:final|abstract|static|strictfp)\\s+)*"
                    + "(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");

    private static final int MAX_DIAGNOSTICS = 50;

    private final JavaCompiler compiler;
    private StandardJavaFileManager fileManager;

    JavacChecker() {
        JavaCompiler c;
        try {
            c = ToolProvider.getSystemJavaCompiler();
        } catch (RuntimeException | LinkageError e) {
            c = null; // modul jdk.compiler tidak ada -> fitur koreksi dimatikan diam-diam
        }
        compiler = c;
    }

    boolean available() {
        return compiler != null;
    }

    /**
     * @param source isi editor
     * @param file   lokasi file di disk (null untuk editor tanpa file, misalnya Praktek)
     */
    List<Diag> check(String source, Path file) {
        if (compiler == null) {
            return List.of();
        }
        String pkg = find(PACKAGE, source);
        // Nama file "virtual" harus sama dengan nama class public, kalau tidak javac protes.
        String simpleName = file != null ? baseName(file) : orElse(find(PUBLIC_TYPE, source), "Main");
        String relative = (pkg == null ? "" : pkg.replace('.', '/') + "/") + simpleName;
        SourceUnit unit = new SourceUnit(relative, source);

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        try {
            StandardJavaFileManager fm = fileManager();
            // sourcepath = folder akar paket, supaya class lain di proyek yang sama dikenali
            // (tanpa ini, memakai class buatan sendiri akan dianggap "cannot find symbol").
            Path root = sourceRoot(file, pkg);
            fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, root == null ? List.of() : List.of(root));
            fm.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of());

            List<String> options = List.of(
                    "-proc:none",          // tanpa annotation processing (lebih cepat)
                    "-implicit:none",
                    "-Xlint:none",
                    "-nowarn",
                    // Biasanya javac berhenti setelah salah ketik tanda baca (syntax error).
                    // Opsi ini membuatnya tetap memeriksa tipe & variabel, jadi lebih banyak
                    // kesalahan terlihat sekaligus, mirip IDE besar.
                    "-XDshould-stop.ifError=FLOW");

            JavacTask task = (JavacTask) compiler.getTask(
                    Writer.nullWriter(), fm, collector, options, null, List.of(unit));
            task.analyze(); // parse + cek tipe + cek alur, TANPA membuat file .class
        } catch (IOException | RuntimeException ex) {
            // Untuk kode yang sangat rusak javac kadang melempar exception internal.
            // Abaikan saja: diagnostik yang sudah terkumpul tetap dipakai.
        }

        List<Diag> out = new ArrayList<>();
        URI unitUri = unit.toUri();
        for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            if (d.getSource() == null || !unitUri.equals(d.getSource().toUri())) continue; // error file lain diabaikan
            out.add(new Diag(d.getCode(), d.getMessage(Locale.ENGLISH),
                    (int) d.getPosition(), (int) d.getStartPosition(), (int) d.getEndPosition()));
            if (out.size() >= MAX_DIAGNOSTICS) break;
        }
        return out;
    }

    private StandardJavaFileManager fileManager() {
        if (fileManager == null) {
            fileManager = compiler.getStandardFileManager(null, Locale.ENGLISH, StandardCharsets.UTF_8);
        }
        return fileManager;
    }

    /** src/com/contoh/Main.java + package com.contoh  ->  src */
    private static Path sourceRoot(Path file, String pkg) {
        if (file == null) return null;
        Path dir = file.toAbsolutePath().getParent();
        if (pkg != null) {
            for (String ignored : pkg.split("\\.")) {
                if (dir == null) return null;
                dir = dir.getParent();
            }
        }
        return dir != null && Files.isDirectory(dir) ? dir : null;
    }

    static String baseName(Path file) {
        Path name = file.getFileName();
        String n = name == null ? "Main" : name.toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String find(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String orElse(String a, String b) {
        return a != null ? a : b;
    }

    /** File Java "virtual" yang isinya diambil dari memori, bukan dari disk. */
    private static final class SourceUnit extends SimpleJavaFileObject {
        private final String code;

        SourceUnit(String relativePath, String code) {
            super(uri(relativePath), Kind.SOURCE);
            this.code = code;
        }

        private static URI uri(String relativePath) {
            try {
                return new URI("string", null, "/" + relativePath + Kind.SOURCE.extension, null);
            } catch (URISyntaxException e) {
                return URI.create("string:///Main.java");
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
