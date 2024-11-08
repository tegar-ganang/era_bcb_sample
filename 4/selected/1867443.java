package be.oniryx.lean.session;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import be.oniryx.lean.entity.*;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Name("security")
@Stateful
@Scope(ScopeType.EVENT)
public class SecurityModelImpl implements SecurityModel {

    public static final String PROJECT_ADMIN = "Project Admin";

    public static final String CLIENT_ADMIN = "Client Admin";

    public static final String ALL_ADMIN_TEMPLATE = "All admin";

    public static final String ADMIN_AND_EMPOWERED_MEMBERS_TEMPLATE = "Admin and empowered members";

    public static final String ADMIN_AND_MEMBERS_TEMPLATE = "Admin and members";

    public static final String BUNCH_OF_ROLES_TEMPLATE = "Admin, architect, analyst, developer, tester, documentation writer and graphics artist";

    public static final String ADMIN_AND_READ_ONLY_MEMBERS_TEMPLATE = "Admin and read only members";

    public static final String CUSTOM_TEMPLATE = "Custom";

    public static final String CLIENT_MANAGES_REQUESTS_TEMPLATE = "Client manages requests";

    public static final String CLIENT_SEES_REQUESTS_TEMPLATE = "Client sees requests";

    public static final String CLIENT_HAS_NO_ACCESS_TEMPLATE = "Client has no access";

    public static final List<String> TEMPLATE_NAMES = new LinkedList<String>();

    public static final List<String> CLIENT_TEMPLATE_NAMES = new LinkedList<String>();

    static {
        TEMPLATE_NAMES.add(ALL_ADMIN_TEMPLATE);
        TEMPLATE_NAMES.add(ADMIN_AND_EMPOWERED_MEMBERS_TEMPLATE);
        TEMPLATE_NAMES.add(ADMIN_AND_MEMBERS_TEMPLATE);
        TEMPLATE_NAMES.add(BUNCH_OF_ROLES_TEMPLATE);
        TEMPLATE_NAMES.add(ADMIN_AND_READ_ONLY_MEMBERS_TEMPLATE);
        TEMPLATE_NAMES.add(CUSTOM_TEMPLATE);
        CLIENT_TEMPLATE_NAMES.add(CLIENT_MANAGES_REQUESTS_TEMPLATE);
        CLIENT_TEMPLATE_NAMES.add(CLIENT_SEES_REQUESTS_TEMPLATE);
        CLIENT_TEMPLATE_NAMES.add(CLIENT_HAS_NO_ACCESS_TEMPLATE);
        CLIENT_TEMPLATE_NAMES.add(CUSTOM_TEMPLATE);
    }

    @In
    private EntityManager em;

    @In(create = true)
    private Utilities utilities;

    @In(create = true)
    private GroupRoleManager groupRoleManager;

    @In
    private Events events;

    private List<Resource> resources;

    private Map<Long, Map<Long, Map<String, RoleGrant>>> grants = new HashMap<Long, Map<Long, Map<String, RoleGrant>>>();

    public boolean hasSecurityModel(long projectId) {
        Project p = em.find(Project.class, projectId);
        return (p.getSecurityTemplateName() != null) && (p.getClientTemplateName() != null);
    }

    public boolean hasProjectAdmin(long projectId) {
        return false;
    }

    public boolean hasClientAdmin(long clientId) {
        return false;
    }

    public List<Resource> getResources() {
        if (resources == null) {
            resources = em.createNamedQuery("Resource.getOrderedById").getResultList();
        }
        return resources;
    }

    private RoleGrant getGrant(long groupRoleId, long resourceId, String actionName) {
        Map<Long, Map<String, RoleGrant>> grantsOfGroupRole = grants.get(groupRoleId);
        if (grantsOfGroupRole == null) {
            List<RoleGrant> retrievedGrants = em.createNamedQuery("RoleGrant.findGrantsByGroupRole").setParameter("groupRoleId", groupRoleId).getResultList();
            Long currentResourceId = null;
            grantsOfGroupRole = new HashMap<Long, Map<String, RoleGrant>>();
            Map<String, RoleGrant> currentGrantsOfResource = null;
            for (RoleGrant grant : retrievedGrants) {
                if (currentResourceId == null) {
                    currentResourceId = grant.getResource().getId();
                    currentGrantsOfResource = new HashMap<String, RoleGrant>();
                } else if (currentResourceId != grant.getResource().getId()) {
                    grantsOfGroupRole.put(currentResourceId, currentGrantsOfResource);
                    currentResourceId = grant.getResource().getId();
                    currentGrantsOfResource = new HashMap<String, RoleGrant>();
                }
                currentGrantsOfResource.put(grant.getAction().getName(), grant);
            }
            if (currentGrantsOfResource != null) {
                grantsOfGroupRole.put(currentResourceId, currentGrantsOfResource);
            }
            grants.put(groupRoleId, grantsOfGroupRole);
        }
        Map<String, RoleGrant> grantsOfResource = grantsOfGroupRole.get(resourceId);
        if (grantsOfResource == null) {
            return null;
        }
        return grantsOfResource.get(actionName);
    }

    private Action getActionByName(String actionName) {
        return (Action) em.createNamedQuery("Action.findByName").setParameter("name", actionName).getSingleResult();
    }

    /**
	 * If the grant already exists, this method does nothing. Otherwise, it
	 * creates the grant.
	 * 
	 * @param groupRoleId
	 *            the id of the group role
	 * @param resourceId
	 *            the id of the resource
	 * @param actionName
	 *            the name of the action
	 */
    public void addGrant(long groupRoleId, long resourceId, String actionName) {
        GroupRole role = em.find(GroupRole.class, groupRoleId);
        RoleGrant grant = getGrant(groupRoleId, resourceId, actionName);
        if (grant == null) {
            GroupRole groupRole = em.find(GroupRole.class, groupRoleId);
            Resource resource = em.find(Resource.class, resourceId);
            Action action = getActionByName(actionName);
            grant = new RoleGrant();
            grant.setAction(action);
            grant.setResource(resource);
            grant.setRole(groupRole);
            em.persist(grant);
            if (CLIENT_ADMIN.equals(role.getName())) utilities.setCustomClientSecurityTemplate(role.getGrp().getId()); else utilities.setCustomProjectSecurityTemplate(grant.getRole().getGrp().getId());
            events.raiseEvent("SecurityChanged");
            resetGrantsCache();
        }
    }

    /**
	 * If the grant did not already exists, this method does nothing. Otherwise,
	 * it removes the grant
	 * 
	 * @param groupRoleId
	 *            the id of the group role
	 * @param resourceId
	 *            the id of the resource
	 * @param actionName
	 *            the name of the action
	 */
    public void removeGrant(long groupRoleId, long resourceId, String actionName) {
        GroupRole role = em.find(GroupRole.class, groupRoleId);
        RoleGrant grant = getGrant(groupRoleId, resourceId, actionName);
        if (grant != null) {
            em.remove(grant);
            if (CLIENT_ADMIN.equals(role.getName())) utilities.setCustomClientSecurityTemplate(role.getGrp().getId()); else utilities.setCustomProjectSecurityTemplate(grant.getRole().getGrp().getId());
            events.raiseEvent("SecurityChanged");
            resetGrantsCache();
        }
    }

    public void resetGrantsCache() {
        grants.clear();
    }

    public boolean isGranted(long groupRoleId, long resourceId, String actionName) {
        return getGrant(groupRoleId, resourceId, actionName) != null;
    }

    public List<String> getTemplateNames() {
        return TEMPLATE_NAMES;
    }

    private void clearSecuritySettings(long projectId) {
        Project project = em.find(Project.class, projectId);
        Grp grp = project.getGrp();
        em.refresh(grp);
        for (Iterator<GroupRole> it = grp.getGroupRoles().iterator(); it.hasNext(); ) {
            GroupRole groupRole = it.next();
            if (!groupRole.getName().equals(CLIENT_ADMIN)) {
                em.refresh(groupRole);
                groupRole.getRoleGrants();
                groupRole.getPersonRoles();
                em.remove(groupRole);
                it.remove();
            }
        }
        em.flush();
        events.raiseEvent("SecurityChanged");
    }

    private void createRoleGrant(Action action, Resource resource, GroupRole groupRole) {
        RoleGrant rg = new RoleGrant();
        rg.setAction(action);
        rg.setResource(resource);
        rg.setRole(groupRole);
        em.persist(rg);
    }

    private void createMemberGroupRole(Grp grp, String roleName, Action readAction, Action createAction, Action updateAction, Action deleteAction) {
        GroupRole memberGroupRole = new GroupRole();
        memberGroupRole.setGrp(grp);
        memberGroupRole.setName(roleName);
        em.persist(memberGroupRole);
        for (Resource r : getResources()) {
            createRoleGrant(readAction, r, memberGroupRole);
            if (!Resource.SECURITY_SETTING.equals(r.getName()) && !Resource.PROJECT_MEMBER.equals(r.getName())) {
                createRoleGrant(createAction, r, memberGroupRole);
                createRoleGrant(updateAction, r, memberGroupRole);
                createRoleGrant(deleteAction, r, memberGroupRole);
            }
        }
    }

    /**
	 * Clears the project's security settings and applies the template to have
	 * the template's security settings applied to the project
	 * 
	 * @param templateName
	 *            the name of the template to apply
	 * @param projectId
	 *            the identifier of the project who's security settings should
	 *            be modified
	 */
    public void applyTemplate(String templateName, long projectId) {
        List<Person> projectAdmins = em.createNamedQuery("Project.findProjectAdmins").setParameter("projectId", projectId).getResultList();
        clearSecuritySettings(projectId);
        Project project = em.find(Project.class, projectId);
        Action createAction = getActionByName(Action.CREATE);
        Action readAction = getActionByName(Action.READ);
        Action updateAction = getActionByName(Action.UPDATE);
        Action deleteAction = getActionByName(Action.DELETE);
        GroupRole adminGroupRole = new GroupRole();
        adminGroupRole.setGrp(project.getGrp());
        adminGroupRole.setName(PROJECT_ADMIN);
        em.persist(adminGroupRole);
        for (Resource r : getResources()) {
            createRoleGrant(createAction, r, adminGroupRole);
            createRoleGrant(readAction, r, adminGroupRole);
            createRoleGrant(updateAction, r, adminGroupRole);
            createRoleGrant(deleteAction, r, adminGroupRole);
        }
        if (ALL_ADMIN_TEMPLATE.equals(templateName)) {
        } else if (ADMIN_AND_EMPOWERED_MEMBERS_TEMPLATE.equals(templateName)) {
            GroupRole memberGroupRole = new GroupRole();
            memberGroupRole.setGrp(project.getGrp());
            memberGroupRole.setName("Project member");
            em.persist(memberGroupRole);
            for (Resource r : getResources()) {
                createRoleGrant(readAction, r, memberGroupRole);
                if (!Resource.SECURITY_SETTING.equals(r.getName())) {
                    createRoleGrant(createAction, r, memberGroupRole);
                    createRoleGrant(updateAction, r, memberGroupRole);
                    createRoleGrant(deleteAction, r, memberGroupRole);
                }
            }
        } else if (ADMIN_AND_MEMBERS_TEMPLATE.equals(templateName)) {
            createMemberGroupRole(project.getGrp(), "Project member", readAction, createAction, updateAction, deleteAction);
        } else if (BUNCH_OF_ROLES_TEMPLATE.equals(templateName)) {
            createMemberGroupRole(project.getGrp(), "Architect", readAction, createAction, updateAction, deleteAction);
            createMemberGroupRole(project.getGrp(), "Analyst", readAction, createAction, updateAction, deleteAction);
            createMemberGroupRole(project.getGrp(), "Developer", readAction, createAction, updateAction, deleteAction);
            createMemberGroupRole(project.getGrp(), "Tester", readAction, createAction, updateAction, deleteAction);
            createMemberGroupRole(project.getGrp(), "Documentation writer", readAction, createAction, updateAction, deleteAction);
            createMemberGroupRole(project.getGrp(), "Graphics artist", readAction, createAction, updateAction, deleteAction);
        } else if (ADMIN_AND_READ_ONLY_MEMBERS_TEMPLATE.equals(templateName)) {
            GroupRole memberGroupRole = new GroupRole();
            memberGroupRole.setGrp(project.getGrp());
            memberGroupRole.setName("Project member");
            em.persist(memberGroupRole);
            for (Resource r : getResources()) {
                createRoleGrant(readAction, r, memberGroupRole);
            }
        } else {
        }
        for (Person projectAdmin : projectAdmins) {
            PersonRole pr = new PersonRole();
            pr.setRole(adminGroupRole);
            pr.setPerson(projectAdmin);
            em.persist(pr);
        }
        em.flush();
        events.raiseEvent("SecurityChanged");
    }

    public List<String> getClientTemplateNames() {
        return CLIENT_TEMPLATE_NAMES;
    }

    public void applyClientTemplate(String templateName, long projectId) {
        Project project = em.find(Project.class, projectId);
        GroupRole clientAdminRole = getClientAdminRoleByProject(projectId);
        if (clientAdminRole == null) {
            clientAdminRole = new GroupRole();
            clientAdminRole.setGrp(project.getGrp());
            clientAdminRole.setName(CLIENT_ADMIN);
            em.persist(clientAdminRole);
        } else {
            for (Iterator<RoleGrant> it = clientAdminRole.getRoleGrants().iterator(); it.hasNext(); ) {
                RoleGrant roleGrant = it.next();
                em.remove(roleGrant);
                it.remove();
            }
            em.flush();
        }
        Action createAction = getActionByName(Action.CREATE);
        Action readAction = getActionByName(Action.READ);
        Action updateAction = getActionByName(Action.UPDATE);
        Action deleteAction = getActionByName(Action.DELETE);
        if (CLIENT_MANAGES_REQUESTS_TEMPLATE.equals(templateName)) {
            for (Resource r : getResources()) {
                if (Resource.CLIENT_REQUEST.equals(r.getName())) {
                    createRoleGrant(createAction, r, clientAdminRole);
                    createRoleGrant(readAction, r, clientAdminRole);
                    createRoleGrant(updateAction, r, clientAdminRole);
                    createRoleGrant(deleteAction, r, clientAdminRole);
                }
            }
        } else if (CLIENT_SEES_REQUESTS_TEMPLATE.equals(templateName)) {
            for (Resource r : getResources()) {
                if (Resource.CLIENT_REQUEST.equals(r.getName())) {
                    createRoleGrant(readAction, r, clientAdminRole);
                }
            }
        } else if (CLIENT_HAS_NO_ACCESS_TEMPLATE.equals(templateName)) {
        } else {
        }
        em.flush();
        events.raiseEvent("SecurityChanged");
    }

    public GroupRole getClientAdminRoleByProject(long projectId) {
        Project project = em.find(Project.class, projectId);
        List<GroupRole> roles = em.createNamedQuery("GroupRole.findClientAdminRoleByGroup").setParameter("grpId", project.getGrp().getId()).getResultList();
        if (roles != null && !roles.isEmpty()) return roles.get(0);
        return null;
    }

    public List<GroupRole> getClientGroupRoles(long projectId) {
        Project project = em.find(Project.class, projectId);
        return em.createNamedQuery("GroupRole.findClientAdminRoleByGroup").setParameter("grpId", project.getGrp().getId()).getResultList();
    }

    public GroupRole findGroupRole(String groupRoleName, long projectId) {
        List<GroupRole> roles = em.createNamedQuery("GroupRole.findByNameAndProjectId").setParameter("grpName", groupRoleName).setParameter("prId", projectId).getResultList();
        if (roles != null && !roles.isEmpty()) return roles.get(0);
        return null;
    }

    @Remove
    @Destroy
    public void remove() {
    }
}
