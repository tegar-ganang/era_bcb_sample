package com.koutra.dist.proc.pipeline;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.IDemuxFaucet;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IFaucetTemplate;
import com.koutra.dist.proc.model.IMuxSink;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISimplePipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.ISplitPipelineItem;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.ExecutorsHelper;

public abstract class ResultSetToWriterDemuxPipelineItem implements ISimplePipelineItem, IDemuxFaucet {

    private static final Logger logger = Logger.getLogger(ResultSetToWriterDemuxPipelineItem.class);

    protected String id;

    protected boolean hookedUpFaucet;

    protected boolean hookedUpSink;

    protected AtomicReference<ISink> sink;

    protected ISinkTemplate pipelineTemplate;

    protected IFaucet faucet;

    protected PipedReader readerForFaucet;

    protected List<PipedReader> readerListForDisposal;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ResultSetToWriterDemuxPipelineItem() {
    }

    public ResultSetToWriterDemuxPipelineItem(String id, ISinkTemplate pipelineTemplate) {
        if (!pipelineTemplate.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("SinkTemplate '" + pipelineTemplate.getId() + "' must support the ByteStream content type.");
        this.id = id;
        this.hookedUpFaucet = false;
        this.hookedUpSink = false;
        this.sink = new AtomicReference<ISink>(null);
        this.faucet = null;
        this.pipelineTemplate = pipelineTemplate;
        this.readerForFaucet = null;
        this.readerListForDisposal = new ArrayList<PipedReader>();
        this.writer = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ISink getSink() {
        return sink.get();
    }

    @Override
    public IFaucet getFaucet() {
        return faucet;
    }

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

    protected void checkFaucetValidity(IFaucet faucet) {
        if (!faucet.supportsOutput(ContentType.ResultSet)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' must support the ResultSet content type.");
        if (faucet instanceof IDemuxFaucet) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' is an IDemuxFaucet instance. This is not allowed.");
        if (faucet instanceof IFaucetTemplate) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' is an IFaucetTemplate instance. This is not allowed.");
    }

    @Override
    public void hookupFaucet(IFaucet faucet) {
        checkFaucetValidity(faucet);
        if (hookedUpFaucet && this.faucet == faucet) {
            return;
        }
        if (hookedUpFaucet) throw new XformationException("Trying to hook up an already hooked up sink.");
        this.faucet = faucet;
        this.hookedUpFaucet = true;
        this.faucet.hookupSink(this);
    }

    protected void checkSinkValidity(ISink sink) {
        if (!sink.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("Sink '" + sink.getId() + "' must support the CharStream content type.");
        if (!(sink instanceof IMuxSink) && !(sink instanceof ISinkTemplate)) throw new IllegalArgumentException("Sink '" + sink.getId() + "' is neither an IMuxSink or an ISinkTemplate instance. " + "This is not allowed.");
    }

    @Override
    public void hookupSink(ISink sink) {
        checkSinkValidity(sink);
        if (hookedUpSink && this.sink.get() == sink) {
            return;
        }
        this.sink.set(sink);
        this.hookedUpSink = true;
        this.sink.get().hookupFaucet(this);
    }

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

    @Override
    public void registerSource(Object source) {
    }

    @Override
    public void handleMuxSwitch() {
    }

    @Override
    public void endMux() {
    }

    protected void createPipeline() {
        pipelineTemplate.createClone(this);
    }

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

    @Override
    public String dumpPipeline() {
        return sink.get().dumpPipeline() + "\n" + getClass().getName() + ": " + writer + "->" + readerForFaucet;
    }

    public void signalTermination() {
        if (pipelineTemplate instanceof IPipelineItem) ((IPipelineItem) pipelineTemplate).endMux(); else pipelineTemplate.registerSource(null);
    }

    @Override
    public void dispose() {
        ISink currentSink = sink.get();
        if (currentSink == null) return;
        if (currentSink instanceof ISinkTemplate) if (((ISinkTemplate) currentSink).isClone()) return;
        try {
            for (PipedReader reader : readerListForDisposal) {
                reader.close();
            }
        } catch (IOException e) {
            throw new XformationException("Unable to close input stream", e);
        }
        faucet.dispose();
    }

    @Override
    public void consume() {
        throw new XformationException("Calling ISink.consume() on a IPipelineItem " + "implementation. Call IPipelineItem.consume(ISink) instead.");
    }

    @Override
    public Future<?> consumeAsynchronously() {
        throw new XformationException("Calling ISink.consumeAsynchronously() on a IPipelineItem " + "implementation. Call IPipelineItem.consume(ISink) instead.");
    }

    /**
	 * Allows implementors to perform an arbitrary transformation of the result set to
	 * the writer. Implementors should normally just consume the current row and
	 * not call the next() interface, but are free to do so if they must.
	 * @param resultSet the result set that contains the data
	 * @param writer the writer to write to
	 */
    public abstract void transformRow(ResultSet resultSet, Writer writer);

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

    @Override
    public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
        this.id = in.readUTF();
        this.hookedUpFaucet = false;
        this.hookedUpSink = false;
        this.sink = new AtomicReference<ISink>(null);
        this.faucet = null;
        this.readerForFaucet = null;
        this.readerListForDisposal = new ArrayList<PipedReader>();
        this.writer = null;
        String sinkClassName = in.readUTF();
        Class<?> sinkClass;
        try {
            sinkClass = Class.forName(sinkClassName);
        } catch (ClassNotFoundException e) {
            throw new InstantiationException("Unable to load class: " + sinkClassName);
        }
        this.pipelineTemplate = (ISinkTemplate) sinkClass.newInstance();
        this.pipelineTemplate.readFrom(in);
        this.pipelineTemplate.hookupFaucet(this);
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeUTF(id);
        out.writeUTF(pipelineTemplate.getClass().getCanonicalName());
        pipelineTemplate.writeTo(out);
    }

    @Override
    public List<ISink> getTerminalSinks() {
        if (pipelineTemplate instanceof IFaucet) return ((IFaucet) pipelineTemplate).getTerminalSinks(); else if (pipelineTemplate instanceof ISplitPipelineItem) return ((ISplitPipelineItem) pipelineTemplate).getTerminalSinks(); else {
            List<ISink> retVal = new ArrayList<ISink>();
            retVal.add(pipelineTemplate);
            return retVal;
        }
    }
}
