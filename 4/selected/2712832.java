package info.jonclark.corpus.management.documents;

import info.jonclark.corpus.management.etc.CorpusManRuntimeException;
import info.jonclark.io.LineWriter;
import info.jonclark.util.FileUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Abstracts the location of a corpus document from the user so that tasks such
 * as distributed processing become easy.
 */
public class OutputDocument implements LineWriter, CloseableDocument {

    private PrintWriter writer;

    private boolean closed = true;

    protected final File file;

    private final Charset encoding;

    private final MetaDocument meta;

    public OutputDocument(File file, MetaDocument meta, Charset encoding) {
        assert file.getParentFile().exists() : "Parent directory does not exist: " + file.getAbsolutePath();
        this.file = file;
        this.encoding = encoding;
        this.meta = meta;
    }

    /**
         * Do lazy initialization of the writer so that we can do non-closed
         * file detection properly.
         */
    private void open() {
        if (this.writer == null) {
            closed = false;
            try {
                this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding)));
            } catch (FileNotFoundException e) {
                throw new CorpusManRuntimeException("File should exist but does not. Deleted during runtime?", e);
            }
        }
    }

    public OutputDocument(URL url) {
        throw new Error("Unimplemented. Please wait for our distributed architecture.");
    }

    /**
         * Get a document that can store meta data about this document. The meta
         * data is the same for both the input and output documents and is
         * global accross all runs.
         */
    public MetaDocument getMetaDocument() {
        return meta;
    }

    public void println(String line) {
        open();
        this.writer.println(line);
    }

    public void copyFrom(InputDocument doc) throws IOException {
        boolean thisWasClosed = closed;
        closed = false;
        if (doc.encoding.equals(this.encoding)) {
            FileUtils.copyFile(doc.file, this.file);
        } else {
            open();
            BufferedReader in = doc.getBufferedReader();
            String line;
            while ((line = in.readLine()) != null) writer.println(line);
            in.close();
            if (thisWasClosed) writer.close();
        }
        closed = thisWasClosed;
    }

    public void close() {
        closed = true;
        if (writer != null) writer.close();
    }

    public boolean isClosed() {
        return closed;
    }
}
