package org.simpleframework.http.core;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import org.simpleframework.http.Pipeline;
import org.simpleframework.http.transport.Transport;
import org.simpleframework.http.transport.SocketTransport;
import org.simpleframework.util.select.Operation;

/**
 * The <code>Negotiation</code> object is used to perform secure SSL
 * negotiations on a pipeline or <code>Transport</code>. This can
 * be used to perform either an SSL handshake or termination. To
 * perform the negotiation this uses an SSL engine provided with the
 * pipeliine to direct the conversation. The SSL engine tells the
 * negotiation what is expected next, whether this is a response to
 * the client or a message from it. During the negotiation this may
 * need to wait for either a write ready event or a read ready 
 * event. Event notification is done using the negotiatior provided.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.Negotiator
 */
abstract class Negotiation implements Operation {

    /**
    * This is the socket channel used to read and write data to.
    */
    protected SocketChannel channel;

    /**
    * This is the negotiator used to dispatch the transport to.
    */
    protected Negotiator negotiator;

    /**
    * This is the transport dispatched when the negotiation ends.
    */
    protected Transport transport;

    /**
    * This is the output buffer used to generate data to.
    */
    protected ByteBuffer output;

    /**
    * This is the input buffer used to read data from the socket.
    */
    protected ByteBuffer input;

    /**
    * This is an empty byte buffer used to generate a response.
    */
    protected ByteBuffer empty;

    /**
    * This is the SSL engine used to direct the conversation.
    */
    protected SSLEngine engine;

    /**
    * This determines whether the negotiation has been configured.
    */
    protected boolean configured;

    /**
    * This determines whether the negotiation has finished.
    */
    protected boolean ready;

    /**
    * This is used to determine whether the negotiation should close.
    */
    protected boolean close;

    /**
    * Constructor for the <code>Negotiation</code> object. This is
    * used to create an operation capable of performing negotiations
    * for SSL connections. Typically this is used to perform request
    * response negotiations, such as a handshake or termination.
    *
    * @param negotiator the negotiator used to check socket events
    * @param pipeline the pipeline to perform the negotiation for
    */
    public Negotiation(Pipeline pipeline, Negotiator negotiator) throws Exception {
        this(new SecureTransport(pipeline, negotiator), negotiator);
    }

    /**
    * Constructor for the <code>Negotiation</code> object. This is
    * used to create an operation capable of performing negotiations
    * for SSL connections. Typically this is used to perform request
    * response negotiations, such as a handshake or termination.
    *
    * @param negotiator the negotiator used to check socket events
    * @param transport the transport to perform the negotiation for
    */
    public Negotiation(Transport transport, Negotiator negotiator) throws Exception {
        this(transport, negotiator, 20480);
    }

    /**
    * Constructor for the <code>Negotiation</code> object. This is
    * used to create an operation capable of performing negotiations
    * for SSL connections. Typically this is used to perform request
    * response negotiations, such as a handshake or termination.
    *
    * @param negotiator the negotiator used to check socket events
    * @param transport the transport to perform the negotiation for
    * @param size the size of the buffers used for the negotiation
    */
    public Negotiation(Transport transport, Negotiator negotiator, int size) throws Exception {
        this.output = ByteBuffer.allocate(size);
        this.input = ByteBuffer.allocate(size);
        this.channel = transport.getSocket();
        this.engine = transport.getEngine();
        this.empty = ByteBuffer.allocate(0);
        this.negotiator = negotiator;
        this.transport = transport;
    }

    /**
    * This returns the socket channel for the connected pipeline. It
    * is this channel that is used to determine if there are bytes
    * that can be read. When closed this is no longer selectable.
    *
    * @return this returns the connected channel for the pipeline
    */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence. This may be executed several times 
    * depending on whether reading or writing blocks.
    */
    public void run() {
        try {
            negotiate();
        } catch (Exception e) {
            cancel();
        }
    }

    /**
    * This is used to terminate the negotiation. This is excecuted
    * when the negotiation times out. When the negotiation expires it
    * is rejected by the negotiator and must be canceled. Canceling
    * is basically termination of the connection to free resources.
    */
    public void cancel() {
        try {
            channel.close();
        } catch (Throwable e) {
            return;
        }
    }

    /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence.
    */
    public void negotiate() throws Exception {
        try {
            int ready = handshake();
            if (close) {
                channel.close();
            } else if (ready != -1) {
                negotiator.process(this, ready);
            } else {
                commit();
            }
        } catch (Exception e) {
            cancel();
        }
    }

    /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence. Here we configured the negotiation if
    * it has not already been configued.
    *
    * @return the I/O operation that the negotiation requires next
    */
    private int handshake() throws Exception {
        if (!configured) {
            configure();
            configured = true;
        }
        if (!ready) {
            write();
        }
        while (!ready) {
            if (!read()) {
                return OP_READ;
            }
            if (!write()) {
                return OP_WRITE;
            }
        }
        return -1;
    }

    /**
    * This is used to perform the read part of the negotiation. The
    * read part is where the client sends information to the server
    * and the server consumes the data and determines what action 
    * to take. Typically it is the SSL engine that determines what
    * action is to be taken depending on the client data.
    *
    * @return true if the read part of the negotiation has finished
    */
    private boolean read() throws Exception {
        boolean available = fill();
        while (available) {
            SSLEngineResult result = engine.unwrap(input, output);
            HandshakeStatus status = result.getHandshakeStatus();
            switch(status) {
                case FINISHED:
                case NOT_HANDSHAKING:
                case NEED_WRAP:
                    return clear();
                case NEED_UNWRAP:
                    available = available();
                case NEED_TASK:
                    execute();
            }
        }
        clear();
        return false;
    }

    /**
    * This is used to determine if there are any buffered bytes that
    * can be used within the negotiation. Typically this is used to
    * determine if the SSL engine was consumed all the data that was
    * sent by the client, if not then the read part is not complete.
    *
    * @return this returns true if there is input to be processed
    */
    private boolean available() throws Exception {
        boolean left = input.hasRemaining();
        if (!left) {
            input.clear();
        }
        return left;
    }

    /**
    * This is used to clear out the input buffer so that is can be
    * used in another operation. The input buffer will be used many
    * times depending on how many messages it receives from the 
    * client taking part in the negotiation.
    *
    * @return true if the input buffer was cleared successfully
    */
    private boolean clear() throws Exception {
        int ready = input.position();
        if (ready > 0) {
            input.clear();
        }
        return true;
    }

    /**
    * This is used to fill the input buffer. The input buffer is 
    * filled with data sent from the client participating in the
    * negotiation. This data is then used to perform the next step
    * in the handskake or termination of the SSL connection.
    *
    * @param this is used to fill the input buffer with data
    */
    private boolean fill() throws Exception {
        int count = channel.read(input);
        if (count < 0) {
            throw new NegotiationException("Channel closed");
        }
        if (count > 0) {
            input.flip();
        }
        return count > 0;
    }

    /**
    * This is used to perform the write part of the negotiation. The
    * read part is where the server sends information to the client
    * and the client interprets the data and determines what action 
    * to take. After a write the negotiation typically completes or
    * waits for the next response from the client.
    *
    * @return true if the write part of the negotiation has finished
    */
    private boolean write() throws Exception {
        while (true) {
            SSLEngineResult result = engine.wrap(empty, output);
            HandshakeStatus status = result.getHandshakeStatus();
            switch(status) {
                case FINISHED:
                case NOT_HANDSHAKING:
                    ready = true;
                case NEED_UNWRAP:
                    return flush();
                case NEED_TASK:
                    execute();
            }
        }
    }

    /**
    * This is used to flush the data within the  output buffer. This
    * is done when all the data required by the SSL response is done.
    * Once this method is invoked the handshake response is sent and
    * the negotiation waits for the next client message to arrive.
    *
    * @return this returns true if all the response data is flushed
    */
    private boolean flush() throws Exception {
        int ready = output.position();
        if (ready > 0) {
            output.flip();
        } else {
            return true;
        }
        int count = channel.write(output);
        if (count == ready) {
            output.clear();
        }
        return count == ready;
    }

    /**
    * This is used to execute the delegated tasks. These tasks are
    * used to digest the information received from the client in
    * order to generate a response. This may need to execute several
    * tasks from the associated SSL engine.
    */
    private void execute() throws Exception {
        while (true) {
            Runnable runnable = engine.getDelegatedTask();
            if (runnable == null) {
                break;
            }
            runnable.run();
        }
    }

    /**
    * This is used to configure the negotiation before any of the
    * data is sent. Configuration is different depending on the
    * type of negotiation. For instance an SSL handshake needs to
    * know which side of the negotiation it is on, for instance 
    * the server side or client side of the negotiation.    
    */
    public abstract void configure() throws Exception;

    /**
    * This is used to terminate the negotiation. This is excecuted
    * if either the negotiation completes successfully or if there
    * is an error within the negotiation. Implementations should
    * override this and provide a reasonable action.
    */
    public abstract void commit() throws Exception;
}
