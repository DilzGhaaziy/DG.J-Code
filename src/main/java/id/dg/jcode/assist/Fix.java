package id.dg.jcode.assist;

import java.util.List;

/**
 * Saran perbaikan: daftar perubahan + pratinjau kode hasil perbaikan
 * (ditampilkan abu-abu sebagai "ghost text" di kotak saran).
 */
public record Fix(List<Edit> edits, String preview) {
    public Fix {
        edits = List.copyOf(edits);
    }
}
