package org.apache.myfaces.trinidad.component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import org.apache.myfaces.trinidad.bean.FacesBean;
import org.apache.myfaces.trinidad.bean.FacesBeanFactory;
import org.apache.myfaces.trinidad.bean.PropertyKey;
import org.apache.myfaces.trinidad.bean.util.StateUtils;
import org.apache.myfaces.trinidad.bean.util.ValueMap;
import org.apache.myfaces.trinidad.change.AttributeComponentChange;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.AttributeChangeEvent;
import org.apache.myfaces.trinidad.event.AttributeChangeListener;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidad.render.ExtendedRenderer;
import org.apache.myfaces.trinidad.render.LifecycleRenderer;
import org.apache.myfaces.trinidad.util.ThreadLocalUtils;

public abstract class UIXComponentBase extends UIXComponent {

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(UIXComponentBase.class);

    public static final FacesBean.Type TYPE = _createType();

    public static final PropertyKey ID_KEY = TYPE.registerKey("id", String.class, PropertyKey.CAP_NOT_BOUND);

    private static final PropertyKey _GENERATED_ID_KEY = TYPE.registerKey("_genId", String.class, PropertyKey.CAP_NOT_BOUND);

    public static final PropertyKey RENDERED_KEY = TYPE.registerKey("rendered", Boolean.class, Boolean.TRUE);

    public static final PropertyKey BINDING_KEY = TYPE.registerKey("binding");

    public static final PropertyKey TRANSIENT_KEY = TYPE.registerKey("transient", Boolean.class, PropertyKey.CAP_NOT_BOUND | PropertyKey.CAP_TRANSIENT);

    public static final PropertyKey RENDERER_TYPE_KEY = TYPE.registerKey("rendererType", String.class, PropertyKey.CAP_NOT_BOUND);

    private static final PropertyKey _LISTENERS_KEY = TYPE.registerKey("listeners", FacesListener[].class, PropertyKey.CAP_LIST);

    private static final PropertyKey _ATTRIBUTE_CHANGE_LISTENER_KEY = TYPE.registerKey("attributeChangeListener", MethodExpression.class);

    static {
        TYPE.registerKey("javax.faces.webapp.COMPONENT_IDS", List.class, PropertyKey.CAP_NOT_BOUND);
        TYPE.registerKey("javax.faces.webapp.FACET_NAMES", List.class, PropertyKey.CAP_NOT_BOUND);
        TYPE.lock();
    }

    public UIXComponentBase() {
    }

    public UIXComponentBase(String rendererType) {
        setRendererType(rendererType);
    }

    protected FacesBean createFacesBean(String rendererType) {
        FacesBean bean = FacesBeanFactory.createFacesBean(getClass(), rendererType);
        UIXFacesBean uixBean = (UIXFacesBean) bean;
        uixBean.init(this, getBeanType());
        return uixBean;
    }

    protected PropertyKey getPropertyKey(String name) {
        PropertyKey key = getBeanType().findKey(name);
        if (key == null) key = PropertyKey.createPropertyKey(name);
        return key;
    }

    protected FacesBean.Type getBeanType() {
        return TYPE;
    }

    @Override
    public FacesBean getFacesBean() {
        if (_facesBean == null) _init(null);
        return _facesBean;
    }

    @Override
    public String getContainerClientId(FacesContext context, UIComponent child) {
        return getContainerClientId(context);
    }

    @Override
    public void addAttributeChangeListener(AttributeChangeListener acl) {
        addFacesListener(acl);
    }

    @Override
    public void removeAttributeChangeListener(AttributeChangeListener acl) {
        removeFacesListener(acl);
    }

    @Override
    public AttributeChangeListener[] getAttributeChangeListeners() {
        return (AttributeChangeListener[]) getFacesListeners(AttributeChangeListener.class);
    }

    @Override
    public void setAttributeChangeListener(MethodExpression mb) {
        setProperty(_ATTRIBUTE_CHANGE_LISTENER_KEY, mb);
    }

    @Deprecated
    public void setAttributeChangeListener(MethodBinding mb) {
        setAttributeChangeListener(adaptMethodBinding(mb));
    }

    @Override
    public MethodExpression getAttributeChangeListener() {
        return (MethodExpression) getProperty(_ATTRIBUTE_CHANGE_LISTENER_KEY);
    }

    @Override
    public ValueExpression getValueExpression(String name) {
        if (name == null) throw new NullPointerException();
        PropertyKey key = getPropertyKey(name);
        if (!key.getSupportsBinding()) return null;
        return getFacesBean().getValueExpression(key);
    }

    @Override
    public void setValueExpression(String name, ValueExpression expression) {
        if (name == null) throw new NullPointerException();
        if ((expression != null) && expression.isLiteralText()) {
            ELContext context = FacesContext.getCurrentInstance().getELContext();
            getAttributes().put(name, expression.getValue(context));
        } else {
            PropertyKey key = getPropertyKey(name);
            getFacesBean().setValueExpression(key, expression);
        }
    }

    /**
   */
    @Override
    public ValueBinding getValueBinding(String name) {
        if (name == null) throw new NullPointerException();
        PropertyKey key = getPropertyKey(name);
        if (!key.getSupportsBinding()) return null;
        return getFacesBean().getValueBinding(key);
    }

    @Override
    public void setValueBinding(String name, ValueBinding binding) {
        if (name == null) throw new NullPointerException();
        PropertyKey key = getPropertyKey(name);
        getFacesBean().setValueBinding(key, binding);
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (_attributes == null) _init(null);
        return _attributes;
    }

    @Override
    public String getClientId(FacesContext context) {
        String clientId = getId();
        if (clientId == null) {
            clientId = (String) getProperty(_GENERATED_ID_KEY);
            if (clientId == null) {
                clientId = context.getViewRoot().createUniqueId();
                setProperty(_GENERATED_ID_KEY, clientId);
            }
        }
        UIComponent containerComponent = getParent();
        while (null != containerComponent) {
            if (containerComponent instanceof NamingContainer) {
                String contClientId;
                if (containerComponent instanceof UIXComponent) contClientId = ((UIXComponent) containerComponent).getContainerClientId(context, this); else contClientId = containerComponent.getContainerClientId(context);
                StringBuilder bld = __getSharedStringBuilder();
                bld.append(contClientId).append(NamingContainer.SEPARATOR_CHAR).append(clientId);
                clientId = bld.toString();
                break;
            }
            containerComponent = containerComponent.getParent();
        }
        Renderer renderer = getRenderer(context);
        if (null != renderer) clientId = renderer.convertClientId(context, clientId);
        return clientId;
    }

    /**
   * Gets the identifier for the component.
   */
    @Override
    public String getId() {
        return (String) getProperty(ID_KEY);
    }

    /**
   * Sets the identifier for the component.  The identifier
   * must follow a subset of the syntax allowed in HTML:
   * <ul>
   * <li>Must not be a zero-length String.</li>
   * <li>First character must be an ASCII letter (A-Za-z) or an underscore ('_').</li>
   * <li>Subsequent characters must be an ASCII letter or digit (A-Za-z0-9), an underscore ('_'), or a dash ('-').
   * </ul>
   */
    @Override
    public void setId(String id) {
        _validateId(id);
        if (id != null) setProperty(_GENERATED_ID_KEY, null);
        setProperty(ID_KEY, id);
    }

    @Override
    public abstract String getFamily();

    @Override
    public UIComponent getParent() {
        return _parent;
    }

    /**
   * <p>Set the parent <code>UIComponent</code> of this
   * <code>UIComponent</code>.</p>
   *
   * @param parent The new parent, or <code>null</code> for the root node
   *  of a component tree
   */
    @Override
    public void setParent(UIComponent parent) {
        _parent = parent;
    }

    @Override
    public boolean isRendered() {
        return getBooleanProperty(RENDERED_KEY, true);
    }

    @Override
    public void setRendered(boolean rendered) {
        setBooleanProperty(RENDERED_KEY, rendered);
    }

    public boolean isTransient() {
        return getBooleanProperty(TRANSIENT_KEY, false);
    }

    public void setTransient(boolean newTransient) {
        setBooleanProperty(TRANSIENT_KEY, newTransient);
    }

    @Override
    public String getRendererType() {
        if (_facesBean == null) return null;
        return (String) getProperty(RENDERER_TYPE_KEY);
    }

    @Override
    public void setRendererType(String rendererType) {
        String oldRendererType = getRendererType();
        if (oldRendererType == null) {
            if (rendererType == null) return;
        } else if (oldRendererType.equals(rendererType)) {
            return;
        }
        _init(rendererType);
        setProperty(RENDERER_TYPE_KEY, rendererType);
    }

    @Override
    public boolean getRendersChildren() {
        Renderer renderer = getRenderer(getFacesContext());
        if (renderer == null) return false;
        return renderer.getRendersChildren();
    }

    @Override
    public UIComponent findComponent(String id) {
        if (id == null) throw new NullPointerException();
        if ("".equals(id)) throw new IllegalArgumentException();
        UIComponent from = this;
        if (id.charAt(0) == NamingContainer.SEPARATOR_CHAR) {
            id = id.substring(1);
            while (from.getParent() != null) from = from.getParent();
        } else if (this instanceof NamingContainer) {
            ;
        } else {
            while (from.getParent() != null) {
                from = from.getParent();
                if (from instanceof NamingContainer) break;
            }
        }
        String searchId;
        int separatorIndex = id.indexOf(NamingContainer.SEPARATOR_CHAR);
        if (separatorIndex < 0) searchId = id; else searchId = id.substring(0, separatorIndex);
        if (searchId.equals(from.getId())) {
            ;
        } else {
            from = _findInsideOf(from, searchId);
        }
        if (separatorIndex < 0) {
            return from;
        } else {
            if (from == null) return null;
            if (!(from instanceof NamingContainer)) throw new IllegalArgumentException();
            return from.findComponent(id.substring(separatorIndex + 1));
        }
    }

    /**
   * <p>Create (if necessary) and return a List of the children associated
   * with this component.</p>
   */
    @Override
    public List<UIComponent> getChildren() {
        if (_children == null) _children = new ChildArrayList(this);
        return _children;
    }

    @Override
    public int getChildCount() {
        if (_children == null) return 0;
        return getChildren().size();
    }

    /**
   * <p>Create (if necessary) and return a Map of the facets associated
   * with this component.</p>
   */
    @Override
    public Map<String, UIComponent> getFacets() {
        if (_facets == null) _facets = new FacetHashMap(this);
        return _facets;
    }

    @Override
    public UIComponent getFacet(String facetName) {
        if (facetName == null) throw new NullPointerException();
        if (_facets == null) return null;
        return getFacets().get(facetName);
    }

    /**
   * Returns an Iterator over the names of all facets.
   * Unlike getFacets().keySet().iterator(), this does
   * not require instantiating a Map if there are
   * no facets.  (Note that this is not part of the
   * UIComponent API.)
   */
    public Iterator<String> getFacetNames() {
        if (_facets == null) return _EMPTY_STRING_ITERATOR;
        return _facets.keySet().iterator();
    }

    @Override
    public Iterator<UIComponent> getFacetsAndChildren() {
        if (_facets == null) {
            if (_children == null) return _EMPTY_UICOMPONENT_ITERATOR;
            return _children.iterator();
        } else {
            if (_children == null) return _facets.values().iterator();
        }
        return new CompositeIterator<UIComponent>(_children.iterator(), _facets.values().iterator());
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event == null) throw new NullPointerException();
        if (_LOG.isFine()) _LOG.fine("Broadcasting event " + event + " to " + this);
        UIComponent component = event.getComponent();
        if (component != null) {
            RequestContext adfContext = RequestContext.getCurrentInstance();
            if (adfContext != null) adfContext.partialUpdateNotify(component);
        }
        Iterator<FacesListener> iter = (Iterator<FacesListener>) getFacesBean().entries(_LISTENERS_KEY);
        while (iter.hasNext()) {
            FacesListener listener = iter.next();
            if (event.isAppropriateListener(listener)) {
                event.processListener(listener);
            }
        }
        if (event instanceof AttributeChangeEvent) {
            broadcastToMethodExpression(event, getAttributeChangeListener());
        }
    }

    @Override
    public void decode(FacesContext context) {
        if (context == null) throw new NullPointerException();
        Map<String, Object> attrs = getAttributes();
        Object triggers = attrs.get("partialTriggers");
        if (triggers instanceof String[]) {
            RequestContext adfContext = RequestContext.getCurrentInstance();
            if (adfContext != null) adfContext.addPartialTriggerListeners(this, (String[]) triggers);
        }
        __rendererDecode(context);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (context == null) throw new NullPointerException();
        if (!isRendered()) return;
        _cacheRenderer(context);
        Renderer renderer = getRenderer(context);
        if (renderer != null) {
            renderer.encodeBegin(context, this);
        }
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        if (context == null) throw new NullPointerException();
        if (!isRendered()) return;
        Renderer renderer = getRenderer(context);
        if (renderer != null) {
            renderer.encodeChildren(context, this);
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (context == null) throw new NullPointerException();
        if (isRendered()) {
            Renderer renderer = getRenderer(context);
            if (renderer != null) {
                renderer.encodeEnd(context, this);
            }
        }
    }

    /**
   * Encodes a component and all of its children, whether
   * getRendersChildren() is true or false.  When rendersChildren
   * is false, each child whose "rendered" property is true
   * will be sequentially rendered;  facets will be ignored.
   */
    @Override
    public void encodeAll(FacesContext context) throws IOException {
        if (context == null) throw new NullPointerException();
        __encodeRecursive(context, this);
    }

    @Override
    public void queueEvent(FacesEvent event) {
        if (event == null) throw new NullPointerException();
        UIComponent parent = getParent();
        if (parent == null) throw new IllegalStateException();
        parent.queueEvent(event);
    }

    @Override
    public void processDecodes(FacesContext context) {
        if (context == null) throw new NullPointerException();
        if (!isRendered()) return;
        decodeChildren(context);
        decode(context);
    }

    @Override
    public void processValidators(FacesContext context) {
        if (context == null) throw new NullPointerException();
        if (!isRendered()) return;
        validateChildren(context);
    }

    @Override
    public void processUpdates(FacesContext context) {
        if (context == null) throw new NullPointerException();
        if (!isRendered()) return;
        updateChildren(context);
    }

    @Override
    public Object processSaveState(FacesContext context) {
        if (context == null) throw new NullPointerException();
        if (_LOG.isFiner()) _LOG.finer("processSaveState() on " + this);
        Object state = null;
        try {
            if (((_children == null) || _children.isEmpty()) && ((_facets == null) || _facets.isEmpty())) {
                state = saveState(context);
            } else {
                TreeState treeState = new TreeState();
                treeState.saveState(context, this);
                if (treeState.isEmpty()) state = null;
                state = treeState;
            }
        } catch (RuntimeException e) {
            _LOG.warning(_LOG.getMessage("COMPONENT_CHILDREN_SAVED_STATE_FAILED", this));
            throw e;
        }
        if (StateUtils.checkComponentStateSerialization(context)) {
            try {
                new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(state);
            } catch (IOException e) {
                throw new RuntimeException(_LOG.getMessage("COMPONENT_SAVED_STATE_FAILED", this), e);
            }
        }
        return state;
    }

    @Override
    public void processRestoreState(FacesContext context, Object state) {
        if (context == null) throw new NullPointerException();
        if (_LOG.isFiner()) _LOG.finer("processRestoreState() on " + this);
        if (state instanceof TreeState) {
            ((TreeState) state).restoreState(context, this);
        } else {
            restoreState(context, state);
        }
    }

    @Override
    public void markInitialState() {
        getFacesBean().markInitialState();
    }

    public Object saveState(FacesContext context) {
        return getFacesBean().saveState(context);
    }

    public void restoreState(FacesContext context, Object stateObj) {
        getFacesBean().restoreState(context, stateObj);
    }

    @Override
    public String toString() {
        String className = getClass().getName();
        int periodIndex = className.lastIndexOf('.');
        if (periodIndex >= 0) className = className.substring(periodIndex + 1);
        return className + "[" + getFacesBean().toString() + ", id=" + getId() + "]";
    }

    /**
   * <p>Return the {@link FacesContext} instance for the current request.</p>
   */
    @Override
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    /**
   * Delegates to LifecycleRenderer, if present,
   * otherwise calls decodeChildrenImpl.
   *
   * @param context the current FacesContext
   */
    protected final void decodeChildren(FacesContext context) {
        LifecycleRenderer renderer = getLifecycleRenderer(context);
        if (renderer != null) {
            if (renderer.decodeChildren(context, this)) return;
        }
        decodeChildrenImpl(context);
    }

    /**
   * Calls processDecodes on all facets and children of this
   * component.
   * @param context the current FacesContext
   */
    protected void decodeChildrenImpl(FacesContext context) {
        Iterator<UIComponent> kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = kids.next();
            kid.processDecodes(context);
        }
    }

    /**
   * Delegates to LifecycleRenderer, if present,
   * otherwise calls validateChildrenImpl.
   *
   * @param context the current FacesContext
   */
    protected final void validateChildren(FacesContext context) {
        LifecycleRenderer renderer = getLifecycleRenderer(context);
        if (renderer != null) {
            if (renderer.validateChildren(context, this)) return;
        }
        validateChildrenImpl(context);
    }

    /**
   * Calls processValidators on all facets and children of this
   * component.
   * @param context the current FacesContext
   */
    protected void validateChildrenImpl(FacesContext context) {
        Iterator<UIComponent> kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = kids.next();
            kid.processValidators(context);
        }
    }

    /**
   * Delegates to LifecycleRenderer, if present,
   * otherwise calls upateChildrenImpl.
   *
   * @param context the current FacesContext
   */
    protected final void updateChildren(FacesContext context) {
        LifecycleRenderer renderer = getLifecycleRenderer(context);
        if (renderer != null) {
            if (renderer.updateChildren(context, this)) return;
        }
        updateChildrenImpl(context);
    }

    protected void updateChildrenImpl(FacesContext context) {
        Iterator<UIComponent> kids = getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = kids.next();
            kid.processUpdates(context);
        }
    }

    @Override
    protected void addFacesListener(FacesListener listener) {
        if (listener == null) throw new NullPointerException();
        getFacesBean().addEntry(_LISTENERS_KEY, listener);
    }

    @Override
    protected void removeFacesListener(FacesListener listener) {
        if (listener == null) throw new NullPointerException();
        getFacesBean().removeEntry(_LISTENERS_KEY, listener);
    }

    @Override
    protected FacesListener[] getFacesListeners(Class clazz) {
        if (clazz == null) throw new NullPointerException();
        if (!FacesListener.class.isAssignableFrom(clazz)) throw new IllegalArgumentException();
        return (FacesListener[]) getFacesBean().getEntries(_LISTENERS_KEY, clazz);
    }

    protected void addAttributeChange(String attributeName, Object attributeValue) {
        AttributeComponentChange aa = new AttributeComponentChange(attributeName, attributeValue);
        RequestContext adfContext = RequestContext.getCurrentInstance();
        adfContext.getChangeManager().addComponentChange(getFacesContext(), this, aa);
    }

    void __rendererDecode(FacesContext context) {
        _cacheRenderer(context);
        Renderer renderer = getRenderer(context);
        if (renderer != null) {
            renderer.decode(context, this);
        }
    }

    private void _cacheRenderer(FacesContext context) {
        Renderer renderer = _getRendererImpl(context);
        _cachedRenderer = renderer;
        if (renderer instanceof LifecycleRenderer) {
            _cachedLifecycleRenderer = (LifecycleRenderer) renderer;
        } else {
            _cachedLifecycleRenderer = null;
        }
    }

    private Renderer _getRendererImpl(FacesContext context) {
        String rendererType = getRendererType();
        if (rendererType != null) {
            RenderKit renderKit = context.getRenderKit();
            Renderer renderer = renderKit.getRenderer(getFamily(), rendererType);
            if (renderer == null) {
                _LOG.warning("CANNOT_FIND_RENDERER", new Object[] { this, rendererType });
            }
            return renderer;
        }
        return null;
    }

    private LifecycleRenderer _getLifecycleRendererImpl(FacesContext context) {
        Renderer renderer = _getRendererImpl(context);
        if (renderer instanceof LifecycleRenderer) {
            return (LifecycleRenderer) renderer;
        }
        return null;
    }

    @Override
    protected Renderer getRenderer(FacesContext context) {
        Renderer renderer = _cachedRenderer;
        if (renderer != _UNDEFINED_RENDERER) return renderer;
        return _getRendererImpl(context);
    }

    protected LifecycleRenderer getLifecycleRenderer(FacesContext context) {
        LifecycleRenderer renderer = _cachedLifecycleRenderer;
        if (renderer != _UNDEFINED_LIFECYCLE_RENDERER) return renderer;
        return _getLifecycleRendererImpl(context);
    }

    protected void setProperty(PropertyKey key, Object value) {
        getFacesBean().setProperty(key, value);
    }

    protected Object getProperty(PropertyKey key) {
        return getFacesBean().getProperty(key);
    }

    protected void setBooleanProperty(PropertyKey key, boolean value) {
        getFacesBean().setProperty(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    protected boolean getBooleanProperty(PropertyKey key, boolean defaultValue) {
        Object o = getFacesBean().getProperty(key);
        if (defaultValue) return !Boolean.FALSE.equals(o); else return Boolean.TRUE.equals(o);
    }

    protected void setIntProperty(PropertyKey key, int value) {
        getFacesBean().setProperty(key, Integer.valueOf(value));
    }

    protected int getIntProperty(PropertyKey key, int defaultValue) {
        Number n = (Number) getFacesBean().getProperty(key);
        if (n == null) return defaultValue;
        return n.intValue();
    }

    /**
   * Return the number of facets.  This is more efficient than
   * calling getFacets().size();
   */
    @Override
    public int getFacetCount() {
        if (_facets == null) return 0;
        return _facets.size();
    }

    /**
   * Broadcast an event to a MethodBinding.
   * This can be used to support MethodBindings such as the "actionListener"
   * binding on ActionSource components:
   * &lt;tr:commandButton actionListener="#{mybean.myActionListener}">
   * @deprecated
   */
    protected final void broadcastToMethodBinding(FacesEvent event, MethodBinding method) throws AbortProcessingException {
        if (method != null) {
            try {
                FacesContext context = getFacesContext();
                method.invoke(context, new Object[] { event });
            } catch (EvaluationException ee) {
                Throwable t = ee.getCause();
                if (t instanceof AbortProcessingException) throw ((AbortProcessingException) t);
                throw ee;
            }
        }
    }

    /**
   * Given a MethodBinding, create a MethodExpression that
   * adapts it.
   */
    public static MethodExpression adaptMethodBinding(MethodBinding binding) {
        return new MethodBindingMethodExpression(binding);
    }

    /**
   * Broadcast an event to a MethodExpression.
   * This can be used to support MethodBindings such as the "actionListener"
   * binding on ActionSource components:
   * &lt;tr:commandButton actionListener="#{mybean.myActionListener}">
   */
    protected final void broadcastToMethodExpression(FacesEvent event, MethodExpression method) throws AbortProcessingException {
        if (method != null) {
            try {
                FacesContext context = getFacesContext();
                method.invoke(context.getELContext(), new Object[] { event });
            } catch (ELException ee) {
                Throwable t = ee.getCause();
                if (t instanceof AbortProcessingException) throw ((AbortProcessingException) t);
                throw ee;
            }
        }
    }

    /**
   * <p>
   * This gets a single threadlocal shared stringbuilder instance, each time you call
   * __getSharedStringBuilder it sets the length of the stringBuilder instance to 0.
   * </p><p>
   * This allows you to use the same StringBuilder instance over and over.
   * You must call toString on the instance before calling __getSharedStringBuilder again.
   * </p>
   * Example that works
   * <pre><code>
   * StringBuilder sb1 = __getSharedStringBuilder();
   * sb1.append(a).append(b);
   * String c = sb1.toString();
   *
   * StringBuilder sb2 = __getSharedStringBuilder();
   * sb2.append(b).append(a);
   * String d = sb2.toString();
   * </code></pre>
   * <br><br>
   * Example that doesn't work, you must call toString on sb1 before
   * calling __getSharedStringBuilder again.
   * <pre><code>
   * StringBuilder sb1 = __getSharedStringBuilder();
   * StringBuilder sb2 = __getSharedStringBuilder();
   *
   * sb1.append(a).append(b);
   * String c = sb1.toString();
   *
   * sb2.append(b).append(a);
   * String d = sb2.toString();
   * </code></pre>
   *
   */
    static StringBuilder __getSharedStringBuilder() {
        StringBuilder sb = _STRING_BUILDER.get();
        if (sb == null) {
            sb = new StringBuilder();
            _STRING_BUILDER.set(sb);
        }
        sb.setLength(0);
        return sb;
    }

    /**
   * render a component. this is called by renderers whose
   * getRendersChildren() return true.
   */
    void __encodeRecursive(FacesContext context, UIComponent component) throws IOException {
        if (component.isRendered()) {
            component.encodeBegin(context);
            if (component.getRendersChildren()) {
                component.encodeChildren(context);
            } else {
                if (component.getChildCount() > 0) {
                    for (UIComponent child : component.getChildren()) {
                        __encodeRecursive(context, child);
                    }
                }
            }
            component.encodeEnd(context);
        }
    }

    private static UIComponent _findInsideOf(UIComponent from, String id) {
        Iterator<UIComponent> kids = from.getFacetsAndChildren();
        while (kids.hasNext()) {
            UIComponent kid = kids.next();
            if (id.equals(kid.getId())) return kid;
            if (!(kid instanceof NamingContainer)) {
                UIComponent returned = _findInsideOf(kid, id);
                if (returned != null) return returned;
            }
        }
        return null;
    }

    /**
   * <p>Verify that the specified component id is safe to add to the tree.
   * </p>
   *
   * @param id The proposed component id to check for validity
   *
   * @exception IllegalArgumentException if <code>id</code>
   *  is <code>null</code> or contains invalid characters
   */
    private void _validateId(String id) {
        if (id == null) return;
        int n = id.length();
        if (0 == n || NamingContainer.SEPARATOR_CHAR == id.charAt(0)) _throwBadId(id);
        for (int i = 0; i < n; i++) {
            char c = id.charAt(i);
            if (i == 0) {
                if (!Character.isLetter(c) && (c != '_')) _throwBadId(id);
            } else {
                if (!(Character.isLetter(c) || Character.isDigit(c) || (c == '-') || (c == '_'))) {
                    _throwBadId(id);
                }
            }
        }
    }

    private void _throwBadId(String id) {
        throw new IllegalArgumentException(_LOG.getMessage("ILLEGAL_ID", id));
    }

    private void _init(String rendererType) {
        FacesBean oldBean = _facesBean;
        _facesBean = createFacesBean(rendererType);
        if (oldBean != null) _facesBean.addAll(oldBean);
        _attributes = new ValueMap(_facesBean);
    }

    private FacesBean _facesBean;

    private List<UIComponent> _children;

    private Map<String, Object> _attributes;

    private Map<String, UIComponent> _facets;

    private UIComponent _parent;

    private transient Renderer _cachedRenderer = _UNDEFINED_RENDERER;

    private transient LifecycleRenderer _cachedLifecycleRenderer = _UNDEFINED_LIFECYCLE_RENDERER;

    private static final Iterator<String> _EMPTY_STRING_ITERATOR = new EmptyIterator<String>();

    private static final Iterator<UIComponent> _EMPTY_UICOMPONENT_ITERATOR = new EmptyIterator<UIComponent>();

    private static final ThreadLocal<StringBuilder> _STRING_BUILDER = ThreadLocalUtils.newRequestThreadLocal();

    private static FacesBean.Type _createType() {
        try {
            ClassLoader cl = _getClassLoader();
            URL url = cl.getResource("META-INF/faces-bean-type.properties");
            if (url != null) {
                Properties properties = new Properties();
                InputStream is = url.openStream();
                try {
                    properties.load(is);
                    String className = (String) properties.get(UIXComponentBase.class.getName());
                    return (FacesBean.Type) cl.loadClass(className).newInstance();
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            _LOG.severe("CANNOT_LOAD_TYPE_PROPERTIES", e);
        }
        return new FacesBean.Type();
    }

    private static ClassLoader _getClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = FacesBeanFactory.class.getClassLoader();
        return loader;
    }

    private static class RendererImpl extends Renderer {
    }

    private static class ExtendedRendererImpl extends ExtendedRenderer {
    }

    private static class EmptyIterator<T> implements Iterator<T> {

        public boolean hasNext() {
            return false;
        }

        public T next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static final LifecycleRenderer _UNDEFINED_LIFECYCLE_RENDERER = new ExtendedRendererImpl();

    private static final Renderer _UNDEFINED_RENDERER = new RendererImpl();
}
