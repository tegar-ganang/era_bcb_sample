package net.qrivy.jbioapi;

import es.uvigo.tsc.gts.biowebauth.lib.jbioapi.utils.IOUtil;
import es.uvigo.tsc.gts.biowebauth.lib.model.vo.FileVO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.qrivy.bioapi.BioAPI;
import net.qrivy.bioapi.BioAPI_BIR;
import net.qrivy.bioapi.SWIGTYPE_p_a_16__unsigned_char;
import net.qrivy.bioapi.SchemaArray;
import org.apache.log4j.Logger;

/**
 * @author Michael R. Crusoe <michael@qrivy.net>
 *
 */
public class BiometricsFramework implements Runnable {

    private static Logger logger = Logger.getLogger(BiometricsFramework.class.getName());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new BiometricsFramework()));
        String os = System.getProperty("os.name");
        logger.info("O.S.: " + os);
        System.out.println("O.S.: " + os);
        try {
            if (os.startsWith("Windows")) {
                InputStream in = BiometricsFramework.class.getResourceAsStream("/es/uvigo/tsc/gts/biowebauth/lib/jbioapi/resources/jbioapi.dll");
                File tempFile = File.createTempFile("jbioapi", ".dll");
                IOUtil.streamAndClose(in, new FileOutputStream(tempFile));
                tempFile.deleteOnExit();
                String fileName = tempFile.getAbsolutePath();
                logger.info("fileName:" + fileName);
                System.out.println("fileAName:" + fileName);
                System.load(fileName.substring(0, fileName.length()));
            } else if (os.equals("Linux")) {
                InputStream in = BiometricsFramework.class.getResourceAsStream("/es/uvigo/tsc/gts/biowebauth/lib/jbioapi/resources/libjbioapi.so");
                File tempFile = File.createTempFile("libjbioapi", ".so");
                IOUtil.streamAndClose(in, new FileOutputStream(tempFile));
                tempFile.deleteOnExit();
                String fileName = tempFile.getAbsolutePath();
                logger.info("fileName:" + fileName);
                System.out.println("fileName:" + fileName);
                System.load(fileName.substring(0, fileName.length()));
            }
            logger.info("Success loading embedded jbioapi");
            System.out.println("Success loading embedded jbioapi");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Could not load the BioAPI library");
            throw e;
        } catch (FileNotFoundException ex) {
            logger.warn("Embedded jbioapi library not found.");
            System.out.println("Embedded jbioapi library not found.");
            loadSystemJBioAPI();
        } catch (IOException ex) {
            logger.warn("IO exception loading embedded jbioapi library");
            System.out.println("IO exception loading embedded jbioapi library");
            loadSystemJBioAPI();
        }
    }

    private static void loadSystemJBioAPI() {
        logger.warn("Trying to load the system jbioapi");
        System.out.println("Trying to load the system jbioapi");
        try {
            System.loadLibrary("jbioapi");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Could not load the jbioapi library (" + System.mapLibraryName("jbioapi") + ") using the " + "java.library.path property. Is the BioAPI library " + "installed on the path? You can set this " + "property by adding \"-Djava.library.path=path\" " + "to the command line.");
            throw e;
        }
        logger.info("Success loading system jbioapi");
        System.out.println("Success loading system jbioapi");
    }

    private static boolean dirty = true;

    private static BiometricsFramework instance;

    private static Map bsps;

    public static BiometricServiceProvider getBiometricServiceProvider(String uuid, long deviceID) throws BioApiException {
        logger.info("getBiometricServiceProvider(" + uuid + "," + deviceID);
        setup();
        String hashInput = uuid + deviceID;
        Long hash = new Long((long) hashInput.hashCode());
        if (!bsps.containsKey(hash)) {
            SWIGTYPE_p_a_16__unsigned_char moduleUuid;
            long bspHandle;
            moduleUuid = BioAPI.getStructuredUuid(uuid);
            BioAPI.loadModule(moduleUuid);
            bspHandle = BioAPI.attachModule(moduleUuid, BioAPI.getBioAPIMemoryFuncs(), deviceID);
            bsps.put(hash, new BiometricServiceProvider(bspHandle, moduleUuid));
        }
        return (BiometricServiceProvider) bsps.get(hash);
    }

    public static long getNumberOfModules() throws BioApiException {
        setup();
        return BioAPI.getNumberOfModules();
    }

    public static SchemaArray getSchemas() throws BioApiException {
        return SchemaArray.frompointer(BioAPI.getSchemas());
    }

    public static BioAPI_BIR readBir(String filename) throws IOException {
        BioAPI_BIR bir;
        FileInputStream in = null;
        FileChannel channel = null;
        try {
            in = new FileInputStream(filename);
            channel = in.getChannel();
            long channelSize = channel.size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(16);
            channel.read(buffer);
            bir = new BioAPI_BIR();
            BioAPI.setBirHeader(bir, buffer);
            buffer = ByteBuffer.allocateDirect((int) (channelSize - channel.position()));
            channel.read(buffer);
            BioAPI.setBirData(bir, buffer);
            if (channel.position() != channelSize) {
                buffer = buffer.compact();
                BioAPI.setSignatureData(bir, buffer);
            }
            channel.close();
            channel = null;
            in.close();
            in = null;
        } catch (IOException e1) {
            if (channel != null) {
                try {
                    channel.close();
                    channel = null;
                } catch (IOException e2) {
                    channel = null;
                }
            }
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (IOException e2) {
                    in = null;
                }
            }
            throw e1;
        }
        return bir;
    }

    public static BioAPI_BIR readBir(FileVO filevo) throws Exception {
        BioAPI_BIR bir;
        ByteArrayInputStream bin = null;
        ReadableByteChannel channel = null;
        try {
            bin = new ByteArrayInputStream(filevo.getContent());
            channel = Channels.newChannel(bin);
            long channelSize = filevo.getContent().length;
            ByteBuffer buffer = ByteBuffer.allocateDirect(16);
            channel.read(buffer);
            bir = new BioAPI_BIR();
            BioAPI.setBirHeader(bir, buffer);
            buffer = ByteBuffer.allocateDirect((int) (channelSize - 16));
            channel.read(buffer);
            BioAPI.setBirData(bir, buffer);
            channel.close();
            channel = null;
            bin.close();
            bin = null;
        } catch (Exception e1) {
            if (channel != null) {
                try {
                    channel.close();
                    channel = null;
                } catch (IOException e2) {
                    channel = null;
                }
            }
            if (bin != null) {
                try {
                    bin.close();
                    bin = null;
                } catch (Exception e2) {
                    bin = null;
                }
            }
            throw e1;
        }
        return bir;
    }

    public static FileVO birToFileVO(BioAPI_BIR bir, String filename) throws IOException {
        ByteArrayOutputStream bout = null;
        WritableByteChannel channel = null;
        FileVO fvo = new FileVO();
        try {
            bout = new ByteArrayOutputStream();
            channel = Channels.newChannel(bout);
            channel.write(BioAPI.getBirHeaderByteBuffer(bir));
            channel.write(BioAPI.getBirDataByteBuffer(bir));
            channel.write(BioAPI.getBirSignatureByteBuffer(bir));
            channel.close();
            channel = null;
            bout.close();
            fvo.setContent(bout.toByteArray());
            fvo.setName(filename);
            bout = null;
        } catch (IOException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e1) {
                }
                channel = null;
            }
            if (bout != null) {
                try {
                    bout.close();
                } catch (IOException e1) {
                }
                bout = null;
            }
            IOException b = new IOException("Error writing BIR to " + filename);
            b.initCause(e);
            throw b;
        }
        if (fvo == null) throw new IOException("Error Creatina the FileVO from the BIR to " + filename);
        return fvo;
    }

    public static void writeBir(BioAPI_BIR bir, String filename) throws IOException {
        FileOutputStream out = null;
        FileChannel channel = null;
        try {
            out = new FileOutputStream(filename);
            channel = out.getChannel();
            channel.write(BioAPI.getBirHeaderByteBuffer(bir));
            channel.write(BioAPI.getBirDataByteBuffer(bir));
            channel.write(BioAPI.getBirSignatureByteBuffer(bir));
            channel.close();
            channel = null;
            out.close();
            out = null;
        } catch (IOException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e1) {
                }
                channel = null;
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                }
                out = null;
            }
            IOException b = new IOException("Error writing BIR to " + filename);
            b.initCause(e);
            throw b;
        }
    }

    public static void showInfo(BioAPI_BIR inBIR) throws BioApiException {
        System.out.println("HeaderBIR  purpose: (" + inBIR.getHeader().getPurpose() + ")");
        System.out.println("HeaderBIR  type: (" + inBIR.getHeader().getType() + ")");
        System.out.println("HeaderBIR  formatID: (" + inBIR.getHeader().getFormat().getFormatID() + ")");
        System.out.println("HeaderBIR  formatOwner: (" + inBIR.getHeader().getFormat().getFormatOwner() + ")");
        System.out.println("HeaderBIR  headerVersion: (" + inBIR.getHeader().getHeaderVersion() + ")");
        System.out.println("HeaderBIR  quality: (" + inBIR.getHeader().getQuality() + ")");
        System.out.println("HeaderBIR  length: (" + inBIR.getHeader().getLength() + ")");
        System.out.println("HeaderBIR  factorsMask: (" + inBIR.getHeader().getFactorsMask() + ")");
    }

    private static void init() throws BioApiException {
        BioAPI.init();
        dirty = false;
    }

    public static void terminate() {
        Iterator bspIterator = bsps.keySet().iterator();
        while (bspIterator.hasNext()) {
            ((BiometricServiceProvider) (bsps.get((Long) bspIterator.next()))).destroy();
            bspIterator.remove();
        }
        if (!dirty) {
            try {
                BioAPI.terminate();
            } catch (BioApiException e) {
            }
            dirty = true;
        }
    }

    private static void setup() throws BioApiException {
        if (bsps == null) {
            bsps = new HashMap();
        }
        if (dirty) {
            init();
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        terminate();
    }

    public void run() {
        BiometricsFramework.terminate();
    }
}
