package com.koutra.dist.proc.pipeline;

import java.io.BufferedReader;
import java.io.Reader;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.ExecutorsHelper;

/**
 * A pipeline item that reads information from a reader and writes to a writer.
 * This is quite similar in functionality to the ReaderToWriterPipelineItem, but this
 * pipeline item transforms the input one line at a time.
 * Implementors need only give the implementation of the transformLine() method.
 * 
 * @author Pafsanias Ftakas
 */
public abstract class LineBasedReaderToWriterPipelineItem extends ReaderToWriterPipelineItem {

    private static final Logger logger = Logger.getLogger(LineBasedReaderToWriterPipelineItem.class);

    protected BufferedReader bufferedReader;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public LineBasedReaderToWriterPipelineItem() {
    }

    /**
	 * Initializing constructor.
	 * @param id the ID of the pipeline item.
	 */
    public LineBasedReaderToWriterPipelineItem(String id) {
        super(id);
        bufferedReader = null;
    }

    /**
	 * Implementation of the ReaderToWriterPipelineItem interface for the stream
	 * transformation. Subclasses of this class, should provide an implementation of
	 * the <code>transformLine</code> method instead.
	 */
    @Override
    public char[] transformBuffer(char[] buffer, int off, int len) {
        return null;
    }

    /**
	 * Transform a single line of input.
	 * @param line The input line or null if the end of the input has been reached.
	 * @return The transformed String to pass down the pipeline chain.
	 */
    public abstract String transformLine(String line);

    /**
	 * Implementation of the IPipelineItem interface. Override the implementation in the
	 * superclass, as we provide an alternative implementation that uses
	 * <code>transformLine</code> instead of <code>transformBuffer</code>.
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
        bufferedReader = new BufferedReader(reader);
        getSource(ContentType.CharStream);
        ExecutorsHelper.getInstance().executeInProc(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String transformedLine = transformLine(line);
                        if (logger.isTraceEnabled()) logger.trace("Read '" + line + "' from reader" + bufferedReader + ", writing '" + transformedLine + "' to writer " + writer);
                        writer.write(transformedLine);
                    }
                    String transformedLine = transformLine(line);
                    logger.trace("Writing final '" + transformedLine + "' to writer " + writer);
                    writer.write(transformedLine);
                    writer.close();
                    logger.debug("Executable for the line reader pipeline will now exit.");
                } catch (Throwable t) {
                    logger.error("Error during pipeline thread execution.", t);
                }
            }
        });
        return null;
    }
}
