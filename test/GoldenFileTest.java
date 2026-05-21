import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * End-to-end golden-file test. Drives {@code examples/project_1.jtlp} through the full
 * compile+generate pipeline in a temp directory, then diffs the generated output files
 * against checked-in golden files in {@code test/expected/}.
 *
 * <p>To regenerate goldens after an intentional change:
 * <pre>./build/test.sh --update</pre>
 * or pass {@code -Dupdate=true} directly to the JVM.
 */
public class GoldenFileTest {

    /** Files produced by running project_1.jtlp, relative to the process CWD. */
    private static final String[] GOLDEN_FILES = {
        "main.cpp",          // written by example2 via file("main.cpp")
        "example4_out.txt",  // written by example4 via file("example4_out.txt")
    };

    private final Path examplesDir;
    private final Path expectedDir;
    private final boolean updateMode;

    public GoldenFileTest() {
        String rootProp = System.getProperty("jtl.root", System.getProperty("user.dir"));
        Path root = Paths.get(rootProp);
        examplesDir = root.resolve("examples");
        expectedDir = root.resolve("test").resolve("expected");
        updateMode = "true".equalsIgnoreCase(System.getProperty("update"));
    }

    public void testProject1GoldenFiles() throws Exception {
        String jarPath = findJtlJar();

        Path tempDir = Files.createTempDirectory("jtl-golden-");
        try {
            copyInputFiles(examplesDir, tempDir);
            runJtl(jarPath, tempDir, "project_1.jtlp");

            if (updateMode) {
                Files.createDirectories(expectedDir);
            }

            List<String> diffs = new ArrayList<String>();
            for (String fname : GOLDEN_FILES) {
                Path generated = tempDir.resolve(fname);
                Path golden = expectedDir.resolve(fname);

                if (updateMode) {
                    if (!Files.exists(generated)) {
                        throw new AssertionError("JTL did not generate expected file: " + fname);
                    }
                    Files.copy(generated, golden, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[golden] Updated: test/expected/" + fname);
                } else {
                    String d = diffFiles(golden, generated, fname);
                    if (d != null) diffs.add(d);
                }
            }

            if (!diffs.isEmpty()) {
                StringBuilder sb = new StringBuilder(
                    "Golden file differences (run './build/test.sh --update' to regenerate):\n");
                for (String d : diffs) sb.append(d);
                throw new AssertionError(sb.toString());
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String findJtlJar() throws Exception {
        URI uri = JTL.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return Paths.get(uri).toAbsolutePath().toString();
    }

    private void runJtl(String jarPath, Path workDir, String projectFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jarPath, projectFile);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exit = p.waitFor();
        if (exit != 0) {
            throw new AssertionError("JTL subprocess exited " + exit + ":\n" + out);
        }
    }

    /** Returns null if the files match; a human-readable diff snippet otherwise. */
    private String diffFiles(Path golden, Path generated, String label) throws IOException {
        if (!Files.exists(golden)) {
            return "  MISSING golden for '" + label + "' — run with --update to create it\n";
        }
        if (!Files.exists(generated)) {
            return "  JTL did not generate: " + label + "\n";
        }
        List<String> goldenLines = Files.readAllLines(golden);
        List<String> genLines    = Files.readAllLines(generated);
        if (goldenLines.equals(genLines)) return null;

        StringBuilder sb = new StringBuilder("  --- ").append(label).append(" ---\n");
        int max = Math.max(goldenLines.size(), genLines.size());
        int shown = 0;
        for (int i = 0; i < max && shown < 8; i++) {
            String g = i < goldenLines.size() ? goldenLines.get(i) : "<eof>";
            String a = i < genLines    .size() ? genLines    .get(i) : "<eof>";
            if (!g.equals(a)) {
                sb.append("  line ").append(i + 1).append(":\n");
                sb.append("    expected: ").append(g).append('\n');
                sb.append("    actual:   ").append(a).append('\n');
                shown++;
            }
        }
        if (goldenLines.size() != genLines.size()) {
            sb.append("  (golden ").append(goldenLines.size())
              .append(" lines vs generated ").append(genLines.size()).append(" lines)\n");
        }
        return sb.toString();
    }

    /** Copies only JTL input files (.jtlp, .jtl, .jtl_header, .def, .csv, .json). */
    private void copyInputFiles(final Path src, final Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a) throws IOException {
                Files.createDirectories(dst.resolve(src.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
                String name = file.getFileName().toString();
                if (isInputFile(name)) {
                    Files.copy(file, dst.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isInputFile(String name) {
        return name.endsWith(".jtlp")
            || name.endsWith(".jtl")
            || name.endsWith(".jtl_header")
            || name.endsWith(".def")
            || name.endsWith(".csv")
            || name.endsWith(".json");
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
