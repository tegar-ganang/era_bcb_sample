package net.sourceforge.freejava.potato.adapter.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import net.sourceforge.freejava.potato.IPotatoProperty;

public class PotatoPropertyDescriptor extends PropertyDescriptor {

    public PotatoPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod, IPotatoProperty potatoProperty) throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);
        FeatureDescriptorUtil.initFeatureDescriptorFromPotatoElement(this, potatoProperty);
    }
}
