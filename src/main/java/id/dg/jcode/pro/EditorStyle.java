package id.dg.jcode.pro;

import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.theme.ThemeMode;
import id.dg.jcode.ui.util.Colors;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/** Tampilan editor: pewarnaan sesuai tema + garis penghubung blok { }. */
public final class EditorStyle {

    private EditorStyle() {
    }

    public static void configure(RSyntaxTextArea area) {
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setTabSize(4);
        area.setTabsEmulated(true);
        area.setAutoIndentEnabled(true);
        area.setBracketMatchingEnabled(true);
        area.setPaintMatchedBracketPair(true);   // sorot { dan } pasangannya sekaligus
        area.setPaintTabLines(true);             // garis vertikal penghubung blok { ... }
        area.setCloseCurlyBraces(true);
        area.setMarkOccurrences(true);
        area.setHighlightCurrentLine(true);
    }

    public static void apply(RSyntaxTextArea area, RTextScrollPane scroll, int fontSize) {
        ThemeMode m = ThemeManager.current();
        String res = m.dark
                ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                : "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
        try (InputStream in = RSyntaxTextArea.class.getResourceAsStream(res)) {
            if (in != null) {
                Theme.load(in, Fonts.code(fontSize)).apply(area);
            }
        } catch (IOException e) {
            System.err.println("Tema editor gagal dimuat: " + e.getMessage());
        }
        area.setFont(Fonts.code(fontSize));
        area.setBackground(m.editorBg);
        area.setForeground(m.editorText);
        area.setCaretColor(m.editorText);
        area.setCurrentLineHighlightColor(m.currentLine);
        area.setSelectionColor(m.selection);
        area.setTabLineColor(Colors.mix(m.editorBg, m.lineNumber, 0.6f));
        area.setMatchedBracketBGColor(Colors.mix(m.editorBg, m.keyword, 0.18f));
        area.setMatchedBracketBorderColor(m.keyword);

        SyntaxScheme s = area.getSyntaxScheme();
        set(s, m.keyword, Token.RESERVED_WORD, Token.RESERVED_WORD_2, Token.LITERAL_BOOLEAN);
        set(s, m.type, Token.DATA_TYPE);
        set(s, m.string, Token.LITERAL_STRING_DOUBLE_QUOTE, Token.LITERAL_CHAR, Token.LITERAL_BACKQUOTE);
        set(s, m.number, Token.LITERAL_NUMBER_DECIMAL_INT, Token.LITERAL_NUMBER_FLOAT,
                Token.LITERAL_NUMBER_HEXADECIMAL);
        set(s, m.comment, Token.COMMENT_EOL, Token.COMMENT_MULTILINE, Token.COMMENT_DOCUMENTATION,
                Token.COMMENT_KEYWORD, Token.COMMENT_MARKUP);
        set(s, m.annotation, Token.ANNOTATION);
        set(s, m.editorText, Token.IDENTIFIER, Token.FUNCTION, Token.OPERATOR, Token.SEPARATOR);

        if (scroll != null) {
            Gutter gutter = scroll.getGutter();
            gutter.setBackground(m.gutterBg);
            gutter.setLineNumberColor(m.lineNumber);
            gutter.setBorderColor(m.gutterBg);
            gutter.setLineNumberFont(Fonts.code(Math.max(10, fontSize - 1)));
        }
        area.revalidate();
        area.repaint();
    }

    private static void set(SyntaxScheme scheme, Color color, int... types) {
        for (int t : types) {
            Style style = scheme.getStyle(t);
            if (style != null) {
                style.foreground = color;
                style.background = null;
            }
        }
    }
}
