package org.biomage.QuantitationType;

import java.io.Serializable;
import java.util.*;
import org.xml.sax.Attributes;
import java.io.Writer;
import java.io.IOException;
import org.biomage.Interface.HasChannel;
import org.biomage.Interface.HasQuantitationTypeMaps;
import org.biomage.Interface.HasDataType;
import org.biomage.Interface.HasScale;
import org.biomage.Interface.HasConfidenceIndicators;
import org.biomage.BioAssay.Channel;
import org.biomage.BioAssayData.QuantitationTypeMap;
import org.biomage.Common.Identifiable;
import org.biomage.Description.OntologyEntry;

/**
 *  A method for calculating a single datum of the matrix (e.g. raw 
 *  intensity, background, error).
 *  
 */
public abstract class QuantitationType extends Identifiable implements Serializable, HasChannel, HasQuantitationTypeMaps, HasDataType, HasScale, HasConfidenceIndicators {

    /**
     *  Indicates whether the quantitation has been measured from the 
     *  background or from the feature itself.
     *  
     */
    boolean isBackground;

    /**
     *  The optional channel associated with the QuantitationType.
     *  
     */
    protected Channel channel;

    /**
     *  Indication of how to interpret the value.  From a suggested 
     *  vocabulary of {LINEAR | LN | LOG2 |LOG10 | FOLD_CHANGE | OTHER}
     *  
     */
    protected OntologyEntry scale;

    /**
     *  The specific type for the quantitations.  From a controlled 
     *  vocabulary of {float, int, boolean, etc.}
     *  
     */
    protected OntologyEntry dataType;

    /**
     *  The association between a ConfidenceIndicator and the 
     *  QuantitationType its is an indicator for.
     *  
     */
    protected ConfidenceIndicators_list confidenceIndicators = new ConfidenceIndicators_list();

    /**
     *  The QuantitationType whose value will be produced from the values 
     *  of the source QuantitationType according to the Protocol.
     *  
     */
    protected QuantitationTypeMaps_list quantitationTypeMaps = new QuantitationTypeMaps_list();

    /**
     *  Default constructor.
     *  
     */
    public QuantitationType() {
        super();
    }

    public QuantitationType(Attributes atts) {
        super(atts);
        {
            int nIndex = atts.getIndex("", "isBackground");
            if (nIndex != -1) {
                isBackground = (new Boolean(atts.getValue(nIndex))).booleanValue();
            }
        }
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
        out.write(" isBackground=\"" + isBackground + "\"");
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
        if (channel != null) {
            out.write("<Channel_assnref>");
            out.write("<" + channel.getModelClassName() + "_ref identifier=\"" + channel.getIdentifier() + "\"/>");
            out.write("</Channel_assnref>");
        }
        if (scale != null) {
            out.write("<Scale_assn>");
            scale.writeMAGEML(out);
            out.write("</Scale_assn>");
        }
        if (dataType != null) {
            out.write("<DataType_assn>");
            dataType.writeMAGEML(out);
            out.write("</DataType_assn>");
        }
        if (confidenceIndicators.size() > 0) {
            out.write("<ConfidenceIndicators_assnreflist>");
            for (int i = 0; i < confidenceIndicators.size(); i++) {
                String modelClassName = ((ConfidenceIndicator) confidenceIndicators.elementAt(i)).getModelClassName();
                out.write("<" + modelClassName + "_ref identifier=\"" + ((ConfidenceIndicator) confidenceIndicators.elementAt(i)).getIdentifier() + "\"/>");
            }
            out.write("</ConfidenceIndicators_assnreflist>");
        }
        if (quantitationTypeMaps.size() > 0) {
            out.write("<QuantitationTypeMaps_assnreflist>");
            for (int i = 0; i < quantitationTypeMaps.size(); i++) {
                String modelClassName = ((QuantitationTypeMap) quantitationTypeMaps.elementAt(i)).getModelClassName();
                out.write("<" + modelClassName + "_ref identifier=\"" + ((QuantitationTypeMap) quantitationTypeMaps.elementAt(i)).getIdentifier() + "\"/>");
            }
            out.write("</QuantitationTypeMaps_assnreflist>");
        }
    }

    /**
     *  Set method for isBackground
     *  
     *  @param value to set
     *  
     *  
     */
    public void setIsBackground(boolean isBackground) {
        this.isBackground = isBackground;
    }

    /**
     *  Get method for isBackground
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public boolean getIsBackground() {
        return isBackground;
    }

    public String getModelClassName() {
        return new String("QuantitationType");
    }

    /**
     *  Set method for channel
     *  
     *  @param value to set
     *  
     *  
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     *  Get method for channel
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     *  Set method for scale
     *  
     *  @param value to set
     *  
     *  
     */
    public void setScale(OntologyEntry scale) {
        this.scale = scale;
    }

    /**
     *  Get method for scale
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public OntologyEntry getScale() {
        return scale;
    }

    /**
     *  Set method for dataType
     *  
     *  @param value to set
     *  
     *  
     */
    public void setDataType(OntologyEntry dataType) {
        this.dataType = dataType;
    }

    /**
     *  Get method for dataType
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public OntologyEntry getDataType() {
        return dataType;
    }

    /**
     *  Set method for confidenceIndicators
     *  
     *  @param value to set
     *  
     *  
     */
    public void setConfidenceIndicators(ConfidenceIndicators_list confidenceIndicators) {
        this.confidenceIndicators = confidenceIndicators;
    }

    /**
     *  Get method for confidenceIndicators
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public ConfidenceIndicators_list getConfidenceIndicators() {
        return confidenceIndicators;
    }

    /**
     *  Method to add ConfidenceIndicator to ConfidenceIndicators_list
     *  
     */
    public void addToConfidenceIndicators(ConfidenceIndicator confidenceIndicator) {
        this.confidenceIndicators.add(confidenceIndicator);
    }

    /**
     *  Method to add ConfidenceIndicator at position to 
     *  ConfidenceIndicators_list
     *  
     */
    public void addToConfidenceIndicators(int position, ConfidenceIndicator confidenceIndicator) {
        this.confidenceIndicators.add(position, confidenceIndicator);
    }

    /**
     *  Method to get ConfidenceIndicator from ConfidenceIndicators_list
     *  
     */
    public ConfidenceIndicator getFromConfidenceIndicators(int position) {
        return (ConfidenceIndicator) this.confidenceIndicators.get(position);
    }

    /**
     *  Method to remove by position from ConfidenceIndicators_list
     *  
     */
    public void removeElementAtFromConfidenceIndicators(int position) {
        this.confidenceIndicators.removeElementAt(position);
    }

    /**
     *  Method to remove first ConfidenceIndicator from 
     *  ConfidenceIndicators_list
     *  
     */
    public void removeFromConfidenceIndicators(ConfidenceIndicator confidenceIndicator) {
        this.confidenceIndicators.remove(confidenceIndicator);
    }

    /**
     *  Set method for quantitationTypeMaps
     *  
     *  @param value to set
     *  
     *  
     */
    public void setQuantitationTypeMaps(QuantitationTypeMaps_list quantitationTypeMaps) {
        this.quantitationTypeMaps = quantitationTypeMaps;
    }

    /**
     *  Get method for quantitationTypeMaps
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public QuantitationTypeMaps_list getQuantitationTypeMaps() {
        return quantitationTypeMaps;
    }

    /**
     *  Method to add QuantitationTypeMap to QuantitationTypeMaps_list
     *  
     */
    public void addToQuantitationTypeMaps(QuantitationTypeMap quantitationTypeMap) {
        this.quantitationTypeMaps.add(quantitationTypeMap);
    }

    /**
     *  Method to add QuantitationTypeMap at position to 
     *  QuantitationTypeMaps_list
     *  
     */
    public void addToQuantitationTypeMaps(int position, QuantitationTypeMap quantitationTypeMap) {
        this.quantitationTypeMaps.add(position, quantitationTypeMap);
    }

    /**
     *  Method to get QuantitationTypeMap from QuantitationTypeMaps_list
     *  
     */
    public QuantitationTypeMap getFromQuantitationTypeMaps(int position) {
        return (QuantitationTypeMap) this.quantitationTypeMaps.get(position);
    }

    /**
     *  Method to remove by position from QuantitationTypeMaps_list
     *  
     */
    public void removeElementAtFromQuantitationTypeMaps(int position) {
        this.quantitationTypeMaps.removeElementAt(position);
    }

    /**
     *  Method to remove first QuantitationTypeMap from 
     *  QuantitationTypeMaps_list
     *  
     */
    public void removeFromQuantitationTypeMaps(QuantitationTypeMap quantitationTypeMap) {
        this.quantitationTypeMaps.remove(quantitationTypeMap);
    }
}
