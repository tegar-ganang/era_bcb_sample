package pyrasun.eio;

import pyrasun.eio.handlers.EIOEventHandler;
import pyrasun.eio.handlers.EIOEventHandlerFactory;
import pyrasun.eio.util.Logger;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;

/**
 * An EndpointCoordinator is used coordinate activity for a given type of endpoint.  A bit less cryptically, 
 * the EndpointCoordinator receives I/O events from the EIOEventManager for specific
 * endpoints, and routes them to the appropriate workers.  It's also the centralized location
 * where a user tells EIO how to handle a given type of Endpoint.
 *
 * If it helps - you could probably change the name of this to EndpointType, to indicate that
 * it represents a specific type or flavor of Endpoint.  In fact, it used to be called that.
 *
 * In fact, I really don't know _what_ to call this.  I'll send a free copy of the EmberIO
 * source to anyone who comes up with a really effective name!
 */
public class EndpointCoordinator {

    private WorkerController controller;

    private String name;

    private String stringRep;

    private EIOPoolingStrategy strategy;

    private EIOContext context;

    private Logger log;

    private Map endpoints = Collections.synchronizedMap(new HashMap());

    private boolean alive = false;

    private boolean enableDirectWrites = false;

    /**
   * Construct a new EndpointCoordinator.
   *
   * @param context the global context this coordinator will be running within
   * @param name a wetware readable name for this component
   * @param factory A factory to use to construct workers.  These workers are used to service events fired against endpoints of this type.
   * @param strategy the strategy used to handle endpoints of this type.
   */
    public EndpointCoordinator(EIOContext context, final String name, final EIOWorkerFactory factory, EIOPoolingStrategy strategy) {
        this.name = name;
        this.context = context;
        this.strategy = strategy;
        log = context.getLogger(this);
        stringRep = "EndpointCoordinator: " + name;
        if (strategy.getEventDescriptor(EIOEvent.READ).useUniqueThread()) {
            enableDirectWrites = true;
            EIOEventDescriptor read = strategy.getEventDescriptor(EIOEvent.READ);
            read.setUseBlockingIO(true);
            EIOEventDescriptor write = strategy.getEventDescriptor(EIOEvent.WRITE);
            write.setUseBlockingIO(true);
        }
        controller = new WorkerController(this, factory, strategy);
        log.info("Created " + this + " with strategy " + strategy);
        context.registerCoordinator(this);
    }

    /**
   * Get the global context associated with this EndpointCoordinator.
   */
    public EIOContext getContext() {
        return (context);
    }

    /**
   * Return an event descriptor that defines this coordinator's pooling strategy for a
   * given event.
   */
    public EIOEventDescriptor getEventDescriptor(EIOEvent event) {
        return (strategy.getEventDescriptor(event));
    }

    /**
   * Start this EndpointCoordinator - generally initializes all of the worker threads
   * associated with this coordinator.  Your endpoints won't do anything until you call
   * start() on them.  NOTE: EIOEventManger will generally call this for you, so long
   * as you add your endpoints to the EIOEventManager you should be golden.  Just don't
   * forget to call start() on the EIOEventManager!
   */
    public synchronized void start() {
        if (alive) {
            return;
        }
        controller.start();
        Iterator iter = endpoints.values().iterator();
        while (iter.hasNext()) {
            Endpoint endpoint = (Endpoint) iter.next();
            registerForEvents(endpoint);
        }
        alive = true;
    }

    /**
   * Stop this EndpointCoordinator - generally stops all worker threads.
   */
    public synchronized void stop() {
        if (!alive) {
            return;
        }
        Iterator iter = endpoints.values().iterator();
        while (iter.hasNext()) {
            Endpoint endpoint = (Endpoint) iter.next();
            endpoint.close(EIOReasonCode.SHUTDOWN);
        }
        controller.stop();
    }

    /**
   * Get the event manager associated with this coordinator
   */
    public EIOEventManager getEventManager() {
        return (context.getEventManager());
    }

    public void handleEvent(Endpoint endpoint) {
        try {
            controller.handleEvent(endpoint);
        } catch (IOException ioe) {
            endpoint.close(EIOReasonCode.UNSPECIFIED, ioe);
        }
    }

    /**
   * Register the given endpoint to receive events. Uses the endpoint's
   * ready indicators to figure out which events exactly.
   *
   * End users shouldn't touch this - bad baby, bad bad baby!!!
   */
    public void registerForEvents(Endpoint endpoint) {
        if (endpoint.getNIOChannel() == null) {
            try {
                throw new Exception("Fuck me: " + endpoint);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (enableDirectWrites && endpoint.getState() == EPStatus.LIMBO) {
            System.out.println("Adding direct writer handler to endpoint " + endpoint);
            EIOEventHandler writeHandler = controller.getInitializedHandler(EIOEvent.WRITE);
            writeHandler.setIsDedicatedThread(true);
            endpoint.enableDirectWrites(writeHandler);
        }
        if (endpoint.getState() == EPStatus.LIMBO) {
            endpoint.gotoWaitState();
        }
        EIOEventManager em = endpoint.getEventManager();
        if (em == null) {
            em = getEventManager();
            endpoint.setEventManager(em);
        }
        if (endpoint.isOpen() && endpoint.getNIOInterestEvents() != 0 && endpoint.isSelectorized()) {
            em.registerForEvents(endpoint);
        }
    }

    /**
   * Only here for debugging purposes
   */
    void addEndpoint(Endpoint endpoint) {
        endpoints.put(endpoint, endpoint);
    }

    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove(endpoint);
    }

    /**
   * Only here for debugging purposes
   */
    public void dumpEndpoints() {
        StringBuffer sb = new StringBuffer();
        sb.append(toString() + "\n");
        sb.append("-----------------------------------------------\n");
        Iterator iter = endpoints.values().iterator();
        while (iter.hasNext()) {
            Endpoint endpoint = (Endpoint) iter.next();
            sb.append("\t" + endpoint.toString() + "\n" + "\t\t" + endpoint.masksAsString() + "\n");
            sb.append(endpoint.dumpProcessedStats("\t\t"));
            if (endpoint instanceof ReadWriteEndpoint) {
                ReadWriteEndpoint rwEndpoint = (ReadWriteEndpoint) endpoint;
                if (rwEndpoint.getWriteQueueDepth() > 0) {
                    sb.append("\t\t" + rwEndpoint.getWriteQueueDepth() + " writes pending\n");
                }
                if (rwEndpoint.getProcessingQueueDepth() > 0) {
                    sb.append("\t\t" + rwEndpoint.getProcessingQueueDepth() + " process pending\n");
                }
            }
        }
        System.out.println(sb.toString());
    }

    /**
   * Get the wetware readable name for this coordinator.
   */
    public String getName() {
        return (name);
    }

    public String toString() {
        return (stringRep);
    }
}
