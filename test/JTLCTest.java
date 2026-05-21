import java.io.File;

/**
 * Unit tests for {@link JTLC} — the .jtl → .java translator. Tests compile small
 * .jtl fragments and assert the generated source contains the expected fragments.
 * They do not invoke javac on the result.
 */
public class JTLCTest {

    private static void assertContains(String h, String n) { TestHarness.assertContains(h, n); }
    private static void assertTrue(boolean c)              { TestHarness.assertTrue(c); }

    /** Compiles the given template body and returns the generated Java source. */
    private String compile(String jtl) throws Exception {
        File f = TestFixtures.tempFile(".jtl", jtl);
        JTLC c = new JTLC(f.getAbsolutePath());
        c.run();
        File java = new File(c.getJavaTemplateFile());
        java.deleteOnExit();
        return TestFixtures.readUtf8(java);
    }

    public void testGeneratedHeaderDeclaresClassExtendingTemplate() throws Exception {
        String out = compile("hello\n");
        assertContains(out, "extends JTLTemplate");
        assertContains(out, "protected void process(Object object)");
    }

    public void testPlainTextLineBecomesPrintln() throws Exception {
        String out = compile("Hello, world\n");
        assertContains(out, "println(\"Hello, world\");");
    }

    public void testInlineExpressionInjectsConcat() throws Exception {
        // @[ ... ]@ inline expression
        String out = compile("name=@[entity.name]@!\n");
        assertContains(out, "println(\"name=\"+entity.name+\"!\");");
    }

    public void testSingleLineCodeStartsWithAt() throws Exception {
        String out = compile("@ int x = 1;\n");
        assertContains(out, " int x = 1;");
    }

    public void testMultiLineCodeBlock() throws Exception {
        // Inside @< ... @> every line is raw Java, so mixed text/expressions must use
        // single-line @ prefix for code with text outside the block.
        String jtl = "@ for (int i=0; i<3; i++) {\n"
                   + "line @[i]@\n"
                   + "@ }\n";
        String out = compile(jtl);
        assertContains(out, "for (int i=0; i<3; i++) {");
        assertContains(out, "println(\"line \"+i+\"\");");
        assertContains(out, " }");
    }

    public void testCodeBlockBetweenAtBrackets() throws Exception {
        // Entire @< ... @> body emitted verbatim as Java.
        String jtl = "@<\n"
                   + "int sum = 0;\n"
                   + "for (int k=0;k<5;k++) sum += k;\n"
                   + "@>\n";
        String out = compile(jtl);
        assertContains(out, "int sum = 0;");
        assertContains(out, "for (int k=0;k<5;k++) sum += k;");
    }

    public void testInternalQuotesAreEscaped() throws Exception {
        String out = compile("say \"hi\"\n");
        // Plain text containing literal quotes must be escaped in the emitted Java string.
        assertContains(out, "println(\"say \\\"hi\\\"\");");
    }
}
