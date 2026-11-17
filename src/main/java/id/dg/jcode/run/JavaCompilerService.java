package id.dg.jcode.run;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Mengompilasi kode pengguna dengan javax.tools.JavaCompiler (modul jdk.compiler). */
public final class JavaCompilerService {

    public record Result(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics, long millis) {
    }

    private JavaCompilerService() {
    }

    public static Result compile(List<Path> sources, Path outDir) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("Compiler Java tidak ditemukan. Pastikan modul jdk.compiler ikut dibundel.");
        }
        Files.createDirectories(outDir);
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        long start = System.nanoTime();
        boolean ok;
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(sources);
            List<String> options = List.of("-d", outDir.toString(), "-encoding", "UTF-8", "-g", "-proc:none");
            ok = Boolean.TRUE.equals(compiler.getTask(null, fm, collector, options, null, units).call());
        }
        return new Result(ok, List.copyOf(collector.getDiagnostics()), (System.nanoTime() - start) / 1_000_000L);
    }
}
