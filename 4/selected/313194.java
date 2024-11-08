package org.wwweeeportal.portal.channels;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.activation.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.converter.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.io.*;
import org.wwweeeportal.util.security.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * A {@link Channel} which provides content stored directly within it's {@linkplain ConfigManager configuration
 * properties}.
 * </p>
 * 
 * <p>
 * This channel can display both {@linkplain Channel#VIEW_MODE view mode} {@linkplain #VIEW_CONTENT_VALUE_BY_PATH_PROP
 * content} and {@linkplain Channel#RESOURCE_MODE resource mode} {@linkplain #RESOURCE_CONTENT_VALUE_BY_PATH_PROP
 * content} from {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter properties using regular
 * expressions} which are
 * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
 * matched} against the requested {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) channel
 * local path}.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * In addition to those inherited from the {@link Channel} class, the following {@linkplain ConfigManager configuration
 * properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #VIEW_CONTENT_VALUE_BY_PATH_PROP}</li>
 * <li>{@link #VIEW_CONTENT_TYPE_BY_PATH_PROP}</li>
 * <li>{@link #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP}</li>
 * <li>{@link #VIEW_CONTENT_MAX_AGE_BY_PATH_PROP}</li>
 * <li>{@link #VIEW_CONTENT_ETAG_BY_PATH_PROP}</li>
 * <li>{@link #RESOURCE_CONTENT_VALUE_BY_PATH_PROP}</li>
 * <li>{@link #RESOURCE_CONTENT_TYPE_BY_PATH_PROP}</li>
 * <li>{@link #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP}</li>
 * <li>{@link #RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP}</li>
 * <li>{@link #RESOURCE_CONTENT_ETAG_BY_PATH_PROP}</li>
 * </ul>
 */
public class StaticChannel extends Channel {

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to property values, each of which is an {@linkplain MarkupManager#OBJECT_INPUT_SOURCE_CONVERTER input source} which
   * will be {@linkplain DOMUtil#INPUT_SOURCE_DOCUMENT_CONVERTER parsed} into the {@linkplain Document XML} to be
   * {@linkplain DOMUtil#appendChild(Node, Node) included} as the {@linkplain Channel#VIEW_MODE view}
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement() content} for this channel
   * whenever the {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see MarkupManager#OBJECT_INPUT_SOURCE_CONVERTER
   * @see DOMUtil#INPUT_SOURCE_DOCUMENT_CONVERTER
   * @see #getViewContentValue(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String VIEW_CONTENT_VALUE_BY_PATH_PROP = "StaticChannel.View.Content.Value.Path.";

    /**
   * @see #VIEW_CONTENT_VALUE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getViewContentValue(Page.Request, String)
   */
    protected static final Pattern VIEW_CONTENT_VALUE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_CONTENT_VALUE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will extract it's {@linkplain RSProperties#RESULT_VALUE_CONVERTER value}, use it to create an
   * {@linkplain MarkupManager#OBJECT_INPUT_SOURCE_CONVERTER input source}, and
   * {@linkplain DOMUtil#INPUT_SOURCE_DOCUMENT_CONVERTER parse} that into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Document}&gt;</code>.
   * 
   * @see org.wwweeeportal.util.ws.rs.RSProperties.ResultEntryConverter
   * @see RSProperties#RESULT_VALUE_CONVERTER
   * @see MarkupManager#OBJECT_INPUT_SOURCE_CONVERTER
   * @see DOMUtil#INPUT_SOURCE_DOCUMENT_CONVERTER
   */
    protected static final Converter<RSProperties.Result, RSProperties.Entry<Document>> ENTRY_DOCUMENT_ENTRY_CONVERTER = new RSProperties.ResultEntryConverter<Document>(new ConverterChain<RSProperties.Result, Object, Document>(RSProperties.RESULT_VALUE_CONVERTER, new CastingConverter<Object, Document>(Document.class, new ConverterChain<Object, InputSource, Document>(MarkupManager.OBJECT_INPUT_SOURCE_CONVERTER, DOMUtil.INPUT_SOURCE_DOCUMENT_CONVERTER))));

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain #ENTRY_DOCUMENT_ENTRY_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Document}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #VIEW_CONTENT_VALUE_BY_PATH_PROP
   * @see #ENTRY_DOCUMENT_ENTRY_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getViewContentValue(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Document>, Map.Entry<String, Pattern>>> VIEW_CONTENT_VALUE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Document>, Map.Entry<RSProperties.Entry<Document>, Map.Entry<String, Pattern>>>(ENTRY_DOCUMENT_ENTRY_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Document>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Document>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER MediaType} property values which will be
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#setContentType(MediaType) set} on a
   * {@link org.wwweeeportal.portal.Channel.ViewResponse ViewResponse} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER
   * @see #getViewContentType(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String VIEW_CONTENT_TYPE_BY_PATH_PROP = "StaticChannel.View.Content.Type.Path.";

    /**
   * @see #VIEW_CONTENT_TYPE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getViewContentType(Page.Request, String)
   */
    protected static final Pattern VIEW_CONTENT_TYPE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_CONTENT_TYPE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link MediaType}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #VIEW_CONTENT_TYPE_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getViewContentType(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>> VIEW_CONTENT_TYPE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<MediaType>, Map.Entry<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_MEDIA_TYPE_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<MediaType>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER Date} property values which will be
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#setLastModified(Date) set} on a
   * {@link org.wwweeeportal.portal.Channel.ViewResponse ViewResponse} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER
   * @see #getViewContentLastModified(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP = "StaticChannel.View.Content.LastModified.Path.";

    /**
   * @see #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getViewContentLastModified(Page.Request, String)
   */
    protected static final Pattern VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Date}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getViewContentLastModified(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>> VIEW_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Date>, Map.Entry<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>>(new ConverterChain<RSProperties.Result, RSProperties.Entry<Calendar>, RSProperties.Entry<Date>>(RSProperties.RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER, new RSProperties.EntryValueTypeConverter<Calendar, Date>(DateUtil.CALENDAR_DATE_CONVERTER)), new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Date>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_INTEGER_CONVERTER Integer} property values which will be
   * {@linkplain CacheControl#setMaxAge(int) set} on the
   * {@link org.wwweeeportal.portal.Channel.ViewResponse#getCacheControl() CacheControl} for a
   * {@link org.wwweeeportal.portal.Channel.ViewResponse ViewResponse} (overriding the value from the
   * {@linkplain #getCacheControlDefault() default}) whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_INTEGER_CONVERTER
   * @see #getViewContentMaxAge(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String VIEW_CONTENT_MAX_AGE_BY_PATH_PROP = "StaticChannel.View.Content.MaxAge.Path.";

    /**
   * @see #VIEW_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getViewContentMaxAge(Page.Request, String)
   */
    protected static final Pattern VIEW_CONTENT_MAX_AGE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_CONTENT_MAX_AGE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_INTEGER_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Integer}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #VIEW_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_INTEGER_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getViewContentMaxAge(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>> VIEW_CONTENT_MAX_AGE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Integer>, Map.Entry<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_INTEGER_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Integer>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER EntityTag} property values which will be
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#setEntityTag(EntityTag) set} on a
   * {@link org.wwweeeportal.portal.Channel.ViewResponse ViewResponse} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp. Note that if no matching property is found a tag will be automatically
   * {@linkplain Page#createEntityTag(Page.Request, CacheControl, CharSequence, Long, Date, boolean) generated} from the
   * content.
   * 
   * @see RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER
   * @see #getViewContentEntityTag(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String VIEW_CONTENT_ETAG_BY_PATH_PROP = "StaticChannel.View.Content.ETag.Path.";

    /**
   * @see #VIEW_CONTENT_ETAG_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getViewContentEntityTag(Page.Request, String)
   */
    protected static final Pattern VIEW_CONTENT_ETAG_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_CONTENT_ETAG_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link EntityTag}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #VIEW_CONTENT_ETAG_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getViewContentEntityTag(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>> VIEW_CONTENT_ETAG_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<EntityTag>, Map.Entry<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_ENTITY_TAG_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<EntityTag>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_DATA_SOURCE_CONVERTER DataSource} property values which will be
   * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#entity(Object) set} on a {@link Response} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_DATA_SOURCE_CONVERTER
   * @see #getResourceContentValue(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String RESOURCE_CONTENT_VALUE_BY_PATH_PROP = "StaticChannel.Resource.Content.Value.Path.";

    /**
   * @see #RESOURCE_CONTENT_VALUE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getResourceContentValue(Page.Request, String)
   */
    protected static final Pattern RESOURCE_CONTENT_VALUE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(RESOURCE_CONTENT_VALUE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_DATA_SOURCE_CONVERTER wrap} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link DataSource}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #RESOURCE_CONTENT_VALUE_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_DATA_SOURCE_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getResourceContentValue(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<DataSource>, Map.Entry<String, Pattern>>> RESOURCE_CONTENT_VALUE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<DataSource>, Map.Entry<RSProperties.Entry<DataSource>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_DATA_SOURCE_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<DataSource>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<DataSource>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER MediaType} property values which will be
   * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#type(MediaType) set} on a {@link Response} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER
   * @see #getResourceContentType(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String RESOURCE_CONTENT_TYPE_BY_PATH_PROP = "StaticChannel.Resource.Content.Type.Path.";

    /**
   * @see #RESOURCE_CONTENT_TYPE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getResourceContentType(Page.Request, String)
   */
    protected static final Pattern RESOURCE_CONTENT_TYPE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(RESOURCE_CONTENT_TYPE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link MediaType}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #RESOURCE_CONTENT_TYPE_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_MEDIA_TYPE_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getResourceContentType(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>> RESOURCE_CONTENT_TYPE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<MediaType>, Map.Entry<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_MEDIA_TYPE_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<MediaType>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<MediaType>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER Date} property values which will be
   * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#lastModified(Date) set} on a {@link Response} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER
   * @see DateUtil#CALENDAR_DATE_CONVERTER
   * @see #getResourceContentLastModified(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP = "StaticChannel.Resource.Content.LastModified.Path.";

    /**
   * @see #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getResourceContentLastModified(Page.Request, String)
   */
    protected static final Pattern RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Date}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getResourceContentLastModified(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>> RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Date>, Map.Entry<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>>(new ConverterChain<RSProperties.Result, RSProperties.Entry<Calendar>, RSProperties.Entry<Date>>(RSProperties.RESULT_ENTRY_RFC_1123_CALENDAR_CONVERTER, new RSProperties.EntryValueTypeConverter<Calendar, Date>(DateUtil.CALENDAR_DATE_CONVERTER)), new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Date>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Date>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_INTEGER_CONVERTER Integer} property values which will be
   * {@linkplain CacheControl#setMaxAge(int) set} on the
   * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#cacheControl(CacheControl) CacheControl} (overriding the
   * value from the {@linkplain #getCacheControlDefault() default}) for a {@link Response} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp.
   * 
   * @see RSProperties#RESULT_ENTRY_INTEGER_CONVERTER
   * @see #getResourceContentMaxAge(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP = "StaticChannel.Resource.Content.MaxAge.Path.";

    /**
   * @see #RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getResourceContentMaxAge(Page.Request, String)
   */
    protected static final Pattern RESOURCE_CONTENT_MAX_AGE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_INTEGER_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link Integer}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_INTEGER_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getResourceContentMaxAge(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>> RESOURCE_CONTENT_MAX_AGE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Integer>, Map.Entry<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_INTEGER_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Integer>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Integer>()));

    /**
   * The prefix defining a set of {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp keys}
   * to {@link RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER EntityTag} property values which will be
   * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#tag(EntityTag) set} on a {@link Response} whenever the
   * {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) path} of the request
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matches} the regexp. Note that if no matching property is found a tag will be automatically
   * {@linkplain Page#createEntityTag(Page.Request, CacheControl, CharSequence, Long, Date, boolean) generated} from the
   * content.
   * 
   * @see RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER
   * @see #getResourceContentEntityTag(Page.Request, String)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String RESOURCE_CONTENT_ETAG_BY_PATH_PROP = "StaticChannel.Resource.Content.ETag.Path.";

    /**
   * @see #RESOURCE_CONTENT_ETAG_BY_PATH_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   * @see #getResourceContentEntityTag(Page.Request, String)
   */
    protected static final Pattern RESOURCE_CONTENT_ETAG_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(RESOURCE_CONTENT_ETAG_BY_PATH_PROP) + ".*");

    /**
   * A {@link Converter} which, given an {@link org.wwweeeportal.util.ws.rs.RSProperties.Result RSProperties.Result},
   * will {@linkplain RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER parse} it into an
   * <code>{@link org.wwweeeportal.util.ws.rs.RSProperties.Entry RSProperties.Entry}&lt;{@link EntityTag}&gt;</code>
   * {@linkplain SourceTargetMapEntryConverterWrapper mapped} to it's associated
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter regexp key} for later
   * {@linkplain org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter#getMatchingValues(Map, String, boolean, String)
   * matching}.
   * 
   * @see #RESOURCE_CONTENT_ETAG_BY_PATH_PROP
   * @see RSProperties#RESULT_ENTRY_ENTITY_TAG_CONVERTER
   * @see org.wwweeeportal.portal.ConfigManager.RegexPropKeyConverter
   * @see #getResourceContentEntityTag(Page.Request, String)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>> RESOURCE_CONTENT_ETAG_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<EntityTag>, Map.Entry<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_ENTITY_TAG_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<EntityTag>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<EntityTag>()));

    /**
   * Construct a new <code>StaticChannel</code> instance.
   */
    public StaticChannel(final WWWeeePortal portal, final ContentManager.ChannelDefinition<?, ? extends StaticChannel> definition, final Page page) throws WWWeeePortal.Exception {
        super(portal, definition, page);
        return;
    }

    /**
   * @see #VIEW_CONTENT_VALUE_BY_PATH_PROP
   * @see #VIEW_CONTENT_VALUE_BY_PATH_PATTERN
   * @see #VIEW_CONTENT_VALUE_BY_PATH_CONVERTER
   */
    protected Map.Entry<RSProperties.Entry<Document>, Map.Entry<String, Pattern>> getViewContentValue(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_CONTENT_VALUE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), VIEW_CONTENT_VALUE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #VIEW_CONTENT_TYPE_BY_PATH_PROP
   * @see #VIEW_CONTENT_TYPE_BY_PATH_PATTERN
   * @see #VIEW_CONTENT_TYPE_BY_PATH_CONVERTER
   */
    protected MediaType getViewContentType(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_CONTENT_TYPE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), VIEW_CONTENT_TYPE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN
   * @see #VIEW_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER
   */
    protected Date getViewContentLastModified(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), VIEW_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #VIEW_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see #VIEW_CONTENT_MAX_AGE_BY_PATH_PATTERN
   * @see #VIEW_CONTENT_MAX_AGE_BY_PATH_CONVERTER
   */
    protected Integer getViewContentMaxAge(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_CONTENT_MAX_AGE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), VIEW_CONTENT_MAX_AGE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #VIEW_CONTENT_ETAG_BY_PATH_PROP
   * @see #VIEW_CONTENT_ETAG_BY_PATH_PATTERN
   * @see #VIEW_CONTENT_ETAG_BY_PATH_CONVERTER
   */
    protected EntityTag getViewContentEntityTag(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_CONTENT_ETAG_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), VIEW_CONTENT_ETAG_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #RESOURCE_CONTENT_VALUE_BY_PATH_PROP
   * @see #RESOURCE_CONTENT_VALUE_BY_PATH_PATTERN
   * @see #RESOURCE_CONTENT_VALUE_BY_PATH_CONVERTER
   */
    protected Map.Entry<RSProperties.Entry<DataSource>, Map.Entry<String, Pattern>> getResourceContentValue(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), RESOURCE_CONTENT_VALUE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RESOURCE_CONTENT_VALUE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #RESOURCE_CONTENT_TYPE_BY_PATH_PROP
   * @see #RESOURCE_CONTENT_TYPE_BY_PATH_PATTERN
   * @see #RESOURCE_CONTENT_TYPE_BY_PATH_CONVERTER
   */
    protected MediaType getResourceContentType(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), RESOURCE_CONTENT_TYPE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RESOURCE_CONTENT_TYPE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PROP
   * @see #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN
   * @see #RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER
   */
    protected Date getResourceContentLastModified(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RESOURCE_CONTENT_LAST_MODIFIED_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #RESOURCE_CONTENT_MAX_AGE_BY_PATH_PROP
   * @see #RESOURCE_CONTENT_MAX_AGE_BY_PATH_PATTERN
   * @see #RESOURCE_CONTENT_MAX_AGE_BY_PATH_CONVERTER
   */
    protected Integer getResourceContentMaxAge(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), RESOURCE_CONTENT_MAX_AGE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RESOURCE_CONTENT_MAX_AGE_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    /**
   * @see #RESOURCE_CONTENT_ETAG_BY_PATH_PROP
   * @see #RESOURCE_CONTENT_ETAG_BY_PATH_PATTERN
   * @see #RESOURCE_CONTENT_ETAG_BY_PATH_CONVERTER
   */
    protected EntityTag getResourceContentEntityTag(final Page.Request pageRequest, final String path) throws WWWeeePortal.Exception {
        return CollectionUtil.first(CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), RESOURCE_CONTENT_ETAG_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RESOURCE_CONTENT_ETAG_BY_PATH_CONVERTER, true, true), null, false, path)), null);
    }

    @Override
    public void doViewRequestImpl(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final String path = StringUtil.toString(pageRequest.getChannelLocalPath(this), null);
        final MediaType type = getViewContentType(pageRequest, path);
        if (type != null) viewResponse.setContentType(type);
        final Date lastModified = getViewContentLastModified(pageRequest, path);
        if (lastModified != null) viewResponse.setLastModified(lastModified);
        final Integer maxAge = getViewContentMaxAge(pageRequest, path);
        if (maxAge != null) {
            final CacheControl cacheControl = ConversionUtil.invokeConverter(RESTUtil.CACHE_CONTROL_DEFAULT_CONVERTER, viewResponse.getCacheControl());
            cacheControl.setMaxAge(maxAge.intValue());
            viewResponse.setCacheControl(cacheControl);
        }
        final Map.Entry<RSProperties.Entry<Document>, Map.Entry<String, Pattern>> content = getViewContentValue(pageRequest, path);
        EntityTag entityTag = getViewContentEntityTag(pageRequest, path);
        if (entityTag == null) {
            String contentSignature = null;
            if (content != null) {
                final Document document = content.getKey().getValue();
                final MD5DigestOutputStream md5dos = new MD5DigestOutputStream();
                synchronized (document) {
                    DOMUtil.createLSSerializer(true, true, false).write(document, ConversionUtil.invokeConverter(DOMUtil.OUTPUT_STREAM_LS_OUTPUT_CONVERTER, md5dos));
                }
                contentSignature = md5dos.getDigestHexString();
            }
            entityTag = Page.createEntityTag(pageRequest, viewResponse.getCacheControl(), contentSignature, null, lastModified, false);
        }
        if (entityTag != null) viewResponse.setEntityTag(entityTag);
        if (content != null) {
            viewResponse.setLocale(content.getKey().getValueLocale());
            final Element contentContainerElement = viewResponse.getContentContainerElement();
            final Document document = content.getKey().getValue();
            if (document.getDocumentElement() != null) DOMUtil.appendChild(contentContainerElement, document.getDocumentElement());
        }
        return;
    }

    @Override
    protected Response doResourceRequestImpl(final Page.Request pageRequest) throws WWWeeePortal.Exception, WebApplicationException {
        final String path = StringUtil.toString(pageRequest.getChannelLocalPath(this), null);
        final Map.Entry<RSProperties.Entry<DataSource>, Map.Entry<String, Pattern>> content = getResourceContentValue(pageRequest, path);
        if (content == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Date lastModified = getResourceContentLastModified(pageRequest, path);
        CacheControl cacheControl = getCacheControlDefault();
        final Integer maxAge = getResourceContentMaxAge(pageRequest, path);
        if (maxAge != null) {
            cacheControl = ConversionUtil.invokeConverter(RESTUtil.CACHE_CONTROL_DEFAULT_CONVERTER, cacheControl);
            cacheControl.setMaxAge(maxAge.intValue());
        }
        EntityTag entityTag = getResourceContentEntityTag(pageRequest, path);
        if (entityTag == null) {
            final MD5DigestOutputStream md5dos = new MD5DigestOutputStream();
            final DataSource dataSource = content.getKey().getValue();
            try {
                final InputStream inputStream = dataSource.getInputStream();
                IOUtil.pipe(inputStream, md5dos, -1, false, -1);
            } catch (IOException ioe) {
            }
            entityTag = Page.createEntityTag(pageRequest, cacheControl, md5dos.getDigestHexString(), null, lastModified, false);
        }
        Response.ResponseBuilder responseBuilder = RESTUtil.evaluatePreconditions(pageRequest.getRSRequest(), lastModified, entityTag);
        if (responseBuilder != null) return responseBuilder.build();
        responseBuilder = Response.ok(content.getKey().getValue(), getResourceContentType(pageRequest, path));
        responseBuilder.lastModified(getResourceContentLastModified(pageRequest, path));
        responseBuilder.tag(entityTag);
        responseBuilder.cacheControl(cacheControl);
        responseBuilder.language(content.getKey().getValueLocale());
        return responseBuilder.build();
    }
}
