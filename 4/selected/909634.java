package au.edu.uq.itee.eresearch.dimer.webapp.app.util;

import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.apache.jackrabbit.util.ISO9075;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.ViewContext;

public class ProjectUtils {

    public static QueryResult getUsers(Node project, String role) throws RepositoryException {
        @SuppressWarnings("deprecation") Query query = project.getSession().getWorkspace().getQueryManager().createQuery("/jcr:root" + ISO9075.encodePath(project.getPath()) + "/" + ISO9075.encode(role) + "/jcr:deref(@users, '*') order by @lastModified descending", Query.XPATH);
        return query.execute();
    }

    public static QueryResult getGroups(Node project, String role) throws RepositoryException {
        @SuppressWarnings("deprecation") Query query = project.getSession().getWorkspace().getQueryManager().createQuery("/jcr:root" + ISO9075.encodePath(project.getPath()) + "/" + ISO9075.encode(role) + "/jcr:deref(@groups, '*') order by @lastModified descending", Query.XPATH);
        return query.execute();
    }

    public static Node createProject(ViewContext context, Node parent, String name, String title, String description, List<String> managerUserURLs, List<String> writerUserURLs, List<String> readerUserURLs, List<String> managerGroupURLs, List<String> writerGroupURLs, List<String> readerGroupURLs) throws RepositoryException {
        Node project = parent.addNode(name, "project");
        project.setProperty("published", false);
        project.addNode("managers", "accessors");
        project.addNode("writers", "accessors");
        project.addNode("readers", "accessors");
        project.addNode("experiments", "experiments");
        return updateProject(context, project, title, description, managerUserURLs, writerUserURLs, readerUserURLs, managerGroupURLs, writerGroupURLs, readerGroupURLs);
    }

    public static Node updateProject(ViewContext context, Node project, String title, String description) throws RepositoryException {
        project.setProperty("title", title);
        project.setProperty("description", description);
        return project;
    }

    public static Node updateProject(ViewContext context, Node project, String title, String description, List<String> managerUserURLs, List<String> writerUserURLs, List<String> readerUserURLs, List<String> managerGroupURLs, List<String> writerGroupURLs, List<String> readerGroupURLs) throws RepositoryException {
        updateProject(context, project, title, description);
        String[] managerUsers = NodeUtils.buildUUIDArray(context, managerUserURLs);
        if ((managerUsers.length == 0) && !project.getSession().getUserID().equals("admin")) {
            Node currentUser = (Node) project.getSession().getItem("/users/" + project.getSession().getUserID());
            managerUsers = new String[] { currentUser.getIdentifier() };
        }
        String[] writerUsers = NodeUtils.buildUUIDArray(context, writerUserURLs, managerUsers);
        String[] readerUsers = NodeUtils.buildUUIDArray(context, readerUserURLs, managerUsers, writerUsers);
        String[] managerGroups = NodeUtils.buildUUIDArray(context, managerGroupURLs);
        String[] writerGroups = NodeUtils.buildUUIDArray(context, writerGroupURLs, managerGroups);
        String[] readerGroups = NodeUtils.buildUUIDArray(context, readerGroupURLs, managerGroups, writerGroups);
        project.getNode("managers").setProperty("users", managerUsers);
        project.getNode("managers").setProperty("groups", managerGroups);
        project.getNode("writers").setProperty("users", writerUsers);
        project.getNode("writers").setProperty("groups", writerGroups);
        project.getNode("readers").setProperty("users", readerUsers);
        project.getNode("readers").setProperty("groups", readerGroups);
        return project;
    }
}
