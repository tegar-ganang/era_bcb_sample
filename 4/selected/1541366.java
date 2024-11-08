package com.koutra.dist.proc.pipeline;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.ExecutorsHelper;

/**
 * A pipeline item that reads information from a reader and writes to a writer.
 * Implementors need only give the implementation of the transformBuffer() method.
 * 
 * @author Pafsanias Ftakas
 */
public abstract class ReaderToWriterPipelineItem extends AbstractSimplePipelineItem {

    private static final Logger logger = Logger.getLogger(ReaderToWriterPipelineItem.class);

    protected Reader reader;

    protected PipedReader readerForFaucet;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ReaderToWriterPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 */
    public ReaderToWriterPipelineItem(String id) {
        super(id);
        this.reader = null;
        this.readerForFaucet = null;
        this.writer = null;
    }

    /**
	 * Implementation of the ISink interface.
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
	 * Implementation of the IFaucet interface.
	 */
    @Override
    public boolean supportsOutput(ContentType contentType) {
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
	 * Implementation of the ISink interface. Override to check for content type.
	 */
    protected void checkFaucetValidity(IFaucet faucet) {
        super.checkFaucetValidity(faucet);
        if (!faucet.supportsOutput(ContentType.CharStream)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' must support the CharStream content type.");
    }

    /**
	 * Implementation of the IFaucet interface. Override to check for content type.
	 */
    protected void checkSinkValidity(ISink sink) {
        super.checkSinkValidity(sink);
        if (!sink.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("Sink '" + sink.getId() + "' must support the CharStream content type.");
    }

    /**
	 * Implementation of the IFaucet interface.
	 */
    @Override
    public Object getSource(ContentType contentType) {
        switch(contentType) {
            case ByteStream:
            case XML:
            case ResultSet:
                throw new XformationException("Content type: " + contentType + " is not supported.");
        }
        if (readerForFaucet != null) return readerForFaucet;
        this.writer = new PipedWriter();
        try {
            this.readerForFaucet = new PipedReader(this.writer);
        } catch (IOException e) {
            throw new XformationException("Unable to create hooked piped reader/writer", e);
        }
        if (logger.isTraceEnabled()) logger.trace("Created reader " + readerForFaucet + " linked with writer " + writer);
        return readerForFaucet;
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public String dumpPipeline() {
        return sink.dumpPipeline() + "\n" + getClass().getName() + ": " + reader + "->" + writer + "->" + readerForFaucet;
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public void dispose() {
        try {
            if (readerForFaucet != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("About to close reader: " + readerForFaucet);
                }
                readerForFaucet.close();
            }
        } catch (IOException e) {
            throw new XformationException("Unable to close reader", e);
        }
        faucet.dispose();
    }

    /**
	 * This method is a callback that performs the transformation. The returned char
	 * array will get written on the writer that hooks this pipeline item to its
	 * sink down the pipeline chain.
	 * 
	 * @param buffer The buffer to read chars from. Only the chars in the specified
	 * range should be used.
	 * @param off The offset of the first char in the buffer that is part of the range
	 * to use.
	 * @param len The number of chars of the valid range, or null if the end of the
	 * input has been reached.
	 * @return The char array to pass down the pipeline chain.
	 */
    public abstract char[] transformBuffer(char[] buffer, int off, int len);

    /**
	 * Implementation of the IPipelineItem interface.
	 */
    @Override
    public Object consume(ISink previousSink) {
        if (!hookedUpFaucet) throw new XformationException("Pipeline item has not been set up " + "correctly: faucet has not been set");
        if (faucet instanceof IPipelineItem) ((IPipelineItem) faucet).consume(this);
        if (this instanceof ISinkTemplate) {
            if (!((ISinkTemplate) this).isClone()) {
                return null;
            }
        }
        if (!hookedUpSink) throw new XformationException("Pipeline item has not been set up " + "correctly: sink has not been set");
        reader = (Reader) faucet.getSource(ContentType.CharStream);
        getSource(ContentType.CharStream);
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    int count;
                    char[] buffer = new char[8 * 1024];
                    logger.trace("Using reader " + reader + " for the transformation pipeline");
                    while ((count = reader.read(buffer)) != -1) {
                        char[] transformation = transformBuffer(buffer, 0, count);
                        if (logger.isTraceEnabled()) logger.trace("Read '" + new String(buffer, 0, count) + "' from " + reader + " writing " + new String(transformation) + " to " + writer);
                        writer.write(transformation, 0, transformation.length);
                    }
                    char[] transformation = transformBuffer(buffer, 0, count);
                    if (logger.isTraceEnabled()) logger.trace("Writing final '" + new String(transformation) + "' to " + writer);
                    writer.write(transformation, 0, transformation.length);
                    writer.close();
                    logger.debug("Executable for the reader pipeline will now exit.");
                } catch (Throwable t) {
                    logger.error("Error during pipeline thread execution.", t);
                }
            }
        });
        return null;
    }
}
