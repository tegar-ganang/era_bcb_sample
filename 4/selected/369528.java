package org.basegen.plugins.basegen.communication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class adjust Stub file created by Axis, adding support to complexTypes.
 */
public class AxisDeployAdjust extends AxisAdjustAbstract {

    /**
     * Constant start of type mapping
     */
    private static final String START_OF_TYPE_MAPPING = "      <typeMapping";

    /**
     * find start of typeMapping tag
     * 
     * @param reader reader
     * @param writer writer
     * @throws IOException io exception
     */
    private void findStartOfTypeMapping(BufferedReader reader, PrintWriter writer) throws IOException {
        String line = null;
        do {
            line = reader.readLine();
            if (!line.equals(START_OF_TYPE_MAPPING)) {
                writer.println(line);
            }
        } while (!line.equals(START_OF_TYPE_MAPPING));
    }

    /**
     * write start of typeMapping tag
     * 
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeStartOfTypeMapping(PrintWriter writer) throws IOException {
        writer.println(START_OF_TYPE_MAPPING);
    }

    /**
     * write custom typeMapping
     * 
     * @param writer writer
     * @throws IOException io exception
     */
    private void writeCustomTypeMapping(PrintWriter writer) throws IOException {
        java.io.InputStream input = this.getClass().getClassLoader().getResourceAsStream("communication/customDeploy.xml.txt");
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
        findStartOfTypeMapping(reader, writer);
        writeCustomTypeMapping(writer);
        writeStartOfTypeMapping(writer);
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
        AxisDeployAdjust axisSkeletonAdjust = new AxisDeployAdjust();
        axisSkeletonAdjust.adjust(file);
        System.out.print(file + " adjusted!");
    }
}
