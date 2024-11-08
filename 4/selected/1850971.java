package com.liferay.portal.wsrp.consumer.invoker;

import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Portlet;
import com.liferay.portlet.PortletURLImpl;
import com.sun.portal.container.ChannelMode;
import com.sun.portal.container.ChannelState;
import com.sun.portal.container.ChannelURL;
import com.sun.portal.container.ChannelURLType;
import com.sun.portal.portletcontainer.appengine.PortletAppEngineUtils;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.portlet.PortletModeException;
import javax.portlet.PortletRequest;
import javax.portlet.WindowStateException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="WSRPWindowChannelURL.java.html"><b><i>View Source</i></b></a>
 *
 * @author Manish Gupta
 *
 */
public class WSRPWindowChannelURL implements ChannelURL, Serializable {

    public static final String NEW_CHANNEL_MODE = "dt.window.newChannelMode";

    public static final String NEW_WINDOW_STATE = "dt.window.newWindowState";

    public static final String PORTLET_ACTION = "dt.window.portletAction";

    public static final String RESOURCE_CACHE_LEVEL = "wsrp-resourceCacheability";

    public static final String RESOURCE_ID = "wsrp-resourceID";

    public static final String RESOURCE_STATE = "wsrp-resourceState";

    public WSRPWindowChannelURL(HttpServletRequest request, Portlet portlet, ChannelState windowState, ChannelMode portletMode, long plid) {
        _portletURLImpl = new PortletURLImpl(request, portlet.getPortletId(), plid, PortletRequest.RENDER_PHASE);
        setWindowState(windowState);
        setChannelMode(portletMode);
    }

    public void addProperty(String name, String value) {
    }

    public String getCacheLevel() {
        return _portletURLImpl.getCacheability();
    }

    public ChannelMode getChannelMode() {
        return PortletAppEngineUtils.getChannelMode(_portletURLImpl.getPortletMode());
    }

    public Map<String, String[]> getParameters() {
        return _portletURLImpl.getParameterMap();
    }

    public Map<String, List<String>> getProperties() {
        return Collections.EMPTY_MAP;
    }

    public ChannelURLType getURLType() {
        return _urlType;
    }

    public ChannelState getWindowState() {
        return PortletAppEngineUtils.getChannelState(_portletURLImpl.getWindowState());
    }

    public boolean isSecure() {
        return _portletURLImpl.isSecure();
    }

    public void setCacheLevel(String cacheLevel) {
        _portletURLImpl.setCacheability(cacheLevel);
    }

    public void setChannelMode(ChannelMode portletMode) {
        try {
            _portletURLImpl.setPortletMode(PortletAppEngineUtils.getPortletMode(portletMode));
        } catch (PortletModeException pme) {
            _log.error(pme, pme);
        }
    }

    public void setParameter(String name, String value) {
        _portletURLImpl.setParameter(name, value);
    }

    public void setParameter(String name, String[] values) {
        _portletURLImpl.setParameter(name, values);
    }

    public void setParameters(Map<String, String[]> parametersMap) {
        _portletURLImpl.setParameters(parametersMap);
    }

    public void setProperty(String name, String value) {
    }

    public void setResourceID(String resourceID) {
        _portletURLImpl.setResourceID(resourceID);
    }

    public void setSecure(boolean secure) {
        _portletURLImpl.setSecure(secure);
    }

    public void setURLType(ChannelURLType urlType) {
        _urlType = urlType;
        _portletURLImpl.setLifecycle(getLifecycle());
        _portletURLImpl.setURLType(getURLType());
    }

    public void setWindowState(ChannelState windowState) {
        try {
            _portletURLImpl.setWindowState(PortletAppEngineUtils.getWindowState(windowState));
        } catch (WindowStateException wse) {
            _log.error(wse, wse);
        }
    }

    public String toString() {
        return _portletURLImpl.toString();
    }

    protected String getLifecycle() {
        if (ChannelURLType.ACTION.equals(getURLType())) {
            return PortletRequest.ACTION_PHASE;
        } else if (ChannelURLType.RENDER.equals(getURLType())) {
            return PortletRequest.ACTION_PHASE;
        } else if (ChannelURLType.RESOURCE.equals(getURLType())) {
            return PortletRequest.RESOURCE_PHASE;
        } else {
            return PortletRequest.RENDER_PHASE;
        }
    }

    protected String getTemplate() {
        StringBuffer sb = new StringBuffer();
        sb.append(_portletURLImpl.toString());
        sb.append(StringPool.AMPERSAND);
        sb.append(PORTLET_ACTION);
        sb.append(StringPool.EQUAL);
        sb.append("{wsrp-urlType}");
        sb.append(StringPool.AMPERSAND);
        sb.append(NEW_WINDOW_STATE);
        sb.append(StringPool.EQUAL);
        sb.append("{wsrp-windowState}");
        sb.append(StringPool.AMPERSAND);
        sb.append(NEW_CHANNEL_MODE);
        sb.append(StringPool.EQUAL);
        sb.append("{wsrp-mode}");
        sb.append("&wsrp-navigationalState={wsrp-navigationalState}");
        if (ChannelURLType.ACTION.equals(_urlType)) {
            sb.append("&wsrp-interactionState={wsrp-interactionState}");
        } else if (ChannelURLType.RESOURCE.equals(_urlType)) {
            sb.append(StringPool.AMPERSAND);
            sb.append(RESOURCE_ID);
            sb.append(StringPool.EQUAL);
            sb.append("{wsrp-resourceID}");
            sb.append(StringPool.AMPERSAND);
            sb.append(RESOURCE_STATE);
            sb.append(StringPool.EQUAL);
            sb.append("{wsrp-resourceState}");
            sb.append(StringPool.AMPERSAND);
            sb.append(RESOURCE_CACHE_LEVEL);
            sb.append(StringPool.EQUAL);
            sb.append("{wsrp-resourceCacheability}");
        }
        return sb.toString();
    }

    private static Log _log = LogFactory.getLog(WSRPWindowChannelURL.class);

    private PortletURLImpl _portletURLImpl;

    private ChannelURLType _urlType;
}
