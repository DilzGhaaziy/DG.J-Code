package id.dg.jcode.pro;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * Formatter sederhana: merapikan indentasi berdasarkan kurung kurawal { }.
 * Mengabaikan kurung di dalam string, char, dan komentar.
 * Cukup untuk kode pemula; text block (""") tidak diproses khusus.
 */
public final class CodeFormatter {

    private static final String INDENT = "    ";

    private CodeFormatter() {
    }

    public static String format(String src) {
        String[] lines = src.split("\\R", -1);
        StringBuilder out = new StringBuilder(src.length() + 64);
        int depth = 0;
        boolean inBlockComment = false;

        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].strip();
            int leading = 0;
            if (!inBlockComment) {
                while (leading < t.length() && t.charAt(leading) == '}') {
                    leading++;
                }
            }
            int indent = Math.max(0, depth - leading);
            if (!t.isEmpty()) {
                out.append(INDENT.repeat(indent));
                if (inBlockComment && t.startsWith("*")) {
                    out.append(' ');
                }
                out.append(t);
            }

            boolean inString = false;
            boolean inChar = false;
            for (int j = 0; j < t.length(); j++) {
                char c = t.charAt(j);
                char n = j + 1 < t.length() ? t.charAt(j + 1) : '\0';
                if (inBlockComment) {
                    if (c == '*' && n == '/') {
                        inBlockComment = false;
                        j++;
                    }
                } else if (inString) {
                    if (c == '\\') {
                        j++;
                    } else if (c == '"') {
                        inString = false;
                    }
                } else if (inChar) {
                    if (c == '\\') {
                        j++;
                    } else if (c == '\'') {
                        inChar = false;
                    }
                } else if (c == '/' && n == '/') {
                    break;
                } else if (c == '/' && n == '*') {
                    inBlockComment = true;
                    j++;
                } else if (c == '"') {
                    inString = true;
                } else if (c == '\'') {
                    inChar = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth = Math.max(0, depth - 1);
                }
            }
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    /** Memformat isi editor sebagai SATU langkah undo, lalu mengembalikan posisi baris. */
    public static void formatArea(RSyntaxTextArea area) {
        String before = area.getText();
        String after = format(before);
        if (before.equals(after)) {
            return;
        }
        int line = area.getCaretLineNumber();
        area.beginAtomicEdit();
        try {
            area.replaceRange(after, 0, before.length());
        } finally {
            area.endAtomicEdit();
        }
        try {
            int target = Math.min(line, area.getLineCount() - 1);
            area.setCaretPosition(area.getLineStartOffset(target));
        } catch (javax.swing.text.BadLocationException ignored) {
            area.setCaretPosition(0);
        }
    }
}
