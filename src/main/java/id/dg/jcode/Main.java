package id.dg.jcode;

import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.MainFrame;
import id.dg.jcode.ui.splash.SplashWindow;
import id.dg.jcode.ui.theme.ThemeManager;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Titik masuk aplikasi.
 *
 * Urutan start:
 * 0. Pasang CrashLog: semua error dicatat ke file dan ditampilkan, tidak pernah diam-diam.
 * 1. Baca pengaturan (tema) dari file lokal.
 * 2. Pasang tema FlatLaf, lalu tampilkan splash secepat mungkin.
 * 3. Siapkan komponen di thread latar belakang (SwingWorker) agar UI tidak macet.
 * 4. Setelah siap dan splash sudah tampil minimal beberapa detik, buka jendela utama.
 */
public final class Main {

    /** Durasi minimal splash dalam milidetik. */
    private static final long MIN_SPLASH_MS = 4500L;

    private Main() {
    }

    public static void main(String[] args) {
        CrashLog.install();

        Settings settings;
        try {
            settings = Settings.load();
        } catch (Throwable t) {
            CrashLog.fatal("pengaturan tidak bisa dibaca", t);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                start(settings);
            } catch (Throwable t) {
                CrashLog.fatal("tampilan tidak bisa disiapkan", t);
            }
        });
    }

    private static void start(Settings settings) {
        ThemeManager.install(settings.getTheme());

        SplashWindow splash = new SplashWindow();
        splash.showWithFadeIn();

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                publish("Menyiapkan editor...");
                preload("org.fife.ui.rsyntaxtextarea.RSyntaxTextArea");
                publish("Menyiapkan compiler Java...");
                preload("javax.tools.ToolProvider");
                publish("Hampir selesai...");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                splash.setStatus(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    System.err.println("Gagal menyiapkan komponen: " + e.getCause());
                }

                MainFrame frame;
                try {
                    frame = new MainFrame(settings);
                } catch (Throwable ex) {
                    closeSplash(splash);
                    CrashLog.fatal("jendela utama gagal dibuat", ex);
                    return;
                }
                splash.setStatus("Siap");
                splash.closeAfterMinimum(MIN_SPLASH_MS, () -> {
                    try {
                        frame.setVisible(true);
                    } catch (Throwable ex) {
                        CrashLog.fatal("jendela utama gagal ditampilkan", ex);
                    }
                });
            }
        }.execute();
    }

    private static void closeSplash(SplashWindow splash) {
        try {
            splash.setVisible(false);
            splash.dispose();
        } catch (Throwable ignored) {
            // tidak penting, aplikasi akan keluar
        }
    }

    /** Memuat class lebih awal (tanpa inisialisasi) agar pembukaan editor nanti lebih cepat. */
    private static void preload(String className) {
        try {
            Class.forName(className, false, Main.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            // Tidak fatal: class akan dimuat normal saat pertama kali dipakai.
        }
    }
}
