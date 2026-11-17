package id.dg.jcode.ui.icons;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Memuat gambar PNG dari folder resources dan memperkecilnya dengan kualitas tinggi.
 * Hasil disimpan di cache agar tidak dihitung ulang.
 */
public final class ImageResources {

    private static final Map<String, Optional<BufferedImage>> ORIGINALS = new HashMap<>();
    private static final Map<String, BufferedImage> SCALED = new HashMap<>();

    private ImageResources() {
    }

    /** Gambar asli, atau null jika file tidak ada. */
    public static synchronized BufferedImage original(String path) {
        return ORIGINALS.computeIfAbsent(path, ImageResources::read).orElse(null);
    }

    public static boolean exists(String path) {
        return original(path) != null;
    }

    /** Gambar persegi size x size (proporsi dijaga, sisa area transparan), atau null. */
    public static synchronized BufferedImage scaled(String path, int size) {
        BufferedImage src = original(path);
        if (src == null) {
            return null;
        }
        return SCALED.computeIfAbsent(path + "@" + size, key -> fit(src, size));
    }

    /**
     * Menggambar gambar di kotak (x, y, s, s). Memperhitungkan skala HiDPI.
     * Mengembalikan false jika gambar tidak ada.
     */
    public static boolean draw(Graphics2D g, String path, double x, double y, double s) {
        double scale = Math.abs(g.getTransform().getScaleX());
        if (scale <= 0) {
            scale = 1;
        }
        int px = Math.max(1, (int) Math.ceil(s * scale));
        BufferedImage img = scaled(path, px);
        if (img == null) {
            return false;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.translate(x, y);
            g2.scale(s / px, s / px);
            g2.drawImage(img, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return true;
    }

    private static Optional<BufferedImage> read(String path) {
        try (InputStream in = ImageResources.class.getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(ImageIO.read(in));
        } catch (IOException e) {
            System.err.println("Gagal membaca gambar " + path + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Memperkecil bertahap (dibagi dua berulang) agar hasil tetap tajam. */
    private static BufferedImage fit(BufferedImage src, int size) {
        double ratio = Math.min((double) size / src.getWidth(), (double) size / src.getHeight());
        int tw = Math.max(1, (int) Math.round(src.getWidth() * ratio));
        int th = Math.max(1, (int) Math.round(src.getHeight() * ratio));

        BufferedImage cur = resize(src, src.getWidth(), src.getHeight());
        int cw = cur.getWidth();
        int ch = cur.getHeight();
        while (cw / 2 >= tw && ch / 2 >= th) {
            cw /= 2;
            ch /= 2;
            cur = resize(cur, cw, ch);
        }
        cur = resize(cur, tw, th);

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(cur, (size - tw) / 2, (size - th) / 2, null);
        g.dispose();
        return out;
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
