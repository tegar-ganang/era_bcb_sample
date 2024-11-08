package de.carne.fs.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import de.carne.io.Closeables;
import de.carne.io.Files;
import de.carne.nio.Buffers;
import de.carne.nio.compression.Decoder;
import de.carne.util.logging.Log;

/**
 * Class used to decode encoded data to a temporary file and make it available for scanning.
 */
final class DecodeCache implements Closeable {

    private static final Log LOG = new Log(DecodeCache.class);

    private static final class CacheFile implements Closeable {

        private File fileName = null;

        private RandomAccessFile file = null;

        /**
		 * Construct <code>CacheFile</code>.
		 * 
		 * @param fileName The cache file name.
		 * @param file The cache file.
		 */
        public CacheFile(File fileName, RandomAccessFile file) {
            this.fileName = fileName;
            this.file = file;
        }

        /**
		 * Get the associated file channel.
		 * 
		 * @return The associated file channel.
		 */
        public FileChannel getCache() {
            return this.file.getChannel();
        }

        @Override
        public void close() throws IOException {
            this.file = Closeables.saveClose(this.file);
            this.fileName = Files.saveDelete(this.fileName);
        }
    }

    /**
	 * Class providing read access to the a decoded data blocks backed up by some other data store. This class defines
	 * the access methods for the data block and provides the functionality to perform access parameter mapping.
	 */
    public abstract static class MapEntry {

        private final long start;

        private final long end;

        /**
		 * Construct <code>Entry</code>.
		 * 
		 * @param start The start position within the underlying data store.
		 * @param end The end position within the underlying data store.
		 */
        protected MapEntry(long start, long end) {
            assert 0 <= start;
            assert start <= end;
            this.start = start;
            this.end = end;
        }

        /**
		 * Get entry size.
		 * 
		 * @return The entry size.
		 */
        public long size() {
            return this.end - this.start;
        }

        /**
		 * Read byte data beginning at a specific position.
		 * 
		 * @param position The position to start reading.
		 * @param buffer The buffer receiving the read data.
		 * @return The number of bytes read.
		 * @throws IOException If an I/O error occurs.
		 */
        public abstract int readAt(long position, ByteBuffer buffer) throws IOException;

        /**
		 * Transfer a number of bytes from a specific position to a <code>WritableByteChannel</code>.
		 * 
		 * @param position The position to start reading data from.
		 * @param count The maximum number of bytes to read.
		 * @param target The target of the transfer operation.
		 * @return The number of bytes transferred.
		 * @throws IOException If an I/O error occurs.
		 */
        public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;

        /**
		 * Limit buffer to this block's size.
		 * 
		 * @param buffer The buffer to limit.
		 * @param position The position the read will start.
		 * @return The previously set buffer limit.
		 */
        protected int limitBuffer(ByteBuffer buffer, long position) {
            final long positionLimit = Math.max(0, this.end - (this.start + position));
            return Buffers.limit(buffer, (int) Math.min(positionLimit, buffer.remaining()));
        }

        /**
		 * Map a position from the underlying data store to this block's extent.
		 * 
		 * @param position The position to map.
		 * @return The mapped position.
		 */
        protected long mapPosition(long position) {
            return this.start + position;
        }
    }

    /**
	 * <code>Entry</code> implementation backed up by the decode cache file.
	 */
    private static class CacheMapEntry extends MapEntry {

        private final FileChannel cache;

        CacheMapEntry(FileChannel cache, long start, long end) {
            super(start, end);
            this.cache = cache;
        }

        @Override
        public int readAt(long position, ByteBuffer buffer) throws IOException {
            final int startBufferPosition = buffer.position();
            final int oldLimit = limitBuffer(buffer, position);
            try {
                this.cache.read(buffer, mapPosition(position));
            } finally {
                buffer.limit(oldLimit);
            }
            return buffer.position() - startBufferPosition;
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            return this.cache.transferTo(mapPosition(position), count, target);
        }
    }

    /**
	 * <code>Entry</code> implementation backed up by a <code>FileScannerInput</code>.
	 */
    private static class InputMapEntry extends MapEntry {

        private final FileScannerInput input;

        InputMapEntry(FileScannerInput input, long start, long end) {
            super(start, end);
            this.input = input;
        }

        @Override
        public int readAt(long position, ByteBuffer buffer) throws IOException {
            final int startBufferPosition = buffer.position();
            final int oldLimit = limitBuffer(buffer, position);
            try {
                this.input.readAt(mapPosition(position), buffer);
            } finally {
                buffer.limit(oldLimit);
            }
            return buffer.position() - startBufferPosition;
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            return this.input.transferTo(mapPosition(position), count, target);
        }
    }

    /**
	 * Helper class holding/providing contextual information/data during data decoding.
	 */
    private class DecoderContext implements ReadableByteChannel {

        private final Decoder decoder;

        private final ReadableByteChannel src;

        private IOException lastIOError = null;

        public DecoderContext(Decoder decoder, FileScannerInput input, long start) {
            assert decoder != null;
            this.decoder = decoder;
            this.src = input.readableByteChannel(start);
        }

        public DecoderContext(Decoder decoder, FileScannerInput input, long start, long end) {
            assert decoder != null;
            this.decoder = decoder;
            this.src = input.readableByteChannel(start, end);
        }

        public void validate() {
            try {
                this.decoder.validateDecoded();
            } catch (final IOException e) {
                this.lastIOError = e;
            }
        }

        public IOException lastIOError() {
            return this.lastIOError;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            final long readStart = this.decoder.totalOut();
            int read;
            try {
                int decoded;
                do {
                    decoded = this.decoder.decode(dst, this.src);
                    read = (int) (this.decoder.totalOut() - readStart);
                } while (decoded >= 0 && read == 0);
            } catch (final IOException e) {
                this.lastIOError = e;
                throw e;
            }
            return (read > 0 ? read : -1);
        }

        @Override
        public void close() throws IOException {
            this.src.close();
        }

        @Override
        public boolean isOpen() {
            return this.src.isOpen();
        }
    }

    private final HashMap<CacheFile, Boolean> cacheFiles = new HashMap<CacheFile, Boolean>();

    private CacheFile acquireCacheFile() throws IOException {
        CacheFile cacheFile = null;
        synchronized (this.cacheFiles) {
            for (final Map.Entry<CacheFile, Boolean> cacheFileEntry : this.cacheFiles.entrySet()) {
                if (cacheFileEntry.getValue().equals(Boolean.FALSE)) {
                    cacheFile = cacheFileEntry.getKey();
                    break;
                }
            }
            if (cacheFile == null) {
                final File fileName = File.createTempFile("fs-decode-cache-", null);
                final RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                cacheFile = new CacheFile(fileName, file);
            }
            this.cacheFiles.put(cacheFile, Boolean.TRUE);
        }
        return cacheFile;
    }

    private void releaseCache(CacheFile cacheFile) {
        synchronized (this.cacheFiles) {
            this.cacheFiles.put(cacheFile, Boolean.FALSE);
        }
    }

    @Override
    public void close() throws IOException {
        for (final CacheFile cacheFile : this.cacheFiles.keySet()) {
            Closeables.saveClose(cacheFile);
        }
    }

    private void logIOException(String inputName, IOException e) {
        LOG.warning(e, "An I/O error occured while decoding data for input ''{0}''", inputName);
    }

    /**
	 * Map a data section from a <code>FileScannerInput</code> and make it accessible via a
	 * <code>FileScannerInput</code>
	 * 
	 * @param parent The <code>FileScannerInput</code> containing the data to map.
	 * @param start The start position of the data section to map.
	 * @param end The end position of the data section to map.
	 * @param inputName The name to use for the new <code>FileScannerInput</code>.
	 * @return The <code>DecodeResult</code> object providing access to the decoded data.
	 */
    public DecodeResult map(FileScannerInput parent, long start, long end, String inputName) {
        return new DecodeResult(new DecodedFileScannerInput(parent, inputName, new InputMapEntry(parent, start, end), null), end);
    }

    /**
	 * Decode data from a <code>FileScannerInput</code> and make it accessible via a <code>FileScannerInput</code>
	 * 
	 * @param parentInput The <code>FileScannerInput</code> containing the encoded data.
	 * @param position The position to start decoding at.
	 * @param decoder The <code>Decoder</code> used to perform the decoding.
	 * @param inputName The name to use for the new <code>FileScannerInput</code>.
	 * @return The <code>DecodeResult</code> object providing access to the decoded data.
	 * @throws IOException If an I/O occurs outside the decoding step.
	 */
    public DecodeResult decode(FileScannerInput parentInput, long position, Decoder decoder, String inputName) throws IOException {
        final CacheFile cacheFile = acquireCacheFile();
        CacheMapEntry cacheEntry = null;
        IOException lastIOError = null;
        try {
            final FileChannel cache = cacheFile.getCache();
            final DecoderContext decoderContext = new DecoderContext(decoder, parentInput, position, parentInput.size());
            final long entryStart = cache.size();
            long entryEnd = entryStart;
            entryEnd += cache.transferFrom(decoderContext, entryStart, Long.MAX_VALUE);
            cacheEntry = new CacheMapEntry(cache, entryStart, entryEnd);
            decoderContext.validate();
            lastIOError = decoderContext.lastIOError();
        } catch (final IOException e) {
            lastIOError = e;
        } finally {
            releaseCache(cacheFile);
        }
        if (cacheEntry == null || lastIOError instanceof ClosedChannelException) {
            throw lastIOError;
        }
        if (lastIOError != null) {
            logIOException(inputName, lastIOError);
        }
        final FileScannerInput decodedInput = new DecodedFileScannerInput(parentInput, inputName, cacheEntry, lastIOError);
        return new DecodeResult(decodedInput, position + decoder.totalIn());
    }

    /**
	 * Map and decode multiple data sections from a <code>FileScannerInput</code> and make it accessible via a
	 * <code>FileScannerInput</code>.
	 * 
	 * @param parentInput The <code>FileScannerInput</code> containing the data to map.
	 * @param map Map array defining the ranges to map as well as the decoder to use accessing the data.
	 * @param inputName The name to use for the new <code>FileScannerInput</code>.
	 * @return The <code>DecodeResult</code> object providing access to the decoded data.
	 * @throws IOException If an I/O occurs outside the decoding step.
	 */
    public DecodeResult decodeMap(FileScannerInput parentInput, DecodeMap map, String inputName) throws IOException {
        final CacheFile cacheFile = acquireCacheFile();
        final ArrayList<MapEntry> cacheEntries = new ArrayList<MapEntry>(map.getEntryCount());
        IOException lastIOError = null;
        long decodeExtent = 0;
        try {
            final FileChannel cache = cacheFile.getCache();
            final DecodeMap.Entry[] mapEntries = map.getEntries();
            for (final DecodeMap.Entry mapEntry : mapEntries) {
                final long mapEntryStart = mapEntry.getStart();
                final long mapEntryEnd = mapEntry.getEnd();
                final Decoder decoder = mapEntry.getDecoder();
                if (decoder != null) {
                    final long decoderTotalInStart = decoder.totalIn();
                    DecoderContext decoderContext;
                    if (mapEntryEnd > mapEntryStart) {
                        decoderContext = new DecoderContext(decoder, parentInput, mapEntryStart, mapEntryEnd);
                    } else {
                        decoderContext = new DecoderContext(decoder, parentInput, mapEntryStart);
                    }
                    final long entryStart = cache.size();
                    final long entryEnd = entryStart + cache.transferFrom(decoderContext, entryStart, Long.MAX_VALUE);
                    cacheEntries.add(new CacheMapEntry(cache, entryStart, entryEnd));
                    decoderContext.validate();
                    lastIOError = decoderContext.lastIOError();
                    final long decoderIn = decoder.totalIn() - decoderTotalInStart;
                    if (mapEntryStart < mapEntryEnd) {
                        decodeExtent = Math.max(decodeExtent, mapEntryEnd + mapEntry.getPadding());
                    } else {
                        decodeExtent = Math.max(decodeExtent, mapEntryStart + decoderIn);
                    }
                } else {
                    cacheEntries.add(new InputMapEntry(parentInput, mapEntryStart, mapEntryEnd));
                    decodeExtent = Math.max(decodeExtent, mapEntryEnd + mapEntry.getPadding());
                }
                if (lastIOError != null) {
                    throw lastIOError;
                }
            }
        } catch (final IOException e) {
            lastIOError = e;
        } finally {
            releaseCache(cacheFile);
        }
        if (cacheEntries.size() == 0 || lastIOError instanceof ClosedChannelException) {
            throw lastIOError;
        }
        if (lastIOError != null) {
            logIOException(inputName, lastIOError);
        }
        final FileScannerInput decodedInput = new DecodeMappedFileScannerInput(parentInput, inputName, cacheEntries, lastIOError);
        return new DecodeResult(decodedInput, decodeExtent);
    }
}
