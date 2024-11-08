package org.openorb.orb.examples.idl.reflection;

import java.io.File;
import java.io.FileNotFoundException;
import org.openorb.compiler.CompilerProperties;
import org.openorb.compiler.idl.reflect.idlAttribute;
import org.openorb.compiler.idl.reflect.idlOperation;
import org.openorb.compiler.object.IdlObject;
import org.openorb.compiler.parser.CompilationException;
import org.openorb.compiler.parser.IdlParser;

/**
 * This class used as an example for the IDL Reflection
 *
 * @author Olivier Modica
 */
public final class Reflection {

    private Reflection() {
    }

    /**
     * Display help to known how to use this compiler
     */
    public static void displayHelp() {
        System.out.println("");
        System.out.println("");
        System.out.println("##################################################");
        System.out.println("#                                                #");
        System.out.println("#          OpenORB IDL Reflection Example        #");
        System.out.println("#                                                #");
        System.out.println("##################################################");
        System.out.println("");
        System.out.println("");
        System.out.println("Usage");
        System.out.println("-----");
        System.out.println("");
        System.out.println("\torg.openorb.orb.examples.orb.idl.reflection.Reflection" + " idl_file_name");
        System.out.println("");
        System.out.println("Example");
        System.out.println("-------");
        System.out.println("\torg.openorb.orb.examples.orb.idl.reflection.Reflection" + " reflection.idl");
        System.exit(0);
    }

    /**
     * Generate the HTML
     */
    public static void generateHTML(java.util.Enumeration contents) {
        try {
            java.io.File htmlFile = new java.io.File("Reflection.html");
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileOutputStream(htmlFile));
            writer.println("<html>\n<head>\n<title>OpenORB IDL Reflection - Generated HTML " + "Page</title>\n</head>\n<body>");
            writer.println("<b>The following document was generated from an IDL file using " + "the IDL Reflection.</b><br>");
            addContents(contents, writer);
            writer.println("</body>\n</html>");
            writer.close();
        } catch (java.io.IOException ex) {
            System.out.println("[IDL Reflection Example] File error.");
            System.exit(0);
        }
    }

    public static void addContents(java.util.Enumeration contents, java.io.PrintWriter writer) {
        org.openorb.compiler.idl.reflect.idlObject obj;
        while (contents.hasMoreElements()) {
            obj = (org.openorb.compiler.idl.reflect.idlObject) contents.nextElement();
            switch(obj.idlType()) {
                case org.openorb.compiler.idl.reflect.idlType.ATTRIBUTE:
                    if (((idlAttribute) obj).isReadOnly()) {
                        writer.println("readonly attribute ");
                    } else {
                        writer.println("attribute ");
                    }
                    writer.println(obj.idlName() + ";<br><br>");
                    break;
                case org.openorb.compiler.idl.reflect.idlType.MODULE:
                    if (obj.idlName().equals("CORBA")) {
                        break;
                    }
                    writer.println("module ");
                    writer.println("<font color=blue>" + obj.idlName() + "</font>{<br><br>");
                    addContents(obj.content(), writer);
                    writer.println("<br>};<br><br>");
                    break;
                case org.openorb.compiler.idl.reflect.idlType.INTERFACE:
                    writer.println("interface ");
                    writer.println("<font color=red>" + obj.idlName() + "</font>{<br><br>");
                    addContents(obj.content(), writer);
                    writer.println("<br>};<br><br>");
                    break;
                case org.openorb.compiler.idl.reflect.idlType.OPERATION:
                    if (((idlOperation) obj).isOneway()) {
                        writer.println("oneway ");
                    }
                    writer.println("void <font color=green>" + obj.idlName() + "</font>();<br><br>");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            displayHelp();
        }
        System.out.println("[IDL Reflection Example] Generating HTML from IDL...");
        CompilerProperties cp = new CompilerProperties();
        IdlParser parser = new IdlParser(cp);
        IdlObject compilationGraph = null;
        for (int i = 0; i < args.length; i++) {
            File file = new File(args[i]);
            try {
                compilationGraph = parser.compile_idl(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                throw new CompilationException("File " + file + " does not exist");
            }
        }
        if (parser.getTotalErrors() != 0 || compilationGraph == null) {
            System.out.println("[IDL Reflection Example] There are errors in the IDL file.");
            System.exit(0);
        }
        generateHTML(compilationGraph.content());
        System.out.println("[IDL Reflection Example] HTML generated from IDL in " + "file ReflectionEx.html.");
    }
}
