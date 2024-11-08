package com.kyte.api.model;

import com.kyte.api.rest.KyteSession;
import com.kyte.api.util.ApiUtil;
import com.kyte.api.util.StructHelper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static com.kyte.api.rest.KyteInvoker.*;
import java.io.InputStream;
import com.kyte.api.service.ServiceFactory;

/**
 *  Channel resource
 *
 */
public class Channel implements ApiModel {

    public static final String ATTRIBUTE_AD_DATA = "adData";

    public static final String ATTRIBUTE_ALIAS = "alias";

    public static final String ATTRIBUTE_ALL_SHOWS_RATE_COUNT = "allShowsRateCount";

    public static final String ATTRIBUTE_ALL_SHOWS_RATE_SUM = "allShowsRateSum";

    public static final String ATTRIBUTE_ANY_PRODUCER_ALLOWED = "anyProducerAllowed";

    public static final String ATTRIBUTE_BACKGROUND_COLOR = "backgroundColor";

    public static final String ATTRIBUTE_BACKGROUND_MEDIA_URI = "backgroundMediaUri";

    public static final String ATTRIBUTE_CHANNEL_HOME_URL = "channelHomeUrl";

    public static final String ATTRIBUTE_CHAT_THREAD_URI = "chatThreadUri";

    public static final String ATTRIBUTE_CREATED_TIME = "createdTime";

    public static final String ATTRIBUTE_CUSTOM_DATA = "customData";

    public static final String ATTRIBUTE_LAST_MESSAGE_TIME = "lastMessageTime";

    public static final String ATTRIBUTE_LAST_SHOW_TIME = "lastShowTime";

    public static final String ATTRIBUTE_LOGO_MEDIA_URI = "logoMediaUri";

    public static final String ATTRIBUTE_MODIFIED_TIME = "modifiedTime";

    public static final String ATTRIBUTE_OWNER_NAME = "ownerName";

    public static final String ATTRIBUTE_OWNER_URI = "ownerUri";

    public static final String ATTRIBUTE_PERMALINK = "permalink";

    public static final String ATTRIBUTE_PRODUCER_MODERATION_ENABLED = "producerModerationEnabled";

    public static final String ATTRIBUTE_PRODUCER_PASS_CODE = "producerPassCode";

    public static final String ATTRIBUTE_PRODUCTION_EMAIL = "productionEmail";

    public static final String ATTRIBUTE_PROFANITY_FILTER_ENABLED = "profanityFilterEnabled";

    public static final String ATTRIBUTE_SHOW_COUNT = "showCount";

    public static final String ATTRIBUTE_SYNOPSIS = "synopsis";

    public static final String ATTRIBUTE_TAGS = "tags";

    public static final String ATTRIBUTE_THUMB_URL120X90 = "thumbUrl120x90";

    public static final String ATTRIBUTE_TITLE = "title";

    public static final String ATTRIBUTE_TOTAL_MESSAGE_COUNT = "totalMessageCount";

    public static final String ATTRIBUTE_TOTAL_WATCHES = "totalWatches";

    public static final String ATTRIBUTE_URI = "uri";

    public static final String ATTRIBUTE_VIEWER_COUNT = "viewerCount";

    protected String adData;

    protected String alias;

    protected int allShowsRateCount;

    protected int allShowsRateSum;

    protected boolean anyProducerAllowed;

    protected Long backgroundColor;

    protected String backgroundMediaUri;

    protected String channelHomeUrl;

    protected String chatThreadUri;

    protected Date createdTime;

    protected String customData;

    protected Date lastMessageTime;

    protected Date lastShowTime;

    protected String logoMediaUri;

    protected Date modifiedTime;

    protected String ownerName;

    protected String ownerUri;

    protected String permalink;

    protected boolean producerModerationEnabled;

    protected String producerPassCode;

    protected String productionEmail;

    protected boolean profanityFilterEnabled;

    protected int showCount;

    protected String synopsis;

    protected String tags;

    protected String thumbUrl120x90;

    protected String title;

    protected int totalMessageCount;

    protected int totalWatches;

    protected String uri;

    protected int viewerCount;

    public static final String PARAM_ATTR_NAME = "attrName";

    public static final String PARAM_ATTR_VALUE = "attrValue";

    public static final String PARAM_AUX_DATA = "auxData";

    public static final String PARAM_CAPTION_TEXT = "captionText";

    public static final String PARAM_CHANNEL_DATA = "channelData";

    public static final String PARAM_CRITERIA = "criteria";

    public static final String PARAM_FIRST_RESULT = "firstResult";

    public static final String PARAM_KEYWORD = "keyword";

    public static final String PARAM_MAX_RESULTS = "maxResults";

    public static final String PARAM_MEDIA = "media";

    public static final String PARAM_ORDER_SPEC = "orderSpec";

    public static final String PARAM_PLAYER_URI = "playerUri";

    public static final String PARAM_SHOW_DATA = "showData";

    public static final String PARAM_SOURCE_SHOW_URI = "sourceShowUri";

    public static final String PARAM_TARGET_TICKET = "targetTicket";

    public static final String PARAM_TRACKBACK_ID = "trackbackId";

    /**
     *  Default constructor
     */
    public Channel() {
    }

    /**
     *  constructs from a Map
     */
    public Channel(Map<String, Object> map) {
        adData = (String) map.get(ATTRIBUTE_AD_DATA);
        alias = (String) map.get(ATTRIBUTE_ALIAS);
        allShowsRateCount = StructHelper.getIntAttribute(map, ATTRIBUTE_ALL_SHOWS_RATE_COUNT);
        allShowsRateSum = StructHelper.getIntAttribute(map, ATTRIBUTE_ALL_SHOWS_RATE_SUM);
        anyProducerAllowed = StructHelper.getBooleanAttribute(map, ATTRIBUTE_ANY_PRODUCER_ALLOWED);
        backgroundColor = StructHelper.getLongObjectAttribute(map, ATTRIBUTE_BACKGROUND_COLOR);
        backgroundMediaUri = (String) map.get(ATTRIBUTE_BACKGROUND_MEDIA_URI);
        channelHomeUrl = (String) map.get(ATTRIBUTE_CHANNEL_HOME_URL);
        chatThreadUri = (String) map.get(ATTRIBUTE_CHAT_THREAD_URI);
        createdTime = StructHelper.getDateObjectAttribute(map, ATTRIBUTE_CREATED_TIME);
        customData = (String) map.get(ATTRIBUTE_CUSTOM_DATA);
        lastMessageTime = StructHelper.getDateObjectAttribute(map, ATTRIBUTE_LAST_MESSAGE_TIME);
        lastShowTime = StructHelper.getDateObjectAttribute(map, ATTRIBUTE_LAST_SHOW_TIME);
        logoMediaUri = (String) map.get(ATTRIBUTE_LOGO_MEDIA_URI);
        modifiedTime = StructHelper.getDateObjectAttribute(map, ATTRIBUTE_MODIFIED_TIME);
        ownerName = (String) map.get(ATTRIBUTE_OWNER_NAME);
        ownerUri = (String) map.get(ATTRIBUTE_OWNER_URI);
        permalink = (String) map.get(ATTRIBUTE_PERMALINK);
        producerModerationEnabled = StructHelper.getBooleanAttribute(map, ATTRIBUTE_PRODUCER_MODERATION_ENABLED);
        producerPassCode = (String) map.get(ATTRIBUTE_PRODUCER_PASS_CODE);
        productionEmail = (String) map.get(ATTRIBUTE_PRODUCTION_EMAIL);
        profanityFilterEnabled = StructHelper.getBooleanAttribute(map, ATTRIBUTE_PROFANITY_FILTER_ENABLED);
        showCount = StructHelper.getIntAttribute(map, ATTRIBUTE_SHOW_COUNT);
        synopsis = (String) map.get(ATTRIBUTE_SYNOPSIS);
        tags = (String) map.get(ATTRIBUTE_TAGS);
        thumbUrl120x90 = (String) map.get(ATTRIBUTE_THUMB_URL120X90);
        title = (String) map.get(ATTRIBUTE_TITLE);
        totalMessageCount = StructHelper.getIntAttribute(map, ATTRIBUTE_TOTAL_MESSAGE_COUNT);
        totalWatches = StructHelper.getIntAttribute(map, ATTRIBUTE_TOTAL_WATCHES);
        uri = (String) map.get(ATTRIBUTE_URI);
        viewerCount = StructHelper.getIntAttribute(map, ATTRIBUTE_VIEWER_COUNT);
    }

    static final String[] _downNames = { ATTRIBUTE_AD_DATA, ATTRIBUTE_ALIAS, ATTRIBUTE_ALL_SHOWS_RATE_COUNT, ATTRIBUTE_ALL_SHOWS_RATE_SUM, ATTRIBUTE_ANY_PRODUCER_ALLOWED, ATTRIBUTE_BACKGROUND_COLOR, ATTRIBUTE_BACKGROUND_MEDIA_URI, ATTRIBUTE_CHANNEL_HOME_URL, ATTRIBUTE_CHAT_THREAD_URI, ATTRIBUTE_CREATED_TIME, ATTRIBUTE_CUSTOM_DATA, ATTRIBUTE_LAST_MESSAGE_TIME, ATTRIBUTE_LAST_SHOW_TIME, ATTRIBUTE_LOGO_MEDIA_URI, ATTRIBUTE_MODIFIED_TIME, ATTRIBUTE_OWNER_NAME, ATTRIBUTE_OWNER_URI, ATTRIBUTE_PERMALINK, ATTRIBUTE_PRODUCER_MODERATION_ENABLED, ATTRIBUTE_PRODUCER_PASS_CODE, ATTRIBUTE_PRODUCTION_EMAIL, ATTRIBUTE_PROFANITY_FILTER_ENABLED, ATTRIBUTE_SHOW_COUNT, ATTRIBUTE_SYNOPSIS, ATTRIBUTE_TAGS, ATTRIBUTE_THUMB_URL120X90, ATTRIBUTE_TITLE, ATTRIBUTE_TOTAL_MESSAGE_COUNT, ATTRIBUTE_TOTAL_WATCHES, ATTRIBUTE_URI, ATTRIBUTE_VIEWER_COUNT };

    /**
     * @return member names for serialization from server
     */
    public String[] _downMemberNames() {
        return _downNames;
    }

    static final String[] _upNames = { ATTRIBUTE_AD_DATA, ATTRIBUTE_ALIAS, ATTRIBUTE_ANY_PRODUCER_ALLOWED, ATTRIBUTE_BACKGROUND_COLOR, ATTRIBUTE_BACKGROUND_MEDIA_URI, ATTRIBUTE_CHANNEL_HOME_URL, ATTRIBUTE_CREATED_TIME, ATTRIBUTE_CUSTOM_DATA, ATTRIBUTE_LOGO_MEDIA_URI, ATTRIBUTE_PRODUCER_MODERATION_ENABLED, ATTRIBUTE_PRODUCER_PASS_CODE, ATTRIBUTE_PRODUCTION_EMAIL, ATTRIBUTE_PROFANITY_FILTER_ENABLED, ATTRIBUTE_SYNOPSIS, ATTRIBUTE_TAGS, ATTRIBUTE_TITLE };

    /**
     * @return member names for serialization to server
     */
    public String[] _upMemberNames() {
        return _upNames;
    }

    public Page<String> browseSubscriberUris(KyteSession session, int firstResult, int maxResults) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_FIRST_RESULT, firstResult);
        _params.put(PARAM_MAX_RESULTS, maxResults);
        Object _obj = session.invoke(getUri(), "browseSubscriberUris", _params);
        return (Page<String>) _obj;
    }

    public String copyShow(KyteSession session, String sourceShowUri, Map auxData) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_SOURCE_SHOW_URI, sourceShowUri);
        _params.put(PARAM_AUX_DATA, auxData);
        Object _obj = session.invoke(getUri(), "copyShow", _params);
        return (String) _obj;
    }

    public void delete(KyteSession session) {
        Map<String, Object> _params = new HashMap<String, Object>();
        session.invoke(getUri(), "delete", _params);
    }

    public String embedCode(KyteSession session, String trackbackId, String playerUri) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_TRACKBACK_ID, trackbackId);
        _params.put(PARAM_PLAYER_URI, playerUri);
        Object _obj = session.invoke(getUri(), "embedCode", _params);
        return (String) _obj;
    }

    public Channel fetchMetaData(KyteSession session) {
        Map<String, Object> _params = new HashMap<String, Object>();
        Object _obj = session.invoke(getUri(), "fetchMetaData", _params);
        return (Channel) _obj;
    }

    public Page<Show> fetchShows(KyteSession session, int firstResult, int maxResults) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_FIRST_RESULT, firstResult);
        _params.put(PARAM_MAX_RESULTS, maxResults);
        Object _obj = session.invoke(getUri(), "fetchShows", _params);
        return (Page<Show>) _obj;
    }

    public Page<Show> findShows(KyteSession session, Map<java.lang.String, java.lang.Object> criteria, int firstResult, int maxResults) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_CRITERIA, criteria);
        _params.put(PARAM_FIRST_RESULT, firstResult);
        _params.put(PARAM_MAX_RESULTS, maxResults);
        Object _obj = session.invoke(getUri(), "findShows", _params);
        return (Page<Show>) _obj;
    }

    public String nextTrackbackId(KyteSession session) {
        Map<String, Object> _params = new HashMap<String, Object>();
        Object _obj = session.invoke(getUri(), "nextTrackbackId", _params);
        return (String) _obj;
    }

    public String postShow(KyteSession session, Object showData, String captionText, Object media) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_SHOW_DATA, showData);
        _params.put(PARAM_CAPTION_TEXT, captionText);
        _params.put(PARAM_MEDIA, media);
        Object _obj = session.invoke(getUri(), "postShow", _params);
        return (String) _obj;
    }

    public Object putAttribute(KyteSession session, String attrName, Object attrValue) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_ATTR_NAME, attrName);
        _params.put(PARAM_ATTR_VALUE, attrValue);
        Object _obj = session.invoke(getUri(), "putAttribute", _params);
        return (Object) _obj;
    }

    public Object removeAttribute(KyteSession session, String attrName) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_ATTR_NAME, attrName);
        Object _obj = session.invoke(getUri(), "removeAttribute", _params);
        return (Object) _obj;
    }

    public Object resolveAttribute(KyteSession session, String attrName) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_ATTR_NAME, attrName);
        Object _obj = session.invoke(getUri(), "resolveAttribute", _params);
        return (Object) _obj;
    }

    public Page<Show> searchShows(KyteSession session, String keyword, int firstResult, int maxResults, String orderSpec) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_KEYWORD, keyword);
        _params.put(PARAM_FIRST_RESULT, firstResult);
        _params.put(PARAM_MAX_RESULTS, maxResults);
        _params.put(PARAM_ORDER_SPEC, orderSpec);
        Object _obj = session.invoke(getUri(), "searchShows", _params);
        return (Page<Show>) _obj;
    }

    public void transferOwner(KyteSession session, String targetTicket) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_TARGET_TICKET, targetTicket);
        session.invoke(getUri(), "transferOwner", _params);
    }

    public void update(KyteSession session, Map channelData) {
        Map<String, Object> _params = new HashMap<String, Object>();
        _params.put(PARAM_CHANNEL_DATA, channelData);
        session.invoke(getUri(), "update", _params);
    }

    public String getAdData() {
        return adData;
    }

    public void setAdData(String _value) {
        adData = _value;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String _value) {
        alias = _value;
    }

    public int getAllShowsRateCount() {
        return allShowsRateCount;
    }

    public int getAllShowsRateSum() {
        return allShowsRateSum;
    }

    public boolean isAnyProducerAllowed() {
        return anyProducerAllowed;
    }

    public void setAnyProducerAllowed(boolean _value) {
        anyProducerAllowed = _value;
    }

    public Long getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Long _value) {
        backgroundColor = _value;
    }

    public String getBackgroundMediaUri() {
        return backgroundMediaUri;
    }

    public void setBackgroundMediaUri(String _value) {
        backgroundMediaUri = _value;
    }

    public String getChannelHomeUrl() {
        return channelHomeUrl;
    }

    public void setChannelHomeUrl(String _value) {
        channelHomeUrl = _value;
    }

    public String getChatThreadUri() {
        return chatThreadUri;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date _value) {
        createdTime = _value;
    }

    public String getCustomData() {
        return customData;
    }

    public void setCustomData(String _value) {
        customData = _value;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public Date getLastShowTime() {
        return lastShowTime;
    }

    public String getLogoMediaUri() {
        return logoMediaUri;
    }

    public void setLogoMediaUri(String _value) {
        logoMediaUri = _value;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerUri() {
        return ownerUri;
    }

    public String getPermalink() {
        return permalink;
    }

    public boolean isProducerModerationEnabled() {
        return producerModerationEnabled;
    }

    public void setProducerModerationEnabled(boolean _value) {
        producerModerationEnabled = _value;
    }

    public String getProducerPassCode() {
        return producerPassCode;
    }

    public void setProducerPassCode(String _value) {
        producerPassCode = _value;
    }

    public String getProductionEmail() {
        return productionEmail;
    }

    public void setProductionEmail(String _value) {
        productionEmail = _value;
    }

    public boolean isProfanityFilterEnabled() {
        return profanityFilterEnabled;
    }

    public void setProfanityFilterEnabled(boolean _value) {
        profanityFilterEnabled = _value;
    }

    public int getShowCount() {
        return showCount;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String _value) {
        synopsis = _value;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String _value) {
        tags = _value;
    }

    public String getThumbUrl120x90() {
        return thumbUrl120x90;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String _value) {
        title = _value;
    }

    public int getTotalMessageCount() {
        return totalMessageCount;
    }

    public int getTotalWatches() {
        return totalWatches;
    }

    public String getUri() {
        return uri;
    }

    public int getViewerCount() {
        return viewerCount;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Channel [");
        sb.append("uri").append("=").append(uri);
        sb.append(", adData=").append(adData);
        sb.append(", alias=").append(alias);
        sb.append(", allShowsRateCount=").append(allShowsRateCount);
        sb.append(", allShowsRateSum=").append(allShowsRateSum);
        sb.append(", anyProducerAllowed=").append(anyProducerAllowed);
        sb.append(", backgroundColor=").append(backgroundColor);
        sb.append(", backgroundMediaUri=").append(backgroundMediaUri);
        sb.append(", channelHomeUrl=").append(channelHomeUrl);
        sb.append(", chatThreadUri=").append(chatThreadUri);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", customData=").append(customData);
        sb.append(", lastMessageTime=").append(lastMessageTime);
        sb.append(", lastShowTime=").append(lastShowTime);
        sb.append(", logoMediaUri=").append(logoMediaUri);
        sb.append(", modifiedTime=").append(modifiedTime);
        sb.append(", ownerName=").append(ownerName);
        sb.append(", ownerUri=").append(ownerUri);
        sb.append(", permalink=").append(permalink);
        sb.append(", producerModerationEnabled=").append(producerModerationEnabled);
        sb.append(", producerPassCode=").append(producerPassCode);
        sb.append(", productionEmail=").append(productionEmail);
        sb.append(", profanityFilterEnabled=").append(profanityFilterEnabled);
        sb.append(", showCount=").append(showCount);
        sb.append(", synopsis=").append(synopsis);
        sb.append(", tags=").append(tags);
        sb.append(", thumbUrl120x90=").append(thumbUrl120x90);
        sb.append(", title=").append(title);
        sb.append(", totalMessageCount=").append(totalMessageCount);
        sb.append(", totalWatches=").append(totalWatches);
        sb.append(", viewerCount=").append(viewerCount);
        sb.append("]");
        return sb.toString();
    }

    public String postShow(KyteSession session, Object showData, InputStream inputStream, long dataSize, String mediaType) {
        Media media = ServiceFactory.getMediaService().createMedia(session, inputStream, dataSize, "image/jpg");
        if (media != null) {
            return postShow(session, showData, "", media.getUri());
        }
        return null;
    }
}
