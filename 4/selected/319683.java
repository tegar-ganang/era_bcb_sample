package org.jrichclient.richdock.utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.List;
import org.jrichclient.richdock.Dockable;
import org.jrichclient.richdock.DockingPort;

public class PropertyDescriptorFactory {

    public static final boolean BOUND = true;

    public static final boolean NOT_BOUND = false;

    public static final boolean CONSTRAINED = true;

    public static final boolean NOT_CONSTRAINED = false;

    public static final boolean TRANSIENT = true;

    public static final boolean NOT_TRANSIENT = false;

    public static PropertyDescriptor createPropertyDescriptor(Class<?> beanClass, String propertyName, String readMethodName, String writeMethodName, boolean bound, boolean constrained, boolean isTransient) {
        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, beanClass, readMethodName, writeMethodName);
            descriptor.setBound(bound);
            descriptor.setConstrained(constrained);
            if (isTransient) descriptor.setValue("transient", Boolean.TRUE);
            return descriptor;
        } catch (IntrospectionException ex) {
            throw new RuntimeException("Error creating PropertyDescriptor for " + beanClass.getCanonicalName() + "." + propertyName, ex);
        }
    }

    public static void addPropertyChangeBroadcasterPropertyDescriptors(List<PropertyDescriptor> descriptorList, Class<?> beanClass) {
        descriptorList.add(createPropertyDescriptor(beanClass, "propertyChangeListeners", "getPropertyChangeListeners", null, NOT_BOUND, NOT_CONSTRAINED, TRANSIENT));
    }

    public static void addDockablePropertyDescriptors(List<PropertyDescriptor> descriptorList, Class<?> beanClass) {
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_TITLE, "getTitle", "setTitle", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_ICON_FILE, "getIconFile", "setIconFile", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_TOOL_TIP_TEXT, "getToolTipText", "setToolTipText", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_POPUP_MENU, "getPopupMenu", "setPopupMenu", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_DRAGABLE, "isDragable", "setDragable", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_FLOATABLE, "isFloatable", "setFloatable", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_DOCKING_PORT, "getDockingPort", "setDockingPort", BOUND, NOT_CONSTRAINED, TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, Dockable.PROPERTYNAME_DISPOSED, "isDisposed", null, BOUND, NOT_CONSTRAINED, TRANSIENT));
        addPropertyChangeBroadcasterPropertyDescriptors(descriptorList, beanClass);
    }

    public static void addDockingPortPropertyDescriptors(List<PropertyDescriptor> descriptorList, Class<?> beanClass) {
        descriptorList.add(createPropertyDescriptor(beanClass, DockingPort.PROPERTYNAME_DOCKABLE_COUNT, "getDockableCount", null, BOUND, NOT_CONSTRAINED, TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, DockingPort.PROPERTYNAME_DROPABLE, "isDropable", "setDropable", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        descriptorList.add(createPropertyDescriptor(beanClass, DockingPort.PROPERTYNAME_DISPOSE_ON_EMPTY, "getDisposeOnEmpty", "setDisposeOnEmpty", BOUND, NOT_CONSTRAINED, NOT_TRANSIENT));
        addDockablePropertyDescriptors(descriptorList, beanClass);
    }

    public static PropertyDescriptor[] createPropertyDescriptorArray(List<PropertyDescriptor> descriptorList) {
        return (PropertyDescriptor[]) descriptorList.toArray(new PropertyDescriptor[descriptorList.size()]);
    }
}
