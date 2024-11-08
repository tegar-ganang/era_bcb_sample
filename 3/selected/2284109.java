package sun.security.provider;

import java.security.*;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.net.*;
import sun.security.util.Debug;

abstract class SeedGenerator {

    private static SeedGenerator instance;

    private static final Debug debug = Debug.getInstance("provider");

    private static final String PROP_EGD = "java.security.egd";

    private static final String PROP_RNDSOURCE = "securerandom.source";

    static final String URL_DEV_RANDOM = "file:/dev/random";

    static {
        String egdSource = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                String egdSource = System.getProperty(PROP_EGD, "");
                if (egdSource.length() != 0) {
                    return egdSource;
                }
                egdSource = Security.getProperty(PROP_RNDSOURCE);
                if (egdSource == null) {
                    return "";
                }
                return egdSource;
            }
        });
        if (egdSource.equals(URL_DEV_RANDOM)) {
            try {
                instance = new NativeSeedGenerator();
                if (debug != null) {
                    debug.println("Using operating system seed generator");
                }
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("Failed to use operating system seed " + "generator: " + e.toString());
                }
            }
        } else if (egdSource.length() != 0) {
            try {
                instance = new URLSeedGenerator(egdSource);
                if (debug != null) {
                    debug.println("Using URL seed generator reading from " + egdSource);
                }
            } catch (IOException e) {
                if (debug != null) debug.println("Failed to create seed generator with " + egdSource + ": " + e.toString());
            }
        }
        if (instance == null) {
            if (debug != null) {
                debug.println("Using default threaded seed generator");
            }
            instance = new ThreadedSeedGenerator();
        }
    }

    /**
     * Fill result with bytes from the queue. Wait for it if it isn't ready.
     */
    public static void generateSeed(byte[] result) {
        instance.getSeedBytes(result);
    }

    void getSeedBytes(byte[] result) {
        for (int i = 0; i < result.length; i++) {
            result[i] = getSeedByte();
        }
    }

    abstract byte getSeedByte();

    /**
     * Retrieve some system information, hashed.
     */
    static byte[] getSystemEntropy() {
        byte[] ba;
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("internal error: SHA-1 not available.");
        }
        byte b = (byte) System.currentTimeMillis();
        md.update(b);
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

            public Object run() {
                try {
                    String s;
                    Properties p = System.getProperties();
                    Enumeration e = p.propertyNames();
                    while (e.hasMoreElements()) {
                        s = (String) e.nextElement();
                        md.update(s.getBytes());
                        md.update(p.getProperty(s).getBytes());
                    }
                    md.update(InetAddress.getLocalHost().toString().getBytes());
                    File f = new File(p.getProperty("java.io.tmpdir"));
                    String[] sa = f.list();
                    for (int i = 0; i < sa.length; i++) md.update(sa[i].getBytes());
                } catch (Exception ex) {
                    md.update((byte) ex.hashCode());
                }
                Runtime rt = Runtime.getRuntime();
                byte[] memBytes = longToByteArray(rt.totalMemory());
                md.update(memBytes, 0, memBytes.length);
                memBytes = longToByteArray(rt.freeMemory());
                md.update(memBytes, 0, memBytes.length);
                return null;
            }
        });
        return md.digest();
    }

    /**
     * Helper function to convert a long into a byte array (least significant
     * byte first).
     */
    private static byte[] longToByteArray(long l) {
        byte[] retVal = new byte[8];
        for (int i = 0; i < 8; i++) {
            retVal[i] = (byte) l;
            l >>= 8;
        }
        return retVal;
    }

    private static class ThreadedSeedGenerator extends SeedGenerator implements Runnable {

        private byte[] pool;

        private int start, end, count;

        ThreadGroup seedGroup;

        /**
         * The constructor is only called once to construct the one
         * instance we actually use. It instantiates the message digest
         * and starts the thread going.
         */
        ThreadedSeedGenerator() {
            pool = new byte[20];
            start = end = 0;
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                throw new InternalError("internal error: SHA-1 not available.");
            }
            final ThreadGroup[] finalsg = new ThreadGroup[1];
            Thread t = (Thread) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                public Object run() {
                    ThreadGroup parent, group = Thread.currentThread().getThreadGroup();
                    while ((parent = group.getParent()) != null) group = parent;
                    finalsg[0] = new ThreadGroup(group, "SeedGenerator ThreadGroup");
                    Thread newT = new Thread(finalsg[0], ThreadedSeedGenerator.this, "SeedGenerator Thread");
                    newT.setPriority(Thread.MIN_PRIORITY);
                    newT.setDaemon(true);
                    return newT;
                }
            });
            seedGroup = finalsg[0];
            t.start();
        }

        /**
	 * This method does the actual work. It collects random bytes and
	 * pushes them into the queue.
	 */
        public final void run() {
            try {
                while (true) {
                    synchronized (this) {
                        while (count >= pool.length) wait();
                    }
                    int counter, quanta;
                    byte v = 0;
                    for (counter = quanta = 0; (counter < 64000) && (quanta < 6); quanta++) {
                        try {
                            BogusThread bt = new BogusThread();
                            Thread t = new Thread(seedGroup, bt, "SeedGenerator Thread");
                            t.start();
                        } catch (Exception e) {
                            throw new InternalError("internal error: " + "SeedGenerator thread creation error.");
                        }
                        int latch = 0;
                        latch = 0;
                        long l = System.currentTimeMillis() + 250;
                        while (System.currentTimeMillis() < l) {
                            synchronized (this) {
                            }
                            ;
                            latch++;
                        }
                        v ^= rndTab[latch % 255];
                        counter += latch;
                    }
                    synchronized (this) {
                        pool[end] = v;
                        end++;
                        count++;
                        if (end >= pool.length) end = 0;
                        notifyAll();
                    }
                }
            } catch (Exception e) {
                throw new InternalError("internal error: " + "SeedGenerator thread generated an exception.");
            }
        }

        byte getSeedByte() {
            byte b = 0;
            try {
                synchronized (this) {
                    while (count <= 0) wait();
                }
            } catch (Exception e) {
                if (count <= 0) throw new InternalError("internal error: " + "SeedGenerator thread generated an exception.");
            }
            synchronized (this) {
                b = pool[start];
                pool[start] = 0;
                start++;
                count--;
                if (start == pool.length) start = 0;
                notifyAll();
            }
            return b;
        }

        private static byte[] rndTab = { 56, 30, -107, -6, -86, 25, -83, 75, -12, -64, 5, -128, 78, 21, 16, 32, 70, -81, 37, -51, -43, -46, -108, 87, 29, 17, -55, 22, -11, -111, -115, 84, -100, 108, -45, -15, -98, 72, -33, -28, 31, -52, -37, -117, -97, -27, 93, -123, 47, 126, -80, -62, -93, -79, 61, -96, -65, -5, -47, -119, 14, 89, 81, -118, -88, 20, 67, -126, -113, 60, -102, 55, 110, 28, 85, 121, 122, -58, 2, 45, 43, 24, -9, 103, -13, 102, -68, -54, -101, -104, 19, 13, -39, -26, -103, 62, 77, 51, 44, 111, 73, 18, -127, -82, 4, -30, 11, -99, -74, 40, -89, 42, -76, -77, -94, -35, -69, 35, 120, 76, 33, -73, -7, 82, -25, -10, 88, 125, -112, 58, 83, 95, 6, 10, 98, -34, 80, 15, -91, 86, -19, 52, -17, 117, 49, -63, 118, -90, 36, -116, -40, -71, 97, -53, -109, -85, 109, -16, -3, 104, -95, 68, 54, 34, 26, 114, -1, 106, -121, 3, 66, 0, 100, -84, 57, 107, 119, -42, 112, -61, 1, 48, 38, 12, -56, -57, 39, -106, -72, 41, 7, 71, -29, -59, -8, -38, 79, -31, 124, -124, 8, 91, 116, 99, -4, 9, -36, -78, 63, -49, -67, -87, 59, 101, -32, 92, 94, 53, -41, 115, -66, -70, -122, 50, -50, -22, -20, -18, -21, 23, -2, -48, 96, 65, -105, 123, -14, -110, 69, -24, -120, -75, 74, 127, -60, 113, 90, -114, 105, 46, 27, -125, -23, -44, 64 };

        /**
	 * This inner thread causes the thread scheduler to become 'noisy',
	 * thus adding entropy to the system load.
	 * At least one instance of this class is generated for every seed byte.
	 */
        private class BogusThread implements Runnable {

            public final void run() {
                try {
                    for (int i = 0; i < 5; i++) Thread.sleep(50);
                } catch (Exception e) {
                }
            }
        }
    }

    static class URLSeedGenerator extends SeedGenerator {

        private String deviceName;

        private BufferedInputStream devRandom;

        /**
	 * The constructor is only called once to construct the one
	 * instance we actually use. It opens the entropy gathering device
	 * which will supply the randomness.
	 */
        URLSeedGenerator(String egdurl) throws IOException {
            if (egdurl == null) {
                throw new IOException("No random source specified");
            }
            deviceName = egdurl;
            init();
        }

        URLSeedGenerator() throws IOException {
            this(SeedGenerator.URL_DEV_RANDOM);
        }

        private void init() throws IOException {
            final URL device = new URL(deviceName);
            devRandom = (BufferedInputStream) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                public Object run() {
                    try {
                        return new BufferedInputStream(device.openStream());
                    } catch (IOException ioe) {
                        return null;
                    }
                }
            });
            if (devRandom == null) {
                throw new IOException("failed to open " + device);
            }
        }

        byte getSeedByte() {
            byte b[] = new byte[1];
            int stat;
            try {
                stat = devRandom.read(b, 0, b.length);
            } catch (IOException ioe) {
                throw new InternalError("URLSeedGenerator " + deviceName + " generated exception: " + ioe.getMessage());
            }
            if (stat == b.length) {
                return b[0];
            } else if (stat == -1) {
                throw new InternalError("URLSeedGenerator " + deviceName + " reached end of file");
            } else {
                throw new InternalError("URLSeedGenerator " + deviceName + " failed read");
            }
        }
    }
}
