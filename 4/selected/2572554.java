package jes.jazz.groovy;

import groovy.lang.Closure;
import groovy.model.DefaultTableModel;
import groovy.model.ValueHolder;
import groovy.model.ValueModel;
import groovy.swing.impl.ComponentFacade;
import groovy.swing.impl.ContainerFacade;
import groovy.swing.impl.DefaultAction;
import groovy.swing.impl.Factory;
import groovy.swing.impl.Startable;
import groovy.swing.impl.TableLayout;
import groovy.swing.impl.TableLayoutCell;
import groovy.swing.impl.TableLayoutRow;
import groovy.util.BuilderSupport;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * A helper class for creating Swing widgets using GroovyMarkup
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision: 1.7 $
 */
public class JazzBuilder extends BuilderSupport {

    private Logger log = Logger.getLogger(getClass().getName());

    private Map factories = new HashMap();

    private Object constraints;

    public JazzBuilder(JazzGUI conductor) {
        registerWidgets();
    }

    protected void setParent(Object parent, Object child) {
        if (child instanceof Action) {
            Action action = (Action) child;
            InvokerHelper.setProperty(parent, "action", action);
            Object keyStroke = action.getValue("KeyStroke");
            if (parent instanceof JComponent) {
                JComponent component = (JComponent) parent;
                KeyStroke stroke = null;
                if (keyStroke instanceof String) {
                    stroke = KeyStroke.getKeyStroke((String) keyStroke);
                } else if (keyStroke instanceof KeyStroke) {
                    stroke = (KeyStroke) keyStroke;
                }
                if (stroke != null) {
                    String key = action.toString();
                    component.getInputMap().put(stroke, key);
                    component.getActionMap().put(key, action);
                }
            }
        } else if (child instanceof LayoutManager) {
            if (parent instanceof RootPaneContainer) {
                RootPaneContainer rpc = (RootPaneContainer) parent;
                parent = rpc.getContentPane();
            }
            InvokerHelper.setProperty(parent, "layout", child);
        } else if (parent instanceof JTable && child instanceof TableColumn) {
            JTable table = (JTable) parent;
            TableColumn column = (TableColumn) child;
            table.addColumn(column);
        } else {
            Component component = null;
            if (child instanceof Component) {
                component = (Component) child;
            } else if (child instanceof ComponentFacade) {
                ComponentFacade facade = (ComponentFacade) child;
                component = facade.getComponent();
            }
            if (component != null) {
                if (parent instanceof JFrame && component instanceof JMenuBar) {
                    JFrame frame = (JFrame) parent;
                    frame.setJMenuBar((JMenuBar) component);
                } else if (parent instanceof RootPaneContainer) {
                    RootPaneContainer rpc = (RootPaneContainer) parent;
                    rpc.getContentPane().add(component);
                } else if (parent instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) parent;
                    scrollPane.setViewportView(component);
                } else if (parent instanceof JSplitPane) {
                    JSplitPane splitPane = (JSplitPane) parent;
                    if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                        if (splitPane.getTopComponent() == null) {
                            splitPane.setTopComponent(component);
                        } else {
                            splitPane.setBottomComponent(component);
                        }
                    } else {
                        if (splitPane.getLeftComponent() == null) {
                            splitPane.setLeftComponent(component);
                        } else {
                            splitPane.setRightComponent(component);
                        }
                    }
                } else if (parent instanceof JMenuBar && component instanceof JMenu) {
                    JMenuBar menuBar = (JMenuBar) parent;
                    menuBar.add((JMenu) component);
                } else if (parent instanceof Container) {
                    Container container = (Container) parent;
                    if (constraints != null) {
                        container.add(component, constraints);
                    } else {
                        container.add(component);
                    }
                } else if (parent instanceof ContainerFacade) {
                    ContainerFacade facade = (ContainerFacade) parent;
                    facade.addComponent(component);
                }
            }
        }
    }

    protected void nodeCompleted(Object parent, Object node) {
        if (node instanceof TableModel && parent instanceof JTable) {
            JTable table = (JTable) parent;
            TableModel model = (TableModel) node;
            table.setModel(model);
        }
        if (node instanceof Startable) {
            Startable startable = (Startable) node;
            startable.start();
        }
    }

    protected Object createNode(Object name) {
        return createNode(name, Collections.EMPTY_MAP);
    }

    protected Object createNode(Object name, Object value) {
        Object widget = createNode(name);
        if (widget != null && value instanceof String) {
            InvokerHelper.invokeMethod(widget, "setText", value);
        }
        return widget;
    }

    protected Object createNode(Object name, Map attributes, Object value) {
        Object widget = createNode(name, attributes);
        if (widget != null) {
            InvokerHelper.invokeMethod(widget, "setText", value.toString());
        }
        return widget;
    }

    protected Object createNode(Object name, Map attributes) {
        constraints = attributes.remove("constraints");
        Object widget = null;
        Factory factory = (Factory) factories.get(name);
        if (factory != null) {
            try {
                widget = factory.newInstance(attributes);
                if (widget == null) {
                    log.log(Level.WARNING, "Factory for name: " + name + " returned null");
                } else {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("For name: " + name + " created widget: " + widget);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create component for" + name + " reason: " + e, e);
            }
        } else {
            log.log(Level.WARNING, "Could not find match for name: " + name);
        }
        if (widget != null) {
            if (widget instanceof Action) {
                Action action = (Action) widget;
                Closure closure = (Closure) attributes.remove("closure");
                if (closure != null && action instanceof DefaultAction) {
                    DefaultAction defaultAction = (DefaultAction) action;
                    defaultAction.setClosure(closure);
                }
                for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String actionName = (String) entry.getKey();
                    actionName = capitalize(actionName);
                    Object value = entry.getValue();
                    action.putValue(actionName, value);
                }
            } else {
                for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String property = entry.getKey().toString();
                    Object value = entry.getValue();
                    InvokerHelper.setProperty(widget, property, value);
                }
            }
        }
        return widget;
    }

    protected String capitalize(String text) {
        char ch = text.charAt(0);
        if (Character.isUpperCase(ch)) {
            return text;
        }
        StringBuffer buffer = new StringBuffer(text.length());
        buffer.append(Character.toUpperCase(ch));
        buffer.append(text.substring(1));
        return buffer.toString();
    }

    protected void registerWidgets() {
        registerBeanFactory("action", DefaultAction.class);
        registerFactory("boxLayout", new Factory() {

            public Object newInstance(Map properties) throws InstantiationException, InstantiationException, IllegalAccessException {
                return createBoxLayout(properties);
            }
        });
        registerBeanFactory("button", JButton.class);
        registerBeanFactory("buttonGroup", ButtonGroup.class);
        registerBeanFactory("checkBox", JCheckBox.class);
        registerBeanFactory("checkBoxMenuItem", JCheckBoxMenuItem.class);
        registerFactory("comboBox", new Factory() {

            public Object newInstance(Map properties) throws InstantiationException, InstantiationException, IllegalAccessException {
                return createComboBox(properties);
            }
        });
        registerBeanFactory("desktopPane", JDesktopPane.class);
        registerFactory("dialog", new Factory() {

            public Object newInstance(Map properties) throws InstantiationException, InstantiationException, IllegalAccessException {
                return createDialog(properties);
            }
        });
        registerBeanFactory("editorPane", JEditorPane.class);
        registerBeanFactory("fileChooser", JFileChooser.class);
        registerBeanFactory("frame", JFrame.class);
        registerBeanFactory("internalFrame", JInternalFrame.class);
        registerBeanFactory("label", JLabel.class);
        registerBeanFactory("list", JList.class);
        registerFactory("map", new Factory() {

            public Object newInstance(Map properties) throws InstantiationException, InstantiationException, IllegalAccessException {
                return properties;
            }
        });
        registerBeanFactory("menu", JMenu.class);
        registerBeanFactory("menuBar", JMenuBar.class);
        registerBeanFactory("menuItem", JMenuItem.class);
        registerBeanFactory("panel", JPanel.class);
        registerBeanFactory("passwordField", JPasswordField.class);
        registerBeanFactory("popupMenu", JPopupMenu.class);
        registerBeanFactory("progressBar", JProgressBar.class);
        registerBeanFactory("radioButton", JRadioButton.class);
        registerBeanFactory("radioButtonMenuItem", JRadioButtonMenuItem.class);
        registerBeanFactory("optionPane", JOptionPane.class);
        registerBeanFactory("scrollPane", JScrollPane.class);
        registerBeanFactory("separator", JSeparator.class);
        registerFactory("splitPane", new Factory() {

            public Object newInstance(Map properties) {
                JSplitPane answer = new JSplitPane();
                answer.setLeftComponent(null);
                answer.setRightComponent(null);
                answer.setTopComponent(null);
                answer.setBottomComponent(null);
                return answer;
            }
        });
        registerFactory("hbox", new Factory() {

            public Object newInstance(Map properties) {
                return Box.createHorizontalBox();
            }
        });
        registerFactory("vbox", new Factory() {

            public Object newInstance(Map properties) {
                return Box.createVerticalBox();
            }
        });
        registerBeanFactory("tabbedPane", JTabbedPane.class);
        registerBeanFactory("table", JTable.class);
        registerBeanFactory("textArea", JTextArea.class);
        registerBeanFactory("textPane", JTextPane.class);
        registerBeanFactory("textField", JTextField.class);
        registerBeanFactory("toggleButton", JToggleButton.class);
        registerBeanFactory("tree", JTree.class);
        registerBeanFactory("toolBar", JToolBar.class);
        registerFactory("tableModel", new Factory() {

            public Object newInstance(Map properties) {
                ValueModel model = (ValueModel) properties.remove("model");
                if (model == null) {
                    Object list = properties.remove("list");
                    if (list == null) {
                        list = new ArrayList();
                    }
                    model = new ValueHolder(list);
                }
                return new DefaultTableModel(model);
            }
        });
        registerFactory("propertyColumn", new Factory() {

            public Object newInstance(Map properties) {
                Object current = getCurrent();
                if (current instanceof DefaultTableModel) {
                    DefaultTableModel model = (DefaultTableModel) current;
                    Object header = properties.remove("header");
                    if (header == null) {
                        header = "";
                    }
                    String property = (String) properties.remove("propertyName");
                    if (property == null) {
                        throw new IllegalArgumentException("Must specify a property for a propertyColumn");
                    }
                    Class type = (Class) properties.remove("type");
                    if (type == null) {
                        type = Object.class;
                    }
                    return model.addPropertyColumn(header, property, type);
                } else {
                    throw new RuntimeException("propertyColumn must be a child of a tableModel");
                }
            }
        });
        registerFactory("closureColumn", new Factory() {

            public Object newInstance(Map properties) {
                Object current = getCurrent();
                if (current instanceof DefaultTableModel) {
                    DefaultTableModel model = (DefaultTableModel) current;
                    Object header = properties.remove("header");
                    if (header == null) {
                        header = "";
                    }
                    Closure readClosure = (Closure) properties.remove("read");
                    if (readClosure == null) {
                        throw new IllegalArgumentException("Must specify 'read' Closure property for a closureColumn");
                    }
                    Closure writeClosure = (Closure) properties.remove("write");
                    Class type = (Class) properties.remove("type");
                    if (type == null) {
                        type = Object.class;
                    }
                    return model.addClosureColumn(header, readClosure, writeClosure, type);
                } else {
                    throw new RuntimeException("propertyColumn must be a child of a tableModel");
                }
            }
        });
        registerBeanFactory("tableLayout", TableLayout.class);
        registerFactory("tr", new Factory() {

            public Object newInstance(Map properties) {
                Object parent = getCurrent();
                if (parent instanceof TableLayout) {
                    return new TableLayoutRow((TableLayout) parent);
                } else {
                    throw new RuntimeException("'tr' must be within a 'tableLayout'");
                }
            }
        });
        registerFactory("td", new Factory() {

            public Object newInstance(Map properties) {
                Object parent = getCurrent();
                if (parent instanceof TableLayoutRow) {
                    return new TableLayoutCell((TableLayoutRow) parent);
                } else {
                    throw new RuntimeException("'td' must be within a 'tr'");
                }
            }
        });
    }

    protected Object createBoxLayout(Map properties) {
        Object parent = getCurrent();
        if (parent instanceof Container) {
            Object axisObject = properties.remove("axis");
            int axis = 0;
            if (axisObject != null) {
                Integer i = (Integer) axisObject;
                axis = i.intValue();
            }
            BoxLayout answer = new BoxLayout((Container) parent, axis);
            InvokerHelper.setProperty(parent, "layout", answer);
            return answer;
        } else {
            throw new RuntimeException("Must be nested inside a Container");
        }
    }

    protected Object createDialog(Map properties) {
        Object owner = properties.remove("owner");
        if (owner instanceof Frame) {
            return new JDialog((Frame) owner);
        } else if (owner instanceof Dialog) {
            return new JDialog((Dialog) owner);
        } else {
            return new JDialog();
        }
    }

    protected Object createComboBox(Map properties) {
        Object items = properties.remove("items");
        if (items instanceof Vector) {
            return new JComboBox((Vector) items);
        } else if (items instanceof List) {
            List list = (List) items;
            return new JComboBox(list.toArray());
        } else if (items instanceof Object[]) {
            return new JComboBox((Object[]) items);
        } else {
            return new JComboBox();
        }
    }

    protected void registerBeanFactory(String name, final Class beanClass) {
        registerFactory(name, new Factory() {

            public Object newInstance(Map properties) throws InstantiationException, IllegalAccessException {
                return beanClass.newInstance();
            }
        });
    }

    protected void registerFactory(String name, Factory factory) {
        factories.put(name, factory);
    }
}
