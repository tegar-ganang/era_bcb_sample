package fr.esrf.TangoApi.events;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.CallBack;
import fr.esrf.TangoApi.DeviceProxy;
import java.util.Hashtable;

public class EventConsumerUtil {

    private static EventConsumerUtil instance = null;

    private static boolean zmqTested = false;

    private static boolean zmqLoadable = true;

    public static EventConsumerUtil getInstance() {
        if (instance == null) {
            instance = new EventConsumerUtil();
        }
        return instance;
    }

    private EventConsumerUtil() {
        try {
            NotifdEventConsumer.getInstance();
        } catch (DevFailed e) {
        }
    }

    private EventConsumer isChannelAlreadyConnected(DeviceProxy deviceProxy) {
        try {
            String adminName = deviceProxy.adm_name();
            EventChannelStruct eventChannelStruct = EventConsumer.getChannelMap().get(adminName);
            if (eventChannelStruct == null) {
                return null;
            } else {
                return eventChannelStruct.consumer;
            }
        } catch (DevFailed e) {
            return null;
        }
    }

    private boolean isZmqLoadable() {
        if (!zmqTested) {
            String zmqEnable = System.getenv("ZMQ_ENABLE");
            if (zmqEnable == null) zmqEnable = System.getProperty("ZMQ_ENABLE");
            if (zmqEnable != null && zmqEnable.equals("true")) {
                try {
                    ZMQutils.getInstance();
                } catch (java.lang.NoClassDefFoundError error) {
                    System.err.println("======================================================================");
                    System.err.println("  " + error);
                    System.err.println("  Event system will be available only for notifd notification system ");
                    System.err.println("======================================================================");
                    zmqLoadable = false;
                } catch (java.lang.UnsatisfiedLinkError error) {
                    System.err.println("======================================================================");
                    System.err.println("  " + error);
                    System.err.println("  Event system will be available only for notifd notification system ");
                    System.err.println("======================================================================");
                    zmqLoadable = false;
                }
            } else {
                System.err.println("======================================================================");
                System.err.println("  ZMQ event system not enabled");
                System.err.println("  Event system will be available only for notifd notification system ");
                System.err.println("======================================================================");
                zmqLoadable = false;
            }
            zmqTested = true;
        }
        return zmqLoadable;
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, CallBack callback, String[] filters, boolean stateless) throws DevFailed {
        return subscribe_event(device, attribute, event, callback, -1, filters, stateless);
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, int max_size, String[] filters, boolean stateless) throws DevFailed {
        return subscribe_event(device, attribute, event, null, max_size, filters, stateless);
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, CallBack callback, int max_size, String[] filters, boolean stateless) throws DevFailed {
        int id;
        EventConsumer consumer = isChannelAlreadyConnected(device);
        if (consumer != null) {
            id = consumer.subscribe_event(device, attribute, event, callback, max_size, filters, stateless);
        } else if (isZmqLoadable()) {
            try {
                id = ZmqEventConsumer.getInstance().subscribe_event(device, attribute, event, callback, max_size, filters, stateless);
                ApiUtil.printTrace(device.name() + "/" + attribute + "  connected to ZMQ event system");
            } catch (DevFailed e) {
                ApiUtil.printTrace(e.errors[0].desc);
                if (e.errors[0].desc.equals("Command ZmqEventSubscriptionChange not found")) {
                    id = subscribeEventWithNotifd(device, attribute, event, callback, max_size, filters, stateless);
                } else throw e;
            }
        } else {
            id = subscribeEventWithNotifd(device, attribute, event, callback, max_size, filters, stateless);
        }
        return id;
    }

    private int subscribeEventWithNotifd(DeviceProxy device, String attribute, int event, CallBack callback, int max_size, String[] filters, boolean stateless) throws DevFailed {
        int id;
        id = NotifdEventConsumer.getInstance().subscribe_event(device, attribute, event, callback, max_size, filters, stateless);
        ApiUtil.printTrace(device.name() + "/" + attribute + "  connected to Notifd event system");
        return id;
    }

    public void unsubscribe_event(int event_id) throws DevFailed {
        Hashtable<String, EventCallBackStruct> callBackMap = EventConsumer.getEventCallbackMap();
        EventCallBackStruct callbackStruct = EventConsumer.getCallBackStruct(callBackMap, event_id);
        callbackStruct.consumer.unsubscribe_event(event_id);
    }
}
