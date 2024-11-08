package org.colllibui.beanmeta;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.colllib.datastruct.AutoInitMap;
import org.colllib.factories.Factory;
import org.colllib.proxies.MulticallProxy;
import org.colllib.util.CompareUtil;
import org.colllibui.swing.LongTextField;
import org.colllibui.swing.NullDoubleTextField;
import org.colllibui.swing.NullLongTextField;
import org.colllibui.swing.SliderFieldCombo;
import org.jdesktop.swingx.JXDatePicker;

/**
 * This file is part of CollLibUI released under the terms of the LGPL V3.0.
 * See the file licenses/lgpl-3.0.txt for details.
 *
 * @author mjackisch
 */
public class BeanEdit {

    private BeanEdit() {
    }

    private static class Accessor {

        private Object bean;

        private Method readMethod;

        private Method writeMethod;

        private String label;

        private PropertyType type;

        private BeanProperty propertyAnnotation;

        private String propertyName;

        private EditComponent editComponent;

        private List<PropertyValidator> validators;

        private String oldErrorMessage;

        public Accessor(Object bean, Method readMethod, Method writeMethod, String label, PropertyType type, BeanProperty propertyAnnotation, String propertyName, EditComponent editComponent, List<PropertyValidator> validators) {
            this.bean = bean;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.label = label;
            this.type = type;
            this.propertyAnnotation = propertyAnnotation;
            this.propertyName = propertyName;
            this.editComponent = editComponent;
            this.validators = validators;
        }

        @Override
        public String toString() {
            return "Accessor [bean=" + bean + ", readMethod=" + readMethod + ", writeMethod=" + writeMethod + ", label=" + label + ", type=" + type + ", propertyAnnotation=" + propertyAnnotation + "]";
        }

        public JLabel makeLabel() {
            return new JLabel(label);
        }

        public JComponent makeComponent() {
            if (propertyAnnotation.customEditor().length > 0) {
                return makeCustomEditor();
            } else {
                switch(type) {
                    case STRING:
                        return makeTextField(false);
                    case PASSWORD:
                        return makeTextField(true);
                    case BOOLEAN:
                        return makeCheckBox();
                    case INTEGER:
                        return makeIntField();
                    case INTEGER_NULLABLE:
                        return makeNullableIntField();
                    case DOUBLE_NULLABLE:
                        return makeNullableDoubleField();
                    case DATE:
                        return makeDateField();
                    case SLIDER:
                        return makeSlider();
                    case CUSTOM:
                        throw new RuntimeException("PropertyType.CUSTOM without customEditor set on " + bean.getClass() + "." + readMethod);
                    default:
                        return new JLabel("I am the component");
                }
            }
        }

        private JComponent makeCustomEditor() {
            try {
                CustomEditor customEditor = propertyAnnotation.customEditor()[0];
                Class<?> cl = Class.forName(customEditor.classname());
                CustomEditorComponent cec = (CustomEditorComponent) cl.newInstance();
                return cec.initEditor(bean, propertyName, customEditor.optionClass(), customEditor.optionProperty());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private JTextField makeTextField(boolean pass) {
            final JTextField res = pass ? new JPasswordField(propertyAnnotation.textSize()) : new JTextField(propertyAnnotation.textSize());
            try {
                res.setText((String) readMethod.invoke(bean, new Object[0]));
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
            res.getDocument().addDocumentListener(new DocumentListener() {

                private void updated() {
                    try {
                        Object oldValue = readMethod.invoke(bean, new Object[0]);
                        writeMethod.invoke(bean, res.getText());
                        editComponent.fireEditorPropertyChange(propertyName, oldValue, res.getText());
                        performValidation(res.getText());
                    } catch (IllegalArgumentException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    } catch (InvocationTargetException e1) {
                        throw new RuntimeException(e1);
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updated();
                }
            });
            return res;
        }

        private JComponent makeIntField() {
            try {
                long value = ((Number) readMethod.invoke(bean, new Object[0])).longValue();
                final LongTextField res = new LongTextField(value, propertyAnnotation.textSize(), 10, propertyAnnotation.intMin(), propertyAnnotation.intMax());
                res.getDocument().addDocumentListener(new DocumentListener() {

                    private void docChanged() {
                        try {
                            Object oldValue = readMethod.invoke(bean, new Object[0]);
                            Object v = new Long(res.getValue());
                            Class<?> cl = writeMethod.getParameterTypes()[0];
                            if (cl.equals(Integer.class) || cl.equals(Integer.TYPE)) v = new Integer((int) res.getValue());
                            writeMethod.invoke(bean, v);
                            editComponent.fireEditorPropertyChange(propertyName, oldValue, v);
                            performValidation(v);
                        } catch (IllegalArgumentException e1) {
                            throw new RuntimeException(e1);
                        } catch (IllegalAccessException e1) {
                            throw new RuntimeException(e1);
                        } catch (InvocationTargetException e1) {
                            throw new RuntimeException(e1);
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        docChanged();
                    }
                });
                return res;
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
        }

        private JComponent makeNullableIntField() {
            try {
                Number num = (Number) readMethod.invoke(bean, new Object[0]);
                Long value = num != null ? num.longValue() : null;
                final NullLongTextField res = new NullLongTextField(value, propertyAnnotation.textSize(), propertyAnnotation.intMin(), propertyAnnotation.intMax());
                res.getDocument().addDocumentListener(new DocumentListener() {

                    private void docChanged() {
                        try {
                            Object oldValue = readMethod.invoke(bean, new Object[0]);
                            Object v = res.getValue();
                            Class<?> cl = writeMethod.getParameterTypes()[0];
                            if (v != null && (cl.equals(Integer.class) || cl.equals(Integer.TYPE))) v = new Integer(((Long) res.getValue()).intValue());
                            if (v != null || (!cl.equals(Integer.TYPE) && !cl.equals(Long.TYPE))) writeMethod.invoke(bean, v);
                            editComponent.fireEditorPropertyChange(propertyName, oldValue, v);
                            performValidation(v);
                        } catch (IllegalArgumentException e1) {
                            throw new RuntimeException(e1);
                        } catch (IllegalAccessException e1) {
                            throw new RuntimeException(e1);
                        } catch (InvocationTargetException e1) {
                            throw new RuntimeException(e1);
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        docChanged();
                    }
                });
                return res;
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
        }

        private JComponent makeNullableDoubleField() {
            try {
                Number num = (Number) readMethod.invoke(bean, new Object[0]);
                Double value = num != null ? num.doubleValue() : null;
                final NullDoubleTextField res = new NullDoubleTextField(value, propertyAnnotation.textSize(), propertyAnnotation.intMin(), propertyAnnotation.intMax(), propertyAnnotation.pattern());
                res.getDocument().addDocumentListener(new DocumentListener() {

                    private void docChanged() {
                        try {
                            Object oldValue = readMethod.invoke(bean, new Object[0]);
                            Object v = res.getValue();
                            Class<?> cl = writeMethod.getParameterTypes()[0];
                            if (v != null && (cl.equals(Integer.class) || cl.equals(Integer.TYPE))) v = new Double(((Double) res.getValue()).doubleValue());
                            if (v != null || (!cl.equals(Integer.TYPE) && !cl.equals(Double.TYPE))) writeMethod.invoke(bean, v);
                            editComponent.fireEditorPropertyChange(propertyName, oldValue, v);
                            performValidation(v);
                        } catch (IllegalArgumentException e1) {
                            throw new RuntimeException(e1);
                        } catch (IllegalAccessException e1) {
                            throw new RuntimeException(e1);
                        } catch (InvocationTargetException e1) {
                            throw new RuntimeException(e1);
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        docChanged();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        docChanged();
                    }
                });
                return res;
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
        }

        private JXDatePicker makeDateField() {
            final JXDatePicker res = new JXDatePicker();
            try {
                res.setDate((Date) readMethod.invoke(bean, new Object[0]));
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
            res.getEditor().getDocument().addDocumentListener(new DocumentListener() {

                private void updated() {
                    try {
                        Object oldValue = readMethod.invoke(bean, new Object[0]);
                        writeMethod.invoke(bean, res.getDate());
                        editComponent.fireEditorPropertyChange(propertyName, oldValue, res.getDate());
                        performValidation(res.getDate());
                    } catch (IllegalArgumentException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    } catch (InvocationTargetException e1) {
                        throw new RuntimeException(e1);
                    }
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updated();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updated();
                }
            });
            return res;
        }

        private SliderFieldCombo makeSlider() {
            final SliderFieldCombo res = new SliderFieldCombo(JSlider.HORIZONTAL, propertyAnnotation.intMin(), propertyAnnotation.intMax(), propertyAnnotation.intMin());
            try {
                res.setValue(((Number) readMethod.invoke(bean, new Object[0])).intValue());
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
            res.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent ev) {
                    try {
                        Object oldValue = readMethod.invoke(bean, new Object[0]);
                        Object value = new Integer(res.getValue());
                        if (writeMethod.getParameterTypes()[0].equals(Long.class) || writeMethod.getParameterTypes()[0].equals(Long.TYPE)) value = new Long(res.getValue());
                        writeMethod.invoke(bean, value);
                        editComponent.fireEditorPropertyChange(propertyName, oldValue, value);
                        performValidation(value);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return res;
        }

        private JCheckBox makeCheckBox() {
            final JCheckBox res = new JCheckBox();
            try {
                Boolean selected = (Boolean) readMethod.invoke(bean, new Object[0]);
                res.setSelected(selected != null ? selected : Boolean.FALSE);
            } catch (IllegalArgumentException e1) {
                throw new RuntimeException(e1);
            } catch (IllegalAccessException e1) {
                throw new RuntimeException(e1);
            } catch (InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
            res.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent ev) {
                    try {
                        Object oldValue = readMethod.invoke(bean, new Object[0]);
                        writeMethod.invoke(bean, res.isSelected());
                        editComponent.fireEditorPropertyChange(propertyName, oldValue, res.isSelected());
                        performValidation(res.isSelected());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return res;
        }

        private void performValidation(Object newValue) {
            if (validators != null) {
                String error = null;
                for (PropertyValidator v : validators) {
                    error = v.getError(bean, propertyName, newValue);
                    if (error != null) break;
                }
                if (!CompareUtil.nullSafeEquals(oldErrorMessage, error)) {
                    editComponent.fireEditorPropertyChange("propertyValidation[" + propertyName + "]", oldErrorMessage, error);
                    oldErrorMessage = error;
                }
            }
        }
    }

    private static ArrayList<Accessor> makeBeanAccessors(Object bean, EditComponent comp, AutoInitMap<String, List<PropertyValidator>> valis) {
        try {
            if (bean == null) throw new IllegalArgumentException("null argument");
            ArrayList<Accessor> alist = new ArrayList<Accessor>();
            HashSet<Integer> indicesUsed = new HashSet<Integer>();
            Class<?> cl = bean.getClass();
            BeanInfo bi = Introspector.getBeanInfo(cl);
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    BeanProperty bprop = readMethod.getAnnotation(BeanProperty.class);
                    if (bprop != null) {
                        PropertyType t = bprop.type();
                        if (t == PropertyType.AUTO) {
                            Class<?> retType = readMethod.getReturnType();
                            for (PropertyType pt : PropertyType.values()) if (pt.autoClasses.contains(retType)) t = pt;
                            if (t == PropertyType.AUTO) throw new IllegalArgumentException("Cannot determine auto-mapping for method " + cl.getName() + "." + readMethod.getName() + " with return type " + retType.getCanonicalName());
                        }
                        String label = bprop.label();
                        if (label.length() == 0) {
                            int o = readMethod.getName().startsWith("is") ? 2 : 3;
                            label = readMethod.getName().substring(o);
                        }
                        List<PropertyValidator> validators = new ArrayList<PropertyValidator>();
                        validators.addAll(valis.get(pd.getName()));
                        for (Annotation anno : readMethod.getAnnotations()) if (anno.annotationType().equals(Validator.class)) {
                            try {
                                validators.add((PropertyValidator) Class.forName(((Validator) anno).className()).newInstance());
                            } catch (InstantiationException e) {
                                throw new RuntimeException(e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Accessor a = new Accessor(bean, readMethod, pd.getWriteMethod(), label, t, bprop, pd.getName(), comp, validators);
                        if (a.propertyAnnotation.index() < 0) throw new IllegalArgumentException("Illegal index " + a.propertyAnnotation.index() + " in @BeanProperty annotations of class " + cl);
                        if (!indicesUsed.add(a.propertyAnnotation.index())) throw new IllegalArgumentException("Duplicate index in @BeanProperty annotations of class " + cl);
                        alist.add(a);
                    }
                }
            }
            Collections.sort(alist, new Comparator<Accessor>() {

                @Override
                public int compare(Accessor o1, Accessor o2) {
                    return o1.propertyAnnotation.index() - o2.propertyAnnotation.index();
                }
            });
            return alist;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    public static EditComponent getEditComponent(Object bean, Map<String, List<PropertyValidator>> validators) {
        AutoInitMap<String, List<PropertyValidator>> valis = new AutoInitMap<String, List<PropertyValidator>>(validators == null ? new HashMap<String, List<PropertyValidator>>() : validators, new Factory<List<PropertyValidator>>() {

            @Override
            public List<PropertyValidator> create() {
                return new ArrayList<PropertyValidator>();
            }
        });
        EditComponent p = new EditComponent(bean, new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints(0, 0, GridBagConstraints.RELATIVE, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 10, 0);
        GridBagConstraints cc = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        GridBagConstraints sc = new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 5);
        int lastAccessorIndex = -1;
        for (Accessor a : makeBeanAccessors(bean, p, valis)) {
            if (lastAccessorIndex > -1 && a.propertyAnnotation.index() > lastAccessorIndex + 1) {
                for (int i = lastAccessorIndex + 1; i < a.propertyAnnotation.index(); i++) {
                    sc.gridy = i;
                    p.add(new JSeparator(JSeparator.HORIZONTAL), sc);
                }
            }
            lc.gridy = a.propertyAnnotation.index();
            cc.gridy = a.propertyAnnotation.index();
            p.add(a.makeLabel(), lc);
            p.add(a.makeComponent(), cc);
            lastAccessorIndex = a.propertyAnnotation.index();
        }
        p.validate();
        return p;
    }

    public static EditComponent getEditComponent(Object bean) {
        return getEditComponent(bean, null);
    }

    public static class EditComponent extends JPanel {

        private static final long serialVersionUID = 390791828935754968L;

        private Object bean;

        private MulticallProxy<PropertyChangeListener> changeListeners;

        private EditComponent(Object bean, LayoutManager layout) {
            super(layout);
            this.bean = bean;
            changeListeners = new MulticallProxy<PropertyChangeListener>(PropertyChangeListener.class);
        }

        public void addEditorPropertyChangeListener(PropertyChangeListener l) {
            changeListeners.add(l);
        }

        public void removeEditorPropertyChangeListener(PropertyChangeListener l) {
            changeListeners.remove(l);
        }

        private void fireEditorPropertyChange(String propertyName, Object oldValue, Object newValue) {
            PropertyChangeEvent e = new PropertyChangeEvent(bean, propertyName, oldValue, newValue);
            changeListeners.getProxyInterface().propertyChange(e);
        }
    }
}
