package net.sourceforge.olduvai.lrac.drawer.strips;

import java.util.Iterator;
import java.util.List;
import net.sourceforge.olduvai.lrac.util.Util;
import org.jdom.Element;

/**
 * StripChannel defines the name of an InputChannel associated with the strip.
 * It provides a label for that InputChannel (to be used when creating dataseries object). 
 * It also provides a helper method for creating a dataseries object using the strip's 
 * specified template. 
 *   
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class StripChannel implements Comparable<StripChannel> {

    static final String ROOTTAG = "inputchannel";

    static final String CHANNELIDTAG = "id";

    static final String TYPETAG = "type";

    static final String DESCRIPTIONTAG = "description";

    static final String LABELTAG = "label";

    static final String TEMPLATESLOTTAG = "templateslot";

    public static final int ALARM = 0;

    public static final int STAT = 1;

    /**
	 * Prefixes are 'fake' channels that map to a StripChannelGroup (a list of other StripChannels)
	 */
    public static final int PREFIX = 2;

    public static final String[] CHANNELTYPES = { "Alarms", "Stats", "Prefix" };

    public static final String[] CHANNELTYPESHORTS = { "a", "s", "p" };

    private static final String ALARMTYPESHORT = "a";

    private static final String STATTYPESHORT = "s";

    private static final String PREFIXTYPESHORT = "p";

    public static int getChannelType(String channelType) {
        for (int i = 0; i < CHANNELTYPES.length; i++) {
            if (CHANNELTYPES[i].equals(channelType)) return i;
        }
        return -1;
    }

    private static final String TOKENIZER = "\\|";

    Element rootElement;

    /**
	 * SWIFT channel identifier
	 * ('alarmid' field for alarms, 'id' field for stats)
	 */
    String channelID;

    Element channelIDElement;

    String label;

    Element labelElement;

    int templateSlot;

    Element templateSlotElement;

    /**
	 * The type of channel, either ALARM or METRIC
	 * @see #ALARM
	 * @see #STAT
	 */
    int type;

    Element typeElement;

    /**
	 * Text description of the channel
	 */
    String description;

    Element descriptionElement;

    Strip parentStrip;

    /**
	 * @param pp 
	 * @param e 
	 * 
	 */
    public StripChannel(Element root, Strip parentStrip) {
        rootElement = root;
        this.parentStrip = parentStrip;
        List l = root.getChildren();
        Element e;
        Iterator it = l.iterator();
        while (it.hasNext()) {
            e = (Element) it.next();
            if (e.getName().equals(CHANNELIDTAG)) {
                setChannelID(e);
            } else if (e.getName().equals(TYPETAG)) {
                setType(e);
            } else if (e.getName().equals(DESCRIPTIONTAG)) {
                setDescription(e);
            } else if (e.getName().equals(LABELTAG)) {
                setLabel(e);
            } else if (e.getName().equals(TEMPLATESLOTTAG)) {
                templateSlotElement = e;
                templateSlot = Integer.parseInt(templateSlotElement.getTextTrim());
            } else {
                System.err.println("StripChannel: unhandled child node: " + e.getName() + " value: " + e.getText());
            }
        }
        if (label == null) setLabel(getDescription());
    }

    /**
	 * Creates a new stripChannel from parameters, adds & configures XML components
	 * 
	 * @param type The type of channel, either ALARM or METRIC
	 * @param channelID SWIFT channel identifier
	 * @param description Text description of the channel
	 */
    public StripChannel(int type, String channelID, String description) {
        rootElement = new Element(ROOTTAG);
        setType(type);
        setChannelID(channelID);
        setDescription(description);
        setLabel(description);
    }

    public void setType(Element typeEl) {
        this.typeElement = typeEl;
        String typeStr = typeEl.getTextTrim();
        int type = -1;
        if (typeStr.equals(ALARMTYPESHORT)) type = ALARM; else if (typeStr.equals(STATTYPESHORT)) type = STAT; else if (typeStr.equals(PREFIXTYPESHORT)) type = PREFIX;
        this.type = type;
    }

    public void setType(int type) {
        this.type = type;
        rootElement.removeContent(typeElement);
        typeElement = new Element(TYPETAG);
        typeElement.setText(CHANNELTYPESHORTS[type]);
        rootElement.addContent(typeElement);
    }

    public void setDescription(Element descEl) {
        this.descriptionElement = descEl;
        this.description = descEl.getTextTrim();
    }

    public void setDescription(String desc) {
        this.description = desc;
        rootElement.removeContent(descriptionElement);
        descriptionElement = new Element(DESCRIPTIONTAG);
        descriptionElement.setText(desc);
        rootElement.addContent(descriptionElement);
    }

    public String toString() {
        return getLabel();
    }

    public String getLabel() {
        return label;
    }

    private void setChannelID(Element idElement) {
        channelIDElement = idElement;
        channelID = idElement.getTextTrim();
    }

    public void setChannelID(String channelId) {
        this.channelID = channelId;
        rootElement.removeContent(channelIDElement);
        channelIDElement = new Element(CHANNELIDTAG);
        channelIDElement.setText(channelId);
        rootElement.addContent(channelIDElement);
    }

    private void setLabel(Element labelElement) {
        this.labelElement = labelElement;
        label = labelElement.getTextTrim();
    }

    public void setLabel(String label) {
        this.label = label;
        rootElement.removeContent(labelElement);
        labelElement = new Element(LABELTAG);
        labelElement.setText(label);
        rootElement.addContent(labelElement);
    }

    public int getTemplateSlot() {
        return templateSlot;
    }

    public void setTemplateSlot(int templateSlot) {
        this.templateSlot = templateSlot;
        rootElement.removeContent(templateSlotElement);
        templateSlotElement = new Element(TEMPLATESLOTTAG);
        templateSlotElement.setText(Integer.toString(templateSlot));
        rootElement.addContent(templateSlotElement);
    }

    public int compareTo(StripChannel o) {
        if (getTemplateSlot() < o.getTemplateSlot()) return -1; else if (getTemplateSlot() > o.getTemplateSlot()) return 1;
        return 0;
    }

    public String getChannelID() {
        return channelID;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static final StripChannel createChannel(String tokenString) {
        String[] tok = Util.myTokenize(tokenString, TOKENIZER);
        if (tok.length != 3) {
            System.err.println("StripChannel.createChannel: invalid number of channel fields");
            return null;
        }
        int type = -1;
        if (tok[0].equals(ALARMTYPESHORT)) type = ALARM; else if (tok[0].equals(STATTYPESHORT)) type = STAT; else if (tok[0].equals(PREFIXTYPESHORT)) type = PREFIX; else System.err.println("ChannelDescription: Invalid record type field: " + tok[0]);
        return new StripChannel(type, tok[1], tok[2]);
    }

    /**
	 * Copies fields from the parameter to this object
	 * @param channel Channel to copy settings FROM
	 */
    public void importSettings(StripChannel channel) {
        if (channel == null) {
            return;
        }
        setChannelID(channel.getChannelID());
        setLabel(channel.getLabel());
        setType(channel.getType());
        if (parentStrip != null) parentStrip.getStripHandler().rebuildChannelStripMap();
    }
}
