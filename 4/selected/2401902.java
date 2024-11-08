package hpc;

import hpc.condor.BuildDagCommand;
import hpc.condor.BuildDagCommand.PowerOfTwo;
import hpc.io.DataLinkLayerParser;
import hpc.io.DataLinkLayerParserImpl;
import hpc.io.PresentationLayerParser;
import hpc.io.PresentationLayerParserImpl;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * http://www.freesoft.org/CIE/Topics/15.htm
 * The seven layers of the OSI Basic Reference Model are (from bottom to top):

   1. The Physical Layer describes the physical properties of the various communications media, as well as the electrical properties and interpretation of the exchanged signals. Ex: this layer defines the size of Ethernet coaxial cable, the type of BNC connector used, and the termination method.

   2. The Data Link Layer describes the logical organization of data bits transmitted on a particular medium. Ex: this layer defines the framing, addressing and checksumming of Ethernet packets.

   3. The Network Layer describes how a series of exchanges over various data links can deliver data between any two nodes in a network. Ex: this layer defines the addressing and routing structure of the Internet.

   4. The Transport Layer describes the quality and nature of the data delivery. Ex: this layer defines if and how retransmissions will be used to ensure data delivery.

   5. The Session Layer describes the organization of data sequences larger than the packets handled by lower layers. Ex: this layer describes how request and reply packets are paired in a remote procedure call.

   6. The Presentation Layer describes the syntax of data being transferred. Ex: this layer describes how floating point numbers can be exchanged between hosts with different math formats.

   7. The Application Layer describes how real work actually gets done. Ex: this layer would implement file system operations. 
 * @author bbrooks
 *
 */
public class CondorDriver implements ThreadExceptionListener {

    public static String VERSION_ID = "$Id: CondorDriver.java 71 2007-08-07 20:35:32Z bbrooks $";

    /**
     * A buffer must be big enough to store a long and at least one whitespace. 
     */
    private static final int MINIMUM_BUFFER_CAPACTIY = String.valueOf(Long.MAX_VALUE).length();

    /**
     * But we increase from that minimum to improve performance. 
     */
    private static final int BUFFER_CAPACTIY = 16 * 1024 * MINIMUM_BUFFER_CAPACTIY;

    private ReadableByteChannel leftAsFile;

    private ReadableByteChannel rightIn;

    private WritableByteChannel out;

    public static void main(String[] args) throws IOException {
        if (isSort(args)) {
            sort(args);
        } else if (isMerge(args)) {
            merge(args);
        } else if (isBuildDag(args)) {
            buildDag(args);
        } else {
            System.out.println("Usage: CondorDriver <-dag inFilename numCPUs | -sort inFilename | -merge inFilename1 inFilename2> outFilename");
            System.out.println("split --numeric-suffixes num.100.unsorted 'SXXX-'");
            System.out.println("where XXX is 1 less than the log base 2 of the numCPUs (the zero-based maximum depth of the mergesort)");
        }
    }

    /**
	 * Build Condor directed acyclic graph based on arguments.
	 * 
	 * @param args Expect two positional parameters input and numPieces.
	 */
    private static void buildDag(String[] args) {
        String input = args[1];
        String numPieces = args[2];
        PowerOfTwo numPiecesAsPowerOfTwo = PowerOfTwo.fromString(numPieces);
        BuildDagCommand dag = new BuildDagCommand(input, numPiecesAsPowerOfTwo);
        System.out.println(dag.toString());
    }

    private static boolean isBuildDag(String[] args) {
        return args.length > 2 && "-dag".equals(String.valueOf(args[0]).toLowerCase());
    }

    private static boolean isMerge(String[] args) {
        return args.length > 2 && "-merge".equals(String.valueOf(args[0]).toLowerCase());
    }

    private static boolean isSort(String[] args) {
        return args.length >= 2 && "-sort".equals(String.valueOf(args[0]).toLowerCase());
    }

    public static void sort(String[] args) throws IOException {
        Date begin = new Date();
        String input = args[1];
        String out = args[2];
        FileInputStream unsorted = null;
        try {
            unsorted = new FileInputStream(input);
        } catch (FileNotFoundException e) {
            System.err.println("sort input file args[1]=\"" + input + "\" caused FileNotFoundException");
            e.printStackTrace();
            throw e;
        }
        ReadableByteChannel leftAsFile = unsorted.getChannel();
        FileOutputStream outStream = new FileOutputStream(out);
        WritableByteChannel outChannel = outStream.getChannel();
        CondorDriver driver = new CondorDriver(leftAsFile, null, outChannel);
        try {
            driver.sort();
        } finally {
            if (leftAsFile != null) leftAsFile.close();
            if (outStream != null) {
                outStream.flush();
                outStream.close();
            }
        }
        if (driver.hasCaughtExceptions()) {
            System.err.println("main thread caught the following child exceptions: ");
            System.err.println("<exceptions>");
            StringWriter logOfExceptions = new StringWriter();
            PrintWriter writer = new PrintWriter(logOfExceptions);
            driver.printCaughtExceptions(writer);
            System.err.print(logOfExceptions.toString());
            System.err.println("</exceptions>");
        } else {
            System.out.println("no exceptions caught by main thread.");
        }
        Date end = new Date();
        System.out.println("sort took: " + (end.getTime() - begin.getTime()) + " milliseconds");
    }

    private void sort() {
        ByteBuffer leftRaw = ByteBuffer.allocate(BUFFER_CAPACTIY);
        DataLinkLayerParser leftDataParser = new DataLinkLayerParserImpl(leftAsFile, leftRaw);
        PresentationLayerParser leftPresentationParser = new PresentationLayerParserImpl(leftDataParser);
        ByteBuffer outBuffer = ByteBuffer.allocate(BUFFER_CAPACTIY);
        MergeOutputAdapter mergeOutput = new MergeOutputAdapterWriteableByteChannel(outBuffer, out);
        try {
            List<Long> inOut = new ArrayList<Long>();
            for (Long number : leftPresentationParser) {
                inOut.add(number);
            }
            JavaBuiltinSortHelper helper = new JavaBuiltinSortHelper(inOut, null, this);
            helper.run();
            for (Long number : inOut) {
                mergeOutput.put(number);
            }
        } finally {
            if (mergeOutput != null) mergeOutput.close();
        }
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void merge(String[] args) throws IOException {
        Date begin = new Date();
        FileInputStream left = null;
        try {
            left = new FileInputStream(args[1]);
        } catch (FileNotFoundException e) {
            System.err.println("left file args[1]=\"" + args[1] + "\" caused FileNotFoundException");
            e.printStackTrace();
            throw e;
        }
        FileInputStream right = null;
        try {
            right = new FileInputStream(args[2]);
        } catch (FileNotFoundException e) {
            System.err.println("right file args[2]=\"" + args[2] + "\" caused FileNotFoundException");
            e.printStackTrace();
            throw e;
        }
        FileOutputStream out = new FileOutputStream(args[3]);
        CondorDriver driver = new CondorDriver(left.getChannel(), right.getChannel(), out.getChannel());
        try {
            driver.mergeHelper();
        } finally {
            if (left != null) left.close();
            if (right != null) right.close();
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        if (driver.hasCaughtExceptions()) {
            System.err.println("main thread caught the following child exceptions: ");
            System.err.println("<exceptions>");
            StringWriter logOfExceptions = new StringWriter();
            PrintWriter writer = new PrintWriter(logOfExceptions);
            driver.printCaughtExceptions(writer);
            System.err.print(logOfExceptions.toString());
            System.err.println("</exceptions>");
        } else {
            System.out.println("no exceptions caught by main thread.");
        }
        Date end = new Date();
        System.out.println("merge took: " + (end.getTime() - begin.getTime()) + " milliseconds");
    }

    /**
     * @param leftIn
     * @param rightIn
     * @param out
     */
    public CondorDriver(ReadableByteChannel leftIn, ReadableByteChannel rightIn, WritableByteChannel out) {
        this.leftAsFile = leftIn;
        this.rightIn = rightIn;
        this.out = out;
    }

    public void mergeHelper() throws IOException {
        ByteBuffer leftRaw = ByteBuffer.allocate(BUFFER_CAPACTIY);
        DataLinkLayerParser leftDataParser = new DataLinkLayerParserImpl(leftAsFile, leftRaw);
        PresentationLayerParser leftPresentationParser = new PresentationLayerParserImpl(leftDataParser);
        Iterator<Long> itLeft = leftPresentationParser.iterator();
        ByteBuffer rightRaw = ByteBuffer.allocate(BUFFER_CAPACTIY);
        DataLinkLayerParser rightDataParser = new DataLinkLayerParserImpl(rightIn, rightRaw);
        PresentationLayerParser rightPresentationParser = new PresentationLayerParserImpl(rightDataParser);
        Iterator<Long> itRight = rightPresentationParser.iterator();
        ByteBuffer outBuffer = ByteBuffer.allocate(BUFFER_CAPACTIY);
        MergeOutputAdapter mergeOutput = new MergeOutputAdapterWriteableByteChannel(outBuffer, out);
        MergeSortHelper helper = new MergeSortHelper(itLeft, itRight, mergeOutput, null, this);
        helper.run();
        mergeOutput.close();
    }

    private Collection<Throwable> caughtExceptions = new ArrayList<Throwable>();

    public synchronized void caughtException(Throwable t) {
        caughtExceptions.add(t);
    }

    public synchronized boolean hasCaughtExceptions() {
        return !caughtExceptions.isEmpty();
    }

    public synchronized void printCaughtExceptions(PrintWriter writer) {
        for (Throwable caughtException : caughtExceptions) {
            caughtException.printStackTrace(writer);
        }
    }
}
