package org.broad.igv.tdf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.broad.igv.tdf.TDFWriter.IndexEntry;
import org.broad.igv.track.WindowFunction;
import org.broad.igv.util.CompressionUtils;

/**
 * Assumptions
 * <p/>
 * Little endian is used throughout Strings are null terminated ascii (single
 * byte
 * 
 * @author Thomas Abeel
 * @author jrobinso
 */
public class TDFWriter {

    private static Logger log = Logger.getLogger(TDFWriter.class.getCanonicalName());

    private static int version = 3;

    private OutputStream fos = null;

    private long bytesWritten = 0;

    private File file;

    private Map<String, TDFGroup> groupCache = new LinkedHashMap<String, TDFGroup>();

    private Map<String, TDFDataset> datasetCache = new LinkedHashMap<String, TDFDataset>();

    private Map<String, IndexEntry> datasetIndex = new LinkedHashMap<String, IndexEntry>();

    private Map<String, IndexEntry> groupIndex = new LinkedHashMap<String, IndexEntry>();

    private long indexPositionPosition;

    private boolean compressed;

    public TDFWriter(File f, String genomeId, String trackType, String trackLine, String[] trackNames, Collection<WindowFunction> windowFunctions, boolean compressed) {
        if (f.getName().endsWith(".tdf")) {
            this.file = f;
        } else {
            this.file = new File(f.getAbsolutePath() + ".tdf");
        }
        this.compressed = compressed;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(file));
            writeHeader(genomeId, trackType, trackLine, trackNames, windowFunctions);
            TDFGroup rootGroup = new TDFGroup("/");
            groupCache.put(rootGroup.getName(), rootGroup);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating file" + file.getAbsolutePath());
        }
    }

    private void writeHeader(String genomeId, String trackType, String trackLine, String[] trackNames, Collection<WindowFunction> windowFunctions) throws IOException {
        byte[] magicNumber = new byte[] { 'T', 'D', 'F', '3' };
        BufferedByteWriter buffer = new BufferedByteWriter(24);
        buffer.put(magicNumber);
        buffer.putInt(version);
        indexPositionPosition = buffer.bytesWritten();
        buffer.putLong(0l);
        buffer.putInt(0);
        write(buffer.getBytes());
        buffer = new BufferedByteWriter(24);
        buffer.putInt(windowFunctions.size());
        for (WindowFunction wf : windowFunctions) {
            buffer.putNullTerminatedString(wf.toString());
        }
        buffer.putNullTerminatedString(trackType.toString());
        byte[] trackLineBuffer = bufferString(trackLine, 1024);
        buffer.put(trackLineBuffer);
        buffer.putInt(trackNames.length);
        for (String nm : trackNames) {
            buffer.putNullTerminatedString(nm);
        }
        buffer.putNullTerminatedString(genomeId);
        int flags = 0;
        if (compressed) {
            flags |= TDFReader.GZIP_FLAG;
        } else {
            flags &= ~TDFReader.GZIP_FLAG;
        }
        buffer.putInt(flags);
        byte[] bytes = buffer.getBytes();
        writeInt(bytes.length);
        write(buffer.getBytes());
    }

    /**
	 * Write out the group and dataset index and close the underlying file.
	 */
    public void closeFile() {
        try {
            writeDatasets();
            writeGroups();
            long indexPosition = bytesWritten;
            writeIndex();
            int nbytes = (int) (bytesWritten - indexPosition);
            fos.close();
            writeIndexPosition(indexPosition, nbytes);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error closing file");
        }
    }

    private void writeIndexPosition(long indexPosition, int nbytes) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.getChannel().position(indexPositionPosition);
            System.out.println("Index position position: " + indexPositionPosition);
            System.out.println("Index position: " + indexPosition);
            System.out.println("nBytes: " + nbytes);
            BufferedByteWriter buffer = new BufferedByteWriter();
            buffer.putLong(indexPosition);
            buffer.putInt(nbytes);
            raf.write(buffer.getBytes());
            raf.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public TDFGroup getGroup(String name) {
        return groupCache.get(name);
    }

    public TDFGroup getRootGroup() {
        if (!groupCache.containsKey("/")) {
            groupCache.put("/", new TDFGroup("/"));
        }
        return groupCache.get("/");
    }

    public TDFGroup createGroup(String name) {
        if (groupCache.containsKey(name)) {
            throw new RuntimeException("Group: " + name + " already exists");
        }
        TDFGroup group = new TDFGroup(name);
        groupCache.put(name, group);
        return group;
    }

    public TDFDataset createDataset(String name, TDFDataset.DataType dataType, int tileWidth, int nTiles) {
        if (datasetCache.containsKey(name)) {
            throw new RuntimeException("Dataset: " + name + " already exists");
        }
        TDFDataset ds = new TDFDataset(name, dataType, tileWidth, nTiles);
        datasetCache.put(name, ds);
        return ds;
    }

    public void writeTile(String dsId, int tileNumber, TDFTile tile) throws IOException {
        TDFDataset dataset = datasetCache.get(dsId);
        if (dataset == null) {
            throw new java.lang.NoSuchFieldError("Dataset: " + dsId + " doese not exist.  " + "Call createDataset first");
        }
        long pos = bytesWritten;
        if (tileNumber < dataset.tilePositions.length) {
            dataset.tilePositions[tileNumber] = pos;
            BufferedByteWriter buffer = new BufferedByteWriter();
            tile.writeTo(buffer);
            byte[] bytes = buffer.getBytes();
            if (compressed) {
                bytes = CompressionUtils.compress(bytes);
            }
            write(bytes);
            int nBytes = bytes.length;
            dataset.tileSizes[tileNumber] = nBytes;
        } else {
            if (tileNumber > dataset.tilePositions.length) {
                System.out.println("Unexpected tile number: " + tileNumber + " (max of " + dataset.tilePositions.length + " expected).");
            }
        }
    }

    private void writeGroups() throws IOException {
        for (TDFGroup group : groupCache.values()) {
            long position = bytesWritten;
            BufferedByteWriter buffer = new BufferedByteWriter();
            group.write(buffer);
            write(buffer.getBytes());
            int nBytes = (int) (bytesWritten - position);
            groupIndex.put(group.getName(), new IndexEntry(position, nBytes));
        }
    }

    private void writeDatasets() throws IOException {
        for (TDFDataset dataset : datasetCache.values()) {
            long position = bytesWritten;
            BufferedByteWriter buffer = new BufferedByteWriter();
            dataset.write(buffer);
            write(buffer.getBytes());
            int nBytes = (int) (bytesWritten - position);
            datasetIndex.put(dataset.getName(), new IndexEntry(position, nBytes));
        }
    }

    private void writeIndex() throws IOException {
        BufferedByteWriter buffer = new BufferedByteWriter();
        buffer.putInt(datasetIndex.size());
        for (Map.Entry<String, IndexEntry> entry : datasetIndex.entrySet()) {
            buffer.putNullTerminatedString(entry.getKey());
            buffer.putLong(entry.getValue().position);
            buffer.putInt(entry.getValue().nBytes);
        }
        System.out.println("Group idx: " + groupIndex.size());
        buffer.putInt(groupIndex.size());
        for (Map.Entry<String, IndexEntry> entry : groupIndex.entrySet()) {
            buffer.putNullTerminatedString(entry.getKey());
            buffer.putLong(entry.getValue().position);
            buffer.putInt(entry.getValue().nBytes);
        }
        byte[] bytes = buffer.getBytes();
        write(bytes);
    }

    private byte[] bufferString(String str, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        Arrays.fill(buffer, (byte) ' ');
        buffer[bufferSize - 1] = 0;
        if (str != null) {
            int len = Math.min(bufferSize, str.length());
            System.arraycopy(str.getBytes(), 0, buffer, 0, len);
        }
        return buffer;
    }

    private void writeInt(int v) throws IOException {
        fos.write((v >>> 0) & 0xFF);
        fos.write((v >>> 8) & 0xFF);
        fos.write((v >>> 16) & 0xFF);
        fos.write((v >>> 24) & 0xFF);
        bytesWritten += 4;
    }

    private void write(byte[] bytes) throws IOException {
        fos.write(bytes);
        bytesWritten += bytes.length;
    }

    class IndexEntry {

        long position;

        int nBytes;

        public IndexEntry(long position, int nBytes) {
            this.position = position;
            this.nBytes = nBytes;
        }
    }
}
