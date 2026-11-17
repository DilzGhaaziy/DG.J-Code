package id.dg.jcode.practice;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Membuat hasil cetak: identitas, "Kode" (dengan nomor baris + garis pemisah), lalu "Output". */
final class PrintExporter {
    record Info(String name, String nim, String date) {
        List<String> lines() {
            List<String> l = new ArrayList<>();
            if (!name.isEmpty()) l.add("Nama    : " + name);
            if (!nim.isEmpty()) l.add("NIM     : " + nim);
            if (!date.isEmpty()) l.add("Tanggal : " + date);
            return l;
        }
    }

    private PrintExporter() { }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        for (String line : s.replace("\r", "").split("\n", -1)) out.add(line.replace("\t", "    "));
        while (out.size() > 1 && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        return out;
    }

    // ------------------------------------------------------------ PNG / JPG

    static void image(File f, String ext, String code, String output, Info info) throws IOException {
        List<String> codeLines = split(code);
        List<String> outLines = split(output.isBlank() ? "(belum ada output)" : output);
        List<String> head = info.lines();

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        for (String n : new String[]{"JetBrains Mono", "Consolas"}) {
            Font t = new Font(n, Font.PLAIN, 14);
            if (t.getFamily().equals(n)) { mono = t; break; }
        }
        Font ui = new Font("Segoe UI", Font.PLAIN, 14);
        Font title = new Font("Segoe UI", Font.BOLD, 18);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics fm = pg.getFontMetrics(mono);
        int lh = fm.getHeight() + 2;
        int gutter = fm.stringWidth(String.valueOf(codeLines.size())) + 16;
        int maxW = 400;
        for (String s : codeLines) maxW = Math.max(maxW, gutter + 16 + fm.stringWidth(s));
        for (String s : outLines) maxW = Math.max(maxW, fm.stringWidth(s));
        pg.dispose();

        int pad = 32;
        int width = Math.min(maxW + pad * 2, 4000);
        int height = pad + head.size() * 24 + (head.isEmpty() ? 0 : 16)
                + 32 + codeLines.size() * lh + 32 + 32 + outLines.size() * lh + pad;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // RGB: aman untuk JPG
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        int y = pad;

        g.setColor(new Color(0x1F2328));
        g.setFont(ui);
        for (String s : head) { y += 20; g.drawString(s, pad, y); y += 4; }
        if (!head.isEmpty()) y += 16;

        g.setFont(title);
        y += 24; g.drawString("Kode", pad, y); y += 8;
        int codeTop = y;
        g.setFont(mono);
        for (int i = 0; i < codeLines.size(); i++) {
            y += lh;
            g.setColor(new Color(0x8B929C));
            String num = String.valueOf(i + 1);
            g.drawString(num, pad + gutter - 8 - fm.stringWidth(num), y - 4);
            g.setColor(new Color(0x1F2328));
            g.drawString(codeLines.get(i), pad + gutter + 8, y - 4);
        }
        g.setColor(new Color(0xE1E4E8));
        g.drawLine(pad + gutter, codeTop, pad + gutter, y);   // garis pemisah nomor baris
        y += 16;
        g.drawLine(pad, y, width - pad, y);                    // garis pemisah Kode / Output

        g.setColor(new Color(0x1F2328));
        g.setFont(title);
        y += 32; g.drawString("Output", pad, y); y += 8;
        g.setFont(mono);
        for (String s : outLines) { y += lh; g.drawString(s, pad, y - 4); }
        g.dispose();
        ImageIO.write(img, ext.equals("jpg") ? "jpg" : "png", f);
    }

    // ------------------------------------------------------------ PDF (PDFBox 3)

    static void pdf(File f, String code, String output, Info info) throws IOException {
        PDType1Font mono = new PDType1Font(Standard14Fonts.FontName.COURIER);
        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font reg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        try (PDDocument doc = new PDDocument()) {
            Pdf p = new Pdf(doc);
            for (String s : info.lines()) p.text(reg, 11, 40, s);
            if (!info.lines().isEmpty()) p.y -= 8;
            p.text(bold, 14, 40, "Kode");
            List<String> lines = split(code);
            int digits = String.valueOf(lines.size()).length();
            float sep = 40 + digits * 6 + 6;
            for (int i = 0; i < lines.size(); i++) {
                p.ensure(13);
                String num = String.format("%" + digits + "d", i + 1);
                p.raw(mono, 10, 40, num);
                p.line(sep, p.y + 3, sep, p.y - 10);
                p.raw(mono, 10, sep + 6, lines.get(i));
                p.y -= 13;
            }
            p.y -= 8;
            p.ensure(30);
            p.line(40, p.y, PDRectangle.A4.getWidth() - 40, p.y);
            p.y -= 16;
            p.text(bold, 14, 40, "Output");
            for (String s : split(output.isBlank() ? "(belum ada output)" : output)) p.text(mono, 10, 40, s);
            p.close();
            doc.save(f);
        }
    }

    /** Pembantu kecil: menulis baris demi baris dan membuat halaman baru otomatis. */
    private static final class Pdf {
        final PDDocument doc;
        PDPageContentStream cs;
        float y;

        Pdf(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        void newPage() throws IOException {
            if (cs != null) cs.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            cs.setLineWidth(0.5f);
            y = PDRectangle.A4.getHeight() - 48;
        }

        void ensure(float h) throws IOException {
            if (y - h < 40) newPage();
        }

        void text(PDType1Font font, float size, float x, String s) throws IOException {
            ensure(size + 4);
            raw(font, size, x, s);
            y -= size + 4;
        }

        void raw(PDType1Font font, float size, float x, String s) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y - size);
            cs.showText(safe(s));
            cs.endText();
        }

        void line(float x1, float y1, float x2, float y2) throws IOException {
            cs.setStrokingColor(0.85f, 0.86f, 0.88f);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        void close() throws IOException {
            cs.close();
        }

        /** Font standar PDF hanya mendukung huruf Latin; karakter lain diganti '?' agar tidak error. */
        private static String safe(String s) {
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) b.append(c >= 32 && c < 127 || c >= 160 && c <= 255 ? c : '?');
            return b.toString();
        }
    }
}
