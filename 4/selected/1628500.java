package com.koutra.dist.proc.pipeline;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.cluster.PipedReader;
import com.koutra.dist.proc.cluster.PipedWriter;
import com.koutra.dist.proc.model.ContentType;
import com.koutra.dist.proc.model.FaucetAdapter;
import com.koutra.dist.proc.model.IDemuxFaucet;
import com.koutra.dist.proc.model.IFaucet;
import com.koutra.dist.proc.model.IFaucetTemplate;
import com.koutra.dist.proc.model.IMuxSink;
import com.koutra.dist.proc.model.IPipelineItem;
import com.koutra.dist.proc.model.ISink;
import com.koutra.dist.proc.model.ISinkTemplate;
import com.koutra.dist.proc.model.XformationException;
import com.koutra.dist.proc.util.EmptyReader;

public class ReaderTaskBoundaryTemplate extends AbstractTaskBoundaryTemplate {

    private static final Logger logger = Logger.getLogger(ReaderTaskBoundaryTemplate.class);

    protected Reader reader;

    protected PipedReader readerForFaucet;

    protected PipedWriter writer;

    /**
	 * @deprecated Use any of the initializing constructors instead.
	 */
    public ReaderTaskBoundaryTemplate() {
        reader = null;
        readerForFaucet = null;
        writer = null;
    }

    public ReaderTaskBoundaryTemplate(String id) {
        super(id);
        this.reader = null;
        this.readerForFaucet = null;
        this.writer = null;
    }

    @Override
    protected ContentType getSupportedFaucetContentType() {
        return ContentType.CharStream;
    }

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

    protected void checkFaucetValidity(IFaucet faucet) {
        if (!faucet.supportsOutput(ContentType.CharStream)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' must support the CharStream content type.");
        if (!(faucet instanceof IDemuxFaucet) && !(faucet instanceof IFaucetTemplate) && !(faucet instanceof FaucetAdapter)) throw new IllegalArgumentException("Faucet '" + faucet.getId() + "' is neither an IDemuxFaucet nor an IFaucetTemplate instance, " + "nor a FaucetAdapter. " + "This is not allowed.");
    }

    protected void checkSinkValidity(ISink sink) {
        if (!sink.supportsInput(ContentType.CharStream)) throw new IllegalArgumentException("Sink '" + sink.getId() + "' must support the CharStream content type.");
        if (!(sink instanceof IMuxSink) && !(sink instanceof ISinkTemplate)) throw new IllegalArgumentException("Sink '" + sink.getId() + "' is neither an IMuxSink or an ISinkTemplate instance. " + "This is not allowed.");
    }

    @Override
    public Object getSource(ContentType contentType) {
        switch(contentType) {
            case ByteStream:
            case XML:
            case ResultSet:
                throw new XformationException("Content type: " + contentType + " is not supported.");
        }
        if (readerForFaucet != null) return readerForFaucet;
        try {
            if (isRemoteTaskSide) {
                if (sink instanceof IMuxSink) {
                    if (this.writer != null) return new EmptyReader();
                    this.writer = new PipedWriter("UTF-8");
                    if (logger.isTraceEnabled()) logger.trace("Created writer (end of pipeline) " + writer);
                    return new EmptyReader();
                } else {
                    this.readerForFaucet = new PipedReader(InetAddress.getByAddress(remoteServerIP), remoteServerPort, "UTF-8");
                    if (logger.isTraceEnabled()) logger.trace("Created reader (start of pipeline) " + readerForFaucet + " linked with IP: " + InetAddress.getByAddress(remoteServerIP).toString() + ":" + remoteServerPort);
                    return readerForFaucet;
                }
            } else {
                if (sink instanceof IMuxSink) {
                    this.readerForFaucet = new PipedReader(InetAddress.getByAddress(remoteServerIP), remoteServerPort, "UTF-8");
                    if (logger.isTraceEnabled()) logger.trace("Created reader (end of pipeline) " + readerForFaucet + " linked with IP: " + InetAddress.getByAddress(remoteServerIP).toString() + ":" + remoteServerPort);
                    return readerForFaucet;
                } else {
                    if (this.writer != null) return new EmptyReader();
                    this.writer = new PipedWriter("UTF-8");
                    if (logger.isTraceEnabled()) logger.trace("Created writer (start of pipeline) " + writer);
                    return new EmptyReader();
                }
            }
        } catch (Exception e) {
            throw new XformationException("Unable to get source", e);
        }
    }

    protected byte[] getOtherEndHostIP() {
        return writer.getOtherEndHostIP();
    }

    protected int getOtherEndPort() {
        return writer.getOtherEndPort();
    }

    @Override
    public void handleMuxSwitch() {
        if (sink instanceof IPipelineItem) {
            if (!isRemoteTaskSide) {
                try {
                    writer.connect();
                } catch (IOException e) {
                    throw new XformationException("Unable to connect our server socket to a client.", e);
                }
            }
        } else {
            if (!isRemoteTaskSide) {
                sink.registerSource(getSource(ContentType.CharStream));
            }
        }
        super.handleMuxSwitch();
    }

    @Override
    public String dumpPipeline() {
        return sink.dumpPipeline() + "\n" + getClass().getName() + ": " + reader + "->" + writer + "->" + readerForFaucet;
    }

    @Override
    public void dispose() {
        try {
            if (readerForFaucet != null) readerForFaucet.close();
        } catch (IOException e) {
            throw new XformationException("Unable to close reader", e);
        }
        faucet.dispose();
    }

    protected byte[] getServerHostIP() {
        return writer.getServerHostIP();
    }

    protected int getServerPort() {
        return writer.getServerPort();
    }

    protected void localStartBoundaryImpl(Object inputSource) {
        reader = (Reader) faucet.getSource(ContentType.CharStream);
        getSource(ContentType.CharStream);
        try {
            int count;
            char[] buffer = new char[8 * 1024];
            if (logger.isTraceEnabled()) logger.trace("Using input stream " + reader + " for the stream transformation (start of pipeline).");
            while ((count = reader.read(buffer)) != -1) {
                String readInput = new String(buffer, 0, count);
                if (logger.isTraceEnabled()) logger.trace("Read " + readInput + " from reader " + reader + ", writing to writer " + writer);
                writer.write(buffer, 0, count);
            }
            writer.close();
        } catch (Throwable t) {
            logger.error("Error during pipeline thread execution.", t);
        }
    }

    protected void remoteEndBoundaryImpl(Object inputSource) {
        try {
            writer.connect();
        } catch (IOException e) {
            throw new XformationException("Unable to connect our server socket to a client.", e);
        }
        reader = (Reader) faucet.getSource(ContentType.CharStream);
        getSource(ContentType.CharStream);
        try {
            int count;
            char[] buffer = new char[8 * 1024];
            if (logger.isTraceEnabled()) logger.trace("Using input stream " + reader + " for the stream transformation (start of pipeline).");
            while ((count = reader.read(buffer)) != -1) {
                String readInput = new String(buffer, 0, count);
                if (logger.isTraceEnabled()) logger.trace("Read " + readInput + " from reader " + reader + ", writing to writer " + writer);
                writer.write(buffer, 0, count);
            }
            writer.close();
        } catch (Throwable t) {
            logger.error("Error during pipeline thread execution.", t);
        }
    }

    @Override
    public void readFrom(DataInputStream in) throws IOException, IllegalAccessException, InstantiationException {
        super.readFrom(in);
        this.reader = null;
        this.readerForFaucet = null;
        this.writer = null;
        this.isRemoteTaskSide = true;
        boolean serializedRemoteDetailsExist = in.readBoolean();
        if (serializedRemoteDetailsExist) {
            int byteCount = in.readInt();
            this.remoteServerIP = new byte[byteCount];
            in.readFully(this.remoteServerIP);
            this.remoteServerPort = in.readInt();
        }
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        super.writeTo(out);
        boolean serializeRemoteDetails = !(sink instanceof IMuxSink) && writer != null;
        out.writeBoolean(serializeRemoteDetails);
        if (serializeRemoteDetails) {
            out.writeInt(writer.getServerHostIP().length);
            out.write(writer.getServerHostIP());
            out.writeInt(writer.getServerPort());
        }
    }
}
