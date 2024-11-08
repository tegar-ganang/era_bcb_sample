package net.sf.syracuse.request.impl;

import net.sf.syracuse.net.NetworkEventThread;
import net.sf.syracuse.request.Command;
import net.sf.syracuse.request.EncoderException;
import net.sf.syracuse.request.RequestException;
import net.sf.syracuse.request.RequestGlobals;
import net.sf.syracuse.request.RequestServicer;
import net.sf.syracuse.request.ResultEncoder;
import net.sf.syracuse.threads.ThreadStateManager;
import org.apache.commons.logging.Log;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.Map;

public class Responder implements RequestServicer {

    private Log log;

    private NetworkEventThread networkEventThread;

    private Map<String, ResultEncoder> resultEncoders;

    private RequestGlobals requestGlobals;

    private ThreadStateManager threadStateManager;

    public void service() throws RequestException {
        Command command = requestGlobals.getCommand();
        ResultEncoder encoder = resultEncoders.get(command.getResponseEncoding());
        if (encoder == null) {
            throw new RequestException("Unknown response encoding: " + command.getResponseEncoding());
        }
        String results;
        try {
            results = encoder.encode(requestGlobals.getResult());
        } catch (EncoderException e) {
            log.error("Error encoding results", e);
            throw new RequestException("Error encoding results", e);
        }
        log.debug("Responding with: " + results);
        threadStateManager.setThreadStateAttachment(results);
        ByteBuffer buffer = Charset.defaultCharset().encode(results);
        requestGlobals.getNetworkRequest().storeResponseBuffer(buffer);
        networkEventThread.addChannelInterestOps(requestGlobals.getNetworkRequest().getChannel(), SelectionKey.OP_WRITE);
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setNetworkEventThread(NetworkEventThread networkEventThread) {
        this.networkEventThread = networkEventThread;
    }

    public void setResultEncoders(Map<String, ResultEncoder> resultEncoders) {
        this.resultEncoders = resultEncoders;
    }

    public void setRequestGlobals(RequestGlobals requestGlobals) {
        this.requestGlobals = requestGlobals;
    }

    public void setThreadStateManager(ThreadStateManager threadStateManager) {
        this.threadStateManager = threadStateManager;
    }
}
