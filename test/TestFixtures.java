import java.io.*;
import java.nio.file.*;

/** Helpers for writing temporary fixture files used by parser/writer/compiler tests. */
public class TestFixtures {

    /** Creates a temp file with the given suffix and UTF-8 contents. Caller is responsible for cleanup. */
    public static File tempFile(String suffix, String contents) throws IOException {
        File f = File.createTempFile("jtl-test-", suffix);
        f.deleteOnExit();
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), "UTF8")) {
            w.write(contents);
        }
        return f;
    }

    /** Reads a UTF-8 file fully. */
    public static String readUtf8(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF8");
    }
}
