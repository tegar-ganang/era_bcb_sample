package org.odiem.sdk.mappers;

import java.lang.reflect.Method;
import org.odiem.sdk.annotations.Attribute;
import org.odiem.sdk.exceptions.OdmException;

public class AttributeMapper extends AbstractMapper {

    private Attribute attribute;

    public Attribute getAttribute() {
        return attribute;
    }

    public AttributeMapper(Attribute attribute, Method readMethod, Method writeMethod) throws OdmException {
        super(readMethod, writeMethod);
        this.attribute = attribute;
    }
}
