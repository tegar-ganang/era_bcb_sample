package org.wwweeeportal.portal.contentstores;

import java.net.*;
import java.util.*;
import javax.activation.*;
import javax.ws.rs.core.*;
import net.sf.jsr107cache.*;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.*;
import org.apache.chemistry.opencmis.commons.*;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.io.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.net.*;
import org.wwweeeportal.util.text.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.util.cmis.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channelplugins.*;
import org.wwweeeportal.portal.channels.*;

/**
 * <p>
 * A ContentStore for retrieving Page definitions over CMIS.
 * </p>
 * FIXME This whole class needs to be audited for exception handling.
 */
public class CMISContentStore extends ContentManager.ContentStore {

    public static final String OPERATION_MODE_PROP = "CMISContentStore.OperationMode";

    public static final String OPERATION_MODE_USER = "user";

    public static final String OPERATION_MODE_ROLE = "role";

    public static final String REPOSITORY_URL_PROP = "CMISContentStore.Repository.URL";

    public static final String REPOSITORY_USER_PROP = "CMISContentStore.Repository.User";

    public static final String REPOSITORY_PASSWORD_PROP = "CMISContentStore.Repository.Password";

    public static final String REPOSITORY_ID_PROP = "CMISContentStore.Repository.ID";

    public static final String REPOSITORY_PATH_TEMPLATE_PROP = "CMISContentStore.Repository.Path.Template";

    public static final String ROLES_ACCESS_ROLE_TEMPLATE_PROP = "CMISContentStore.Roles.AccessRole.Template";

    public static final TemplateString ROLES_ACCESS_ROLE_TEMPLATE_DEFAULT = new TemplateString("{UserRole}");

    public static final String PROXY_CHANNEL_BASE_URI_TEMPLATE_PROP = "CMISContentStore.ProxyChannel.BaseURI.Template";

    public static final String CONTENT_DEFINITION_TEMPLATE_RESOURCE_PROP = "CMISContentStore.ContentDefinition.Template";

    public static final String CONTENT_DEFINITION_TEMPLATE_RESOURCE_DEFAULT = "/CMISContentDefinitionTemplate.xml";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String ROLES_JCACHE_PROPERTY_PROP = "CMISContentStore.Roles.JCache.Property.";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String ROLES_JCACHE_NAME_PROP = "CMISContentStore.Roles.JCache.Name";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String ROLES_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP = "CMISContentStore.Roles.JCache.Properties.CacheNameProp";

    protected final String operationMode;

    protected ContentManager.PageContentContainer templateContentContainer = null;

    protected final URL repositoryURL;

    protected final String repositoryID;

    protected final SessionFactory sessionFactory;

    protected final Session session;

    protected final Cache rolesCache;

    public CMISContentStore(final ContentManager contentManager, final RSProperties properties) throws WWWeeePortal.Exception {
        contentManager.super(properties);
        operationMode = getConfigProp(OPERATION_MODE_PROP, null, null, null, false);
        if (!((OPERATION_MODE_USER.equals(operationMode)) || (OPERATION_MODE_ROLE.equals(operationMode)))) throw new ConfigManager.ConfigException("Unknown operation mode: " + operationMode, null);
        final org.w3c.dom.Document contentDefinitionTemplateDocument = ConfigManager.getConfigProp(this.properties, CONTENT_DEFINITION_TEMPLATE_RESOURCE_PROP, null, null, new RSProperties.Entry<String>(CONTENT_DEFINITION_TEMPLATE_RESOURCE_PROP, CONTENT_DEFINITION_TEMPLATE_RESOURCE_DEFAULT, Locale.ROOT, null), getPortal().getMarkupManager().getConfigDocumentConverter(), null, false, true);
        templateContentContainer = (contentDefinitionTemplateDocument != null) ? ContentManager.PageContentContainer.parseXML(properties, contentDefinitionTemplateDocument.getDocumentElement(), null) : null;
        sessionFactory = SessionFactoryImpl.newInstance();
        final Map<String, String> repositoryParams = new HashMap<String, String>();
        repositoryParams.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        repositoryURL = getConfigProp(REPOSITORY_URL_PROP, null, null, RSProperties.RESULT_URL_CONVERTER, null, false, false);
        repositoryParams.put(SessionParameter.ATOMPUB_URL, repositoryURL.toString());
        final String repositoryUser = getConfigProp(REPOSITORY_USER_PROP, null, null, null, true);
        if (repositoryUser != null) repositoryParams.put(SessionParameter.USER, repositoryUser);
        final String repositoryPassword = getConfigProp(REPOSITORY_PASSWORD_PROP, null, null, null, true);
        if (repositoryPassword != null) repositoryParams.put(SessionParameter.PASSWORD, repositoryPassword);
        try {
            final String repositoryIDProp = getConfigProp(REPOSITORY_ID_PROP, null, null, null, true);
            if (repositoryIDProp != null) {
                repositoryID = repositoryIDProp;
                repositoryParams.put(SessionParameter.REPOSITORY_ID, repositoryID);
                session = sessionFactory.createSession(repositoryParams);
            } else {
                final Repository repository = CollectionUtil.first(sessionFactory.getRepositories(repositoryParams), null);
                repositoryID = repository.getId();
                session = repository.createSession();
            }
        } catch (CmisObjectNotFoundException confe) {
            throw new ConfigManager.ConfigException(confe);
        }
        rolesCache = ConfigManager.getCache(CMISContentStore.class, "Roles", properties, ROLES_JCACHE_PROPERTY_PROP, ROLES_JCACHE_NAME_PROP, ROLES_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP);
        return;
    }

    @Override
    protected void destroyInternal(final LogAnnotation.Message logMessage) {
        super.destroyInternal(logMessage);
        return;
    }

    protected List<String> getRoleFolderNames(final TemplateString repositoryPathTemplateString) throws WWWeeePortal.Exception {
        final String repositoryPathString = repositoryPathTemplateString.toString();
        final int userRoleVariableIndex = repositoryPathString.indexOf("{UserRole}");
        if (userRoleVariableIndex < 0) return null;
        final String roleFoldersParentFolderPath = repositoryPathString.substring(0, userRoleVariableIndex);
        final String rolesCacheKey = ConversionUtil.invokeConverter(StringUtil.COMPONENTS_TO_DASH_DELIMITED_STRING_CONVERTER, Arrays.asList(getPortal().getPortalID(), "CMISContentStore", "Roles", repositoryURL.toString(), repositoryID, roleFoldersParentFolderPath));
        @SuppressWarnings("unchecked") final List<String> cachedRoles = (List<String>) rolesCache.get(rolesCacheKey);
        if (cachedRoles != null) return cachedRoles;
        final Folder roleFoldersParentFolder = (Folder) session.getObjectByPath(roleFoldersParentFolderPath);
        final List<Tree<FileableCmisObject>> roleFoldersParentFolderDescendants;
        if (roleFoldersParentFolder != null) {
            roleFoldersParentFolderDescendants = roleFoldersParentFolder.getDescendants(1, session.createOperationContext(null, false, false, false, IncludeRelationships.NONE, null, true, "name", true, 99999));
        } else {
            roleFoldersParentFolderDescendants = Collections.emptyList();
        }
        final List<String> roleFolderNames = new ArrayList<String>(roleFoldersParentFolderDescendants.size());
        for (Tree<FileableCmisObject> roleFolderCandidateTree : roleFoldersParentFolderDescendants) {
            if (!(roleFolderCandidateTree.getItem() instanceof Folder)) continue;
            roleFolderNames.add(roleFolderCandidateTree.getItem().getName());
        }
        rolesCache.put(rolesCacheKey, roleFolderNames);
        return roleFolderNames;
    }

    protected RepositoryContentDefinition getRepositoryContentDefinition(final String ownerID, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final TemplateString repositoryPathTemplateString = getConfigProp(REPOSITORY_PATH_TEMPLATE_PROP, securityContext, httpHeaders, RSProperties.RESULT_TEMPLATE_STRING_CONVERTER, null, true, false);
        final Map<String, String> repositoryPathTemplateArguments = new HashMap<String, String>();
        if (OPERATION_MODE_USER.equals(operationMode)) {
            final String userLogin = ownerID;
            repositoryPathTemplateArguments.put("UserLogin", userLogin);
            return new RepositoryContentDefinition(repositoryPathTemplateString.format(repositoryPathTemplateArguments, null), new RSAccessControl.UserLogin(null, userLogin), ownerID);
        } else if (OPERATION_MODE_ROLE.equals(operationMode)) {
            final String userRole = ownerID;
            repositoryPathTemplateArguments.put("UserRole", userRole);
            final TemplateString accessRoleTemplate = getConfigProp(ROLES_ACCESS_ROLE_TEMPLATE_PROP, securityContext, httpHeaders, RSProperties.RESULT_TEMPLATE_STRING_CONVERTER, ROLES_ACCESS_ROLE_TEMPLATE_DEFAULT, true, false);
            final Map<String, String> accessRoleTemplateArguments = new HashMap<String, String>();
            accessRoleTemplateArguments.put("UserRole", userRole);
            final String accessRole = accessRoleTemplate.format(accessRoleTemplateArguments, null);
            return new RepositoryContentDefinition(repositoryPathTemplateString.format(repositoryPathTemplateArguments, null), new RSAccessControl.UserRole(null, accessRole), ownerID);
        }
        return null;
    }

    protected List<RepositoryContentDefinition> getRepositoryContentDefinitions(final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final TemplateString repositoryPathTemplateString = getConfigProp(REPOSITORY_PATH_TEMPLATE_PROP, securityContext, httpHeaders, RSProperties.RESULT_TEMPLATE_STRING_CONVERTER, null, true, true);
        if (repositoryPathTemplateString == null) return null;
        final Map<String, String> repositoryPathTemplateArguments = new HashMap<String, String>();
        if (OPERATION_MODE_USER.equals(operationMode)) {
            final String userLogin = securityContext.getUserPrincipal().getName();
            final String ownerID = userLogin;
            repositoryPathTemplateArguments.put("UserLogin", userLogin);
            return Arrays.asList(new RepositoryContentDefinition(repositoryPathTemplateString.format(repositoryPathTemplateArguments, null), new RSAccessControl.UserLogin(null, userLogin), ownerID));
        } else if (OPERATION_MODE_ROLE.equals(operationMode)) {
            final List<String> roleFolderNames = getRoleFolderNames(repositoryPathTemplateString);
            if ((roleFolderNames == null) || (roleFolderNames.isEmpty())) return null;
            final TemplateString accessRoleTemplate = getConfigProp(ROLES_ACCESS_ROLE_TEMPLATE_PROP, securityContext, httpHeaders, RSProperties.RESULT_TEMPLATE_STRING_CONVERTER, ROLES_ACCESS_ROLE_TEMPLATE_DEFAULT, true, false);
            final List<RepositoryContentDefinition> repoContentDefs = new ArrayList<RepositoryContentDefinition>(roleFolderNames.size());
            for (int i = 0; i < roleFolderNames.size(); i++) {
                final String roleFolderName = roleFolderNames.get(i);
                final String userRole = roleFolderName;
                final Map<String, String> accessRoleTemplateArguments = new HashMap<String, String>();
                accessRoleTemplateArguments.put("UserRole", userRole);
                final String accessRole = accessRoleTemplate.format(accessRoleTemplateArguments, null);
                if (!securityContext.isUserInRole(accessRole)) continue;
                final String ownerID = userRole;
                repositoryPathTemplateArguments.put("UserRole", userRole);
                repoContentDefs.add(new RepositoryContentDefinition(repositoryPathTemplateString.format(repositoryPathTemplateArguments, null), new RSAccessControl.UserRole(null, accessRole), ownerID));
            }
            return repoContentDefs;
        }
        return null;
    }

    protected static final void addProperties(final CmisObject object, final RSProperties properties) {
        for (Property<?> property : object.getProperties()) {
            final Object value = property.getFirstValue();
            if (value == null) continue;
            properties.setProp(property.getId(), value, Locale.ROOT, null);
        }
        return;
    }

    protected ContentManager.PageContentContainer createContentContainer(final RepositoryContentDefinition repoContentDef, final FileableCmisObject contentDefFolder, final ContentManager.PageContentContainer templatePageContentContainer) throws WWWeeePortal.Exception {
        final RSProperties contentContainerProperties = new RSProperties(this, properties);
        if (templatePageContentContainer != null) contentContainerProperties.putAll(templatePageContentContainer.getProperties());
        addProperties(contentDefFolder, contentContainerProperties);
        return new ContentManager.PageContentContainer(contentContainerProperties, repoContentDef.getAccessControl(), repoContentDef.getOwnerID());
    }

    protected ContentManager.PageGroupDefinition createPageGroup(final ContentManager.PageContentContainer contentContainer, final FileableCmisObject pageGroupFolder, final ContentManager.PageGroupDefinition templatePageGroupDefinition) throws WWWeeePortal.Exception {
        final RSProperties pageGroupProperties = new RSProperties(this, contentContainer.getProperties());
        pageGroupProperties.setProp(Page.GROUP_TITLE_TEXT_PROP, pageGroupFolder.getName(), Locale.ROOT, null);
        addProperties(pageGroupFolder, pageGroupProperties);
        if (templatePageGroupDefinition != null) pageGroupProperties.putAll(templatePageGroupDefinition.getProperties());
        final String pageGroupID = pageGroupFolder.getName();
        final ContentManager.PageGroupDefinition pageGroupDefinition = new ContentManager.PageGroupDefinition(contentContainer, pageGroupProperties, pageGroupID, null);
        return pageGroupDefinition;
    }

    protected static final Document getChannelIndexDocument(final List<Tree<FileableCmisObject>> channelFolderDescendants) throws WWWeeePortal.Exception {
        for (Tree<FileableCmisObject> channelDocumentCandidateTree : channelFolderDescendants) {
            if (!(channelDocumentCandidateTree.getItem() instanceof Document)) continue;
            final Document channelDocument = (Document) channelDocumentCandidateTree.getItem();
            if ((channelDocument.getName().startsWith("index.")) || (channelDocument.getName().startsWith("default."))) return channelDocument;
        }
        return null;
    }

    protected URI resolveProxyChannelBaseURI(final ContentManager.AbstractContentContainer<?> contentContainer, final FileableCmisObject[] path, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final TemplateString baseURITemplateString = getConfigProp(PROXY_CHANNEL_BASE_URI_TEMPLATE_PROP, securityContext, httpHeaders, RSProperties.RESULT_TEMPLATE_STRING_CONVERTER, null, true, false);
        final Map<String, String> templateArguments = new HashMap<String, String>();
        if (OPERATION_MODE_USER.equals(operationMode)) {
            final String userLogin = contentContainer.getOwnerID();
            templateArguments.put("UserLogin", userLogin);
        } else if (OPERATION_MODE_ROLE.equals(operationMode)) {
            final String userRole = contentContainer.getOwnerID();
            templateArguments.put("UserRole", userRole);
        }
        final URI unresolvedBaseURI = ConversionUtil.invokeConverter(NetUtil.STRING_URI_CONVERTER, baseURITemplateString.format(templateArguments, null));
        final StringBuffer pageGroupToDocumentPath = new StringBuffer();
        for (int i = 1; i < 6; i++) {
            if (i > 1) pageGroupToDocumentPath.append('/');
            pageGroupToDocumentPath.append(ConversionUtil.invokeConverter(NetUtil.URL_ENCODE_CONVERTER, path[i].getName()));
        }
        return unresolvedBaseURI.resolve(pageGroupToDocumentPath.toString());
    }

    protected ContentManager.LocalChannelDefinition<?> createChannel(final ContentManager.ChannelGroupDefinition channelGroup, final FileableCmisObject[] path, final List<Tree<FileableCmisObject>> channelFolderDescendants, final ContentManager.ChannelSpecification<?> templateChannelSpecification, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final Document channelIndexDocument = getChannelIndexDocument(channelFolderDescendants);
        if (channelIndexDocument == null) return null;
        path[5] = channelIndexDocument;
        final String channelID = channelGroup.getID() + '.' + path[4].getName();
        final ContentManager.LocalChannelSpecification<ProxyChannel> localChannelSpecification = new ContentManager.LocalChannelSpecification<ProxyChannel>(channelGroup, new RSProperties(this, channelGroup.getProperties()), channelID, null);
        final RSProperties channelProperties = new RSProperties(this, localChannelSpecification.getProperties());
        channelProperties.setProp(Channel.TITLE_TEXT_PROP, path[4].getName(), Locale.ROOT, null);
        addProperties(path[4], channelProperties);
        channelProperties.setProp(ProxyChannel.BASE_URI_PROP, resolveProxyChannelBaseURI(channelGroup.getContentContainer(), path, securityContext, httpHeaders), Locale.ROOT, null);
        final ContentManager.LocalChannelDefinition<ProxyChannel> channelDefinition;
        if ((templateChannelSpecification != null) && (templateChannelSpecification.getFirstChildDefinition() != null)) {
            @SuppressWarnings("unchecked") final ContentManager.LocalChannelDefinition<ProxyChannel> templateChannelDefinition = (ContentManager.LocalChannelDefinition<ProxyChannel>) templateChannelSpecification.getFirstChildDefinition();
            channelDefinition = templateChannelDefinition.duplicate(localChannelSpecification, channelProperties, true, null);
        } else {
            channelDefinition = new ContentManager.LocalChannelDefinition<ProxyChannel>(localChannelSpecification, channelProperties, null, ProxyChannel.class);
            final MimeType contentType = ConversionUtil.invokeConverter(IOUtil.STRING_MIME_TYPE_CONVERTER, channelIndexDocument.getContentStreamMimeType());
            if (HTMLUtil.isHTML(contentType)) {
                final RSProperties pluginProperties = new RSProperties(this, channelDefinition.getProperties());
                new ContentManager.ChannelPluginDefinition<ProxyChannelHTMLSource>(channelDefinition, pluginProperties, null, ProxyChannelHTMLSource.class);
            }
        }
        return channelDefinition;
    }

    protected ContentManager.ChannelGroupDefinition createChannelGroup(final ContentManager.PageDefinition<Page> pageDefinition, final FileableCmisObject[] path, final List<Tree<FileableCmisObject>> channelGroupFolderDescendants, final ContentManager.ChannelGroupDefinition templateChannelGroupDefinition, final int channelGroupFolderIndex, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final String channelGroupID = ((templateChannelGroupDefinition != null) ? templateChannelGroupDefinition.getID() : "body") + ((channelGroupFolderIndex > 0) ? String.valueOf(channelGroupFolderIndex + 1) : "");
        final RSProperties channelGroupProperties = new RSProperties(this, pageDefinition.getProperties());
        addProperties(path[3], channelGroupProperties);
        if (templateChannelGroupDefinition != null) channelGroupProperties.putAll(templateChannelGroupDefinition.getProperties());
        final ContentManager.ChannelGroupDefinition channelGroup = new ContentManager.ChannelGroupDefinition(pageDefinition, channelGroupProperties, channelGroupID, null);
        final ContentManager.ChannelSpecification<?> templateChannelSpecification = (templateChannelGroupDefinition != null) ? templateChannelGroupDefinition.getChildDefinition("document_template") : null;
        for (Tree<FileableCmisObject> channelFolderCandidateTree : channelGroupFolderDescendants) {
            if (!(channelFolderCandidateTree.getItem() instanceof Folder)) continue;
            path[4] = channelFolderCandidateTree.getItem();
            createChannel(channelGroup, path, channelFolderCandidateTree.getChildren(), templateChannelSpecification, securityContext, httpHeaders);
        }
        return channelGroup;
    }

    protected ContentManager.PageDefinition<?> createPage(final ContentManager.PageGroupDefinition pageGroupDefinition, final FileableCmisObject[] path, final List<Tree<FileableCmisObject>> pageFolderDescendants, final ContentManager.PageDefinition<?> templatePageDefinition, final boolean channelDefinitionsRequired, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final RSProperties pageProperties = new RSProperties(this, pageGroupDefinition.getProperties());
        pageProperties.setProp(Page.TITLE_TEXT_PROP, path[2].getName(), Locale.ROOT, null);
        addProperties(path[2], pageProperties);
        if (templatePageDefinition != null) pageProperties.putAll(templatePageDefinition.getProperties());
        final String pageID = path[2].getName();
        final ContentManager.PageDefinition<Page> pageDefinition = new ContentManager.PageDefinition<Page>(pageGroupDefinition, pageProperties, pageID, null, Page.class);
        if (!channelDefinitionsRequired) return pageDefinition;
        final ContentManager.ChannelSpecification<?> templateChannelSpecification = (templatePageDefinition != null) ? templatePageDefinition.getChannelSpecification("document_template") : null;
        final String templateChannelGroupID = (templateChannelSpecification != null) ? templateChannelSpecification.getParentDefinition().getID() : "body";
        ContentManager.ChannelGroupDefinition templateChannelGroupDefinition = null;
        if (templatePageDefinition != null) {
            for (ContentManager.ChannelGroupDefinition channelGroupDefinition : CollectionUtil.mkNotNull(templatePageDefinition.getChildDefinitions())) {
                if (templateChannelGroupID.equals(channelGroupDefinition.getID())) {
                    templateChannelGroupDefinition = channelGroupDefinition;
                    break;
                }
                channelGroupDefinition.duplicate(pageDefinition, null, true, null);
            }
        }
        if (pageFolderDescendants != null) {
            int channelGroupFolderIndex = -1;
            for (Tree<FileableCmisObject> channelGroupFolderCandidateTree : pageFolderDescendants) {
                if (!(channelGroupFolderCandidateTree.getItem() instanceof Folder)) continue;
                path[3] = channelGroupFolderCandidateTree.getItem();
                channelGroupFolderIndex++;
                createChannelGroup(pageDefinition, path, channelGroupFolderCandidateTree.getChildren(), templateChannelGroupDefinition, channelGroupFolderIndex, securityContext, httpHeaders);
            }
        }
        if (templatePageDefinition != null) {
            boolean afterPlaceholder = false;
            for (ContentManager.ChannelGroupDefinition channelGroupDefinition : CollectionUtil.mkNotNull(templatePageDefinition.getChildDefinitions())) {
                if (!afterPlaceholder) {
                    if (templateChannelGroupID.equals(channelGroupDefinition.getID())) afterPlaceholder = true;
                    continue;
                }
                channelGroupDefinition.duplicate(pageDefinition, null, true, null);
            }
        }
        return pageDefinition;
    }

    @Override
    public List<ContentManager.PageDefinition<?>> getPageDefinitions(final Map<String, String> withMatchingPageContainerProps, final Map<String, String> withoutMatchingPageContainerProps, final Map<String, String> withMatchingPageGroupProps, final Map<String, String> withoutMatchingPageGroupProps, final Map<String, String> withMatchingPageProps, final Map<String, String> withoutMatchingPageProps, final boolean checkAccessControl, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders, final boolean channelDefinitionsRequired, final int startIndex, final int count) throws WWWeeePortal.Exception {
        final List<RepositoryContentDefinition> repoContentDefs = getRepositoryContentDefinitions(uriInfo, securityContext, httpHeaders);
        if (repoContentDefs == null) return null;
        final ArrayList<ContentManager.PageDefinition<?>> pages = new ArrayList<ContentManager.PageDefinition<?>>();
        for (RepositoryContentDefinition repoContentDef : repoContentDefs) {
            final Folder contentDefFolder = (Folder) session.getObjectByPath(repoContentDef.getRepositoryPath());
            if (contentDefFolder == null) continue;
            final List<Tree<FileableCmisObject>> contentDefFolderDescendants = contentDefFolder.getDescendants((channelDefinitionsRequired) ? 5 : 2, session.createOperationContext(null, false, false, false, IncludeRelationships.NONE, null, true, "name", true, 100));
            if ((contentDefFolderDescendants == null) || (contentDefFolderDescendants.size() == 0)) continue;
            final FileableCmisObject[] path = new FileableCmisObject[6];
            path[0] = contentDefFolder;
            final ContentManager.PageGroupDefinition templatePageGroupDefinition = (templateContentContainer != null) ? templateContentContainer.getFirstChildDefinition() : null;
            final ContentManager.PageDefinition<?> templatePageDefinition = (templatePageGroupDefinition != null) ? templatePageGroupDefinition.getFirstChildDefinition() : null;
            ContentManager.PageContentContainer contentContainer = null;
            for (Tree<FileableCmisObject> pageGroupCandidateTree : contentDefFolderDescendants) {
                if (!(pageGroupCandidateTree.getItem() instanceof Folder)) continue;
                path[1] = pageGroupCandidateTree.getItem();
                ContentManager.PageGroupDefinition pageGroupDefinition = null;
                for (Tree<FileableCmisObject> pageCandidateTree : pageGroupCandidateTree.getChildren()) {
                    if (!(pageCandidateTree.getItem() instanceof Folder)) continue;
                    path[2] = pageCandidateTree.getItem();
                    if ((startIndex >= 0) && (pages.size() < startIndex)) {
                        pages.add(null);
                        if ((count >= 0) && (startIndex >= 0) && (pages.size() >= startIndex + count)) break;
                        continue;
                    }
                    if (contentContainer == null) contentContainer = createContentContainer(repoContentDef, contentDefFolder, templateContentContainer);
                    if (pageGroupDefinition == null) pageGroupDefinition = createPageGroup(contentContainer, path[1], templatePageGroupDefinition);
                    final ContentManager.PageDefinition<?> page = createPage(pageGroupDefinition, path, pageCandidateTree.getChildren(), templatePageDefinition, channelDefinitionsRequired, securityContext, httpHeaders);
                    pages.add(page);
                    if ((count >= 0) && (startIndex >= 0) && (pages.size() >= startIndex + count)) break;
                }
            }
        }
        if (pages.isEmpty()) return null;
        ContentManager.AbstractContentContainer.init(pages);
        pages.trimToSize();
        return pages;
    }

    @Override
    public ContentManager.PageDefinition<?> getPageDefinition(final ContentManager.PageDefinition.Key pageKey, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders, final boolean channelDefinitionsRequired) throws WWWeeePortal.Exception {
        final RepositoryContentDefinition repoContentDef = getRepositoryContentDefinition(pageKey.getOwnerID(), uriInfo, securityContext, httpHeaders);
        if (repoContentDef == null) return null;
        final StringBuffer pageFolderPath = new StringBuffer();
        pageFolderPath.append(repoContentDef.getRepositoryPath());
        if (pageFolderPath.charAt(pageFolderPath.length() - 1) != '/') pageFolderPath.append('/');
        pageFolderPath.append(pageKey.getPageGroupID());
        pageFolderPath.append("/");
        pageFolderPath.append(pageKey.getPageID());
        final List<Folder> pageToContentDefFolders = CMISUtil.getFolderAndParents(session, pageFolderPath.toString(), 2);
        if ((pageToContentDefFolders == null) || (pageToContentDefFolders.size() < 3)) return null;
        final FileableCmisObject[] path = new FileableCmisObject[] { pageToContentDefFolders.get(2), pageToContentDefFolders.get(1), pageToContentDefFolders.get(0), null, null, null };
        final List<Tree<FileableCmisObject>> pageFolderDescendants = (channelDefinitionsRequired) ? ((Folder) path[2]).getDescendants(3, session.createOperationContext(null, false, false, false, IncludeRelationships.NONE, null, true, "name", true, 100)) : null;
        final ContentManager.PageGroupDefinition templatePageGroupDefinition = (templateContentContainer != null) ? templateContentContainer.getFirstChildDefinition() : null;
        final ContentManager.PageDefinition<?> templatePageDefinition = (templatePageGroupDefinition != null) ? templatePageGroupDefinition.getFirstChildDefinition() : null;
        final ContentManager.PageContentContainer contentContainer = createContentContainer(repoContentDef, path[0], templateContentContainer);
        final ContentManager.PageGroupDefinition pageGroupDefinition = createPageGroup(contentContainer, path[1], templatePageGroupDefinition);
        final ContentManager.PageDefinition<?> pageDefinition = createPage(pageGroupDefinition, path, pageFolderDescendants, templatePageDefinition, channelDefinitionsRequired, securityContext, httpHeaders);
        contentContainer.init();
        return pageDefinition;
    }

    protected static class RepositoryContentDefinition {

        protected final String repositoryPath;

        protected final RSAccessControl accessControl;

        protected final String ownerID;

        public RepositoryContentDefinition(final String repositoryPath, final RSAccessControl accessControl, final String ownerID) {
            this.repositoryPath = repositoryPath;
            this.accessControl = accessControl;
            this.ownerID = ownerID;
            return;
        }

        public String getRepositoryPath() {
            return repositoryPath;
        }

        public RSAccessControl getAccessControl() {
            return accessControl;
        }

        public String getOwnerID() {
            return ownerID;
        }
    }
}
