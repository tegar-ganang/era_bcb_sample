package blue.orchestra.blueSynthBuilder;

import blue.mixer.Channel;
import electric.xml.Element;
import electric.xml.Elements;

public class BSBSubChannelDropdown extends BSBObject {

    String channelOutput = Channel.MASTER;

    public static BSBObject loadFromXML(Element data) {
        BSBSubChannelDropdown dropDown = new BSBSubChannelDropdown();
        initBasicFromXML(data, dropDown);
        Elements nodes = data.getElements();
        while (nodes.hasMoreElements()) {
            Element elem = nodes.next();
            String name = elem.getName();
            if (name.equals("channelOutput")) {
                dropDown.channelOutput = elem.getTextString();
            }
        }
        return dropDown;
    }

    public Element saveAsXML() {
        Element retVal = getBasicXML(this);
        retVal.addElement("channelOutput").setText(channelOutput);
        return retVal;
    }

    public String getPresetValue() {
        return null;
    }

    public void setPresetValue(String val) {
    }

    public void setupForCompilation(BSBCompilationUnit compilationUnit) {
        compilationUnit.addReplacementValue(objectName, channelOutput);
    }

    public String getChannelOutput() {
        return channelOutput;
    }

    public void setChannelOutput(String channelOutput) {
        if (this.channelOutput.equals(channelOutput)) {
            return;
        }
        String oldChannel = this.channelOutput;
        this.channelOutput = channelOutput;
        if (propListeners != null) {
            propListeners.firePropertyChange("channelOutput", oldChannel, this.channelOutput);
        }
    }
}
