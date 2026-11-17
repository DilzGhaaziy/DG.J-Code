package id.dg.jcode.run;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/** Menjalankan program pengguna sebagai proses java terpisah dari runtime bundel. */
public final class ProgramRunner {

    public interface Listener {
        void out(String text);

        void err(String text);

        void exited(int code, long millis);
    }

    private volatile Process process;
    private volatile Writer stdin;

    public boolean isRunning() {
        Process p = process;
        return p != null && p.isAlive();
    }

    public static Path javaExecutable() {
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return Path.of(System.getProperty("java.home"), "bin", win ? "java.exe" : "java");
    }

    public synchronized void start(Path classpath, String mainClass, Path workDir, Listener listener) throws IOException {
        if (isRunning()) {
            throw new IOException("Masih ada program yang berjalan.");
        }
        ProcessBuilder pb = new ProcessBuilder(javaExecutable().toString(),
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-cp", classpath.toString(), mainClass);
        pb.directory(workDir.toFile());
        long start = System.nanoTime();
        Process p = pb.start();
        process = p;
        stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
        Thread out = pump(p.getInputStream(), listener::out, "program-stdout");
        Thread err = pump(p.getErrorStream(), listener::err, "program-stderr");
        Thread waiter = new Thread(() -> {
            int code = -1;
            try {
                code = p.waitFor();
                out.join(2000);
                err.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            listener.exited(code, (System.nanoTime() - start) / 1_000_000L);
        }, "program-waiter");
        waiter.setDaemon(true);
        waiter.start();
    }

    /** Mengirim satu baris input ke program (dibaca oleh Scanner). */
    public void send(String line) {
        Writer w = stdin;
        if (w == null || !isRunning()) {
            return;
        }
        try {
            w.write(line);
            w.write(System.lineSeparator());
            w.flush();
        } catch (IOException ignored) {
            // program sudah berhenti
        }
    }

    public void stop() {
        Process p = process;
        if (p == null) {
            return;
        }
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }

    private static Thread pump(InputStream in, Consumer<String> sink, String name) {
        Thread t = new Thread(() -> {
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                char[] buf = new char[2048];
                int n;
                while ((n = r.read(buf)) != -1) {
                    sink.accept(new String(buf, 0, n));
                }
            } catch (IOException ignored) {
                // stream tertutup
            }
        }, name);
        t.setDaemon(true);
        t.start();
        return t;
    }
}
