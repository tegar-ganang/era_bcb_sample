package com.germinus.xpression.groupware;

import com.germinus.xpression.cms.CMSRuntimeException;
import com.germinus.xpression.cms.Location;
import com.germinus.xpression.cms.NotEnoughAdminsException;
import com.germinus.xpression.cms.NotMemberException;
import com.germinus.xpression.cms.contents.ContentManager;
import com.germinus.xpression.cms.directory.DirectoryFolder;
import com.germinus.xpression.cms.directory.DirectoryPersister;
import com.germinus.xpression.cms.jcr.JCRUtil;
import com.germinus.xpression.cms.util.ManagerRegistry;
import com.germinus.xpression.cms.web.TemporaryFilesHandler;
import com.germinus.xpression.cms.worlds.World;
import com.germinus.xpression.cms.worlds.WorldManager;
import com.germinus.xpression.groupware.communities.AlreadyMemberException;
import com.germinus.xpression.groupware.communities.Community;
import com.germinus.xpression.groupware.communities.CommunityMembership;
import com.germinus.xpression.groupware.communities.CommunityMembershipIterator;
import com.germinus.xpression.groupware.communities.CommunityMembershipList;
import com.germinus.xpression.groupware.communities.CommunityNotFoundException;
import com.germinus.xpression.groupware.communities.CommunityPersister;
import com.germinus.xpression.groupware.communities.CommunityPrivilege;
import com.germinus.xpression.groupware.communities.CommunityAlreadyInRepositoryException;
import com.germinus.xpression.groupware.communities.CommunityRuntimeException;
import com.germinus.xpression.groupware.communities.ImplicitCommunitiesRule;
import com.germinus.xpression.groupware.communities.ImplicitCommunitiesRuleException;
import com.germinus.xpression.groupware.communities.ImportCommunityResult;
import com.germinus.xpression.groupware.communities.ParentOrganizationNotImportedException;
import com.germinus.xpression.groupware.util.GroupwareConfig;
import com.germinus.xpression.groupware.util.GroupwareManagerRegistry;
import com.germinus.xpression.groupware.util.LiferayHelper;
import com.germinus.xpression.groupware.util.LiferayHelperFactory;
import com.germinus.xpression.groupware.webs.NoWebLink;
import com.germinus.xpression.groupware.webs.Web;
import com.germinus.xpression.groupware.webs.WebNotFoundException;
import com.germinus.xpression.groupware.webs.WebPersister;
import com.germinus.xpression.i18n.I18NString;
import com.germinus.xpression.i18n.I18NUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CommunityManagerImpl implements CommunityManager {

    private static Log log = LogFactory.getLog(CommunityManagerImpl.class);

    private static final String ROOT_FOLDER_NAME = I18NUtils.getLocalizedMessage("file_directory.rootFolder");

    private static final String LIFERAY_COMMUNITY_TYPE = "liferaycommunity";

    private static final String LIFERAY_ORGANIZATION_TYPE = "organization";

    private static final String SUCCESS = "success";

    private static final String ERROR = "error";

    private static final String PARENT_ORGANIZATION_NOT_IMPORTED_YET = "parentNotImportedYet";

    private static final String RESULT = "result";

    private static final String IN_NEW_VERSION = "inNewVersion";

    private static final String COMMUNITY_ADMINISTRATOR = "communityAdministrator";

    private static final String TRACE = "trace";

    protected CommunityPersister communityPersister = GroupwareManagerRegistry.getCommunityPersister();

    protected WebPersister webPersister = GroupwareManagerRegistry.getWebPersister();

    protected PrivilegePersister privilegePersister = GroupwareManagerRegistry.getPrivilegePersister();

    protected Authorizator authorizator = GroupwareManagerRegistry.getAuthorizator();

    protected WorldManager worldManager = ManagerRegistry.getWorldManager();

    protected ContentManager contentManager = ManagerRegistry.getContentManager();

    protected DirectoryPersister dirPersister = ManagerRegistry.getDirectoryPersister();

    protected LiferayHelper liferayHelper;

    private static Set<CommunityManagementListener> communityManagementListeners = new HashSet<CommunityManagementListener>();

    public Community createCommunity(Community community, GroupwareUser user) {
        return createCommunity(community, null, user);
    }

    /**
     * Creates a community with a specified web.
     * @param community Community to create
     * @param web Community's web. If null default Web is created
     * @param user Administrator user of the community. If null, ignored
     */
    public Community createCommunity(final Community community, final Web web, GroupwareUser user) {
        Location defaultCommunitiesLocation = communityPersister.getDefaultCommunitiesLocation();
        return createCommunity(community, web, user, defaultCommunitiesLocation);
    }

    public Community createCommunity(final Community community, final Web web, GroupwareUser user, Location communitiesLocation) {
        Community communityToCreate;
        try {
            String userId = null;
            if (user != null) userId = user.getId();
            communityToCreate = fireBeforeCreate(userId, community);
        } catch (RollbackCreationException e) {
            throw new GroupwareRuntimeException("Error in listener before creating community. Community not created", e);
        }
        Community createdCommunity = communityPersister.createCommunity(communityToCreate, communitiesLocation);
        if (user != null) {
            addAdminToCommunity(user, createdCommunity);
        }
        try {
            JCRUtil.getNodeById(createdCommunity.getId(), community.getWorkspace());
        } catch (ItemNotFoundException e) {
            throw new GroupwareRuntimeException(e);
        } catch (RepositoryException e) {
            throw new GroupwareRuntimeException(e);
        }
        createWorld(null, createdCommunity);
        createWeb(web, createdCommunity);
        createFileDirectory(createdCommunity);
        if (log.isDebugEnabled()) log.debug("Created community with id " + createdCommunity.getId());
        return createdCommunity;
    }

    public Community createPersonalCommunity(GroupwareUser user) {
        Location defaultCommunitiesLocation = communityPersister.getDefaultCommunitiesLocation();
        return createPersonalCommunity(user, defaultCommunitiesLocation);
    }

    public Community createPersonalCommunity(GroupwareUser user, Location defaultCommunitiesLocation) {
        Community community = new Community();
        String comunityTitle = I18NUtils.getLocalizedMessage("community_admin.personalCommunity.title");
        community.setTitle(new I18NString(comunityTitle, I18NUtils.getThreadLocale()));
        community.setName(user.getId());
        community.setType(CommunityManager.PERSONAL_COMMUNITY_TYPE);
        Community createdCommunity = communityPersister.createCommunity(community, defaultCommunitiesLocation);
        World world = new World();
        world.setTitle(new I18NString(comunityTitle, I18NUtils.getThreadLocale()));
        world.setName(community.getName());
        world.setLiferayGroupId(community.getExternalId());
        createWorld(world, createdCommunity);
        createFileDirectory(createdCommunity);
        Web web = new Web();
        String webTitle = I18NUtils.getLocalizedMessage("community_admin.personalCommunity.titleWeb", new String[] { user.getId() });
        web.setTitle(new I18NString(webTitle, I18NUtils.getThreadLocale()));
        createWeb(web, createdCommunity);
        return createdCommunity;
    }

    public void deleteCommunity(Community community, GroupwareUser user) throws NotAuthorizedException, CommunityNotFoundException {
        communityPersister.deleteCommunity(community);
        fireOnDelete(community);
    }

    public void deleteCommunityById(String communityId, GroupwareUser user) throws NotAuthorizedException, CommunityNotFoundException {
        log.info("Removing the community by id: " + communityId);
        Community community = getCommunityById(communityId);
        deleteCommunity(community, user);
    }

    public void saveCommunity(Community community) {
        communityPersister.saveCommunity(community);
    }

    public Collection<Community> getAllCommunities() {
        return communityPersister.getAllCommunities();
    }

    public Community getCommunityById(String communityId) throws CommunityNotFoundException {
        return communityPersister.getCommunityById(communityId);
    }

    public Community getCommunityByName(String communityName) throws CommunityNotFoundException {
        return communityPersister.searchCommunityByName(communityName);
    }

    @Deprecated
    public Community getPersonalCommunity(String userId) throws CommunityNotFoundException {
        return communityPersister.searchCommunityByName(userId);
    }

    public Community getPersonalCommunity(GroupwareUser groupwareUser) throws CommunityNotFoundException {
        return communityPersister.searchCommunity(groupwareUser.getId(), PERSONAL_COMMUNITY_TYPE);
    }

    public long getCommunitySize(String communityId) throws CommunityNotFoundException {
        Community com = getCommunityById(communityId);
        return com.getCommunitySize();
    }

    public Community getCommunityByExternalId(String externalId) throws CommunityNotFoundException {
        Web web = getWebByExternalId(externalId);
        Community ownerCommunity;
        try {
            ownerCommunity = getOwnerCommunity(web);
        } catch (Exception e) {
            web = removeFromCacheAndRetryByExternalId(externalId, web);
            ownerCommunity = getOwnerCommunity(web);
        }
        ownerCommunity.setCurrentWeb(web);
        return ownerCommunity;
    }

    private Web removeFromCacheAndRetryByExternalId(String externalId, Web web) throws CommunityNotFoundException {
        webPersister.removeWebFromExternalIdCache(web);
        return getWebByExternalId(externalId);
    }

    private Web getWebByExternalId(String externalId) throws CommunityNotFoundException {
        try {
            return webPersister.getWebByExternalId(externalId);
        } catch (WebNotFoundException e) {
            throw new CommunityNotFoundException("Community not found with a web with external ID: " + externalId);
        }
    }

    public Collection<Community> getUserCommunities(GroupwareUser groupwareUser) {
        Set<Community> communities = new HashSet<Community>();
        String role = CommunityPrivilege.MEMBER_ROLE;
        communities.addAll(getImplicitUserCommunities(groupwareUser, role));
        communities.addAll(getExplicitUserCommunities(groupwareUser, role));
        return communities;
    }

    public Collection<Community> getAdministrableUserCommunities(GroupwareUser groupwareUser) {
        Set<Community> communities = new HashSet<Community>();
        String role = CommunityPrivilege.ADMIN_ROLE;
        communities.addAll(getImplicitUserCommunities(groupwareUser, role));
        communities.addAll(getExplicitUserCommunities(groupwareUser, role));
        return communities;
    }

    String[] rules = GroupwareConfig.getImplicitCommunitiesRules();

    @SuppressWarnings("unchecked")
    protected Collection<Community> getImplicitUserCommunities(GroupwareUser user, String role) {
        Collection<Community> implicitCommunities = new ArrayList<Community>();
        for (int i = 0; i < rules.length; i++) {
            String ruleIdentifier = rules[i];
            if (StringUtils.isNotEmpty(ruleIdentifier)) {
                Class<ImplicitCommunitiesRule> ruleClass;
                try {
                    ruleClass = (Class<ImplicitCommunitiesRule>) GroupwareConfig.getImplicitCommunitiesRule(ruleIdentifier);
                } catch (ClassNotFoundException e) {
                    log.warn("Implicit communities rule (" + ruleIdentifier + ") class does not exists: " + e);
                    continue;
                }
                try {
                    ImplicitCommunitiesRule implicitCommunitiesRule = (ImplicitCommunitiesRule) ruleClass.newInstance();
                    List<Community> communities = implicitCommunitiesRule.getCommunities(user, role);
                    if (communities != null) implicitCommunities.addAll(communities);
                } catch (InstantiationException e) {
                    log.warn("Implicit communities rule (" + ruleIdentifier + ") does not instantiate: " + e);
                } catch (IllegalAccessException e) {
                    log.warn("Implicit communities rule (" + ruleIdentifier + ") does not instantiate: " + e);
                } catch (ClassCastException e) {
                    log.warn("Implicit communities rule (" + ruleIdentifier + ") class [" + ruleClass.getName() + "] does not extends " + ImplicitCommunitiesRule.class.getName());
                } catch (ImplicitCommunitiesRuleException e) {
                    log.error("Error obtaining communities for implicit rule: " + ruleClass.getName());
                }
            }
        }
        return implicitCommunities;
    }

    protected Collection<Community> getExplicitUserCommunities(GroupwareUser user, String role) {
        Collection<Community> communities = new ArrayList<Community>();
        CommunityPrivilege prototype = new CommunityPrivilege();
        prototype.setUserId(user.getId());
        prototype.setRole(role);
        Collection<CommunityPrivilege> privileges = privilegePersister.searchByPrototype(prototype);
        for (CommunityPrivilege privilege : privileges) {
            try {
                Community community = communityPersister.getCommunityById(privilege.getCommunityId());
                communities.add(community);
            } catch (CommunityNotFoundException e) {
                String message = "Privilege found for user " + privilege.getUserId() + " with role " + privilege.getRole() + " in community " + privilege.getCommunityId() + " but community was not found";
                log.debug(message);
            }
        }
        return communities;
    }

    public Web getCommunityWeb(String communityId) throws CommunityNotFoundException {
        Community community = getCommunityById(communityId);
        return webPersister.getCommunityWeb(community);
    }

    public Collection<Web> getCommunityWebs(String communityId) throws CommunityNotFoundException {
        Community community = getCommunityById(communityId);
        return webPersister.getCommunityWebs(community);
    }

    public World getCommunityWorld(Community community) throws CommunityNotFoundException {
        Location worldsLocation = communityPersister.getWorldsLocation(community);
        try {
            Collection<World> worlds = worldManager.getWorldsByLocation(worldsLocation);
            if (worlds.isEmpty()) {
                return createWorld(null, community);
            } else {
                return (World) worlds.iterator().next();
            }
        } catch (RepositoryException e) {
            throw new GroupwareRuntimeException("Error accessing the repository.", e);
        }
    }

    public World getPersonalWorld(GroupwareUser user) throws CommunityNotFoundException {
        return getPersonalCommunity(user).getWorld();
    }

    public DirectoryFolder getCommunityDirectory(Community community) throws CommunityNotFoundException {
        Location fileDirLocation = communityPersister.getFileDirLocation(community);
        try {
            Collection<DirectoryFolder> rootFolders = dirPersister.getDirectoriesByLocation(fileDirLocation);
            if (rootFolders.isEmpty()) {
                DirectoryFolder createdFileDirectory = createFileDirectory(community);
                JCRUtil.currentSession(community.getWorkspace()).save();
                return createdFileDirectory;
            } else {
                return (DirectoryFolder) rootFolders.iterator().next();
            }
        } catch (RepositoryException e) {
            throw new GroupwareRuntimeException("Error accessing the repository.", e);
        }
    }

    public void saveWeb(Web web) {
        webPersister.saveWeb(web);
    }

    private World createWorld(World world, Community community) {
        if (world == null) {
            world = new World();
            world.setTitle(community.getTitle());
            world.setName(community.getName());
            world.setLiferayGroupId(community.getExternalId());
        }
        Location worldsLocation = communityPersister.getWorldsLocation(community);
        return worldManager.createWorld(worldsLocation, world);
    }

    private DirectoryFolder createFileDirectory(Community community) {
        Location fileDirLocation = communityPersister.getFileDirLocation(community);
        DirectoryFolder itemFolder = new DirectoryFolder(ROOT_FOLDER_NAME);
        return dirPersister.addRootFolder(fileDirLocation, itemFolder);
    }

    public Web createWeb(Community community) {
        return createWeb(null, community);
    }

    private Web createWeb(Web web, Community community) {
        if (web == null) {
            web = new Web();
            web.setTitle(community.getTitle());
        }
        if (web.getExternalWebId() == null) {
            web.setExternalWebId(community.getExternalId());
        }
        Location websLocation = communityPersister.getWebsLocation(community.getId(), community.getWorkspace());
        return webPersister.createWeb(web, websLocation);
    }

    protected void addAdminToCommunity(GroupwareUser user, Community community) throws GroupwareRuntimeException {
        CommunityPrivilege adminprivilege = new CommunityPrivilege(user.getId(), CommunityPrivilege.ADMIN_ROLE, community.getId());
        privilegePersister.save(adminprivilege);
        CommunityPrivilege memberprivilege = new CommunityPrivilege(user.getId(), CommunityPrivilege.MEMBER_ROLE, community.getId());
        privilegePersister.save(memberprivilege);
    }

    public void giveAdminRole(String userId, GroupwareUser requester, Community community) throws GroupwareRuntimeException, NotAuthorizedException, NotMemberException {
        authorizator.assertAdminAuthorization(requester, community);
        CommunityPrivilege privilege = new CommunityPrivilege(userId, CommunityPrivilege.ADMIN_ROLE, community.getId());
        privilegePersister.save(privilege);
    }

    public void removeAdminRole(String userId, GroupwareUser requester, Community community) throws GroupwareRuntimeException, NotAuthorizedException, NotMemberException, NotEnoughAdminsException {
        authorizator.assertAdminAuthorization(requester, community);
        CommunityMembers communityMembers = searchMembers(community);
        if (communityMembers.getExplicitAdminMembers().size() > 1) {
            Iterator<CommunityPrivilege> it = communityMembers.findExplicitPrivileges(userId).iterator();
            while (it.hasNext()) {
                CommunityPrivilege privilege = it.next();
                if (privilege.isAdminPrivilege()) {
                    privilegePersister.delete(privilege);
                }
            }
        } else {
            throw new NotEnoughAdminsException("There must be an administrator at least");
        }
    }

    public void addMember(String userId, GroupwareUser requester, Community community) throws GroupwareRuntimeException, NotAuthorizedException, AlreadyMemberException {
        authorizator.assertAdminAuthorization(requester, community);
        authorizator.assertUserIsNotMember(userId, community);
        CommunityPrivilege privilege = new CommunityPrivilege(userId, CommunityPrivilege.MEMBER_ROLE, community.getId());
        privilegePersister.save(privilege);
    }

    public void removeMember(String userId, GroupwareUser requester, Community community) throws GroupwareRuntimeException, NotAuthorizedException, NotMemberException {
        authorizator.assertAdminAuthorization(requester, community);
        CommunityMembers members = searchMembers(community);
        authorizator.assertUserIsMember(userId, members);
        Iterator<CommunityPrivilege> it = members.findExplicitPrivileges(userId).iterator();
        while (it.hasNext()) {
            CommunityPrivilege privilege = it.next();
            log.info("DELETING privilege: " + privilege);
            privilegePersister.delete(privilege);
        }
    }

    public CommunityMembers searchMembers(GroupwareUser requester, Community community) throws GroupwareRuntimeException, NotAuthorizedException {
        return searchMembers(community);
    }

    protected CommunityMembers searchMembers(Community community) throws GroupwareRuntimeException {
        CommunityPrivilege privilegePrototype = new CommunityPrivilege();
        privilegePrototype.setCommunityId(community.getId());
        Collection<CommunityPrivilege> explicitPrivileges = privilegePersister.searchByPrototype(privilegePrototype);
        Collection<CommunityPrivilege> implicitPrivileges = implicitCommunityPrivileges(community);
        CommunityMembers members = new CommunityMembers(community, explicitPrivileges, implicitPrivileges);
        return members;
    }

    private Collection<CommunityPrivilege> implicitCommunityPrivileges(Community community) {
        String[] rules = GroupwareConfig.getImplicitCommunitiesRules();
        Collection<CommunityPrivilege> implicitCommunityPrivileges = new ArrayList<CommunityPrivilege>();
        for (int i = 0; i < rules.length; i++) {
            String ruleIdentifier = rules[i];
            Class<? extends ImplicitCommunitiesRule> ruleClass;
            try {
                ruleClass = GroupwareConfig.getImplicitCommunitiesRule(ruleIdentifier);
            } catch (ClassNotFoundException e) {
                log.warn("Implicit communities rule (" + ruleIdentifier + ") class does not exists: " + e);
                continue;
            }
            try {
                ImplicitCommunitiesRule implicitCommunitiesRule = ruleClass.newInstance();
                CommunityMembershipList communityMembershipList = implicitCommunitiesRule.getMemberships(community);
                if (communityMembershipList != null && communityMembershipList.size() > 0) {
                    implicitCommunityPrivileges.addAll(transformToPrivileges(communityMembershipList));
                }
            } catch (InstantiationException e) {
                log.warn("Implicit communities rule (" + ruleIdentifier + ") does not instantiate: " + e);
            } catch (IllegalAccessException e) {
                log.warn("Implicit communities rule (" + ruleIdentifier + ") does not instantiate: " + e);
            } catch (ClassCastException e) {
                log.warn("Implicit communities rule (" + ruleIdentifier + ") class [" + ruleClass.getName() + "] does not extends " + ImplicitCommunitiesRule.class.getName());
            } catch (ImplicitCommunitiesRuleException e) {
                log.error("Error obtaining communities for implicit rule: " + ruleClass.getName());
            }
        }
        return implicitCommunityPrivileges;
    }

    private Collection<CommunityPrivilege> transformToPrivileges(CommunityMembershipList communityMembershipList) {
        Collection<CommunityPrivilege> communityPrivileges = new ArrayList<CommunityPrivilege>();
        CommunityMembershipIterator it = communityMembershipList.communityMembershipIterator();
        while (it.hasNext()) {
            CommunityMembership communityMembership = it.nextCommunityMembership();
            CommunityPrivilege communityPrivilege = transformToPrivilege(communityMembership);
            communityPrivileges.add(communityPrivilege);
        }
        return communityPrivileges;
    }

    private CommunityPrivilege transformToPrivilege(CommunityMembership communityMembership) {
        CommunityPrivilege communityPrivilege = new CommunityPrivilege(communityMembership.getUserId(), communityMembership.getRole(), communityMembership.getCommunity().getId());
        return communityPrivilege;
    }

    public Community getOwnerCommunity(World world) throws GroupwareRuntimeException {
        return communityPersister.searchCommunityByWorld(world);
    }

    public Community getOwnerCommunity(Web web) {
        return communityPersister.searchCommunityByWeb(web);
    }

    public Community getOwnerCommunity(DirectoryFolder folder) {
        return communityPersister.searchCommunityByFolder(folder);
    }

    public void addCommunityManagementListener(CommunityManagementListener communityManagementListener) {
        communityManagementListeners.add(communityManagementListener);
    }

    public boolean removeCommunityManagementListener(CommunityManagementListener communityManagementListener) {
        return communityManagementListeners.remove(communityManagementListener);
    }

    private Community fireBeforeCreate(String userId, Community community) throws RollbackCreationException {
        Iterator<CommunityManagementListener> it = communityManagementListeners.iterator();
        while (it.hasNext()) {
            CommunityManagementListener communityManagementListener = it.next();
            try {
                community = communityManagementListener.beforeCreateCommunity(userId, community);
            } catch (RuntimeException e) {
                log.warn("Error on listener before creation: " + e);
            }
        }
        return community;
    }

    private void fireOnDelete(Community community) {
        Iterator<CommunityManagementListener> it = communityManagementListeners.iterator();
        while (it.hasNext()) {
            CommunityManagementListener communityManagementListener = it.next();
            try {
                communityManagementListener.onDeleteCommunity(community);
            } catch (RuntimeException e) {
                log.warn("Error on listener [" + communityManagementListener + "] on delete: " + e);
            }
        }
    }

    public Web addWeb(Community community, Web newWeb) {
        Location websLocation = communityPersister.getWebsLocation(community.getId(), community.getWorkspace());
        if (newWeb.getExternalWebId() == null) {
            String localizedTitle = newWeb.getName();
            if (StringUtils.isEmpty(localizedTitle)) {
                localizedTitle = I18NUtils.localize(community.getTitle());
            }
            String groupName = community.getName() + "web" + (community.getWebs().size() + 1);
            String groupId;
            try {
                if (log.isDebugEnabled()) log.debug("Add new group with name [" + groupName + "].");
                groupId = getLiferayHelper().addGroup(null, groupName, localizedTitle, community.getType(), null);
            } catch (LiferayDuplicateGroupException e) {
                log.warn("Group with name [" + groupName + "] exists yet.");
                try {
                    if (log.isDebugEnabled()) log.debug("Set new web the groupId of group with name [" + groupName + "].");
                    groupId = getLiferayHelper().getGroupIdByName(groupName);
                } catch (LiferaySystemException e1) {
                    throw new GroupwareRuntimeException(e1);
                } catch (LiferayPortalException e1) {
                    throw new GroupwareRuntimeException(e1);
                } catch (LiferayRemoteException e1) {
                    throw new GroupwareRuntimeException(e1);
                }
            } catch (LiferayPortalException e) {
                throw new GroupwareRuntimeException(e);
            } catch (LiferaySystemException e) {
                throw new GroupwareRuntimeException(e);
            } catch (LiferayRemoteException e) {
                throw new GroupwareRuntimeException(e);
            }
            newWeb.setExternalWebId(groupId);
        }
        if (log.isDebugEnabled()) log.debug("Save the new web in web persister.");
        newWeb = webPersister.createWeb(newWeb, websLocation);
        if (log.isDebugEnabled()) log.debug("Add the new web to community.");
        community.getWebs().add(newWeb);
        return newWeb;
    }

    public LiferayHelper getLiferayHelper() {
        if (liferayHelper == null) {
            liferayHelper = LiferayHelperFactory.getLiferayHelper();
        }
        return liferayHelper;
    }

    public void deleteWebById(String webId) {
        Web web = webPersister.getWebById(webId);
        Community community = getOwnerCommunity(web);
        if (web.getExternalWebId() != null) try {
            getLiferayHelper().deleteGroup(web.getExternalWebId());
        } catch (LiferayNoSuchGroupException e) {
        } catch (LiferayPortalException e) {
        } catch (LiferaySystemException e) {
            throw new GroupwareRuntimeException(e);
        } catch (LiferayRemoteException e) {
            throw new GroupwareRuntimeException(e);
        }
        webPersister.deleteWeb(web);
        if (community.getWebs().size() == 0) {
            Web defectWeb = new Web();
            defectWeb.setTitle(community.getTitle());
            defectWeb.setWebLink(new NoWebLink());
            addWeb(community, defectWeb);
        }
    }

    public void setLiferayHelper(LiferayHelper liferayHelper) {
        this.liferayHelper = liferayHelper;
    }

    public byte[] exportCommunityData(String communityId) throws RepositoryException, IOException {
        Community community;
        try {
            community = getCommunityById(communityId);
        } catch (CommunityNotFoundException e1) {
            throw new GroupwareRuntimeException("Community to export not found");
        }
        String contentPath = JCRUtil.getNodeById(communityId, community.getWorkspace()).getPath();
        try {
            File zipOutFilename = File.createTempFile("exported-community", ".zip.tmp");
            TemporaryFilesHandler.register(null, zipOutFilename);
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipOutFilename));
            File file = File.createTempFile("exported-community", null);
            TemporaryFilesHandler.register(null, file);
            FileOutputStream fos = new FileOutputStream(file);
            exportCommunitySystemView(community, contentPath, fos);
            fos.close();
            File propertiesFile = File.createTempFile("exported-community-properties", null);
            TemporaryFilesHandler.register(null, propertiesFile);
            FileOutputStream fosProperties = new FileOutputStream(propertiesFile);
            fosProperties.write(("communityId=" + communityId).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("externalId=" + community.getExternalId()).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("title=" + I18NUtils.localize(community.getTitle())).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("communityType=" + community.getType()).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("communityName=" + community.getName()).getBytes());
            fosProperties.close();
            FileInputStream finProperties = new FileInputStream(propertiesFile);
            byte[] bufferProperties = new byte[4096];
            out.putNextEntry(new ZipEntry("properties"));
            int readProperties = 0;
            while ((readProperties = finProperties.read(bufferProperties)) > 0) {
                out.write(bufferProperties, 0, readProperties);
            }
            finProperties.close();
            FileInputStream fin = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            out.putNextEntry(new ZipEntry("xmlData"));
            int read = 0;
            while ((read = fin.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            fin.close();
            out.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fisZipped = new FileInputStream(zipOutFilename);
            byte[] bufferOut = new byte[4096];
            int readOut = 0;
            while ((readOut = fisZipped.read(bufferOut)) > 0) {
                baos.write(bufferOut, 0, readOut);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            String errorMessage = "Error exporting backup data, for comunnity with id " + communityId;
            log.error(errorMessage, e);
            throw new CMSRuntimeException(errorMessage, e);
        }
    }

    private void exportCommunitySystemView(Community community, String contentPath, FileOutputStream fos) throws IOException, PathNotFoundException, RepositoryException {
        JCRUtil.currentSession(community.getWorkspace()).exportSystemView(contentPath, fos, false, false);
    }

    public String importPersonalCommunity(GroupwareUser groupwareUser, ZipInputStream zis) throws IOException, RepositoryException {
        Community formerPersonalCommunity = null;
        try {
            formerPersonalCommunity = getPersonalCommunity(groupwareUser);
        } catch (CommunityNotFoundException e) {
        } catch (Throwable t) {
            log.error("Error finding former personal community", t);
        }
        try {
            ImportCommunityResult doImportCommunity = communityPersister.doImportCommunity(zis);
            Community community = doImportCommunity.getCommunity();
            setPersonalCommunityData(community, groupwareUser);
            if (formerPersonalCommunity != null) try {
                deleteCommunityById(formerPersonalCommunity.getId(), groupwareUser);
            } catch (NotAuthorizedException e) {
            } catch (CommunityNotFoundException e) {
            }
            log.info("Importing personal community with result: " + doImportCommunity.getStatus().toString());
            return doImportCommunity.getUserType();
        } catch (IOException e1) {
            log.error("Error importing personal community. Error with data zip: " + e1);
            throw e1;
        } catch (RepositoryException e1) {
            log.error("Error importing personal community. Error with repository: " + e1);
            throw e1;
        } catch (CommunityAlreadyInRepositoryException e) {
            throw new GroupwareRuntimeException("Unexpected exception ", e);
        }
    }

    public Properties unattendedCommunityImport(ZipInputStream zis) throws IOException {
        String communityAdministrator = "";
        Properties result = new Properties();
        try {
            ImportCommunityResult importCommunityResult = communityPersister.doImportCommunity(zis);
            communityAdministrator = importCommunityResult.getCommunityAdministrator();
            GroupwareUser groupwareUser = getLiferayHelper().getGroupwareUserByScreenName(communityAdministrator);
            String communityType = importCommunityResult.getCommunityType();
            Community importedCommunity = importCommunityResult.getCommunity();
            if (groupwareUser == null) {
                groupwareUser = getLiferayHelper().getGroupwareUserByScreenName(getLiferayHelper().getDefaultAdminScreenName());
                result.put(RESULT, SUCCESS);
                result.put(IN_NEW_VERSION, Boolean.FALSE);
                result.put(COMMUNITY_ADMINISTRATOR, communityAdministrator);
            } else {
                result.put(RESULT, SUCCESS);
                result.put(IN_NEW_VERSION, Boolean.TRUE);
                result.put(COMMUNITY_ADMINISTRATOR, communityAdministrator);
            }
            log.info("Checking if community must be imported as organization");
            if (mustBeImportedAsOrganization(communityType)) {
                log.info("Community must be imported as organization");
                importAsOrganization(groupwareUser, importedCommunity, importCommunityResult);
            } else {
                importAsCommunity(groupwareUser, importedCommunity, importCommunityResult.getWebName());
            }
            return result;
        } catch (CommunityAlreadyInRepositoryException e) {
            communityAdministrator = (String) e.getProperties().get("communityAdministrator");
            return returnExceptionResult(communityAdministrator, result, e, ERROR);
        } catch (ParentOrganizationNotImportedException e) {
            communityAdministrator = (String) e.getProperties().get("communityAdministrator");
            return returnExceptionResult(communityAdministrator, result, e, PARENT_ORGANIZATION_NOT_IMPORTED_YET);
        } catch (CommunityRuntimeException e) {
            communityAdministrator = (String) e.getProperties().get("communityAdministrator");
            return returnExceptionResult(communityAdministrator, result, e, ERROR);
        } catch (SystemException e) {
            return returnExceptionResult(communityAdministrator, result, e, ERROR);
        } catch (PortalException e) {
            return returnExceptionResult(communityAdministrator, result, e, ERROR);
        } catch (RepositoryException e) {
            return returnExceptionResult(communityAdministrator, result, e, ERROR);
        }
    }

    private Properties returnExceptionResult(String communityAdministrator, Properties result, Exception e, String resultType) {
        result.clear();
        result.put(RESULT, resultType);
        result.put(COMMUNITY_ADMINISTRATOR, communityAdministrator);
        StringWriter out = new StringWriter();
        e.printStackTrace(new PrintWriter(out));
        result.put(TRACE, out.toString());
        return result;
    }

    private boolean mustBeImportedAsOrganization(String communityType) {
        return GroupwareConfig.isImportedAsOrganization(communityType);
    }

    private void importAsOrganization(GroupwareUser groupwareUser, Community importedCommunity, ImportCommunityResult importCommunityResult) throws SystemException, PortalException {
        String type = importCommunityResult.getCommunityType();
        boolean isOrganizationTypeRootable = getLiferayHelper().getIsOrganizationTypeRootable(type);
        log.info("Creating organization for imported community");
        Organization createdOrganizationFromCommunityData;
        if (!isOrganizationTypeRootable) {
            createdOrganizationFromCommunityData = createSubOrganization(groupwareUser, importedCommunity, importCommunityResult);
        } else createdOrganizationFromCommunityData = getLiferayHelper().createOrganizationFromCommunityData(importedCommunity, groupwareUser, importCommunityResult.getWebName(), importedCommunity.getType());
        setCustomAttributes(importedCommunity, importCommunityResult, createdOrganizationFromCommunityData);
        String groupId = String.valueOf(createdOrganizationFromCommunityData.getGroup().getGroupId());
        String name = createdOrganizationFromCommunityData.getName();
        String description = createdOrganizationFromCommunityData.getComments();
        log.info("Assigning imported community to organization with groupId " + groupId);
        updateCommunityAndWeb(groupId, importedCommunity, name, description, Boolean.TRUE);
    }

    private Organization createSubOrganization(GroupwareUser groupwareUser, Community importedCommunity, ImportCommunityResult importCommunityResult) throws PortalException, SystemException {
        Organization parent = importCommunityResult.findParentOrganization(getLiferayHelper());
        String parentType = parent.getType();
        String type = importCommunityResult.getCommunityType();
        List<String> availableParentTypes = getLiferayHelper().getAvailableParentTypes(type);
        if (!availableParentTypes.contains(parentType)) {
            throw new GroupwareRuntimeException("Parent type " + parentType + " is not elegible for an organization of type [" + type + "]. It should be one of [" + availableParentTypes + "]");
        }
        return getLiferayHelper().createSubOrganizationFromCommunityData(importedCommunity, groupwareUser, importCommunityResult.getWebName(), importedCommunity.getType(), parent);
    }

    private void setCustomAttributes(Community importedCommunity, ImportCommunityResult importCommunityResult, Organization createOrganizationFromCommunityData) {
        Map<String, String> customAttributes = customAttributesForOrganization(importedCommunity.getType(), importCommunityResult);
        getLiferayHelper().setCustomAttributes(createOrganizationFromCommunityData, customAttributes);
    }

    private Map<String, String> customAttributesForOrganization(String type, ImportCommunityResult importCommunityResult) {
        Map<String, String> customAttributesValues = new HashMap<String, String>();
        List<String> customAttributes = GroupwareConfig.customAttributesForOrganizationType(type);
        for (String customAttribute : customAttributes) {
            String communityZipProperty = GroupwareConfig.getCommunityZipMappingForCustomAttribute(customAttribute);
            customAttributesValues.put(customAttribute, importCommunityResult.getProperty(communityZipProperty));
        }
        return customAttributesValues;
    }

    private void importAsCommunity(GroupwareUser groupwareUser, Community importedCommunity, String webName) throws PortalException, SystemException {
        log.info("Creating group in Liferay for this imported community");
        Group createGroupFromCommunityData = getLiferayHelper().createGroupFromCommunityData(importedCommunity, groupwareUser, webName);
        String groupId = String.valueOf(createGroupFromCommunityData.getGroupId());
        log.info("Group created: " + groupId);
        String name = createGroupFromCommunityData.getName();
        String description = createGroupFromCommunityData.getDescription();
        log.info("Assigning imported community to group " + groupId);
        updateCommunityAndWeb(groupId, importedCommunity, name, description, Boolean.FALSE);
    }

    private void updateCommunityAndWeb(String groupId, Community importedCommunity, String name, String description, boolean isOrganization) {
        importedCommunity.setName(name);
        importedCommunity.setDescription(new I18NString(description, I18NUtils.getThreadLocale()));
        importedCommunity.setTitle(new I18NString(name, I18NUtils.getThreadLocale()));
        importedCommunity.setExternalId(groupId);
        if (isOrganization) {
            importedCommunity.setType(LIFERAY_ORGANIZATION_TYPE);
        } else {
            importedCommunity.setType(LIFERAY_COMMUNITY_TYPE);
        }
        Web web = importedCommunity.getWeb();
        web.setExternalWebId(groupId);
        saveWeb(web);
        log.info("Saving imported community with externalId " + groupId);
        saveCommunity(importedCommunity);
        log.info("Saved imported community with externalId " + groupId);
    }

    protected void setPersonalCommunityData(Community community, GroupwareUser groupwareUser) {
        throw new RuntimeException("Method not implemented");
    }

    @Override
    public boolean isParentOrganizationImported(Properties properties) {
        boolean result = Boolean.TRUE;
        try {
            if (properties.containsKey("communityType")) {
                String communityType = properties.getProperty("communityType");
                if (mustBeImportedAsOrganization(communityType)) {
                    boolean isOrganizationTypeRootable = getLiferayHelper().getIsOrganizationTypeRootable(communityType);
                    if (!isOrganizationTypeRootable) {
                        String parentOrganizationValue = properties.getProperty("parentOrganizationCode");
                        String parentOrganizationAttribute = properties.getProperty("parentOrganizationAttribute");
                        if (StringUtils.isNotEmpty(parentOrganizationAttribute)) {
                            if (parentOrganizationAttribute.startsWith(ImportCommunityResult.CUSTOM_ATTRIBUTE_PREFIX)) {
                                String customAttribute = parentOrganizationAttribute.substring(ImportCommunityResult.CUSTOM_ATTRIBUTE_PREFIX.length());
                                List<Organization> findOrganizationByCustomAttribute = getLiferayHelper().findOrganizationByCustomAttribute(customAttribute, parentOrganizationValue);
                                if (findOrganizationByCustomAttribute.size() == 0) {
                                    result = Boolean.FALSE;
                                }
                            }
                        }
                    }
                }
            }
        } catch (PortalException e) {
        } catch (SystemException e) {
        }
        return result;
    }
}
