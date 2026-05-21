import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;

/**
 * Unit tests for {@link JTLTemplate} — covers file/close, folder,
 * manual_begin/manual_end, load_file, and disable_backup/enable_backup.
 * All filesystem work uses temp files/dirs that are cleaned up on JVM exit.
 */
public class JTLTemplateTest {

    private static final String BEGIN_PAT = "//--jtl--@id@--begin--*";
    private static final String END_PAT   = "//--jtl--@id@--end--*";

    private static void assertEquals(Object e, Object a) { TestHarness.assertEquals(e, a); }
    private static void assertTrue(String m, boolean c)  { TestHarness.assertTrue(m, c); }
    private static void assertContains(String h, String n) { TestHarness.assertContains(h, n); }

    /** Returns a temp file path that does not yet exist (writer creates it). */
    private File tempFilePath() throws Exception {
        File f = File.createTempFile("jtl-tmpl-", ".out");
        f.deleteOnExit();
        f.delete();
        return f;
    }

    /** Builds a fresh JTLTemplate with a clean context. */
    private JTLTemplate freshTemplate() {
        JTLTemplate t = new JTLTemplate();
        t.ctx(new JTLContext());
        return t;
    }

    // -------------------------------------------------------------------------
    // file / close
    // -------------------------------------------------------------------------

    public void testFileCreatesOutputFile() throws Exception {
        File target = tempFilePath();
        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        t.println("hello");
        t.close();
        assertTrue("file should exist after close", target.exists());
        target.delete();
    }

    public void testFileWritesContent() throws Exception {
        File target = tempFilePath();
        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        t.println("line one");
        t.println("line two");
        t.close();
        String content = TestFixtures.readUtf8(target);
        assertContains(content, "line one");
        assertContains(content, "line two");
        target.delete();
    }

    public void testCloseIsIdempotentWhenNoFileOpen() throws Exception {
        JTLTemplate t = freshTemplate();
        // close() with no file open must not throw
        t.close();
        t.close();
    }

    // -------------------------------------------------------------------------
    // folder
    // -------------------------------------------------------------------------

    public void testFolderCreatesNewDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("jtl-tmpl-folder-");
        tempDir.toFile().deleteOnExit();
        Path newDir = tempDir.resolve("sub");
        JTLTemplate t = freshTemplate();
        t.folder(newDir.toString());
        assertTrue("sub-directory should have been created", Files.isDirectory(newDir));
        newDir.toFile().delete();
        tempDir.toFile().delete();
    }

    public void testFolderDoesNotThrowOnExistingDirectory() throws Exception {
        Path existing = Files.createTempDirectory("jtl-tmpl-existing-");
        existing.toFile().deleteOnExit();
        JTLTemplate t = freshTemplate();
        t.folder(existing.toString()); // must not throw
        assertTrue("existing directory should still be there", Files.isDirectory(existing));
        existing.toFile().delete();
    }

    // -------------------------------------------------------------------------
    // manual_begin / manual_end
    // -------------------------------------------------------------------------

    public void testManualBeginEndWritesMarkers() throws Exception {
        File target = tempFilePath();
        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        t.manual_begin("sec1");
        t.println("generated body");
        t.manual_end();
        t.close();

        String content = TestFixtures.readUtf8(target);
        assertContains(content, "//--jtl--sec1--begin--*");
        assertContains(content, "//--jtl--sec1--end--*");
        assertContains(content, "generated body");
        target.delete();
    }

    public void testManualBeginEndPreservesUserContent() throws Exception {
        File target = tempFilePath();
        // Seed the file with user-edited content inside a manual section.
        String existingContent =
                "//--jtl--sec1--begin--*\r\n" +
                "USER CODE\r\n" +
                "//--jtl--sec1--end--*\r\n";
        try (OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(target), "UTF8")) {
            ow.write(existingContent);
        }
        target.deleteOnExit();

        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        t.manual_begin("sec1");
        t.println("this should be skipped");
        t.manual_end();
        t.close();

        String content = TestFixtures.readUtf8(target);
        assertContains(content, "USER CODE");
        target.delete();
    }

    public void testManualBeginThrowsWhenNoFileOpen() throws Exception {
        JTLTemplate t = freshTemplate();
        try {
            t.manual_begin("key");
            TestHarness.fail("expected exception when no file is open");
        } catch (Exception e) {
            // expected
        }
    }

    public void testManualEndThrowsWithoutBegin() throws Exception {
        File target = tempFilePath();
        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        try {
            t.manual_end();
            TestHarness.fail("expected exception for unmatched manual_end");
        } catch (Exception e) {
            // expected
        }
        target.delete();
    }

    public void testNestedManualBeginThrows() throws Exception {
        File target = tempFilePath();
        JTLTemplate t = freshTemplate();
        t.file(target.getAbsolutePath());
        t.manual_begin("outer");
        try {
            t.manual_begin("inner");
            TestHarness.fail("expected exception for nested manual_begin");
        } catch (Exception e) {
            // expected
        }
        target.delete();
    }

    // -------------------------------------------------------------------------
    // load_file
    // -------------------------------------------------------------------------

    public void testLoadFileReturnsLines() throws Exception {
        File src = File.createTempFile("jtl-load-", ".txt");
        src.deleteOnExit();
        try (OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(src), "UTF8")) {
            ow.write("alpha\nbeta\ngamma\n");
        }
        JTLTemplate t = freshTemplate();
        Vector<String> lines = t.load_file(src.getAbsolutePath());
        assertEquals(3, lines.size());
        assertEquals("alpha", lines.get(0));
        assertEquals("beta",  lines.get(1));
        assertEquals("gamma", lines.get(2));
        src.delete();
    }

    private void assertEquals(int e, int a) { TestHarness.assertEquals(e, a); }

    public void testLoadFileMissingFileThrows() throws Exception {
        JTLTemplate t = freshTemplate();
        try {
            t.load_file("/no/such/file/jtl-missing.txt");
            TestHarness.fail("expected exception for missing file");
        } catch (Exception e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // disable_backup / enable_backup
    // -------------------------------------------------------------------------

    public void testDisableBackupPreventsBackupOnChange() throws Exception {
        File target = tempFilePath();
        // First write.
        JTLTemplate t1 = freshTemplate();
        t1.file(target.getAbsolutePath());
        t1.println("version one");
        t1.close();

        File parent = target.getParentFile();
        int before = countBaks(parent, target.getName());

        // Second write with different content but backup disabled.
        JTLTemplate t2 = freshTemplate();
        t2.file(target.getAbsolutePath());
        t2.disable_backup();
        t2.println("version two");
        t2.close();

        int after = countBaks(parent, target.getName());
        assertEquals(before, after);
        target.delete();
    }

    public void testEnableBackupCreatesBackupOnChange() throws Exception {
        File target = tempFilePath();
        // First write.
        JTLTemplate t1 = freshTemplate();
        t1.file(target.getAbsolutePath());
        t1.println("version one");
        t1.close();

        File parent = target.getParentFile();
        int before = countBaks(parent, target.getName());

        // Second write — backup is enabled by default.
        JTLTemplate t2 = freshTemplate();
        t2.file(target.getAbsolutePath());
        t2.enable_backup(); // explicit; same as default
        t2.println("version two");
        t2.close();

        int after = countBaks(parent, target.getName());
        assertTrue("backup should have been created", after > before);
        target.delete();
    }

    private static int countBaks(File parent, String baseName) {
        File[] files = parent.listFiles();
        if (files == null) return 0;
        int n = 0;
        for (File f : files) {
            if (f.getName().startsWith(baseName) && f.getName().endsWith(".bak")) {
                f.deleteOnExit();
                n++;
            }
        }
        return n;
    }
}
