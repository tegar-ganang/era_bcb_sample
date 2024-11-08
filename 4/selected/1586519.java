package com.koutra.dist.proc.pipeline.demux;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.sql.ResultSet;
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
 * A demultiplexer class that reads a result set and feeds pipelines
 * with byte streams. Implementors should implement the
 * and the <code>transformRow</code> method.
 * 
 * @author Pafsanias Ftakas
 */
public abstract class ResultSetToWriterDemuxPipelineItem extends AbstractDemuxPipelineItem {

    private static final Logger logger = Logger.getLogger(ResultSetToWriterDemuxPipelineItem.class);

    protected PipedReader readerForFaucet;

    protected List<PipedReader> readerListForDisposal;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ResultSetToWriterDemuxPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 * @param pipelineTemplate the sink template that follows this item. Actual sinks
	 * will be clones of this template.
	 */
    public ResultSetToWriterDemuxPipelineItem(String id, ISinkTemplate pipelineTemplate) {
        super(id, pipelineTemplate);
        if (!pipelineTemplate.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("SinkTemplate '" + pipelineTemplate.getId() + "' must support the ByteStream content type.");
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
            case ResultSet:
                return true;
            case ByteStream:
            case CharStream:
            case XML:
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
            case XML:
            case ByteStream:
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
        if (!faucet.supportsOutput(ContentType.ResultSet)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' must support the ResultSet content type.");
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
            case XML:
            case ByteStream:
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
    public void switchPipeline() {
        sink.set(null);
        readerForFaucet = null;
        writer = null;
        hookedUpSink = false;
        createPipeline();
        getSource(ContentType.CharStream);
        if (sink.get() instanceof IPipelineItem) ((IPipelineItem) sink.get()).handleMuxSwitch();
        if (logger.isTraceEnabled()) logger.trace("Dumping pipeline:\n" + dumpPipeline());
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public String dumpPipeline() {
        return sink.get().dumpPipeline() + "\n" + getClass().getName() + ": " + writer + "->" + readerForFaucet;
    }

    /**
	 * Sends a notification down the pipeline that the multiplexing pipeline has finished.
	 * The notification is basically calling the <code>registerSource</code> interface
	 * at the final multiplexer with a value of <code>null</code>. This will signal to
	 * the multiplexer that it should stop its main loop.
	 */
    public void signalTermination() {
        if (pipelineTemplate instanceof IPipelineItem) ((IPipelineItem) pipelineTemplate).endMux(); else pipelineTemplate.registerSource(null);
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
            throw new XformationException("Unable to close input stream", e);
        }
        super.dispose();
    }

    /**
	 * Allows implementors to perform an arbitrary transformation of the result set to
	 * the writer. Implementors should normally just consume the current row and
	 * not call the next() interface, but are free to do so if they must.
	 * @param resultSet the result set that contains the data
	 * @param writer the writer to write to
	 */
    public abstract void transformRow(ResultSet resultSet, Writer writer);

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
        final ResultSet resultSet = (ResultSet) faucet.getSource(ContentType.ResultSet);
        if (logger.isTraceEnabled()) logger.trace("Using result set: " + resultSet + " for input");
        getSource(ContentType.CharStream);
        if (sink.get() instanceof IPipelineItem) ((IPipelineItem) sink.get()).handleMuxSwitch();
        final ISink fPreviousSink = previousSink;
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    boolean firstTime = true;
                    while (resultSet.next()) {
                        if (!firstTime) {
                            writer.close();
                            switchPipeline();
                        }
                        transformRow(resultSet, writer);
                        firstTime = false;
                    }
                    writer.close();
                    if (fPreviousSink instanceof IPipelineItem) ((IPipelineItem) sink.get()).endMux();
                    logger.debug("Executable for the stream demux pipeline will now exit.");
                } catch (Throwable t) {
                    logger.error("Error while running pipeline", t);
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
        this.readerForFaucet = null;
        this.readerListForDisposal = new ArrayList<PipedReader>();
        this.writer = null;
    }
}
