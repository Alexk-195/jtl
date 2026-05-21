import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.text.*;

/**
 * Code generation context. This will be shared during processing of one
 * template by main template and potential inner classes
 *
 */
public class JTLContext {

    /** Tool version major number. */
    public static final int majorVersion = 3;

    /** Tool version minor number. */
    public static final int minorVersion = 4;

    /**
     * Writer object which will be used for output. If its null the standard output
     * will be used.
     */
    public JTLResultWriter twriter;

    public boolean inManualCode;
    public String definitionFileName;
    public String templateFileName;

    /** If true we are currently in manual section and skipping the generated output. */
    public boolean skipUntilManualSectionEnd;

    /** Current user section key. */
    public String manualCodeKey;

    /** Skipped lines if processing user sections. */
    public int skippedLines;

    /** Current template line. */
    public int tline;

    /** Default manual pattern for start. */
    public static final String DefaultManualStartPattern = "//--jtl--@id@--begin--*";

    /** Default manual pattern for end. */
    public static final String DefaultManualEndPattern = "//--jtl--@id@--end--*";

    /** Current manual pattern for start. */
    public String ManualSectionStartPattern = DefaultManualStartPattern;

    /** Current manual pattern for end. */
    public String ManualSectionEndPattern = DefaultManualEndPattern;

    /** Default prefix. */
    public static final String DefaultManualCodePrefix = "//--jtl--";

    /** Default postfix. */
    public static final String DefaultManualCodePostfix = "*";

    /** Current prefix. */
    public String ManualCodePrefix = "//--jtl--";

    /** Current postfix. */
    public String ManualCodePostfix = "*";

    public void updateManualPatternsInWriter() {
        if (null != twriter) {
            twriter.setManualSectionBeginPattern(ManualSectionStartPattern);
            twriter.setManualSectionEndPattern(ManualSectionEndPattern);
        }
    }

    public void updateManualPatterns() {
        ManualSectionStartPattern = ManualCodePrefix + "@id@--begin--" + ManualCodePostfix;
        ManualSectionEndPattern = ManualCodePrefix + "@id@--end--" + ManualCodePostfix;
        updateManualPatternsInWriter();
    }

    /** Reference to the root entity. */
    public JTLEntity root;

    public JTLContext() {
        twriter = null;
        inManualCode = false;
        skipUntilManualSectionEnd = false;
        skippedLines = 0;
        manualCodeKey = null;
        root = null;
        tline = 0;
    }

    public void println(CharSequence c) throws IOException {
        if (skipUntilManualSectionEnd) {
            skippedLines++;
        } else {
            if (twriter == null) {
                JTLOut.out.println(c);
            } else {
                twriter.append(c);
            }
        }
    }

    public void manual_begin(JTLEntity e) throws Exception {
        manual_begin(e.fullpath());
    }

    /**
     * Start of manual code section. The string s is the key for this section and
     * will be used to identify it in the generated java file.
     */
    public void manual_begin(String s) throws Exception {
        if (inManualCode) {
            throw new Exception("Nested manual code sections are not allowed");
        }
        if (twriter == null) {
            throw new Exception("Manual code section can only be used after a file() commando was issued. Section ID=" + s);
        }

        inManualCode = true;
        manualCodeKey = s;

        if (twriter.copyManualSection(s, twriter)) {
            skipUntilManualSectionEnd = true;
        } else {
            twriter.write(twriter.getManualSectionID_Begin(s));
        }
    }

    /** End of manual code section. */
    public void manual_end() throws Exception {
        if (!inManualCode) {
            throw new Exception("End of manual code without start");
        }
        inManualCode = false;

        if (skipUntilManualSectionEnd) {
            skipUntilManualSectionEnd = false;
        } else {
            twriter.write(twriter.getManualSectionID_End(manualCodeKey));
        }
    }

    /** Reads a file and returns its contents as vector of strings. */
    public Vector<String> load_file(String fname) throws Exception {
        Vector<String> filebuffer = new Vector<String>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(fname), StandardCharsets.UTF_8))) {
            String line = in.readLine();
            while (line != null) {
                filebuffer.add(line);
                line = in.readLine();
            }
        }
        return filebuffer;
    }
}
