import java.io.File;

/** Unit tests for {@link JTLDefinitionParser} covering DEF, JSON, and CSV formats. */
public class JTLDefinitionParserTest {

    private static void assertEquals(Object e, Object a) { TestHarness.assertEquals(e, a); }
    private static void assertEquals(int e, int a)       { TestHarness.assertEquals(e, a); }
    private static void assertTrue(boolean c)            { TestHarness.assertTrue(c); }
    private static void assertNotNull(Object o)          { TestHarness.assertNotNull(o); }
    private static void fail(String msg)                 { TestHarness.fail(msg); }

    // ----- DEF -----

    public void testDefSingleEntityNoParams() throws Exception {
        File f = TestFixtures.tempFile(".def", "module {}");
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());
        assertEquals("module", p.root.name);
        assertEquals(0, p.root.params.size());
        assertEquals(0, p.root.children.size());
    }

    public void testDefEntityWithParams() throws Exception {
        File f = TestFixtures.tempFile(".def", "thing(\"a\",b,\"with space\") {}");
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());
        assertEquals("thing", p.root.name);
        assertEquals(3, p.root.params.size());
        assertEquals("a", p.root.params.get(0));
        assertEquals("b", p.root.params.get(1));
        assertEquals("with space", p.root.params.get(2));
    }

    public void testDefNestedChildren() throws Exception {
        String def = "root { a(1) { leaf } b(2) }";
        File f = TestFixtures.tempFile(".def", def);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());

        assertEquals("root", p.root.name);
        assertEquals(2, p.root.children.size());

        JTLEntity a = p.root.child("a");
        assertNotNull(a);
        assertEquals("1", a.params.get(0));
        assertNotNull(a.child("leaf"));

        JTLEntity b = p.root.child("b");
        assertNotNull(b);
        assertEquals("2", b.params.get(0));
    }

    public void testDefRejectsUnknownExtension() {
        try {
            File f = TestFixtures.tempFile(".xyz", "module {}");
            new JTLDefinitionParser(f.getAbsolutePath());
            fail("expected exception for unrecognized extension");
        } catch (Exception expected) {
            // ok
        }
    }

    // ----- JSON -----

    public void testJsonFlatObject() throws Exception {
        String json = "{ \"name\": \"alice\", \"age\": 30 }";
        File f = TestFixtures.tempFile(".json", json);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());
        assertEquals("json", p.root.name);
        assertEquals(2, p.root.children.size());

        JTLEntity name = p.root.child("name");
        assertNotNull(name);
        assertEquals("alice", name.params.get(0));

        JTLEntity age = p.root.child("age");
        assertNotNull(age);
        assertEquals("30", age.params.get(0));
    }

    public void testJsonArrayBecomesElemChildren() throws Exception {
        String json = "{ \"tags\": [\"red\",\"green\",\"blue\"] }";
        File f = TestFixtures.tempFile(".json", json);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());

        JTLEntity tags = p.root.child("tags");
        assertNotNull(tags);
        assertEquals(JTLDefinitionParser.JSON_ARRAY_NAME, tags.params.get(0));
        assertEquals(3, tags.children.size());
        assertEquals(JTLDefinitionParser.JSON_ELEM_NAME, tags.children.get(0).name);
        assertEquals("red", tags.children.get(0).params.get(0));
        assertEquals("blue", tags.children.get(2).params.get(0));
    }

    public void testJsonNestedObject() throws Exception {
        String json = "{ \"outer\": { \"inner\": \"v\" } }";
        File f = TestFixtures.tempFile(".json", json);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());

        JTLEntity outer = p.root.child("outer");
        assertNotNull(outer);
        JTLEntity inner = outer.child("inner");
        assertNotNull(inner);
        assertEquals("v", inner.params.get(0));
    }

    // ----- CSV -----

    public void testCsvRowsBecomeElems() throws Exception {
        String csv = "alice;30;ny\nbob;25;la\n";
        File f = TestFixtures.tempFile(".csv", csv);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());

        assertEquals("csv", p.root.name);
        assertEquals(2, p.root.children.size());

        JTLEntity row0 = p.root.children.get(0);
        assertEquals(JTLDefinitionParser.CSV_ELEM_NAME, row0.name);
        assertEquals("alice", row0.params.get(0));
        assertEquals("30",    row0.params.get(1));
        assertEquals("ny",    row0.params.get(2));

        JTLEntity row1 = p.root.children.get(1);
        assertEquals("bob", row1.params.get(0));
    }

    public void testCsvSkipsEmptyRows() throws Exception {
        String csv = "a;b\n\nc;d\n";
        File f = TestFixtures.tempFile(".csv", csv);
        JTLDefinitionParser p = new JTLDefinitionParser(f.getAbsolutePath());
        assertEquals(2, p.root.children.size());
    }
}
