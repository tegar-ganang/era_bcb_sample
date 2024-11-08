package ru.beta2.testyard.engine;

import junit.framework.Assert;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import ru.beta2.testyard.MessageControl;
import ru.beta2.testyard.Scenario;
import ru.beta2.testyard.config.Configuration;
import ru.beta2.testyard.engine.points.ChannelEvent;
import ru.beta2.testyard.engine.points.MessageEvent;
import ru.beta2.testyard.engine.points.PlayerEvent;
import java.lang.reflect.Field;
import java.util.*;

/**
 * User: Inc
 * Date: 17.06.2008
 * Time: 14:38:44
 */
public class ScenarioEngine extends AbstractStep implements Scenario, Expectations, SequenceGenerator {

    static final String UNEXPECTED_SIGNATURE = "Received unexpected events";

    static final String ABSENT_SIGNATURE = "Expected events absent";

    private final HotspotLink link;

    private Collection<HotspotEvent> expectedEvents = new ArrayList<HotspotEvent>();

    private HashMap<HotspotEvent, ScriptPoint> waiters = new HashMap<HotspotEvent, ScriptPoint>();

    private ArrayList<ScriptPoint> passAfterwards;

    private ArrayList<HotspotEvent> unexpectedEvents = new ArrayList<HotspotEvent>();

    private int seq;

    private boolean smartCheck = true;

    private boolean skipSyntheticFields = true;

    private MsgCtl mctl = new MsgCtl();

    private boolean stepByStep = false;

    private AbstractStep currentStep = this;

    private final ScriptContext context = new ScriptContext();

    public ScenarioEngine(HotspotLink link, Configuration cfg) {
        this.link = link;
        link.setListener(new Listener());
        context.setCfg(cfg);
        context.setExpectations(this);
        context.setHotspotLink(link);
        context.setSequenceGenerator(this);
    }

    private void execChildren(AbstractScript point) {
        for (ScriptPoint p : point.points) {
            p.exec();
            if (p.execChildren()) {
                execChildren(p);
            }
        }
    }

    private void passScript(AbstractScript script) {
        if (script.points.size() == 0) {
            return;
        }
        joinSkippings(script);
        execChildren(script);
        if (expectedEvents.size() == 0) {
            return;
        }
        passAfterwards = new ArrayList<ScriptPoint>();
        link.waitForMessages(this);
        ArrayList<ScriptPoint> passAfterwards = this.passAfterwards;
        for (ScriptPoint p : passAfterwards) {
            passScript(p);
        }
        checkUnexpectedEvent();
        checkAbsentEvents();
    }

    private void joinSkippings(AbstractScript point) {
        skippings.addAll(point.skippings);
    }

    private void checkUnexpectedEvent() {
        if (unexpectedEvents.size() > 0) {
            StringBuilder b = new StringBuilder(UNEXPECTED_SIGNATURE + ": ");
            for (HotspotEvent e : unexpectedEvents) {
                b.append("\n  * ");
                b.append(eventToString(e));
            }
            b.append("\nWhile expected events are:");
            fail(b.toString());
        }
    }

    private void checkAbsentEvents() {
        if (expectedEvents.size() > 0) {
            fail(ABSENT_SIGNATURE + ": " + expectedEvents.size());
        }
    }

    private void fail(String message) {
        StringBuilder b = new StringBuilder(message);
        buildExpectedEventsInfo(b);
        Assert.fail(b.toString());
    }

    private void buildExpectedEventsInfo(StringBuilder b) {
        for (HotspotEvent e : expectedEvents) {
            b.append("\n  - ");
            b.append(eventToString(e));
        }
    }

    private String eventToString(HotspotEvent event) {
        if (event instanceof MessageEvent) {
            return new MessageEventToStringBuilder(event).toString();
        }
        return event.toString();
    }

    public void go() {
        try {
            AbstractStep step = currentStep;
            while (step != null) {
                step = processStep(step);
            }
        } finally {
            link.shutdown();
        }
    }

    private AbstractStep processStep(AbstractStep step) {
        System.out.println("STEP: " + step.stepName);
        passScript(step);
        clearSkippings();
        if (waiters.size() > 0) {
            throw new ImplementationBugException("Waiters exist after pass step:" + waiters.size());
        }
        return step.next;
    }

    public void goStep(AbstractStep step) {
        boolean error = false;
        try {
            currentStep = processStep(step);
        } catch (RuntimeException e) {
            System.out.println("ERROR in GO STEP");
            e.printStackTrace();
            error = true;
            throw e;
        } finally {
            if (error) {
                link.shutdown();
            }
        }
    }

    private void clearSkippings() {
        Iterator<SkipEntry> i = skippings.iterator();
        while (i.hasNext()) {
            if (!i.next().keepWholeScenario()) {
                i.remove();
            }
        }
    }

    public MessageControl getMessageControl() {
        return mctl;
    }

    public boolean isStepByStepMode() {
        return stepByStep;
    }

    public void setStepByStepMode(boolean stepByStep) {
        this.stepByStep = stepByStep;
    }

    private HotspotEvent removeEvent(HotspotEvent event) {
        boolean directRemove = expectedEvents.remove(event);
        if (directRemove || (!(event instanceof MessageEvent)) || (!smartCheck)) {
            return directRemove ? event : null;
        }
        MessageEvent actual = (MessageEvent) event;
        if (actual.getMessage() == null) {
            return null;
        }
        for (HotspotEvent e : expectedEvents) {
            if (e instanceof MessageEvent) {
                MessageEvent found = (MessageEvent) e;
                boolean equals = new EqualsBuilder().append(found.getType(), actual.getType()).append(found.getPlayer(), actual.getPlayer()).append(found.getChannel(), actual.getChannel()).isEquals() && messagesEquals(found.getMessage(), actual.getMessage());
                if (equals) {
                    expectedEvents.remove(found);
                    return found;
                }
            }
        }
        return null;
    }

    private boolean messagesEquals(Object msg1, Object msg2) {
        if (msg1 == null || msg2 == null) {
            return false;
        }
        boolean direct = msg1.equals(msg2);
        System.out.println("CLASS: " + msg1.getClass() + ", direct=" + direct + ", msg=" + msg1);
        Class clazz = msg1.getClass();
        if (direct) {
            System.out.println("DIRECT EQUALS");
            return true;
        }
        if (!clazz.equals(msg2.getClass())) {
            System.out.println("CLASSES NOT EQUALS");
            return false;
        }
        if (isStopProcessingClass(clazz)) {
            System.out.println("STOP PROCESSING FALSE: " + clazz + ", <" + msg1 + "> != <" + msg2 + ">, false==" + msg1.equals(msg2));
            return false;
        }
        boolean reflect = EqualsBuilder.reflectionEquals(msg1, msg2);
        if (reflect && !clazz.isArray()) {
            System.out.println("REFLECT EQUALS");
            return true;
        }
        if (clazz.isArray()) {
            if (clazz.getComponentType().isPrimitive() || clazz.getComponentType().isEnum()) {
                return false;
            }
            Object[] a1 = (Object[]) msg1;
            Object[] a2 = (Object[]) msg2;
            if (a1.length != a2.length) {
                return false;
            }
            for (int i = 0; i < a1.length; i++) {
                if (!messagesEquals(a1[i], a2[i])) {
                    return false;
                }
            }
            return true;
        }
        ArrayList<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        for (Object c : ClassUtils.getAllSuperclasses(clazz)) {
            fields.addAll(Arrays.asList(((Class) c).getDeclaredFields()));
        }
        for (Field f : fields) {
            f.setAccessible(true);
            if (skipSyntheticFields && f.isSynthetic()) {
                continue;
            }
            if (mctl.isFieldIgnored(clazz, f.getName())) {
                continue;
            }
            boolean result;
            Object value1;
            Object value2;
            try {
                value1 = f.get(msg1);
                value2 = f.get(msg2);
            } catch (IllegalAccessException e) {
                throw new VerifyException("Error getting value from messages objects", e);
            }
            result = ObjectUtils.equals(value1, value2);
            if (!(result || stopTypeProcessing(f.getType()))) {
                result = messagesEquals(value1, value2);
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private void checkEvent(HotspotEvent event) {
        System.out.println("Event in engine: " + eventToString(event));
        HotspotEvent found = removeEvent(event);
        if (found == null) {
            for (SkipEntry se : skippings) {
                if (se.skip(event)) {
                    System.out.println("Skip event: " + eventToString(event));
                    return;
                }
            }
            System.out.println("Received unexpected event: " + eventToString(event));
            unexpectedEvents.add(event);
            return;
        }
        ScriptPoint p;
        if ((p = waiters.remove(found)) != null) {
            if (isWaiterFree(p)) {
                if (p instanceof EventCallback) {
                    ((EventCallback) p).eventReceived(event);
                }
                passAfterwards.add(p);
            }
        }
    }

    private boolean isWaiterFree(ScriptPoint p) {
        return !waiters.values().contains(p);
    }

    public void expectEvent(HotspotEvent event, ScriptPoint waiter) {
        waiters.put(event, waiter);
        expectedEvents.add(event);
    }

    public int count() {
        return expectedEvents.size();
    }

    public int genNext() {
        return ++seq;
    }

    public int currentValue() {
        return seq;
    }

    public void assignValue(int value) {
        seq = value;
    }

    public boolean isAutoTagMessages() {
        return mctl.autoTag;
    }

    protected ScriptContext getContext() {
        return context;
    }

    /**
     * HotspotListener implementation
     */
    class Listener implements HotspotListener {

        public void loggedIn(int player) {
            checkEvent(new PlayerEvent(PlayerEvent.LOGGED_IN, player));
        }

        public void disconnected(int player) {
            checkEvent(new PlayerEvent(PlayerEvent.DISCONNECTED, player));
        }

        public void channelJoined(int player, String channel) {
            checkEvent(new ChannelEvent(ChannelEvent.JOINED, player, channel));
        }

        public void channelLeft(int player, String channel) {
            checkEvent(new ChannelEvent(ChannelEvent.LEFT, player, channel));
        }

        public void messageFromSession(int player, Object message) {
            checkEvent(new MessageEvent(player, message));
        }

        public void messageFromChannel(int player, String channel, Object message) {
            checkEvent(new MessageEvent(player, channel, message));
        }
    }

    private static final Class[] STOP_PROCESSING_CLASSES = new Class[] { String.class, Date.class };

    private static boolean isStopProcessingClass(Class clazz) {
        return ArrayUtils.contains(STOP_PROCESSING_CLASSES, clazz);
    }

    private static boolean stopTypeProcessing(Class type) {
        return type.isPrimitive() || type.isEnum() || isStopProcessingClass(type);
    }

    class MessageEventToStringBuilder extends ReflectionToStringBuilder {

        public MessageEventToStringBuilder(Object o) {
            super(o, ToStringStyle.SHORT_PREFIX_STYLE);
        }

        protected Object getValue(Field field) throws IllegalArgumentException, IllegalAccessException {
            if (field.getName().equals("message")) {
                return MessageToStringBuilder.messageToString(field.get(getObject()));
            }
            return super.getValue(field);
        }
    }

    static class MessageToStringBuilder extends ReflectionToStringBuilder {

        private static final Class[] DIRECT_TO_STRING = new Class[] { String.class, Date.class };

        public MessageToStringBuilder(Object o) {
            super(o);
        }

        protected Object getValue(Field field) throws IllegalArgumentException, IllegalAccessException {
            if (stopTypeProcessing(field.getType()) || (field.getType().isArray() && stopTypeProcessing(field.getType().getComponentType()))) {
                return super.getValue(field);
            }
            if (field.getType().isArray()) {
                StringBuilder b = new StringBuilder("{");
                boolean comma = false;
                for (Object o : (Object[]) field.get(getObject())) {
                    if (comma) {
                        b.append(',');
                    } else {
                        comma = true;
                    }
                    b.append(messageToString(o));
                }
                b.append('}');
                return b.toString();
            }
            return messageToString(field.get(getObject()));
        }

        static String messageToString(Object object) {
            return new MessageToStringBuilder(object).toString();
        }
    }

    class MsgCtl implements MessageControl {

        private HashMap<Class, ArrayList<String>> ignoredFields = new HashMap<Class, ArrayList<String>>();

        boolean autoTag = true;

        public boolean isSmartCheck() {
            return smartCheck;
        }

        public void setSmartCheck(boolean smartCheck) {
            ScenarioEngine.this.smartCheck = smartCheck;
        }

        public boolean isSkipSyntheticFields() {
            return skipSyntheticFields;
        }

        public void setSkipSyntheticFields(boolean syntheticFields) {
            ScenarioEngine.this.skipSyntheticFields = syntheticFields;
        }

        public void ignoreField(Class objectClass, String fieldName) {
            ArrayList<String> a = ignoredFields.get(objectClass);
            if (a == null) {
                a = new ArrayList<String>();
                ignoredFields.put(objectClass, a);
            }
            a.add(fieldName);
        }

        public void cancelFieldsIgnore(Class objectClass) {
            ignoredFields.remove(objectClass);
        }

        public boolean isAutoTagMessages() {
            return autoTag;
        }

        public void setAutoTagMessages(boolean autoTag) {
            this.autoTag = autoTag;
        }

        boolean isFieldIgnored(Class objectClass, String fieldName) {
            ArrayList<String> a = ignoredFields.get(objectClass);
            return a != null && a.contains(fieldName);
        }
    }
}
