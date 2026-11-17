package id.dg.jcode.theme;

import com.formdev.flatlaf.FlatLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;

/**
 * Warna editor kode (RSyntaxTextArea) yang mengikuti tema FlatLaf.
 * Pemakaian: EditorTheme.install(area, scrollPane);  -> otomatis berubah saat tema diganti.
 */
public final class EditorTheme {
    private static final String LISTENER_KEY = "dg.editorTheme.listener";

    /** Kumpulan warna untuk satu tema. */
    private static final class Palette {
        Color bg, fg, currentLine, selection, caret;
        Color gutterBg, lineNumber, currentLineNumber, gutterBorder;
        Color keyword, classType, string, chr, number, comment, docComment, docTag, annotation;
        Color operator, separator, bracketBg, bracketBorder, markOccurrences, error;
        boolean boldKeywords;
    }

    private static final Palette DARK = dark();
    private static final Palette LIGHT = light();

    private EditorTheme() { }

    private static Palette dark() {
        Palette p = new Palette();
        p.bg = c(0x1E1F22);            p.fg = c(0xD6D8DC);
        p.currentLine = c(0x2A2C31);   p.selection = c(0x214283);   p.caret = c(0xFFFFFF);
        p.gutterBg = c(0x1E1F22);      p.lineNumber = c(0x6F737A);
        p.currentLineNumber = c(0xC9CCD1); p.gutterBorder = c(0x2F3136);
        p.keyword = c(0xFFD93D);       // kuning terang: keyword & reserved word
        p.classType = c(0xFFA040);     // oranye: String, System, Scanner, ...
        p.string = c(0x7FD6FF);        // biru muda cerah: "teks"
        p.chr = c(0x9BE2FF);           // 'a'
        p.number = c(0xC792EA);
        p.comment = c(0x7C8590);       p.docComment = c(0x6FAF7A);  p.docTag = c(0x8FCF8F);
        p.annotation = c(0xB9E07A);
        p.operator = c(0xE6E6E6);      p.separator = c(0xC9CCD1);
        p.bracketBg = c(0x3B514D);     p.bracketBorder = c(0x5A8A80);
        p.markOccurrences = c(0x3A3D41);
        p.error = c(0xFF6B6B);
        p.boldKeywords = false;        // kuning terang sudah cukup menonjol
        return p;
    }

    private static Palette light() {
        Palette p = new Palette();
        p.bg = c(0xFFFFFF);            p.fg = c(0x1F2328);
        p.currentLine = c(0xF3F6FA);   p.selection = c(0xB3D4FF);   p.caret = c(0x000000);
        p.gutterBg = c(0xF7F8FA);      p.lineNumber = c(0x9AA0A6);
        p.currentLineNumber = c(0x1F2328); p.gutterBorder = c(0xE1E4E8);
        p.keyword = c(0x0033B3);
        p.classType = c(0xB45309);     // oranye tua (oranye terang tidak terbaca di latar putih)
        p.string = c(0x0B7BB8);        // biru
        p.chr = c(0x0B7BB8);
        p.number = c(0x7C3AED);
        p.comment = c(0x8C8C8C);       p.docComment = c(0x4E8A4E);  p.docTag = c(0x3B7A3B);
        p.annotation = c(0x9E880D);
        p.operator = c(0x1F2328);      p.separator = c(0x1F2328);
        p.bracketBg = c(0xD5ECFF);     p.bracketBorder = c(0x7AB8F0);
        p.markOccurrences = c(0xE8E8E8);
        p.error = c(0xD12F2F);
        p.boldKeywords = true;
        return p;
    }

    /** Pasang warna sekarang + ikuti setiap pergantian tema. Panggil SETELAH area.setFont(). */
    public static void install(RSyntaxTextArea area, RTextScrollPane scroll) {
        apply(area, scroll);
        PropertyChangeListener l = e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                // invokeLater: tunggu FlatLaf selesai memperbarui semua komponen, baru warnai ulang
                SwingUtilities.invokeLater(() -> apply(area, scroll));
            }
        };
        UIManager.addPropertyChangeListener(l);
        area.putClientProperty(LISTENER_KEY, l);
    }

    /** Panggil saat editor dibuang (misalnya tab ditutup) agar tidak bocor memori. */
    public static void uninstall(RSyntaxTextArea area) {
        if (area.getClientProperty(LISTENER_KEY) instanceof PropertyChangeListener l) {
            UIManager.removePropertyChangeListener(l);
            area.putClientProperty(LISTENER_KEY, null);
        }
    }

    public static boolean isDark() {
        if (UIManager.getLookAndFeel() instanceof FlatLaf) return FlatLaf.isLafDark();
        Color bg = UIManager.getColor("Panel.background");
        return bg != null && (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000 < 128;
    }

    /** Warna latar editor untuk tema aktif (dipakai juga oleh panel output). */
    public static Color background() {
        return (isDark() ? DARK : LIGHT).bg;
    }

    public static void apply(RSyntaxTextArea area, RTextScrollPane scroll) {
        Palette p = isDark() ? DARK : LIGHT;

        area.setBackground(p.bg);
        area.setForeground(p.fg);
        area.setCaretColor(p.caret);
        area.setCurrentLineHighlightColor(p.currentLine);
        area.setFadeCurrentLineHighlight(false);
        area.setSelectionColor(p.selection);
        area.setUseSelectedTextColor(false);
        area.setMarkOccurrencesColor(p.markOccurrences);
        area.setMatchedBracketBGColor(p.bracketBg);
        area.setMatchedBracketBorderColor(p.bracketBorder);

        Font base = area.getFont();
        Font kw = p.boldKeywords ? base.deriveFont(Font.BOLD) : null;
        Font italic = base.deriveFont(Font.ITALIC);

        SyntaxScheme s = area.getSyntaxScheme();
        for (int i = 0; i < s.getStyleCount(); i++) s.setStyle(i, new Style(p.fg)); // reset semua

        set(s, TokenTypes.RESERVED_WORD, p.keyword, kw);       // public class if new null ...
        set(s, TokenTypes.RESERVED_WORD_2, p.keyword, kw);     // return, yield
        set(s, TokenTypes.DATA_TYPE, p.keyword, kw);           // int double boolean char var
        set(s, TokenTypes.LITERAL_BOOLEAN, p.keyword, kw);     // true false
        set(s, TokenTypes.FUNCTION, p.classType, null);        // String System Scanner Math ...
        set(s, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, p.string, null);
        set(s, TokenTypes.ERROR_STRING_DOUBLE, p.string, null); // teks yang sedang diketik (belum ditutup)
        set(s, TokenTypes.LITERAL_CHAR, p.chr, null);
        set(s, TokenTypes.ERROR_CHAR, p.chr, null);
        set(s, TokenTypes.LITERAL_BACKQUOTE, p.string, null);
        set(s, TokenTypes.LITERAL_NUMBER_DECIMAL_INT, p.number, null);
        set(s, TokenTypes.LITERAL_NUMBER_FLOAT, p.number, null);
        set(s, TokenTypes.LITERAL_NUMBER_HEXADECIMAL, p.number, null);
        set(s, TokenTypes.COMMENT_EOL, p.comment, italic);
        set(s, TokenTypes.COMMENT_MULTILINE, p.comment, italic);
        set(s, TokenTypes.COMMENT_DOCUMENTATION, p.docComment, italic);
        set(s, TokenTypes.COMMENT_KEYWORD, p.docTag, italic);
        set(s, TokenTypes.COMMENT_MARKUP, p.docTag, italic);
        set(s, TokenTypes.ANNOTATION, p.annotation, null);
        set(s, TokenTypes.OPERATOR, p.operator, null);
        set(s, TokenTypes.SEPARATOR, p.separator, null);
        set(s, TokenTypes.IDENTIFIER, p.fg, null);
        set(s, TokenTypes.VARIABLE, p.fg, null);
        set(s, TokenTypes.ERROR_IDENTIFIER, p.error, null);
        set(s, TokenTypes.ERROR_NUMBER_FORMAT, p.error, null);
        area.setSyntaxScheme(s);

        if (scroll != null) {
            Gutter g = scroll.getGutter();
            g.setBackground(p.gutterBg);
            g.setLineNumberColor(p.lineNumber);
            g.setCurrentLineNumberColor(p.currentLineNumber);
            g.setBorderColor(p.gutterBorder);
            g.setLineNumberFont(base.deriveFont(Math.max(10f, base.getSize2D() - 1f)));
            scroll.getViewport().setBackground(p.bg);
            scroll.setBackground(p.bg);
        }
        area.revalidate();
        area.repaint();
    }

    private static void set(SyntaxScheme s, int type, Color fg, Font font) {
        s.setStyle(type, new Style(fg, null, font));
    }

    private static Color c(int rgb) {
        return new Color(rgb);
    }
}
