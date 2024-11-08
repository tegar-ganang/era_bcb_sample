package edu.simplemqom;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import edu.simplemqom.objects.SMQOMChannel;

public class MessageBrokerController {

    private static MessageBrokerController instance = null;

    private HashSet<InetAddress> allowedList;

    private HashMap<String, SMQOMChannel> channels;

    private ExecutorService workersPool;

    private static int workersPoolSize = 8;

    private int nextClientHandlerThreadId = 0;

    private AtomicInteger newMessageId = new AtomicInteger(0);

    protected MessageBrokerController() {
        allowedList = null;
        workersPool = Executors.newFixedThreadPool(workersPoolSize);
        channels = new HashMap<String, SMQOMChannel>();
    }

    public static synchronized MessageBrokerController getInstance() {
        if (instance == null) {
            instance = new MessageBrokerController();
        }
        return instance;
    }

    public boolean ipValidation(InetAddress clientAddress) {
        InetSocketAddress clientSocketAddress;
        if (allowedList != null && allowedList.contains(clientAddress)) {
            System.out.println("Accepted connection from client: " + clientAddress.getHostAddress());
            return true;
        }
        System.out.println("Rejected connection from client: " + clientAddress.getHostAddress());
        return false;
    }

    /**
	 * @return the nextClientHandlerThreadId
	 */
    public synchronized int getNextClientHandlerThreadId() {
        return nextClientHandlerThreadId++;
    }

    public int getNewMessageId() {
        return newMessageId.incrementAndGet();
    }

    public void setNewMessageId(AtomicInteger newMessageId) {
        this.newMessageId = newMessageId;
    }

    /**
	 * @return the workersPoolSize
	 */
    public static int getWorkersPoolSize() {
        return workersPoolSize;
    }

    /**
	 * @param workersPoolSize the workersPoolSize to set
	 */
    public static synchronized boolean setWorkersPoolSize(int workersPoolSize) {
        if (instance == null) {
            MessageBrokerController.workersPoolSize = workersPoolSize;
            return true;
        }
        return false;
    }

    /**
	 * @return the workersPool
	 */
    protected ExecutorService getWorkersPool() {
        return workersPool;
    }

    public void executeWorker(Runnable worker) {
        workersPool.execute(worker);
    }

    /**
	 * @param allowedList the allowedList to set
	 */
    public void setAllowedList(HashSet<InetAddress> allowedList) {
        this.allowedList = allowedList;
    }

    /**
	 * @param key
	 * @return
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
    public SMQOMChannel getChannel(String name) {
        return channels.get(name);
    }

    public SMQOMChannel createChannel(int channelWorkersNr, String name) {
        SMQOMChannel newChannel = null;
        newChannel = new SMQOMChannel(channelWorkersNr, name);
        SMQOMChannel oldChannel = channels.put(name, newChannel);
        if (oldChannel == null) {
            return newChannel;
        } else {
            return newChannel;
        }
    }

    public void stopBroker() {
        System.out.println("Closing client handlers threads...");
        workersPool.shutdown();
        try {
            workersPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            MessageBroker.logger.warn("Interrupted when waiting for client acceptors shutdown.", e);
        }
        workersPool.shutdownNow();
    }

    public Set<String> getChannelSet() {
        return channels.keySet();
    }
}
