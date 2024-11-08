package org.granite.config.flex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.granite.messaging.service.security.DestinationSecurizer;
import org.granite.util.ClassUtil;
import org.granite.util.XMap;

/**
 * @author Franck WOLFF
 */
public class Destination implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SECURIZER_PROPERTY_KEY = "securizer";

    private final String id;

    private final List<String> channelRefs;

    private final XMap properties;

    private final List<String> roles;

    private final Adapter adapter;

    private final Class<?> scannedClass;

    private final DestinationSecurizer securizer;

    private DestinationRemoveListener removeListener;

    public Destination(String id, List<String> channelRefs, XMap properties, List<String> roles, Adapter adapter, Class<?> scannedClass) {
        this.id = id;
        this.channelRefs = new ArrayList<String>(channelRefs);
        this.properties = properties;
        this.roles = (roles != null ? new ArrayList<String>(roles) : null);
        this.adapter = adapter;
        this.scannedClass = scannedClass;
        final String securizerClassName = properties.get(SECURIZER_PROPERTY_KEY);
        if (securizerClassName != null) {
            try {
                this.securizer = ClassUtil.newInstance(securizerClassName.trim(), DestinationSecurizer.class);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate securizer: " + securizerClassName, e);
            }
        } else this.securizer = null;
    }

    public void addRemoveListener(DestinationRemoveListener listener) {
        this.removeListener = listener;
    }

    public void remove() {
        if (removeListener != null) removeListener.destinationRemoved(this);
    }

    public String getId() {
        return id;
    }

    public List<String> getChannelRefs() {
        return channelRefs;
    }

    public XMap getProperties() {
        return properties;
    }

    public boolean isSecured() {
        return roles != null;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public Class<?> getScannedClass() {
        return scannedClass;
    }

    public DestinationSecurizer getSecurizer() {
        return securizer;
    }

    public static Destination forElement(XMap element, Adapter defaultAdapter, Map<String, Adapter> adaptersMap) {
        String id = element.get("@id");
        List<String> channelRefs = new ArrayList<String>();
        for (XMap channel : element.getAll("channels/channel[@ref]")) channelRefs.add(channel.get("@ref"));
        XMap properties = new XMap(element.getOne("properties"));
        List<String> rolesList = null;
        if (element.containsKey("security/security-constraint/roles/role")) {
            rolesList = new ArrayList<String>();
            for (XMap role : element.getAll("security/security-constraint/roles/role")) rolesList.add(role.get("."));
        }
        XMap adapter = element.getOne("adapter[@ref]");
        Adapter adapterRef = adapter != null && adaptersMap != null ? adaptersMap.get(adapter.get("@ref")) : defaultAdapter;
        return new Destination(id, channelRefs, properties, rolesList, adapterRef, null);
    }
}
