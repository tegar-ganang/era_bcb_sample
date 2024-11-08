package org.ikasan.framework.exception;

/**
 * This class represents a part (<code>ResubmissionInfo</code>) of the
 * default exception message structure. <p/>
 * 
 * @author Ikasan Development Team
 */
public class ResubmissionInfo {

    /** channelName can be removed once BW CMI has been deprecated */
    private String channelName = null;

    /** modelName can be removed once BW CMI has been deprecated */
    private String modelName = null;

    /** essentially the interface name */
    private String componentGroupName = null;

    /** bean component in the interface */
    private String componentName = null;

    /**
     * Creates a new <code>ResubmissionInfo</code> instance.
     * 
     * @param channelName 
     * @param modelName 
     * @param componentGroupName 
     * @param componentName 
     */
    public ResubmissionInfo(final String channelName, final String modelName, final String componentGroupName, String componentName) {
        this.channelName = channelName;
        this.modelName = modelName;
        this.componentGroupName = componentGroupName;
        this.componentName = componentName;
    }

    /**
     * Creates a new <code>ResubmissionInfo</code> instance.
     * 
     */
    public ResubmissionInfo() {
    }

    /**
     * Sets the channel name.
     * 
     * @param channelName 
     */
    public void setChannelName(final String channelName) {
        this.channelName = channelName;
    }

    /**
     * Returns the channel name.
     * 
     * @return the channel name.
     */
    public String getChannelName() {
        return this.channelName;
    }

    /**
     * Sets the component model name.
     * 
     * @param modelName 
     */
    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    /**
     * Returns the component model name.
     * 
     * @return the component model name.
     */
    public String getModelName() {
        return this.modelName;
    }

    /**
     * Sets the component group name.
     * 
     * @param componentGroupName 
     */
    public void setComponentGroupName(final String componentGroupName) {
        this.componentGroupName = componentGroupName;
    }

    /**
     * Returns the component group name.
     * 
     * @return the component group name.
     */
    public String getComponentGroupName() {
        return this.componentGroupName;
    }

    /**
     * Sets the component name.
     * 
     * @param componentName 
     */
    public void setComponentName(final String componentName) {
        this.componentName = componentName;
    }

    /**
     * Returns the component name.
     * 
     * @return the component name.
     */
    public String getComponentName() {
        return this.componentName;
    }

    /**
     * Returns a string representation of this object.
     */
    @Override
    public String toString() {
        return "channelName: [" + this.channelName + "], " + "modelName: [" + this.modelName + "]" + "componentGroupName: [" + this.componentGroupName + "]" + "componentName: [" + this.componentName + "]";
    }
}
