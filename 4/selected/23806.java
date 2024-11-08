package org.basegen.plugins.basegen.communication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class adjust Skeleton file created by Axis, breaking static method. The original file doesn�t compile.
 */
public class AxisSkeletonAdjust extends AxisAdjustAbstract {

    /**
     * Constant max methods
     */
    private static final int MAX_METHODS = 20;

    /**
     * Constant start of method
     */
    private static final String START_OF_METHOD = "        _params = " + "new org.apache.axis.description.ParameterDesc [] {";

    /**
     * Constant end of method
     */
    private static final String END_OF_METHOD = "    }";

    /**
     * Writer file header. static method line is not written.
     * 
     * @param reader reader
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeHeader(BufferedReader reader, PrintWriter writer) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.indexOf("static {") != -1) {
                break;
            }
            writer.println(line);
        }
    }

    /**
     * Get method variables
     * 
     * @param reader reader
     * @return array os strings
     * @throws IOException io exception
     */
    private String[] getMethodVariables(BufferedReader reader) throws IOException {
        return new String[] { reader.readLine(), reader.readLine(), reader.readLine() };
    }

    /**
     * write internal static method
     * 
     * @param reader reader
     * @param writer writer
     * @param variables variables
     * @param methodNumber method number
     * @return the last line
     * @throws IOException io exception
     */
    private String writeInternalStatic(BufferedReader reader, PrintWriter writer, String[] variables, int methodNumber) throws IOException {
        writer.println("    // internal method created by adjust");
        writer.println("    private static void internalStatic" + methodNumber + "()");
        writer.println("    {");
        for (int counter = 0; counter < variables.length; counter++) {
            writer.println(variables[counter]);
        }
        writer.println(START_OF_METHOD);
        String line = null;
        int methodCounter = 0;
        while ((line = reader.readLine()) != null) {
            if (line.equals(START_OF_METHOD)) {
                methodCounter++;
                if (methodCounter == MAX_METHODS) {
                    break;
                }
            }
            if (line.equals(END_OF_METHOD)) {
                break;
            }
            writer.println(line);
        }
        writer.println("    }");
        writer.println();
        return line;
    }

    /**
     * write submethod from static method
     * 
     * @param reader reader
     * @param writer writer
     * @param variables variables
     * @return the amount of submethod written
     * @throws IOException io exception
     */
    private int writeInternalStaticMethods(BufferedReader reader, PrintWriter writer, String[] variables) throws IOException {
        int result = 0;
        reader.readLine();
        String line;
        do {
            line = writeInternalStatic(reader, writer, variables, result);
            result++;
        } while (!line.equals(END_OF_METHOD));
        return result;
    }

    /**
     * Write new static method, calling submethods
     * 
     * @param writer writer
     * @param amount amount
     * @throws IOException io exception
     */
    private void writeStaticMethod(PrintWriter writer, int amount) throws IOException {
        writer.println("    static {");
        for (int counter = 0; counter < amount; counter++) {
            writer.println("        internalStatic" + counter + "();");
        }
        writer.println("    }");
    }

    /**
     * Adjust file
     * @param file file
     * @throws IOException io exception
     */
    public void adjust(File file) throws IOException {
        BufferedReader reader = getBufferedReader(file);
        PrintWriter writer = getPrintWriter(file);
        writeHeader(reader, writer);
        String[] variables = getMethodVariables(reader);
        int amount = writeInternalStaticMethods(reader, writer, variables);
        writeStaticMethod(writer, amount);
        writeTail(reader, writer);
        writer.close();
        reader.close();
        changeFiles(file);
    }

    /**
     * Main class.
     * 
     * @param args the file to be adjusted
     * @throws IOException io exception
     */
    public static void main(String args[]) throws IOException {
        File file = new File(args[0]);
        AxisSkeletonAdjust axisSkeletonAdjust = new AxisSkeletonAdjust();
        axisSkeletonAdjust.adjust(file);
        System.out.print(file + " adjusted!");
    }
}
