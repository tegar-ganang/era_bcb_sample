package test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import javax.xml.bind.Marshaller;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import dfdl.GenericDFDLTest;

/**
 *  Description of the Class
 *  Modelled after IncludeTest - but with a large number of elements and starting from a file.
 *
 *@author     d3h252
 *@created    May 2, 2005
 */
public class PerfTest {

    /**
   *  Constructor for the PerfTest object
   */
    private PerfTest() {
    }

    private static int maxOccurs = 20000;

    private static String datafile = new String("tempdata.dat");

    /**
   *  The main program for the PerfTest class
   *
   *@param  args  The command line arguments
   */
    public static void main(String[] args) {
        ByteBuffer b = null;
        try {
            writeData();
        } catch (IOException io) {
            System.err.println(io);
        }
        long start = System.currentTimeMillis();
        System.out.println("My TEST!!!!");
        try {
            b = addData();
            long end = System.currentTimeMillis();
            System.out.println("Duration1 = " + (end - start) / 1000.0 + " seconds");
        } catch (IOException io) {
            System.err.println(io);
        }
        try {
            GenericDFDLTest test = new GenericDFDLTest(b, "dfdl.perf:dfdl.newtype", null, "examples/files/results/perf.xml");
            long end = System.currentTimeMillis();
            try {
                System.setOut(new PrintStream(new FileOutputStream("perftest.log", true)));
            } catch (IOException io) {
                System.err.println(io);
            }
            System.out.println("\nDuration = " + (end - start) / 1000.0 + " seconds");
            System.out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   *  Write test data to test file
   *
   *@exception  IOException  Description of the Exception
   */
    private static void writeData() throws IOException {
        float f = new Float(3.2).floatValue();
        float f2 = new Float(32.98).floatValue();
        float f3 = new Float(4.65).floatValue();
        float f4 = new Float(7.8).floatValue();
        float f5 = new Float(1.23).floatValue();
        float f6 = new Float(8.345).floatValue();
        FileOutputStream fos = new FileOutputStream(datafile);
        DataOutputStream dos = new DataOutputStream(fos);
        for (int i = 0; i < maxOccurs; i++) {
            dos.writeFloat(f);
            dos.writeFloat(f2);
            dos.writeFloat(f3);
            dos.writeFloat(f4);
            dos.writeFloat(f5);
            dos.writeFloat(f6);
        }
        dos.close();
        long start = System.currentTimeMillis();
        PrintStream out = System.out;
        System.setOut(new PrintStream(new FileOutputStream("perfpre.log")));
        System.out.println("<?xml version='1.0' encoding='UTF-8'?>\r<DFDL xmlns=\"DFDL\">\r  <vector>\r");
        FileInputStream fis = new FileInputStream(datafile);
        DataInputStream dis = new DataInputStream(fis);
        for (int j = 0; j < 2.5 * maxOccurs; j++) {
            System.out.println("    <point>\r      <real>" + dis.readFloat() + "</real>");
            System.out.println("\r<imaginary>" + dis.readFloat() + "</imaginary>\r    </point>");
        }
        dis.close();
        System.out.println("</vector></DFDL>");
        long end = System.currentTimeMillis();
        System.out.println("Duration = " + (end - start) / 1000.0 + " seconds");
        System.setOut(out);
    }

    /**
   *  Adds a feature to the Data attribute of the PerfTest class
   *
   *@return                  Description of the Return Value
   *@exception  IOException  Description of the Exception
   */
    private static ByteBuffer addData() throws IOException {
        FileInputStream fis = new FileInputStream(datafile);
        FileChannel fc = fis.getChannel();
        ByteBuffer b = fc.map(FileChannel.MapMode.READ_ONLY, 0, maxOccurs * 6 * 4);
        return b;
    }
}
