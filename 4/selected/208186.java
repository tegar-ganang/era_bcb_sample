package mrstk;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author avani
 */
public class MrstkXmlFileWriterTest {

    public MrstkXmlFileWriterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of writeFile method, of class MrstkXmlFileWriter.
     */
    @Test
    public void testWrite() throws Exception {
        MrstkXmlFileReader reader = new MrstkXmlFileReader();
        reader.setFileName("..//data//MrstkXML//prototype3.xml");
        reader.read();
        SpectrumArray sp = reader.getOutput();
        File tmp = File.createTempFile("mrstktest", ".xml");
        System.out.println("Writing temp file: " + tmp.getAbsolutePath());
        MrstkXmlFileWriter writer = new MrstkXmlFileWriter(sp);
        writer.setFile(tmp);
        writer.write();
        MrstkXmlFileReader reader2 = new MrstkXmlFileReader();
        reader2.setFileName(writer.getFile().getAbsolutePath());
        reader2.read();
        SpectrumArray sp2 = reader2.getOutput();
        assertTrue(sp.equals(sp2));
    }
}
