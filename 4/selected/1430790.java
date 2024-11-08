package no.eirikb.sfs.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.eirikb.sfs.share.Share;
import no.eirikb.sfs.share.ShareFileReader;
import no.eirikb.sfs.share.ShareFileWriter;
import no.eirikb.sfs.share.ShareFolder;
import no.eirikb.sfs.share.ShareUtility;
import no.eirikb.utils.file.MD5File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author eirikb
 */
public class SocketTest {

    private final String SHAREPATH = "/home/eirikb/test";

    private final String SHARENAME = "TestShare";

    private final int PARTS = 50;

    private static String initHash;

    private int done;

    private ServerSocket listener;

    private Socket[] servers;

    public SocketTest() {
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
     * *********************************
     * TEST!
     * *********************************
     */
    @Test
    public void initHashTest() {
        System.out.println("Creating hash...");
        final File[] files = { new File(SHAREPATH) };
        initHash = MD5File.MD5Directory(files[0]);
        System.out.println("Hash: " + initHash);
        assertNotNull(initHash);
    }

    @Test
    public void serverTest() throws Exception {
        System.out.println("servertest");
        final File[] files = { new File(SHAREPATH) };
        Share share = ShareUtility.createShare(files, SHARENAME);
        System.out.println("Creating shares...");
        final ShareFolder[] shareFolders = ShareUtility.cropShareToParts(share, PARTS);
        new Thread() {

            @Override
            public void run() {
                try {
                    System.out.println("Creating server...");
                    listener = new ServerSocket(40000);
                    servers = new Socket[PARTS];
                    System.out.println("Accept clients...");
                    for (int i = 0; i < PARTS; i++) {
                        servers[i] = listener.accept();
                    }
                    for (int j = 0; j < PARTS; j++) {
                        final int i = j;
                        new Thread() {

                            @Override
                            public void run() {
                                try {
                                    ShareFileWriter writer = new ShareFileWriter(shareFolders[i], new File("Downloads/" + shareFolders[i].getName()));
                                    byte[] b = new byte[servers[i].getReceiveBufferSize()];
                                    int read;
                                    int tot = 0;
                                    InputStream in = servers[i].getInputStream();
                                    while (tot < shareFolders[i].getSize()) {
                                        read = in.read(b);
                                        writer.write(b, read);
                                        tot += read;
                                    }
                                    done++;
                                    if (done == PARTS) {
                                        System.out.println("SERVER DONE!");
                                    }
                                    servers[i].close();
                                } catch (IOException ex) {
                                    Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                                } finally {
                                    try {
                                        if (servers[i] != null) {
                                            servers[i].close();
                                        }
                                    } catch (IOException ex) {
                                        Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        }.start();
                    }
                    listener.close();
                } catch (Exception ex) {
                    Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    @Test
    public void clientTest() throws Exception {
        System.out.println("clienttest");
        final File[] files = { new File(SHAREPATH) };
        Share share = ShareUtility.createShare(files, SHARENAME);
        System.out.println("Creating shares...");
        final ShareFolder[] shareFolders = ShareUtility.cropShareToParts(share, PARTS);
        System.out.println("Creating clients...");
        done = 0;
        Thread.sleep(5000);
        for (int j = 0; j < PARTS; j++) {
            final int i = j;
            new Thread() {

                public void run() {
                    Socket client = null;
                    try {
                        client = new Socket("localhost", 40000);
                        ShareFileReader reader = new ShareFileReader(shareFolders[i], files[0]);
                        long tot = 0;
                        byte[] b = new byte[client.getSendBufferSize()];
                        OutputStream out = client.getOutputStream();
                        while (tot < shareFolders[i].getSize()) {
                            try {
                                reader.read(b);
                                out.write(b);
                                out.flush();
                                tot += b.length;
                            } catch (IOException ex) {
                                Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        done++;
                        System.out.println((int) (done * 100.0 / PARTS) + "% Complete");
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (IOException ex) {
                                Logger.getLogger(SocketTest.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }.start();
        }
        while (done < PARTS) {
            Thread.yield();
        }
        Thread.sleep(1000);
        System.out.println("CLIENT DONE");
    }

    @Test
    public void resultHashTest() {
        System.out.println("Creating hash of written share...");
        File resultFile = new File("Downloads/" + SHARENAME);
        String resultHash = MD5File.MD5Directory(resultFile);
        System.out.println("Init hash:   " + initHash);
        System.out.println("Result hash: " + resultHash);
        assertEquals(initHash, resultHash);
    }
}
