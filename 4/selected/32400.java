package au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml;

import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.buildClearDiv;
import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.xhtml;
import java.util.Arrays;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.jdom2.Content;
import org.jdom2.Element;
import au.edu.uq.itee.eresearch.dimer.core.util.PagedResult;
import au.edu.uq.itee.eresearch.dimer.webapp.app.GlobalProperties;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ProjectUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UserUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.ViewContext;

public class ProjectXHTMLPage extends XHTMLPage {

    private final ViewContext context;

    private final Node project;

    private final PagedResult experimentsResult;

    private static String deleteEnabled = GlobalProperties.properties.getProperty("node.delete.enabled");

    public ProjectXHTMLPage(ViewContext context, Node project, PagedResult experimentsResult) throws RepositoryException {
        super(context, project.getProperty("title").getString());
        this.context = context;
        this.project = project;
        this.experimentsResult = experimentsResult;
        addTab(new Tab("view", "View", buildViewContent()));
        if (context.getAccessPolicy().canUpdateProject(project.getPath())) {
            addTab(new Tab("edit", "Edit", buildEditContent()));
        }
        addBreadcrumbs(project);
    }

    private void addUsersToList(Element usersList, NodeIterator users, String role) throws RepositoryException {
        while (users.hasNext()) {
            Node user = users.nextNode();
            usersList.addContent(new Element("li", xhtml).addContent(new Element("a", xhtml).setAttribute("href", context.getAppPath(user.getPath(), ViewContext.Format.HTML)).setText(UserUtils.getFullNameFirst(user))).addContent(" (" + role + ")"));
        }
    }

    private void addGroupsToList(Element groupsList, NodeIterator groups, String role) throws RepositoryException {
        while (groups.hasNext()) {
            Node group = groups.nextNode();
            groupsList.addContent(new Element("li", xhtml).addContent(new Element("a", xhtml).setAttribute("href", context.getAppPath(group.getPath(), ViewContext.Format.HTML)).setText(group.getProperty("title").getString())).addContent(" (" + role + ")"));
        }
    }

    private Collection<Content> buildViewContent() throws RepositoryException {
        Element content = new Element("div", xhtml).setAttribute("class", "contentbar");
        if (project.hasProperty("description")) {
            content.addContent(new Element("p", xhtml).addContent(project.getProperty("description").getString()));
        }
        content.addContent(new Element("h3", xhtml).setText("Collaborators"));
        NodeIterator managerUsers = ProjectUtils.getUsers(project, "managers").getNodes();
        NodeIterator writerUsers = ProjectUtils.getUsers(project, "writers").getNodes();
        NodeIterator readerUsers = ProjectUtils.getUsers(project, "readers").getNodes();
        NodeIterator managerGroups = ProjectUtils.getGroups(project, "managers").getNodes();
        NodeIterator writerGroups = ProjectUtils.getGroups(project, "writers").getNodes();
        NodeIterator readerGroups = ProjectUtils.getGroups(project, "readers").getNodes();
        if (!managerUsers.hasNext() && !writerUsers.hasNext() && !readerUsers.hasNext() && !managerGroups.hasNext() && !writerGroups.hasNext() && !readerGroups.hasNext()) {
            content.addContent(new Element("p", xhtml).addContent("There are currently no collaborators on this project."));
        } else {
            Element accessorsDiv = new Element("div", xhtml).setAttribute("style", "margin: 1em 0;");
            if (managerUsers.hasNext() || writerUsers.hasNext() || readerUsers.hasNext()) {
                Element usersList = new Element("ul", xhtml).setAttribute("class", "users");
                usersList.setAttribute("style", "margin: 0; float: left;");
                addUsersToList(usersList, managerUsers, "manager");
                addUsersToList(usersList, writerUsers, "writer");
                addUsersToList(usersList, readerUsers, "reader");
                accessorsDiv.addContent(usersList);
            }
            if (managerGroups.hasNext() || writerGroups.hasNext() || readerGroups.hasNext()) {
                Element groupsList = new Element("ul", xhtml).setAttribute("class", "groups");
                groupsList.setAttribute("style", "margin: 0; float: left;");
                addGroupsToList(groupsList, managerGroups, "manager");
                addGroupsToList(groupsList, writerGroups, "writer");
                addGroupsToList(groupsList, readerGroups, "reader");
                accessorsDiv.addContent(groupsList);
            }
            content.addContent(accessorsDiv);
            content.addContent(buildClearDiv());
        }
        content.addContent(new Element("h3", xhtml).setText("Experiments"));
        content.addContent(new ProjectExperimentsXHTMLFragment(context, project, experimentsResult, true).getContent());
        Element sidebar = new Element("div", xhtml).setAttribute("class", "sidebar");
        if ("true".equals(deleteEnabled) && context.getAccessPolicy().canDeleteDataset(project.getPath())) {
            sidebar.addContent(new Element("br", xhtml));
            Element script = new Element("script", xhtml).setAttribute("type", "text/javascript").addContent("function deleteDataset() {\n" + "  if (!confirm('Are you sure you want to delete project " + project.getProperty("title").getString() + "?')) {\n" + "    return;\n" + "  }\n" + "  var url = getAppPath('" + context.getAppURL(project.getPath()) + "');\n" + "  jQuery.ajax({\n" + "    url: url,\n" + "    type: 'POST',\n" + "    beforeSend: function(xhr) {\n" + "      xhr.setRequestHeader('X-HTTP-Method-Override', 'DELETE');\n" + "    },\n" + "    success: function() {\n" + "      window.location = '" + project.getParent().getPath() + "';\n" + "    },\n" + "    error: function() {\n" + "      alert('Could not delete file.');\n" + "    }\n" + "  });\n" + "}\n");
            sidebar.addContent(script);
            sidebar.addContent(new Element("div", xhtml).setAttribute("align", "right").addContent(new Element("label", xhtml).addContent(new Element("a", xhtml).setAttribute("onclick", "deleteDataset(); return false;").setAttribute("href", project.getParent().getPath()).setText("[delete]"))));
        }
        sidebar.addContent(new NodeDatesTableXHTMLFragment(context, project).getContent());
        if (context.getUserID() != null && !context.getUserID().equals("anonymous") && !project.getName().startsWith("filemonitor-")) {
            sidebar.addContent(new NodePublishStatusXHTMLFragment(context, project, "project").getContent());
        }
        return Arrays.asList((Content) content, (Content) sidebar);
    }

    private Collection<Content> buildEditContent() throws RepositoryException {
        Element content = new Element("div", xhtml).setAttribute("class", "content");
        content.addContent(ProjectXHTMLForm.createEdit(context, project).getContent());
        return Arrays.asList((Content) content);
    }
}
