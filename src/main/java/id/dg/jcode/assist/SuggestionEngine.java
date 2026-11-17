package id.dg.jcode.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mengubah error javac (Inggris) menjadi pesan Bahasa Indonesia + saran perbaikan.
 *
 * Saran otomatis yang didukung:
 *  - titik koma / kurung tutup ) ] } hilang
 *  - kurung kurawal } kurang di akhir file
 *  - salah ketik nama tipe umum (Strng, Scaner, system, Arraylist, ...)
 *  - import belum ada (Scanner, ArrayList, ...)
 *  - salah ketik nama variabel / method
 *  - variabel belum dideklarasikan
 *  - tanda kutip penutup String hilang
 *  - nama class public berbeda dengan nama file
 */
final class SuggestionEngine {

    /** Tipe yang sering dipakai pemula -> paketnya. */
    private static final Map<String, String> TYPES = new LinkedHashMap<>();

    static {
        register("java.lang", "String", "System", "Math", "Integer", "Double", "Float", "Long", "Short",
                "Byte", "Boolean", "Character", "Object", "StringBuilder", "Exception",
                "RuntimeException", "Thread");
        register("java.util", "Scanner", "ArrayList", "List", "HashMap", "Map", "HashSet", "Set",
                "Arrays", "Random", "LinkedList", "Collections", "Iterator");
        register("java.io", "File", "IOException", "BufferedReader", "InputStreamReader",
                "FileReader", "FileWriter", "PrintWriter");
        register("java.time", "LocalDate", "LocalDateTime");
    }

    private static void register(String pkg, String... names) {
        for (String n : names) TYPES.put(n, pkg);
    }

    /** Nama method yang sering dipakai, untuk menebak salah ketik. */
    private static final List<String> COMMON_METHODS = List.of(
            "println", "print", "printf", "nextInt", "nextLine", "next", "nextDouble", "nextBoolean",
            "length", "charAt", "equals", "equalsIgnoreCase", "add", "get", "set", "size", "remove",
            "substring", "toUpperCase", "toLowerCase", "parseInt", "parseDouble", "contains",
            "indexOf", "isEmpty", "close", "sqrt", "pow", "max", "min", "abs", "valueOf", "trim");

    private static final Pattern EXPECTED = Pattern.compile("^'(.)' expected");
    private static final Pattern SYMBOL = Pattern.compile("symbol:\\s+(class|variable|method)\\s+([\\w$]+)");
    private static final Pattern NO_PACKAGE = Pattern.compile("package ([\\w.]+) does not exist");
    private static final Pattern VARIABLE = Pattern.compile("variable ([\\w$]+)");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s", Pattern.MULTILINE);
    private static final Pattern DECL = Pattern.compile(
            "\\b[A-Za-z_][\\w$]*(?:<[^>;]*>)?(?:\\[\\])*\\s+([A-Za-z_$][\\w$]*)\\s*(?=[=;,:)])");
    private static final Pattern METHOD_DECL = Pattern.compile("\\b([a-zA-Z_$][\\w$]*)\\s*\\(");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_$][\\w$]*");

    private final String text;
    private final Tone tone;
    private final String fileBase;

    private SuggestionEngine(String text, Tone tone, String fileBase) {
        this.text = text;
        this.tone = tone;
        this.fileBase = fileBase;
    }

    /** Titik masuk: ubah semua error javac menjadi daftar Problem (maksimal satu per baris). */
    static List<Problem> build(String text, List<JavacChecker.Diag> diags, Tone tone, String fileBase) {
        SuggestionEngine e = new SuggestionEngine(text, tone, fileBase);
        Map<Integer, Problem> perLine = new LinkedHashMap<>();
        for (JavacChecker.Diag d : diags) {
            Problem p;
            try {
                p = e.translate(d);
            } catch (RuntimeException ex) {
                p = null; // satu error yang aneh tidak boleh merusak semua saran
            }
            if (p != null) perLine.putIfAbsent(p.line(), p); // error pertama di baris yang menang
        }
        List<Problem> out = new ArrayList<>(perLine.values());
        out.sort(Comparator.comparingInt(Problem::line));
        return out;
    }

    // ------------------------------------------------------------------ penerjemah utama

    private Problem translate(JavacChecker.Diag d) {
        int len = text.length();
        if (len == 0) return null;
        int pos = clamp(d.pos() >= 0 ? d.pos() : d.start());
        if (pos < 0) pos = 0;
        String code = d.code() == null ? "" : d.code();
        String msg = d.message() == null ? "" : d.message();

        // 1) tanda baca hilang: ; ) ] }
        Matcher ex = EXPECTED.matcher(msg);
        if (code.startsWith("compiler.err.expected") && ex.find()) {
            return missingChar(ex.group(1).charAt(0), pos);
        }
        // 2) kurung kurawal penutup kurang di akhir file
        if (code.equals("compiler.err.premature.eof")) {
            int need = Math.max(1, count('{') - count('}'));
            String add = "\n}".repeat(need);
            int s = Math.max(0, len - 1);
            return problem(s, len, m("Kurung kurawal tutup } kurang di akhir file",
                            "Ada kurung kurawal } yang belum ditutup. Tambahkan } di paling bawah, ya."),
                    new Fix(List.of(new Edit(len, len, add)), "}".repeat(need)));
        }
        // 3) tanda kutip String tidak ditutup
        if (code.equals("compiler.err.unclosed.str.lit")) {
            int le = lineEnd(pos);
            String content = text.substring(lineStart(pos), le).stripTrailing();
            int ins = lineStart(pos) + content.length();
            if (content.endsWith(");")) ins -= 2;
            else if (content.endsWith(";") || content.endsWith(")")) ins -= 1;
            Edit edit = new Edit(ins, ins, "\"");
            return problem(pos, le, m("Tanda kutip penutup (\") hilang pada teks ini",
                            "Teksnya belum ditutup dengan tanda kutip (\"). Tambahkan di akhir teks."),
                    new Fix(List.of(edit), previewLine(edit)));
        }
        // 4) simbol tidak dikenal: class / variabel / method
        if (code.startsWith("compiler.err.cant.resolve")) {
            Matcher sm = SYMBOL.matcher(msg);
            if (sm.find()) {
                return unknownSymbol(sm.group(1), sm.group(2), d, pos);
            }
        }
        // 5) "system.out" -> javac mengira 'system' adalah paket
        if (code.equals("compiler.err.doesnt.exist")) {
            Matcher pm = NO_PACKAGE.matcher(msg);
            if (pm.find()) {
                String first = pm.group(1).split("\\.")[0];
                int ls = lineStart(pos);
                int idx = text.indexOf(first, ls);
                String type = closest(first, TYPES.keySet());
                if (type != null && idx >= 0 && idx < lineEnd(pos)) {
                    Edit edit = new Edit(idx, idx + first.length(), type);
                    return problem(idx, idx + first.length(), typoMessage(first, type),
                            new Fix(List.of(edit), previewLine(edit)));
                }
                return problem(pos, pos + 1, m("Paket '" + pm.group(1) + "' tidak ditemukan",
                        "Paket '" + pm.group(1) + "' tidak ada. Cek lagi ejaan import-nya."), null);
            }
        }
        // 6) nama class public berbeda dengan nama file
        if (code.equals("compiler.err.class.public.should.be.in.file")
                && fileBase != null && IDENT.matcher(fileBase).matches()) {
            Matcher pt = JavacChecker.PUBLIC_TYPE.matcher(text);
            if (pt.find()) {
                Edit edit = new Edit(pt.start(1), pt.end(1), fileBase);
                return problem(pt.start(1), pt.end(1),
                        m("Nama class public harus sama dengan nama file: " + fileBase,
                                "Nama class harus sama dengan nama file-nya, yaitu " + fileBase + "."),
                        new Fix(List.of(edit), previewLine(edit)));
            }
        }
        // 7) error lain: cukup terjemahkan pesannya
        return problem(start(d, pos), end(d, pos), translateOnly(code, msg), null);
    }

    // ------------------------------------------------------------------ kasus khusus

    private Problem missingChar(char c, int pos) {
        String pro;
        String friendly;
        switch (c) {
            case ';' -> {
                pro = "Titik koma (;) hilang di akhir baris ini";
                friendly = "Sepertinya kamu lupa titik koma (;) di akhir baris ini.";
            }
            case ')' -> {
                pro = "Kurung tutup ) hilang";
                friendly = "Kurung buka ( belum ditutup. Tambahkan ) ya.";
            }
            case ']' -> {
                pro = "Kurung siku tutup ] hilang";
                friendly = "Kurung siku [ belum ditutup. Tambahkan ].";
            }
            case '}' -> {
                pro = "Kurung kurawal tutup } hilang";
                friendly = "Kurung kurawal { belum ditutup. Tambahkan }.";
            }
            default -> {
                pro = "Tanda '" + c + "' hilang di sini";
                friendly = "Sepertinya ada tanda '" + c + "' yang terlupa di sini.";
            }
        }
        // javac menunjuk posisi tepat SETELAH kata terakhir -> sisipkan di situ.
        Edit edit = new Edit(pos, pos, String.valueOf(c));
        return problem(Math.max(lineStart(pos), pos - 1), pos, m(pro, friendly),
                new Fix(List.of(edit), previewLine(edit)));
    }

    private Problem unknownSymbol(String kind, String name, JavacChecker.Diag d, int pos) {
        int s = start(d, pos);
        int e = end(d, pos);
        // Pastikan rentang menunjuk tepat ke nama yang salah.
        int idx = text.indexOf(name, lineStart(pos));
        if (!text.substring(s, Math.min(e, text.length())).equals(name) && idx >= 0 && idx < lineEnd(pos)) {
            s = idx;
            e = idx + name.length();
        }
        switch (kind) {
            case "class" -> {
                String pkg = TYPES.get(name);
                if (pkg != null && !pkg.equals("java.lang")) {
                    return missingImport(name, pkg, s, e);
                }
                String type = closest(name, TYPES.keySet());
                if (type != null) return replace(s, e, type, typoMessage(name, type));
                return problem(s, e, m("Tipe '" + name + "' tidak dikenal",
                        "Java tidak mengenal tipe '" + name + "'. Cek lagi ejaannya, huruf besar/kecil berpengaruh."), null);
            }
            case "method" -> {
                Set<String> names = new LinkedHashSet<>(COMMON_METHODS);
                names.addAll(matches(METHOD_DECL));
                String best = closest(name, names);
                if (best != null) return replace(s, e, best, typoMessage(name, best));
                return problem(s, e, m("Method '" + name + "' tidak ditemukan",
                        "Method '" + name + "' tidak ada. Mungkin salah ketik?"), null);
            }
            default -> { // variable
                boolean capital = Character.isUpperCase(name.charAt(0));
                String type = closest(name, TYPES.keySet());
                if (capital && type != null) return replace(s, e, type, typoMessage(name, type));
                String var = closest(name, matches(DECL));
                if (var != null) return replace(s, e, var, typoMessage(name, var));
                if (type != null) return replace(s, e, type, typoMessage(name, type));
                return undeclared(name, s, e);
            }
        }
    }

    private Problem missingImport(String name, String pkg, int s, int e) {
        String line = "import " + pkg + "." + name + ";\n";
        int at = 0;
        Matcher im = IMPORT.matcher(text);
        Matcher pk = JavacChecker.PACKAGE.matcher(text);
        if (im.find()) {
            at = lineStart(im.start() + (text.charAt(im.start()) == '\n' ? 1 : 0));
        } else if (pk.find()) {
            int nl = text.indexOf('\n', pk.end());
            at = nl < 0 ? text.length() : nl + 1;
            if (nl < 0) line = "\n" + line;
        }
        return problem(s, e, m("Import untuk " + name + " belum ada (" + pkg + "." + name + ")",
                        "Untuk memakai " + name + ", tambahkan import " + pkg + "." + name + " di bagian atas."),
                new Fix(List.of(new Edit(at, at, line)), line.strip()));
    }

    private Problem undeclared(String name, int s, int e) {
        int ls = lineStart(s);
        String lineText = text.substring(ls, lineEnd(s));
        String type = guessType(lineText, name);
        String trimmed = lineText.stripLeading();
        int indentLen = lineText.length() - trimmed.length();
        Fix fix;
        if (trimmed.matches(Pattern.quote(name) + "\\s*=(?!=).*")) {
            // "x = 5;"  ->  "int x = 5;"
            Edit edit = new Edit(ls + indentLen, ls + indentLen, type + " ");
            fix = new Fix(List.of(edit), previewLine(edit));
        } else {
            String decl = type + " " + name + " = " + defaultValue(type) + ";";
            fix = new Fix(List.of(new Edit(ls, ls, lineText.substring(0, indentLen) + decl + "\n")), decl);
        }
        return problem(s, e, m("Variabel '" + name + "' belum dideklarasikan",
                "Variabel '" + name + "' belum dibuat. Buat dulu sebelum dipakai."), fix);
    }

    private Problem replace(int s, int e, String with, String message) {
        Edit edit = new Edit(s, e, with);
        return problem(s, e, message, new Fix(List.of(edit), previewLine(edit)));
    }

    private String typoMessage(String wrong, String right) {
        return m("Salah ketik: '" + wrong + "' seharusnya '" + right + "'",
                "Mungkin maksudmu '" + right + "'? (tertulis '" + wrong + "')");
    }

    private String translateOnly(String code, String msg) {
        String first = msg.lines().findFirst().orElse(msg);
        Matcher vm = VARIABLE.matcher(msg);
        String var = vm.find() ? vm.group(1) : "ini";
        return switch (code) {
            case "compiler.err.prob.found.req" -> m("Tipe data tidak cocok",
                    "Jenis datanya tidak cocok, misalnya teks dimasukkan ke variabel angka.");
            case "compiler.err.missing.ret.stmt" -> m("Method ini harus mengembalikan nilai (return)",
                    "Method ini harus diakhiri dengan return yang berisi nilai.");
            case "compiler.err.var.might.not.have.been.initialized" -> m(
                    "Variabel '" + var + "' belum diberi nilai awal",
                    "Variabel '" + var + "' belum punya nilai. Beri nilai dulu, misalnya = 0.");
            case "compiler.err.already.defined" -> m("Nama ini sudah dideklarasikan sebelumnya",
                    "Nama ini sudah dipakai. Gunakan nama lain.");
            case "compiler.err.not.stmt" -> m("Baris ini bukan perintah Java yang lengkap",
                    "Baris ini belum menjadi perintah yang lengkap.");
            case "compiler.err.illegal.start.of.expr", "compiler.err.illegal.start.of.type" -> m(
                    "Ada tanda atau kata yang tidak pada tempatnya",
                    "Ada sesuatu yang salah tempat di sini. Cek kurung dan titik koma di sekitarnya.");
            case "compiler.err.unreported.exception.need.to.catch.or.throw" -> m(
                    "Exception harus ditangkap (try-catch) atau ditambah 'throws'",
                    "Perintah ini bisa gagal. Bungkus dengan try-catch atau tambahkan 'throws Exception'.");
            case "compiler.err.cant.apply.symbol", "compiler.err.cant.apply.symbols" -> m(
                    "Argumen method tidak sesuai", "Isi di dalam kurung method tidak sesuai.");
            case "compiler.err.else.without.if" -> m("'else' tanpa 'if'", "Ada 'else' yang tidak punya pasangan 'if'.");
            case "compiler.err.illegal.char" -> m("Karakter tidak sah di kode", "Ada karakter aneh yang tidak dikenal Java.");
            case "compiler.err.incomparable.types", "compiler.err.operator.cant.be.applied.1" -> m(
                    "Operator tidak bisa dipakai untuk tipe ini", "Operator ini tidak cocok untuk jenis data tersebut.");
            default -> m("Kesalahan: " + first, "Ada yang belum tepat di sini: " + first);
        };
    }

    // ------------------------------------------------------------------ alat bantu

    private Problem problem(int s, int e, String message, Fix fix) {
        s = clamp(s);
        int ls = lineStart(s);
        int le = lineEnd(s);
        if (s >= le && s > ls) s = le - 1;         // posisi di ujung baris -> tandai karakter terakhir
        e = Math.min(Math.max(e, s + 1), Math.max(le, s + 1));
        e = clamp(e);
        return new Problem(lineOf(s), s, e, message, fix);
    }

    private String m(String pro, String friendly) {
        return tone == Tone.FRIENDLY ? friendly : pro;
    }

    private int start(JavacChecker.Diag d, int pos) {
        return clamp(d.start() >= 0 ? d.start() : pos);
    }

    private int end(JavacChecker.Diag d, int pos) {
        int s = start(d, pos);
        return clamp(d.end() > s ? d.end() : s + 1);
    }

    private int clamp(int off) {
        return Math.max(0, Math.min(off, text.length()));
    }

    private int lineStart(int off) {
        return text.lastIndexOf('\n', Math.max(0, off - 1)) + 1 > off ? lineStartSlow(off)
                : text.lastIndexOf('\n', Math.max(0, off - 1)) + 1;
    }

    private int lineStartSlow(int off) {
        int i = off - 1;
        while (i >= 0 && text.charAt(i) != '\n') i--;
        return i + 1;
    }

    private int lineEnd(int off) {
        int i = text.indexOf('\n', off);
        int end = i < 0 ? text.length() : i;
        return end > 0 && text.charAt(end - 1) == '\r' && end - 1 >= off ? end - 1 : end;
    }

    private int lineOf(int off) {
        int n = 0;
        for (int i = 0; i < off && i < text.length(); i++) if (text.charAt(i) == '\n') n++;
        return n;
    }

    private int count(char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == c) n++;
        return n;
    }

    /** Pratinjau = baris tempat edit terjadi, setelah diperbaiki. */
    private String previewLine(Edit ed) {
        int ls = lineStart(ed.start());
        int le = Math.max(lineEnd(ed.end()), ed.end());
        String line = text.substring(ls, le);
        String fixed = line.substring(0, ed.start() - ls) + ed.text() + line.substring(ed.end() - ls);
        return fixed.strip();
    }

    private Set<String> matches(Pattern p) {
        Set<String> out = new HashSet<>();
        Matcher mm = p.matcher(text);
        while (mm.find()) out.add(mm.group(1));
        return out;
    }

    private static String guessType(String line, String name) {
        Matcher mm = Pattern.compile(Pattern.quote(name) + "\\s*=(?!=)\\s*(.*)").matcher(line);
        if (mm.find()) {
            String v = mm.group(1).trim();
            if (v.startsWith("\"")) return "String";
            if (v.startsWith("'")) return "char";
            if (v.startsWith("true") || v.startsWith("false")) return "boolean";
            if (v.matches("-?\\d+\\.\\d.*")) return "double";
            if (v.startsWith("new Scanner")) return "Scanner";
        }
        return "int";
    }

    private static String defaultValue(String type) {
        return switch (type) {
            case "String" -> "\"\"";
            case "char" -> "' '";
            case "boolean" -> "false";
            case "double" -> "0.0";
            case "Scanner" -> "new Scanner(System.in)";
            default -> "0";
        };
    }

    /** Kandidat paling mirip (jarak Levenshtein kecil), atau null. */
    static String closest(String word, Collection<String> candidates) {
        int limit = word.length() <= 4 ? 1 : 2;
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String w = word.toLowerCase();
        for (String c : candidates) {
            if (c.equals(word)) continue;
            int dist = c.equalsIgnoreCase(word) ? 0 : levenshtein(w, c.toLowerCase());
            if (dist <= limit && dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    private static int levenshtein(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 2) return 99;
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }

    @SuppressWarnings("unused")
    private static List<String> known() {
        return Arrays.asList(TYPES.keySet().toArray(new String[0]));
    }
}
