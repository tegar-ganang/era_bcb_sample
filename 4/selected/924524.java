package org.jmule.core.partialfile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.jmule.core.DownloadManager;
import org.jmule.core.SharedFile;

/** The <code>PartialFile</code> holds informations about the file belonging to a download. 
 * 
 * <code>PartialFile</code> is splited into chunks, which have a specifing length for each protocol.   
 * @author pola
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:12 $
 */
public class PartialFile implements SharedFile {

    static final Logger log = Logger.getLogger(PartialFile.class.getName());

    private String tempFileName;

    private String fileName;

    protected long size;

    private long transferredBytes = 0;

    private long lastchangetime = 0;

    private boolean changed = false;

    private boolean resetlastchangetime = true;

    private boolean complete = false;

    private File tempFileHandle;

    private FileChannel tempFileChannel;

    private RandomAccessFile tempFileRandomAccessHandle;

    private String tempFileMode = "rw";

    private GapList gapList;

    /** Constructor of PartialFile.
     * @param fileName Filename of the resulting file.
     * @param fileSize Size of the resulting file.
     * @param tempFileName Filename of the file holding temporal data.
     * @throws IOException If an I/O error occurs.
     */
    public PartialFile(String fileName, long fileSize, String tempFileName) throws IOException {
        this.fileName = DownloadManager.getInstance().getIncomingDirectory().getPath() + File.separator + fileName;
        this.tempFileName = DownloadManager.getInstance().getTempDirectory().getPath() + File.separator + tempFileName;
        this.tempFileHandle = new File(this.tempFileName);
        this.size = fileSize;
        this.gapList = new GapList();
        this.gapList.addGap(0, this.size);
        if (!tempFileHandle.exists()) {
            tempFileRandomAccessHandle = new RandomAccessFile(this.tempFileName, "rws");
            tempFileRandomAccessHandle.setLength(this.size);
            tempFileRandomAccessHandle.close();
            tempFileRandomAccessHandle = null;
        }
    }

    /** Constructor PartialFile. the file with tempFileName has to exist
     * @param fileName Filename of the resulting file.
     * @param fileSize Size of the resulting file.
     * @param tempFileName Filename of the file holding temporal data. absolutepath or relative to jmule dir
     * @throws IOException If an I/O error occurs or the file tempFileName .
     * CHECK: What is exactly the difference between the two constructors ? or why the tempFileName has to exist here and in the other one not ?
     * XXX: merge the tow constructors.
     */
    public PartialFile(String fileName, long fileSize, String tempFileName, GapList gaplist) throws IOException {
        this.fileName = DownloadManager.getInstance().getIncomingDirectory().getPath() + File.separator + fileName;
        this.tempFileName = tempFileName;
        this.tempFileHandle = new File(this.tempFileName);
        this.size = fileSize;
        this.gapList = gaplist;
        if (tempFileHandle.exists()) {
            tempFileRandomAccessHandle = new RandomAccessFile(this.tempFileName, "rws");
            tempFileRandomAccessHandle.close();
            tempFileRandomAccessHandle = null;
        } else {
            throw new IOException("File " + tempFileName + " does not exsists.");
        }
    }

    /** Adds bytes to the partial file.
     * @param bytes Bytes to add to the file.
     * @param startPosition Number of bytes to skip from the begining of the file
     * @throws IOException If an I/O error occurs.
     * @return The ammount of bytes really added to the file. This is because you can
     * try to write bytes that are already in the file.
     */
    public synchronized int addBytes(ByteBuffer bytes, long startPosition) throws IOException {
        if (bytes.remaining() == 0) return 0;
        FileChannel fc = this.getFileChannel();
        long endPosition = startPosition + bytes.limit() - bytes.position();
        long start;
        long end;
        long length;
        ByteBuffer tempByteBuffer;
        int nBytes = 0;
        Collection gaps = this.gapList.getIntersectedGaps(startPosition, endPosition);
        for (Iterator i = gaps.iterator(); i.hasNext(); ) {
            Gap gap = (Gap) i.next();
            start = Math.max(gap.start, startPosition);
            end = Math.min(gap.end, endPosition);
            length = end - start;
            tempByteBuffer = bytes.duplicate();
            tempByteBuffer.position((int) (bytes.position() + start - startPosition));
            tempByteBuffer.limit((int) (bytes.position() + start - startPosition + length));
            fc.position(start);
            nBytes += fc.write(tempByteBuffer);
            gapList.removeGap(start, end);
        }
        if (nBytes > 0) {
            notifyChange();
        }
        return nBytes;
    }

    /**
    * Set *last*changetime to current time, changed to <tt>true</tt> and resetlastchangetime to <tt>false</tt>, if resetlastchangetime.
    */
    private void notifyChange() {
        if (resetlastchangetime) {
            lastchangetime = System.currentTimeMillis();
            changed = true;
            resetlastchangetime = false;
        }
    }

    /**
    * Returns the change flag.
    * @return <tt>true</tt> if bytes was add after the last resetLastChangeTime, otherwise <tt>false</tt>.
    */
    public boolean hasChanged() {
        return changed;
    }

    /**
    * Returns the time of the *last* change that toke place when change flag was false.
    * @return time in ms if ever changed, otherwise <tt>0</tt>.
    */
    public long timeOfLastChange() {
        return lastchangetime;
    }

    /**
    * Resets the change flag.
    */
    public void resetLastChangeTime() {
        resetlastchangetime = true;
        changed = false;
    }

    /** Reads a sequence of bytes from this partial file into the given buffer.
     * @return The number of bytes read, possibly zero, or -1 if no valid data
     * are available.
     * @param startPosition Start position in the partial file.
     * @param bytes The buffer into which bytes are to be transferred.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized long getBytes(long startPosition, ByteBuffer bytes) throws IOException {
        int toRead = bytes.remaining();
        Gap gap = this.gapList.getFirstGapBefore(startPosition);
        if ((gap != null) && (gap.end > startPosition)) return 0;
        gap = this.gapList.getFirstGapAfter(startPosition);
        if ((gap != null) && (gap.start <= startPosition + toRead)) toRead = (int) (gap.start - startPosition);
        bytes.limit(bytes.position() + toRead);
        return this.getFileChannel().read(bytes, startPosition);
    }

    public void registerUploadSession(Object ob) {
    }

    public void releaseUploadSession(Object ob) {
    }

    public String getPath() {
        return fileName;
    }

    private Map extendedInfo = Collections.synchronizedMap(new HashMap());

    ;

    /** The extended Info gets used to pass all other informations
     * about the file, that are not explicite stated in the <code>PartialFile</code> class.
     * E.g. the donkey file hash.
     */
    public Map extendedInfo() {
        return extendedInfo;
    }

    public long getSize() {
        return size;
    }

    public boolean isValid() {
        return tempFileHandle.canRead();
    }

    /**
	* Returns the GapList.
	* 
	* @return GapList
	*/
    public GapList getGapList() {
        return gapList;
    }

    public boolean isComplete() {
        return complete;
    }

    /**
    * Warning: behaves like {@link org.jmule.core.SharedCompleteFile#toString() toString() of SharedCompleteFile}.
    */
    public String toString() {
        return (new File(this.fileName)).getName();
    }

    /**
	* Discards bytes in the partial file.
	* 
	* @param startPosition The range start.
	* @param endPosition The range end. 
	*/
    public void discardBytes(long startPosition, long endPosition) {
        this.gapList.removeGap(startPosition, endPosition);
    }

    /**
	* Returns the amount of bytes transferred.
	* 
	* @return The amount of bytes transferred.
	*/
    public long getTransferredBytes() {
        return this.size - this.gapList.byteSize();
    }

    /** Releases all resources used by this partial file and
     * deletes the file.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized void cancel() throws IOException {
        if (tempFileChannel != null) {
            tempFileChannel.close();
            tempFileChannel = null;
        }
        if (tempFileRandomAccessHandle != null) {
            tempFileRandomAccessHandle.close();
            tempFileRandomAccessHandle = null;
        }
        if (tempFileHandle != null) {
            tempFileHandle.delete();
        }
    }

    /** Releases all resources used by this partial file.
     * @throws IOException If an I/O error occurs.
     */
    public synchronized void close() throws IOException {
        if (tempFileChannel != null) {
            tempFileChannel.close();
            tempFileChannel = null;
        }
        if (tempFileRandomAccessHandle != null) {
            tempFileRandomAccessHandle.close();
            tempFileRandomAccessHandle = null;
        }
    }

    /**
    * Returns the name of the temporary file including the relative path to jmule's working directory
    *@return name of the temporary file including the relative path to jmule's workingdirectory
    */
    public String getTempFileName() {
        return tempFileName;
    }

    /**
    * Moves a complete file to incoming directory.
    * No protocol specific checks are done and resources used by this partial file have to be released before.
    * @return <tt>true</tt> if move into incomming was successful otherwise <tt>false</tt>    */
    public boolean complete() {
        complete = true;
        if (tempFileHandle.renameTo(new File(fileName))) {
            log.info(fileName + " is now complete and moved to incoming directory");
            tempFileHandle = new File(fileName);
            tempFileMode = "r";
            tempFileChannel = null;
            this.tempFileRandomAccessHandle = null;
            return true;
        } else {
            log.warning(fileName + " is now complete, but couldn't be moved to incoming directory; name now stays " + tempFileName);
            tempFileMode = "r";
            return false;
        }
    }

    /**
	* Returns the file channel to write/read from data to the file.
	* We hold one channel per PartialFile. If there isn't a file channel set up, we set 
	* up one and *aquiere a lock* on this file. So nobody else can mess up with this file.
	*/
    private FileChannel getFileChannel() throws IOException {
        if (this.tempFileChannel == null) {
            if (this.tempFileRandomAccessHandle == null) this.tempFileRandomAccessHandle = new RandomAccessFile(this.tempFileHandle, this.tempFileMode);
            this.tempFileChannel = this.tempFileRandomAccessHandle.getChannel();
        }
        ;
        return this.tempFileChannel;
    }

    /** This is only for testing purposes.
     * @param args The command line parameters.
     */
    public static void main(String[] args) {
        int counter = 0;
        java.nio.charset.Charset ascii = java.nio.charset.Charset.forName("US-ASCII");
        java.nio.CharBuffer cb = java.nio.CharBuffer.allocate(26);
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(26);
        String toWrite = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        PartialFile partFile = null;
        Random rand = new Random();
        int iterations = 10;
        for (int i = 0; i < iterations; i++) try {
            System.out.println("\n\n**********************\nIteration: " + i);
            partFile = new PartialFile("prueba.txt", toWrite.length(), "prueba.tmp");
            while (partFile.getTransferredBytes() < toWrite.length()) {
                int start = rand.nextInt(26);
                int lenght = rand.nextInt(6);
                if (start + lenght > toWrite.length()) lenght = toWrite.length() - start;
                String chunk = toWrite.substring(start, start + lenght);
                System.out.println("Transferred bytes: " + partFile.getTransferredBytes());
                System.out.println("Current GapList: " + partFile.getGapList().toString());
                System.out.println("\nTrying to write: " + chunk + " [" + start + ":" + (start + lenght - 1) + "]");
                cb.clear();
                bb.clear();
                cb.put(chunk);
                cb.flip();
                bb.put(ascii.encode(cb));
                bb.flip();
                partFile.addBytes(bb, start);
            }
            System.out.println("Transferred bytes: " + partFile.getTransferredBytes());
            System.out.println("Current GapList: " + partFile.getGapList().toString());
            partFile.close();
            File file = new File("prueba.tmp");
            javax.imageio.stream.FileImageInputStream fip = new javax.imageio.stream.FileImageInputStream(file);
            byte[] testBytes = new byte[toWrite.length()];
            fip.read(testBytes);
            String test = new String(testBytes);
            if (!test.equals(toWrite)) System.err.println("\nWritten doesn`t match ");
        } catch (Exception e) {
            System.err.println("\nError occurred during the test: ");
            e.printStackTrace();
            System.out.println("");
        }
    }

    public ArrayList getP2PProtocols() {
        return null;
    }

    public ArrayList getHashes() {
        return null;
    }
}
