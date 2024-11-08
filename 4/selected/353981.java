package au.edu.uq.itee.eresearch.dimer.core.security;

import java.util.ArrayList;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.security.auth.Subject;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomAccessManager implements AccessManager {

    private static Logger logger = LoggerFactory.getLogger(CustomAccessManager.class);

    private boolean initialized;

    private Session systemSession;

    private Session session;

    private HierarchyManager hierMgr;

    private NamePathResolver resolver;

    private Subject subject;

    private boolean isAnonymous;

    private boolean isUser;

    private boolean isAdmin;

    private boolean isSystem;

    private static int READ = 0x01;

    private static int WRITE = 0x02;

    private static int REMOVE = 0x04;

    public void init(AMContext context) throws AccessDeniedException, Exception {
        init(context, null, null);
    }

    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        this.systemSession = CustomSystemSession.create(context.getSession().getRepository(), context.getWorkspaceName());
        this.session = context.getSession();
        this.hierMgr = context.getHierarchyManager();
        this.resolver = context.getNamePathResolver();
        this.subject = context.getSubject();
        this.isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        this.isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();
        this.isAdmin = !subject.getPrincipals(AdminPrincipal.class).isEmpty();
        this.isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
        this.initialized = true;
    }

    public void close() throws Exception {
        checkInitialized();
        this.initialized = false;
        this.isSystem = false;
        this.isAdmin = false;
        this.isUser = false;
        this.isAnonymous = false;
        this.subject = null;
        this.resolver = null;
        this.hierMgr = null;
        this.systemSession.logout();
        this.systemSession = null;
    }

    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public boolean isGranted(ItemId id, int permissions) throws ItemNotFoundException, RepositoryException {
        int newPermissions = 0x00;
        if ((permissions & READ) != 0x00) {
            newPermissions |= Permission.READ;
        }
        if ((permissions & WRITE) != 0x00) {
            newPermissions |= Permission.ADD_NODE;
            newPermissions |= Permission.SET_PROPERTY;
        }
        if ((permissions & REMOVE) != 0x00) {
            newPermissions |= Permission.REMOVE_NODE;
            newPermissions |= Permission.REMOVE_PROPERTY;
        }
        return isGranted(hierMgr.getPath(id), newPermissions);
    }

    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        try {
            if (!absPath.isAbsolute() || !absPath.isCanonical()) {
                throw new RepositoryException("Absolute canonical path expected");
            }
            checkInitialized();
            String jcrPath = resolver.getJCRPath(absPath);
            logger.debug("Checking {} for {}.", jcrPath, permissions);
            if (isSystem || isAdmin) {
                return true;
            }
            if (absPath.isDescendantOf(resolver.getQPath("/projects"))) {
                if (!systemSession.itemExists(resolver.getJCRPath(absPath))) {
                    if (absPath.getDepth() == 2) {
                        return (isAnonymous && permissions == Permission.READ) || (isUser && (permissions & ~(Permission.READ | Permission.ADD_NODE)) == 0x00);
                    } else if (!systemSession.itemExists(resolver.getJCRPath(absPath.subPath(0, 3)))) {
                        return (isAnonymous && permissions == Permission.READ) || isUser;
                    } else {
                        return canAccessProject(absPath, permissions);
                    }
                } else {
                    Item item = systemSession.getItem(resolver.getJCRPath(absPath));
                    if (!item.isNode() && absPath.getDepth() == 2) {
                        return permissions == Permission.READ;
                    } else {
                        return canAccessProject(absPath, permissions);
                    }
                }
            } else if (absPath.isDescendantOf(resolver.getQPath("/users"))) {
                if (!systemSession.itemExists(resolver.getJCRPath(absPath))) {
                    if (absPath.getDepth() == 2) {
                        return (permissions & ~(Permission.READ | Permission.ADD_NODE)) == 0x00;
                    } else if (!systemSession.itemExists(resolver.getJCRPath(absPath.subPath(0, 3)))) {
                        return true;
                    } else {
                        return canAccessUser(absPath, permissions);
                    }
                } else {
                    Item item = systemSession.getItem(resolver.getJCRPath(absPath));
                    if (!item.isNode() && absPath.getDepth() == 2) {
                        return permissions == Permission.READ;
                    } else {
                        return canAccessUser(absPath, permissions);
                    }
                }
            } else if (absPath.isDescendantOf(resolver.getQPath("/groups"))) {
                if (!systemSession.itemExists(resolver.getJCRPath(absPath))) {
                    if (absPath.getDepth() == 2) {
                        return (permissions & ~(Permission.READ | Permission.ADD_NODE)) == 0x00;
                    } else if (!systemSession.itemExists(resolver.getJCRPath(absPath.subPath(0, 3)))) {
                        return true;
                    } else {
                        return canAccessGroup(absPath, permissions);
                    }
                } else {
                    Item item = systemSession.getItem(resolver.getJCRPath(absPath));
                    if (!item.isNode() && absPath.getDepth() == 2) {
                        return permissions == Permission.READ;
                    } else {
                        return canAccessGroup(absPath, permissions);
                    }
                }
            } else if (absPath.isDescendantOf(resolver.getQPath("/citations"))) {
                return (permissions == Permission.READ) || (isSystem || isAdmin);
            } else {
                return permissions == Permission.READ;
            }
        } catch (Exception exception) {
            throw new RepositoryException(exception);
        }
    }

    private boolean userInUsers(Node user, Property users) throws RepositoryException {
        for (Value userValue : users.getValues()) {
            if (userValue.getString().equals(user.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private boolean userInGroups(Node user, Property groups) throws RepositoryException {
        for (Value groupValue : groups.getValues()) {
            Node group = systemSession.getNodeByIdentifier(groupValue.getString());
            for (Value userValue : group.getProperty("managers").getValues()) {
                if (userValue.getString().equals(user.getIdentifier())) {
                    return true;
                }
            }
            for (Value userValue : group.getProperty("members").getValues()) {
                if (userValue.getString().equals(user.getIdentifier())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean subordinateInAccessors(ArrayList<Node> subordinates, Node accessors) throws RepositoryException {
        for (Node subordinate : subordinates) {
            if (userInAccessors(subordinate, accessors)) {
                return true;
            }
        }
        return false;
    }

    private boolean userInAccessors(Node user, Node accessors) throws RepositoryException {
        return userInUsers(user, accessors.getProperty("users")) || userInGroups(user, accessors.getProperty("groups"));
    }

    private boolean canAccessProject(Path absPath, int permissions) throws Exception {
        Node project = (Node) systemSession.getItem(resolver.getJCRPath(absPath.subPath(0, 3)));
        if (isUser) {
            Node user = (Node) systemSession.getItem("/users/" + session.getUserID());
            ArrayList<Node> subordinates = new ArrayList<Node>();
            @SuppressWarnings("deprecation") Query query = systemSession.getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user)[\n" + "  @supervisors = '" + user.getIdentifier().replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", Query.XPATH);
            NodeIterator results = query.execute().getNodes();
            while (results.hasNext()) {
                subordinates.add(results.nextNode());
            }
            if (userInAccessors(user, project.getNode("managers")) || subordinateInAccessors(subordinates, project.getNode("managers"))) {
                return true;
            }
            if (absPath.getLength() == 3) {
                if ((permissions & Permission.REMOVE_NODE) != 0x00) {
                    return false;
                }
            }
            if (absPath.getLength() == 4) {
                Name name = absPath.getNameElement().getName();
                if (name.equals(resolver.getQName("published")) || name.equals(resolver.getQName("managers")) || name.equals(resolver.getQName("writers")) || name.equals(resolver.getQName("readers"))) {
                    return permissions == Permission.READ;
                }
            }
            if (userInAccessors(user, project.getNode("writers")) || subordinateInAccessors(subordinates, project.getNode("writers"))) {
                return true;
            }
            if (userInAccessors(user, project.getNode("readers")) || subordinateInAccessors(subordinates, project.getNode("readers"))) {
                if (project.getName().startsWith("filemonitor-") && permissions == Permission.REMOVE_NODE) {
                    return true;
                }
                return permissions == Permission.READ;
            }
        }
        if (project.getProperty("published").getBoolean()) {
            if (absPath.getLength() <= 4) {
                return permissions == Permission.READ;
            } else if (absPath.getElements()[3].getName().equals(resolver.getQName("experiments"))) {
                Item item = systemSession.getItem(resolver.getJCRPath(absPath.subPath(0, 5)));
                if (!item.isNode() || ((Node) item).getProperty("published").getBoolean()) {
                    return permissions == Permission.READ;
                }
            }
        }
        return false;
    }

    private boolean canAccessUser(Path absPath, int permissions) throws Exception {
        Node user = (Node) systemSession.getItem(resolver.getJCRPath(absPath.subPath(0, 3)));
        if (user.getName().equals(session.getUserID())) {
            return true;
        } else if (absPath.getDepth() >= 3 && (absPath.subPath(3, 4).isEquivalentTo(resolver.getQPath("agentURI")) || absPath.subPath(3, 4).isEquivalentTo(resolver.getQPath("agentVisibility")) || absPath.subPath(3, 4).isEquivalentTo(resolver.getQPath("passwordLinkReference")) || absPath.subPath(3, 4).isEquivalentTo(resolver.getQPath("passwordLinkExpiration")) || absPath.subPath(3, 4).isEquivalentTo(resolver.getQPath("password"))) && permissions == Permission.SET_PROPERTY) {
            return true;
        } else {
            return permissions == Permission.READ;
        }
    }

    private boolean canAccessGroup(Path absPath, int permissions) throws Exception {
        Node group = (Node) systemSession.getItem(resolver.getJCRPath(absPath.subPath(0, 3)));
        if (isUser) {
            Node user = (Node) systemSession.getItem("/users/" + session.getUserID());
            if (userInUsers(user, group.getProperty("managers"))) {
                return true;
            }
        }
        return permissions == Permission.READ;
    }

    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        PathFactory factory = PathFactoryImpl.getInstance();
        return isGranted(factory.create(parentPath, childName, true), permissions);
    }

    public boolean canRead(Path itemPath) throws RepositoryException {
        return true;
    }

    public boolean canAccess(String workspaceName) throws RepositoryException {
        return true;
    }

    private void checkInitialized() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    @Override
    public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
    }

    @Override
    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        if (itemPath != null) {
            return isGranted(itemPath, READ);
        } else if (itemId != null) {
            return isGranted(itemId, READ);
        } else {
            return false;
        }
    }
}
