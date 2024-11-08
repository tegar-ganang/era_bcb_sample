package org.gwt.mosaic.xul.client.ui;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import org.gwt.beansbinding.core.client.BeanProperty;
import org.gwt.beansbinding.core.client.BindingGroup;
import org.gwt.beansbinding.core.client.Bindings;
import org.gwt.beansbinding.core.client.AutoBinding.UpdateStrategy;
import org.gwt.beansbinding.observablecollections.client.ObservableMap;
import org.gwt.beansbinding.observablecollections.client.ObservableMapListener;
import org.gwt.mosaic.application.client.AbstractBean;
import org.gwt.mosaic.application.client.Application;
import org.gwt.mosaic.core.client.CoreConstants;
import org.gwt.mosaic.core.client.DOM;
import org.gwt.mosaic.ui.client.layout.HasLayoutManager;
import org.gwt.mosaic.ui.client.util.WidgetHelper;
import org.gwt.mosaic.xul.client.application.ApplicationContext;
import org.gwt.mosaic.xul.client.application.ElementMap;
import org.gwt.mosaic.xul.client.application.ObservableMapBean;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author georgopoulos.georgios(at)gmail.com
 * 
 */
public abstract class Element extends AbstractBean implements Serializable {

    private static final long serialVersionUID = -9043025694821549330L;

    protected ObservableMap<String, Serializable> attributeMap;

    public static final String OBSERVES_ELEMENT_LIST = "observes-element-list";

    /**
   * The align attribute specifies how child elements of the box are aligned
   * when the size of the box is larger than the total size of the children. For
   * boxes that have horizontal orientation, it specifies how its children will
   * be aligned vertically. For boxes that have vertical orientation, it is used
   * to specify how its children are aligned horizontal.
   */
    public static final String ALIGN = "align";

    public String getAlign() {
        return getString(ALIGN);
    }

    public void setAlign(String align) {
        putString(ALIGN, align);
    }

    public static final String COLSPAN = "colspan";

    public Integer getColspan() {
        return getInteger(COLSPAN);
    }

    public Integer setColpan(Integer colspan) {
        return getInteger(COLSPAN);
    }

    /**
   * This attribute can be used to make the children of the element equal in
   * size.
   * <p>
   * <li>{@code always}: For a horizontally oriented element, this will make all
   * of its children have the width of the widest child. For a vertically
   * oriented element, this will make its children all have the height of the
   * tallest child.</li>
   * <li>{@code never}: All of the children are displayed at the size required
   * by the content or as specified by the width and height attributes or the
   * CSS width and height properties.</li>
   */
    public static final String EQUALSIZE = "equalsize";

    public boolean isEqualize() {
        return getBoolean(EQUALSIZE);
    }

    public void setEqualsize(boolean equalsize) {
        putBoolean(EQUALSIZE, equalsize);
    }

    /**
   * The pack attribute specifies where the child elements of the box are placed
   * when the box is larger that the size of the children. For boxes with
   * horizontal orientation, it is used to indicate the position of children
   * horizontally.
   */
    public static final String PACK = "pack";

    public String getPack() {
        return getString(PACK);
    }

    public void setPack(String pack) {
        putString(PACK, pack);
    }

    public static final String ROWSPAN = "rowspan";

    public Integer getRowspan() {
        return getInteger(ROWSPAN);
    }

    public void setInteger(Integer rowspan) {
        putInteger(ROWSPAN, rowspan);
    }

    /**
   * The style class of the element. Multiple classes may be specified by
   * separating them with spaces.
   */
    public static final String CLASSNAME = "className";

    public String getClassName() {
        return getString(CLASSNAME);
    }

    public void setClassName(String classname) {
        putString(CLASSNAME, classname);
    }

    /**
   * If true, then the element is collapsed and does not appear. It is
   * equivalent to setting the CSS visibility property to collapse.
   */
    public static final String COLLAPSED = "collapsed";

    public boolean isCollapsed() {
        return getBoolean(COLLAPSED);
    }

    public void setCollapsed(boolean collapsed) {
        putBoolean(COLLAPSED, collapsed);
    }

    /**
   * Should be set to the value of the id of the popup element that should
   * appear when the user context-clicks on the element. A context-click varies
   * on each platform. Usually it will be a right click.
   */
    public static final String CONTEXTMENU = "contextmenu";

    public String getContextmenu() {
        return getString(CONTEXTMENU);
    }

    public void setContextmenu(String contextmenu) {
        putString(CONTEXTMENU, contextmenu);
    }

    /**
   * Indicates the flexibility of the element, which indicates how an element's
   * container distributes remaining empty space among its children. Flexible
   * elements grow and shrink to fit their given space. Elements with larger
   * flex values will be made larger than elements with lower flex values, at
   * the ratio determined by the two elements. The actual value is not relevant
   * unless there are other flexible elements within the same container. Once
   * the default sizes of elements in a box are calculated, the remaining space
   * in the box is divided among the flexible elements, according to their flex
   * ratios. Specifying a flex value of 0 has the same effect as leaving the
   * flex attribute out entirely.
   */
    public static final String FLEX = "flex";

    public int getFlex() {
        return getInteger(FLEX);
    }

    public void setFlex(int flex) {
        putInteger(FLEX, flex);
    }

    /**
   * If set to true, the element is not displayed. This is similar to setting
   * the CSS display property to 'none'.
   */
    public static final String HIDDEN = "hidden";

    public boolean isHidden() {
        return getBoolean(HIDDEN);
    }

    public void setHidden(boolean hidden) {
        putBoolean(HIDDEN, hidden);
    }

    /**
   * A unique identifier so that you can identify the element with. You can use
   * this as a parameter to getElementById() and other DOM functions and to
   * reference the element in style sheets.
   */
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, this.id);
    }

    /**
   * Set to an id of a broadcaster element that is being observed by the
   * element. If an attribute changes in the broadcaster it is also changed in
   * the observer. For boxes with vertical orientation, it is used to indicate
   * the position of children vertically. The align attribute is used to specify
   * the position in the opposite direction.
   */
    public static final String OBSERVES = "observes";

    public String getObserves() {
        return getString(OBSERVES);
    }

    public void setObserves(String observes) {
        putString(OBSERVES, observes);
    }

    /**
   * CSS style rules to be applied to the element. Syntax is as in the HTML
   * style attribute. It is preferred to put style rules in style sheets.
   */
    public static final String STYLE = "style";

    public String getStyle() {
        return getString(STYLE);
    }

    public void setStyle(String style) {
        putString(STYLE, style);
    }

    /**
   * Used to set the text which appears in the tooltip when the user moves the
   * mouse over the element. This can be used instead of setting the tooltip to
   * a popup for the common case where it contains only text. The tooltip is
   * displayed in a default tooltip which displays only a label, however the
   * default tooltip may be changed by setting the default attribute on a
   * tooltip element.
   */
    public static final String TOOLTIPTEXT = "tooltiptext";

    public String getTooltiptext() {
        return getString(TOOLTIPTEXT);
    }

    public void setTooltiptext(String tooltiptext) {
        putString(TOOLTIPTEXT, tooltiptext);
    }

    private transient Widget ui = null;

    private transient BindingGroup bindingGroup;

    /**
   * The parent element.
   */
    Container parent;

    /**
   * Default constructor.
   */
    public Element() {
        attributeMap = new ObservableMapBean<String, Serializable>();
    }

    private void addObservableMapListener() {
        attributeMap.addObservableMapListener(new ObservableMapListener() {

            @SuppressWarnings("unchecked")
            public void mapKeyAdded(ObservableMap map, Object key) {
                updateUI((String) key, null, map.get(key));
            }

            @SuppressWarnings("unchecked")
            public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
                updateUI((String) key, value, null);
            }

            @SuppressWarnings("unchecked")
            public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
                updateUI((String) key, lastValue, map.get(key));
            }
        });
    }

    protected abstract Widget createUI();

    protected void syncUI(Widget ui) {
        final com.google.gwt.user.client.Element elem = ui.getElement();
        String id = getId();
        elem.setId(id != null ? id : DOM.createUniqueId());
        if (attributeMap.containsKey(CLASSNAME)) {
            ui.addStyleName(getClassName());
        }
        if (isCollapsed()) {
            DOM.setStyleAttribute(elem, "visibility", "collapsed");
        } else {
            DOM.setStyleAttribute(elem, "visibility", "visible");
        }
        ui.setVisible(!isHidden());
        if (attributeMap.containsKey(STYLE)) {
            String styles[] = getStyle().split(";");
            for (String style : styles) {
                String[] property = style.split(":");
                if (property != null && property.length == 2) {
                    property[0] = property[0].trim();
                    property[1] = property[1].trim();
                    assertCamelCase(property[0]);
                    DOM.setStyleAttribute(elem, property[0], property[1]);
                }
            }
        }
        if (attributeMap.containsKey(TOOLTIPTEXT)) {
            ui.setTitle(getTooltiptext());
        }
    }

    /**
   * Assert that the specified property does not contain a hyphen.
   * 
   * @param name the property name
   */
    private void assertCamelCase(String name) {
        assert !name.contains("-") : "The style name '" + name + "' should be in camelCase format";
    }

    public ObservableMap<String, Serializable> getAttributeMap() {
        return attributeMap;
    }

    protected BindingGroup getBindingGroup() {
        if (bindingGroup == null) {
            bindingGroup = new BindingGroup();
        }
        return bindingGroup;
    }

    protected Boolean getBoolean(final String key) {
        return attributeMap.containsKey(key) ? (Boolean) attributeMap.get(key) : Boolean.FALSE;
    }

    protected Boolean putBoolean(final String key, final Boolean value) {
        return (Boolean) attributeMap.put(key, value);
    }

    protected Character getCharacter(final String key) {
        return (Character) attributeMap.get(key);
    }

    protected Character putCharacter(final String key, final Character value) {
        return (Character) attributeMap.put(key, value);
    }

    protected Integer getInteger(final String key) {
        return (Integer) attributeMap.get(key);
    }

    protected Integer putInteger(final String key, final Integer value) {
        return (Integer) attributeMap.put(key, value);
    }

    protected String getString(final String key) {
        return (String) attributeMap.get(key);
    }

    protected String putString(final String key, final String value) {
        return (String) attributeMap.put(key, value);
    }

    public Container getParent() {
        return parent;
    }

    public Widget getUI() {
        if (!GWT.isClient()) {
            return null;
        }
        if (ui == null) {
            ui = createUI();
            addObservableMapListener();
            handleObservesAttribute();
            handleObservesElementList();
            if (bindingGroup != null && bindingGroup.getBindings().size() > 0) {
                bindingGroup.bind();
            }
            registerElement(this);
        }
        return ui;
    }

    private void handleObservesAttribute() {
        if (attributeMap.containsKey(OBSERVES)) {
            final Element broadcasterElem = getElementMap().get(getObserves());
            if (broadcasterElem != null) {
                final Set<String> keys = broadcasterElem.getAttributeMap().keySet();
                for (String key : keys) {
                    getBindingGroup().addBinding(Bindings.createAutoBinding(UpdateStrategy.READ, broadcasterElem.getAttributeMap(), BeanProperty.<ObservableMap<String, Serializable>, Serializable>create(key), this, BeanProperty.<Element, Serializable>create(key)));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleObservesElementList() {
        final List<Observes> list = (List<Observes>) attributeMap.get(OBSERVES_ELEMENT_LIST);
        if (list != null) {
            for (Observes element : list) {
                final Element broadcaster = getElementMap().get(element.getElement());
                final String attribute = element.getAttribute();
                if (broadcaster != null && attribute != null) {
                    final String strategy = element.getStrategy();
                    UpdateStrategy updateStrategy = UpdateStrategy.READ;
                    if ("readonce".equalsIgnoreCase(strategy)) {
                        updateStrategy = UpdateStrategy.READ_ONCE;
                    } else if ("readwrite".equalsIgnoreCase(strategy)) {
                        updateStrategy = UpdateStrategy.READ_WRITE;
                    }
                    getBindingGroup().addBinding(Bindings.createAutoBinding(updateStrategy, broadcaster.getAttributeMap(), BeanProperty.<ObservableMap<String, Serializable>, Serializable>create(attribute), getAttributeMap(), BeanProperty.<ObservableMap<String, Serializable>, Serializable>create(element.getTarget() == null ? attribute : element.getTarget())));
                }
            }
        }
    }

    private void registerElement(Element element) {
        if (element.id != null) {
            final ElementMap elementMap = getElementMap();
            if (!GWT.isScript() && elementMap.get(element.id) != null) {
                GWT.log("Doublicate entry for element id='" + element.id + "'!", null);
            }
            elementMap.put(element.id, element);
        }
    }

    private transient Timer layoutTimer = null;

    private void updateUI(String property, Object oldValue, Object newValue) {
        if (FLEX.equals(property)) {
            parent.syncUI(parent.getUI());
            parent.delayedLayout();
        } else {
            syncUI(getUI());
            delayedLayout();
        }
        firePropertyChange(property, oldValue, newValue);
    }

    protected void delayedLayout() {
        if (layoutTimer == null) {
            layoutTimer = new Timer() {

                @Override
                public void run() {
                    layout();
                }
            };
        }
        layoutTimer.schedule(CoreConstants.DEFAULT_DELAY_MILLIS);
    }

    protected void layout() {
        WidgetHelper.invalidate(getUI());
        HasLayoutManager panel = WidgetHelper.getParent(getUI());
        if (panel != null) {
            panel.layout();
        }
    }

    private ElementMap getElementMap() {
        ApplicationContext context = (ApplicationContext) Application.getInstance().getContext();
        return context.getElementMap();
    }
}
