package au.edu.uq.itee.eresearch.dimer.webapp.app.controller;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import au.edu.uq.itee.eresearch.dimer.core.util.DefaultPagedResult;
import au.edu.uq.itee.eresearch.dimer.core.util.PagedResult;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionBuilder;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionFactory;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.GroupUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.GroupProjectsXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.GroupXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLPage;

@Path(BaseResource.appPathSuffix + "/groups/{group}")
public class GroupResource extends BaseResource {

    @PathParam("group")
    private String name;

    private Node parent;

    private Node group;

    private PagedResult projectsResult;

    @PostConstruct
    public void init() throws RepositoryException {
        this.parent = (Node) getSession().getItem("/groups/");
        if (!getAccessPolicy().canReadGroup(parent.getPath() + "/" + name)) {
            throw new WebApplicationException(403);
        }
        if (parent.hasNode(name)) {
            this.group = parent.getNode(name);
            this.projectsResult = getPagedProjects(0, 10);
        }
    }

    @GET
    @Produces("text/html;qs=1.000")
    public XHTMLPage getHTML() throws RepositoryException {
        if (group == null) throw new WebApplicationException(404);
        return new GroupXHTMLPage(getViewContext(), group, projectsResult);
    }

    @PUT
    @Consumes("application/x-www-form-urlencoded")
    public void putForm(@FormParam("title") String title, @FormParam("description") String description, @FormParam("managers") List<String> managerURLs, @FormParam("members") List<String> memberURLs) throws RepositoryException {
        if (!getAccessPolicy().canUpdateGroup(group.getPath())) {
            throw ExceptionFactory.forbidden();
        }
        if (title == null || title.equals("")) {
            throw ExceptionFactory.missingRequiredFields();
        }
        if (managerURLs == null || managerURLs.isEmpty()) {
            throw ExceptionBuilder.badRequest().error("You must specify at least one user as manager.").build();
        }
        GroupUtils.updateGroup(getViewContext(), group, title, description, managerURLs, memberURLs);
        getSession().save();
    }

    @GET
    @Path("projects")
    @Produces("text/html;qs=1.000")
    public XHTMLPage getProjectsHTML() throws RepositoryException {
        if (group == null) throw new WebApplicationException(404);
        return new GroupProjectsXHTMLPage(getViewContext(), group, projectsResult);
    }

    private PagedResult getPagedProjects(int offset, int limit) throws RepositoryException {
        return new DefaultPagedResult(getSession(), "/jcr:root/projects/element(*, project)[\n" + "  managers/@groups = '" + group.getIdentifier().replaceAll("'", "''") + "'" + " or\n" + "  writers/@groups = '" + group.getIdentifier().replaceAll("'", "''") + "'" + " or\n" + "  readers/@groups = '" + group.getIdentifier().replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", offset, limit);
    }
}
