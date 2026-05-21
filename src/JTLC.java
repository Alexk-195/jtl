
/**
 * Compiles jtl file (Java template language) into java file.
 *
 *
 */
import java.io.*;
import java.nio.charset.StandardCharsets;

public class JTLC {

    /** name of template file */
    String tname;
    BufferedReader templateReader;
    int linenr;
    boolean inCodeBlock;
    String classFileTemplate;
    String jtlHeaderFileName;
    String javaTemplateFile;
    PrintStream pout;
    public boolean verbose;

    // Escape sequences
    static final String CS_B = "@<"; // code section start
    static final String CS_E = "@>"; // ... end
    static final String CW_B = "@["; // code word start
    static final String CW_E = "]@"; // ... end
    static final String CL = "@"; // code line

    /**
     * All template output shall be done using this method.
     */
    void tout(String s) {
        pout.println(s);
    }

    /**
     * Constructor requires name of template file (jtl file).
     */
    public JTLC(String tname) {
        this.tname = tname;

    }

    void log(String s) {
        if (verbose)
            JTLOut.out.println(s);
    }

    void err(String s) {
        JTLOut.err.println(s);
    }

    public String getJavaTemplateFile() {
        return javaTemplateFile;
    }

    /**
     * Checks if line starts with the given prefix. If yes, returns the line with
     * "front" replaced by "repl". Otherwise returns null.
     */
    protected String checkReplace(String line, String front, String repl) {
        String ts = line.trim();
        if (ts.startsWith(front)) {
            return repl + line.substring(line.indexOf(front) + front.length());
        }
        return null;
    }

    /**
     * Each created java file will have this header.
     */
    protected void printTemplateHeader() {

        tout("import java.io.*;");
        tout("import java.util.*;");
        tout("import java.text.*;");

        try (BufferedReader headerReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(jtlHeaderFileName), StandardCharsets.UTF_8))) {
            String line = headerReader.readLine();
            while (line != null) {
                tout(line);
                line = headerReader.readLine();
            }
        } catch (FileNotFoundException e) {
            log("No jtl_header file found. Proceed with default header");
        } catch (IOException e) {
            err(e.getLocalizedMessage());
            e.printStackTrace(JTLOut.err);
        }

        tout("public class " + classFileTemplate + " extends JTLTemplate");
        tout("{");
        tout("  static public void main(String[] args) {");
        tout("      JTLTemplate._run(args,new " + classFileTemplate + "(new JTLContext()),\"" + classFileTemplate
                + "\");");
        tout("  }");
        tout("  public " + classFileTemplate + "(JTLContext ctxIn) { ctx(ctxIn); }");
        tout("  @Override");
        tout("  protected void process(Object object)  throws Exception {");
        tout("  JTLEntity entity=null;");
        tout("  if ( object instanceof JTLEntity) entity = (JTLEntity)object;");
        tout("  // Code from jtl file follows");
    }

    /**
     * Each created java file will have this footer.
     */
    protected void printTemplateFooter() {
        tout("} // end of process");
        tout("} // end of class");
        tout("");
    }

    /**
     * Prints a java code line.
     */
    protected void print_code(String s) {
        if (s.trim().endsWith(";")) {
            tout(String.format("%-80s_line(%d);", s, linenr));
        } else {
            tout(s);
        }
    }

    /**
     * Prints a no-code line but replaces code-words (=java statements).
     */
    protected void print_nocode(String s) throws Exception {

        s = s.replace("\\", "\\\\");

        StringBuilder res = new StringBuilder();
        boolean inCode = false;
        final int n = s.length();
        final char cwB0 = CW_B.charAt(0), cwB1 = CW_B.charAt(1);
        final char cwE0 = CW_E.charAt(0), cwE1 = CW_E.charAt(1);

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char next = (i + 1 < n) ? s.charAt(i + 1) : '\0';

            if (c == cwB0 && next == cwB1) {
                if (inCode) {
                    throw new Exception("Nested code sections not allowed : " + s);
                }
                res.append("\"+");
                inCode = true;
                i++;
            } else if (c == cwE0 && next == cwE1) {
                if (!inCode) {
                    throw new Exception("Unmatched " + CW_E + " symbol in :" + s);
                }
                res.append("+\"");
                inCode = false;
                i++;
            } else if (c == '"' && !inCode) {
                res.append("\\\"");
            } else {
                res.append(c);
            }
        }

        print_code("println(\"" + res + "\");");
    }

    /**
     * Processes a single line in the jtl file.
     */
    protected void processTemplateLine(String line) throws Exception {
        String ts = checkReplace(line, CS_B, "");
        if (ts != null) {
            if (!inCodeBlock) {
                inCodeBlock = true;
                print_code(ts);
                return;
            } else {
                JTLOut.err.print("Error: Found nested " + CS_B + " in line ");
                JTLOut.err.println(linenr);
                throw new Exception("Error: Found nested " + CS_B + " in: " + line);
            }
        }

        ts = checkReplace(line, CS_E, "");
        if (ts != null) {
            if (inCodeBlock) {
                inCodeBlock = false;
                // it probably was not intended to print some empty text after closing bracket.
                if (ts.trim().length() != 0) {
                    print_nocode(ts);
                }
                return;
            } else {
                JTLOut.err.print("Error: Found unmatched " + CS_E + " in line ");
                JTLOut.err.println(linenr);
                throw new Exception("Error: unmatched " + CS_E + " in :" + line);
            }
        }

        // check for expression at the beginning, since it has same start as code line
        if (checkReplace(line, CW_B, CW_B) != null) {
            print_nocode(line);
            return;
        }

        // check for single line code sequence
        ts = checkReplace(line, CL, "");
        if (ts != null) {
            print_code(ts);
            return;
        }

        // if in code block print code
        if (inCodeBlock) {
            print_code(line);
            return;
        }

        // otherwise in no-code sections. print with replaced expressions
        print_nocode(line);
    }

    /**
     * Processes a jtl template file line by line. templateReader was already
     * set up to read the jtl file.
     */
    protected void processTemplate() throws Exception {
        log("Generating java file: " + javaTemplateFile);

        linenr = 0;
        printTemplateHeader();
        String line = "";
        try {
            line = templateReader.readLine();
            linenr++;
            while (line != null) {
                processTemplateLine(line);
                line = templateReader.readLine();
                linenr++;
            }

        } catch (Exception e) {
            JTLOut.err.print("Template Line ");
            JTLOut.err.print(linenr);
            if (line != null) {
                JTLOut.err.println(": " + line.trim());
            }
            JTLOut.err.println(e.getMessage());
            throw e;
        }
        printTemplateFooter();
        log("Succesfully generated java file: " + javaTemplateFile);
        log("Compile it and run with your definition file");
    }

    /**
     * Runs the JTLC compiler for the jtl file provided in tname. Creates file
     * readers and writers, sets up class names, etc.
     */
    public void run() throws Exception {
        log("JTL file: " + tname);

        File file = new File(tname);
        jtlHeaderFileName = tname.replace(".jtl", ".jtl_header");
        classFileTemplate = file.getName().replace(".jtl", "");
        javaTemplateFile = file.getAbsolutePath().replace(".jtl", ".java");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(tname), StandardCharsets.UTF_8));
                PrintStream out = new PrintStream(new FileOutputStream(javaTemplateFile), false, "UTF-8")) {
            templateReader = reader;
            pout = out;
            processTemplate();
        } catch (IOException e) {
            JTLOut.err.println(e.getLocalizedMessage());
            e.printStackTrace(JTLOut.err);
        }
    }

    /**
     * Function called from the command line.
     */
    public static void main(String[] args) throws Exception {
        JTLC jtlc;

        JTLOut.out.println("JTCL: Java Template Language Compiler. Version " + JTLContext.majorVersion + "."
                + JTLContext.minorVersion);

        if (args.length == 0) {
            JTLOut.out.println("Usage: JTCL <template_file_1.jtl> <template_file_2.jtl> ... ");
        } else {

            for (String jtl_fname : args) {
                jtlc = new JTLC(jtl_fname);
                jtlc.run();
            }
        }

    }
}
