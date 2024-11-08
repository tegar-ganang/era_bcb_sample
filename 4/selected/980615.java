package com.safi.server.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.apache.mina.common.IoSession;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.AgiServer;
import org.asteriskjava.fastagi.MappingStrategy;
import org.asteriskjava.fastagi.command.VerboseCommand;
import org.asteriskjava.fastagi.internal.AgiReader;
import org.asteriskjava.fastagi.internal.AgiWriter;
import org.asteriskjava.util.Log;
import org.asteriskjava.util.LogFactory;
import com.safi.server.mina.AgiChannelMinaImpl;
import com.safi.server.mina.AgiReaderMinaImpl;
import com.safi.server.mina.AgiWriterMinaImpl;

/**
 * A simplistic HTTP protocol handler that replies back the URL and headers
 * which a client requested.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 555855 $, $Date: 2008/05/29 07:04:20 $
 */
public class AgiProtocolHandler extends org.apache.mina.handler.stream.StreamIoHandler {

    private MappingStrategy mappingStrategy;

    private SafiMinaServer agiServer;

    public AgiProtocolHandler(SafiMinaServer safiMinaServer, MappingStrategy mappingStrategy) {
        this.mappingStrategy = mappingStrategy;
        this.agiServer = safiMinaServer;
    }

    protected void processStreamIo(IoSession session, InputStream in, OutputStream out) {
        this.agiServer.execute(new Worker(this.mappingStrategy, session, in, out));
    }

    private static class Worker implements Runnable {

        private static final String AJ_AGISTATUS_VARIABLE = "AJ_AGISTATUS";

        private static final String AJ_AGISTATUS_NOT_FOUND = "NOT_FOUND";

        private static final String AJ_AGISTATUS_SUCCESS = "SUCCESS";

        private static final String AJ_AGISTATUS_FAILED = "FAILED";

        private final Log logger = LogFactory.getLog(getClass());

        private InputStream in;

        private OutputStream out;

        private IoSession session;

        private MappingStrategy mappingStrategy;

        protected AgiReader createReader() {
            return new AgiReaderMinaImpl(in, (InetSocketAddress) session.getLocalAddress(), (InetSocketAddress) session.getRemoteAddress());
        }

        protected AgiWriter createWriter() {
            return new AgiWriterMinaImpl(out);
        }

        private void runScript(AgiScript script, AgiRequest request, AgiChannel channel) {
            String threadName;
            threadName = Thread.currentThread().getName();
            logger.info("Begin AgiScript " + script.getClass().getName() + " on " + threadName);
            try {
                script.service(request, channel);
                setStatusVariable(channel, AJ_AGISTATUS_SUCCESS);
            } catch (AgiException e) {
                logger.error("AgiException running AgiScript " + script.getClass().getName() + " on " + threadName, e);
                setStatusVariable(channel, AJ_AGISTATUS_FAILED);
            } catch (Exception e) {
                logger.error("Exception running AgiScript " + script.getClass().getName() + " on " + threadName, e);
                setStatusVariable(channel, AJ_AGISTATUS_FAILED);
            }
            logger.info("End AgiScript " + script.getClass().getName() + " on " + threadName);
        }

        private void setStatusVariable(AgiChannel channel, String value) {
            if (channel == null) {
                return;
            }
            try {
                channel.setVariable(AJ_AGISTATUS_VARIABLE, value);
            } catch (Exception e) {
            }
        }

        private void logToAsterisk(AgiChannel channel, String message) {
            if (channel == null) {
                return;
            }
            try {
                channel.sendCommand(new VerboseCommand(message, 1));
            } catch (Exception e) {
            }
        }

        public Worker(MappingStrategy mappingStrategy, IoSession session, InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            this.session = session;
            this.mappingStrategy = mappingStrategy;
        }

        public void run() {
            AgiChannel channel = null;
            try {
                AgiReader reader;
                AgiWriter writer;
                AgiRequest request;
                AgiScript script;
                reader = createReader();
                writer = createWriter();
                request = reader.readRequest();
                channel = new AgiChannelMinaImpl(request, writer, reader);
                script = mappingStrategy.determineScript(request);
                if (script == null) {
                    final String errorMessage;
                    errorMessage = "No script configured for URL '" + request.getRequestURL() + "' (script '" + request.getScript() + "')";
                    logger.error(errorMessage);
                    setStatusVariable(channel, AJ_AGISTATUS_NOT_FOUND);
                    logToAsterisk(channel, errorMessage);
                } else {
                    runScript(script, request, channel);
                }
            } catch (AgiException e) {
                setStatusVariable(channel, AJ_AGISTATUS_FAILED);
                logger.error("AgiException while handling request", e);
            } catch (Exception e) {
                setStatusVariable(channel, AJ_AGISTATUS_FAILED);
                logger.error("Unexpected Exception while handling request", e);
            } finally {
                try {
                    this.in.close();
                    this.out.close();
                    this.session.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
