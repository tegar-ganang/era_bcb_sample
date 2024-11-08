package org.basegen.plugins.basegen.communication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class adjust Stub file created by Axis, adding support to complexTypes.
 */
public class AxisStubAdjust extends AxisAdjustAbstract {

    /**
     * Constant end of method
     */
    private static final String END_OF_METHOD = "    }";

    /**
     * Constant set type mapping version
     */
    private static final String SET_TYPE_MAPPING_VERSION = "setTypeMappingVersion";

    /**
     * find setTypeMappingVersion method call
     * 
     * @param reader reader
     * @param writer writer
     * @throws IOException io exception
     */
    private void findSetTypeMappingVersion(BufferedReader reader, PrintWriter writer) throws IOException {
        String line = null;
        do {
            line = reader.readLine();
            writer.println(line);
        } while (line.indexOf(SET_TYPE_MAPPING_VERSION) == -1);
    }

    /**
     * write addBindings00 method call
     * 
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeAddBindings00MethodCall(PrintWriter writer) throws IOException {
        writer.println("        addBindings00();");
    }

    /**
     * write rest of constructor method
     * 
     * @param reader reader
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeRestOfConstructorMethod(BufferedReader reader, PrintWriter writer) throws IOException {
        String line = null;
        do {
            line = reader.readLine();
            if (!line.equals(END_OF_METHOD)) {
                writer.println(line);
            }
        } while (!line.equals(END_OF_METHOD));
        writer.println(line);
    }

    /**
     * write addBindings00 method
     * 
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeAddBindings00Method(PrintWriter writer) throws IOException {
        java.io.InputStream input = this.getClass().getClassLoader().getResourceAsStream("communication/customFacadeSoapBindingStub.java.txt");
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(input));
        writeTail(reader, writer);
        reader.close();
    }

    /**
     * Adjust file
     * @param file file
     * @throws IOException io exception
     */
    public void adjust(File file) throws IOException {
        BufferedReader reader = getBufferedReader(file);
        PrintWriter writer = getPrintWriter(file);
        findSetTypeMappingVersion(reader, writer);
        writeAddBindings00MethodCall(writer);
        writeRestOfConstructorMethod(reader, writer);
        writeAddBindings00Method(writer);
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
        AxisStubAdjust axisSkeletonAdjust = new AxisStubAdjust();
        axisSkeletonAdjust.adjust(file);
        System.out.print(file + " adjusted!");
    }
}
