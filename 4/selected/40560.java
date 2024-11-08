package de.carne.fs.swt.widgets;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import de.carne.fs.core.transfer.TextData;
import de.carne.fs.core.transfer.TextDataBuffer;
import de.carne.fs.core.transfer.TextDataHandler;
import de.carne.fs.core.transfer.TextDataReader;
import de.carne.fs.core.transfer.TextDataType;
import de.carne.fs.provider.util.ExceptionHandler;
import de.carne.io.Closeables;
import de.carne.io.Files;
import de.carne.io.IncompleteReadException;
import de.carne.io.IncompleteWriteException;

/**
 * Helper class used by <code>TextDataRenderer</code> to make streamed <code>TextData</code> seekable.
 */
final class TextDataCache implements Closeable {

    private static final int CACHE_PAGE_SIZE = 1 << 10;

    private final TextDataReader reader;

    private boolean readerHasMoreLines;

    private int nextReaderLine = 0;

    private File fileCacheName = null;

    private RandomAccessFile fileCache = null;

    private int fileCacheLineCount = 0;

    private final TreeMap<Integer, Long> fileCacheIndex = new TreeMap<Integer, Long>();

    private final List<TextData[]> liveCache = new ArrayList<TextData[]>(CACHE_PAGE_SIZE);

    private int liveCacheLineStart = 0;

    private int liveCacheLineEnd = 0;

    public TextDataCache(TextDataHandler handler) {
        this.reader = handler.reader();
        this.readerHasMoreLines = this.reader.hasMoreLines();
    }

    public int getLineCount() {
        return this.nextReaderLine;
    }

    public int getMaxLineCount() {
        return (this.readerHasMoreLines ? -1 : this.nextReaderLine);
    }

    public boolean canGetLine(int line) {
        return 0 <= line && line <= this.nextReaderLine && (line < this.nextReaderLine || this.readerHasMoreLines);
    }

    public TextData[] getLine(int line) {
        if (!canGetLine(line)) {
            throw new NoSuchElementException("Invalid line: " + line);
        }
        if (line < this.liveCacheLineStart || this.liveCacheLineEnd <= line) {
            try {
                flushLiveCache();
                if (line >= this.fileCacheLineCount) {
                    feedLiveCacheFromReader();
                } else {
                    feedLiveCacheFromFileCache(line);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return this.liveCache.get(line - this.liveCacheLineStart);
    }

    private void feedLiveCacheFromReader() {
        this.liveCache.clear();
        this.liveCacheLineStart = this.liveCacheLineEnd = this.nextReaderLine;
        final TextDataBuffer buffer = new TextDataBuffer();
        while (this.readerHasMoreLines && this.liveCache.size() < CACHE_PAGE_SIZE) {
            buffer.clear();
            try {
                this.reader.readLines(buffer);
                this.readerHasMoreLines = this.reader.hasMoreLines();
            } catch (final IOException e) {
                ExceptionHandler.printException(buffer, e);
                this.readerHasMoreLines = false;
            }
            this.nextReaderLine += buffer.linesCount();
            final TextData[][] lines = buffer.toLines();
            for (final TextData[] line : lines) {
                this.liveCache.add(line);
            }
        }
        this.liveCacheLineEnd = this.nextReaderLine;
    }

    private void flushLiveCache() throws IOException {
        if (this.fileCacheLineCount < this.liveCacheLineEnd) {
            if (this.fileCacheName == null) {
                this.fileCacheName = File.createTempFile("fs-text-data-cache-", null);
                this.fileCache = new RandomAccessFile(this.fileCacheName, "rw");
            }
            final FileChannel fileCacheChannel = this.fileCache.getChannel();
            final long writePosition = fileCacheChannel.size();
            this.fileCacheIndex.put(this.fileCacheLineCount, writePosition);
            final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            final ByteBuffer typeBuffer = ByteBuffer.allocate(1);
            lengthBuffer.putInt(0, this.liveCache.size());
            safeWrite(fileCacheChannel, lengthBuffer);
            for (final TextData[] line : this.liveCache) {
                lengthBuffer.clear();
                lengthBuffer.putInt(0, line.length);
                safeWrite(fileCacheChannel, lengthBuffer);
                for (final TextData element : line) {
                    final ByteBuffer textBuffer = ByteBuffer.wrap(element.text().getBytes());
                    lengthBuffer.clear();
                    lengthBuffer.putInt(0, textBuffer.capacity());
                    safeWrite(fileCacheChannel, lengthBuffer);
                    safeWrite(fileCacheChannel, textBuffer);
                    typeBuffer.clear();
                    switch(element.type()) {
                        case VALUE:
                            typeBuffer.put(0, (byte) 0);
                            break;
                        case SYMBOL:
                            typeBuffer.put(0, (byte) 1);
                            break;
                        case KEYWORD:
                            typeBuffer.put(0, (byte) 2);
                            break;
                        case OPERATOR:
                            typeBuffer.put(0, (byte) 3);
                            break;
                        case LABEL:
                            typeBuffer.put(0, (byte) 4);
                            break;
                        case COMMENT:
                            typeBuffer.put(0, (byte) 5);
                            break;
                        case ERROR:
                            typeBuffer.put(0, (byte) 6);
                            break;
                    }
                    safeWrite(fileCacheChannel, typeBuffer);
                }
            }
            this.fileCacheLineCount += this.liveCache.size();
        }
    }

    private void feedLiveCacheFromFileCache(int line) throws IOException {
        this.liveCache.clear();
        final Map.Entry<Integer, Long> page0Entry = this.fileCacheIndex.floorEntry(line);
        this.liveCacheLineStart = this.liveCacheLineEnd = page0Entry.getKey();
        final FileChannel fileCacheChannel = this.fileCache.getChannel();
        final Map.Entry<Integer, Long> page1Entry = this.fileCacheIndex.higherEntry(page0Entry.getKey());
        final long page0Start = page0Entry.getValue();
        final long page0End = (page1Entry != null ? page1Entry.getValue() : fileCacheChannel.size());
        final ByteBuffer readBuffer = ByteBuffer.allocate((int) (page0End - page0Start));
        safeRead(fileCacheChannel, page0Start, readBuffer);
        readBuffer.flip();
        final TextDataBuffer lineBuffer = new TextDataBuffer();
        final int liveCacheSize = readBuffer.getInt();
        for (int lineIndex = 0; lineIndex < liveCacheSize; lineIndex++) {
            lineBuffer.clear();
            final int lineSize = readBuffer.getInt();
            for (int elementIndex = 0; elementIndex < lineSize; elementIndex++) {
                final int textSize = readBuffer.getInt();
                final byte[] textBytes = new byte[textSize];
                readBuffer.get(textBytes);
                final String text = new String(textBytes);
                final byte typeValue = readBuffer.get();
                TextDataType type;
                switch(typeValue) {
                    case 0:
                        type = TextDataType.VALUE;
                        break;
                    case 1:
                        type = TextDataType.SYMBOL;
                        break;
                    case 2:
                        type = TextDataType.KEYWORD;
                        break;
                    case 3:
                        type = TextDataType.OPERATOR;
                        break;
                    case 4:
                        type = TextDataType.LABEL;
                        break;
                    case 5:
                        type = TextDataType.COMMENT;
                        break;
                    case 6:
                        type = TextDataType.ERROR;
                        break;
                    default:
                        throw new RuntimeException("Unexpected text data type: " + typeValue);
                }
                lineBuffer.append(text, type);
            }
            this.liveCache.add(lineBuffer.toLine());
        }
        this.liveCacheLineEnd = this.liveCacheLineStart + this.liveCache.size();
    }

    private void safeWrite(FileChannel fileChannel, ByteBuffer buffer) throws IOException {
        final int requested = buffer.remaining();
        final int written = fileChannel.write(buffer);
        if (written < requested) {
            throw new IncompleteWriteException(requested, written);
        }
    }

    private void safeRead(FileChannel fileChannel, long position, ByteBuffer buffer) throws IOException {
        final int requested = buffer.remaining();
        final int read = fileChannel.read(buffer, position);
        if (read < requested) {
            throw new IncompleteReadException(requested, read);
        }
    }

    @Override
    public void close() throws IOException {
        this.fileCache = Closeables.saveClose(this.fileCache);
        this.fileCacheName = Files.saveDelete(this.fileCacheName);
    }
}
