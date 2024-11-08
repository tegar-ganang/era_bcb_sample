package org.biomage.BioAssay;

import java.io.Serializable;
import java.util.*;
import org.xml.sax.Attributes;
import java.io.Writer;
import java.io.IOException;

/**
 *  Provides classes that contain information and annotation on the event 
 *  of joining an Array with a BioMaterial preparation, the acquisition of 
 *  images and the extraction of data on a per feature basis from those 
 *  images.  The derived classes of BioAssay represent the base 
 *  PhysicalBioAssays which lead to the production of Images, the 
 *  MeasuredBioAssay which is associated with the set of quantitations 
 *  produced by FeatureExtraction, and DerivedBioAssay (see BioAssayData 
 *  package) which groups together BioAssays that have been analyzed 
 *  together to produce further refinement of the quantitations.
 *      
 *  The design of this package and the related BioAssayData package was 
 *  driven by the following query considerations and the desire to return as 
 *  little data as necessary to satisfy a query.   Often, the first set of 
 *  queries for experiments below the Experiment level will want to discover 
 *  the why of an experiment and this is captured in the BioAssay class 
 *  through its FactorValue, BioEvent and Description associations.  This 
 *  separates it from the data but allows an overview of the experiment 
 *  hierarchy.  The BioAssayData class association to BioDataValues is 
 *  optional only to allow queries on them to discover the how of the 
 *  experiment through the association to the transformation and mappings of 
 *  the three BioAssayData dimensions and the protocols used.  Once a 
 *  researcher, for instance, has narrowed down the experiments of interest 
 *  then the actual data, represented by the BioDataValues, can be 
 *  downloaded.  Because these data can be in the hundreds of megabytes to 
 *  gigabytes range, it was considered desirable to be able to return 
 *  information and annotation on the experiment without the data.
 *  
 */
public class BioAssay_package implements Serializable {

    /**
     *  Inner list class for holding multiple entries for attribute 
     *  channel.  Simply creates a named vector.
     *  
     */
    public class Channel_list extends Vector {
    }

    /**
     *  A channel represents an independent acquisition scheme for the 
     *  ImageAcquisition event, typically a wavelength.
     *  
     */
    public Channel_list channel_list = new Channel_list();

    /**
     *  Inner list class for holding multiple entries for attribute 
     *  bioAssay.  Simply creates a named vector.
     *  
     */
    public class BioAssay_list extends Vector {
    }

    /**
     *  An abstract class which represents both physical and 
     *  computational groupings of arrays and biomaterials.
     *  
     */
    public BioAssay_list bioAssay_list = new BioAssay_list();

    /**
     *  Default constructor.
     *  
     */
    public BioAssay_package() {
    }

    public BioAssay_package(Attributes atts) {
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
        out.write("<BioAssay_package");
        writeAttributes(out);
        out.write(">");
        writeAssociations(out);
        out.write("</BioAssay_package>");
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
        if (channel_list.size() > 0) {
            out.write("<Channel_assnlist>");
            for (int i = 0; i < channel_list.size(); i++) {
                ((Channel) channel_list.elementAt(i)).writeMAGEML(out);
            }
            out.write("</Channel_assnlist>");
        }
        if (bioAssay_list.size() > 0) {
            out.write("<BioAssay_assnlist>");
            for (int i = 0; i < bioAssay_list.size(); i++) {
                ((BioAssay) bioAssay_list.elementAt(i)).writeMAGEML(out);
            }
            out.write("</BioAssay_assnlist>");
        }
    }

    public String getModelClassName() {
        return new String("BioAssay_package");
    }

    /**
     *  Set method for channel_list
     *  
     *  @param value to set
     *  
     *  
     */
    public void setChannel_list(Channel_list channel_list) {
        this.channel_list = channel_list;
    }

    /**
     *  Get method for channel_list
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public Channel_list getChannel_list() {
        return channel_list;
    }

    /**
     *  Method to add Channel to Channel_list
     *  
     */
    public void addToChannel_list(Channel channel) {
        this.channel_list.add(channel);
    }

    /**
     *  Method to add Channel at position to Channel_list
     *  
     */
    public void addToChannel_list(int position, Channel channel) {
        this.channel_list.add(position, channel);
    }

    /**
     *  Method to get Channel from Channel_list
     *  
     */
    public Channel getFromChannel_list(int position) {
        return (Channel) this.channel_list.get(position);
    }

    /**
     *  Method to remove by position from Channel_list
     *  
     */
    public void removeElementAtFromChannel_list(int position) {
        this.channel_list.removeElementAt(position);
    }

    /**
     *  Method to remove first Channel from Channel_list
     *  
     */
    public void removeFromChannel_list(Channel channel) {
        this.channel_list.remove(channel);
    }

    /**
     *  Set method for bioAssay_list
     *  
     *  @param value to set
     *  
     *  
     */
    public void setBioAssay_list(BioAssay_list bioAssay_list) {
        this.bioAssay_list = bioAssay_list;
    }

    /**
     *  Get method for bioAssay_list
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public BioAssay_list getBioAssay_list() {
        return bioAssay_list;
    }

    /**
     *  Method to add BioAssay to BioAssay_list
     *  
     */
    public void addToBioAssay_list(BioAssay bioAssay) {
        this.bioAssay_list.add(bioAssay);
    }

    /**
     *  Method to add BioAssay at position to BioAssay_list
     *  
     */
    public void addToBioAssay_list(int position, BioAssay bioAssay) {
        this.bioAssay_list.add(position, bioAssay);
    }

    /**
     *  Method to get BioAssay from BioAssay_list
     *  
     */
    public BioAssay getFromBioAssay_list(int position) {
        return (BioAssay) this.bioAssay_list.get(position);
    }

    /**
     *  Method to remove by position from BioAssay_list
     *  
     */
    public void removeElementAtFromBioAssay_list(int position) {
        this.bioAssay_list.removeElementAt(position);
    }

    /**
     *  Method to remove first BioAssay from BioAssay_list
     *  
     */
    public void removeFromBioAssay_list(BioAssay bioAssay) {
        this.bioAssay_list.remove(bioAssay);
    }
}
