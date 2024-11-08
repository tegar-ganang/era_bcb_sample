package com.go.tea.util;

import java.lang.reflect.*;
import java.io.*;
import com.go.tea.compiler.*;
import com.go.tea.io.*;
import com.go.tea.runtime.*;

/******************************************************************************
 * A compiler implementation suitable for testing from a command console.
 * The runtime context is a PrintStream so that template output can go to
 * standard out.
 *
 * <p>Templates are read from files that must have the extension ".tea". The
 * code generated are Java class files which are written in the same directory
 * as the source files. Compilation error messages are sent to standard out.
 * 
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 6 $-->, <!--$$JustDate:-->  9/07/00 <!-- $-->
 */
public class TestCompiler extends FileCompiler {

    public static void main(String[] args) throws Exception {
        File dir = new File(".");
        TestCompiler tc = new TestCompiler(dir, null, dir);
        tc.addErrorListener(new ConsoleErrorReporter(System.out));
        tc.setForceCompile(true);
        String[] names = tc.compile(args[0]);
        System.out.println("Compiled " + names.length + " sources");
        for (int i = 0; i < names.length; i++) {
            System.out.println(names[i]);
        }
        int errorCount = tc.getErrorCount();
        if (errorCount > 0) {
            String msg = String.valueOf(errorCount) + " error";
            if (errorCount != 1) {
                msg += 's';
            }
            System.out.println(msg);
            return;
        }
        TemplateLoader loader = new TemplateLoader();
        TemplateLoader.Template template = loader.getTemplate(args[0]);
        int length = args.length - 1;
        Object[] params = new Object[length];
        for (int i = 0; i < length; i++) {
            params[i] = args[i + 1];
        }
        System.out.println("Executing " + template);
        template.execute(new Context(System.out), params);
    }

    public TestCompiler(File rootSourceDir, String rootPackage, File rootDestDir) {
        super(rootSourceDir, rootPackage, rootDestDir, null);
    }

    public static class Context extends DefaultContext {

        private PrintStream mOut;

        public Context(PrintStream out) {
            super();
            mOut = out;
        }

        public void print(Object obj) {
            mOut.print(toString(obj));
        }
    }
}
