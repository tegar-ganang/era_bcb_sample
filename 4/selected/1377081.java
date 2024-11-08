package uk.ac.dl.escience.junkdev.tests;

import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileOutputStream;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.commons.vfs.abstractclasses.AbstractTestClass;
import static org.junit.Assert.*;

/**
 * @deprecated this is not part of the test suite, rather just for dabbling around
 * and experimentation. 
 * @author David Meredith
 */
@Deprecated
public class JargonSrbTest extends AbstractTestClass {

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("one time setup");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("one time tead down");
    }

    @Before
    public void setUp() throws Exception {
        this.loadProperties();
        this.loadCertificate();
    }

    @After
    public void tearDown() throws Exception {
    }

    public void testCopyFrom() throws Exception {
        String srbGsiUri = "srb://" + srbGsiHost + ":" + srbGsiPort + "/ngs/home/david-meredith.ngs/test.txt";
        DefaultFileSystemManager fsManager = this.getFsManager();
        FileSystemOptions opts = this.createFileSystemOptions();
        FileObject to = fsManager.resolveFile(srbGsiUri, opts);
        assertTrue(to.exists());
        String localFile = "file:///tmp/from.txt";
        FileObject from = fsManager.resolveFile(localFile);
        assertTrue(from.exists());
        to.copyFrom(from, new AllFileSelector());
    }

    public void overwriteFileTest() throws Exception {
        File filefrom = new File("/tmp/from.txt");
        File fileto = new File("/tmp/to.txt");
        InputStream from = null;
        OutputStream to = null;
        try {
            from = new FileInputStream(filefrom);
            to = new FileOutputStream(fileto);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytes_read);
            }
        } finally {
            if (from != null) {
                from.close();
            }
            if (to != null) {
                to.close();
            }
        }
    }

    @Test
    public void listingTest() throws Exception {
        SRBAccount srbAccount = new SRBAccount("srb1.ngs.rl.ac.uk", 5544, this.cred);
        srbAccount.setDefaultStorageResource("ral-ngs1");
        SRBFileSystem client = new SRBFileSystem(srbAccount);
        client.setFirewallPorts(64000, 65000);
        String home = client.getHomeDirectory();
        System.out.println("home: " + home);
        SRBFile file = new SRBFile(client, "/ngs/home");
        GeneralFile[] kids = file.listFiles();
        if (kids != null) {
            for (int i = 0; i < kids.length; i++) {
                GeneralFile child = kids[i];
                String name = child.getName();
                boolean isDir = child.isDirectory();
                boolean isFile = child.isFile();
                System.out.println("name: " + name + " " + isDir + " " + isFile);
            }
        }
    }

    public void overwriteTest() throws Exception {
        SRBAccount srbAccount = new SRBAccount("srb1.ngs.rl.ac.uk", 5544, this.cred);
        srbAccount.setDefaultStorageResource("ral-ngs1");
        SRBFileSystem client = new SRBFileSystem(srbAccount);
        client.setFirewallPorts(64000, 65000);
        String home = client.getHomeDirectory();
        System.out.println("home: " + home);
        SRBFile file = new SRBFile(client, home + "/test.txt");
        assertTrue(file.exists());
        File filefrom = new File("/tmp/from.txt");
        assertTrue(filefrom.exists());
        SRBFileOutputStream to = null;
        InputStream from = null;
        try {
            to = new SRBFileOutputStream((SRBFile) file);
            from = new FileInputStream(filefrom);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytes_read);
            }
            to.flush();
        } finally {
            try {
                if (to != null) {
                    to.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (from != null) {
                    from.close();
                }
            } catch (Exception ex) {
            }
        }
    }
}
