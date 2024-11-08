package org.biomage.BioAssay;

import java.io.Serializable;
import java.util.*;
import org.xml.sax.Attributes;
import java.io.Writer;
import java.io.IOException;
import org.biomage.Interface.HasChannels;
import org.biomage.Interface.HasBioAssayFactorValues;
import org.biomage.Common.Identifiable;
import org.biomage.Experiment.FactorValue;

/**
 *  An abstract class which represents both physical and computational 
 *  groupings of arrays and biomaterials.
 *  
 */
public abstract class BioAssay extends Identifiable implements Serializable, HasChannels, HasBioAssayFactorValues {

    /**
     *  Channels can be non-null for all subclasses.  For instance, 
     *  collapsing across replicate features will create a DerivedBioAssay 
     *  that will potentially reference channels.
     *  
     */
    protected Channels_list channels = new Channels_list();

    /**
     *  The values that this BioAssay is associated with for the 
     *  experiment.
     *  
     */
    protected BioAssayFactorValues_list bioAssayFactorValues = new BioAssayFactorValues_list();

    /**
     *  Default constructor.
     *  
     */
    public BioAssay() {
        super();
    }

    public BioAssay(Attributes atts) {
        super(atts);
    }

    /**
     *  writeMAGEML
     *  
     *  This method is responsible for assembling the attribute and 
     *  association data into XML. It creates the object tag and then calls 
     *  the writeAttributes and writeAssociation methods.
     *  
     *  
     */
    public void writeMAGEML(Writer out) throws IOException {
    }

    /**
     *  writeAttributes
     *  
     *  This method is responsible for assembling the attribute data into 
     *  XML. It calls the super method to write out all attributes of this 
     *  class and it's ancestors.
     *  
     *  
     */
    public void writeAttributes(Writer out) throws IOException {
        super.writeAttributes(out);
    }

    /**
     *  writeAssociations
     *  
     *  This method is responsible for assembling the association data 
     *  into XML. It calls the super method to write out all associations of 
     *  this class's ancestors.
     *  
     *  
     */
    public void writeAssociations(Writer out) throws IOException {
        super.writeAssociations(out);
        if (channels.size() > 0) {
            out.write("<Channels_assnreflist>");
            for (int i = 0; i < channels.size(); i++) {
                String modelClassName = ((Channel) channels.elementAt(i)).getModelClassName();
                out.write("<" + modelClassName + "_ref identifier=\"" + ((Channel) channels.elementAt(i)).getIdentifier() + "\"/>");
            }
            out.write("</Channels_assnreflist>");
        }
        if (bioAssayFactorValues.size() > 0) {
            out.write("<BioAssayFactorValues_assnreflist>");
            for (int i = 0; i < bioAssayFactorValues.size(); i++) {
                String modelClassName = ((FactorValue) bioAssayFactorValues.elementAt(i)).getModelClassName();
                out.write("<" + modelClassName + "_ref identifier=\"" + ((FactorValue) bioAssayFactorValues.elementAt(i)).getIdentifier() + "\"/>");
            }
            out.write("</BioAssayFactorValues_assnreflist>");
        }
    }

    public String getModelClassName() {
        return new String("BioAssay");
    }

    /**
     *  Set method for channels
     *  
     *  @param value to set
     *  
     *  
     */
    public void setChannels(Channels_list channels) {
        this.channels = channels;
    }

    /**
     *  Get method for channels
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public Channels_list getChannels() {
        return channels;
    }

    /**
     *  Method to add Channel to Channels_list
     *  
     */
    public void addToChannels(Channel channel) {
        this.channels.add(channel);
    }

    /**
     *  Method to add Channel at position to Channels_list
     *  
     */
    public void addToChannels(int position, Channel channel) {
        this.channels.add(position, channel);
    }

    /**
     *  Method to get Channel from Channels_list
     *  
     */
    public Channel getFromChannels(int position) {
        return (Channel) this.channels.get(position);
    }

    /**
     *  Method to remove by position from Channels_list
     *  
     */
    public void removeElementAtFromChannels(int position) {
        this.channels.removeElementAt(position);
    }

    /**
     *  Method to remove first Channel from Channels_list
     *  
     */
    public void removeFromChannels(Channel channel) {
        this.channels.remove(channel);
    }

    /**
     *  Set method for bioAssayFactorValues
     *  
     *  @param value to set
     *  
     *  
     */
    public void setBioAssayFactorValues(BioAssayFactorValues_list bioAssayFactorValues) {
        this.bioAssayFactorValues = bioAssayFactorValues;
    }

    /**
     *  Get method for bioAssayFactorValues
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public BioAssayFactorValues_list getBioAssayFactorValues() {
        return bioAssayFactorValues;
    }

    /**
     *  Method to add FactorValue to BioAssayFactorValues_list
     *  
     */
    public void addToBioAssayFactorValues(FactorValue factorValue) {
        this.bioAssayFactorValues.add(factorValue);
    }

    /**
     *  Method to add FactorValue at position to 
     *  BioAssayFactorValues_list
     *  
     */
    public void addToBioAssayFactorValues(int position, FactorValue factorValue) {
        this.bioAssayFactorValues.add(position, factorValue);
    }

    /**
     *  Method to get FactorValue from BioAssayFactorValues_list
     *  
     */
    public FactorValue getFromBioAssayFactorValues(int position) {
        return (FactorValue) this.bioAssayFactorValues.get(position);
    }

    /**
     *  Method to remove by position from BioAssayFactorValues_list
     *  
     */
    public void removeElementAtFromBioAssayFactorValues(int position) {
        this.bioAssayFactorValues.removeElementAt(position);
    }

    /**
     *  Method to remove first FactorValue from BioAssayFactorValues_list
     *  
     */
    public void removeFromBioAssayFactorValues(FactorValue factorValue) {
        this.bioAssayFactorValues.remove(factorValue);
    }
}
