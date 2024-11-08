package test.com.ek.mitapp.excel;

import java.io.*;
import junit.framework.TestCase;
import jxl.read.biff.BiffException;
import jxl.write.*;
import com.ek.mitapp.excel.DefaultWriter;

/**
 * TODO: Class description.
 * <br>
 * Id: $Id: ExcelWriterTest.java 1469 2006-03-22 19:27:26Z dhirwinjr $
 *
 * @author dhirwinjr
 */
public class ExcelWriterTest extends TestCase {

    private static final String inputFilename = "Mitigation Prioritization.xls";

    private static final String outputFilename = "output.xls";

    /**
	 * Main application start-point.
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(ExcelWriterTest.class);
    }

    /**
	 * Constructor for ExcelWriterTest.
	 * 
	 * @param name
	 */
    public ExcelWriterTest(String name) {
        super(name);
    }

    /**
	 * @see TestCase#setUp()
	 */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
	 * @see TestCase#tearDown()
	 */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
	 * Test method for 'com.ek.mitapp.excel.SynchroWriter.open()'
	 */
    public final void testRead() {
        DefaultWriter writer = new DefaultWriter(inputFilename, inputFilename);
        try {
            writer.read();
        } catch (IOException ioe) {
            System.out.println("Error: " + ioe.getMessage());
        } catch (BiffException be) {
            System.out.println("Error: " + be.getMessage());
        }
    }
}
