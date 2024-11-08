package com.koutra.dist.proc.pipeline;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.ExecutorsHelper;

/**
 * An implementation of the <code>ISplitPipelineItem</code> interface. It reads
 * information from a character stream and writes the same information (no transformation)
 * to a set of ISink implementations. This allows for the generation of multiple
 * transformation pipelines that are fed from the same source.
 * 
 * @author Pafsanias Ftakas
 */
public class SplitReaderPipelineItem extends AbstractSplitPipelineItem {

    private static final Logger logger = Logger.getLogger(SplitReaderPipelineItem.class);

    protected Reader reader;

    protected List<PipedReader> readerForFaucets;

    protected List<PipedWriter> writers;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public SplitReaderPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 * @param numberOfSinks the number of sinks that are to be connected to this item.
	 */
    public SplitReaderPipelineItem(String id, int numberOfSinks) {
        super(id, numberOfSinks);
        this.reader = null;
        this.readerForFaucets = new ArrayList<PipedReader>(numberOfSinks);
        this.writers = new ArrayList<PipedWriter>(numberOfSinks);
        for (int i = 0; i < numberOfSinks; i++) {
            this.readerForFaucets.add(null);
            this.writers.add(null);
        }
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
    public Object getSource(int index, ContentType contentType) {
        switch(contentType) {
            case ByteStream:
            case XML:
            case ResultSet:
                throw new XformationException("Content type: " + contentType + " is not supported.");
        }
        if (readerForFaucets.get(index) != null) return readerForFaucets.get(index);
        this.writers.set(index, new PipedWriter());
        try {
            this.readerForFaucets.set(index, new PipedReader(this.writers.get(index)));
        } catch (IOException e) {
            throw new XformationException("Unable to create hooked piped reader/writer", e);
        }
        if (logger.isTraceEnabled()) logger.trace("Created for index " + index + ", reader " + readerForFaucets.get(index) + " linked with writer " + writers.get(index));
        return readerForFaucets.get(index);
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public String dumpPipeline() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (ISink sink : sinks) {
            sb.append(sink.dumpPipeline() + "\n" + getClass().getName() + ": " + reader + "->" + writers.get(index) + "->" + readerForFaucets.get(index));
            index++;
        }
        return sb.toString();
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public void dispose(int index) {
        try {
            if (readerForFaucets.get(index) != null) readerForFaucets.get(index).close();
            readerForFaucets.set(index, null);
        } catch (IOException e) {
            throw new XformationException("Unable to close input stream", e);
        }
        boolean isLastDisposal = true;
        for (Reader reader : readerForFaucets) {
            if (reader != null) {
                isLastDisposal = false;
                break;
            }
        }
        if (isLastDisposal) faucet.dispose();
    }

    /**
	 * Implementation of the IPipelineItem interface.
	 */
    @Override
    public Object consume(ISink previousSink) {
        if (!hookedUpFaucet) throw new XformationException("Pipeline item has not been set up " + "correctly: faucet has not been set");
        if (!consumptionGate.getAndSet(false)) return null;
        consumptionSemaphore.acquireUninterruptibly(sinks.size());
        if (faucet instanceof IPipelineItem) ((IPipelineItem) faucet).consume(this);
        if (this instanceof ISinkTemplate) {
            if (!((ISinkTemplate) this).isClone()) {
                return null;
            }
        }
        boolean hookedUpSinks = true;
        for (Boolean hookedUpSink : hookedUpSinkFlags) {
            if (!hookedUpSink) {
                hookedUpSinks = false;
                break;
            }
        }
        if (!hookedUpSinks) throw new XformationException("Pipeline item has not been set up " + "correctly: sinks has not been all set");
        reader = (Reader) faucet.getSource(ContentType.CharStream);
        for (int i = 0; i < sinks.size(); i++) getSource(i, ContentType.CharStream);
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    int count;
                    char[] buffer = new char[8 * 1024];
                    if (logger.isTraceEnabled()) logger.trace("Using reader " + reader + " for the stream transformation.");
                    while ((count = reader.read(buffer)) != -1) {
                        for (Writer writer : writers) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Read " + new String(buffer, 0, count) + " from reader " + reader + ", writing to writer " + writer);
                            }
                            writer.write(buffer, 0, count);
                        }
                    }
                    for (Writer writer : writers) writer.close();
                    logger.debug("Executable for the stream pipeline will now exit.");
                } catch (Throwable t) {
                    logger.error("Error during pipeline thread execution.", t);
                }
            }
        });
        return null;
    }

    /**
	 * Override the <code>Streamable</code> implementation in order to deserialize
	 * local members.
	 */
    @Override
    public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
        super.readFrom(in);
        int numberOfSinks = sinks.size();
        this.reader = null;
        this.readerForFaucets = new ArrayList<PipedReader>(numberOfSinks);
        this.writers = new ArrayList<PipedWriter>(numberOfSinks);
        for (int i = 0; i < numberOfSinks; i++) {
            this.readerForFaucets.add(null);
            this.writers.add(null);
        }
    }
}
