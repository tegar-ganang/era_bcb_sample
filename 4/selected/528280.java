package jade.core.messaging;

import java.io.IOException;
import jade.core.AID;
import jade.core.Profile;
import jade.util.leap.List;
import jade.util.leap.LinkedList;
import jade.util.leap.Map;
import jade.util.leap.HashMap;
import jade.util.leap.Iterator;

/**
 * This class supports the ACL persistent delivery service, managing
 * actual ACL messages storage, scheduled message delivery and other
 * utility tasks related to the service.
 *
 * @author  Giovanni Rimassa - FRAMeTech s.r.l.
 * @author  Nicolas Lhuillier - Motorola
 *
 */
class PersistentDeliveryManager {

    public static synchronized PersistentDeliveryManager instance(Profile p, MessageManager.Channel ch) {
        if (theInstance == null) {
            theInstance = new PersistentDeliveryManager();
            theInstance.initialize(p, ch);
        }
        return theInstance;
    }

    private static final long DEFAULT_SENDFAILUREPERIOD = 60 * 1000;

    private static final String FILE_STORAGE_SHORTCUT = "file";

    private static final String DEFAULT_STORAGE = "jade.core.messaging.PersistentDeliveryManager$DummyStorage";

    private static final String FILE_STORAGE = "jade.core.messaging.FileMessageStorage";

    private static class DeliveryItem {

        public DeliveryItem(GenericMessage msg, AID id, MessageManager.Channel ch, String sid) {
            toDeliver = msg;
            receiver = id;
            channel = ch;
            storeName = sid;
        }

        public GenericMessage getMessage() {
            return toDeliver;
        }

        public AID getReceiver() {
            return receiver;
        }

        public MessageManager.Channel getChannel() {
            return channel;
        }

        public String getStoreName() {
            return storeName;
        }

        private GenericMessage toDeliver;

        private AID receiver;

        private MessageManager.Channel channel;

        private String storeName;
    }

    private class ExpirationChecker implements Runnable {

        public ExpirationChecker(long t) {
            period = t;
            myThread = new Thread(this, "Persistent Delivery Service -- Expiration Checker Thread");
        }

        public void run() {
            while (active) {
                try {
                    Thread.sleep(period);
                    synchronized (pendingMessages) {
                        Object[] keys = pendingMessages.keySet().toArray();
                        for (int i = 0; i < keys.length; i++) {
                            flushMessages((AID) keys[i]);
                        }
                    }
                } catch (InterruptedException ie) {
                }
            }
        }

        public void start() {
            active = true;
            myThread.start();
        }

        public void stop() {
            active = false;
            myThread.interrupt();
        }

        private boolean active = false;

        private long period;

        private Thread myThread;
    }

    public static class DummyStorage implements MessageStorage {

        public void init(Profile p) {
        }

        public String store(GenericMessage msg, AID receiver) throws IOException {
            return null;
        }

        public void delete(String storeName, AID receiver) throws IOException {
        }

        public void loadAll(LoadListener il) throws IOException {
        }
    }

    public void initialize(Profile p, MessageManager.Channel ch) {
        users = 0;
        myMessageManager = MessageManager.instance(p);
        deliveryChannel = ch;
        try {
            String storageClass = p.getParameter(PersistentDeliveryService.PERSISTENT_DELIVERY_STORAGEMETHOD, DEFAULT_STORAGE);
            if (FILE_STORAGE_SHORTCUT.equalsIgnoreCase(storageClass)) {
                storageClass = FILE_STORAGE;
            }
            storage = (MessageStorage) Class.forName(storageClass).newInstance();
            storage.init(p);
            storage.loadAll(new MessageStorage.LoadListener() {

                public void loadStarted(String storeName) {
                    System.out.println("--> Load BEGIN <--");
                }

                public void itemLoaded(String storeName, GenericMessage msg, AID receiver) {
                    synchronized (pendingMessages) {
                        List msgs = (List) pendingMessages.get(receiver);
                        if (msgs == null) {
                            msgs = new LinkedList();
                            pendingMessages.put(receiver, msgs);
                        }
                        DeliveryItem item = new DeliveryItem(msg, receiver, deliveryChannel, storeName);
                        msgs.add(item);
                    }
                    System.out.println("Message for <" + receiver.getLocalName() + ">");
                }

                public void loadEnded(String storeName) {
                    System.out.println("--> Load END <--");
                }
            });
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendFailurePeriod = DEFAULT_SENDFAILUREPERIOD;
        String s = p.getParameter(PersistentDeliveryService.PERSISTENT_DELIVERY_SENDFAILUREPERIOD, null);
        if (s != null) {
            try {
                sendFailurePeriod = Long.parseLong(s);
            } catch (NumberFormatException nfe) {
            }
        }
    }

    public void storeMessage(String storeName, GenericMessage msg, AID receiver) throws IOException {
        synchronized (pendingMessages) {
            List msgs = (List) pendingMessages.get(receiver);
            if (msgs == null) {
                msgs = new LinkedList();
                pendingMessages.put(receiver, msgs);
            }
            String tmpName = storage.store(msg, receiver);
            msgs.add(new DeliveryItem(msg, receiver, deliveryChannel, tmpName));
        }
    }

    public int flushMessages(AID receiver) {
        int cnt = 0;
        List l = null;
        synchronized (pendingMessages) {
            l = (List) pendingMessages.remove(receiver);
        }
        if (l != null) {
            Iterator it = l.iterator();
            while (it.hasNext()) {
                DeliveryItem item = (DeliveryItem) it.next();
                retry(item);
                cnt++;
            }
        }
        return cnt;
    }

    public synchronized void start() {
        if (users == 0) {
            failureSender = new ExpirationChecker(sendFailurePeriod);
            failureSender.start();
        }
        users++;
    }

    public synchronized void stop() {
        users--;
        if (users == 0) {
            failureSender.stop();
        }
    }

    private static PersistentDeliveryManager theInstance;

    private PersistentDeliveryManager() {
    }

    private void retry(DeliveryItem item) {
        try {
            storage.delete(item.getStoreName(), item.getReceiver());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        myMessageManager.deliver(item.getMessage(), item.getReceiver(), item.getChannel());
    }

    private MessageManager myMessageManager;

    private MessageManager.Channel deliveryChannel;

    private long sendFailurePeriod;

    private long users;

    private Map pendingMessages = new HashMap();

    private ExpirationChecker failureSender;

    private MessageStorage storage;
}
