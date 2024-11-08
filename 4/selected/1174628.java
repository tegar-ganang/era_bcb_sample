package com.ibm.tuningfork.infra.feed;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import com.ibm.tuningfork.infra.Logging;
import com.ibm.tuningfork.infra.chunk.Chunk;
import com.ibm.tuningfork.infra.chunk.ChunkDescriptor;
import com.ibm.tuningfork.infra.chunk.ChunkProcessor;
import com.ibm.tuningfork.infra.chunk.ChunkProcessorRegistry;
import com.ibm.tuningfork.infra.sharing.ISharingConvertibleCallback;
import com.ibm.tuningfork.infra.util.FileUtility;
import com.ibm.tuningfork.infra.util.IProgressTracker;

/**
 * A feed whose backing TraceSource is a FiniteTraceSource.
 * For example, feeds that are backed by trace files.
 */
public final class FiniteFeed extends Feed {

    protected double progress = 0.0;

    protected boolean closed = false;

    protected final IProgressTracker progressTracker;

    protected long crc = 0;

    public FiniteFeed(FiniteTraceSource source) throws IOException {
        this(source, null, null);
    }

    public FiniteFeed(FiniteTraceSource source, FeedGroup group) throws IOException {
        this(source, group, null);
    }

    public FiniteFeed(FiniteTraceSource source, FeedGroup group, IProgressTracker tracker) throws IOException {
        super(source);
        progressTracker = tracker;
        source.setFeed(this);
        if (group != null) {
            group.addFeed(this);
        }
        source.open();
    }

    @Override
    public void collectSpecificReconstructionArguments(ISharingConvertibleCallback cb) throws Exception {
        cb.convert(source);
    }

    public final boolean isFinite() {
        return true;
    }

    public final void end() {
        feedletManager.end();
        closed = true;
    }

    public final boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isFromSource(TraceSource otherSource) {
        if (otherSource instanceof FiniteTraceSource) {
            try {
                long myCRC = ((FiniteTraceSource) source).sampledCRC();
                long otherCRC = ((FiniteTraceSource) otherSource).sampledCRC();
                return myCRC == otherCRC;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public final String getModeString() {
        String result = super.getModeString();
        switch(getMode()) {
            case CHUNK_INDEXING:
            case EVENT_INDEXING:
            case READING_INDEX:
                result += "(" + ((int) (100.0 * progress)) + "%)";
        }
        return result;
    }

    public double getAverageEventLength() {
        return 24.0;
    }

    public long getEstimatedEvents() {
        if (isClosed()) {
            return numberOfEvents();
        } else {
            return (long) (source.getEstimatedLength() / getAverageEventLength());
        }
    }

    private void updateProgress(double newProgress) {
        progress = newProgress;
        if (progressTracker != null) {
            double eventIndexingToChunkIndexingRatio = 4.0;
            double overallProgress = progress;
            if (getMode() == Mode.CHUNK_INDEXING) {
                overallProgress = progress / eventIndexingToChunkIndexingRatio;
            } else if (getMode() == Mode.EVENT_INDEXING) {
                overallProgress = ((1 + (eventIndexingToChunkIndexingRatio - 1) * progress)) / eventIndexingToChunkIndexingRatio;
            }
            progressTracker.update(overallProgress);
        }
    }

    public void addedMoreEvents() {
        try {
            long pos = source.position();
            updateProgress(((double) pos) / ((double) source.getEstimatedLength()));
        } catch (IOException ioe) {
        }
    }

    /**
     * Create an index from this trace source.
     *
     * @throws FeedFormatException
     * @throws IOException
     * @throws EOFException
     */
    public synchronized void createIndex() throws IOException, FeedFormatException {
        Chunk dataChunk = new Chunk(FeedConstants.MAX_CHUNK_BODY_SIZE);
        final ChunkProcessorRegistry chunkTypeRegistry = getChunkTypeRegistry();
        long startTimeMs = System.currentTimeMillis();
        setMode(Feed.Mode.CHUNK_INDEXING);
        try {
            while (true) {
                int curChunkIndex = feedChunkIndex.getMaxChunkIndex() + 1;
                ChunkDescriptor descriptor = source.readChunkHeader(curChunkIndex);
                if (Logging.verbose(2)) {
                    Logging.errorln("Chunk indexing chunk " + curChunkIndex + "  descriptor = " + descriptor.toString());
                }
                source.readChunkBody(dataChunk, descriptor.getBodyLength());
                long chunkEndPosition = source.position();
                addedMoreEvents();
                dataChunk.setIndex(descriptor.getIndex());
                final ChunkProcessor processor = chunkTypeRegistry.getChunkProcessor(descriptor.getTypeId());
                descriptor = processor.process(descriptor, dataChunk);
                feedChunkIndex.addChunkDescriptor(descriptor);
                source.seek(chunkEndPosition);
            }
        } catch (EOFException e) {
        }
        long endIndexTimeMs = System.currentTimeMillis();
        end();
        Logging.msgln("Chunk indexing of " + (1 + feedChunkIndex.getMaxChunkIndex()) + " chunks took " + (((endIndexTimeMs - startTimeMs) / 100) / 10.0) + " seconds");
        setMode(Mode.EVENT_INDEXING);
        generateTailEvents();
        long endPrefetchTimeMs = System.currentTimeMillis();
        if (numberOfEvents() == 0) {
            throw new IllegalStateException(getDisplayName() + " has no events");
        }
        Logging.msgln("Event indexing of " + (numberOfEvents()) + " events took " + (((endPrefetchTimeMs - endIndexTimeMs) / 100) / 10.0) + " seconds");
    }

    public long getCanonicalId() {
        if (crc == 0) {
            Logging.errorln("FiniteSeekableFeed.getCanonicalId: crc = 0");
        }
        return crc;
    }

    private File getIndexFile() {
        if (!FileUtility.cacheAvailable()) {
            return null;
        }
        try {
            setMode(Mode.CRCING);
            FiniteTraceSource finiteSource = (FiniteTraceSource) source;
            crc = finiteSource.sampledCRC();
            return FileUtility.getCacheFile(crc + ".index");
        } catch (IOException e) {
            Logging.errorln("Error in getIndexFile: " + e.toString());
        }
        return null;
    }

    public synchronized boolean readIndex() throws IOException, FeedFormatException {
        File f = getIndexFile();
        if (f == null || (!f.exists()) || (!f.canRead())) {
            return false;
        }
        setMode(Mode.READING_INDEX);
        FileInputStream in = new FileInputStream(f);
        FileChannel fc = in.getChannel();
        ObjectInputStream objIn = new ObjectInputStream(in);
        int count = 0;
        while (readIndexEntry(objIn)) {
            if ((count--) < 0) {
                updateProgress(((double) fc.position()) / fc.size());
                count = 10;
            }
        }
        end();
        setMode(Mode.READY);
        return true;
    }

    public boolean writeIndex() {
        File f = getIndexFile();
        if (f == null) {
            setMode(Mode.READY);
            return false;
        }
        setMode(Mode.WRITING_INDEX);
        f.delete();
        try {
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            writeIndexEntryTimeRangeAndEventCount(objOut);
            for (int i = 0; i <= feedChunkIndex.getMaxChunkIndex(); i++) {
                writeIndexEntry(objOut, feedChunkIndex.getChunkDescriptor(i));
            }
            Feedlet[] feedlets = feedletManager.getFeedlets();
            for (int i = 0; i < feedlets.length; i++) {
                FeedletIndex index = feedlets[i].getEventIndex();
                for (int j = 0; j < index.numEntries(); j++) {
                    writeIndexEntry(objOut, index.getEntry(j));
                }
            }
            for (int i = 0; i < feedSeekIndex.numFeedPositions(); i++) {
                writeIndexEntry(objOut, feedSeekIndex.getFeedPosition(i));
            }
            setMode(Mode.READY);
            return true;
        } catch (IOException ioe) {
            Logging.errorln("Could not create index file " + f + " due to " + ioe.toString());
        }
        return false;
    }
}
