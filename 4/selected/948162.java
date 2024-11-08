package CommonAggregateComponents_2.xsd.schema.ubl.specification.names.oasis;

public class CommunicationType implements java.io.Serializable {

    private CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelCodeType channelCode;

    private CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelType channel;

    private CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ValueType value;

    private long hjid;

    public CommunicationType() {
    }

    public CommunicationType(CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelCodeType channelCode, CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelType channel, CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ValueType value, long hjid) {
        this.channelCode = channelCode;
        this.channel = channel;
        this.value = value;
        this.hjid = hjid;
    }

    /**
     * Gets the channelCode value for this CommunicationType.
     * 
     * @return channelCode
     */
    public CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelCodeType getChannelCode() {
        return channelCode;
    }

    /**
     * Sets the channelCode value for this CommunicationType.
     * 
     * @param channelCode
     */
    public void setChannelCode(CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelCodeType channelCode) {
        this.channelCode = channelCode;
    }

    /**
     * Gets the channel value for this CommunicationType.
     * 
     * @return channel
     */
    public CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelType getChannel() {
        return channel;
    }

    /**
     * Sets the channel value for this CommunicationType.
     * 
     * @param channel
     */
    public void setChannel(CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ChannelType channel) {
        this.channel = channel;
    }

    /**
     * Gets the value value for this CommunicationType.
     * 
     * @return value
     */
    public CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ValueType getValue() {
        return value;
    }

    /**
     * Sets the value value for this CommunicationType.
     * 
     * @param value
     */
    public void setValue(CommonBasicComponents_2.xsd.schema.ubl.specification.names.oasis.ValueType value) {
        this.value = value;
    }

    /**
     * Gets the hjid value for this CommunicationType.
     * 
     * @return hjid
     */
    public long getHjid() {
        return hjid;
    }

    /**
     * Sets the hjid value for this CommunicationType.
     * 
     * @param hjid
     */
    public void setHjid(long hjid) {
        this.hjid = hjid;
    }

    private java.lang.Object __equalsCalc = null;

    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CommunicationType)) return false;
        CommunicationType other = (CommunicationType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && ((this.channelCode == null && other.getChannelCode() == null) || (this.channelCode != null && this.channelCode.equals(other.getChannelCode()))) && ((this.channel == null && other.getChannel() == null) || (this.channel != null && this.channel.equals(other.getChannel()))) && ((this.value == null && other.getValue() == null) || (this.value != null && this.value.equals(other.getValue()))) && this.hjid == other.getHjid();
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;

    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getChannelCode() != null) {
            _hashCode += getChannelCode().hashCode();
        }
        if (getChannel() != null) {
            _hashCode += getChannel().hashCode();
        }
        if (getValue() != null) {
            _hashCode += getValue().hashCode();
        }
        _hashCode += new Long(getHjid()).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(CommunicationType.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2", "CommunicationType"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("hjid");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Hjid"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        typeDesc.addFieldDesc(attrField);
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("channelCode");
        elemField.setXmlName(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "ChannelCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "ChannelCodeType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("channel");
        elemField.setXmlName(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "Channel"));
        elemField.setXmlType(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "ChannelType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("value");
        elemField.setXmlName(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "Value"));
        elemField.setXmlType(new javax.xml.namespace.QName("urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2", "ValueType"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType, java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
        return new org.apache.axis.encoding.ser.BeanSerializer(_javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType, java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
        return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType, _xmlType, typeDesc);
    }
}
