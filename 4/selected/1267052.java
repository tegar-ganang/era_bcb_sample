package com.koutra.dist.proc.pipeline;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.ExecutorsHelper;

/**
 * A pipeline item that reads from a result set faucet and writes to a character stream.
 * Implementors need only implement the <code>transformRow</code> method.
 * 
 * @author Pafsanias Ftakas
 */
public abstract class ResultSetToWriterPipelineItem extends AbstractSimplePipelineItem {

    private static final Logger logger = Logger.getLogger(ResultSetToWriterPipelineItem.class);

    protected PipedReader readerForFaucet;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ResultSetToWriterPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 */
    public ResultSetToWriterPipelineItem(String id) {
        super(id);
        this.readerForFaucet = null;
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
            throw new XformationException("Unable to create hooked piped streams", e);
        }
        if (logger.isTraceEnabled()) logger.trace("Created reader " + readerForFaucet + " linked with writer " + writer);
        return readerForFaucet;
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public String dumpPipeline() {
        return sink.dumpPipeline() + "\n" + getClass().getName() + ": " + writer + "->" + readerForFaucet;
    }

    /**
	 * Implementation of the ISink interface.
	 */
    @Override
    public void dispose() {
        faucet.dispose();
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
        if (!hookedUpFaucet) throw new XformationException("Pipeline item has not been set up " + "correctly: faucet has not been set");
        if (faucet instanceof IPipelineItem) ((IPipelineItem) faucet).consume(this);
        if (this instanceof ISinkTemplate) {
            if (!((ISinkTemplate) this).isClone()) {
                return null;
            }
        }
        if (!hookedUpSink) throw new XformationException("Pipeline item has not been set up " + "correctly: sink has not been set");
        final ResultSet resultSet = (ResultSet) faucet.getSource(ContentType.ResultSet);
        if (logger.isTraceEnabled()) logger.trace("Using result set: " + resultSet + " for input");
        getSource(ContentType.CharStream);
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    while (resultSet.next()) {
                        transformRow(resultSet, writer);
                    }
                    writer.close();
                    logger.debug("Executable for the stream pipeline will now exit.");
                } catch (Throwable t) {
                    logger.error("Error during pipeline thread execution.", t);
                }
            }
        });
        return null;
    }
}
