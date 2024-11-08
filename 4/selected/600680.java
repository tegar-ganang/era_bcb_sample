package org.jmule.core;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jmule.core.partialfile.GapList;
import org.jmule.core.partialfile.PartialFile;

/** FIXME: a class deserves a proper javadoc or a shot in the head. 
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:05 $
 */
public class SharedCompleteFile implements SharedFile {

    Logger log = Logger.getLogger(SharedCompleteFile.class.getName());

    public SharedCompleteFile(File myFile) {
        this.myFile = myFile;
        fileName = toString();
        if (myFile.isFile() && myFile.canRead()) {
            isComplete = true;
        }
        fileSize = myFile.length();
        extendedInfo = Collections.synchronizedMap(new HashMap());
        interestedSendSessions = new HashSet();
    }

    /**
    * Use this constructor to get a SharedCompleteFile from a PartialFile just completed.
    * Only use this if the partialfile is complete (the hashes form extendedInfo() fit to the file).
    */
    public SharedCompleteFile(PartialFile pf) {
        myFile = new File(pf.getPath());
        extendedInfo = pf.extendedInfo();
        isComplete = true;
        fileSize = pf.getSize();
        interestedSendSessions = new HashSet();
    }

    /**For parallel upload of a shared file this method should be uesd to the get Bytes.
     * @param startPosition Start position in the partial file.
     * @param bytes The buffer into which bytes are to be transferred.
     * @return number of bytes written into filedata
     * @throws IOException If an I/O error occurs.
     */
    public synchronized long getBytes(long startPosition, ByteBuffer filedata) throws IOException {
        long result = 0L;
        if (channel == null) throw new IOException("file not available");
        if (channel.isOpen()) {
            result = channel.read(filedata, startPosition);
        } else {
            log.warning("File " + this + " is not open.");
        }
        return result;
    }

    public void registerUploadSession(Object ob) {
        if (ob != null) {
            boolean ok = interestedSendSessions.add(ob);
            log.finer(ok ? ("register " + ob) : ob + (" is allready registered"));
            if (ok && interestedSendSessions.size() == 1) {
                try {
                    channel = (new RandomAccessFile(myFile, READONLY)).getChannel();
                } catch (FileNotFoundException fnfe) {
                    log.severe(this + " " + fnfe.getMessage());
                    SharesManager.getInstance().remove(this);
                }
            }
        }
    }

    public void releaseUploadSession(Object ob) {
        if (ob != null) {
            log.finer(interestedSendSessions.remove(ob) ? ("release " + ob) : ob + (" was not registered"));
            if (interestedSendSessions.isEmpty() && channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException ioe) {
                    log.severe(this + " " + ioe.getMessage());
                }
            }
        }
    }

    /** Returns only the file's name.
     * @return only the name of the file ( e.g. <i>name.txt</i> and <b>not</b> <i>share/name.txt</i>)
     */
    public String toString() {
        return (myFile.getName());
    }

    /** Returns the file's name with relative path.
     * @return the name of the file with relative path ( e.g. possibly only <i>share/name.txt</i> and not <i>/home/jmule/share/name.txt</i>)
     */
    public String getPath() {
        return (myFile.getPath());
    }

    public static final String READONLY = "r";

    private FileChannel channel;

    private Set interestedSendSessions;

    protected boolean isComplete = false;

    protected File myFile;

    protected String fileName;

    protected long fileSize;

    protected Map extendedInfo;

    private static GapList emptyGapList = new GapList();

    /** Provides a *gaplist*.
         * @return an empty gaplist.
         */
    public GapList getGapList() {
        return emptyGapList;
    }

    /** Returns the fileSize.
         * @return the file size in byte.
         */
    public long getSize() {
        return fileSize;
    }

    /** Returns the isValid.
     * @return <tt>true</tt> if this file is valid for sharing, otherwise <tt>false</tt>.
     */
    public boolean isValid() {
        return myFile.isFile() && myFile.canRead();
    }

    public File getFile() {
        return myFile;
    }

    /** Watch the name of this class.
         * @return should be <tt>true</tt>, if not don't use this file.
         */
    public boolean isComplete() {
        return isComplete;
    }

    public Map extendedInfo() {
        return extendedInfo;
    }

    public ArrayList getP2PProtocols() {
        return null;
    }

    public ArrayList getHashes() {
        return null;
    }
}
