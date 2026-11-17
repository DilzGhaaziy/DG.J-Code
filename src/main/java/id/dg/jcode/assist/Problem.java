package id.dg.jcode.assist;

/**
 * Satu kesalahan di kode.
 *
 * @param line    nomor baris (mulai dari 0)
 * @param start   awal garis bergelombang (offset karakter)
 * @param end     akhir garis bergelombang
 * @param message pesan Bahasa Indonesia
 * @param fix     saran perbaikan, boleh null jika tidak ada saran otomatis
 */
public record Problem(int line, int start, int end, String message, Fix fix) {
    public boolean hasFix() {
        return fix != null;
    }
}
