package com.koutra.dist.proc.sink;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.XformationException;

/**
 * A sink implementation that writes out to a writer. When using the
 * constructor that supplies the writer, the caller has the responsibility
 * of closing the writer when the transformation is completed.
 * 
 * @author Pafsanias Ftakas
 */
public class WriterSink extends AbstractFileOrStreamSink {

    private static final Logger logger = Logger.getLogger(WriterSink.class);

    protected Writer writer;

    protected Reader reader;

    protected String outputCharset;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public WriterSink() {
    }

    /**
	 * Initializing constructor for the Stream type.
	 * @param id the ID of the sink.
	 * @param writer the writer to write to.
	 */
    public WriterSink(String id, Writer writer) {
        super(id);
        this.writer = writer;
        this.reader = null;
        this.outputCharset = null;
    }

    /**
	 * Initializing constructor for the File type.
	 * @param id the ID of the sink.
	 * @param path the path to the file to write to.
	 */
    public WriterSink(String id, String path, String outputCharset) {
        super(id, path);
        this.writer = null;
        this.reader = null;
        this.outputCharset = outputCharset;
    }

    /**
	 * Implementation of the <code>ISink</code> interface.
	 * 
	 * @param contentType the type that we want this sink to support.
	 * @return true iff this sink supports the content type argument.
	 */
    @Override
    public boolean supportsInput(ContentType contentType) {
        switch(contentType) {
            case CharStream:
                return true;
            case ByteStream:
            case XML:
            case ResultSet:
            default:
                return false;
        }
    }

    /**
	 * Override the implementation in the abstract sink to add a check that the faucet
	 * supports the proper content type.
	 */
    @Override
    protected void checkFaucetValidity(IFaucet faucet) {
        super.checkFaucetValidity(faucet);
        if (!faucet.supportsOutput(ContentType.CharStream)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' must support the CharStream content type.");
    }

    /**
	 * Implementation of the <code>ISink</code> interface.
	 */
    @Override
    public void registerSource(Object source) {
    }

    /**
	 * Implementation of the <code>ISink</code> interface.
	 */
    @Override
    public String dumpPipeline() {
        return getClass().getName() + ": " + reader + "->" + writer;
    }

    /**
	 * Implementation of the <code>ISink</code> interface.
	 */
    @Override
    public void dispose() {
        switch(type) {
            case File:
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new XformationException("Unable to close writer", e);
                }
                break;
            case Stream:
                break;
        }
        faucet.dispose();
    }

    /**
	 * Implementation of the <code>ISink</code> interface.
	 */
    @Override
    public void consume() {
        if (!hookedUp) throw new XformationException("Sink has not been set up correctly: " + "faucet has not been set");
        switch(type) {
            case File:
                try {
                    if (outputCharset == null) writer = new BufferedWriter(new FileWriter(path)); else writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), outputCharset));
                } catch (IOException e) {
                    throw new XformationException("Unable to create output writer", e);
                }
                break;
            case Stream:
                break;
        }
        if (faucet instanceof IPipelineItem) {
            ((IPipelineItem) faucet).consume(this);
        }
        try {
            reader = (Reader) faucet.getSource(ContentType.CharStream);
            int count;
            char[] buffer = new char[8 * 1024];
            if (logger.isTraceEnabled()) logger.trace("Using the reader " + reader + " in the sink");
            while ((count = reader.read(buffer)) != -1) {
                if (logger.isTraceEnabled()) logger.trace("Read '" + new String(buffer, 0, count) + "' from reader " + reader + " in the sink");
                writer.write(buffer, 0, count);
            }
        } catch (IOException ioe) {
            throw new XformationException("Unable to transform char stream", ioe);
        }
    }

    /**
	 * Override the <code>Streamable</code> implementation in order to deserialize
	 * local members.
	 */
    @Override
    public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
        super.readFrom(in);
        this.outputCharset = in.readUTF();
    }

    /**
	 * Override the <code>Streamable</code> implementation in order to serialize local
	 * members.
	 */
    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        super.writeTo(out);
        out.writeUTF(outputCharset);
    }
}
