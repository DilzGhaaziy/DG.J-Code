package id.dg.jcode.ui.splash;

import id.dg.jcode.AppInfo;
import id.dg.jcode.ui.icons.AppLogo;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.Palette;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.util.Gfx;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * Splash screen 480 x 280 tanpa dekorasi jendela, di tengah layar, dengan fade-in.
 *
 * SOLUSI KONTRAS TEKS HITAM DAN PUTIH:
 * Spesifikasi meminta judul "DG J-Code" berwarna HITAM dan teks versi berwarna PUTIH.
 * Jika splash hanya punya satu warna latar, salah satu teks pasti sulit dibaca.
 * Karena itu splash dibagi menjadi dua zona:
 *   1. Area atas berlatar sangat terang (#FAFAFA). Judul hitam di sini
 *      kontrasnya sangat tinggi (sekitar 20:1).
 *   2. Strip bawah setinggi 48 px berlatar gelap (#1E1E2E). Teks versi putih
 *      di sini juga kontras tinggi (sekitar 16:1).
 * Dengan begitu kedua teks lolos standar keterbacaan WCAG AA.
 */
public final class SplashWindow extends JWindow {

    public static final int WIDTH = 480;
    public static final int HEIGHT = 280;
    private static final int STRIP_HEIGHT = 48;
    private static final int PAD = 32;
    private static final int FADE_MS = 280;
    private static final int FRAME_MS = 16;

    private static final Color TOP_BG = new Color(0xFAFAFA);
    private static final Color STRIP_BG = new Color(0x1E1E2E);
    private static final Color TITLE_COLOR = Color.BLACK;
    private static final Color VERSION_COLOR = Color.WHITE;
    private static final Color MUTED = new Color(0x5F6670);
    private static final Color OUTLINE = new Color(0xE1E4E8);
    private static final Color ACCENT = Palette.LIGHT.accent();

    private final boolean canFade;
    private final boolean transparentCorners;
    private final Font titleFont = Fonts.uiSemibold(28f);
    private final Font subtitleFont = Fonts.ui(13f);
    private final Font statusFont = Fonts.ui(12f);
    private final Font versionFont = Fonts.ui(11f);
    private final JComponent content;
    private final Timer progressTimer;

    private volatile String status = "Menyiapkan aplikasi...";
    private float phase;
    private long shownAtNanos;

    public SplashWindow() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        canFade = device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        transparentCorners = device.isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);

        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        content = new SplashContent();
        if (transparentCorners) {
            // Latar jendela transparan agar sudut membulat 8 px terlihat halus (anti-aliasing).
            setBackground(new Color(0, 0, 0, 0));
            getRootPane().setOpaque(false);
            content.setOpaque(false);
        } else {
            content.setOpaque(true);
        }
        setContentPane(content);

        progressTimer = new Timer(FRAME_MS, e -> {
            phase = (phase + 0.012f) % 1f;
            content.repaint();
        });
    }

    /** Menampilkan splash dengan efek fade-in. */
    public void showWithFadeIn() {
        if (canFade) {
            setOpacity(0f);
        }
        setVisible(true);
        shownAtNanos = System.nanoTime();
        progressTimer.start();
        if (canFade) {
            animateOpacity(0f, 1f, null);
        }
    }

    public void setStatus(String text) {
        status = text;
        content.repaint();
    }

    /**
     * Menutup splash setelah minimal minMillis sejak ditampilkan.
     * onReady dijalankan (misalnya membuka jendela utama), lalu splash memudar di atasnya.
     */
    public void closeAfterMinimum(long minMillis, Runnable onReady) {
        long elapsedMs = (System.nanoTime() - shownAtNanos) / 1_000_000L;
        int delay = (int) Math.max(0L, minMillis - elapsedMs);
        Timer wait = new Timer(delay, e -> {
            setAlwaysOnTop(true);
            if (onReady != null) {
                onReady.run();
            }
            toFront();
            Runnable finish = () -> {
                progressTimer.stop();
                setVisible(false);
                dispose();
            };
            if (canFade) {
                animateOpacity(getOpacity(), 0f, finish);
            } else {
                finish.run();
            }
        });
        wait.setRepeats(false);
        wait.start();
    }

    private void animateOpacity(float from, float to, Runnable done) {
        long start = System.nanoTime();
        Timer timer = new Timer(FRAME_MS, null);
        timer.addActionListener(e -> {
            float k = Math.min(1f, (System.nanoTime() - start) / (FADE_MS * 1_000_000f));
            float eased = 1f - (1f - k) * (1f - k);
            float value = from + (to - from) * eased;
            setOpacity(Math.max(0f, Math.min(1f, value)));
            if (k >= 1f) {
                ((Timer) e.getSource()).stop();
                if (done != null) {
                    done.run();
                }
            }
        });
        timer.start();
    }

    /** Komponen yang menggambar seluruh isi splash. */
    private final class SplashContent extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Gfx.create(g, false);
            try {
                int w = getWidth();
                int h = getHeight();
                int arc = ThemeManager.ARC;
                Shape shape = transparentCorners
                        ? new RoundRectangle2D.Float(0, 0, w, h, arc, arc)
                        : new Rectangle(0, 0, w, h);
                g2.clip(shape);

                // Zona 1: area atas terang untuk judul hitam.
                g2.setColor(TOP_BG);
                g2.fillRect(0, 0, w, h);

                // Zona 2: strip bawah gelap untuk teks versi putih.
                g2.setColor(STRIP_BG);
                g2.fillRect(0, h - STRIP_HEIGHT, w, STRIP_HEIGHT);

                // Judul di pojok kiri atas.
                g2.setFont(titleFont);
                FontMetrics tfm = g2.getFontMetrics();
                int titleBaseline = PAD + tfm.getAscent() - 4;
                g2.setColor(TITLE_COLOR);
                g2.drawString(AppInfo.NAME, PAD, titleBaseline);

                // Garis aksen kecil di bawah judul.
                g2.setColor(ACCENT);
                g2.fillRoundRect(PAD, titleBaseline + 12, 32, 4, 4, 4);

                // Subjudul.
                g2.setFont(subtitleFont);
                g2.setColor(MUTED);
                g2.drawString("IDE Java yang ringan untuk belajar dan berkarya", PAD, titleBaseline + 40);

                // Logo orisinal di kanan atas.
                AppLogo.paint(g2, w - PAD - 48, PAD, 48, ACCENT, Color.WHITE);

                // Status pemuatan dan garis progres tak tentu.
                int barY = h - STRIP_HEIGHT - 2;
                g2.setFont(statusFont);
                g2.setColor(MUTED);
                g2.drawString(status, PAD, barY - 16);
                g2.setColor(OUTLINE);
                g2.fillRect(0, barY, w, 2);
                int segment = w / 4;
                int segX = Math.round(phase * (w + segment)) - segment;
                g2.setColor(ACCENT);
                g2.fillRect(segX, barY, segment, 2);

                // Teks versi di pojok kanan bawah (di dalam strip gelap).
                g2.setFont(versionFont);
                FontMetrics vfm = g2.getFontMetrics();
                String version = AppInfo.FULL_NAME + " " + AppInfo.VERSION;
                int vx = w - 24 - vfm.stringWidth(version);
                int vy = h - STRIP_HEIGHT + (STRIP_HEIGHT - vfm.getHeight()) / 2 + vfm.getAscent();
                g2.setColor(VERSION_COLOR);
                g2.drawString(version, vx, vy);

                // Border tipis hanya di zona terang.
                g2.setClip(0, 0, w, h - STRIP_HEIGHT);
                g2.setColor(OUTLINE);
                if (transparentCorners) {
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc));
                } else {
                    g2.drawRect(0, 0, w - 1, h - 1);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
