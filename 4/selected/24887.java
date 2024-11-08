package org.freelords.common.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.freelords.player.PlayerType;
import org.freelords.server.remote.Role;

/**
  * A class used for sending out requests over a network and returning the generated responses and
  * processing remote requests and sending the response back over the network.
  *
  * It should be pointed out that the NetworkNode has not been designed for multipoint
  * connections. I.e. the connection goes from each client to the server only.
  * There are various data items that especially the server uses extensively in association with
  * nodes, so this class also has a unique node id, a name for the computer/client/player
  * on the other end, and an AI type associated with this network connection.
  *
  * @author James Andrews
  */
public class NetworkNode implements RequestExecutor {

    /** A logger */
    private static final Logger LOG = Logger.getLogger(NetworkNode.class.getName());

    /**
	 * A thread responsible from reading requests and responses from the input stream
	 */
    private Thread readerThread;

    /**
	 * A thread that polls the writeQueue and sends it down the network.<br />
	 * This needs to be done in it's own thread, because even if you are careful with synchronization
	 * because Piped streams throw errors if a thread that has written to the stream dies before the stream is
	 * closed (to stop connections being open forever if the program has errored).
	 */
    private Thread writerTread;

    /**
	 * How to deal with remote requests, not to be confused with local requests which are to be
	 * executed on the other side of the network. 
	 */
    private RequestExecutor remoteRequestExecutor;

    /**
	 * A reference to the input stream before it's wrapped with an ObjectInputStream. Wrapping an
	 * empty input stream with an ObjectInputStream can cause it to block before read() is even invoked,
	 * so it is converted in the read thread.
	 */
    private InputStream plainIn;

    /**
	 * The input which provides requests and responses.
	 */
    private ObjectInputStream in;

    /**
	 * Where we send requests and responses.
	 */
    private ObjectOutputStream out;

    /**
	 * The nodeId of this network point, to be used for thread names which is handy in loggers and debuggers.
	 */
    private String nodeId;

    /**
	 * Computer name at the other end.
	 */
    private String computerName;

    /** The type of the player that is on the other side of this network connection. */
    private PlayerType type;

    /** The server can store the access privileges of the client here. */
    private Role privilege = Role.GUEST;

    /**
	 * The next id a request is going to use. This allows us to send of requests while the answer to the first
	 * request is still outstanding. Though you should be careful doing this as it is common for a single
	 * thread to be responsible for executing requests so the future requests might be blocked on the other end
	 * till the first request has completed.
	 */
    private AtomicInteger nextRequestId = new AtomicInteger();

    /**
	 * A map of requestId to a cubby-hole (SynchronousQueue which is a blocking queue with a capacity of 1) for the response.
	 * The cubby-hole is removed from the map as a value is put into it so only outstanding requests should be present in this map.
	 */
    private Map<Integer, SynchronousQueue<Response>> responseMap = Collections.synchronizedMap(new HashMap<Integer, SynchronousQueue<Response>>());

    /**
	 * A queue of items to write over the network which will block if it gets full.
	 */
    private BlockingQueue<Object> writeQueue = new ArrayBlockingQueue<Object>(20);

    /**
	 * ??
	 */
    private ThreadPoolExecutor remoteRequestThreadPool;

    /**
	 * ??
	 */
    private boolean passedHandshake;

    /**
	 * Constructs a network node that deals with requests and responses.<br />
	 * start() must be invoked before the object is capable of sending or receiving requests/responses.
	 * @param in  An input stream that should correspond to an output stream connected to another NetworkNode.
	 * @param out An output stream that should correspond to an input stream connected to another NetworkNode.
	 * @param remoteRequestExecutor Instance that actually processes the incoming requests.
	 * @param remoteRequestThreadPool A threadPoolExecutor for scheduling incoming requests. It would be useful for a server
	 * to provide a single PriorityBlockingQueue backed ThreadPoolExecutor across all NetworkNode.
	 * @param name The nodeId prefix for any threads required by this class.
	 * @throws IOException
	 */
    public NetworkNode(InputStream in, OutputStream out, RequestExecutor remoteRequestExecutor, ThreadPoolExecutor remoteRequestThreadPool, String name) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in cannot be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null.");
        }
        if (remoteRequestExecutor == null) {
            throw new IllegalArgumentException("remoteRequestExecutor can not be null.");
        }
        this.plainIn = in;
        this.out = new ObjectOutputStream(out);
        this.nodeId = name;
        this.type = PlayerType.HUMAN;
        this.remoteRequestExecutor = remoteRequestExecutor;
        this.remoteRequestThreadPool = remoteRequestThreadPool;
    }

    /**
	 * Creates a NetworkNode that can execute 1 request at a time and buffer an infinite amount
	 */
    public NetworkNode(InputStream in, OutputStream out, RequestExecutor remoteRequestExecutor, String name) throws IOException {
        this(in, out, remoteRequestExecutor, new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()), name);
    }

    /** Returns the name of the computer on the other end of the node. */
    public String getComputerName() {
        return computerName;
    }

    /** Sets the name of the computer (player name that appears in the lobby) */
    public void setComputerName(String computerName) {
        this.computerName = computerName;
        passedHandshake = true;
    }

    /** Returns true if the node has got a proper name. */
    public boolean isPassedHandshake() {
        return passedHandshake;
    }

    /**
	 * Begins threads required to read and write requests and responses.
	 */
    public void start() {
        if (readerThread != null) {
            throw new IllegalStateException("Network node already started.");
        }
        readerThread = new Thread(new Runnable() {

            public void run() {
                read();
            }
        }, nodeId + "(reader)");
        readerThread.start();
        writerTread = new Thread(new Runnable() {

            public void run() {
                write();
            }
        }, nodeId + "(writer)");
        writerTread.start();
    }

    /**
	 * Performed in the read thread.
	 */
    protected void read() {
        try {
            in = new ObjectInputStream(plainIn);
            while (true) {
                Object o;
                o = in.readObject();
                if (o instanceof Request) {
                    LOG.trace("received request " + o);
                    processRemoteRequest((Request) o);
                } else if (o instanceof Response) {
                    LOG.trace("received response " + o);
                    processRemoteResponse((Response) o);
                }
            }
        } catch (Throwable e) {
            LOG.error("the object reading thread has died", e);
        }
    }

    /**
	 * Performed in the write thread.
	 */
    protected void write() {
        try {
            while (true) {
                out.writeObject(writeQueue.take());
                out.reset();
                if (writeQueue.isEmpty()) {
                    LOG.trace("Flushing");
                    out.flush();
                }
            }
        } catch (Exception e) {
            LOG.error("the object writing thread has died", e);
        }
    }

    /**
	 * We just received a response from across the network. Presumably there is a thread that is
	 * blocking for this response.
	 * @param r The response
	 */
    protected void processRemoteResponse(Response r) throws InterruptedException {
        Object requestId = r.getMetaData().get("requestId");
        if (requestId == null || responseMap.get(requestId) == null) {
            throw new IllegalArgumentException("Received a response for (" + requestId + ") which does not correspond to any outstanding requests.");
        }
        responseMap.remove(requestId).put(r);
    }

    /**
	 * Queues the response to be sent to the other side of the network
	 * @param response The response to be sent
	 * @throws InterruptedException If the writeQueue blocking is interrupted
	 */
    protected void sendResponse(Response response) throws InterruptedException {
        writeQueue.put(response);
    }

    /**
	 * Processes a remote request in a separate thread to allow the read thread to continue.
	 * @param request
	 */
    protected void processRemoteRequest(final Request request) {
        request.getReceiverMetaData().clear();
        request.getReceiverMetaData().put("caller", this);
        remoteRequestThreadPool.execute(new Runnable() {

            public void run() {
                NDC.push(request.getSenderMetaData().get("requestId").toString());
                Response response = null;
                try {
                    startingRequestHandle(request);
                    LOG.trace("executing " + request);
                    response = new Response(remoteRequestExecutor.invoke(request));
                } catch (Throwable t) {
                    LOG.error("Error when executing request " + request, t);
                    response = new Response(null, t);
                }
                if (request.isWaitForAnswer()) {
                    response.getMetaData().put("requestId", request.getSenderMetaData().get("requestId"));
                    try {
                        sendResponse(response);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        completedRequestHandle(request);
                    }
                }
                NDC.pop();
            }
        });
    }

    /** Creates the request, sends it over the network and returns the response
	  * or throws an error if a problem occurs.
	  */
    public Object invoke(Request request) throws Throwable {
        int requestId = nextRequestId.getAndIncrement();
        request.getSenderMetaData().put("requestId", requestId);
        SynchronousQueue<Response> responseHold = null;
        if (request.isWaitForAnswer()) {
            responseHold = new SynchronousQueue<Response>();
            responseMap.put(requestId, responseHold);
        }
        writeQueue.put(request);
        LOG.trace("sent request " + request);
        if (request.isWaitForAnswer()) {
            Response response = responseHold.take();
            if (response.getThrowable() != null) {
                throw response.getThrowable();
            } else {
                return response.getResponseValue();
            }
        } else {
            return null;
        }
    }

    /** Function called before an incoming request is handled (empty skeleton). */
    private void startingRequestHandle(Request r) {
    }

    /** Function called after an incoming request has been handled (empty skeleton). */
    private void completedRequestHandle(Request r) {
    }

    /** Returns the node id */
    public String getNodeId() {
        return nodeId;
    }

    public PlayerType getType() {
        return type;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }

    public Role getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Role privilege) {
        this.privilege = privilege;
    }
}
