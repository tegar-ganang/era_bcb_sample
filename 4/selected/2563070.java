package fr.esrf.TangoApi.events;

import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.*;
import fr.esrf.TangoDs.Except;
import fr.esrf.TangoDs.TangoConst;
import org.omg.CosNotifyComm.StructuredPushConsumerPOA;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author pascal_verdier
 */
public abstract class EventConsumer extends StructuredPushConsumerPOA implements TangoConst, Runnable, IEventConsumer {

    protected static int subscribe_event_id = 0;

    protected static Hashtable<String, EventChannelStruct> channel_map = new Hashtable<String, EventChannelStruct>();

    protected static Hashtable<String, String> device_channel_map = new Hashtable<String, String>();

    protected static Hashtable<String, EventCallBackStruct> event_callback_map = new Hashtable<String, EventCallBackStruct>();

    protected static Hashtable<String, EventCallBackStruct> failed_event_callback_map = new Hashtable<String, EventCallBackStruct>();

    protected abstract void checkDeviceConnection(DeviceProxy device, String attribute, DeviceData deviceData, String event_name) throws DevFailed;

    protected abstract void connect_event_channel(ConnectionStructure cs) throws DevFailed;

    protected abstract boolean reSubscribe(EventChannelStruct event_channel_struct, EventCallBackStruct callback_struct);

    protected abstract void removeFilters(EventCallBackStruct cb_struct) throws DevFailed;

    protected abstract String getEventSubscriptionCommandName();

    protected abstract void checkIfAlreadyConnected(DeviceProxy device, String attribute, String event_name, CallBack callback, int max_size, boolean stateless) throws DevFailed;

    protected abstract void setAdditionalInfoToEventCallBackStruct(EventCallBackStruct callback_struct, String device_name, String attribute, String event_name, String[] filters, EventChannelStruct channel_struct) throws DevFailed;

    protected abstract void unsubscribeTheEvent(EventCallBackStruct callbackStruct) throws DevFailed;

    protected abstract void checkIfHeartbeatSkipped(String name, EventChannelStruct eventChannelStruct);

    protected EventConsumer() throws DevFailed {
    }

    static Hashtable<String, EventChannelStruct> getChannelMap() {
        return channel_map;
    }

    static Hashtable<String, EventCallBackStruct> getEventCallbackMap() {
        return event_callback_map;
    }

    public void disconnect_structured_push_consumer() {
        System.out.println("calling EventConsumer.disconnect_structured_push_consumer()");
    }

    public void offer_change(org.omg.CosNotification.EventType[] added, org.omg.CosNotification.EventType[] removed) throws org.omg.CosNotifyComm.InvalidEventType {
        System.out.println("calling EventConsumer.offer_change()");
    }

    protected void push_structured_event_heartbeat(String domain_name) {
        try {
            if (channel_map.containsKey(domain_name)) {
                EventChannelStruct event_channel_struct = channel_map.get(domain_name);
                event_channel_struct.last_heartbeat = System.currentTimeMillis();
            } else {
                Enumeration keys = channel_map.keys();
                boolean found = false;
                while (keys.hasMoreElements() && !found) {
                    String name = (String) keys.nextElement();
                    EventChannelStruct event_channel_struct = channel_map.get(name);
                    if (event_channel_struct.adm_device_proxy.name().equals(domain_name)) {
                        event_channel_struct.last_heartbeat = System.currentTimeMillis();
                        found = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callEventSubscriptionAndConnect(DeviceProxy device, String attribute, String event_name) throws DevFailed {
        String device_name = device.name();
        String[] info = new String[] { device_name, attribute, "subscribe", event_name };
        DeviceData argin = new DeviceData();
        argin.insert(info);
        DeviceData argout = device.get_adm_dev().command_inout(getEventSubscriptionCommandName(), argin);
        checkDeviceConnection(device, attribute, argout, event_name);
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, CallBack callback, String[] filters, boolean stateless) throws DevFailed {
        return subscribe_event(device, attribute, event, callback, -1, filters, stateless);
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, int max_size, String[] filters, boolean stateless) throws DevFailed {
        return subscribe_event(device, attribute, event, null, max_size, filters, stateless);
    }

    public int subscribe_event(DeviceProxy device, String attribute, int event, CallBack callback, int max_size, String[] filters, boolean stateless) throws DevFailed {
        String event_name = eventNames[event];
        checkIfAlreadyConnected(device, attribute, event_name, callback, max_size, stateless);
        if (callback == null && max_size >= 0) {
            if (device.getEventQueue() == null) if (max_size > 0) device.setEventQueue(new EventQueue(max_size)); else device.setEventQueue(new EventQueue());
        }
        String device_name = device.name();
        String callback_key = device_name.toLowerCase() + "/" + attribute + "." + event_name;
        try {
            callEventSubscriptionAndConnect(device, attribute.toLowerCase(), event_name);
        } catch (DevFailed e) {
            if (!stateless || e.errors[0].desc.equals("Command ZmqEventSubscriptionChange not found")) throw e; else {
                subscribe_event_id++;
                EventCallBackStruct new_event_callback_struct = new EventCallBackStruct(device, attribute, event_name, "", callback, max_size, subscribe_event_id, event, filters, false);
                failed_event_callback_map.put(callback_key, new_event_callback_struct);
                return subscribe_event_id;
            }
        }
        String channel_name = device_channel_map.get(device_name);
        EventChannelStruct event_channel_struct = channel_map.get(channel_name);
        event_channel_struct.last_subscribed = System.currentTimeMillis();
        int evnt_id;
        EventCallBackStruct failed_struct = failed_event_callback_map.get(callback_key);
        if (failed_struct == null) {
            subscribe_event_id++;
            evnt_id = subscribe_event_id;
        } else evnt_id = failed_struct.id;
        EventCallBackStruct new_event_callback_struct = new EventCallBackStruct(device, attribute, event_name, channel_name, callback, max_size, evnt_id, event, filters, true);
        setAdditionalInfoToEventCallBackStruct(new_event_callback_struct, device_name, attribute, event_name, filters, event_channel_struct);
        event_callback_map.put(callback_key, new_event_callback_struct);
        if ((event == CHANGE_EVENT) || (event == PERIODIC_EVENT) || (event == QUALITY_EVENT) || (event == ARCHIVE_EVENT) || (event == USER_EVENT) || (event == ATT_CONF_EVENT)) {
            new PushAttrValueLater(new_event_callback_struct).start();
        }
        return evnt_id;
    }

    static void subscribe_if_not_done() {
        Enumeration callback_structs = failed_event_callback_map.elements();
        while (callback_structs.hasMoreElements()) {
            EventCallBackStruct callback_struct = (EventCallBackStruct) callback_structs.nextElement();
            String callback_key = callback_struct.device.name().toLowerCase() + "/" + callback_struct.attr_name + "." + callback_struct.event_name;
            if (callback_struct.consumer != null) {
                try {
                    callback_struct.consumer.subscribe_event(callback_struct.device, callback_struct.attr_name, callback_struct.event_type, callback_struct.callback, callback_struct.max_size, callback_struct.filters, false);
                    failed_event_callback_map.remove(callback_key);
                } catch (DevFailed e) {
                    int source = (callback_struct.consumer instanceof NotifdEventConsumer) ? EventData.NOTIFD_EVENT : EventData.ZMQ_EVENT;
                    EventData event_data = new EventData(callback_struct.device, callback_key, callback_struct.event_name, source, callback_struct.event_type, null, null, null, e.errors);
                    if (callback_struct.use_ev_queue) {
                        EventQueue ev_queue = callback_struct.device.getEventQueue();
                        ev_queue.insert_event(event_data);
                    } else callback_struct.callback.push_event(event_data);
                }
            } else {
                System.err.println("====================================================");
                System.err.println("callback_struct.consumer=null  for " + callback_key);
            }
        }
    }

    static EventCallBackStruct getCallBackStruct(Hashtable map, int id) {
        Enumeration keys = map.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            EventCallBackStruct callback_struct = (EventCallBackStruct) map.get(name);
            if (callback_struct.id == id) return callback_struct;
        }
        return null;
    }

    private void removeCallBackStruct(Hashtable map, EventCallBackStruct cb_struct) throws DevFailed {
        removeFilters(cb_struct);
        String callback_key = cb_struct.device.name().toLowerCase() + "/" + cb_struct.attr_name + "." + cb_struct.event_name;
        map.remove(callback_key);
    }

    public void unsubscribe_event(int event_id) throws DevFailed {
        EventCallBackStruct callbackStruct = getCallBackStruct(event_callback_map, event_id);
        if (callbackStruct != null) {
            removeCallBackStruct(event_callback_map, callbackStruct);
            unsubscribeTheEvent(callbackStruct);
        } else {
            callbackStruct = getCallBackStruct(failed_event_callback_map, event_id);
            if (callbackStruct != null) removeCallBackStruct(failed_event_callback_map, callbackStruct); else Except.throw_event_system_failed("API_EventNotFound", "Failed to unsubscribe event, the event id (" + event_id + ") specified does not correspond with any known one", "EventConsumer.unsubscribe_event()");
        }
    }

    void reSubscribeByName(EventChannelStruct event_channel_struct, String name) {
        Enumeration callback_structs = event_callback_map.elements();
        while (callback_structs.hasMoreElements()) {
            EventCallBackStruct callback_struct = (EventCallBackStruct) callback_structs.nextElement();
            if (callback_struct.channel_name.equals(name)) {
                reSubscribe(event_channel_struct, callback_struct);
            }
        }
    }

    void pushReceivedException(EventChannelStruct event_channel_struct, EventCallBackStruct callback_struct, DevError error) {
        try {
            if (event_channel_struct != null) {
                if (event_channel_struct.consumer instanceof NotifdEventConsumer) {
                    if (!callback_struct.filter_ok) {
                        callback_struct.filter_id = NotifdEventConsumer.getInstance().add_filter_for_channel(event_channel_struct, callback_struct.filter_constraint);
                        callback_struct.filter_ok = true;
                    }
                }
            } else return;
            int eventSource = (event_channel_struct.consumer instanceof NotifdEventConsumer) ? EventData.NOTIFD_EVENT : EventData.ZMQ_EVENT;
            DevError[] errors = { error };
            String domain_name = callback_struct.device.name() + "/" + callback_struct.attr_name.toLowerCase();
            EventData event_data = new EventData(event_channel_struct.adm_device_proxy, domain_name, callback_struct.event_name, callback_struct.event_type, eventSource, null, null, null, errors);
            CallBack callback = callback_struct.callback;
            event_data.device = callback_struct.device;
            event_data.name = callback_struct.device.name();
            event_data.event = callback_struct.event_name;
            if (callback_struct.use_ev_queue) {
                EventQueue ev_queue = callback_struct.device.getEventQueue();
                ev_queue.insert_event(event_data);
            } else callback.push_event(event_data);
        } catch (DevFailed e) {
        }
    }

    void readAttributeAndPush(EventChannelStruct event_channel_struct, EventCallBackStruct callback_struct) {
        boolean found = false;
        for (int i = 0; !found && i < eventNames.length; i++) found = callback_struct.event_name.equals(eventNames[i]);
        if (!found) return;
        DeviceAttribute da = null;
        AttributeInfoEx info = null;
        DevError[] err = null;
        String domain_name = callback_struct.device.name() + "/" + callback_struct.attr_name;
        boolean old_transp = callback_struct.device.get_transparency_reconnection();
        callback_struct.device.set_transparency_reconnection(true);
        try {
            if (callback_struct.event_name.equals(eventNames[ATT_CONF_EVENT])) info = callback_struct.device.get_attribute_info_ex(callback_struct.attr_name); else da = callback_struct.device.read_attribute(callback_struct.attr_name);
            event_channel_struct.has_notifd_closed_the_connection++;
        } catch (DevFailed e) {
            err = e.errors;
        }
        callback_struct.device.set_transparency_reconnection(old_transp);
        int eventSource = (event_channel_struct.consumer instanceof NotifdEventConsumer) ? EventData.NOTIFD_EVENT : EventData.ZMQ_EVENT;
        EventData event_data = new EventData(callback_struct.device, domain_name, callback_struct.event_name, eventSource, callback_struct.event_type, da, info, null, err);
        if (callback_struct.use_ev_queue) {
            EventQueue ev_queue = callback_struct.device.getEventQueue();
            ev_queue.insert_event(event_data);
        } else callback_struct.callback.push_event(event_data);
    }

    class PushAttrValueLater extends Thread {

        private EventCallBackStruct cb_struct;

        PushAttrValueLater(EventCallBackStruct cb_struct) {
            this.cb_struct = cb_struct;
        }

        public void run() {
            try {
                sleep(10);
            } catch (Exception e) {
            }
            DeviceAttribute da = null;
            AttributeInfoEx info = null;
            DevError[] err = null;
            String domain_name = cb_struct.device.name() + "/" + cb_struct.attr_name.toLowerCase();
            try {
                if (cb_struct.event_type == ATT_CONF_EVENT) info = cb_struct.device.get_attribute_info_ex(cb_struct.attr_name); else da = cb_struct.device.read_attribute(cb_struct.attr_name);
            } catch (DevFailed e) {
                err = e.errors;
            }
            int eventSource = (cb_struct.consumer instanceof NotifdEventConsumer) ? EventData.NOTIFD_EVENT : EventData.ZMQ_EVENT;
            EventData event_data = new EventData(cb_struct.device, domain_name, cb_struct.event_name, cb_struct.event_type, eventSource, da, info, null, err);
            if (cb_struct.use_ev_queue) {
                EventQueue ev_queue = cb_struct.device.getEventQueue();
                ev_queue.insert_event(event_data);
            } else cb_struct.callback.push_event(event_data);
        }
    }

    protected class ConnectionStructure {

        String channelName;

        String attributeName;

        String deviceName;

        String eventName;

        Database dbase;

        DeviceData deviceData = null;

        boolean reconnect = false;

        ConnectionStructure(String channelName, String deviceName, String attributeName, String eventName, Database dbase, DeviceData deviceData, boolean reconnect) {
            this.channelName = channelName;
            this.deviceName = deviceName;
            this.attributeName = attributeName;
            this.eventName = eventName;
            this.dbase = dbase;
            this.deviceData = deviceData;
            this.reconnect = reconnect;
        }

        ConnectionStructure(String name, Database dbase, boolean reconnect) {
            this(name, null, null, null, dbase, null, reconnect);
        }

        public String toString() {
            return "channel name: " + channelName + "\ndbase:        " + dbase + "\nreconnect:    " + reconnect;
        }
    }
}
