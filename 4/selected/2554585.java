package eln.nob;

import java.lang.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import java.text.*;
import eln.util.*;
import javax.swing.tree.*;
import emsl.JavaShare.EmslProperties;
import eln.VersionInfo;

public class NOb extends Hashtable implements NObNode {

    public static final String OIDPROTOCOL = "oid://";

    public static final String FILEPROTOCOL = "file://";

    public static final String TEMPFILEPROTOCOL = "temp://";

    public static final String DATAREFPROTOCOL = "dataref://";

    public static final int COMPLETEDEBUG = 0;

    public static final int NONSIGNATUREONLY = 1;

    public static final int SIGNATURESONLY = 2;

    public static final Enumeration EMPTY_ENUMERATION = null;

    public static final TreeNode EMPTY_TREENODE = null;

    public static int kDefaultMaxInMemoryDataLength = 100000;

    private static int maxInMemoryDataLength = -1;

    private boolean generatedDataRef = false;

    public MutableTreeNode mParent = null;

    public NOb() {
        super();
        super.put("authorname", "");
        super.put("objectid", "");
        super.put("datetime", "");
        super.put("label", "");
        super.put("datatype", "");
        super.put("dataref", "");
        super.put("description", "");
    }

    public String readNObFromMIMEPart1(LineNumberReader theArchiveLines) throws IOException {
        String NObFieldBoundary;
        Hashtable headerBlock = NObArchive.readMIMEBlock(theArchiveLines);
        if (headerBlock.size() == 0) {
            throw new IOException("Empty block");
        }
        if (headerBlock.containsKey("Content-Arc-Field".toLowerCase())) {
            System.err.println("Warning: Non-standard ORNL Content-Arc-Field found. It will be skipped.");
            System.err.println("Field contents: \n" + headerBlock);
            String line = theArchiveLines.readLine();
            while (!line.equals("")) {
                System.err.println("Field data: " + line);
                line = theArchiveLines.readLine();
            }
            System.err.println("End field.");
            NObFieldBoundary = null;
        } else {
            if (!headerBlock.containsKey("Content-NOb-Num".toLowerCase())) {
                throw new IOException("Error reading NOb from archive. Cannot find \"Content-NOb-Num:\".");
            }
            if (!headerBlock.containsKey("Content-NOb-Version".toLowerCase())) {
                if (!headerBlock.containsKey("X-NOb-Version".toLowerCase())) {
                    throw new IOException("Error reading NOb from archive. NOb is not Content-NOb-Version compliant.");
                } else {
                    System.err.println("Warning: Non-standard \"X-NOb-Version\" used instead of \"Content-NOb-Version\"");
                }
            }
            String value = (String) headerBlock.get("Content-NOb-Version".toLowerCase());
            if (value == null) {
                value = (String) headerBlock.get("X-NOb-Version".toLowerCase());
            }
            if (!(value.equals("1.1"))) {
                throw new IOException("Error reading NOb from archive. NOb is not Content-NOb-Version: 1.1 compliant.");
            }
            if (!headerBlock.containsKey("content-type".toLowerCase())) {
                throw new IOException("Error reading NOb from archive. Cannot find \"Content-Type:\".");
            }
            StringTokenizer boundaryTokenizer = new StringTokenizer((String) headerBlock.get("Content-Type".toLowerCase()), "\"");
            boundaryTokenizer.nextToken();
            NObFieldBoundary = boundaryTokenizer.nextToken();
            String boundaryLine = theArchiveLines.readLine();
            while (boundaryLine.equals("")) {
                boundaryLine = theArchiveLines.readLine();
            }
            while (boundaryLine.equals("--" + NObFieldBoundary) && get("dataType").equals("")) {
                readFieldFromMIME(theArchiveLines);
                boundaryLine = theArchiveLines.readLine();
                while (boundaryLine.equals("")) {
                    boundaryLine = theArchiveLines.readLine();
                }
            }
            if (!boundaryLine.equals("--" + NObFieldBoundary)) {
                throw new IOException("Error reading archive boundary.\r\n Expected --" + NObFieldBoundary + "\r\nGot: " + boundaryLine);
            }
        }
        return NObFieldBoundary;
    }

    public void readNObFromMIMEPart2(LineNumberReader theArchiveLines, String theNObFieldBoundary) throws IOException {
        String boundaryLine = "--" + theNObFieldBoundary;
        while (boundaryLine.equals("--" + theNObFieldBoundary)) {
            readFieldFromMIME(theArchiveLines);
            boundaryLine = theArchiveLines.readLine();
            while (boundaryLine.equals("")) {
                boundaryLine = theArchiveLines.readLine();
            }
        }
        if (!boundaryLine.equals("--" + theNObFieldBoundary + "--")) {
            throw new IOException("Part 2: Error reading archive boundary.\r\n Expected --" + theNObFieldBoundary + "--\r\nGot: " + boundaryLine);
        }
    }

    public void remove(String key) {
        String realKey = key.toLowerCase();
        if ((realKey.equals("authorname")) || (realKey.equals("objectid")) || (realKey.equals("datetime")) || (realKey.equals("label")) || (realKey.equals("datatype")) || (realKey.equals("dataref"))) {
            super.put(realKey, "");
        } else {
            super.remove(realKey);
        }
    }

    public void releaseData() {
        remove("data");
        put("dataRef", OIDPROTOCOL + get("objectID"));
        remove("objectID");
    }

    public Object get(String key) {
        Object ret = super.get(key.toLowerCase());
        return ret;
    }

    public Object put(String key, Object value) {
        return (super.put(key.toLowerCase(), value));
    }

    public String toString() {
        return this.toString(NOb.COMPLETEDEBUG);
    }

    /** Print a string representation of the NOb for:
 * debugging, and
 * for signatures
 *
 *@param level COMPLETEDEBUG - all fields
 *             SIGNATURESONLY - just signature related fields
 *             NONSIGNATUREONLY - everything except signature fields

         //WARNING: This method assumes that the value for non-"data" fields are
         // Strings or have reasonable String representations. For instance,
         // if you have a field named "parent" and its value is this NOb's
         // parent NObList, this method will cause INFINITE RECURSION!!! (the
         // parent NOb's toString() representation of it's children calls their
         // toString methods.

 */
    public String toString(int level) {
        StringBuffer readout = new StringBuffer();
        readout.append("\nNotebook Object:\n");
        for (Enumeration elements = new NObFieldEnumeration(keys()); elements.hasMoreElements(); ) {
            String nextKey = (String) elements.nextElement();
            if ((nextKey.equals("data")) && ((level == NOb.COMPLETEDEBUG) || (level == NOb.NONSIGNATUREONLY))) {
                writeData(readout);
            } else if (nextKey.startsWith("sig")) {
                if (level != NOb.NONSIGNATUREONLY) {
                    readout.append(nextKey + ": " + get(nextKey).toString() + "\n");
                }
            } else {
                if (level != NOb.SIGNATURESONLY) {
                    readout.append(nextKey + ": " + get(nextKey).toString() + "\n");
                }
            }
        }
        readout.append("End Notebook Object.\n\n");
        return readout.toString();
    }

    protected void writeData(StringBuffer NObBuffer) {
        Object dataObject = get("data");
        if (dataObject == null) {
            NObBuffer.append("No data");
        } else {
            if (dataObject instanceof String) {
                NObBuffer.append("Data: " + ((String) dataObject).length() + " byte String:\n" + ((String) dataObject) + "\nEnd Data String.\n");
            } else if (dataObject instanceof byte[]) {
                NObBuffer.append("Data: " + ((byte[]) dataObject).length + " byte byte[]\n");
            } else {
                NObBuffer.append("Unprintable Data. ");
            }
        }
    }

    public void writeToXML(OutputStream anOutputStream, boolean encodeData) {
        NObXMLExportUtility export = new NObXMLExportUtility((NObNode) this);
        export.writeToXML(anOutputStream, encodeData);
    }

    public void writeToXML(NObXMLArchive anArchive, String currentIndent, boolean encodeData) {
        NObXMLExportUtility export = new NObXMLExportUtility((NObNode) this);
        export.writeToXML(anArchive, currentIndent, encodeData);
    }

    public void writeToMIME(OutputStream anOutputStream) {
        NObExportUtility export = new NObExportUtility((NObNode) this);
        export.writeToMIME(anOutputStream);
    }

    public void writeToMIME(NObArchive anArchive) {
        NObExportUtility export = new NObExportUtility((NObNode) this);
        export.writeToMIME(anArchive);
    }

    public void readFieldFromMIME(LineNumberReader theArchiveLines) throws IOException {
        Hashtable fieldBlock = NObArchive.readMIMEBlock(theArchiveLines);
        String exitBoundary;
        String key;
        String encoding;
        String mimeType;
        Object value;
        long fieldLength;
        if (!fieldBlock.containsKey("Content-NOb-Field".toLowerCase())) {
            throw new IOException("Error reading NOb Field from archive. Cannot find \"Content-NOb-Field:\".");
        }
        if (!fieldBlock.containsKey("Content-Transfer-Encoding".toLowerCase())) {
            throw new IOException("Error reading NOb Field from archive. \"Content-Transfer-Encoding\" is not set.");
        }
        if (!fieldBlock.containsKey("Content-Type".toLowerCase())) {
            throw new IOException("Error reading NOb Field from archive. \"Content-Type\" is not set.");
        }
        if (!fieldBlock.containsKey("Content-Length".toLowerCase())) {
            if (!((String) fieldBlock.get("Content-Type".toLowerCase())).startsWith("multipart")) {
                throw new IOException("Error reading NOb Field from archive. \"Content-Length\" must be set.");
            }
        }
        key = (String) fieldBlock.get("Content-NOb-Field".toLowerCase());
        encoding = (String) fieldBlock.get("Content-Transfer-Encoding".toLowerCase());
        mimeType = (String) fieldBlock.get("Content-Type".toLowerCase());
        String lengthString = (String) fieldBlock.get("Content-Length".toLowerCase());
        if (lengthString != null) {
            fieldLength = (new Long(lengthString)).longValue();
        } else {
            fieldLength = -1;
        }
        if (!(key.equals("data"))) {
            if (!(mimeType.startsWith("text/plain"))) {
                throw new IOException("Error reading NOb Field from archive. \r\nExpected Content-Type: text/plain\r\nGot: " + (String) fieldBlock.get("Content-Type".toLowerCase()));
            }
        }
        decodeAndStoreFieldData(key, encoding, mimeType, fieldLength, theArchiveLines);
    }

    protected void decodeAndStoreFieldData(String theKey, String theEncoding, String theMimeType, long theLength, LineNumberReader theArchiveLines) throws IOException {
        Object value = null;
        if (theLength == 0L) {
            value = "";
        } else {
            OutputStream dataStream = null;
            if (theKey.equalsIgnoreCase("data")) {
                dataStream = openNObNodeDataOutputStream(theLength);
            } else {
                if (theLength > getMaxInMemoryDataLength()) {
                    throw new IOException("Warning: Encountered non data field (" + theKey + ") longer than maximum in memory length(currently " + Long.toString(getMaxInMemoryDataLength()) + ")");
                }
                dataStream = new ByteArrayOutputStream((int) theLength);
            }
            if (theEncoding.equalsIgnoreCase("quoted-printable")) {
                QuotedPrintable.decodeFromReader(theArchiveLines, theLength, dataStream);
            } else if (theEncoding.equalsIgnoreCase("base64")) {
                Base64.decodeFromReader(theArchiveLines, theLength, dataStream);
            }
            dataStream.close();
            if (dataStream instanceof ByteArrayOutputStream) {
                if (theMimeType.startsWith("text/")) {
                    value = ((ByteArrayOutputStream) dataStream).toString();
                } else {
                    value = ((ByteArrayOutputStream) dataStream).toByteArray();
                }
            }
        }
        if (value != null) {
            if ((!theKey.equalsIgnoreCase("dataref")) || (generatedDataRef == false)) {
                super.put(theKey.toLowerCase(), value);
            }
        }
        theArchiveLines.readLine();
    }

    /** @return the index of the NOb's "level", e.g. NObList.kNotebookLevel
 */
    public int getLevelIndex() {
        String level = (String) get("level");
        assert (level != null);
        int myLevelIndex = -1;
        int i;
        for (i = 0; i < NObListNode.kLevels.length; i++) {
            if (level.equals(NObListNode.kLevels[i])) {
                myLevelIndex = i;
            }
        }
        assert (myLevelIndex != -1);
        return myLevelIndex;
    }

    /**
*/
    public InputStream openNObNodeDataInputStream() throws IOException {
        InputStream dataStream = null;
        Object dataObject = get("data");
        boolean usingFile = false;
        if (dataObject == null) {
            String dataRef = (String) get("dataref");
            if (dataRef.startsWith(FILEPROTOCOL)) {
                usingFile = true;
                dataRef = dataRef.substring(FILEPROTOCOL.length());
            }
            if (dataRef.startsWith(TEMPFILEPROTOCOL)) {
                usingFile = true;
                dataRef = dataRef.substring(TEMPFILEPROTOCOL.length());
            }
            if (usingFile == true) {
                File dataFile = new File(dataRef);
                dataStream = (InputStream) new FileInputStream(dataFile);
            } else {
                dataStream = (InputStream) new ByteArrayInputStream(("dataref://" + dataRef).getBytes());
            }
        } else {
            if (dataObject instanceof String) {
                dataStream = (InputStream) new ByteArrayInputStream(((String) dataObject).getBytes());
            } else if (dataObject instanceof byte[]) {
                dataStream = (InputStream) new ByteArrayInputStream((byte[]) dataObject);
            }
        }
        return dataStream;
    }

    /**
*/
    public OutputStream openNObNodeDataOutputStream(long theLength) throws IOException {
        OutputStream dataStream = null;
        if (theLength <= getMaxInMemoryDataLength()) {
            dataStream = new ByteArrayOutputStream((int) theLength);
        } else {
            File tempFile = generateTempFile();
            dataStream = new FileOutputStream(tempFile);
        }
        return dataStream;
    }

    /** Will convert an existing String or byte[] data field in memory to a
 * tempfile if it is larger than the default max in memory length.
 * dataref and data fields are changed appropriately
 */
    public void createTempFileIfNeeded() {
        Object data = get("data");
        long dataLength = 0;
        if (data != null) {
            if (data instanceof String) {
                dataLength = ((String) data).length();
            } else if (data instanceof byte[]) {
                dataLength = ((byte[]) data).length;
            }
            if (dataLength > getMaxInMemoryDataLength()) {
                try {
                    OutputStream dataStream = openNObNodeDataOutputStream(dataLength);
                    if (data instanceof String) {
                        OutputStreamWriter dataWriter = new OutputStreamWriter(dataStream);
                        dataWriter.write((String) data);
                        dataWriter.close();
                    } else if (data instanceof byte[]) {
                        dataStream.write((byte[]) data);
                        dataStream.close();
                    }
                    if ((data instanceof String) || (data instanceof byte[])) {
                        remove(data);
                        put("dataref", TEMPFILEPROTOCOL + (String) get("objectid"));
                    } else {
                        System.err.println("Encountered data of type: " + data.getClass().getName() + " in Nob.java");
                    }
                } catch (IOException io) {
                    System.err.println("Error moving data from in memory to temp file in NOb.java");
                }
            }
        }
    }

    /**Use the objectid and datatype to calculate a temporary file name,
 * creating any directories needed.
 * Note: Sets dataref to reflect the creation of a temporary file

    //Keep full OID path in temp space.
    //Create a temp file:  <elnTempDir>/<oid>. For new files, create a newEntry subdir?
    //By keeping full OID, and recycling the new Entry dir (delete contents after
    // submission, we don't have to worry about server name translation,
    //imagine signing two entries consecutively that have elncap0.gif in them, or
    //an entry with a short text message and a long one (elnfile.txt on the client)
    //after going to the server, the short text will have OID .../elnfile.txt and
    //the long one will be .../elnfile_1.txt (or something worse if other text
    //entries exist in other notes on this page

 */
    protected File generateTempFile() throws IOException {
        String elnTempDir = "elntemp";
        String fileSep = System.getProperty("file.separator");
        try {
            EmslProperties elnProps = EmslProperties.getApplicationProperties();
            elnTempDir = (String) elnProps.get("tmpdir");
            if (elnTempDir == null) {
                elnTempDir = "elntemp";
            }
        } catch (IOException io) {
            System.err.println("Couldn't open Application Properties file in PseudoServer.java: " + io.getMessage());
        }
        String objectID = (String) get("objectid");
        File tempFile;
        if (objectID.length() > 0) {
            if (objectID.startsWith("http")) {
                objectID = objectID.substring(objectID.indexOf("note.cgi/top/") + "note.cgi/top/".length());
            }
            objectID.replace('/', fileSep.charAt(0));
            tempFile = new File(elnTempDir + File.separator + objectID);
        } else {
            String extension = MimeTypes.getExtension((String) get("datatype"));
            int index = 0;
            objectID = ("newentry" + fileSep + "ELNFile" + (new Integer(index)).toString() + extension);
            tempFile = new File(elnTempDir + File.separator + objectID);
            while (tempFile.exists()) {
                index++;
                objectID = ("newentry" + fileSep + "ELNFile" + (new Integer(index)).toString() + extension);
                tempFile = new File(elnTempDir + File.separator + objectID);
            }
        }
        File tempDir = new File(tempFile.getParent());
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new IOException("Unable to create dir for temporary files" + " in NOb.openNObDataOutputStream()");
            }
        }
        put("dataref", TEMPFILEPROTOCOL + elnTempDir + fileSep + objectID);
        return tempFile;
    }

    /** Clear the data and dataref fields, releasing the data object for
 *  garbage collection, and deleting any associated temp file.
 */
    public void clearData() {
        String dataref = (String) get("dataref");
        if (dataref.startsWith(TEMPFILEPROTOCOL)) {
            dataref = dataref.substring(TEMPFILEPROTOCOL.length());
            File tempFile = new File(dataref);
            if (!tempFile.delete()) {
                System.err.println("Unable to delete temp file: " + dataref + "in NOb.clearData()");
            }
        }
        remove("data");
        remove("dataref");
    }

    /** initializes the maxInMemoryDataLength variable and returns it.
 *  @return long value of the int maxInMemoryDataLength.
 */
    public static long getMaxInMemoryDataLength() {
        if (maxInMemoryDataLength == -1) {
            maxInMemoryDataLength = kDefaultMaxInMemoryDataLength;
            try {
                EmslProperties elnProps = EmslProperties.getApplicationProperties();
                String max = (String) elnProps.get("maxInMemoryDataLength");
                if (max != null) {
                    maxInMemoryDataLength = new Integer((String) elnProps.get("maxInMemoryDataLength")).intValue();
                }
            } catch (IOException io) {
                System.err.println("Warning: Error reading maxInMemoryDataLength from ini file - using default value:" + io.getMessage());
            } catch (NumberFormatException nf) {
                System.err.println("Warning: Error converting maxInMemoryDataLength from ini file to long value - using default");
            }
        }
        return ((long) maxInMemoryDataLength);
    }

    /**
//  getDataLength()
// @return long length - the length of the data:
//                    - the length of the data file (if dataref is a file:// reference)
//                    - the length of the data object
//                    - the length of the dataref (prepended with dataref://)
*/
    public long getDataLength() throws IOException {
        long length = 0;
        Object dataObject = get("data");
        if (dataObject == null) {
            String dataRef = (String) get("dataref");
            boolean isFile = false;
            if (dataRef.startsWith(FILEPROTOCOL)) {
                isFile = true;
                dataRef = dataRef.substring(FILEPROTOCOL.length());
            } else if (dataRef.startsWith(TEMPFILEPROTOCOL)) {
                isFile = true;
                dataRef = dataRef.substring(TEMPFILEPROTOCOL.length());
            }
            if (isFile == true) {
                File dataFile = new File(dataRef);
                length = dataFile.length();
            } else {
                length = "dataref://".length() + dataRef.length();
            }
        } else {
            if (dataObject instanceof String) {
                length = ((String) dataObject).length();
            } else if (dataObject instanceof byte[]) {
                length = ((byte[]) dataObject).length;
            }
        }
        return length;
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public TreeNode getChildAt(int childIndex) {
        return EMPTY_TREENODE;
    }

    public int getChildCount() {
        return 0;
    }

    public TreeNode getParent() {
        return (TreeNode) mParent;
    }

    public int getIndex(TreeNode node) {
        throw new IllegalArgumentException("NObs don't have children");
    }

    public boolean isLeaf() {
        return true;
    }

    public Enumeration children() {
        return EMPTY_ENUMERATION;
    }

    public void setParent(MutableTreeNode newParent) {
        mParent = newParent;
    }

    public void removeFromParent() {
        mParent.remove((MutableTreeNode) this);
    }

    public void remove(MutableTreeNode aChild) {
        throw new IllegalArgumentException("NObs don't have children");
    }

    public void remove(int index) {
        throw new IllegalArgumentException("NObs don't have children");
    }

    public void insert(MutableTreeNode newChild, int childIndex) {
        throw new IllegalStateException("node does not allow children");
    }

    public void setUserObject(Object userObject) {
    }

    public String getOriginalOID() {
        String origID = (String) get("objectid");
        if (origID.equals("")) {
            String ref = (String) get("dataRef");
            if (ref.startsWith(OIDPROTOCOL)) {
                origID = ref.substring(OIDPROTOCOL.length());
            }
        }
        return origID;
    }
}
