package org.jcvi.trace.frg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import org.jcvi.Distance;
import org.jcvi.Range;
import org.jcvi.datastore.DataStoreException;
import org.jcvi.glyph.EncodedGlyphs;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jcvi.glyph.phredQuality.PhredQuality;
import org.jcvi.sequence.Library;
import org.jcvi.sequence.MateOrientation;
import org.jcvi.util.MemoryMappedFileRange;

public class MemoryMappedFragmentDataStore extends AbstractFragmentDataStore {

    private final MemoryMappedFileRange fragmentInfomemoryMappedFileRange, mateInfoMemoryMappedFileRange;

    private final FileChannel fragFile;

    private final Frg2Parser parser;

    private int currentStart = 0;

    private int currentPosition = -1;

    public MemoryMappedFragmentDataStore(File file, MemoryMappedFileRange fragmentInfomemoryMappedFileRange, MemoryMappedFileRange mateInfoMemoryMappedFileRange, Frg2Parser parser) throws FileNotFoundException {
        this.fragmentInfomemoryMappedFileRange = fragmentInfomemoryMappedFileRange;
        this.mateInfoMemoryMappedFileRange = mateInfoMemoryMappedFileRange;
        this.parser = parser;
        this.fragFile = new RandomAccessFile(file, "r").getChannel();
    }

    @Override
    public void visitFragment(FrgVisitorAction action, String fragmentId, String libraryId, NucleotideEncodedGlyphs bases, EncodedGlyphs<PhredQuality> qualities, Range validRange, Range vectorClearRange, String source) {
        throwErrorIfAlreadyInitialized();
        if (this.isAddOrModify(action)) {
            Range fragmentRange = Range.buildRange(currentStart, currentPosition);
            fragmentInfomemoryMappedFileRange.put(fragmentId, fragmentRange);
            updateRangeStartPosition();
        } else if (this.isDelete(action)) {
            fragmentInfomemoryMappedFileRange.remove(fragmentId);
        }
    }

    @Override
    public void visitLibrary(FrgVisitorAction action, String id, MateOrientation orientation, Distance distance) {
        super.visitLibrary(action, id, orientation, distance);
        updateRangeStartPosition();
    }

    private void updateRangeStartPosition() {
        currentStart = currentPosition + 1;
    }

    @Override
    public void visitLine(String line) {
        throwErrorIfAlreadyInitialized();
        currentPosition += line.length();
    }

    @Override
    public boolean contains(String fragmentId) throws DataStoreException {
        throwErrorIfClosed();
        return fragmentInfomemoryMappedFileRange.contains(fragmentId);
    }

    @Override
    public Fragment get(String id) throws DataStoreException {
        throwErrorIfClosed();
        Range range = fragmentInfomemoryMappedFileRange.getRangeFor(id);
        InputStream in = getInputStreamFor(range);
        final SingleFragVisitor singleFragVisitor = new SingleFragVisitor();
        parser.parse(in, singleFragVisitor);
        return singleFragVisitor.getFragmentToReturn();
    }

    private InputStream getInputStreamFor(Range range) throws DataStoreException {
        ByteBuffer buffer;
        try {
            buffer = fragFile.map(FileChannel.MapMode.READ_ONLY, range.getStart(), range.size());
        } catch (IOException e) {
            throw new DataStoreException("error memory mapping file ", e);
        }
        byte[] array = new byte[(int) range.size()];
        buffer.get(array);
        InputStream in = new ByteArrayInputStream(array);
        return in;
    }

    @Override
    public Iterator<String> getIds() {
        throwErrorIfClosed();
        return fragmentInfomemoryMappedFileRange.getIds();
    }

    @Override
    public int size() throws DataStoreException {
        throwErrorIfClosed();
        return fragmentInfomemoryMappedFileRange.size();
    }

    @Override
    public void close() throws IOException {
        super.close();
        fragmentInfomemoryMappedFileRange.close();
        fragFile.close();
    }

    @Override
    public Fragment getMateOf(Fragment fragment) throws DataStoreException {
        throwErrorIfClosed();
        final String fragId = fragment.getId();
        String mateId = getMateIdOf(fragId);
        return get(mateId);
    }

    @Override
    public boolean hasMate(Fragment fragment) throws DataStoreException {
        throwErrorIfClosed();
        final String fragId = fragment.getId();
        return getMateIdOf(fragId) != null;
    }

    private String getMateIdOf(final String fragId) throws DataStoreException {
        Range range = mateInfoMemoryMappedFileRange.getRangeFor(fragId);
        InputStream in = getInputStreamFor(range);
        SingleLinkVisitor singleLinkVisitor = new SingleLinkVisitor(fragId);
        parser.parse(in, singleLinkVisitor);
        return singleLinkVisitor.getMateId();
    }

    @Override
    public void visitLink(FrgVisitorAction action, List<String> fragIds) {
        throwErrorIfAlreadyInitialized();
        if (this.isAddOrModify(action)) {
            Range fragmentRange = Range.buildRange(currentStart, currentPosition);
            for (String fragmentId : fragIds) {
                mateInfoMemoryMappedFileRange.put(fragmentId, fragmentRange);
            }
            updateRangeStartPosition();
        } else if (this.isDelete(action)) {
            for (String fragmentId : fragIds) {
                mateInfoMemoryMappedFileRange.remove(fragmentId);
            }
        }
    }

    private class SingleFragVisitor implements Frg2Visitor {

        Fragment fragmentToReturn = null;

        public Fragment getFragmentToReturn() {
            return fragmentToReturn;
        }

        @Override
        public void visitFragment(FrgVisitorAction action, String fragmentId, String libraryId, NucleotideEncodedGlyphs bases, EncodedGlyphs<PhredQuality> qualities, Range validRange, Range vectorClearRange, String source) {
            Library library;
            try {
                library = getLibrary(libraryId);
            } catch (DataStoreException e) {
                throw new IllegalStateException("Fragment uses library " + libraryId + "before it is declared", e);
            }
            fragmentToReturn = new DefaultFragment(fragmentId, bases, qualities, validRange, vectorClearRange, library, source);
        }

        @Override
        public void visitLibrary(FrgVisitorAction action, String id, MateOrientation orientation, Distance distance) {
        }

        @Override
        public void visitLink(FrgVisitorAction action, List<String> fragIds) {
        }

        @Override
        public void visitEndOfFile() {
        }

        @Override
        public void visitLine(String line) {
        }

        @Override
        public void visitFile() {
        }
    }

    private static class SingleLinkVisitor implements Frg2Visitor {

        private String mateId = null;

        private String fragmentIdToGetMateOf;

        public SingleLinkVisitor(String fragmentIdToGetMateOf) {
            this.fragmentIdToGetMateOf = fragmentIdToGetMateOf;
        }

        public String getMateId() {
            return mateId;
        }

        @Override
        public void visitFragment(FrgVisitorAction action, String fragmentId, String libraryId, NucleotideEncodedGlyphs bases, EncodedGlyphs<PhredQuality> qualities, Range validRange, Range vectorClearRange, String source) {
        }

        @Override
        public void visitLibrary(FrgVisitorAction action, String id, MateOrientation orientation, Distance distance) {
        }

        @Override
        public void visitLink(FrgVisitorAction action, List<String> fragIds) {
            for (String fragId : fragIds) {
                if (!fragId.equals(fragmentIdToGetMateOf)) {
                    mateId = fragId;
                }
            }
        }

        @Override
        public void visitEndOfFile() {
        }

        @Override
        public void visitLine(String line) {
        }

        @Override
        public void visitFile() {
        }
    }
}
