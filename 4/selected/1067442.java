package au.edu.uq.itee.eresearch.dimer.webapp.app.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.jackrabbit.util.ISO9075;
import au.edu.uq.itee.eresearch.dimer.core.util.DefaultPagedResult;
import au.edu.uq.itee.eresearch.dimer.core.util.PagedResult;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionBuilder;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionFactory;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExperimentUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.NodeUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ProjectUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.URIUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UploadUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.AtomFeed;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.ProjectExperimentsAtomFeed;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.ProjectExperimentsXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.ProjectXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLPage;

@Path(BaseResource.appPathSuffix + "/projects/{project}")
public class ProjectResource extends BaseResource {

    @PathParam("project")
    private String projectName;

    private Node project;

    @PostConstruct
    public void init() throws RepositoryException {
        if (!getAccessPolicy().canReadProject("/projects/" + projectName)) {
            throw new WebApplicationException(403);
        }
        this.project = (Node) getSession().getItem("/projects/" + projectName);
    }

    @GET
    @Produces("text/html;qs=1.000")
    public XHTMLPage getHTML() throws RepositoryException {
        return new ProjectXHTMLPage(getViewContext(), project, getPagedExperiments(0, 10));
    }

    @PUT
    @Consumes("application/x-www-form-urlencoded")
    public void putForm(@FormParam("title") String title, @FormParam("description") String description, @FormParam("manager-users") List<String> managerUserURLs, @FormParam("writer-users") List<String> writerUserURLs, @FormParam("reader-users") List<String> readerUserURLs, @FormParam("manager-groups") List<String> managerGroupURLs, @FormParam("writer-groups") List<String> writerGroupURLs, @FormParam("reader-groups") List<String> readerGroupURLs) throws RepositoryException {
        List<Node> users = new ArrayList<Node>();
        if (!getAccessPolicy().canUpdateProject(project.getPath()) || (!getAccessPolicy().canUpdateProjectAccessRights(project.getPath()) && (managerUserURLs != null || writerUserURLs != null || readerUserURLs != null))) {
            throw ExceptionFactory.forbidden();
        }
        if (title == null || title.equals("")) {
            throw ExceptionFactory.missingRequiredFields();
        }
        if (getAccessPolicy().canUpdateProjectAccessRights(project.getPath())) {
            if ((managerUserURLs == null || managerUserURLs.isEmpty()) && (managerGroupURLs == null || managerGroupURLs.isEmpty())) {
                throw ExceptionBuilder.badRequest().error("You must specify at least one user or group as manager.").build();
            }
            NodeIterator managerUsers = ProjectUtils.getUsers(project, "managers").getNodes();
            while (managerUsers.hasNext()) {
                Node user = (Node) managerUsers.next();
                if (!users.contains(user)) {
                    users.add(user);
                }
            }
            NodeIterator writerUsers = ProjectUtils.getUsers(project, "writers").getNodes();
            while (writerUsers.hasNext()) {
                Node user = (Node) writerUsers.next();
                if (!users.contains(user)) {
                    users.add(user);
                }
            }
            ProjectUtils.updateProject(getViewContext(), project, title, description, managerUserURLs, writerUserURLs, readerUserURLs, managerGroupURLs, writerGroupURLs, readerGroupURLs);
        } else {
            ProjectUtils.updateProject(getViewContext(), project, title, description);
        }
        getSession().save();
        String q = "/jcr:root" + project.getPath() + "/experiments/element(*, experiment) order by @lastModified descending";
        @SuppressWarnings("deprecation") Query query = getViewContext().getSession().getWorkspace().getQueryManager().createQuery(q, Query.XPATH);
        NodeIterator experiments = query.execute().getNodes();
        while (experiments.hasNext()) {
            Node experiment = experiments.nextNode();
            UploadUtils.updateCollection(getViewContext(), experiment, users);
        }
        getSession().save();
    }

    @DELETE
    public void delete() throws RepositoryException {
        getSession().getItem(project.getPath()).remove();
        getSession().save();
    }

    @GET
    @Path("experiments")
    @Produces("text/html;qs=1.000")
    public XHTMLPage getExperimentsHTML(@QueryParam("offset") @DefaultValue("0") int offset, @QueryParam("limit") @DefaultValue("20") int limit) throws RepositoryException {
        return new ProjectExperimentsXHTMLPage(getViewContext(), project, getPagedExperiments(offset, limit));
    }

    @GET
    @Path("experiments")
    @Produces("application/atom+xml;qs=0.999")
    public AtomFeed getExperimentsAtom(@QueryParam("offset") @DefaultValue("0") int offset, @QueryParam("limit") @DefaultValue("20") int limit) throws RepositoryException {
        return new ProjectExperimentsAtomFeed(getViewContext(), project, getPagedExperiments(offset, limit), NodeUtils.getAuthors(project));
    }

    @POST
    @Path("experiments")
    @Consumes("application/x-www-form-urlencoded")
    public Response postExperimentForm(@HeaderParam("Slug") String name, @FormParam("title") String title, @FormParam("description") String description) throws RepositoryException {
        Node parent = project.getNode("experiments");
        if (name != null && !getAccessPolicy().canCreateExperiment(parent.getPath() + "/" + name)) {
            throw ExceptionFactory.forbidden();
        }
        if (name == null || name.equals("") || title == null || title.equals("")) {
            throw ExceptionFactory.missingRequiredFields();
        }
        if (!URIUtils.isValidSlug(name)) {
            throw ExceptionFactory.invalidSlug(name);
        }
        if (parent.hasNode(name)) {
            throw ExceptionFactory.itemExists(parent.getPath() + "/" + name);
        }
        Node experiment = ExperimentUtils.createExperiment(parent, name, title, description);
        getSession().save();
        return Response.created(URI.create(getViewContext().getAppURL(experiment.getPath()))).build();
    }

    @GET
    @Path("published")
    @Produces("text/plain;qs=1.000")
    public String getPublishedText() throws RepositoryException {
        return project.getProperty("published").getBoolean() ? "true" : "false";
    }

    @PUT
    @Path("published")
    @Consumes("text/plain")
    public void putPublishedText(String published) throws RepositoryException {
        if (!getAccessPolicy().canUpdatePublishStatus(project.getPath())) throw new WebApplicationException(403);
        if (published.trim().equals("true")) project.setProperty("published", true); else if (published.trim().equals("false")) project.setProperty("published", false); else throw new WebApplicationException(Response.Status.BAD_REQUEST);
        getSession().save();
    }

    private PagedResult getPagedExperiments(int offset, int limit) throws RepositoryException {
        return new DefaultPagedResult(getSession(), "/jcr:root" + ISO9075.encodePath(project.getPath()) + "/experiments/element(*, experiment) order by @lastModified descending", offset, limit);
    }
}
