import java.lang.reflect.*;
import java.util.*;

/**
 * Minimal zero-dependency test runner. Test classes are listed in {@link #TEST_CLASSES};
 * every public no-arg instance method whose name starts with {@code test} is executed.
 * A test passes if it returns normally and fails if it throws.
 */
public class TestHarness {

    private static final String[] TEST_CLASSES = {
        "JTLEntityTest",
        "JTLDefinitionParserTest",
        "JTLCTest",
        "JTLResultWriterTest",
        "JTLContextTest",
        "JTLTemplateTest",
        "GoldenFileTest",
    };

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failures = new ArrayList<String>();

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(boolean cond) {
        if (!cond) throw new AssertionError("expected true");
    }

    public static void assertTrue(String msg, boolean cond) {
        if (!cond) throw new AssertionError(msg);
    }

    public static void assertFalse(boolean cond) {
        if (cond) throw new AssertionError("expected false");
    }

    public static void assertNotNull(Object o) {
        if (o == null) throw new AssertionError("expected non-null");
    }

    public static void assertNull(Object o) {
        if (o != null) throw new AssertionError("expected null but was <" + o + ">");
    }

    public static void assertContains(String haystack, String needle) {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError("expected to contain <" + needle + "> in:\n" + haystack);
        }
    }

    public static void fail(String msg) {
        throw new AssertionError(msg);
    }

    public static void main(String[] args) throws Exception {
        for (String name : TEST_CLASSES) {
            runClass(name);
        }

        System.out.println();
        System.out.println("===========================================");
        System.out.println("Tests: " + (passed + failed) + ", Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println();
            System.out.println("Failures:");
            for (String f : failures) System.out.println("  " + f);
            System.exit(1);
        }
    }

    private static void runClass(String name) throws Exception {
        Class<?> cls = Class.forName(name);
        System.out.println();
        System.out.println("--- " + name + " ---");
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().startsWith("test")
                    && m.getParameterCount() == 0
                    && Modifier.isPublic(m.getModifiers())) {
                runMethod(cls, m);
            }
        }
    }

    private static void runMethod(Class<?> cls, Method m) {
        String label = cls.getSimpleName() + "." + m.getName();
        try {
            Object inst = cls.getDeclaredConstructor().newInstance();
            m.invoke(inst);
            passed++;
            System.out.println("  PASS  " + m.getName());
        } catch (InvocationTargetException ite) {
            failed++;
            Throwable cause = ite.getCause();
            String reason = cause.getClass().getSimpleName()
                    + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
            failures.add(label + " — " + reason);
            System.out.println("  FAIL  " + m.getName() + " — " + reason);
            cause.printStackTrace(System.out);
        } catch (Exception e) {
            failed++;
            failures.add(label + " — " + e.getMessage());
            System.out.println("  ERROR " + m.getName() + " — " + e.getMessage());
        }
    }
}
