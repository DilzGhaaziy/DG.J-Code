package id.dg.jcode.assist;

/**
 * Satu perubahan teks: ganti karakter [start, end) dengan {@code text}.
 * Kalau start == end, artinya hanya menyisipkan teks.
 */
public record Edit(int start, int end, String text) {
}
