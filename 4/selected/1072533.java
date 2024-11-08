package org.apache.jetspeed.xml.api.jcm;

import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import org.exolab.castor.xml.*;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.xml.sax.DocumentHandler;

/**
 * 
 * @version $Revision$ $Date$
**/
public class Content implements java.io.Serializable {

    private java.lang.String _version;

    private Channel _channel;

    public Content() {
        super();
    }

    /**
    **/
    public Channel getChannel() {
        return this._channel;
    }

    /**
    **/
    public java.lang.String getVersion() {
        return this._version;
    }

    /**
    **/
    public boolean isValid() {
        try {
            validate();
        } catch (org.exolab.castor.xml.ValidationException vex) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @param out
    **/
    public void marshal(java.io.Writer out) throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, out);
    }

    /**
     * 
     * @param handler
    **/
    public void marshal(org.xml.sax.DocumentHandler handler) throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        Marshaller.marshal(this, handler);
    }

    /**
     * 
     * @param channel
    **/
    public void setChannel(Channel channel) {
        this._channel = channel;
    }

    /**
     * 
     * @param version
    **/
    public void setVersion(java.lang.String version) {
        this._version = version;
    }

    /**
     * 
     * @param reader
    **/
    public static org.apache.jetspeed.xml.api.jcm.Content unmarshal(java.io.Reader reader) throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
        return (org.apache.jetspeed.xml.api.jcm.Content) Unmarshaller.unmarshal(org.apache.jetspeed.xml.api.jcm.Content.class, reader);
    }

    /**
    **/
    public void validate() throws org.exolab.castor.xml.ValidationException {
        org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
        validator.validate(this);
    }
}
