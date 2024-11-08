package net.myphpshop.www.myPHPShopAdmin;

public class Function implements java.io.Serializable {

    private java.lang.Object functionName;

    private boolean read;

    private boolean write;

    public Function() {
    }

    public Function(java.lang.Object functionName, boolean read, boolean write) {
        this.functionName = functionName;
        this.read = read;
        this.write = write;
    }

    /**
     * Gets the functionName value for this Function.
     * 
     * @return functionName
     */
    public java.lang.Object getFunctionName() {
        return functionName;
    }

    /**
     * Sets the functionName value for this Function.
     * 
     * @param functionName
     */
    public void setFunctionName(java.lang.Object functionName) {
        this.functionName = functionName;
    }

    /**
     * Gets the read value for this Function.
     * 
     * @return read
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets the read value for this Function.
     * 
     * @param read
     */
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Gets the write value for this Function.
     * 
     * @return write
     */
    public boolean isWrite() {
        return write;
    }

    /**
     * Sets the write value for this Function.
     * 
     * @param write
     */
    public void setWrite(boolean write) {
        this.write = write;
    }

    private java.lang.Object __equalsCalc = null;

    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Function)) return false;
        Function other = (Function) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && ((this.functionName == null && other.getFunctionName() == null) || (this.functionName != null && this.functionName.equals(other.getFunctionName()))) && this.read == other.isRead() && this.write == other.isWrite();
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
        if (getFunctionName() != null) {
            _hashCode += getFunctionName().hashCode();
        }
        _hashCode += (isRead() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isWrite() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(Function.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.myphpshop.net/myPHPShopAdmin", ">Function"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("read");
        attrField.setXmlName(new javax.xml.namespace.QName("", "read"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("write");
        attrField.setXmlName(new javax.xml.namespace.QName("", "write"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        typeDesc.addFieldDesc(attrField);
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("functionName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.myphpshop.net/myPHPShopAdmin", "FunctionName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "anyType"));
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
