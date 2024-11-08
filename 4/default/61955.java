import java.util.Date;
import java.util.Random;

/**
 * Tester for the disk driver, CS 537, Project 4, Spring 2007.
 * Note that this class will not compile until you have added methods getTime,
 * getDiskBlockCount, getDiskBlockSize, writeDiskBlock, and readDiskBlock
 * to Library.java.
 */
public class DiskTester {

    /** The id of this tester process.  Used to distinguish multiple concurrent
     * disk testers.
     */
    private int id;

    /** Testing modes. */
    private enum Mode {

        READWRITE, READONLY, WRITEONLY
    }

    ;

    /** Mode. */
    private Mode mode = Mode.READWRITE;

    /** Level of verbosity */
    private int verbose = 1;

    /** Debugging print.
     * @param l verbosity threshold.
     * @param o the message to print.
     */
    private void debug(int l, Object o) {
        if (verbose >= l) {
            System.out.println(id + ": " + o);
        }
    }

    /** The main program.
     * @param args command-line options.
     */
    public static void main(String[] args) {
        new DiskTester().run(args);
    }

    /** Prints a usage message and terminates the program. */
    private void usage() {
        debug(0, "usage: DiskTester [-qvicVrw] [-n count] id\n" + "  -q   less verbose debugging\n" + "  -v   more verbose debugging\n" + "  -c   random reads/writes are clustered (default is uniform)\n" + "  -i   start by initializing all blocks with a known value\n" + "  -V   verify all reads to ensure they return the right value\n" + "  -r   only read\n" + "  -w   only write\n" + "           default is read or write with equal probability\n" + "  -n N number of random reads/writes (defautl is zero)\n" + "  id   an id to identify this DiskTester instance\n");
        System.exit(1);
    }

    /** The actual body of main.  Defined to minimize use of static fields.
     * @param args command-line options.
     */
    private void run(String[] args) {
        int accessCount = 0;
        boolean clustered = false;
        boolean doInit = false;
        boolean verify = false;
        long start, stop;
        GetOpt options = new GetOpt("DiskTester", args, "qvicVrwn:");
        byte[] buf;
        int c;
        while ((c = options.nextOpt()) != -1) {
            switch(c) {
                default:
                    usage();
                    break;
                case 'v':
                    verbose++;
                    break;
                case 'q':
                    verbose--;
                    break;
                case 'i':
                    doInit = true;
                    break;
                case 'c':
                    clustered = true;
                    break;
                case 'V':
                    verify = true;
                    break;
                case 'r':
                    mode = Mode.READONLY;
                    break;
                case 'w':
                    mode = Mode.WRITEONLY;
                    break;
                case 'n':
                    accessCount = Integer.parseInt(options.optarg);
                    break;
            }
        }
        if (options.optind != args.length - 1) {
            usage();
        }
        id = Integer.parseInt(args[options.optind]);
        Random rand = new Random(System.currentTimeMillis() * id);
        start = Library.getTime();
        debug(1, "starting at " + new Date(start));
        int dsize = Library.getDiskBlockCount();
        if (dsize < 0) {
            debug(0, "bad disk size: " + Library.errorMessage[-dsize]);
            return;
        }
        debug(1, "disk size is " + dsize);
        int bsize = Library.getDiskBlockSize();
        if (bsize < 0) {
            debug(0, "bad block size: " + Library.errorMessage[-bsize]);
            return;
        }
        buf = new byte[bsize];
        debug(1, "block size is " + bsize);
        if (doInit) {
            debug(1, "writing to all blocks on disk");
            pack(id, buf, 0);
            pack(0, buf, 8);
            for (int i = 0; i < dsize; i++) {
                pack(i, buf, 4);
                debug(2, id + " write block " + show(buf));
                check(Library.writeDiskBlock(i, buf));
            }
            debug(1, "done initializing disk");
        }
        if (accessCount > 0) {
            debug(1, "random probes");
            int serial = 0;
            int hotSize = dsize / 10;
            int hotBase = (id % 10) * hotSize;
            for (int i = 0; i < accessCount; i++) {
                int b;
                if (clustered) {
                    if (rand.nextDouble() < 0.9) {
                        b = (hotBase + rand.nextInt(hotSize)) % dsize;
                    } else {
                        b = (hotBase + hotSize + rand.nextInt(dsize - hotSize)) % dsize;
                    }
                } else {
                    b = rand.nextInt(dsize);
                }
                boolean doWrite;
                switch(mode) {
                    case READONLY:
                        doWrite = false;
                        break;
                    case WRITEONLY:
                        doWrite = true;
                        break;
                    default:
                        doWrite = rand.nextBoolean();
                        break;
                }
                if (doWrite) {
                    pack(id, buf, 0);
                    pack(b, buf, 4);
                    pack(++serial, buf, 8);
                    debug(2, "write block " + show(buf));
                    check(Library.writeDiskBlock(b, buf));
                } else {
                    check(Library.readDiskBlock(b, buf));
                    if (verify && unpack(buf, 4) != b) {
                        debug(0, "bad data in block " + b + ": " + show(buf));
                        debug(0, "expected " + id + ":" + b);
                    } else {
                        debug(2, "read block " + b + " = " + show(buf));
                    }
                }
            }
        }
        stop = Library.getTime();
        debug(0, "done: " + (stop - start) / 1e3 + " sec elapsed");
    }

    /** Pack an integer into bytes of a byte array in big-endian (most
     * significant first) order.
     * @param val the value to be packed.
     * @param buf the array into which the value should be placed.
     * @param offset the starting location in buf.
     */
    private static void pack(int val, byte[] buf, int offset) {
        for (int i = 0; i < 4; i++) {
            buf[offset + i] = (byte) (val >> (8 * (3 - i)));
        }
    }

    /** Unack an integer from bytes of a byte array in big-endian (most
     * significant first) order.
     * @param buf the array into which the value was placed.
     * @param offset the starting location in buf.
     * @return the unpacked value.
     */
    private static int unpack(byte[] buf, int offset) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + (buf[offset + i] & 0xff);
        }
        return result;
    }

    /** Dump the first 12 bytes of the buffer in a readable format.
     * @param buf the buffer to be shown.
     * @return a string "checksum" of the buffer.
     */
    private static String show(byte[] buf) {
        return unpack(buf, 0) + ":" + unpack(buf, 4) + ":" + unpack(buf, 8);
    }

    /** Verify that a value that is supposed to be zero is in fact zero.
     * @param i the value to be checked.
     * @throws RuntimeException if i is non-zero.
     */
    private static void check(int i) {
    }
}
