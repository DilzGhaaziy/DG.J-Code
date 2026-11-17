package id.dg.jcode.practice;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Compile kode dengan javax.tools, lalu jalankan sebagai proses java terpisah. */
final class PracticeRunner {
    interface Listener {
        void out(String s);
        void err(String s);
        void info(String s);
        void finished();
    }

    private static final Pattern PKG = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern PUB = Pattern.compile("\\bpublic\\s+(?:final\\s+)?class\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern CLS = Pattern.compile("\\bclass\\s+([A-Za-z_$][\\w$]*)");

    private volatile Process process;
    private volatile BufferedWriter stdin;
    private volatile boolean stopped;

    boolean isRunning() {
        Process p = process;
        return p != null && p.isAlive();
    }

    /** Dipanggil dari thread latar belakang. */
    void compileAndRun(String code, Listener l) {
        stopped = false;
        try {
            Path dir = Files.createTempDirectory("dgjcode-praktek");
            String pkg = find(PKG, code);
            String name = Optional.ofNullable(find(PUB, code)).orElse(Optional.ofNullable(find(CLS, code)).orElse("Main"));
            Path src = dir.resolve(name + ".java");
            Files.writeString(src, code, StandardCharsets.UTF_8);
            Path out = Files.createDirectories(dir.resolve("out"));

            l.info("Mengompilasi...\n");
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc == null) { l.err("Compiler Java tidak ditemukan di runtime aplikasi.\n"); return; }
            DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
            boolean ok;
            try (StandardJavaFileManager fm = jc.getStandardFileManager(diags, Locale.ENGLISH, StandardCharsets.UTF_8)) {
                ok = jc.getTask(null, fm, diags,
                        List.of("-d", out.toString(), "-encoding", "UTF-8", "-proc:none"),
                        null, fm.getJavaFileObjects(src)).call();
            }
            if (!ok) {
                l.err("Kode belum bisa dijalankan karena masih ada kesalahan:\n");
                for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                    if (d.getKind() != Diagnostic.Kind.ERROR) continue;
                    l.err("  Baris " + d.getLineNumber() + ": " + friendly(d.getCode(), d.getMessage(Locale.ENGLISH)) + "\n");
                }
                return;
            }

            String main = (pkg == null ? "" : pkg + ".") + name;
            String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
            Path java = Path.of(System.getProperty("java.home"), "bin", exe);
            ProcessBuilder pb = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8",
                    "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", out.toString(), main);
            pb.directory(dir.toFile());
            l.info("Menjalankan " + main + "...\n\n");
            long t0 = System.currentTimeMillis();
            Process p = pb.start();
            process = p;
            stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
            StringBuilder errText = new StringBuilder();
            Thread to = pump(p.getInputStream(), l::out, null);
            Thread te = pump(p.getErrorStream(), l::err, errText);
            int code2 = p.waitFor();
            to.join();
            te.join();
            if (stopped) {
                l.info("\nProgram dihentikan.\n");
            } else if (code2 != 0) {
                String hint = runtimeHint(errText.toString());
                if (hint != null) l.err("\nPenjelasan: " + hint + "\n");
                l.info("\nProgram selesai dengan kode " + code2 + ".\n");
            } else {
                l.info("\nSelesai (" + (System.currentTimeMillis() - t0) + " ms).\n");
            }
        } catch (IOException | InterruptedException ex) {
            l.err("Gagal menjalankan program: " + ex.getMessage() + "\n");
        } finally {
            process = null;
            stdin = null;
            l.finished();
        }
    }

    void sendInput(String line) {
        BufferedWriter w = stdin;
        if (w == null) return;
        try {
            w.write(line);
            w.newLine();
            w.flush();
        } catch (IOException ignored) { }
    }

    void stop() {
        Process p = process;
        if (p != null) {
            stopped = true;
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
        }
    }

    private static Thread pump(InputStream in, java.util.function.Consumer<String> sink, StringBuilder copy) {
        Thread t = new Thread(() -> {
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                char[] buf = new char[1024];
                int n;
                while ((n = r.read(buf)) > 0) {
                    String s = new String(buf, 0, n);
                    if (copy != null) copy.append(s);
                    sink.accept(s);
                }
            } catch (IOException ignored) { }
        }, "dgjcode-praktek-io");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Pesan compile yang lebih ramah untuk pemula. */
    private static String friendly(String code, String msg) {
        String first = msg.lines().findFirst().orElse(msg);
        if (code == null) return first;
        if (code.startsWith("compiler.err.expected")) {
            if (first.startsWith("';'")) return "Sepertinya titik koma (;) terlupa.";
            if (first.startsWith("')'")) return "Kurung tutup ) belum ada.";
            return "Ada tanda yang terlupa: " + first;
        }
        return switch (code) {
            case "compiler.err.cant.resolve.location", "compiler.err.cant.resolve" ->
                    "Ada nama yang tidak dikenal Java (salah ketik atau belum dibuat/di-import). " + first;
            case "compiler.err.premature.eof" -> "Kurung kurawal } kurang di akhir kode.";
            case "compiler.err.unclosed.str.lit" -> "Teks belum ditutup dengan tanda kutip (\").";
            case "compiler.err.prob.found.req" -> "Jenis datanya tidak cocok. " + first;
            case "compiler.err.not.stmt" -> "Baris ini belum menjadi perintah yang lengkap.";
            case "compiler.err.var.might.not.have.been.initialized" -> "Variabel dipakai sebelum diberi nilai.";
            case "compiler.err.missing.ret.stmt" -> "Method ini perlu return di akhirnya.";
            default -> first;
        };
    }

    /** Penjelasan error runtime yang paling sering ditemui pemula. */
    private static String runtimeHint(String err) {
        if (err.contains("ArithmeticException")) return "ada pembagian dengan nol.";
        if (err.contains("ArrayIndexOutOfBoundsException") || err.contains("StringIndexOutOfBoundsException")
                || err.contains("IndexOutOfBoundsException"))
            return "indeks di luar batas. Ingat, indeks dimulai dari 0 sampai panjang - 1.";
        if (err.contains("NullPointerException")) return "ada objek yang masih null (belum dibuat) tapi sudah dipakai.";
        if (err.contains("InputMismatchException")) return "input tidak sesuai jenisnya, misalnya huruf diketik saat diminta angka.";
        if (err.contains("NumberFormatException")) return "teks tidak bisa diubah menjadi angka.";
        if (err.contains("NoSuchElementException")) return "program menunggu input, tetapi input sudah habis.";
        if (err.contains("StackOverflowError")) return "method memanggil dirinya sendiri tanpa henti (rekursi tanpa batas).";
        return err.isBlank() ? null : "program berhenti karena error saat berjalan (lihat pesan merah di atas).";
    }

    private static String find(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
