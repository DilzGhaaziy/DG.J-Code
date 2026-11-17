package id.dg.jcode;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pencatat error. Aplikasi hasil jpackage tidak punya jendela konsol, jadi tanpa kelas ini
 * error saat start hilang begitu saja dan aplikasi terlihat "tidak terjadi apa-apa".
 *
 * Semua isi System.out / System.err ikut ditulis ke:
 *   %LOCALAPPDATA%\DGJCode\logs\dgjcode.log          (start terakhir)
 *   %LOCALAPPDATA%\DGJCode\logs\dgjcode-sebelumnya.log (start sebelumnya)
 */
public final class CrashLog {

    private static final AtomicBoolean DIALOG_SHOWN = new AtomicBoolean();
    private static volatile Path logFile;

    private CrashLog() {
    }

    public static Path logDir() {
        String local = System.getenv("LOCALAPPDATA");
        Path base = local != null && !local.isBlank()
                ? Path.of(local, "DGJCode")
                : Path.of(System.getProperty("user.home"), ".dgjcode");
        return base.resolve("logs");
    }

    public static Path logFile() {
        return logFile;
    }

    /** Panggil PALING AWAL di main(). Tidak pernah melempar exception. */
    public static void install() {
        try {
            Path dir = logDir();
            Files.createDirectories(dir);
            Path file = dir.resolve("dgjcode.log");
            if (Files.exists(file)) {
                Files.move(file, dir.resolve("dgjcode-sebelumnya.log"), StandardCopyOption.REPLACE_EXISTING);
            }
            OutputStream out = Files.newOutputStream(file);
            System.setOut(new PrintStream(new Tee(System.out, out), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new Tee(System.err, out), true, StandardCharsets.UTF_8));
            logFile = file;
        } catch (IOException | RuntimeException e) {
            System.err.println("Log tidak bisa dibuat: " + e);
        }
        System.out.println("=== " + AppInfo.NAME + " dimulai " + LocalDateTime.now() + " ===");
        System.out.println("Java " + Runtime.version() + " | java.home=" + System.getProperty("java.home"));
        System.out.println("OS " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " | user.dir=" + System.getProperty("user.dir"));

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            System.err.println("Error tidak tertangani di thread \"" + thread.getName() + "\":");
            error.printStackTrace();
            if (DIALOG_SHOWN.compareAndSet(false, true)) {      // dialog sekali saja, sisanya cukup di log
                SwingUtilities.invokeLater(() -> showDialog("Terjadi error yang tidak terduga.", error));
            }
        });
    }

    /** Error fatal saat start: catat, tampilkan pesan yang bisa disalin, lalu keluar. */
    public static void fatal(String what, Throwable error) {
        System.err.println("FATAL: " + what);
        error.printStackTrace();
        DIALOG_SHOWN.set(true);
        String headline = "Maaf, " + AppInfo.NAME + " gagal dibuka (" + what + ").";
        if (SwingUtilities.isEventDispatchThread()) {
            showDialog(headline, error);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> showDialog(headline, error));
            } catch (Exception ignored) {
                // tetap keluar di bawah
            }
        }
        System.exit(1);
    }

    private static void showDialog(String headline, Throwable error) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        if (trace.length() > 4000) {
            trace = trace.substring(0, 4000) + "\n...";
        }
        JTextArea text = new JTextArea(headline + "\n\n" + trace
                + "\nLog lengkap: " + (logFile != null ? logFile : "(tidak tersedia)"));
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        text.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(720, 360));
        try {
            JOptionPane.showMessageDialog(null, scroll, AppInfo.NAME, JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            // kalau dialog pun gagal, error tetap ada di file log
        }
    }

    /** Menulis ke konsol (kalau ada) dan ke file sekaligus. */
    private static final class Tee extends OutputStream {
        private final OutputStream console;
        private final OutputStream file;

        Tee(OutputStream console, OutputStream file) {
            this.console = console;
            this.file = file;
        }

        @Override
        public void write(int b) {
            try {
                console.write(b);
            } catch (IOException ignored) {
            }
            synchronized (file) {
                try {
                    file.write(b);
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            try {
                console.write(b, off, len);
            } catch (IOException ignored) {
            }
            synchronized (file) {
                try {
                    file.write(b, off, len);
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public void flush() {
            try {
                console.flush();
            } catch (IOException ignored) {
            }
            synchronized (file) {
                try {
                    file.flush();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
