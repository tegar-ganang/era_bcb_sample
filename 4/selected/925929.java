package fr.esrf.TangoApi.events;

import fr.esrf.TangoDs.TangoConst;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;

class KeepAliveThread extends TimerTask implements TangoConst {

    private static final long EVENT_RESUBSCRIBE_PERIOD = 600000;

    private static final long EVENT_HEARTBEAT_PERIOD = 10000;

    public KeepAliveThread() {
        super();
    }

    public void run() {
        long MAX_TARDINESS = EVENT_HEARTBEAT_PERIOD * 3 / 2;
        if (System.currentTimeMillis() - scheduledExecutionTime() >= MAX_TARDINESS) {
            return;
        }
        EventConsumer.subscribe_if_not_done();
        resubscribe_if_needed();
    }

    static boolean heartbeatHasBeenSkipped(EventChannelStruct eventChannelStruct) {
        long now = System.currentTimeMillis();
        return ((now - eventChannelStruct.last_heartbeat) > EVENT_HEARTBEAT_PERIOD);
    }

    static long getEventHeartbeatPeriod() {
        return EVENT_HEARTBEAT_PERIOD;
    }

    private void resubscribe_if_needed() {
        Enumeration channel_names = EventConsumer.getChannelMap().keys();
        long now = System.currentTimeMillis();
        while (channel_names.hasMoreElements()) {
            String name = (String) channel_names.nextElement();
            EventChannelStruct eventChannelStruct = EventConsumer.getChannelMap().get(name);
            if ((now - eventChannelStruct.last_subscribed) > EVENT_RESUBSCRIBE_PERIOD / 3) {
                reSubscribeByName(eventChannelStruct, name);
            }
            eventChannelStruct.consumer.checkIfHeartbeatSkipped(name, eventChannelStruct);
        }
    }

    private void reSubscribeByName(EventChannelStruct eventChannelStruct, String name) {
        Hashtable<String, EventCallBackStruct> callBackMap = EventConsumer.getEventCallbackMap();
        EventCallBackStruct callbackStruct = null;
        Enumeration channelNames = callBackMap.keys();
        while (channelNames.hasMoreElements()) {
            String key = (String) channelNames.nextElement();
            EventCallBackStruct eventStruct = callBackMap.get(key);
            if (eventStruct.channel_name.equals(name)) {
                callbackStruct = eventStruct;
            }
        }
        if (callbackStruct != null) {
            callbackStruct.consumer.reSubscribeByName(eventChannelStruct, name);
        }
    }
}
