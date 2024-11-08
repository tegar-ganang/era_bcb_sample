package de.jlab.config;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ct-module-parameters")
public class CTModuleParameterConfig {

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @XmlAttribute(required = true, name = "type")
    String moduleType;

    @XmlAttribute(required = true, name = "address")
    int address;

    @XmlAttribute(required = true, name = "comm-channel")
    String channelName;

    @XmlElement(name = "parameter")
    List<ParameterConfig> params = null;

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public List<ParameterConfig> getParams() {
        return params;
    }

    public void setParams(List<ParameterConfig> params) {
        this.params = params;
    }

    public boolean equals(Object other) {
        if (!(other instanceof CTModuleParameterConfig)) return false;
        CTModuleParameterConfig otherConfig = (CTModuleParameterConfig) other;
        return (otherConfig.moduleType == moduleType && otherConfig.address == address);
    }

    public int hashCode() {
        return (address + moduleType).hashCode();
    }

    public String getParameterByName(String name) {
        String result = null;
        for (ParameterConfig currParam : params) {
            if (currParam.getName().equals(name)) {
                result = currParam.getValue();
                break;
            }
        }
        return result;
    }
}
