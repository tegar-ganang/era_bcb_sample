package net.seismon.seismolinkClient.webservice;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for component complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="component">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="code" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="channel" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="azimuth" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="dip" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="gain" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "component", namespace = "http://geofon.gfz-potsdam.de/ns/inventory/0.2/")
public class Component {

    @XmlAttribute(name = "code", required = true)
    protected String code;

    @XmlAttribute(name = "channel", required = true)
    protected BigInteger channel;

    @XmlAttribute(name = "azimuth", required = true)
    protected double azimuth;

    @XmlAttribute(name = "dip", required = true)
    protected double dip;

    @XmlAttribute(name = "gain", required = true)
    protected double gain;

    /**
     * Gets the value of the code property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCode(String value) {
        this.code = value;
    }

    /**
     * Gets the value of the channel property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getChannel() {
        return channel;
    }

    /**
     * Sets the value of the channel property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setChannel(BigInteger value) {
        this.channel = value;
    }

    /**
     * Gets the value of the azimuth property.
     * 
     */
    public double getAzimuth() {
        return azimuth;
    }

    /**
     * Sets the value of the azimuth property.
     * 
     */
    public void setAzimuth(double value) {
        this.azimuth = value;
    }

    /**
     * Gets the value of the dip property.
     * 
     */
    public double getDip() {
        return dip;
    }

    /**
     * Sets the value of the dip property.
     * 
     */
    public void setDip(double value) {
        this.dip = value;
    }

    /**
     * Gets the value of the gain property.
     * 
     */
    public double getGain() {
        return gain;
    }

    /**
     * Sets the value of the gain property.
     * 
     */
    public void setGain(double value) {
        this.gain = value;
    }
}
