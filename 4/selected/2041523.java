package net.sourceforge.olduvai.lrac.genericdataservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import net.sourceforge.jglchartutil.datamodels.SimpleDataSeries;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.StripChannelBinding;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;

/**
 * Implements most of the functionality required for an InputChannelGroup. 
 * @see InputChannelGroupInterface
 * 
 * Developers note: it is not currently safe to have any given instance of an InputChannelGroup 
 * be displayed in more than one Strip.  
 * 
 * @author peter
 *
 */
public abstract class AbstractInputChannelGroup implements InputChannelGroupInterface {

    private List<InputChannelInterface> channels = new ArrayList<InputChannelInterface>();

    String name;

    String internalName;

    String type;

    String unit;

    /**
	 * Track the number of slots we've assigned.  This should never 
	 * exceed the number of slots available in the template group.  
	 */
    int templateSlotsAssigned = 0;

    /**
	 * Map that looks up a binding based on a simpleSeries uniqueID
	 */
    Map<String, StripChannelBinding> bindingLookup = new HashMap<String, StripChannelBinding>();

    /**
	 * Creates a new instance of this class.  Name and internalname can, and often are,
	 * identical.  Name provides a mechanism to provide a "human readable" string that 
	 * may differ from an internal object name.  
	 * 
	 * @param name "Human readable" name of this group. 
	 * @param internalName Unique internal key for this object.
	 * @param type A description of the "type" of group this is.  Types are currently
	 * arbitrary and descriptive.  Many data sources may have only one "type" of group.
	 * @param unit The type of units being displayed, ex. "%", "$", "ppm", ...  
	 */
    public AbstractInputChannelGroup(String name, String internalName, String type, String unit) {
        this.name = name;
        this.internalName = internalName;
        this.type = type;
        this.unit = unit;
    }

    public void addChannel(InputChannelInterface channel) {
        channels.add(channel);
    }

    public List<InputChannelInterface> getChannelList() {
        return channels;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getInternalType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUnit() {
        return unit;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public void setInternalType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
	 * Default comparison is by name
	 */
    public int compareTo(InputChannelItemInterface o) {
        return getName().compareTo(o.getName());
    }

    public String toString() {
        return getName() + " (" + getType() + ")";
    }

    public StripChannelBinding getBinding(Strip strip, Template template, SimpleDataSeries simpleSeries) {
        final String uniqueID = simpleSeries.getUniqueID();
        StripChannelBinding binding = bindingLookup.get(uniqueID);
        if (binding == null) {
            if (templateSlotsAssigned <= template.getCrayons().size()) {
                InputChannelInterface virtualChannel = new AbstractInputChannel(uniqueID, uniqueID, getType(), getUnit(), uniqueID) {

                    public Element getXML() {
                        return null;
                    }
                };
                binding = new StripChannelBinding(strip, virtualChannel, templateSlotsAssigned++);
            }
        }
        return binding;
    }
}
