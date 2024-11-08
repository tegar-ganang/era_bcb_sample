package org.ozoneDB.test.blob;

import org.ozoneDB.OzoneInterface;
import org.ozoneDB.RemoteDatabase;
import org.ozoneDB.blob.*;
import java.util.Random;
import java.util.zip.*;

class BLOBTest extends Thread {

    static final int POOLSIZE = 10;

    static final int BUFFERSIZE = 32000;

    static final int PAGESIZE = 32;

    static final int MINSIZE = 1;

    static final int MAXSIZE = 1024;

    int pool;

    int bsize;

    int page;

    int min;

    int max;

    String[] names;

    int[] sizes;

    byte[] buffer;

    long[] check;

    RemoteDatabase db;

    Random objNr;

    Random objSize;

    boolean halted = false;

    long movement = 0;

    long avWritespeed = 0;

    int writecount = 0;

    long avReadspeed = 0;

    int readcount = 0;

    /** Constructor */
    public BLOBTest(RemoteDatabase _db, int _poolsize, int _buffersize, int _pagesize, int _rangeMin, int _rangeMax) {
        pool = _poolsize;
        bsize = _buffersize;
        page = _pagesize;
        min = _rangeMin;
        max = _rangeMax;
        names = new String[pool];
        sizes = new int[pool];
        check = new long[pool];
        buffer = new byte[_buffersize];
        db = _db;
        objNr = new Random();
        objSize = new Random();
    }

    /**  RUN THE THREAD */
    public void run() {
        int size;
        while (true) {
            int actObj = objNr.nextInt() % pool;
            actObj = actObj < 0 ? -actObj : actObj;
            try {
                if (names[actObj] == null) {
                    if (halted) {
                        break;
                    }
                    createBlob(actObj);
                } else {
                    BLOBContainer blob = (BLOBContainer) db.objectForName(names[actObj]);
                    if (blob != null) {
                        if (halted) {
                            break;
                        }
                        verifyBlob(blob, actObj);
                        db.deleteObject(blob);
                        names[actObj] = null;
                        sizes[actObj] = 0;
                        check[actObj] = 0;
                        System.out.println(" -> deleted.");
                        if (halted) {
                            break;
                        }
                        createBlob(actObj);
                    } else {
                        System.out.println("\nERROR!!! Object for name '" + names[actObj] + "' not found!\n");
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace(System.out);
            }
        }
        try {
            System.out.println("\n\nclearing database...");
            for (int i = 0; i < pool; i++) {
                if (names[i] != null) {
                    BLOBContainer blob = (BLOBContainer) db.objectForName(names[i]);
                    db.deleteObject(blob);
                    System.out.println(names[i] + " deleted.");
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }

    /** */
    private void createBlob(int _actObj) throws Exception {
        int size = objSize.nextInt() % (max - min);
        size = (size < 0 ? -size : size) + min;
        size *= 1024;
        String name = new String("Blob" + String.valueOf(_actObj));
        System.out.print("create " + name + "...");
        BLOBContainer blob = (BLOBContainer) db.createObject(BLOBContainerImpl.class.getName(), OzoneInterface.Public, name);
        blob.init(page);
        names[_actObj] = name;
        sizes[_actObj] = size;
        BLOBOutputStream out = new BLOBOutputStream(blob);
        CRC32 checksum = new CRC32();
        CheckedOutputStream checkedOut = new CheckedOutputStream(out, checksum);
        int pos = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            buffer[pos++] = (byte) i;
            if (pos == bsize) {
                checkedOut.write(buffer);
                pos = 0;
            }
        }
        if (pos != 0) {
            checkedOut.write(buffer, 0, pos);
        }
        long stop = System.currentTimeMillis();
        long bps = (long) size * 1000 / (stop - start);
        avWritespeed += bps;
        writecount++;
        movement += size;
        check[_actObj] = checksum.getValue();
        System.out.println("ok. (Size: " + sizes[_actObj] / 1024 + " kB, " + bps / 1024 + " kB/s)");
    }

    /** */
    private void verifyBlob(BLOBContainer _blob, int _actObj) throws Exception {
        System.out.print("verify " + names[_actObj] + "...");
        BLOBInputStream in = new BLOBInputStream(_blob);
        CRC32 checksum = new CRC32();
        CheckedInputStream checkedIn = new CheckedInputStream(in, checksum);
        int read = 0;
        int got;
        long start = System.currentTimeMillis();
        while ((got = checkedIn.read(buffer)) != -1) {
            read += got;
        }
        long stop = System.currentTimeMillis();
        long bps = (long) read * 1000 / (stop - start);
        avReadspeed += bps;
        readcount++;
        movement += read;
        if (read == sizes[_actObj] && checksum.getValue() == check[_actObj]) {
            System.out.print("ok. (Size: " + String.valueOf(read / 1024) + " kB, " + bps / 1024 + " kB/s), Checksum: " + checksum.getValue() + ")");
        } else {
            System.out.println("\nERROR!!! " + names[_actObj] + " is invalid. Size: " + String.valueOf(read / 1024) + " kB != " + String.valueOf(sizes[_actObj] / 1024) + "kB");
            System.out.println("                CHECKSUM is " + checksum.getValue() + " (should be " + check[_actObj] + ")\n");
        }
    }

    public static void main(String[] _args) throws Exception {
        int pool = POOLSIZE;
        int buffer = BUFFERSIZE;
        int page = PAGESIZE;
        int min = MINSIZE;
        int max = MAXSIZE;
        String host = "localhost";
        int port = 3333;
        try {
            for (int i = 0; i < _args.length; i++) {
                if (_args[i].startsWith("-help")) {
                    help();
                } else if (_args[i].startsWith("-pool")) {
                    pool = Integer.parseInt(_args[i].substring(5));
                } else if (_args[i].startsWith("-buffer")) {
                    buffer = Integer.parseInt(_args[i].substring(7));
                } else if (_args[i].startsWith("-page")) {
                    page = Integer.parseInt(_args[i].substring(5));
                } else if (_args[i].startsWith("-min")) {
                    min = Integer.parseInt(_args[i].substring(4));
                } else if (_args[i].startsWith("-max")) {
                    max = Integer.parseInt(_args[i].substring(4));
                } else if (_args[i].startsWith("-host")) {
                    host = _args[i].substring(5);
                } else {
                    if (_args[i].startsWith("-port")) {
                        port = Integer.parseInt(_args[i].substring(5));
                    }
                }
            }
        } catch (Exception e) {
            help();
        }
        RemoteDatabase db = new RemoteDatabase();
        try {
            db.open(host, port);
        } catch (Exception e) {
            System.out.print("No db found...");
            System.exit(0);
        }
        db.reloadClasses();
        System.out.println("connected...");
        BLOBTest blob = new BLOBTest(db, pool, buffer, page * 1024, min, max);
        System.out.println("----------------------------------------------------------------");
        System.out.println("  B L O B Test  starting the testthread. Press ENTER to abort.\n");
        System.out.println("environment:");
        System.out.println("connected to db at " + host + ":" + port);
        System.out.println("a pool of " + pool + " Blobs will be managed. ");
        System.out.println("Streambuffersize = " + buffer + " byte");
        System.out.println("BlobPagesize = " + page + " kB");
        System.out.println("Blobs will be created in the range from " + min + " kB to " + max + " kB");
        System.out.println("----------------------------------------------------------------");
        blob.start();
        long start = System.currentTimeMillis();
        System.in.read();
        blob.halted = true;
        System.out.println("\nTest halted. Waiting for last databaseoperation to quit...");
        blob.join();
        long stop = System.currentTimeMillis();
        db.close();
        System.out.println("disconnected.");
        System.out.println("----------------------------------------------------------------");
        System.out.println("statistics:");
        stop = (stop - start) / 1000;
        start = stop / 60;
        stop = (stop - start * 60);
        System.out.println("test runs " + start + " min and " + stop + " seconds, ca." + blob.movement / 1024 / 1024 + " MB of data were moved (read + write)");
        System.out.println("average writespeed: " + blob.avWritespeed / (blob.writecount == 0 ? 1 : blob.writecount) + " Bytes/s");
        System.out.println("average readspeed:  " + blob.avReadspeed / (blob.readcount == 0 ? 1 : blob.readcount) + " Bytes/s");
    }

    private static void help() {
        System.out.println("usage: BLOBTest -pool<> -buffer<> -page<> -min<> -max<> -host<> -port<>");
        System.out.println(" -pool<number of managed objects>");
        System.out.println(" -buffer<size of StreamBuffer (bytes)>");
        System.out.println(" -page<BlobPagesize (kBytes)>");
        System.out.println(" -min<minSize for Blobs (kBytes)>");
        System.out.println(" -max<maxSize for Blobs (kBytes)>");
        System.out.println(" -host<databaseserver (default: localhost)>");
        System.out.println(" -port<port for dbserver (default: 3333)>");
        System.out.println(" NOTE: Database has to be running!");
        System.exit(0);
    }
}
