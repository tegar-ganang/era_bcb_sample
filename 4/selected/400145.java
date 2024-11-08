package org.biomage.BioAssay;

import java.io.Serializable;
import java.util.*;
import org.xml.sax.Attributes;
import java.io.Writer;
import java.io.IOException;
import org.biomage.Interface.HasChannels;
import org.biomage.Interface.HasFormat;
import org.biomage.Common.Identifiable;
import org.biomage.Description.OntologyEntry;

/**
 *  An image is created by an imageAcquisition event, typically by 
 *  scanning the hybridized array (the PhysicalBioAssay).
 *  
 */
public class Image extends Identifiable implements Serializable, HasChannels, HasFormat {

    /**
     *  The file location in which an image may be found.
     *  
     */
    String URI;

    /**
     *  The channels captured in this image.
     *  
     */
    protected Channels_list channels = new Channels_list();

    /**
     *  The file format of the image typically a TIF or a JPEG.
     *  
     */
    protected OntologyEntry format;

    /**
     *  Default constructor.
     *  
     */
    public Image() {
        super();
    }

    public Image(Attributes atts) {
        super(atts);
        {
            int nIndex = atts.getIndex("", "URI");
            if (nIndex != -1) {
                URI = atts.getValue(nIndex);
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
        out.write("<Image");
        writeAttributes(out);
        out.write(">");
        writeAssociations(out);
        out.write("</Image>");
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
        if (URI != null) {
            out.write(" URI=\"" + URI + "\"");
        }
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
        if (format != null) {
            out.write("<Format_assn>");
            format.writeMAGEML(out);
            out.write("</Format_assn>");
        }
    }

    /**
     *  Set method for URI
     *  
     *  @param value to set
     *  
     *  
     */
    public void setURI(String URI) {
        this.URI = URI;
    }

    /**
     *  Get method for URI
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public String getURI() {
        return URI;
    }

    public String getModelClassName() {
        return new String("Image");
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
     *  Set method for format
     *  
     *  @param value to set
     *  
     *  
     */
    public void setFormat(OntologyEntry format) {
        this.format = format;
    }

    /**
     *  Get method for format
     *  
     *  @return value of the attribute
     *  
     *  
     */
    public OntologyEntry getFormat() {
        return format;
    }
}
