package org.wwweeeportal.portal.db;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.annotation.*;
import org.w3c.dom.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.converter.*;
import org.codehaus.jettison.json.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.io.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.util.ws.rs.annotation.*;
import org.wwweeeportal.portal.*;

/**
 * This servlet can be used by client browsers via web forms and AJAX to manage (create/update/delete) the users content
 * (pages/channels) in the WWWeee-Portal database.
 */
@Path("/")
@Consumes("application/x-www-form-urlencoded")
@Produces("application/json")
public class DBService extends Application {

    public static final String USER_CONTENT_PAGE_DEFINITION_TEMPLATE_RESOURCE_DEFAULT = "/DBUserContentPageDefinitionTemplate.xml";

    public static final String USER_CONTENT_GLOBAL_CHANNEL_DEFINITION_TEMPLATE_RESOURCE_DEFAULT = "/DBUserContentGlobalChannelDefinitionTemplate.xml";

    public static final String USER_CONTENT_PAGE_DEFINITION_TEMPLATE_RESOURCE_PROP = "DBService.UserContent.PageDefinitionTemplate";

    public static final String USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_GROUP_PROP = "DBService.UserContent.PageDefinitionTemplate.PageGroup";

    protected static final Map<Pattern, Pattern> USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_GROUP_MATCH_PROPS = Collections.singletonMap(Pattern.compile('^' + Pattern.quote(USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_GROUP_PROP) + '$'), null);

    public static final String USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_PROP = "DBService.UserContent.PageDefinitionTemplate.Page";

    protected static final Map<Pattern, Pattern> USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_MATCH_PROPS = Collections.singletonMap(Pattern.compile('^' + Pattern.quote(USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_PROP) + '$'), null);

    public static final String USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_PROP = "DBService.UserContent.PageDefinitionTemplate.ChannelGroup";

    protected static final Map<Pattern, Pattern> USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_MATCH_PROPS = Collections.singletonMap(Pattern.compile('^' + Pattern.quote(USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_PROP) + '$'), null);

    public static final String USER_CONTENT_PAGE_DEFINITION_INSERT_CHANNEL_GROUP_RELATIVE_TO_THIS_PROP = "DBService.UserContent.PageDefinitionTemplate.Insert.ChannelGroup.RelativeToThis";

    protected static final Map<Pattern, Pattern> USER_CONTENT_PAGE_DEFINITION_INSERT_CHANNEL_GROUP_RELATIVE_TO_THIS_MATCH_PROPS = Collections.singletonMap(Pattern.compile('^' + Pattern.quote(USER_CONTENT_PAGE_DEFINITION_INSERT_CHANNEL_GROUP_RELATIVE_TO_THIS_PROP) + '$'), null);

    public static final String REDIRECT_DEFAULT_PROP = "DBService.RedirectDefault";

    public static final Converter<String, Boolean> PRECEDING_STRING_BOOLEAN_CONVERTER = new AbstractConverter<String, Boolean>() {

        @Override
        protected Boolean getNullSourceResult() {
            return Boolean.FALSE;
        }

        @Override
        protected Boolean convertImpl(final String input) throws Exception {
            return Boolean.valueOf(("preceding".equals(input)));
        }
    };

    public static final Converter<RSProperties.Result, Boolean> PRECEDING_STRING_RESULT_BOOLEAN_CONVERTER = new ConverterChain<RSProperties.Result, String, Boolean>(RSProperties.RESULT_STRING_CONVERTER, PRECEDING_STRING_BOOLEAN_CONVERTER);

    public static final List<Pattern> PROPERTY_KEY_STORAGE_BLACKLIST = Arrays.asList(Pattern.compile("^DBService\\.UserContent\\.PageDefinitionTemplate\\..*"));

    protected final RSProperties properties;

    protected final WWWeeeDB wwweeeDB;

    protected final ContentManager.PageContentContainer templateUserPageContentContainer;

    public DBService(@Context final ResourceLocation.Locator resourceLocator, @ConfigProperties final RSProperties configProperties) throws IllegalArgumentException, WWWeeePortal.Exception {
        if (resourceLocator == null) throw new IllegalArgumentException("null resourceLocator");
        if (configProperties == null) throw new IllegalArgumentException("null configProperties");
        try {
            properties = configProperties.duplicate(this, null, true);
            wwweeeDB = WWWeeeDBFactory.getInstanceJNDI(properties);
            wwweeeDB.initRef(this);
            final ConfigManager configManager = new ConfigManager(resourceLocator);
            final MarkupManager markupManager = new MarkupManager(configManager);
            final Document templatePageDocument = ConfigManager.getConfigProp(properties, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_RESOURCE_PROP, null, null, new RSProperties.Entry<String>(USER_CONTENT_PAGE_DEFINITION_TEMPLATE_RESOURCE_PROP, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_RESOURCE_DEFAULT, Locale.ROOT, null), markupManager.getConfigDocumentConverter(), null, false, false);
            templateUserPageContentContainer = ContentManager.PageContentContainer.parseXML(properties, templatePageDocument.getDocumentElement(), null);
            final ContentManager.PageGroupDefinition templatePageGroupDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_GROUP_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            if (templatePageGroupDefinition == null) throw new ConfigManager.ConfigException("User DB Content Page Container template contains no template Page Group Container", null);
            final ContentManager.PageDefinition<?> templatePageDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingPageDefinitions(null, null, null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            if (templatePageDefinition == null) throw new ConfigManager.ConfigException("User DB Content Page Definition template contains no template Page Definition", null);
            final ContentManager.ChannelGroupDefinition templateChannelGroupDefinition = CollectionUtil.first(templatePageDefinition.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            if (templateChannelGroupDefinition == null) throw new ConfigManager.ConfigException("User DB Content Page Definition template Page Definition contains no template Channel Group Definition", null);
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), wpe, getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), new WWWeeePortal.SoftwareException(e), getClass(), e);
        }
        return;
    }

    public static final ContentManager.PageDefinition<?> mkReferencePageDefinition(final RSProperties properties, final String referenceBaseProp, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws IllegalArgumentException, WWWeeePortal.Exception {
        final String ownerID = ConfigManager.getConfigProp(properties, referenceBaseProp + ".OwnerID", securityContext, httpHeaders, null, RSProperties.RESULT_STRING_CONVERTER, null, false, true);
        final String pageGroupID = ConfigManager.getConfigProp(properties, referenceBaseProp + ".PageGroupID", securityContext, httpHeaders, null, RSProperties.RESULT_STRING_CONVERTER, null, false, true);
        final String pageID = ConfigManager.getConfigProp(properties, referenceBaseProp + ".PageID", securityContext, httpHeaders, null, RSProperties.RESULT_STRING_CONVERTER, null, false, true);
        if ((ownerID == null) || (pageGroupID == null) || (pageID == null)) return null;
        final ContentManager.PageContentContainer pageContentContainer = new ContentManager.PageContentContainer(properties, null, ownerID);
        final ContentManager.PageGroupDefinition pageGroupDefinition = new ContentManager.PageGroupDefinition(pageContentContainer, new RSProperties(null, pageContentContainer.getProperties()), pageGroupID, null);
        final ContentManager.PageDefinition<?> pageDefinition = new ContentManager.PageDefinition<Page>(pageGroupDefinition, new RSProperties(null, pageGroupDefinition.getProperties()), pageID, null, Page.class);
        return pageDefinition;
    }

    protected static final <ChildDefinitionType extends ContentManager.AbstractContentDefinition<?, ?>> ChildDefinitionType getLastAutoNamedChild(final ContentManager.AbstractContentDefinition<?, ChildDefinitionType> contentDefinition, final String name) {
        if ((contentDefinition == null) || (name == null)) return null;
        final Pattern namePattern = Pattern.compile('^' + Pattern.quote(name) + "[2-9]*$");
        return CollectionUtil.last(contentDefinition.getMatchingChildDefinitions(namePattern, null, null, false, null, null, -1, false, false), null);
    }

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(WWWeeePortal.ExceptionMapper.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        final Set<Object> singletons = new HashSet<Object>(1);
        singletons.add(this);
        return singletons;
    }

    @PreDestroy
    public void shutdown() {
        if (wwweeeDB != null) wwweeeDB.destroyRef(this);
        return;
    }

    protected Logger getLogger() {
        return Logger.getLogger(getClass().getPackage().getName());
    }

    public static final String[] getClientUserRoles(final WWWeeeDB wwweeeDB, final SecurityContext securityContext) throws WWWeeePortal.Exception {
        if (securityContext == null) return null;
        final String[] accessControlUserRoles = wwweeeDB.selectAccessControlUserRoles();
        if (accessControlUserRoles == null) return null;
        final ArrayList<String> clientUserRoles = new ArrayList<String>(accessControlUserRoles.length);
        for (String accessControlUserRole : accessControlUserRoles) {
            if (securityContext.isUserInRole(accessControlUserRole)) {
                clientUserRoles.add(accessControlUserRole);
            }
        }
        return (!clientUserRoles.isEmpty()) ? clientUserRoles.toArray(new String[clientUserRoles.size()]) : null;
    }

    protected ContentManager.PageDefinition<?> getRequestedPage(final String ownerID, final String pageGroupID, final String pageID, final String clientUserLogin, final String[] clientUserRoles) throws WWWeeePortal.Exception, WebApplicationException {
        final ContentManager.PageDefinition<?> pageDefinition = wwweeeDB.getPage(new ContentManager.PageDefinition.Key(ownerID, pageGroupID, pageID), true, true, clientUserLogin, clientUserRoles, properties, true);
        if (pageDefinition == null) throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        return pageDefinition;
    }

    protected ContentManager.ChannelSpecification<?> getRequestedChannel(final String ownerID, final String pageGroupID, final String pageID, final String channelID, final String clientUserLogin, final String[] clientUserRoles) throws WWWeeePortal.Exception, WebApplicationException {
        final ContentManager.PageDefinition<?> pageDefinition = wwweeeDB.getPage(new ContentManager.PageDefinition.Key(ownerID, pageGroupID, pageID), true, true, clientUserLogin, clientUserRoles, properties, true);
        if (pageDefinition == null) throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        final ContentManager.ChannelSpecification<?> channelSpecification = pageDefinition.getChannelSpecification(channelID);
        if (channelSpecification == null) throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        return channelSpecification;
    }

    protected static final void validateRequestAccess(final ContentManager.AbstractContentContainer<?> contentContainer, final SecurityContext securityContext) throws WWWeeePortal.Exception, WebApplicationException {
        if (!MiscUtil.equal(contentContainer.getOwnerID(), (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null)) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        }
        return;
    }

    @POST
    @Path("/content_container/{OwnerID}/{PageGroupID}/{PageID}/add_page_group")
    public Response contentContainerAddPageGroup(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageContentContainer reqDBContentContainer = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles).getParentDefinition().getParentDefinition();
            validateRequestAccess(reqDBContentContainer, securityContext);
            final ContentManager.PageGroupDefinition templatePageGroupDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_GROUP_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            final ContentManager.PageGroupDefinition insertPageGroupDefinition = templatePageGroupDefinition.duplicate(reqDBContentContainer.duplicate(null, null, null, false, null, false, null, false, null, false, null), null, true, null, true, null, true, null);
            final String newPageGroupID = wwweeeDB.insertPageGroup(reqDBContentContainer, null, false, insertPageGroupDefinition, true, true, true, true, true, PROPERTY_KEY_STORAGE_BLACKLIST);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            entityMap.put("pageGroupID", newPageGroupID);
            if (insertPageGroupDefinition.getFirstChildDefinition() != null) {
                entityMap.put("pageID", insertPageGroupDefinition.getFirstChildDefinition().getID());
            }
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page_group/{OwnerID}/{PageGroupID}/{PageID}/add_page")
    public Response pageGroupAddPage(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageGroupDefinition reqDBPageGroupDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles).getParentDefinition();
            validateRequestAccess(reqDBPageGroupDefinition.getContentContainer(), securityContext);
            final ContentManager.PageDefinition<?> templatePageDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingPageDefinitions(null, null, null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            final ContentManager.PageDefinition<?> insertPageDefinition = templatePageDefinition.duplicate(reqDBPageGroupDefinition.duplicate(null, null, false, null, false, null, false, null), null, true, null, true, null);
            final String newPageID = wwweeeDB.insertPage(reqDBPageGroupDefinition, null, false, insertPageDefinition, true, true, true, true, PROPERTY_KEY_STORAGE_BLACKLIST);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            entityMap.put("pageID", newPageID);
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page_group/{OwnerID}/{PageGroupID}/{PageID}/rename")
    public Response pageGroupRename(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @FormParam("old_name") String oldName, @FormParam("new_name") String newName, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageGroupDefinition reqDBPageGroupDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles).getParentDefinition();
            validateRequestAccess(reqDBPageGroupDefinition.getContentContainer(), securityContext);
            if (wwweeeDB.isExistingPageGroup(reqDBPageGroupDefinition.getParentDefinition(), newName)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            wwweeeDB.updatePropertiesEntryValueText(reqDBPageGroupDefinition, Page.GROUP_TITLE_TEXT_PROP, oldName, newName, false);
            wwweeeDB.updateNameForPageGroup(reqDBPageGroupDefinition, newName);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            entityMap.put("pageGroupID", newName);
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page_group/{OwnerID}/{PageGroupID}/{PageID}/delete")
    public Response pageGroupDelete(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageGroupDefinition reqDBPageGroupDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles).getParentDefinition();
            validateRequestAccess(reqDBPageGroupDefinition.getContentContainer(), securityContext);
            wwweeeDB.deletePageGroup(reqDBPageGroupDefinition, true);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            final ContentManager.PageDefinition<?> redirectDefaultPageDefinition = mkReferencePageDefinition(properties, REDIRECT_DEFAULT_PROP, securityContext, httpHeaders);
            if (redirectDefaultPageDefinition != null) {
                entityMap.put("ownerID", redirectDefaultPageDefinition.getContentContainer().getOwnerID());
                entityMap.put("pageGroupID", redirectDefaultPageDefinition.getParentDefinition().getID());
                entityMap.put("pageID", redirectDefaultPageDefinition.getID());
            }
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page_group/{OwnerID}/{PageGroupID}/{PageID}/reorder")
    public Response pageGroupReorder(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @FormParam("relative_page_group_id") String relativePageGroupID, @FormParam("preceding_relative_page_group") @DefaultValue("false") boolean precedingRelativePageGroup, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageGroupDefinition reqDBPageGroupDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles).getParentDefinition();
            validateRequestAccess(reqDBPageGroupDefinition.getContentContainer(), securityContext);
            final ContentManager.PageGroupDefinition relativeToPageGroup = wwweeeDB.getPageGroup(reqDBPageGroupDefinition.getParentDefinition(), relativePageGroupID, clientUserLogin, clientUserRoles, properties, true);
            wwweeeDB.updatePositionOfPageGroup(reqDBPageGroupDefinition, relativeToPageGroup.getParentDefinition(), relativeToPageGroup, precedingRelativePageGroup, true);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    protected String insertChannelGroup(final ContentManager.PageDefinition<?> pageDefinition) throws WWWeeePortal.Exception {
        final ContentManager.PageDefinition<?> templatePageDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingPageDefinitions(null, null, null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_MATCH_PROPS, null, false, null, null, 1, false, false), null);
        final ContentManager.ChannelGroupDefinition templateChannelGroupDefinition = CollectionUtil.first(templatePageDefinition.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_MATCH_PROPS, null, false, null, null, 1, false, false), null);
        final ContentManager.ChannelGroupDefinition insertChannelGroupDefinition = templateChannelGroupDefinition.duplicate(pageDefinition.duplicate(null, null, false, null, false, null), null, false, null);
        final ContentManager.ChannelGroupDefinition insertRelativeToThisTemplateChannelGroupDefinition = CollectionUtil.first(templatePageDefinition.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_INSERT_CHANNEL_GROUP_RELATIVE_TO_THIS_MATCH_PROPS, null, false, null, null, 1, false, false), null);
        final ContentManager.ChannelGroupDefinition insertRelativeToThisChannelGroupDefinition = (insertRelativeToThisTemplateChannelGroupDefinition != null) ? pageDefinition.getChildDefinition(insertRelativeToThisTemplateChannelGroupDefinition.getID()) : null;
        final boolean insertPrecedingThisChannelGroup = (insertRelativeToThisTemplateChannelGroupDefinition != null) ? ConfigManager.getConfigProp(insertRelativeToThisTemplateChannelGroupDefinition.getProperties(), USER_CONTENT_PAGE_DEFINITION_INSERT_CHANNEL_GROUP_RELATIVE_TO_THIS_PROP, null, null, null, PRECEDING_STRING_RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue() : false;
        return wwweeeDB.insertChannelGroup(pageDefinition, insertRelativeToThisChannelGroupDefinition, insertPrecedingThisChannelGroup, insertChannelGroupDefinition, true, false, false, PROPERTY_KEY_STORAGE_BLACKLIST);
    }

    @POST
    @Path("/page/{OwnerID}/{PageGroupID}/{PageID}/add_channel")
    public Response pageAddChannel(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @FormParam("owner_id") String addOwnerID, @FormParam("channel_id") String addChannelID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageDefinition<?> reqDBPageDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBPageDefinition.getContentContainer(), securityContext);
            if (wwweeeDB.isExistingChannelSpecification(reqDBPageDefinition, addChannelID)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            final ContentManager.PageDefinition<?> insertPageDefinition = reqDBPageDefinition.duplicate(null, null, true, null, false, null);
            final ContentManager.PageDefinition<?> templatePageDefinition = CollectionUtil.first(templateUserPageContentContainer.getMatchingPageDefinitions(null, null, null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_PAGE_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            final ContentManager.ChannelGroupDefinition templateChannelGroupDefinition = CollectionUtil.first(templatePageDefinition.getMatchingChildDefinitions(null, USER_CONTENT_PAGE_DEFINITION_TEMPLATE_CHANNEL_GROUP_MATCH_PROPS, null, false, null, null, 1, false, false), null);
            ContentManager.ChannelGroupDefinition insertChannelGroup = getLastAutoNamedChild(insertPageDefinition, templateChannelGroupDefinition.getID());
            if (insertChannelGroup == null) {
                final String insertedChannelGroupID = insertChannelGroup(reqDBPageDefinition);
                insertChannelGroup = new ContentManager.ChannelGroupDefinition(insertPageDefinition, new RSProperties(this, insertPageDefinition.getProperties()), insertedChannelGroupID, null);
            }
            final ContentManager.GlobalChannelReference insertChannelReference = new ContentManager.GlobalChannelReference(insertChannelGroup, new ContentManager.GlobalChannelDefinition.Key(addOwnerID, addChannelID), new RSProperties(this, insertChannelGroup.getProperties()), null);
            wwweeeDB.insertGlobalChannelReference(insertChannelGroup, null, false, insertChannelReference, PROPERTY_KEY_STORAGE_BLACKLIST);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page/{OwnerID}/{PageGroupID}/{PageID}/add_channel_group")
    public Response pageAddChannelGroup(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageDefinition<?> reqDBPageDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBPageDefinition.getContentContainer(), securityContext);
            final String insertedChannelGroupID = insertChannelGroup(reqDBPageDefinition);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            entityMap.put("channelGroupID", insertedChannelGroupID);
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page/{OwnerID}/{PageGroupID}/{PageID}/rename")
    public Response pageRename(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @FormParam("old_name") String oldName, @FormParam("new_name") String newName, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageDefinition<?> reqDBPageDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBPageDefinition.getContentContainer(), securityContext);
            if (wwweeeDB.isExistingPage(reqDBPageDefinition.getParentDefinition(), newName)) {
                return Response.status(Response.Status.CONFLICT).build();
            }
            wwweeeDB.updatePropertiesEntryValueText(reqDBPageDefinition, Page.TITLE_TEXT_PROP, oldName, newName, false);
            wwweeeDB.updateNameForPage(reqDBPageDefinition, newName);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            entityMap.put("pageID", newName);
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page/{OwnerID}/{PageGroupID}/{PageID}/delete")
    public Response pageDelete(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageDefinition<?> reqDBPageDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBPageDefinition.getContentContainer(), securityContext);
            String relativePageName = wwweeeDB.selectNameForPageRelative(reqDBPageDefinition, true);
            if (relativePageName == null) relativePageName = wwweeeDB.selectNameForPageRelative(reqDBPageDefinition, false);
            wwweeeDB.deletePage(reqDBPageDefinition, true, true);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            if (relativePageName != null) {
                entityMap.put("ownerID", reqDBPageDefinition.getContentContainer().getOwnerID());
                entityMap.put("pageGroupID", reqDBPageDefinition.getParentDefinition().getID());
                entityMap.put("pageID", relativePageName);
            } else {
                final ContentManager.PageDefinition<?> redirectDefaultPageDefinition = mkReferencePageDefinition(properties, REDIRECT_DEFAULT_PROP, securityContext, httpHeaders);
                if (redirectDefaultPageDefinition != null) {
                    entityMap.put("ownerID", redirectDefaultPageDefinition.getContentContainer().getOwnerID());
                    entityMap.put("pageGroupID", redirectDefaultPageDefinition.getParentDefinition().getID());
                    entityMap.put("pageID", redirectDefaultPageDefinition.getID());
                }
            }
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/page/{OwnerID}/{PageGroupID}/{PageID}/reorder")
    public Response pageReorder(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @FormParam("relative_page_id") String relativePageID, @FormParam("preceding_relative_page") @DefaultValue("false") boolean precedingRelativePage, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.PageDefinition<?> reqDBPageDefinition = getRequestedPage(ownerID, pageGroupID, pageID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBPageDefinition.getContentContainer(), securityContext);
            final ContentManager.PageDefinition<?> relativeToPage = wwweeeDB.getPage(new ContentManager.PageDefinition.Key(ownerID, pageGroupID, relativePageID), false, true, clientUserLogin, clientUserRoles, properties, true);
            wwweeeDB.updatePositionOfPage(reqDBPageDefinition, relativeToPage.getParentDefinition(), relativeToPage, precedingRelativePage, false);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/channel/{OwnerID}/{PageGroupID}/{PageID}/{ChannelID}/delete")
    public Response channelDelete(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @PathParam("ChannelID") String channelID, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.ChannelSpecification<?> reqDBChannelSpecification = getRequestedChannel(ownerID, pageGroupID, pageID, channelID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBChannelSpecification.getContentContainer(), securityContext);
            wwweeeDB.deleteChannelSpecification(reqDBChannelSpecification, true, false, false, false);
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }

    @POST
    @Path("/channel/{OwnerID}/{PageGroupID}/{PageID}/{ChannelID}/reorder")
    public Response channelReorder(@PathParam("OwnerID") final String ownerID, @PathParam("PageGroupID") final String pageGroupID, @PathParam("PageID") final String pageID, @PathParam("ChannelID") String channelID, @FormParam("channel_group_id") String newChannelGroupID, @FormParam("relative_channel_id") String relativeChannelID, @FormParam("preceding_relative_channel") @DefaultValue("false") boolean precedingRelativeChannel, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            final String clientUserLogin = (securityContext.getUserPrincipal() != null) ? securityContext.getUserPrincipal().getName() : null;
            final String[] clientUserRoles = getClientUserRoles(wwweeeDB, securityContext);
            final ContentManager.ChannelSpecification<?> reqDBChannelSpecification = getRequestedChannel(ownerID, pageGroupID, pageID, channelID, clientUserLogin, clientUserRoles);
            validateRequestAccess(reqDBChannelSpecification.getContentContainer(), securityContext);
            final ContentManager.ChannelSpecification<?> relativeToChannel = reqDBChannelSpecification.getParentDefinition().getParentDefinition().getChannelSpecification(relativeChannelID);
            final ContentManager.ChannelGroupDefinition newChannelGroup = (relativeToChannel != null) ? relativeToChannel.getParentDefinition() : reqDBChannelSpecification.getParentDefinition().getParentDefinition().getChildDefinition(newChannelGroupID);
            wwweeeDB.updatePositionOfChannelSpecification(reqDBChannelSpecification, newChannelGroup, relativeToChannel, precedingRelativeChannel, true);
            wwweeeDB.deleteEmptyChannelGroups(reqDBChannelSpecification.getParentDefinition().getParentDefinition());
            final Map<String, Object> entityMap = new HashMap<String, Object>();
            return Response.ok().entity(new JSONObject(entityMap)).build();
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", securityContext, null, false), "UriInfo", uriInfo, null, false), getClass(), e);
        }
    }
}
