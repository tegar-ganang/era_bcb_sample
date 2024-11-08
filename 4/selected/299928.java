package jmxm.jmxmodel;

import javax.management.MBeanAttributeInfo;
import jmxm.event.MBeanEvent;

public class MBeanAttribute extends AbstractNode {

    private Object value;

    private String type;

    private boolean readAble;

    private boolean writeable;

    private boolean isIs;

    public MBeanAttribute(MBean parent, String name) {
        super(parent, name, null);
    }

    public MBeanAttribute(MBean parent, MBeanAttributeInfo mbeanAttributeInfo) {
        this(parent, mbeanAttributeInfo.getName(), mbeanAttributeInfo.getDescription(), mbeanAttributeInfo.getType(), mbeanAttributeInfo.isReadable(), mbeanAttributeInfo.isWritable(), mbeanAttributeInfo.isIs());
    }

    public MBeanAttribute(MBean parent, String name, String description, String type, boolean readAble, boolean writeable, boolean isIs) {
        super(parent, name, description);
        this.type = type;
        this.readAble = readAble;
        this.writeable = writeable;
        this.isIs = isIs;
    }

    public void setAllUnmarked() {
        setMark(false);
    }

    public void removeUnmarked() {
    }

    public boolean isIs() {
        return isIs;
    }

    public void setIs(boolean isIs) {
        this.isIs = isIs;
    }

    public boolean isReadAble() {
        return readAble;
    }

    public void setReadAble(boolean readAble) {
        this.readAble = readAble;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        fireEvent(new MBeanEvent(this, MBeanEvent.UPDATED));
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    public String toString() {
        String temp = type;
        if (temp == null) {
            return name;
        } else if (temp.indexOf("[Ljava.lang.") == 0) {
            temp = "[L" + temp.substring(12);
        } else if (temp.indexOf("java.lang.") == 0) {
            temp = temp.substring(10);
        }
        return temp + " " + name;
    }
}
