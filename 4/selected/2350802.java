package org.mitre.caasd.aixmj.feature.procedure.approach;

import org.dom4j.Document;
import org.dom4j.Element;
import org.mitre.caasd.aixmj.data.AixmCodeApproachPrefixType;
import org.mitre.caasd.aixmj.data.AixmCodeApproachType;
import org.mitre.caasd.aixmj.data.ObjectHash;
import org.mitre.caasd.aixmj.feature.procedure.AixmProcedureTimeSlice;
import org.mitre.caasd.aixmj.util.AixmUtil;

/**
 * Implementation for an instrument approach procedure
 * 
 * @author FARAIN
 *
 */
public class AixmInstrumentApproachProcedureTimeSlice extends AixmProcedureTimeSlice {

    private AixmCodeApproachPrefixType prefixType;

    private AixmCodeApproachType approachType;

    private String multipleIdentification;

    private Double copterTrackBearing;

    private String circlingIdentification;

    private String name;

    private String courseReversalDescription;

    private String additionalEquipment;

    private String channelGNSS;

    public AixmInstrumentApproachProcedureTimeSlice parseElements(Element element) {
        String gmlId = element.attributeValue("id");
        Object o = ObjectHash.getObject(gmlId);
        if (o != null) return (AixmInstrumentApproachProcedureTimeSlice) o;
        AixmProcedureTimeSlice.setFields(element, this);
        this.setGmlId(gmlId);
        this.prefixType = (AixmCodeApproachPrefixType) AixmUtil.parseEnum("approachPrefix", element, AixmCodeApproachPrefixType.CONVERGING);
        this.approachType = (AixmCodeApproachType) AixmUtil.parseEnum("approachType", element, AixmCodeApproachType.ARA);
        this.multipleIdentification = AixmUtil.parseString("multipleIdentification", element);
        this.copterTrackBearing = AixmUtil.parseDouble("copterTrackBearing", element);
        this.circlingIdentification = AixmUtil.parseString("circlingIdentification", element);
        this.name = AixmUtil.parseString("name", element);
        this.courseReversalDescription = AixmUtil.parseString("courseReversalDescription", element);
        this.additionalEquipment = AixmUtil.parseString("additionalEquipment", element);
        this.channelGNSS = AixmUtil.parseString("channelGNSS", element);
        return this;
    }

    public Element constructElements(Element element, Document d) {
        AixmProcedureTimeSlice.setElements(element, this, d);
        AixmUtil.constructSingleElement(this.prefixType, element, "approachPrefix");
        AixmUtil.constructSingleElement(this.approachType, element, "approachType");
        AixmUtil.constructSingleElement(this.multipleIdentification, element, "multipleIdentification");
        AixmUtil.constructSingleElement(this.copterTrackBearing, element, "copterTrackBearing");
        AixmUtil.constructSingleElement(this.circlingIdentification, element, "circlingIdentification");
        AixmUtil.constructSingleElement(this.name, element, "name");
        AixmUtil.constructSingleElement(this.courseReversalDescription, element, "courseReversalDescription");
        AixmUtil.constructSingleElement(this.additionalEquipment, element, "additionalEquipment");
        AixmUtil.constructSingleElement(this.channelGNSS, element, "channelGNSS");
        return element;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the prefixType
     */
    public AixmCodeApproachPrefixType getPrefixType() {
        return prefixType;
    }

    /**
     * @param prefixType the prefixType to set
     */
    public void setPrefixType(AixmCodeApproachPrefixType prefixType) {
        this.prefixType = prefixType;
    }

    /**
     * @return the approachType
     */
    public AixmCodeApproachType getApproachType() {
        return approachType;
    }

    /**
     * @param approachType the approachType to set
     */
    public void setApproachType(AixmCodeApproachType approachType) {
        this.approachType = approachType;
    }

    /**
     * @return the multipleIdentification
     */
    public String getMultipleIdentification() {
        return multipleIdentification;
    }

    /**
     * @param multipleIdentification the multipleIdentification to set
     */
    public void setMultipleIdentification(String multipleIdentification) {
        this.multipleIdentification = multipleIdentification;
    }

    /**
     * @return the copterTrackBearing
     */
    public Double getCopterTrackBearing() {
        return copterTrackBearing;
    }

    /**
     * @param copterTrackBearing the copterTrackBearing to set
     */
    public void setCopterTrackBearing(Double copterTrackBearing) {
        this.copterTrackBearing = copterTrackBearing;
    }

    /**
     * @return the circlingIdentification
     */
    public String getCirclingIdentification() {
        return circlingIdentification;
    }

    /**
     * @param circlingIdentification the circlingIdentification to set
     */
    public void setCirclingIdentification(String circlingIdentification) {
        this.circlingIdentification = circlingIdentification;
    }

    /**
     * @return the courseReversalDescription
     */
    public String getCourseReversalDescription() {
        return courseReversalDescription;
    }

    /**
     * @param courseReversalDescription the courseReversalDescription to set
     */
    public void setCourseReversalDescription(String courseReversalDescription) {
        this.courseReversalDescription = courseReversalDescription;
    }

    /**
     * @return the additionalEquipment
     */
    public String getAdditionalEquipment() {
        return additionalEquipment;
    }

    /**
     * @param additionalEquipment the additionalEquipment to set
     */
    public void setAdditionalEquipment(String additionalEquipment) {
        this.additionalEquipment = additionalEquipment;
    }

    /**
     * @return the channelGNSS
     */
    public String getChannelGNSS() {
        return channelGNSS;
    }

    /**
     * @param channelGNSS the channelGNSS to set
     */
    public void setChannelGNSS(String channelGNSS) {
        this.channelGNSS = channelGNSS;
    }
}
