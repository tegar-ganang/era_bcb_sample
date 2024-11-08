package org.mcisb.beacon.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="property_name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Channel" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="channel_name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="channel_intensity" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "propertyName", "channel" })
@XmlRootElement(name = "CellProperty")
public class CellProperty {

    @XmlElement(name = "property_name", required = true)
    protected String propertyName;

    @XmlElement(name = "Channel", required = true)
    protected List<CellProperty.Channel> channel;

    /**
     * Gets the value of the propertyName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the value of the propertyName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPropertyName(String value) {
        this.propertyName = value;
    }

    /**
     * Gets the value of the channel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the channel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChannel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CellProperty.Channel }
     * 
     * 
     */
    public List<CellProperty.Channel> getChannel() {
        if (channel == null) {
            channel = new ArrayList<CellProperty.Channel>();
        }
        return this.channel;
    }

    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="channel_name" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="channel_intensity" type="{http://www.w3.org/2001/XMLSchema}double"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = { "channelName", "channelIntensity" })
    public static class Channel {

        @XmlElement(name = "channel_name", required = true)
        protected String channelName;

        @XmlElement(name = "channel_intensity")
        protected double channelIntensity;

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
         * Gets the value of the channelIntensity property.
         * 
         */
        public double getChannelIntensity() {
            return channelIntensity;
        }

        /**
         * Sets the value of the channelIntensity property.
         * 
         */
        public void setChannelIntensity(double value) {
            this.channelIntensity = value;
        }
    }
}
