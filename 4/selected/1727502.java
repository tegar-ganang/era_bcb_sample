package com.koutra.dist.proc.pipeline.demux;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
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
 * A demultiplexer class that reads a character stream and feeds pipelines
 * with character streams. Implementors should implement the <code>shouldChangePipeline</code>
 * and the <code>transformBuffer</code> methods.
 * 
 * @author Pafsanias Ftakas
 */
public abstract class ReaderDemuxPipelineItem extends AbstractDemuxPipelineItem {

    private static final Logger logger = Logger.getLogger(ReaderDemuxPipelineItem.class);

    protected Reader reader;

    protected PipedReader readerForFaucet;

    protected List<PipedReader> readerListForDisposal;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ReaderDemuxPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 * @param pipelineTemplate the sink template that follows this item. Actual sinks
	 * will be clones of this template.
	 */
    public ReaderDemuxPipelineItem(String id, ISinkTemplate pipelineTemplate) {
        super(id, pipelineTemplate);
        if (!pipelineTemplate.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("SinkTemplate '" + pipelineTemplate.getId() + "' must support the CharStream content type.");
        this.reader = null;
        this.readerForFaucet = null;
        this.readerListForDisposal = new ArrayList<PipedReader>();
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
        this.readerListForDisposal.add(this.readerForFaucet);
        if (logger.isTraceEnabled()) logger.trace("Created reader " + readerForFaucet + " and writer " + writer);
        return readerForFaucet;
    }

    /**
	 * Implementation of the ISink interface. Override to close the piped readers
	 * that we have amassed.
	 */
    @Override
    public void dispose() {
        ISink currentSink = sink.get();
        if (currentSink == null) return;
        if (currentSink instanceof ISinkTemplate) if (((ISinkTemplate) currentSink).isClone()) return;
        try {
            logger.debug("Disposing reader list now!!!");
            for (PipedReader reader : readerListForDisposal) {
                reader.close();
            }
        } catch (IOException e) {
            throw new XformationException("Unable to close reader", e);
        }
        super.dispose();
    }

    /**
	 * Helper method to create a new pipeline when a pipeline switch should take place.
	 * It creates a clone of our sink template and all the templates following in the
	 * pipeline up until the multiplexer class.
	 */
    protected void createPipeline() {
        pipelineTemplate.createClone(this);
    }

    /**
	 * Helper method to switch to a new pipeline. It eliminates the currently running
	 * pipeline, creates a new one, and hooks everything up.
	 */
    protected void switchPipeline() {
        sink.set(null);
        readerForFaucet = null;
        writer = null;
        hookedUpSink = false;
        createPipeline();
        getSource(ContentType.CharStream);
        if (sink.get() instanceof IPipelineItem) ((IPipelineItem) sink.get()).handleMuxSwitch();
        if (logger.isDebugEnabled()) logger.debug("Dumping pipeline:\n" + dumpPipeline());
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public String dumpPipeline() {
        return sink.get().dumpPipeline() + "\n" + getClass().getName() + ": " + reader + "->" + writer + "->" + readerForFaucet;
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
	 * This method should return true, if the rest of the pipeline should "break"
	 * at this point in the transformation.
	 * 
	 * @return True iff the transformation pipeline should "break" at this point.
	 */
    public abstract boolean shouldChangePipeline();

    /**
	 * Implementation of the IPipelineItem interface.
	 */
    @Override
    public Object consume(ISink previousSink) {
        if (!hookedUpFaucet) throw new XformationException("Pipeline item has not been set up correctly:" + "faucet has not been set");
        if (previousSink instanceof ISinkTemplate) {
            if (((ISinkTemplate) previousSink).isClone()) return null;
        }
        if (faucet instanceof IPipelineItem) ((IPipelineItem) faucet).consume(this);
        createPipeline();
        reader = (Reader) faucet.getSource(ContentType.CharStream);
        getSource(ContentType.CharStream);
        if (sink.get() instanceof IPipelineItem) ((IPipelineItem) sink.get()).handleMuxSwitch();
        final ISink fPreviousSink = previousSink;
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    int count;
                    char[] buffer = new char[8 * 1024];
                    while ((count = reader.read(buffer)) != -1) {
                        char[] transformation = transformBuffer(buffer, 0, count);
                        if (logger.isTraceEnabled()) logger.trace("Read '" + new String(buffer, 0, count) + "' from " + reader + " writing '" + new String(transformation) + "' to writer " + writer);
                        writer.write(transformation, 0, transformation.length);
                        while (shouldChangePipeline()) {
                            writer.close();
                            if (logger.isTraceEnabled()) logger.trace("About to switch pipeline");
                            switchPipeline();
                            transformation = transformBuffer(buffer, 0, 0);
                            if (logger.isTraceEnabled()) logger.trace("Writing  additional '" + new String(transformation) + "' to writer " + writer);
                            writer.write(transformation, 0, transformation.length);
                        }
                    }
                    char[] transformation = transformBuffer(buffer, 0, count);
                    if (logger.isTraceEnabled()) logger.trace("Writing  final '" + new String(transformation) + "' to writer " + writer);
                    writer.write(transformation, 0, transformation.length);
                    writer.close();
                    if (fPreviousSink instanceof IPipelineItem) {
                        IPipelineItem pipelineSink = (IPipelineItem) fPreviousSink;
                        pipelineSink.endMux();
                    }
                    logger.debug("Executable for the reader pipeline will now exit.");
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
        this.reader = null;
        this.readerForFaucet = null;
        this.readerListForDisposal = new ArrayList<PipedReader>();
        this.writer = null;
    }
}
