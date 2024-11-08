package com.kyte.api.model;

import com.kyte.api.rest.KyteSession;
import com.kyte.api.util.ApiUtil;
import com.kyte.api.util.StructHelper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static com.kyte.api.rest.KyteInvoker.*;

/**
 *  Trackback resource
 *
 */
public class Trackback implements ApiModel {

    public static final String ATTRIBUTE_CHANNEL_URI = "channelUri";

    public static final String ATTRIBUTE_LAST_VISIT = "lastVisit";

    public static final String ATTRIBUTE_URI = "uri";

    public static final String ATTRIBUTE_URL = "url";

    protected String channelUri;

    protected Date lastVisit;

    protected String uri;

    protected String url;

    public static final String PARAM_DATA = "data";

    /**
     *  Default constructor
     */
    public Trackback() {
    }

    /**
     *  constructs from a Map
     */
    public Trackback(Map<String, Object> map) {
        channelUri = (String) map.get(ATTRIBUTE_CHANNEL_URI);
        lastVisit = StructHelper.getDateObjectAttribute(map, ATTRIBUTE_LAST_VISIT);
        uri = (String) map.get(ATTRIBUTE_URI);
        url = (String) map.get(ATTRIBUTE_URL);
    }

    static final String[] _downNames = { ATTRIBUTE_CHANNEL_URI, ATTRIBUTE_LAST_VISIT, ATTRIBUTE_URI, ATTRIBUTE_URL };

    /**
     * @return member names for serialization from server
     */
    public String[] _downMemberNames() {
        return _downNames;
    }

    static final String[] _upNames = {};

    /**
     * @return member names for serialization to server
     */
    public String[] _upMemberNames() {
        return _upNames;
    }

    public void update(KyteSession session, Map<java.lang.String, java.lang.Object> data) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_DATA, data);
        session.invoke(getUri(), "update", _params);
    }

    public String getChannelUri() {
        return channelUri;
    }

    public Date getLastVisit() {
        return lastVisit;
    }

    public String getUri() {
        return uri;
    }

    public String getUrl() {
        return url;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Trackback [");
        sb.append("uri").append("=").append(uri);
        sb.append(", channelUri=").append(channelUri);
        sb.append(", lastVisit=").append(lastVisit);
        sb.append(", url=").append(url);
        sb.append("]");
        return sb.toString();
    }
}
