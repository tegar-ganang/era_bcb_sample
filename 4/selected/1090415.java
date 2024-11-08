package org.jcvi.trace.sanger.phd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Properties;
import org.jcvi.common.util.Range;
import org.jcvi.datastore.DataStoreException;
import org.jcvi.glyph.nuc.NucleotideGlyph;
import org.jcvi.glyph.num.ShortGlyph;
import org.jcvi.glyph.phredQuality.PhredQuality;
import org.jcvi.io.IOUtil;
import org.jcvi.util.ByteBufferInputStream;
import org.jcvi.util.CloseableIterator;
import org.jcvi.util.DefaultIndexedFileRange;
import org.jcvi.util.IndexedFileRange;

/**
 * {@code IndexedPhdFileDataStore} is an implementation of 
 * {@link PhdDataStore} that only stores an index containing
 * file offsets to the various phd records contained
 * inside the phdball file.  This allows large files to provide random 
 * access without taking up much memory.  The downside is each phd
 * must be re-parsed each time.
 * @author dkatzel
 *
 *
 */
public class IndexedPhdFileDataStore extends AbstractPhdFileDataStore {

    private final IndexedFileRange recordLocations;

    private long currentStartOffset = 0;

    private long currentOffset = currentStartOffset;

    private boolean initialized = false;

    private final File phdBall;

    private int currentLineLength;

    public IndexedPhdFileDataStore(File phdBall) throws FileNotFoundException {
        this(phdBall, true);
    }

    public IndexedPhdFileDataStore(File phdBall, boolean parseNow) throws FileNotFoundException {
        this(phdBall, new DefaultIndexedFileRange(), parseNow);
    }

    /**
     * @param recordLocations
     * @throws FileNotFoundException 
     */
    public IndexedPhdFileDataStore(File phdBall, IndexedFileRange recordLocations) throws FileNotFoundException {
        this(phdBall, recordLocations, false);
    }

    public IndexedPhdFileDataStore(File phdBall, IndexedFileRange recordLocations, boolean parseNow) throws FileNotFoundException {
        this.recordLocations = recordLocations;
        this.phdBall = phdBall;
        if (parseNow) {
            PhdParser.parsePhd(phdBall, this);
        }
    }

    @Override
    public synchronized void visitLine(String line) {
        super.visitLine(line);
        currentLineLength = line.length();
        currentOffset += currentLineLength;
    }

    private void checkIfNotYetInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not yet initialized");
        }
    }

    @Override
    protected synchronized void visitPhd(String id, List<NucleotideGlyph> bases, List<PhredQuality> qualities, List<ShortGlyph> positions, Properties comments, List<PhdTag> tags) {
        long endOfOldRecord = currentOffset - currentLineLength - 1;
        recordLocations.put(id, Range.buildRange(currentStartOffset, endOfOldRecord));
        currentStartOffset = endOfOldRecord;
    }

    @Override
    public synchronized void visitEndOfFile() {
        super.visitEndOfFile();
        initialized = true;
    }

    @Override
    public boolean contains(String id) throws DataStoreException {
        checkIfNotYetInitialized();
        return recordLocations.contains(id);
    }

    @Override
    public Phd get(String id) throws DataStoreException {
        checkIfNotYetInitialized();
        FileChannel fastaFileChannel = null;
        DefaultPhdFileDataStore dataStore = null;
        InputStream in = null;
        FileInputStream fileInputStream = null;
        try {
            if (!recordLocations.contains(id)) {
                throw new DataStoreException(id + " does not exist");
            }
            Range range = recordLocations.getRangeFor(id);
            fileInputStream = new FileInputStream(phdBall);
            MappedByteBuffer buf = fileInputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, range.getStart(), range.size());
            in = new ByteBufferInputStream(buf);
            dataStore = new DefaultPhdFileDataStore();
            PhdParser.parsePhd(in, dataStore);
            return dataStore.get(id);
        } catch (IOException e) {
            throw new DataStoreException("error getting " + id, e);
        } finally {
            IOUtil.closeAndIgnoreErrors(fastaFileChannel);
            IOUtil.closeAndIgnoreErrors(dataStore);
            IOUtil.closeAndIgnoreErrors(in);
            IOUtil.closeAndIgnoreErrors(fileInputStream);
        }
    }

    @Override
    public CloseableIterator<String> getIds() throws DataStoreException {
        checkIfNotYetInitialized();
        return recordLocations.getIds();
    }

    @Override
    public int size() throws DataStoreException {
        checkIfNotYetInitialized();
        return recordLocations.size();
    }

    @Override
    public synchronized void close() throws IOException {
        recordLocations.close();
    }
}
