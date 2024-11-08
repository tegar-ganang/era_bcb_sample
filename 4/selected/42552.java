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
 *         &lt;element name="ChannelName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="PlasmidName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element ref="{}Peak" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="MaxPeak" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="Period" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="PeriodStdDev" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="DecayRate" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="FirstPeakTime" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "channelName", "plasmidName", "peak", "maxPeak", "period", "periodStdDev", "decayRate", "firstPeakTime" })
@XmlRootElement(name = "AnalysedChannel")
public class AnalysedChannel {

    @XmlElement(name = "ChannelName", required = true)
    protected String channelName;

    @XmlElement(name = "PlasmidName", required = true)
    protected String plasmidName;

    @XmlElement(name = "Peak")
    protected List<Peak> peak;

    @XmlElement(name = "MaxPeak")
    protected Double maxPeak;

    @XmlElement(name = "Period")
    protected Double period;

    @XmlElement(name = "PeriodStdDev")
    protected Double periodStdDev;

    @XmlElement(name = "DecayRate")
    protected Double decayRate;

    @XmlElement(name = "FirstPeakTime")
    protected Double firstPeakTime;

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
     * Gets the value of the plasmidName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlasmidName() {
        return plasmidName;
    }

    /**
     * Sets the value of the plasmidName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlasmidName(String value) {
        this.plasmidName = value;
    }

    /**
     * Gets the value of the peak property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the peak property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeak().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Peak }
     * 
     * 
     */
    public List<Peak> getPeak() {
        if (peak == null) {
            peak = new ArrayList<Peak>();
        }
        return this.peak;
    }

    /**
     * Gets the value of the maxPeak property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMaxPeak() {
        return maxPeak;
    }

    /**
     * Sets the value of the maxPeak property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMaxPeak(Double value) {
        this.maxPeak = value;
    }

    /**
     * Gets the value of the period property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPeriod(Double value) {
        this.period = value;
    }

    /**
     * Gets the value of the periodStdDev property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getPeriodStdDev() {
        return periodStdDev;
    }

    /**
     * Sets the value of the periodStdDev property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPeriodStdDev(Double value) {
        this.periodStdDev = value;
    }

    /**
     * Gets the value of the decayRate property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getDecayRate() {
        return decayRate;
    }

    /**
     * Sets the value of the decayRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setDecayRate(Double value) {
        this.decayRate = value;
    }

    /**
     * Gets the value of the firstPeakTime property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getFirstPeakTime() {
        return firstPeakTime;
    }

    /**
     * Sets the value of the firstPeakTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setFirstPeakTime(Double value) {
        this.firstPeakTime = value;
    }
}
