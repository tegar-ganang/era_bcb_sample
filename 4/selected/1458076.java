package org.odiem.sdk.mappers;

import java.lang.reflect.Method;
import org.odiem.sdk.OdmPojo;
import org.odiem.sdk.annotations.Child;
import org.odiem.sdk.exceptions.OdmException;

public final class ChildMapper extends AbstractMapper {

    private Child childAnnotation;

    private OdmPojo<?> childOdmPojo;

    public Child getChildAnnotation() {
        return childAnnotation;
    }

    public OdmPojo<?> getChildOdmPojo() {
        return childOdmPojo;
    }

    public ChildMapper(Child child, Method readMethod, Method writeMethod) throws OdmException {
        super(readMethod, writeMethod, false);
        this.childAnnotation = child;
        this.childOdmPojo = OdmPojo.getInstance(getCoreClass());
    }
}
