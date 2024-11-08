package com.ibm.tuningfork.systemtap;

import java.nio.ByteBuffer;
import java.util.List;
import com.ibm.tuningfork.infra.Constants;
import com.ibm.tuningfork.infra.data.AbstractRelation;
import com.ibm.tuningfork.infra.data.AbstractTimeInterval;
import com.ibm.tuningfork.infra.data.Annotation;
import com.ibm.tuningfork.infra.data.IRelation;
import com.ibm.tuningfork.infra.data.IStringMapRelation;
import com.ibm.tuningfork.infra.data.ITuple;
import com.ibm.tuningfork.infra.data.SealingException;
import com.ibm.tuningfork.infra.data.TimeInterval;
import com.ibm.tuningfork.infra.data.Tuple_RefOnly;
import com.ibm.tuningfork.infra.event.AttributeType;
import com.ibm.tuningfork.infra.event.EventAttribute;
import com.ibm.tuningfork.infra.event.IEvent;
import com.ibm.tuningfork.infra.event.StaticEventType;
import com.ibm.tuningfork.infra.feed.Feed;
import com.ibm.tuningfork.infra.feed.FeedGroup;
import com.ibm.tuningfork.infra.feed.Feedlet;
import com.ibm.tuningfork.infra.stream.precise.PersistenceUtil;
import com.ibm.tuningfork.infra.util.MiscUtils;
import com.ibm.tuningfork.infra.util.Triple;

public class PidInfo extends AbstractRelation implements IStringMapRelation {

    public final String command;

    public final int pid;

    public final int uid;

    protected int state_policy_cpu_priority;

    public final String threadName;

    public static final int MAX_PID = 32767;

    public static final int TASKSTATE_RUNNING = 0;

    public static final int TASKSTATE_INTERRUPTIBLE = 1;

    public static final int TASKSTATE_UNINTERRUPTIBLE = 2;

    public static final int TASKSTATE_STOPPED = 3;

    public static final int TASKSTATE_TRACED = 4;

    public static final int TASKSTATE_NONINTERACTIVE = 5;

    public static final int TASKSTATE_UNKNOWN = 6;

    public static final String[] taskStateStrings = new String[] { "Running", "Waiting", "Waiting-Unint", "Stopped", "Traced", "Non-Interactive", "Unknown" };

    public static final int SCHED_NORMAL = 0;

    public static final int SCHED_FIFO = 1;

    public static final int SCHED_RR = 2;

    public static final int SCHED_BATCH = 3;

    public static final int SCHED_UNKNOWN = 4;

    public static final int IDLE_PID = 0;

    public static final int IRQ_PID = -1;

    public static final int NONE_PID = -2;

    public static final int ROOT_UID = 0;

    public static final String[] fields = { "Command", "Pid", "UID", "Task State", "Policy", "CPU", "Priority", "Thread" };

    protected static final int COMMAND = 0, PID = 1, UID = 2, STATE = 3, POLICY = 4, CPU = 5, PRIORITY = 6, THREAD = 7;

    protected static final int SEALED_MASK = 0x80000000;

    protected static final int STATE_MASK = 0x70000000;

    protected static final int POLICY_MASK = 0x0f000000;

    protected static final int PRIORITY_MASK = 0x00fff000;

    protected static final int CPU_MASK = 0x00000fff;

    protected static final int[] MASKS = { STATE_MASK, POLICY_MASK, PRIORITY_MASK, CPU_MASK };

    protected static final int STATE_SHIFT = MiscUtils.maskShift(STATE_MASK);

    protected static final int POLICY_SHIFT = MiscUtils.maskShift(POLICY_MASK);

    protected static final int PRIORITY_SHIFT = MiscUtils.maskShift(PRIORITY_MASK);

    protected static final int CPU_SHIFT = MiscUtils.maskShift(CPU_MASK);

    protected static final int[] SHIFTS = { STATE_SHIFT, POLICY_SHIFT, PRIORITY_SHIFT, CPU_SHIFT };

    protected final int PRIORITY_OFFSET = 256;

    private static final String THREAD_NAME_SEPARATOR = " / ";

    public IStringMapRelation toAnnotation() {
        return new PidInfo(command, pid, uid, getState(), getPolicy(), getPriority(), getCPU(), threadName).seal();
    }

    public PidInfo(Feed feed, int pid, int uid, String command) {
        this(command, pid, uid, TASKSTATE_UNKNOWN, SCHED_UNKNOWN, 0, 0, getThreadInfoFromOtherFeeds(feed, pid));
    }

    public PidInfo(String command, int pid, int uid, int state, int policy, int priority, int cpu, String threadName) {
        this.command = command;
        this.pid = pid;
        this.uid = uid;
        this.threadName = threadName;
        assert state >= 0 && policy >= 0 && priority >= 0 && cpu >= 0;
        if (policy == SCHED_RR) {
            pid += 0;
        }
        setTaskState(state);
        updatePolicyPriority(policy, priority);
        setIntValue(CPU, cpu);
    }

    public void updatePolicyPriority(int policy, int combinedPrio) {
        setIntValue(POLICY, policy);
        setIntValue(PRIORITY, combinedPrio);
    }

    public void setTaskState(int state) {
        setIntValue(STATE, state);
    }

    public void setCPU(int cpu) {
        setIntValue(CPU, cpu);
    }

    public PidInfo withCPU(int newCPU) {
        return new PidInfo(command, pid, uid, getState(), getPolicy(), getPriority(), newCPU, threadName);
    }

    public ITuple get(int index) {
        return new Tuple_RefOnly(new String[] { fields[index], getStringValue(index) }, null);
    }

    protected int getIntValue(int index) {
        if (3 <= index && index <= 6) {
            int value = (state_policy_cpu_priority & MASKS[index - 3]) >> SHIFTS[index - 3];
            return index != PRIORITY ? value : value - PRIORITY_OFFSET;
        } else if (index == PID) {
            return pid;
        } else if (index == UID) {
            return uid;
        }
        return -1;
    }

    protected void setIntValue(int index, int value) {
        checkSealed();
        if (3 <= index && index <= 6) {
            value = index != PRIORITY ? value : value + PRIORITY_OFFSET;
            state_policy_cpu_priority &= ~MASKS[index - 3];
            state_policy_cpu_priority |= value << SHIFTS[index - 3];
        }
    }

    protected String getStringValue(int index) {
        if (index == 0) {
            return command;
        } else if (index == 7) {
            return threadName;
        } else {
            return "" + getIntValue(index);
        }
    }

    public int size() {
        return fields.length;
    }

    public String get(String key) {
        int i = 0;
        for (String f : fields) {
            if (f.equals(key)) {
                return getStringValue(i);
            }
            i++;
        }
        return null;
    }

    public int getState() {
        return getIntValue(STATE);
    }

    public int getPolicy() {
        return getIntValue(POLICY);
    }

    public int getPriority() {
        return getIntValue(PRIORITY);
    }

    public int getCPU() {
        return getIntValue(CPU);
    }

    public final boolean isActive() {
        return pid != IDLE_PID;
    }

    public final boolean isIRQ() {
        return pid == IRQ_PID;
    }

    public final boolean isRoot() {
        return uid == ROOT_UID;
    }

    public String getTaskStateAnnotation() {
        return taskStateStrings[getState()];
    }

    public IStringMapRelation concatenate(IStringMapRelation that) {
        return this;
    }

    public String getKey(int index) {
        return fields[index];
    }

    public String[] getKeys() {
        return new String[] { "Command", "Pid", "Task State", "Priority", "Thread", "CPU" };
    }

    public String[] getValues() {
        return new String[] { command, "" + pid, getTaskStateAnnotation(), priorityToString(getPolicy(), getPriority()), threadName, "" + getCPU() };
    }

    public int getPersistedSize() {
        return (3 * Constants.SIZE_OF_INT) + PersistenceUtil.getPersistedSize(command) + PersistenceUtil.getPersistedSize(threadName);
    }

    public void write(ByteBuffer buf) {
        PersistenceUtil.write(buf, command);
        buf.putInt(pid);
        buf.putInt(uid);
        buf.putInt(state_policy_cpu_priority);
        PersistenceUtil.write(buf, threadName);
    }

    public PidInfo(ByteBuffer buf) {
        command = PersistenceUtil.readString(buf).intern();
        pid = buf.getInt();
        uid = buf.getInt();
        state_policy_cpu_priority = buf.getInt();
        threadName = PersistenceUtil.readString(buf).intern();
    }

    public static String priorityToString(int policy, int combinedPriority) {
        String policyString = "Unknown";
        if (policy == SCHED_NORMAL || policy == SCHED_BATCH) {
            int priority = combinedPriority - 120;
            policyString = (policy == SCHED_NORMAL ? "Normal" : "Batch") + (priority == 0 ? "" : " (nice=" + priority + ")");
        } else if (policy == SCHED_FIFO || policy == SCHED_RR) {
            policyString = (policy == SCHED_FIFO ? "RT FIFO " : "RT RR ") + combinedPriority;
        }
        return policyString.intern();
    }

    protected static String getThreadInfoFromOtherFeeds(Feed feed, int pid) {
        FeedGroup fg = feed.getFeedGroup();
        if (fg == null) {
            return "";
        }
        List<Feed> feeds = fg.getFeeds();
        String s = "";
        String sep = "";
        for (Feed f : feeds) {
            if (f.getCanonicalId() != feed.getCanonicalId()) {
                Feedlet fl = f.getFeedletManager().getFeedletByTid(pid);
                if (fl != null) {
                    s += sep + fl.getDisplayName() + getThreadClassName(fl);
                    sep = THREAD_NAME_SEPARATOR;
                }
            }
        }
        return s.intern();
    }

    protected static String getThreadClassName(Feedlet fl) {
        String className = fl.getProperty("class");
        if (className != null && !className.equals("java/lang/Thread")) {
            int index = className.lastIndexOf('/') + 1;
            String shortName = className.substring(index < className.length() ? index : 0);
            return " (" + shortName + ")";
        } else {
            return "";
        }
    }

    public PidInfo seal() {
        state_policy_cpu_priority |= SEALED_MASK;
        return this;
    }

    public boolean isSealed() {
        return (state_policy_cpu_priority & SEALED_MASK) != 0;
    }

    public void clear() {
        throw new UnsupportedOperationException("PID info always has some tuples");
    }

    public boolean add(ITuple arg0) {
        throw new SealingException(this);
    }

    public boolean remove(Object arg0) {
        throw new SealingException(this);
    }

    public static CPUTimeIntervalEventType eventType = new CPUTimeIntervalEventType();

    public static class CPUTimeIntervalEventType extends StaticEventType {

        public CPUTimeIntervalEventType() {
            super("Run Interval", "CPU Time Interval", new EventAttribute[] { AbstractTimeInterval.endTimeAttribute, new EventAttribute("PidInfo", "Process Information", Annotation.eventType) });
        }

        public TimeInterval read(ByteBuffer buf) {
            return new TimeInterval(buf.getLong(), buf.getLong(), buf.get() == 1 ? new PidInfo(buf) : null);
        }

        public String getReflectiveTypeName() {
            return "CPUTimeInterval";
        }

        public Triple<String, String, String> getDisplayStrings(IEvent event, int attributeIndex) {
            if (attributeIndex == 0) {
                return new Triple<String, String, String>("Duration", event != null ? "" + ((TimeInterval) event).length() : "", AttributeType.LONG.toString());
            } else {
                return super.getDisplayStrings(event, attributeIndex);
            }
        }

        public ITuple newInstance(Long time, int[] ints, long[] longs, double[] doubles, String[] strings, boolean[] booleans, IRelation[] relations) {
            PidInfo info = (PidInfo) ((relations != null && relations.length > 0) ? relations[0] : null);
            return new TimeInterval(time, longs[0], info);
        }
    }
}
