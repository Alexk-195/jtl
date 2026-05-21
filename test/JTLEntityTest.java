/** Unit tests for {@link JTLEntity} — pure tree-helper logic, no I/O. */
public class JTLEntityTest {

    private static void assertEquals(Object e, Object a) { TestHarness.assertEquals(e, a); }
    private static void assertEquals(int e, int a)       { TestHarness.assertEquals(e, a); }
    private static void assertTrue(boolean c)            { TestHarness.assertTrue(c); }
    private static void assertFalse(boolean c)           { TestHarness.assertFalse(c); }
    private static void assertNull(Object o)             { TestHarness.assertNull(o); }
    private static void fail(String msg)                 { TestHarness.fail(msg); }

    private JTLEntity root() {
        JTLEntity r = new JTLEntity();
        r.name = "root";
        return r;
    }

    public void testNewEntityHasEmptyState() {
        JTLEntity e = new JTLEntity();
        assertEquals("", e.name);
        assertNull(e.parent);
        assertEquals(0, e.params.size());
        assertEquals(0, e.children.size());
    }

    public void testAddChildSetsParentLink() {
        JTLEntity r = root();
        JTLEntity c = new JTLEntity();
        c.name = "c";
        r.addChild(c);
        assertTrue(c.parent == r);
        assertEquals(1, r.children.size());
    }

    public void testChildByNameReturnsMatch() {
        JTLEntity r = root();
        JTLEntity a = new JTLEntity(); a.name = "a"; r.addChild(a);
        JTLEntity b = new JTLEntity(); b.name = "b"; r.addChild(b);
        assertTrue(r.child("a") == a);
        assertTrue(r.child("b") == b);
        assertNull(r.child("missing"));
    }

    public void testChildDotDotReturnsParent() {
        JTLEntity r = root();
        JTLEntity c = new JTLEntity(); c.name = "c"; r.addChild(c);
        assertTrue(c.child("..") == r);
    }

    public void testHasChild() {
        JTLEntity r = root();
        JTLEntity c = new JTLEntity(); c.name = "c"; r.addChild(c);
        assertTrue(r.hasChild("c"));
        assertFalse(r.hasChild("nope"));
    }

    public void testFullpath() throws Exception {
        JTLEntity r = root();
        JTLEntity a = new JTLEntity(); a.name = "a"; r.addChild(a);
        JTLEntity b = new JTLEntity(); b.name = "b"; a.addChild(b);
        assertEquals("root", r.fullpath());
        assertEquals("root/a", a.fullpath());
        assertEquals("root/a/b", b.fullpath());
    }

    public void testChildByIndex() throws Exception {
        JTLEntity r = root();
        JTLEntity a = new JTLEntity(); a.name = "a"; r.addChild(a);
        JTLEntity b = new JTLEntity(); b.name = "b"; r.addChild(b);
        assertTrue(r.child(0) == a);
        assertTrue(r.child(1) == b);
    }

    public void testChildByIndexOutOfRangeThrows() {
        JTLEntity r = root();
        try {
            r.child(0);
            fail("expected exception on out-of-range index");
        } catch (Exception expected) {
            // ok
        }
    }

    public void testParamAccess() throws Exception {
        JTLEntity e = new JTLEntity();
        e.addParam("first").addParam("second");
        assertEquals("first", e.param(0));
        assertEquals("second", e.param(1));
    }

    public void testSetParam() throws Exception {
        JTLEntity e = new JTLEntity();
        e.addParam("first");
        e.setParam(0, "replaced");
        assertEquals("replaced", e.param(0));
    }

    public void testIsFirstAndIsLast() {
        JTLEntity r = root();
        JTLEntity a = new JTLEntity(); a.name = "a"; r.addChild(a);
        JTLEntity b = new JTLEntity(); b.name = "b"; r.addChild(b);
        JTLEntity c = new JTLEntity(); c.name = "c"; r.addChild(c);
        assertTrue(a.isFirst());
        assertFalse(a.isLast());
        assertFalse(b.isFirst());
        assertFalse(b.isLast());
        assertFalse(c.isFirst());
        assertTrue(c.isLast());
    }

    public void testIfFirstIfLast() {
        JTLEntity r = root();
        JTLEntity a = new JTLEntity(); a.name = "a"; r.addChild(a);
        JTLEntity b = new JTLEntity(); b.name = "b"; r.addChild(b);
        assertEquals(",", a.ifLast("", ","));
        assertEquals("",  b.ifLast("", ","));
        assertEquals("{", a.ifFirst("{", " "));
        assertEquals(" ", b.ifFirst("{", " "));
    }

    public void testToStringReturnsName() {
        JTLEntity e = new JTLEntity();
        e.name = "widget";
        assertEquals("widget", e.toString());
    }
}
