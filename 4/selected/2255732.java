package net.sf.jaer.eventprocessing;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.beans.*;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sf.jaer.chip.Chip2D;
import net.sf.jaer.util.EngineeringFormat;

/**
 * A panel for a filter that has Integer/Float/Boolean/String/enum getter/setter methods (bound properties).
These methods are introspected and a set of controls are built for them. Enclosed filters and
filter chains have panels built for them that are enlosed inside the filter panel, hierarchically.
 * <ul>
 * <li>Numerical properties (ints, floats, but not currently doubles) construct a JTextBox control that also allows changes from mouse wheel or arrow keys.
 * <li> boolean properties construct a JCheckBox control.
 * <li> String properties construct a JTextField control.
 * <li> enum properties construct a JComboBox control, which all the possible enum constant values.
 * </ul>
 * <p>
 * If a filter wants to automatically have the GUI controls reflect what the property state is, then it should 
 * fire PropertyChangeEvent when the property changes. For example, an {@link EventFilter} can implement a setter like this:
 * <pre>
public void setMapEventsToLearnedTopologyEnabled(boolean mapEventsToLearnedTopologyEnabled) {
support.firePropertyChange("mapEventsToLearnedTopologyEnabled", this.mapEventsToLearnedTopologyEnabled, mapEventsToLearnedTopologyEnabled); // property, old value, new value
this.mapEventsToLearnedTopologyEnabled = mapEventsToLearnedTopologyEnabled;
getPrefs().putBoolean("TopologyTracker.mapEventsToLearnedTopologyEnabled", mapEventsToLearnedTopologyEnabled);
}
</pre>
 * Here, <code>support</code> is a protected field of EventFilter. The change event comes here to FilterPanel and the appropriate automatically 
 * generated control is modified.
 * <p>
 * Note that calling firePropertyChange as shown above will inform listeners <em>before</em> the property has actually been
 * changed (this.dt has not been set yet).
 * <p>
 * A tooltip for the property can be installed using the EventFilter setPropertyTooltip method, for example
 * <pre>
 *         setPropertyTooltip("sizeClassificationEnabled", "Enables coloring cluster by size threshold");
 * </pre>
 * will install a tip for the property sizeClassificationEnabled.
 * <p>
 * <strong>Slider controls.</strong>
 * 
 * If you want a slider for an int or float property, then create getMin and getMax methods for the property, e.g., for
 * the property <code>dt</code>:
 * <pre>
public int getDt() {
return this.dt;
}

public void setDt(final int dt) {
getPrefs().putInt("BackgroundActivityFilter.dt",dt);
support.firePropertyChange("dt",this.dt,dt);
this.dt = dt;
}

public int getMinDt(){
return 10;
}

public int getMaxDt(){
return 100000;
}
</pre>
 * <strong>Button control</strong>
 * <p>
 * To add a button control to a panel, implement a method starting with "do", e.g.
 * <pre>
 *     public void doSendParameters() {
sendParameters();
}
 * </pre>
 * This method will construct a button with label "SendParameters" which, when pressed, will call the method "doSendParameters".
 * <p>
 * <strong>
 * Grouping parameters.</strong>
 * <p>
 * Properties are normally sorted alphabetically, with button controls at the top. If you want to group parameters, use
 * the built in EventFilter method {@link net.sf.jaer.eventprocessing.EventFilter#addPropertyToGroup}. All properties of a given group are grouped together. Within
 * a group the parameters are sorted alphabetically, and the groups will also be sorted alphabetically and shown before
 * any ungrouped parameters. E.g., to Create groups "Sizing" and "Tracking" and add properties to each, do
 * <pre>
addPropertyToGroup("Sizing", "clusterSize");
addPropertyToGroup("Sizing", "aspectRatio");
addPropertyToGroup("Sizing", "highwayPerspectiveEnabled");
addPropertyToGroup("Tracking", "mixingFactor");
addPropertyToGroup("Tracking", "velocityMixingFactor");
 * </pre>
 * Or, even simpler, if you have already defined tooltips for your properties, then
 * you can use the overloaded
 * {@link net.sf.jaer.eventprocessing.EventFilter#setPropertyTooltip(java.lang.String, java.lang.String, java.lang.String) setPropertyTooltip} of
 * {@link net.sf.jaer.eventprocessing.EventFilter},
 * as shown next. Here two groups "Size" and "Timing" are defined and properties are added to each (or to neither for "multiOriOutputEnabled").
 * <pre>
final String size="Size", tim="Timing";

setPropertyTooltip(disp,"showGlobalEnabled", "shows line of average orientation");
setPropertyTooltip(tim,"minDtThreshold", "Coincidence time, events that pass this coincidence test are considerd for orientation output");
setPropertyTooltip(tim,"dtRejectMultiplier", "reject delta times more than this factor times minDtThreshold to reduce noise");
setPropertyTooltip(tim,"dtRejectThreshold", "reject delta times more than this time in us to reduce effect of very old events");
setPropertyTooltip("multiOriOutputEnabled", "Enables multiple event output for all events that pass test");
</pre>
 *
 *
 * @author  tobi
 * @see net.sf.jaer.eventprocessing.EventFilter#setPropertyTooltip(java.lang.String, java.lang.String)
 *@see net.sf.jaer.eventprocessing.EventFilter#setPropertyTooltip(java.lang.String, java.lang.String, java.lang.String)
 * @see net.sf.jaer.eventprocessing.EventFilter
 */
public class FilterPanel extends javax.swing.JPanel implements PropertyChangeListener {

    private interface HasSetter {

        void set(Object o);
    }

    static final float ALIGNMENT = Component.LEFT_ALIGNMENT;

    private BeanInfo info;

    private PropertyDescriptor[] props;

    private Method[] methods;

    private static Logger log = Logger.getLogger("Filters");

    private EventFilter filter = null;

    final float fontSize = 10f;

    private Border normalBorder, redLineBorder;

    private TitledBorder titledBorder;

    private HashMap<String, HasSetter> setterMap = new HashMap<String, HasSetter>();

    private java.util.ArrayList<JComponent> controls = new ArrayList<JComponent>();

    private HashMap<String, Container> groupContainerMap = new HashMap();

    private JPanel inheritedPanel = null;

    private float DEFAULT_REAL_VALUE = 0.01f;

    /** Creates new form FilterPanel */
    public FilterPanel() {
        initComponents();
    }

    public FilterPanel(EventFilter f) {
        this.setFilter(f);
        initComponents();
        String cn = getFilter().getClass().getName();
        int lastdot = cn.lastIndexOf('.');
        String name = cn.substring(lastdot + 1);
        setName(name);
        titledBorder = new TitledBorder(name);
        titledBorder.getBorderInsets(this).set(1, 1, 1, 1);
        setBorder(titledBorder);
        normalBorder = titledBorder.getBorder();
        redLineBorder = BorderFactory.createLineBorder(Color.red);
        enabledCheckBox.setSelected(getFilter().isFilterEnabled());
        addIntrospectedControls();
        getFilter().getPropertyChangeSupport().addPropertyChangeListener(this);
        ToolTipManager.sharedInstance().setDismissDelay(10000);
        setToolTipText(f.getDescription());
    }

    private void myadd(JComponent comp, String propertyName, boolean inherited) {
        JPanel pan = new JPanel();
        pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
        controls.add(pan);
        if (!getFilter().hasPropertyGroups()) {
            pan.add(comp);
            pan.add(Box.createVerticalStrut(0));
            add(pan);
            controls.add(comp);
            return;
        }
        String groupName = getFilter().getPropertyGroup(propertyName);
        if (groupName != null) {
            Container container = groupContainerMap.get(groupName);
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(comp);
        } else {
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            pan.add(comp);
            pan.add(Box.createVerticalStrut(0));
        }
        add(pan);
        controls.add(comp);
    }

    private void addIntrospectedControls() {
        JPanel control = null;
        EventFilter filter = getFilter();
        try {
            info = Introspector.getBeanInfo(filter.getClass());
            props = info.getPropertyDescriptors();
            methods = filter.getClass().getMethods();
            control = new JPanel();
            int numDoButtons = 0;
            Insets butInsets = new Insets(0, 0, 0, 0);
            for (Method m : methods) {
                if (m.getName().startsWith("do") && m.getParameterTypes().length == 0 && m.getReturnType() == void.class) {
                    numDoButtons++;
                    JButton button = new JButton(m.getName().substring(2));
                    button.setMargin(butInsets);
                    button.setFont(button.getFont().deriveFont(9f));
                    final EventFilter f = filter;
                    final Method meth = m;
                    button.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            try {
                                meth.invoke(f);
                            } catch (IllegalArgumentException ex) {
                                ex.printStackTrace();
                            } catch (InvocationTargetException ex) {
                                ex.printStackTrace();
                            } catch (IllegalAccessException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    addTip(f, button);
                    control.add(button);
                }
            }
            if (control.getComponentCount() > 0) {
                TitledBorder tb = new TitledBorder("Filter Actions");
                tb.getBorderInsets(this).set(1, 1, 1, 1);
                control.setBorder(tb);
                control.setMinimumSize(new Dimension(0, 0));
                add(control);
                controls.add(control);
            }
            if (numDoButtons > 3) {
                control.setLayout(new GridLayout(0, 3, 3, 3));
            }
            for (PropertyDescriptor p : props) {
                Class c = p.getPropertyType();
                if (p.getName().equals("enclosedFilter")) {
                    try {
                        Method r = p.getReadMethod();
                        EventFilter2D enclFilter = (EventFilter2D) (r.invoke(getFilter()));
                        if (enclFilter != null) {
                            FilterPanel enclPanel = new FilterPanel(enclFilter);
                            this.add(enclPanel);
                            controls.add(enclPanel);
                            ((TitledBorder) enclPanel.getBorder()).setTitle("enclosed: " + enclFilter.getClass().getSimpleName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (p.getName().equals("enclosedFilterChain")) {
                    try {
                        Method r = p.getReadMethod();
                        FilterChain chain = (FilterChain) (r.invoke(getFilter()));
                        if (chain != null) {
                            for (EventFilter f : chain) {
                                FilterPanel enclPanel = new FilterPanel(f);
                                this.add(enclPanel);
                                controls.add(enclPanel);
                                ((TitledBorder) enclPanel.getBorder()).setTitle("enclosed: " + f.getClass().getSimpleName());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String name = p.getName();
                    if (control != null) {
                        control.setToolTipText(getFilter().getPropertyTooltip(name));
                    }
                }
            }
            if (getFilter().hasPropertyGroups()) {
                Set<String> groupSet = getFilter().getPropertyGroupSet();
                for (String s : groupSet) {
                    JPanel groupPanel = new JPanel();
                    groupPanel.setName(s);
                    groupPanel.setBorder(new TitledBorder(s));
                    groupPanel.setLayout(new GridLayout(0, 1));
                    groupContainerMap.put(s, groupPanel);
                    add(groupPanel);
                    controls.add(groupPanel);
                }
            }
            for (PropertyDescriptor p : props) {
                try {
                    boolean inherited = false;
                    Class c = p.getPropertyType();
                    String name = p.getName();
                    if (control != null && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        Method m = p.getReadMethod();
                        if (m.getDeclaringClass() != getFilter().getClass()) {
                            inherited = true;
                        }
                    }
                    if (c == Integer.TYPE && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        SliderParams params;
                        if ((params = isSliderType(p, filter)) != null) {
                            control = new IntSliderControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod(), params);
                        } else {
                            control = new IntControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        }
                        myadd(control, name, inherited);
                    } else if (c == Float.TYPE && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        SliderParams params;
                        if ((params = isSliderType(p, filter)) != null) {
                            control = new FloatSliderControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod(), params);
                        } else {
                            control = new FloatControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        }
                        myadd(control, name, inherited);
                    } else if (c == Boolean.TYPE && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        if (p.getName().equals("filterEnabled")) {
                            continue;
                        }
                        if (p.getName().equals("annotationEnabled")) {
                            continue;
                        }
                        if (p.getName().equals("selected")) {
                            continue;
                        }
                        control = new BooleanControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        myadd(control, name, inherited);
                    } else if (c == String.class && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        if (p.getName().equals("filterEnabled")) {
                            continue;
                        }
                        if (p.getName().equals("annotationEnabled")) {
                            continue;
                        }
                        control = new StringControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        myadd(control, name, inherited);
                    } else if (c != null && c.isEnum() && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        control = new EnumControl(c, getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        myadd(control, name, inherited);
                    } else if (c != null && c == Point2D.Float.class && p.getReadMethod() != null && p.getWriteMethod() != null) {
                        control = new Point2DControl(getFilter(), p.getName(), p.getWriteMethod(), p.getReadMethod());
                        myadd(control, name, inherited);
                    } else {
                    }
                    if (control != null) {
                        control.setToolTipText(getFilter().getPropertyTooltip(name));
                    }
                } catch (Exception e) {
                    log.warning(e + " caught on property " + p.getName() + " from EventFilter " + filter);
                }
            }
            groupContainerMap = null;
        } catch (Exception e) {
            log.warning("on adding controls for EventFilter " + filter + " caught " + e);
            e.printStackTrace();
        }
        add(Box.createHorizontalGlue());
        setControlsVisible(false);
    }

    void addTip(EventFilter f, JLabel label) {
        String s = f.getPropertyTooltip(label.getText());
        if (s == null) {
            return;
        }
        label.setToolTipText(s);
        label.setForeground(Color.BLUE);
    }

    void addTip(EventFilter f, JButton b) {
        String s = f.getPropertyTooltip(b.getText());
        if (s == null) {
            return;
        }
        b.setToolTipText(s);
        b.setForeground(Color.BLUE);
    }

    void addTip(EventFilter f, JCheckBox label) {
        String s = f.getPropertyTooltip(label.getText());
        if (s == null) {
            return;
        }
        label.setToolTipText(s);
        label.setForeground(Color.BLUE);
    }

    class EnumControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        boolean initValue = false, nval;

        final JComboBox control;

        public void set(Object o) {
            control.setSelectedItem(o);
        }

        public EnumControl(final Class<? extends Enum> c, final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            final JLabel label = new JLabel(name);
            label.setAlignmentX(ALIGNMENT);
            label.setFont(label.getFont().deriveFont(fontSize));
            addTip(f, label);
            add(label);
            control = new JComboBox(c.getEnumConstants());
            control.setFont(control.getFont().deriveFont(fontSize));
            add(label);
            add(control);
            try {
                Object x = (Object) r.invoke(filter);
                if (x == null) {
                    log.warning("null Object returned from read method " + r);
                    return;
                }
                control.setSelectedItem(x);
            } catch (Exception e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            control.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        w.invoke(filter, control.getSelectedItem());
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            });
        }
    }

    class StringControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        boolean initValue = false, nval;

        final JTextField textField;

        public void set(Object o) {
            if (o instanceof String) {
                String b = (String) o;
                textField.setText(b);
            }
        }

        public StringControl(final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            final JLabel label = new JLabel(name);
            label.setAlignmentX(ALIGNMENT);
            label.setFont(label.getFont().deriveFont(fontSize));
            addTip(f, label);
            add(label);
            textField = new JTextField(name);
            textField.setFont(textField.getFont().deriveFont(fontSize));
            textField.setHorizontalAlignment(SwingConstants.LEADING);
            textField.setColumns(10);
            add(label);
            add(textField);
            try {
                String x = (String) r.invoke(filter);
                if (x == null) {
                    log.warning("null String returned from read method " + r);
                    return;
                }
                textField.setText(x);
                textField.setToolTipText(x);
            } catch (Exception e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            textField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        w.invoke(filter, textField.getText());
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            });
        }
    }

    final float factor = 1.51f, wheelFactor = 1.05f;

    class BooleanControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        boolean initValue = false, nval;

        final JCheckBox checkBox;

        public BooleanControl(final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            checkBox = new JCheckBox(name);
            checkBox.setAlignmentX(ALIGNMENT);
            checkBox.setFont(checkBox.getFont().deriveFont(fontSize));
            checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
            addTip(f, checkBox);
            add(checkBox);
            try {
                Boolean x = (Boolean) r.invoke(filter);
                if (x == null) {
                    log.warning("null Boolean returned from read method " + r);
                    return;
                }
                initValue = x.booleanValue();
                checkBox.setSelected(initValue);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            checkBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        w.invoke(filter, checkBox.isSelected());
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        iae.printStackTrace();
                    }
                }
            });
        }

        public void set(Object o) {
            if (o instanceof Boolean) {
                Boolean b = (Boolean) o;
                checkBox.setSelected(b);
            }
        }
    }

    class IntSliderControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        int initValue = 0, nval;

        JSlider slider;

        JTextField tf;

        public void set(Object o) {
            if (o instanceof Integer) {
                Integer b = (Integer) o;
                slider.setValue(b);
            }
        }

        public IntSliderControl(final EventFilter f, final String name, final Method w, final Method r, SliderParams params) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            final IntControl ic = new IntControl(f, name, w, r);
            add(ic);
            slider = new JSlider(params.minIntValue, params.maxIntValue);
            slider.setMaximumSize(new Dimension(200, 50));
            try {
                Integer x = (Integer) r.invoke(filter);
                if (x == null) {
                    log.warning("null Integer returned from read method " + r);
                    return;
                }
                initValue = x.intValue();
                slider.setValue(initValue);
            } catch (Exception e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            add(slider);
            slider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    try {
                        w.invoke(filter, new Integer(slider.getValue()));
                        ic.set(slider.getValue());
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        iae.printStackTrace();
                    }
                }
            });
        }
    }

    class FloatSliderControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        JSlider slider;

        JTextField tf;

        EngineeringFormat engFmt;

        FloatControl fc;

        boolean dontProcessEvent = false;

        float minValue, maxValue, currentValue;

        public void set(Object o) {
            if (o instanceof Integer) {
                Integer b = (Integer) o;
                slider.setValue(b);
                fc.set(b);
            } else if (o instanceof Float) {
                float f = (Float) o;
                int sv = Math.round((f - minValue) / (maxValue - minValue) * (slider.getMaximum() - slider.getMinimum()));
                slider.setValue(sv);
            }
        }

        public FloatSliderControl(final EventFilter f, final String name, final Method w, final Method r, SliderParams params) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            fc = new FloatControl(f, name, w, r);
            add(fc);
            minValue = params.minFloatValue;
            maxValue = params.maxFloatValue;
            slider = new JSlider();
            slider.setMaximumSize(new Dimension(200, 50));
            engFmt = new EngineeringFormat();
            try {
                Float x = (Float) r.invoke(filter);
                if (x == null) {
                    log.warning("null Float returned from read method " + r);
                    return;
                }
                currentValue = x.floatValue();
                set(new Float(currentValue));
            } catch (Exception e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            add(slider);
            slider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    try {
                        int v = slider.getValue();
                        currentValue = minValue + (maxValue - minValue) * ((float) slider.getValue() / (slider.getMaximum() - slider.getMinimum()));
                        w.invoke(filter, new Float(currentValue));
                        fc.set(new Float(currentValue));
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        iae.printStackTrace();
                    }
                }
            });
        }
    }

    class IntControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        int initValue = 0, nval;

        final JTextField tf;

        public void set(Object o) {
            if (o instanceof Integer) {
                Integer b = (Integer) o;
                tf.setText(b.toString());
            }
        }

        public IntControl(final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            JLabel label = new JLabel(name);
            label.setAlignmentX(ALIGNMENT);
            label.setFont(label.getFont().deriveFont(fontSize));
            addTip(f, label);
            add(label);
            tf = new JTextField("", 8);
            tf.setMaximumSize(new Dimension(100, 50));
            tf.setToolTipText("Integer control: use arrow keys or mouse wheel to change value by factor. Shift constrains to simple inc/dec");
            try {
                Integer x = (Integer) r.invoke(filter);
                if (x == null) {
                    log.warning("null Integer returned from read method " + r);
                    return;
                }
                initValue = x.intValue();
                String s = Integer.toString(x);
                tf.setText(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
            add(tf);
            tf.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        int y = Integer.parseInt(tf.getText());
                        w.invoke(filter, new Integer(y));
                    } catch (NumberFormatException fe) {
                        tf.selectAll();
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        iae.printStackTrace();
                    }
                }
            });
            tf.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyPressed(java.awt.event.KeyEvent evt) {
                    try {
                        Integer x = (Integer) r.invoke(filter);
                        initValue = x.intValue();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    int code = evt.getKeyCode();
                    int mod = evt.getModifiers();
                    boolean shift = evt.isShiftDown();
                    if (!shift) {
                        if (code == KeyEvent.VK_UP) {
                            try {
                                nval = initValue;
                                if (nval == 0) {
                                    nval = 1;
                                } else {
                                    nval = (int) Math.round(initValue * factor);
                                }
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                                fixIntValue(tf, r);
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        } else if (code == KeyEvent.VK_DOWN) {
                            try {
                                nval = initValue;
                                if (nval == 0) {
                                    nval = 0;
                                } else {
                                    nval = (int) Math.round(initValue / factor);
                                }
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                                fixIntValue(tf, r);
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        }
                    } else {
                        if (code == KeyEvent.VK_UP) {
                            try {
                                nval = initValue + 1;
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                                fixIntValue(tf, r);
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        } else if (code == KeyEvent.VK_DOWN) {
                            try {
                                nval = initValue - 1;
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                                fixIntValue(tf, r);
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        }
                    }
                }
            });
            tf.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

                public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                    try {
                        Integer x = (Integer) r.invoke(filter);
                        initValue = x.intValue();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    int code = evt.getWheelRotation();
                    int mod = evt.getModifiers();
                    boolean shift = evt.isShiftDown();
                    if (!shift) {
                        if (code < 0) {
                            try {
                                nval = initValue;
                                if (Math.round(initValue * wheelFactor) == initValue) {
                                    nval++;
                                } else {
                                    nval = (int) Math.round(initValue * wheelFactor);
                                }
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        } else if (code > 0) {
                            try {
                                nval = initValue;
                                if (Math.round(initValue / wheelFactor) == initValue) {
                                    nval--;
                                } else {
                                    nval = (int) Math.round(initValue / wheelFactor);
                                }
                                if (nval < 0) {
                                    nval = 0;
                                }
                                w.invoke(filter, new Integer(nval));
                                tf.setText(new Integer(nval).toString());
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        }
                    }
                }
            });
            tf.addFocusListener(new FocusListener() {

                public void focusGained(FocusEvent e) {
                    tf.setSelectionStart(0);
                    tf.setSelectionEnd(tf.getText().length());
                }

                public void focusLost(FocusEvent e) {
                }
            });
        }
    }

    void fixIntValue(JTextField tf, Method r) {
        try {
            Integer x = (Integer) r.invoke(getFilter());
            String s = Integer.toString(x);
            tf.setText(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class FloatControl extends JPanel implements HasSetter {

        EngineeringFormat engFmt = new EngineeringFormat();

        Method write, read;

        EventFilter filter;

        float initValue = 0, nval;

        final JTextField tf;

        public void set(Object o) {
            if (o instanceof Float) {
                Float b = (Float) o;
                tf.setText(b.toString());
            }
        }

        public FloatControl(final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            JLabel label = new JLabel(name);
            label.setAlignmentX(ALIGNMENT);
            label.setFont(label.getFont().deriveFont(fontSize));
            addTip(f, label);
            add(label);
            tf = new JTextField("", 10);
            tf.setMaximumSize(new Dimension(100, 50));
            tf.setToolTipText("Float control: use arrow keys or mouse wheel to change value by factor. Shift reduces factor.");
            try {
                Float x = (Float) r.invoke(filter);
                if (x == null) {
                    log.warning("null Float returned from read method " + r);
                    return;
                }
                initValue = x.floatValue();
                tf.setText(engFmt.format(initValue));
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                log.warning("cannot access the field named " + name + " is the class or method not public?");
                e.printStackTrace();
            }
            add(tf);
            tf.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        float y = engFmt.parseFloat(tf.getText());
                        w.invoke(filter, new Float(y));
                        Float x = (Float) r.invoke(filter);
                        nval = x.floatValue();
                        tf.setText(engFmt.format(nval));
                    } catch (NumberFormatException fe) {
                        tf.selectAll();
                    } catch (InvocationTargetException ite) {
                        ite.printStackTrace();
                    } catch (IllegalAccessException iae) {
                        iae.printStackTrace();
                    }
                }
            });
            tf.addKeyListener(new java.awt.event.KeyAdapter() {

                {
                }

                public void keyPressed(java.awt.event.KeyEvent evt) {
                    try {
                        Float x = (Float) r.invoke(filter);
                        initValue = x.floatValue();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    int code = evt.getKeyCode();
                    int mod = evt.getModifiers();
                    boolean shift = evt.isShiftDown();
                    float floatFactor = factor;
                    if (shift) {
                        floatFactor = wheelFactor;
                    }
                    if (code == KeyEvent.VK_UP) {
                        try {
                            nval = initValue;
                            if (nval == 0) {
                                nval = DEFAULT_REAL_VALUE;
                            } else {
                                nval = (initValue * floatFactor);
                            }
                            w.invoke(filter, new Float(nval));
                            Float x = (Float) r.invoke(filter);
                            nval = x.floatValue();
                            tf.setText(engFmt.format(nval));
                        } catch (InvocationTargetException ite) {
                            ite.printStackTrace();
                        } catch (IllegalAccessException iae) {
                            iae.printStackTrace();
                        }
                    } else if (code == KeyEvent.VK_DOWN) {
                        try {
                            nval = initValue;
                            if (nval == 0) {
                                nval = DEFAULT_REAL_VALUE;
                            } else {
                                nval = (initValue / floatFactor);
                            }
                            w.invoke(filter, new Float(initValue / floatFactor));
                            Float x = (Float) r.invoke(filter);
                            nval = x.floatValue();
                            tf.setText(engFmt.format(nval));
                        } catch (InvocationTargetException ite) {
                            ite.printStackTrace();
                        } catch (IllegalAccessException iae) {
                            iae.printStackTrace();
                        }
                    }
                }
            });
            tf.addMouseWheelListener(new java.awt.event.MouseWheelListener() {

                public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                    try {
                        Float x = (Float) r.invoke(filter);
                        initValue = x.floatValue();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    int code = evt.getWheelRotation();
                    int mod = evt.getModifiers();
                    boolean shift = evt.isShiftDown();
                    if (!shift) {
                        if (code < 0) {
                            try {
                                nval = initValue;
                                if (nval == 0) {
                                    nval = DEFAULT_REAL_VALUE;
                                } else {
                                    nval = (initValue * wheelFactor);
                                }
                                w.invoke(filter, new Float(nval));
                                Float x = (Float) r.invoke(filter);
                                nval = x.floatValue();
                                tf.setText(engFmt.format(nval));
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        } else if (code > 0) {
                            try {
                                nval = initValue;
                                if (nval == 0) {
                                    nval = DEFAULT_REAL_VALUE;
                                } else {
                                    nval = (initValue / wheelFactor);
                                }
                                w.invoke(filter, new Float(initValue / wheelFactor));
                                Float x = (Float) r.invoke(filter);
                                nval = x.floatValue();
                                tf.setText(engFmt.format(nval));
                            } catch (InvocationTargetException ite) {
                                ite.printStackTrace();
                            } catch (IllegalAccessException iae) {
                                iae.printStackTrace();
                            }
                        }
                    }
                }
            });
            tf.addFocusListener(new FocusListener() {

                public void focusGained(FocusEvent e) {
                    tf.setSelectionStart(0);
                    tf.setSelectionEnd(tf.getText().length());
                }

                public void focusLost(FocusEvent e) {
                }
            });
        }
    }

    private boolean printedSetterWarning = false;

    /** Called when a filter calls firePropertyChange. The PropertyChangeEvent should send the bound property name and the old and new values.
    The GUI control is then updated by this method.
    @param propertyChangeEvent contains the property that has changed, e.g. it would be called from an EventFilter 
     * with 
     * <code>support.firePropertyChange("mapEventsToLearnedTopologyEnabled", mapEventsToLearnedTopologyEnabled, this.mapEventsToLearnedTopologyEnabled);</code>
     */
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getSource() == getFilter()) {
            if (propertyChangeEvent.getPropertyName().equals("selected")) {
                return;
            } else if (propertyChangeEvent.getPropertyName().equals("filterEnabled")) {
                boolean yes = (Boolean) propertyChangeEvent.getNewValue();
                enabledCheckBox.setSelected(yes);
                setBorderActive(yes);
            } else {
                try {
                    HasSetter setter = setterMap.get(propertyChangeEvent.getPropertyName());
                    if (setter == null) {
                        if (!printedSetterWarning) {
                            log.warning("in filter " + getFilter() + " there is no setter for property change from property named " + propertyChangeEvent.getPropertyName());
                            printedSetterWarning = true;
                        }
                    } else {
                        setter.set(propertyChangeEvent.getNewValue());
                    }
                } catch (Exception e) {
                    log.warning(e.toString());
                }
            }
        }
    }

    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();
        jPanel1 = new javax.swing.JPanel();
        enabledCheckBox = new javax.swing.JCheckBox();
        resetButton = new javax.swing.JButton();
        showControlsToggleButton = new javax.swing.JToggleButton();
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        jPanel1.setAlignmentX(1.0F);
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 23));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));
        enabledCheckBox.setFont(new java.awt.Font("Tahoma", 0, 9));
        enabledCheckBox.setToolTipText("Enable or disable the filter");
        enabledCheckBox.setMargin(new java.awt.Insets(1, 1, 1, 1));
        enabledCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enabledCheckBoxActionPerformed(evt);
            }
        });
        jPanel1.add(enabledCheckBox);
        resetButton.setFont(new java.awt.Font("Tahoma", 0, 9));
        resetButton.setText("Reset");
        resetButton.setToolTipText("Resets the filter");
        resetButton.setMargin(new java.awt.Insets(1, 5, 1, 5));
        resetButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });
        jPanel1.add(resetButton);
        showControlsToggleButton.setFont(new java.awt.Font("Tahoma", 0, 9));
        showControlsToggleButton.setText("Controls");
        showControlsToggleButton.setToolTipText("Show filter parameters, hides other filters. Click again to see all filters.");
        showControlsToggleButton.setMargin(new java.awt.Insets(1, 5, 1, 5));
        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${controlsVisible}"), showControlsToggleButton, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        jPanel1.add(showControlsToggleButton);
        add(jPanel1);
        bindingGroup.bind();
    }

    boolean controlsVisible = false;

    public boolean isControlsVisible() {
        return controlsVisible;
    }

    /** Set visibility of individual filter controls; hides other filters.
     * @param visible true to show filter parameter controls, false to hide this filter's controls and to show all filters in chain.
     */
    public void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        getFilter().setSelected(visible);
        setBorderActive(visible);
        for (JComponent p : controls) {
            p.setVisible(visible);
            p.invalidate();
        }
        invalidate();
        Container c = getTopLevelAncestor();
        if (c == null) {
            return;
        }
        if (!getFilter().isEnclosed() && c instanceof Window) {
            if (c instanceof FilterFrame) {
                FilterFrame ff = (FilterFrame) c;
                for (FilterPanel f : ff.filterPanels) {
                    if (f == this) {
                        f.setVisible(true);
                        continue;
                    }
                    f.setVisible(!visible);
                }
            }
            ((Window) c).pack();
        }
        if (c instanceof Window) {
            ((Window) c).pack();
        }
        if (!getFilter().isEnclosed()) {
            if (visible) {
                getFilter().getChip().getPrefs().put(FilterFrame.LAST_FILTER_SELECTED_KEY, getFilter().getClass().toString());
            } else {
                getFilter().getChip().getPrefs().put(FilterFrame.LAST_FILTER_SELECTED_KEY, "");
            }
        }
        showControlsToggleButton.setSelected(visible);
    }

    private void setBorderActive(final boolean yes) {
        if (yes) {
            ((TitledBorder) getBorder()).setTitleColor(SystemColor.textText);
            titledBorder.setBorder(redLineBorder);
        } else {
            ((TitledBorder) getBorder()).setTitleColor(SystemColor.textInactiveText);
            titledBorder.setBorder(normalBorder);
        }
    }

    void toggleControlsVisible() {
        controlsVisible = !controlsVisible;
        setControlsVisible(controlsVisible);
    }

    public EventFilter getFilter() {
        return filter;
    }

    public void setFilter(EventFilter filter) {
        this.filter = filter;
    }

    private void enabledCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        boolean yes = enabledCheckBox.isSelected();
        if (getFilter() != null) {
            getFilter().setFilterEnabled(yes);
        }
        if (yes) {
            ((TitledBorder) getBorder()).setTitleColor(SystemColor.textText);
            titledBorder.setBorder(redLineBorder);
        } else {
            ((TitledBorder) getBorder()).setTitleColor(SystemColor.textInactiveText);
            titledBorder.setBorder(normalBorder);
        }
        repaint();
        getFilter().setSelected(yes);
    }

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (getFilter() != null) {
            getFilter().resetFilter();
        }
        getFilter().setSelected(true);
    }

    private javax.swing.JCheckBox enabledCheckBox;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JButton resetButton;

    private javax.swing.JToggleButton showControlsToggleButton;

    private org.jdesktop.beansbinding.BindingGroup bindingGroup;

    private class SliderParams {

        Class paramClass = null;

        int minIntValue, maxIntValue;

        float minFloatValue, maxFloatValue;

        SliderParams(Class clazz, int minIntValue, int maxIntValue, float minFloatValue, float maxFloatValue) {
            this.minIntValue = minIntValue;
            this.minFloatValue = minFloatValue;
            this.maxIntValue = maxIntValue;
            this.maxFloatValue = maxFloatValue;
        }
    }

    private SliderParams isSliderType(PropertyDescriptor p, net.sf.jaer.eventprocessing.EventFilter filter) throws SecurityException {
        boolean isSliderType = false;
        String propCapped = p.getName().substring(0, 1).toUpperCase() + p.getName().substring(1);
        String minMethName = "getMin" + propCapped;
        String maxMethName = "getMax" + propCapped;
        SliderParams params = null;
        try {
            Method minMethod = filter.getClass().getMethod(minMethName, (Class[]) null);
            Method maxMethod = filter.getClass().getMethod(maxMethName, (Class[]) null);
            isSliderType = true;
            if (p.getPropertyType() == Integer.TYPE) {
                int min = (Integer) minMethod.invoke(filter);
                int max = (Integer) maxMethod.invoke(filter);
                params = new SliderParams(Integer.class, min, max, 0, 0);
            } else if (p.getPropertyType() == Float.TYPE) {
                float min = (Float) minMethod.invoke(filter);
                float max = (Float) maxMethod.invoke(filter);
                params = new SliderParams(Integer.class, 0, 0, min, max);
            }
        } catch (NoSuchMethodException e) {
        } catch (Exception iae) {
            log.warning(iae.toString() + " for property " + p + " in filter " + filter);
        }
        return params;
    }

    class Point2DControl extends JPanel implements HasSetter {

        Method write, read;

        EventFilter filter;

        Point2D.Float point;

        float initValue = 0, nval;

        final JTextField tfx, tfy;

        final String format = "%.1f";

        public final void set(Object o) {
            if (o instanceof Point2D.Float) {
                Point2D.Float b = (Point2D.Float) o;
                tfx.setText(String.format(format, b.x));
                tfy.setText(String.format(format, b.y));
            }
        }

        final class PointActionListener implements ActionListener {

            Method readMethod, writeMethod;

            Point2D.Float point = new Point2D.Float(0, 0);

            public PointActionListener(Method readMethod, Method writeMethod) {
                this.readMethod = readMethod;
                this.writeMethod = writeMethod;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    float x = Float.parseFloat(tfx.getText());
                    float y = Float.parseFloat(tfy.getText());
                    point.setLocation(x, y);
                    writeMethod.invoke(filter, point);
                    point = (Point2D.Float) readMethod.invoke(filter);
                    set(point);
                } catch (NumberFormatException fe) {
                    tfx.selectAll();
                    tfy.selectAll();
                } catch (InvocationTargetException ite) {
                    ite.printStackTrace();
                } catch (IllegalAccessException iae) {
                    iae.printStackTrace();
                }
            }
        }

        public Point2DControl(final EventFilter f, final String name, final Method w, final Method r) {
            super();
            setterMap.put(name, this);
            filter = f;
            write = w;
            read = r;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentX(ALIGNMENT);
            JLabel label = new JLabel(name);
            label.setAlignmentX(ALIGNMENT);
            label.setFont(label.getFont().deriveFont(fontSize));
            addTip(f, label);
            add(label);
            tfx = new JTextField("", 10);
            tfx.setMaximumSize(new Dimension(100, 50));
            tfx.setToolTipText("Point2D X: type new value here and press enter.");
            tfy = new JTextField("", 10);
            tfy.setMaximumSize(new Dimension(100, 50));
            tfy.setToolTipText("Point2D Y: type new value here and press enter.");
            try {
                Point2D.Float p = (Point2D.Float) r.invoke(filter);
                if (p == null) {
                    log.warning("null object returned from read method " + r);
                    return;
                }
                set(p);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                log.warning("cannot access the field named " + name + " check if the class or method is not public?");
                e.printStackTrace();
            }
            add(tfx);
            add(new JLabel(", "));
            add(tfy);
            tfx.addActionListener(new PointActionListener(r, w));
            tfy.addActionListener(new PointActionListener(r, w));
            tfx.addFocusListener(new FocusListener() {

                public void focusGained(FocusEvent e) {
                    tfx.setSelectionStart(0);
                    tfx.setSelectionEnd(tfx.getText().length());
                }

                public void focusLost(FocusEvent e) {
                }
            });
            tfy.addFocusListener(new FocusListener() {

                public void focusGained(FocusEvent e) {
                    tfy.setSelectionStart(0);
                    tfy.setSelectionEnd(tfy.getText().length());
                }

                public void focusLost(FocusEvent e) {
                }
            });
        }
    }
}
