import java.io.*;

/**
 * Tests for the JTLContext manual-section state machine:
 * inManualCode, skipUntilManualSectionEnd, manualCodeKey, and skippedLines.
 */
public class JTLContextTest {

    private static final String BEGIN_PATTERN = "//--jtl--@id@--begin--*";
    private static final String END_PATTERN   = "//--jtl--@id@--end--*";

    private static void assertEquals(Object e, Object a) { TestHarness.assertEquals(e, a); }
    private static void assertEquals(int e, int a)       { TestHarness.assertEquals(e, a); }
    private static void assertTrue(boolean c)            { TestHarness.assertTrue(c); }
    private static void assertTrue(String m, boolean c)  { TestHarness.assertTrue(m, c); }
    private static void assertFalse(boolean c)           { TestHarness.assertFalse(c); }
    private static void assertNull(Object o)             { TestHarness.assertNull(o); }
    private static void assertContains(String h, String n) { TestHarness.assertContains(h, n); }
    private static void fail(String msg)                 { TestHarness.fail(msg); }

    /** Context backed by an in-memory writer; oldContent may be null for a fresh file. */
    private JTLContext makeContext(StringWriter out, String oldContent) throws Exception {
        Reader old = oldContent == null ? null : new StringReader(oldContent);
        JTLResultWriter rw = new JTLResultWriter(out, old, "def", "tmpl");
        rw.setManualSectionBeginPattern(BEGIN_PATTERN);
        rw.setManualSectionEndPattern(END_PATTERN);

        JTLContext ctx = new JTLContext();
        ctx.twriter = rw;
        ctx.ManualSectionStartPattern = BEGIN_PATTERN;
        ctx.ManualSectionEndPattern   = END_PATTERN;
        return ctx;
    }

    /** Close the writer and return the captured output string. */
    private String flush(StringWriter out, JTLContext ctx) throws IOException {
        ctx.twriter.close();
        return out.toString();
    }

    private String beginMarker(String id) { return BEGIN_PATTERN.replace("@id@", id); }
    private String endMarker(String id)   { return END_PATTERN.replace("@id@", id); }

    /** Minimal old-file content containing one manual section. */
    private String oldFile(String id, String userContent) {
        return beginMarker(id) + "\r\n" + userContent + "\r\n" + endMarker(id) + "\r\n";
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    public void testInitialState() {
        JTLContext ctx = new JTLContext();
        assertFalse(ctx.inManualCode);
        assertFalse(ctx.skipUntilManualSectionEnd);
        assertNull(ctx.manualCodeKey);
        assertEquals(0, ctx.skippedLines);
    }

    // -------------------------------------------------------------------------
    // println routing
    // -------------------------------------------------------------------------

    public void testPrintlnWritesToWriter() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);
        ctx.println("hello world");
        assertContains(flush(out, ctx), "hello world");
    }

    public void testPrintlnSkipsLinesWhenSkipFlagSet() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, oldFile("sec1", "OLD USER LINE"));

        ctx.manual_begin("sec1");
        assertTrue("should be in skip mode", ctx.skipUntilManualSectionEnd);

        int before = ctx.skippedLines;
        ctx.println("generated line A");
        ctx.println("generated line B");
        assertEquals(before + 2, ctx.skippedLines);

        ctx.manual_end();
        String result = flush(out, ctx);
        assertTrue("skipped lines must not appear in output",
                !result.contains("generated line A") && !result.contains("generated line B"));
    }

    // -------------------------------------------------------------------------
    // manual_begin error paths
    // -------------------------------------------------------------------------

    public void testManualBeginWithoutWriterThrows() {
        JTLContext ctx = new JTLContext();
        try {
            ctx.manual_begin("k");
            fail("expected exception — no writer");
        } catch (Exception e) {
            // expected
        }
    }

    public void testManualBeginNestedThrows() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);
        ctx.manual_begin("outer");
        try {
            ctx.manual_begin("inner");
            fail("expected exception — nested manual sections");
        } catch (Exception e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // manual_begin happy paths
    // -------------------------------------------------------------------------

    public void testManualBeginNewSectionSetsState() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);

        ctx.manual_begin("s1");

        assertTrue("inManualCode should be true", ctx.inManualCode);
        assertTrue("should not skip for a brand-new section", !ctx.skipUntilManualSectionEnd);
        assertEquals("s1", ctx.manualCodeKey);
    }

    public void testManualBeginNewSectionWritesBeginMarker() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);

        ctx.manual_begin("s1");
        ctx.manual_end();
        assertContains(flush(out, ctx), beginMarker("s1"));
    }

    public void testManualBeginExistingSectionActivatesSkip() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, oldFile("s2", "USER CODE"));

        ctx.manual_begin("s2");

        assertTrue("inManualCode should be true", ctx.inManualCode);
        assertTrue("should be skipping — section already exists", ctx.skipUntilManualSectionEnd);
        assertEquals("s2", ctx.manualCodeKey);
    }

    // -------------------------------------------------------------------------
    // manual_end error paths
    // -------------------------------------------------------------------------

    public void testManualEndWithoutBeginThrows() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);
        try {
            ctx.manual_end();
            fail("expected exception — end without begin");
        } catch (Exception e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // manual_end happy paths
    // -------------------------------------------------------------------------

    public void testManualEndClearsInManualCode() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);
        ctx.manual_begin("k");
        ctx.manual_end();
        assertTrue("inManualCode should be false after end", !ctx.inManualCode);
    }

    public void testManualEndClearsSkipFlag() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, oldFile("k", "USER"));

        ctx.manual_begin("k");
        assertTrue(ctx.skipUntilManualSectionEnd);
        ctx.manual_end();
        assertTrue("skipUntilManualSectionEnd cleared after end", !ctx.skipUntilManualSectionEnd);
        assertTrue("inManualCode cleared after end", !ctx.inManualCode);
    }

    // -------------------------------------------------------------------------
    // Full begin/end cycles
    // -------------------------------------------------------------------------

    public void testFullCycleNewSectionEmitsMarkersAndContent() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);

        ctx.manual_begin("id1");
        ctx.println("generated content");
        ctx.manual_end();
        String result = flush(out, ctx);

        assertContains(result, beginMarker("id1"));
        assertContains(result, "generated content");
        assertContains(result, endMarker("id1"));
    }

    public void testFullCycleExistingSectionPreservesUserContent() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, oldFile("id2", "USER PRESERVED CODE"));

        ctx.manual_begin("id2");
        ctx.println("generated — should be skipped");
        ctx.manual_end();
        String result = flush(out, ctx);

        assertContains(result, "USER PRESERVED CODE");
        assertTrue("generated content must not appear", !result.contains("generated — should be skipped"));
    }

    public void testSkippedLinesCountedCorrectly() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, oldFile("cnt", "user line"));

        assertEquals(0, ctx.skippedLines);
        ctx.manual_begin("cnt");
        ctx.println("line 1");
        ctx.println("line 2");
        ctx.println("line 3");
        assertEquals(3, ctx.skippedLines);
        ctx.manual_end();
    }

    public void testSkippedLinesNotIncrementedForNormalOutput() throws Exception {
        StringWriter out = new StringWriter();
        JTLContext ctx = makeContext(out, null);

        ctx.println("normal line");
        assertEquals(0, ctx.skippedLines);
    }
}
