package websphinx;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;

public class HTMLTransformer {

    private OutputStream stream;

    private Writer writer;

    private boolean openedStream = false;

    private RandomAccessFile readwrite;

    private HTMLTransformer next;

    private HTMLTransformer head;

    private HTMLTransformer tail;

    private String content;

    private int emitStart, emitEnd;

    private int transformEnd;

    /**
     * Make an HTMLTransformer that writes pages to a
     * stream.
     * @param out Stream to receive HTML output
     */
    public HTMLTransformer(OutputStream out) {
        head = tail = this;
        next = null;
        setOutput(out);
    }

    /**
     * Make an HTMLTransformer that writes pages to a
     * file.
     * @param filename Name of file to receive HTML output
     * @exception IOException if file cannot be opened
     */
    public HTMLTransformer(String filename) throws IOException {
        head = tail = this;
        next = null;
        openFile(filename, false);
    }

    /**
     * Make an HTMLTransformer that writes pages to a
     * file.
     * @param filename Name of file to receive HTML output
     * @param seekable True if file should be opened for random access
     */
    public HTMLTransformer(String filename, boolean seekable) throws IOException {
        head = tail = this;
        next = null;
        openFile(filename, seekable);
    }

    /**
     * Make an HTMLTransformer that writes pages to a
     * downstream HTMLTransformer.  Use this constructor
     * to chain together several HTMLTransformers.
     * @param next HTMLTransformer to receive HTML output
     */
    public HTMLTransformer(HTMLTransformer next) {
        this.next = next;
        tail = next != null ? next.tail : this;
        for (HTMLTransformer u = this; u != null; u = u.next) u.head = this;
    }

    private void openFile(String filename, boolean seekable) throws IOException {
        File file = new File(filename);
        OutputStream out = Access.getAccess().writeFile(file, false);
        if (!seekable) setOutput(out); else {
            out.close();
            RandomAccessFile raf = Access.getAccess().readWriteFile(file);
            setRandomAccessFile(raf);
        }
        openedStream = true;
    }

    public void setOutput(OutputStream out) {
        if (next == null) {
            stream = out;
            writer = new OutputStreamWriter(out);
        } else next.setOutput(out);
    }

    public OutputStream getOutputStream() {
        return tail.stream;
    }

    public Writer getOutputWriter() {
        return tail.writer;
    }

    public void setRandomAccessFile(RandomAccessFile raf) {
        if (next == null) readwrite = raf; else next.setRandomAccessFile(raf);
    }

    public RandomAccessFile getRandomAccessFile() {
        return tail.readwrite;
    }

    /**
     * Writes a literal string through the HTML transformer
     * (without parsing it or transforming it).
     * @param string String to write
     */
    public synchronized void write(String string) throws IOException {
        if (next == null) emit(string); else next.write(string);
    }

    /**
     * Writes a chunk of HTML through the HTML transformer.
     * @param region Region to write
     */
    public synchronized void write(Region region) throws IOException {
        if (next == null) {
            emitPendingRegion();
            String oldContent = content;
            int oldEmitStart = emitStart;
            int oldEmitEnd = emitEnd;
            int oldTransformEnd = transformEnd;
            content = region.getSource().getContent();
            emitStart = emitEnd = region.getStart();
            transformEnd = region.getEnd();
            processElementsInRegion(region.getRootElement(), region.getStart(), region.getEnd());
            emitPendingRegion();
            content = oldContent;
            emitStart = oldEmitStart;
            emitEnd = oldEmitEnd;
            transformEnd = oldTransformEnd;
        } else next.write(region);
    }

    /**
     * Writes a page through the HTML transformer.
     * @param page Page to write
     */
    public synchronized void writePage(Page page) throws IOException {
        if (next == null) {
            if (page.isHTML()) write(page); else {
                System.err.println("binary write of " + page.getURL());
                writeStream(page.getContentBytes(), 0, page.getLength());
            }
        } else next.writePage(page);
    }

    /**
     * Flushes transformer to its destination stream.
     * Empties any buffers in the transformer chain.
     */
    public synchronized void flush() throws IOException {
        if (next == null) {
            emitPendingRegion();
            if (stream != null) stream.flush();
            if (writer != null) writer.flush();
        } else next.flush();
    }

    /**
     * Close the transformer.  Flushes all buffered data
     * to disk by calling flush().  This call may be
     * time-consuming!  Don't use the transformer again after
     * closing it.
     * @exception IOException if an I/O error occurs
     */
    public synchronized void close() throws IOException {
        flush();
        if (next == null) {
            if (openedStream) {
                if (stream != null) stream.close();
                if (readwrite != null) readwrite.close();
            }
        } else next.close();
    }

    /**
     * Finalizes the transformer (calling close()).
     */
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * Get the file pointer.
     * @return current file pointer
     * @exception IOException if this transformer not opened for random access
     */
    public long getFilePointer() throws IOException {
        if (readwrite == null) throw new IOException("HTMLTransformer not opened for random access");
        return readwrite.getFilePointer();
    }

    /**
     * Seek to a file position.
     * @param pos file position to seek
     * @exception IOException if this transformer not opened for random access
     */
    public void seek(long pos) throws IOException {
        if (readwrite == null) throw new IOException("HTMLTransformer not opened for random access");
        readwrite.seek(pos);
    }

    /**
     * Transform an element by passing it through the entire
     * filter chain.
     * @param elem Element to be transformed
     */
    protected void transformElement(Element elem) throws IOException {
        head.handleElement(elem);
    }

    /**
     * Transform the contents of an element.  Passes
     * the child elements through the filter chain
     * and emits the text between them.
     * @param elem Element whose contents should be transformed
     */
    protected void transformContents(Element elem) throws IOException {
        Tag startTag = elem.getStartTag();
        Tag endTag = elem.getEndTag();
        tail.processElementsInRegion(elem.getChild(), startTag.getEnd(), endTag != null ? endTag.getStart() : elem.getEnd());
    }

    /**
     * Handle the transformation of an HTML element.
     * Override this method to modify the HTML as it is
     * written.
     * @param elem Element to transform
     */
    protected void handleElement(Element elem) throws IOException {
        if (next == null) {
            Tag startTag = elem.getStartTag();
            Tag endTag = elem.getEndTag();
            emit(startTag);
            transformContents(elem);
            if (endTag != null) emit(endTag);
        } else next.handleElement(elem);
    }

    /**
     * Emit a region on the transformer chain's final output.
     * (The region isn't passed through the chain.)
     * @param r Region to emit
     */
    protected void emit(Region r) throws IOException {
        tail.emitInternal(r.getSource().getContent(), r.getStart(), r.getEnd());
    }

    /**
     * Emit a string on the transformer chain's final output.
     * @param string String to emit
     */
    protected void emit(String string) throws IOException {
        tail.emitInternal(string, 0, string.length());
    }

    private void processElementsInRegion(Element elem, int start, int end) throws IOException {
        if (this != tail) throw new RuntimeException("processElementsInRegion not called on tail");
        int p = start;
        if (elem != null && elem.getSource().getContent() == content) end = Math.min(end, transformEnd);
        while (elem != null && elem.getStartTag().getEnd() <= end) {
            emitInternal(content, p, elem.getStart());
            transformElement(elem);
            p = elem.getEnd();
            elem = elem.getNext();
        }
        emitInternal(content, Math.min(p, end), end);
    }

    private void emitInternal(String str, int start, int end) throws IOException {
        if (this != tail) throw new RuntimeException("emitInternal not called on tail");
        if (str == content) {
            start = Math.min(start, transformEnd);
            end = Math.min(end, transformEnd);
            if (start == emitEnd) emitEnd = end; else {
                emitPendingRegion();
                emitStart = start;
                emitEnd = end;
            }
        } else {
            emitPendingRegion();
            writeStream(str.substring(start, end));
        }
    }

    private void emitPendingRegion() throws IOException {
        if (this != tail) throw new RuntimeException("emitPendingRegion not called on tail");
        if (emitStart != emitEnd) {
            writeStream(content.substring(emitStart, emitEnd));
            emitStart = emitEnd;
        }
    }

    private void writeStream(String s) throws IOException {
        if (writer != null) {
            writer.write(s);
        } else readwrite.writeBytes(s);
    }

    private void writeStream(byte[] buf, int offset, int len) throws IOException {
        if (stream != null) {
            stream.write(buf, offset, len);
        } else readwrite.write(buf, offset, len);
    }
}
