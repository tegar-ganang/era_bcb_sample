package org.granite.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.granite.util.JDOMUtil;
import org.jdom.Element;

public class Destination {

    private String id = null;

    private List<String> channelRefs = new ArrayList<String>();

    private Map<String, Object> properties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public List<String> getChannelRefs() {
        return channelRefs;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static Destination forElement(Element element) {
        JDOMUtil.checkElement(element, "destination", "id");
        Destination destination = new Destination();
        destination.id = element.getAttributeValue("id");
        Element channels = element.getChild("channels");
        if (channels != null) {
            for (Element channel : (List<Element>) channels.getChildren("channel")) {
                JDOMUtil.checkElement(channel, "channel", "ref");
                destination.channelRefs.add(channel.getAttributeValue("ref"));
            }
        }
        Element properties = element.getChild("properties");
        if (properties != null) {
            for (Element property : (List<Element>) properties.getChildren()) {
                if ("source".equals(property.getName()) || "factory".equals(property.getName())) destination.properties.put(property.getName(), property.getText());
            }
        }
        return destination;
    }
}
