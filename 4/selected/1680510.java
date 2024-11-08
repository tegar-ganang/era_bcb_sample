package ru.beta2.testyard.engine;

import ru.beta2.testyard.SkipControl;
import ru.beta2.testyard.engine.points.MessageEvent;

/**
 * User: Inc
 * Date: 19.06.2008
 * Time: 16:21:35
 */
public class SkipEntry implements SkipControl {

    private final String channel;

    private final Class messageClass;

    private boolean wholeScenario = false;

    public SkipEntry(Class messageClass) {
        this.messageClass = messageClass;
        channel = null;
    }

    SkipEntry(String channel, Class messageClass) {
        this.channel = channel;
        this.messageClass = messageClass;
    }

    public boolean skip(HotspotEvent event) {
        if (!(event instanceof MessageEvent)) {
            return false;
        }
        MessageEvent msgEvent = (MessageEvent) event;
        if (channel == null) {
            return msgEvent.getChannel() == null && messageClass.isInstance(msgEvent.getMessage());
        }
        return channel.equals(msgEvent.getChannel()) && messageClass.isInstance(msgEvent.getMessage());
    }

    public SkipControl wholeScenario() {
        wholeScenario = true;
        return this;
    }

    boolean keepWholeScenario() {
        return wholeScenario;
    }
}
