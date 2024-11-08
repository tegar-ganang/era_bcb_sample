package org.apache.jetspeed.xml.api.jcm;

import org.exolab.castor.mapping.AccessMode;
import org.exolab.castor.mapping.ClassDescriptor;
import org.exolab.castor.mapping.FieldDescriptor;
import org.exolab.castor.xml.*;
import org.exolab.castor.xml.FieldValidator;
import org.exolab.castor.xml.TypeValidator;
import org.exolab.castor.xml.XMLFieldDescriptor;
import org.exolab.castor.xml.handlers.*;
import org.exolab.castor.xml.util.XMLFieldDescriptorImpl;
import org.exolab.castor.xml.validators.*;

/**
 * 
 * @version $Revision$ $Date$
**/
public class ContentDescriptor extends org.exolab.castor.xml.util.XMLClassDescriptorImpl {

    private java.lang.String nsPrefix;

    private java.lang.String nsURI;

    private java.lang.String xmlName;

    private org.exolab.castor.xml.XMLFieldDescriptor identity;

    public ContentDescriptor() {
        super();
        nsURI = "http://jakarta.apache.org/jetspeed/xml/jetspeed-portal-content";
        xmlName = "content";
        XMLFieldDescriptorImpl desc = null;
        XMLFieldHandler handler = null;
        FieldValidator fieldValidator = null;
        desc = new XMLFieldDescriptorImpl(java.lang.String.class, "_version", "version", NodeType.Attribute);
        desc.setImmutable(true);
        handler = (new XMLFieldHandler() {

            public Object getValue(Object object) throws IllegalStateException {
                Content target = (Content) object;
                return target.getVersion();
            }

            public void setValue(Object object, Object value) throws IllegalStateException, IllegalArgumentException {
                try {
                    Content target = (Content) object;
                    target.setVersion((java.lang.String) value);
                } catch (Exception ex) {
                    throw new IllegalStateException(ex.toString());
                }
            }

            public Object newInstance(Object parent) {
                return null;
            }
        });
        desc.setHandler(handler);
        desc.setNameSpaceURI("http://jakarta.apache.org/jetspeed/xml/jetspeed-portal-content");
        addFieldDescriptor(desc);
        fieldValidator = new FieldValidator();
        {
            StringValidator sv = new StringValidator();
            sv.setWhiteSpace("preserve");
            fieldValidator.setValidator(sv);
        }
        desc.setValidator(fieldValidator);
        desc = new XMLFieldDescriptorImpl(Channel.class, "_channel", "channel", NodeType.Element);
        handler = (new XMLFieldHandler() {

            public Object getValue(Object object) throws IllegalStateException {
                Content target = (Content) object;
                return target.getChannel();
            }

            public void setValue(Object object, Object value) throws IllegalStateException, IllegalArgumentException {
                try {
                    Content target = (Content) object;
                    target.setChannel((Channel) value);
                } catch (Exception ex) {
                    throw new IllegalStateException(ex.toString());
                }
            }

            public Object newInstance(Object parent) {
                return new Channel();
            }
        });
        desc.setHandler(handler);
        desc.setNameSpaceURI("http://jakarta.apache.org/jetspeed/xml/jetspeed-portal-content");
        desc.setRequired(true);
        desc.setMultivalued(false);
        addFieldDescriptor(desc);
        fieldValidator = new FieldValidator();
        fieldValidator.setMinOccurs(1);
        desc.setValidator(fieldValidator);
    }

    /**
    **/
    public org.exolab.castor.mapping.AccessMode getAccessMode() {
        return null;
    }

    /**
    **/
    public org.exolab.castor.mapping.ClassDescriptor getExtends() {
        return null;
    }

    /**
    **/
    public org.exolab.castor.mapping.FieldDescriptor getIdentity() {
        return identity;
    }

    /**
    **/
    public java.lang.Class getJavaClass() {
        return org.apache.jetspeed.xml.api.jcm.Content.class;
    }

    /**
    **/
    public java.lang.String getNameSpacePrefix() {
        return nsPrefix;
    }

    /**
    **/
    public java.lang.String getNameSpaceURI() {
        return nsURI;
    }

    /**
    **/
    public org.exolab.castor.xml.TypeValidator getValidator() {
        return this;
    }

    /**
    **/
    public java.lang.String getXMLName() {
        return xmlName;
    }
}
