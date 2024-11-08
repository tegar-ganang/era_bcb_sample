package gov.nasa.jpf.ts;

import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.Invocation;
import gov.nasa.jpf.util.Trace;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * a test candidate for races
 */
public class RaceCandidate {

    Invocation testCall;

    Trace trace;

    FieldInfo fi;

    Instruction readInsn;

    Instruction writeInsn;

    RaceCandidate(Invocation testCall, Trace trace, FieldInfo fi, Instruction readInsn, Instruction writeInsn) {
        this.testCall = testCall;
        this.fi = fi;
        this.readInsn = readInsn;
        this.writeInsn = writeInsn;
        this.trace = trace.clone();
    }

    String getJNIMethodName() {
        return testCall.getMethodInfo().getJNIName();
    }

    String getMethodName() {
        return testCall.getMethodInfo().getName();
    }

    MethodInfo getMethodInfo() {
        return testCall.getMethodInfo();
    }

    FieldInfo getFieldInfo() {
        return fi;
    }

    Instruction getInstruction1() {
        return readInsn;
    }

    Instruction getInstruction2() {
        return writeInsn;
    }

    void printOn(PrintWriter pw) {
        pw.println("  method: " + testCall.getMethodInfo().getUniqueName());
        pw.println("  field:  " + fi.getName());
        pw.println("  read:   " + readInsn.getSourceLocation());
        pw.println("  write:  " + writeInsn.getSourceLocation());
    }

    String[] printArgDeclsOn(PrintWriter pw) {
        Object[] argVals = testCall.getExplicitArguments();
        String[] argTypes = testCall.getArgumentTypeNames();
        String[] argVars = new String[argVals.length];
        for (int i = 0; i < argVals.length; i++) {
            argVars[i] = "p" + i;
            pw.print("    final " + argTypes[i]);
            pw.print(" ");
            pw.print(argVars[i]);
            pw.print(" = ");
            String lit = testCall.getArgumentValueLiteral(argVals[i]);
            if (lit != null) {
                pw.print(lit);
            } else {
                pw.print(" new" + argTypes[i] + "(); // CHECK!");
            }
            pw.println(";");
        }
        return argVars;
    }

    void printArgListOn(PrintWriter pw, String[] vars) {
        for (int i = 0; i < vars.length; i++) {
            if (i > 0) {
                pw.print(',');
            }
            pw.print(vars[i]);
        }
    }

    void printTestMethodOn(PrintWriter pw) {
        MethodInfo mi = testCall.getMethodInfo();
        String clsName = mi.getClassInfo().getSimpleName();
        String mangledMthName = "test_" + mi.getJNIName();
        String mthName = mi.getName();
        pw.println("  public void " + mangledMthName + "() {");
        pw.println("    final " + clsName + " o = new " + clsName + "();");
        String[] argVars = printArgDeclsOn(pw);
        pw.println();
        pw.println("    Runnable r = new Runnable() {");
        pw.println("      public void run () {");
        pw.print("        o." + mthName + "(");
        printArgListOn(pw, argVars);
        pw.println(");");
        pw.println("      }");
        pw.println("    };");
        pw.println();
        pw.println("    Thread t = new Thread(r);");
        pw.println("    t.start();");
        pw.print("    o." + mthName + "(");
        printArgListOn(pw, argVars);
        pw.println(");");
        pw.println("  }");
    }
}
