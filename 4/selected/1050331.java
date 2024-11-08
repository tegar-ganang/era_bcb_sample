package gov.sns.apps.quadshaker.utils;

import gov.sns.ca.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.scan.SecondEdition.WrappedChannel;

/**
 *  Description of the Class
 *
 *@author     shishlo
 *@created    December 12, 2006
 */
public class BPM_Element implements Dev_Element {

    private String name = "null";

    private WrappedChannel wrpChX = new WrappedChannel();

    private WrappedChannel wrpChY = new WrappedChannel();

    private Boolean isActive = new Boolean(false);

    /**
	 *  Constructor for the BPM_Element object
	 */
    public BPM_Element() {
    }

    /**
	 *  Constructor for the BPM_Element object
	 *
	 *@param  name_in  The Parameter
	 */
    public BPM_Element(String name_in) {
        name = name_in;
    }

    /**
	 *  Returns the name attribute of the BPM_Element object
	 *
	 *@return    The name value
	 */
    public String getName() {
        return name;
    }

    /**
	 *  Sets the name attribute of the BPM_Element object
	 *
	 *@param  name  The new name value
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 *  Returns the wrpChannelX attribute of the BPM_Element object
	 *
	 *@return    The wrpChannelX value
	 */
    public WrappedChannel getWrpChannelX() {
        return wrpChX;
    }

    /**
	 *  Returns the wrpChannelY attribute of the BPM_Element object
	 *
	 *@return    The wrpChannelY value
	 */
    public WrappedChannel getWrpChannelY() {
        return wrpChY;
    }

    /**
	 *  Description of the Method
	 */
    public void startMonitor() {
        wrpChX.startMonitor();
        wrpChX.startMonitor();
    }

    /**
	 *  Description of the Method
	 */
    public void stopMonitor() {
        wrpChX.stopMonitor();
        wrpChY.stopMonitor();
    }

    /**
	 *  Returns the x attribute of the BPM_Element object
	 *
	 *@return    The x value
	 */
    public double getX() {
        return wrpChX.getValue();
    }

    /**
	 *  Returns the y attribute of the BPM_Element object
	 *
	 *@return    The y value
	 */
    public double getY() {
        return wrpChY.getValue();
    }

    /**
	 *  Returns the activeObj attribute of the BPM_Element object
	 *
	 *@return    The activeObj value
	 */
    public Boolean isActiveObj() {
        return isActive;
    }

    /**
	 *  Returns the active attribute of the BPM_Element object
	 *
	 *@return    The active value
	 */
    public boolean isActive() {
        return isActive.booleanValue();
    }

    /**
	 *  Sets the active attribute of the BPM_Element object
	 *
	 *@param  state  The new active value
	 */
    public void setActive(boolean state) {
        if (state != isActive.booleanValue()) {
            isActive = new Boolean(state);
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@param  da  The Parameter
	 */
    public void dumpData(XmlDataAdaptor da) {
        da.setValue("name", name);
        da.setValue("xPV", wrpChX.getChannelName());
        da.setValue("yPV", wrpChY.getChannelName());
        da.setValue("isActive", isActive.booleanValue());
    }

    /**
	 *  Description of the Method
	 *
	 *@param  da  The Parameter
	 */
    public void readData(XmlDataAdaptor da) {
        name = da.stringValue("name");
        wrpChX.setChannelName(da.stringValue("xPV"));
        wrpChY.setChannelName(da.stringValue("yPV"));
        setActive(da.booleanValue("isActive"));
    }
}
