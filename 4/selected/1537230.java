package com.liferay.portal.wsrp.consumer.admin;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for WSRPPortlet complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WSRPPortlet">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PortletId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ChannelName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Title" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ShortTitle" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DisplayName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Keywords" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Status" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ProducerEntityId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ConsumerId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="PortletHandle" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="mimeTypes" type="{http://www.sun.com/portal/wsrp/consumer}MimeType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WSRPPortlet", propOrder = { "portletId", "channelName", "title", "shortTitle", "displayName", "keywords", "status", "producerEntityId", "consumerId", "portletHandle", "mimeTypes" })
public class WSRPPortlet {

    @XmlElement(name = "PortletId", required = true)
    protected String portletId;

    @XmlElement(name = "ChannelName", required = true)
    protected String channelName;

    @XmlElement(name = "Title", required = true)
    protected String title;

    @XmlElement(name = "ShortTitle", required = true)
    protected String shortTitle;

    @XmlElement(name = "DisplayName", required = true)
    protected String displayName;

    @XmlElement(name = "Keywords", required = true)
    protected String keywords;

    @XmlElement(name = "Status")
    protected boolean status;

    @XmlElement(name = "ProducerEntityId", required = true)
    protected String producerEntityId;

    @XmlElement(name = "ConsumerId", required = true)
    protected String consumerId;

    @XmlElement(name = "PortletHandle", required = true)
    protected String portletHandle;

    @XmlElement(required = true)
    protected List<MimeType> mimeTypes;

    /**
     * Gets the value of the portletId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPortletId() {
        return portletId;
    }

    /**
     * Sets the value of the portletId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPortletId(String value) {
        this.portletId = value;
    }

    /**
     * Gets the value of the channelName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Sets the value of the channelName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChannelName(String value) {
        this.channelName = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the shortTitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShortTitle() {
        return shortTitle;
    }

    /**
     * Sets the value of the shortTitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShortTitle(String value) {
        this.shortTitle = value;
    }

    /**
     * Gets the value of the displayName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the value of the displayName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDisplayName(String value) {
        this.displayName = value;
    }

    /**
     * Gets the value of the keywords property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Sets the value of the keywords property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKeywords(String value) {
        this.keywords = value;
    }

    /**
     * Gets the value of the status property.
     * 
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     */
    public void setStatus(boolean value) {
        this.status = value;
    }

    /**
     * Gets the value of the producerEntityId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProducerEntityId() {
        return producerEntityId;
    }

    /**
     * Sets the value of the producerEntityId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProducerEntityId(String value) {
        this.producerEntityId = value;
    }

    /**
     * Gets the value of the consumerId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConsumerId() {
        return consumerId;
    }

    /**
     * Sets the value of the consumerId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConsumerId(String value) {
        this.consumerId = value;
    }

    /**
     * Gets the value of the portletHandle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPortletHandle() {
        return portletHandle;
    }

    /**
     * Sets the value of the portletHandle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPortletHandle(String value) {
        this.portletHandle = value;
    }

    /**
     * Gets the value of the mimeTypes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mimeTypes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMimeTypes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MimeType }
     * 
     * 
     */
    public List<MimeType> getMimeTypes() {
        if (mimeTypes == null) {
            mimeTypes = new ArrayList<MimeType>();
        }
        return this.mimeTypes;
    }
}
