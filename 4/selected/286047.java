package net.sourceforge.jenesis4java.impl;

import java.io.BufferedInputStream;
import java.io.StringWriter;
import net.sourceforge.jenesis4java.CompilationUnit;

/**
 * Implementation of <code>VirtualCompiler</code> which makes a system
 * call to jikes.  
 */
public class Jikes extends MVC {

    /**
     * Constructs a new Jikes compiler instance.
     */
    public Jikes() {
        super();
    }

    public boolean compile(CompilationUnit unit) throws java.io.IOException {
        String sep = System.getProperty("path.separator");
        StringBuffer cmd = new StringBuffer("jikes ");
        if (dest != null) cmd.append(" -d ").append(dest);
        if (srcs.size() > 0) {
            cmd.append(" -sourcepath");
            for (int i = 0; i < srcs.size(); i++) {
                if (i > 0) cmd.append(sep);
                cmd.append((String) libs.elementAt(i));
            }
        }
        cmd.append(" -classpath ").append(unit.getCodebase());
        for (int i = 0; i < libs.size(); i++) cmd.append(sep).append((String) libs.elementAt(i));
        for (int i = 0; i < opts.size(); i++) cmd.append((String) opts.elementAt(i));
        cmd.append(' ').append(toFilename(unit));
        String result = execute(cmd.toString());
        if (result != null && result.length() > 0) {
            System.out.println(result);
            return false;
        } else {
            return true;
        }
    }

    protected String execute(String cmd) {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        BufferedInputStream in = null;
        StringWriter out = null;
        try {
            p = r.exec(cmd);
            in = new BufferedInputStream(p.getErrorStream());
            out = new StringWriter();
            int c;
            while ((c = in.read()) != -1) out.write(c);
            out.close();
            in.close();
            return out.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return out == null ? null : out.toString();
        } finally {
            Exception exeption = null;
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    exeption = e;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    exeption = e;
                }
            }
            if (exeption != null) {
                throw new RuntimeException(exeption);
            }
        }
    }
}
