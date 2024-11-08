package blue.csladspa;

import blue.utility.XMLUtilities;
import electric.xml.Element;
import electric.xml.Elements;
import java.io.Serializable;
import org.apache.commons.lang.text.StrBuilder;

/**
 *
 * @author SYi
 */
public class PortDefinition implements Serializable {

    private String displayName;

    private String channelName;

    private float rangeMin;

    private float rangeMax;

    private boolean logarithmic;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public float getRangeMin() {
        return rangeMin;
    }

    public void setRangeMin(float rangeMin) {
        this.rangeMin = rangeMin;
    }

    public float getRangeMax() {
        return rangeMax;
    }

    public void setRangeMax(float rangeMax) {
        this.rangeMax = rangeMax;
    }

    public boolean isLogarithmic() {
        return logarithmic;
    }

    public void setLogarithmic(boolean logarithmic) {
        this.logarithmic = logarithmic;
    }

    public String getCSDText() {
        StrBuilder builder = new StrBuilder();
        builder.append("ControlPort=").append(this.displayName);
        builder.append("|").append(this.channelName).append("\n");
        builder.append("Range=").append(this.rangeMin);
        builder.append("|").append(this.rangeMax);
        if (this.logarithmic) {
            builder.append(" &log");
        }
        builder.append("\n");
        return builder.toString();
    }

    public static PortDefinition loadFromXML(Element data) {
        PortDefinition retVal = new PortDefinition();
        Elements nodes = data.getElements();
        while (nodes.hasMoreElements()) {
            Element node = nodes.next();
            String nodeName = node.getName();
            String nodeVal = node.getTextString();
            if (nodeName.equals("displayName")) {
                retVal.displayName = nodeVal;
            } else if (nodeName.equals("channelName")) {
                retVal.channelName = nodeVal;
            } else if (nodeName.equals("rangeMin")) {
                retVal.rangeMin = Float.parseFloat(nodeVal);
            } else if (nodeName.equals("rangeMax")) {
                retVal.rangeMax = Float.parseFloat(nodeVal);
            } else if (nodeName.equals("logarithmic")) {
                retVal.logarithmic = Boolean.valueOf(nodeVal).booleanValue();
            }
        }
        return retVal;
    }

    public Element saveAsXML() {
        Element retVal = new Element("portDefinition");
        retVal.addElement("displayName").setText(displayName);
        retVal.addElement("channelName").setText(channelName);
        retVal.addElement(XMLUtilities.writeFloat("rangeMin", rangeMin));
        retVal.addElement(XMLUtilities.writeFloat("rangeMax", rangeMax));
        retVal.addElement(XMLUtilities.writeBoolean("logarithmic", logarithmic));
        return retVal;
    }
}
