package com.googlecode.acpj.services;

import com.googlecode.acpj.actors.Actor;
import com.googlecode.acpj.actors.ActorFactory;
import com.googlecode.acpj.channels.BufferedChannel;
import com.googlecode.acpj.channels.ChannelException;
import com.googlecode.acpj.channels.ChannelFactory;
import com.googlecode.acpj.channels.ChannelPoisonedException;
import com.googlecode.acpj.channels.ChannelRegistry;
import com.googlecode.acpj.channels.ReadPort;

/**
 * <p>
 * Captures the notion of a service which is a particular Actor pattern:
 * </p>
 * <ul>
 *   <li>Starts early on in application initialization and effectively runs for
 *       ever.</li>
 *   <li>Has a buffered, any-to-one <em>request queue</em> which clients use to
 *       send requests.</li>
 *   <li>This queue is registered for clients to discover.</li>
 *   <li>The service is stopped when the request queue is poisoned.</li>
 * </ul>
 * <p>
 * The BasicService class therefore handles the creation and registering of the 
 * notification channel, creation of the service Actor and the base run-loop
 * implementation. This means that many subclasses will only need to implement
 * the {@link #handleRequest(Object)} method to process each request read by
 * the service from the request queue. As an example of if it's use consider 
 * the included simple LogService:
 * </p>
 * <pre>
 *    public class LogService extends BasicService<LogRecord> {
 *        
 *        public static final String CHANNEL_NAME = "com.googlecode.acpj.services.RequestChannel";
 *        
 *        private Logger logger = null;
 *    
 *        public LogService() {
 *            setChannelName(CHANNEL_NAME);
 *        }
 *    
 *        public void startup() {
 *            this.logger = Logger.getLogger("com.googlecode.acpj.services.logger");
 *            super.startup();
 *        }
 *        
 *        public boolean handleRequest(LogRecord request) {
 *            this.logger.log(request);
 *            return true;
 *        }
 *    }
 * </pre>
 * <p>
 * Starting and stopping the service and managing registration of the request queue and so
 * forth are handled by this class, in fact the <code>setChannelName</code> and
 * <code>setActorName</code> are not necessary in the constructor as default names will
 * be chosen if not supplied. 
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public abstract class BasicService<RT> implements Runnable {

    private String channelName = null;

    private String actorName = null;

    private BufferedChannel<RT> requestChannel = null;

    private Actor serviceActor = null;

    private Object serviceLock = new Object();

    private ReadPort<RT> requestPort = null;

    /**
	 * Default constructor, allows framework creation of services.
	 */
    public BasicService() {
    }

    /**
	 * Return the request channel registered name.
	 *  
	 * @return the channel name as used in the channel registry.
	 */
    public String getChannelName() {
        return this.channelName;
    }

    /**
	 * Set the channel registered name. Note that channels are only 
	 * registered during {@link #start()}, if you call this method
	 * when the service is running nothing will happen.
	 * 
	 * @param channelName the new channel name.
	 */
    protected void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
	 * Return the logical name of the service actor.
	 * 
	 * @return the name of the service actor.
	 */
    public String getActorName() {
        return this.actorName;
    }

    /**
	 * Set the logical name of the service actor. As actors are only
	 * created during {@link #start()}, if you call this method
	 * when the service is running nothing will happen.
	 * 
	 * @param actorName the new name
	 */
    protected void setActorName(String actorName) {
        this.actorName = actorName;
    }

    /**
	 * Retrieve the read port for the request channel.
	 * 
	 * @return the read port connected to the service request channel.
	 */
    protected ReadPort<RT> getReadPort() {
        return this.requestPort;
    }

    /**
	 * Retrieve the next request message from the service request channel.
	 * 
	 * @return the next request message to process.
	 * 
	 * @throws ChannelException
	 */
    protected RT getNextRequest() throws ChannelException {
        return this.requestPort.read();
    }

    /**
	 * Start the service, note that this will do nothing if the service
	 * is already running.
	 * 
	 * @return the instance of the actor actually performing this service.
	 */
    public Actor start() {
        synchronized (this.serviceLock) {
            if (this.serviceActor == null || !this.serviceActor.isRunning()) {
                if (this.channelName == null) {
                    this.channelName = this.getClass().getCanonicalName();
                }
                if (this.actorName == null) {
                    this.actorName = this.getClass().getCanonicalName();
                }
                this.requestChannel = ChannelFactory.getInstance().createAnyToOneChannel(this.channelName, BufferedChannel.BUFFER_CAPACITY_UNLIMITED);
                ChannelRegistry.getInstance().register(this.requestChannel, this.channelName, true);
                this.requestPort = this.requestChannel.getReadPort(false);
                this.serviceActor = ActorFactory.getInstance().createActor(this, this.actorName);
            }
        }
        return this.serviceActor;
    }

    /**
	 * Determines whether this service is actually running.
	 * 
	 * @return <code>true</code> if the service actor is running.
	 */
    public boolean isRunning() {
        return (this.serviceActor != null || this.serviceActor.isRunning());
    }

    /**
	 * Stop the service, note that this will do nothing if the service is not
	 * currently running.
	 */
    public void stop() {
        synchronized (this.serviceLock) {
            if (this.serviceActor != null && this.serviceActor.isRunning()) {
                ChannelRegistry.getInstance().deregister(this.channelName);
                this.requestChannel.poison();
                this.requestChannel = null;
                this.serviceActor = null;
            }
        }
    }

    /**
	 * Subclasses should override this to provide logic before the main run-loop
	 * initializes.
	 */
    public void startup() {
    }

    /**
	 * Subclasses must override this to handle each request messages read from the 
	 * request channel. The handler returns <code>true</code> if the service should
	 * continue to read requests, <code>false</code> and the service will shut down. 
	 * 
	 * @param request the request read from the channel.
	 * 
	 * @return <code>true</code> and the service will continue to read requests.
	 */
    public abstract boolean handleRequest(RT request);

    /**
	 * Subclasses should override this to provide logic after the main run-loop
	 * terminates and before the Actor completes.
	 * 
	 * @param poisoned will be <code>true</code> if the service is exiting due
	 *        to the request channel being poisoned.
	 */
    public void shutdown(boolean poisoned) {
    }

    /**
	 * This is the actual service method that will process request messages.
	 */
    public void run() {
        startup();
        try {
            getReadPort().claim();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        boolean running = true;
        while (running) {
            RT request = null;
            try {
                request = getNextRequest();
            } catch (ChannelPoisonedException e) {
                break;
            } catch (Throwable t) {
                t.printStackTrace();
            }
            running = handleRequest(request);
        }
        if (!running) {
            getReadPort().close();
        }
        shutdown(this.requestChannel.isPoisoned());
    }
}
