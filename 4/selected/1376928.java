package de.jlab.config.runs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "run")
public class Run {

    @XmlAttribute(name = "name", required = true)
    String name;

    @XmlAttribute(name = "channel", required = true)
    String channel;

    @XmlAttribute(name = "address", required = true)
    int address;

    @XmlAttribute(name = "scale-t")
    String scaleT;

    @XmlAttribute(name = "scale-v")
    String scaleV;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public String getScaleT() {
        return scaleT;
    }

    public void setScaleT(String scaleT) {
        this.scaleT = scaleT;
    }

    public String getScaleV() {
        return scaleV;
    }

    public void setScaleV(String scaleV) {
        this.scaleV = scaleV;
    }
}
