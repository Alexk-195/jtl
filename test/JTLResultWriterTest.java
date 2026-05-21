import java.io.File;
import java.io.StringWriter;

/**
 * Unit tests for {@link JTLResultWriter} — focuses on the manual-section preservation
 * logic, which is the highest-risk area (silent data loss if it regresses).
 */
public class JTLResultWriterTest {

    private static final String BEGIN = "//--jtl--@id@--begin--";
    private static final String END   = "//--jtl--@id@--end--";

    private static void assertEquals(Object e, Object a) { TestHarness.assertEquals(e, a); }
    private static void assertTrue(boolean c)            { TestHarness.assertTrue(c); }
    private static void assertTrue(String m, boolean c)  { TestHarness.assertTrue(m, c); }
    private static void assertContains(String h, String n) { TestHarness.assertContains(h, n); }

    private File tempPath() throws Exception {
        File f = File.createTempFile("jtl-writer-", ".out");
        f.deleteOnExit();
        f.delete(); // we want the path; the writer creates it
        return f;
    }

    public void testWritesNewFileWhenAbsent() throws Exception {
        File target = tempPath();
        JTLResultWriter w = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w.write("hello");
        w.close();
        assertTrue("file should have been created", target.exists());
        assertContains(TestFixtures.readUtf8(target), "hello");
    }

    public void testIdenticalContentDoesNotCreateBackup() throws Exception {
        File target = tempPath();
        // Real templates call write() once per line without trailing newlines (close()
        // adds them via println). Mimic that or the in-memory vs on-disk comparison
        // never matches.
        JTLResultWriter w1 = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w1.write("same");
        w1.close();

        File parent = target.getParentFile();
        int bakBefore = countBackups(parent, target.getName());

        JTLResultWriter w2 = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w2.write("same");
        w2.close();

        int bakAfter = countBackups(parent, target.getName());
        assertEquals(bakBefore, bakAfter);
    }

    public void testChangedContentCreatesBackup() throws Exception {
        File target = tempPath();
        JTLResultWriter w1 = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w1.write("first");
        w1.close();

        File parent = target.getParentFile();
        int bakBefore = countBackups(parent, target.getName());

        JTLResultWriter w2 = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w2.write("second");
        w2.close();

        int bakAfter = countBackups(parent, target.getName());
        assertTrue("expected new backup file", bakAfter > bakBefore);
    }

    public void testCopyManualSectionPreservesUserBlock() throws Exception {
        File target = tempPath();
        // Seed the target with an existing file containing a manual section.
        String userBody = "USER EDITED CONTENT\n";
        String existing =
                "generated header\n" +
                BEGIN.replace("@id@", "key1") + "\n" +
                userBody +
                END.replace("@id@", "key1") + "\n" +
                "generated footer\n";
        java.nio.file.Files.write(target.toPath(), existing.getBytes("UTF8"));

        JTLResultWriter w = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w.setManualSectionBeginPattern(BEGIN);
        w.setManualSectionEndPattern(END);

        StringWriter captured = new StringWriter();
        boolean found = w.copyManualSection("key1", captured);
        assertTrue("manual section should have been found", found);

        String copied = captured.toString();
        assertContains(copied, BEGIN.replace("@id@", "key1"));
        assertContains(copied, "USER EDITED CONTENT");
        assertContains(copied, END.replace("@id@", "key1"));
    }

    public void testCopyManualSectionReturnsFalseWhenAbsent() throws Exception {
        File target = tempPath();
        java.nio.file.Files.write(target.toPath(), "no markers here\n".getBytes("UTF8"));

        JTLResultWriter w = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w.setManualSectionBeginPattern(BEGIN);
        w.setManualSectionEndPattern(END);

        StringWriter captured = new StringWriter();
        boolean found = w.copyManualSection("missing", captured);
        assertTrue("expected not found", !found);
        assertEquals("", captured.toString());
    }

    public void testManualPatternsResolveIdPlaceholder() throws Exception {
        File target = tempPath();
        JTLResultWriter w = new JTLResultWriter(target.getAbsolutePath(), "def", "tmpl");
        w.setManualSectionBeginPattern("/* BEGIN @id@ */");
        w.setManualSectionEndPattern("/* END @id@ */");
        assertEquals("/* BEGIN section1 */", w.getManualSectionID_Begin("section1"));
        assertEquals("/* END section1 */",   w.getManualSectionID_End("section1"));
    }

    private static int countBackups(File parent, String baseName) {
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
