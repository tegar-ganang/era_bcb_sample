package barde.t4c;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import barde.log.Message;
import junit.framework.TestCase;

/** * @author cbonar */
public class T4CClientWriterTest extends TestCase {

    String testFileName;

    File tmpDir;

    ResourceBundle rc;

    protected void setUp() throws Exception {
        this.tmpDir = new File(System.getProperty("java.io.tmpdir", "."));
        Properties props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("tests.properties"));
        this.testFileName = (String) props.get("logfile.t4c.client.fr.txt");
        this.rc = ResourceBundle.getBundle("barde_t4c", new Locale("fr"));
    }

    public final void testT4CClientWriter() throws Exception {
        InputStream is = ClassLoader.getSystemResourceAsStream(this.testFileName);
        T4CClientReader reader = new T4CClientReader(is, rc);
        File tmpFile = File.createTempFile("barde", ".log", this.tmpDir);
        System.out.println("tmp=" + tmpFile.getAbsolutePath());
        T4CClientWriter writer = new T4CClientWriter(new FileOutputStream(tmpFile), rc);
        for (Message m = reader.read(); m != null; m = reader.read()) writer.write(m);
        writer.close();
        InputStream fa = ClassLoader.getSystemResourceAsStream(this.testFileName);
        FileInputStream fb = new FileInputStream(tmpFile);
        for (int ba = fa.read(); ba != -1; ba = fa.read()) assertEquals(ba, fb.read());
    }
}
