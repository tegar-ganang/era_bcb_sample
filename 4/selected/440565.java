package net.sf.dozer.util.mapping.fieldmap;

import java.lang.reflect.Method;
import net.sf.dozer.util.mapping.classmap.ClassMap;
import net.sf.dozer.util.mapping.propertydescriptor.DozerPropertyDescriptorIF;
import net.sf.dozer.util.mapping.propertydescriptor.GetterSetterPropertyDescriptor;
import net.sf.dozer.util.mapping.propertydescriptor.PropertyDescriptorFactory;
import net.sf.dozer.util.mapping.util.MapperConstants;
import net.sf.dozer.util.mapping.util.MappingUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal class that represents a field mapping definition. Holds all of the information about a single field mapping
 * definition. Only intended for internal use.
 * 
 * @author garsombke.franz
 * @author sullins.ben
 * @author tierney.matt
 * 
 */
public abstract class FieldMap implements Cloneable {

    private static final Log log = LogFactory.getLog(FieldMap.class);

    private ClassMap classMap;

    private DozerField srcField;

    private DozerField destField;

    private HintContainer srcHintContainer;

    private HintContainer destHintContainer;

    private HintContainer srcDeepIndexHintContainer;

    private HintContainer destDeepIndexHintContainer;

    private String type;

    private boolean copyByReference;

    private boolean copyByReferenceOveridden;

    private String mapId;

    private String customConverter;

    private String relationshipType;

    public FieldMap(ClassMap classMap) {
        this.classMap = classMap;
    }

    public ClassMap getClassMap() {
        return classMap;
    }

    public void setClassMap(ClassMap classMap) {
        this.classMap = classMap;
    }

    public Object getSrcFieldValue(Object runtimeSrcObj) {
        return getSrcPropertyDescriptor(runtimeSrcObj.getClass()).getPropertyValue(runtimeSrcObj);
    }

    public void writeDestValue(Object runtimeDestObj, Object destFieldValue) {
        if (log.isDebugEnabled()) {
            log.debug("Getting ready to invoke write method on the destination object.  Dest Obj: " + MappingUtils.getClassNameWithoutPackage(runtimeDestObj.getClass()) + ", Dest value: " + destFieldValue);
        }
        DozerPropertyDescriptorIF propDescriptor = getDestPropertyDescriptor(runtimeDestObj.getClass());
        propDescriptor.setPropertyValue(runtimeDestObj, destFieldValue, this);
    }

    public Class getDestHintType(Class runtimeSrcClass) {
        if (getDestHintContainer() != null) {
            if (getSrcHintContainer() != null) {
                return getDestHintContainer().getHint(runtimeSrcClass, getSrcHintContainer().getHints());
            } else {
                return getDestHintContainer().getHint();
            }
        } else {
            return runtimeSrcClass;
        }
    }

    public Class getDestFieldType(Class runtimeDestClass) {
        Class result = null;
        if (isDestFieldIndexed()) {
            result = destHintContainer != null ? destHintContainer.getHint() : null;
        }
        if (result == null) {
            result = getDestPropertyDescriptor(runtimeDestClass).getPropertyType();
        }
        return result;
    }

    public Class getSrcFieldType(Class runtimeSrcClass) {
        return getSrcPropertyDescriptor(runtimeSrcClass).getPropertyType();
    }

    /**
   * @deprecated As of 3.2 release
   */
    public Method getDestFieldWriteMethod(Class runtimeDestClass) {
        DozerPropertyDescriptorIF dpd = getDestPropertyDescriptor(runtimeDestClass);
        Method result = null;
        try {
            result = ((GetterSetterPropertyDescriptor) dpd).getWriteMethod();
        } catch (Exception e) {
            MappingUtils.throwMappingException(e);
        }
        return result;
    }

    public Object getDestValue(Object runtimeDestObj) {
        return getDestPropertyDescriptor(runtimeDestObj.getClass()).getPropertyValue(runtimeDestObj);
    }

    public HintContainer getDestHintContainer() {
        return destHintContainer;
    }

    public void setDestHintContainer(HintContainer destHint) {
        this.destHintContainer = destHint;
    }

    public HintContainer getSrcHintContainer() {
        return srcHintContainer;
    }

    public void setSrcHintContainer(HintContainer sourceHint) {
        this.srcHintContainer = sourceHint;
    }

    public String getSrcFieldMapGetMethod() {
        return !MappingUtils.isBlankOrNull(srcField.getMapGetMethod()) ? srcField.getMapGetMethod() : classMap.getSrcClassMapGetMethod();
    }

    public String getSrcFieldMapSetMethod() {
        return !MappingUtils.isBlankOrNull(srcField.getMapSetMethod()) ? srcField.getMapSetMethod() : classMap.getSrcClassMapSetMethod();
    }

    public String getDestFieldMapGetMethod() {
        return !MappingUtils.isBlankOrNull(destField.getMapGetMethod()) ? destField.getMapGetMethod() : classMap.getDestClassMapGetMethod();
    }

    public String getDestFieldMapSetMethod() {
        return !MappingUtils.isBlankOrNull(destField.getMapSetMethod()) ? destField.getMapSetMethod() : classMap.getDestClassMapSetMethod();
    }

    public String getSrcFieldName() {
        return srcField.getName();
    }

    public String getDestFieldName() {
        return destField.getName();
    }

    public String getDestFieldType() {
        return destField.getType();
    }

    public String getSrcFieldType() {
        return srcField.getType();
    }

    public String getSrcFieldDateFormat() {
        return getDateFormat();
    }

    public String getDestFieldDateFormat() {
        return getDateFormat();
    }

    public String getDateFormat() {
        if (!MappingUtils.isBlankOrNull(destField.getDateFormat())) {
            return destField.getDateFormat();
        } else if (!MappingUtils.isBlankOrNull(srcField.getDateFormat())) {
            return srcField.getDateFormat();
        } else {
            return classMap.getDateFormat();
        }
    }

    public String getDestFieldCreateMethod() {
        return destField.getCreateMethod();
    }

    public String getSrcFieldCreateMethod() {
        return srcField.getCreateMethod();
    }

    public boolean isDestFieldIndexed() {
        return destField.isIndexed();
    }

    public boolean isSrcFieldIndexed() {
        return srcField.isIndexed();
    }

    public int getSrcFieldIndex() {
        return srcField.getIndex();
    }

    public int getDestFieldIndex() {
        return destField.getIndex();
    }

    public String getSrcFieldTheGetMethod() {
        return srcField.getTheGetMethod();
    }

    public String getDestFieldTheGetMethod() {
        return destField.getTheGetMethod();
    }

    public String getSrcFieldTheSetMethod() {
        return srcField.getTheSetMethod();
    }

    public String getDestFieldTheSetMethod() {
        return destField.getTheSetMethod();
    }

    public String getSrcFieldKey() {
        return srcField.getKey();
    }

    public String getDestFieldKey() {
        return destField.getKey();
    }

    public boolean isDestFieldAccessible() {
        return destField.isAccessible();
    }

    public boolean isSrcFieldAccessible() {
        return srcField.isAccessible();
    }

    public void setSrcField(DozerField sourceField) {
        this.srcField = sourceField;
    }

    public void setDestField(DozerField destField) {
        this.destField = destField;
    }

    public HintContainer getDestDeepIndexHintContainer() {
        return destDeepIndexHintContainer;
    }

    public void setDestDeepIndexHintContainer(HintContainer destDeepIndexHintHint) {
        this.destDeepIndexHintContainer = destDeepIndexHintHint;
    }

    public HintContainer getSrcDeepIndexHintContainer() {
        return srcDeepIndexHintContainer;
    }

    public void setSrcDeepIndexHintContainer(HintContainer srcDeepIndexHint) {
        this.srcDeepIndexHintContainer = srcDeepIndexHint;
    }

    public Object clone() {
        Object result = null;
        try {
            result = super.clone();
        } catch (CloneNotSupportedException e) {
            MappingUtils.throwMappingException(e);
        }
        return result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCopyByReference() {
        return copyByReference;
    }

    public void setCopyByReference(boolean copyByReference) {
        this.copyByReference = copyByReference;
        this.copyByReferenceOveridden = true;
    }

    /**
   * Return true if is self referencing. Is considered self referencing where no other sources are specified, i.e., no
   * source properties or #CDATA in the xml def.
   */
    protected boolean isSrcSelfReferencing() {
        return getSrcFieldName().equals(MapperConstants.SELF_KEYWORD);
    }

    protected boolean isDestSelfReferencing() {
        return getDestFieldName().equals(MapperConstants.SELF_KEYWORD);
    }

    public boolean isCopyByReferenceOveridden() {
        return copyByReferenceOveridden;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public String getCustomConverter() {
        return customConverter;
    }

    public void setCustomConverter(String customConverter) {
        this.customConverter = customConverter;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public void validate() {
        if (srcField == null) {
            MappingUtils.throwMappingException("src field must be specified");
        }
        if (destField == null) {
            MappingUtils.throwMappingException("dest field must be specified");
        }
    }

    protected DozerPropertyDescriptorIF getSrcPropertyDescriptor(Class runtimeSrcClass) {
        return PropertyDescriptorFactory.getPropertyDescriptor(runtimeSrcClass, getSrcFieldTheGetMethod(), getSrcFieldTheSetMethod(), getSrcFieldMapGetMethod(), getSrcFieldMapSetMethod(), isSrcFieldAccessible(), isSrcFieldIndexed(), getSrcFieldIndex(), getSrcFieldName(), getSrcFieldKey(), isSrcSelfReferencing(), getDestFieldName(), getSrcDeepIndexHintContainer(), getDestDeepIndexHintContainer());
    }

    protected DozerPropertyDescriptorIF getDestPropertyDescriptor(Class runtimeDestClass) {
        return PropertyDescriptorFactory.getPropertyDescriptor(runtimeDestClass, getDestFieldTheGetMethod(), getDestFieldTheSetMethod(), getDestFieldMapGetMethod(), getDestFieldMapSetMethod(), isDestFieldAccessible(), isDestFieldIndexed(), getDestFieldIndex(), getDestFieldName(), getDestFieldKey(), isDestSelfReferencing(), getSrcFieldName(), getSrcDeepIndexHintContainer(), getDestDeepIndexHintContainer());
    }

    protected DozerField getSrcField() {
        return srcField;
    }

    protected DozerField getDestField() {
        return destField;
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("source field", srcField).append("destination field", destField).append("type", type).append("customConverter", customConverter).append("relationshipType", relationshipType).append("mapId", mapId).append("copyByReference", copyByReference).append("copyByReferenceOveridden", copyByReferenceOveridden).append("srcTypeHint", srcHintContainer).append("destTypeHint", destHintContainer).toString();
    }
}
