package au.edu.uq.itee.eresearch.dimer.webapp.app.controller;

import static org.apache.jackrabbit.util.Text.escapeIllegalXpathSearchChars;
import java.net.URI;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import au.edu.uq.itee.eresearch.dimer.core.util.DefaultPagedResult;
import au.edu.uq.itee.eresearch.dimer.core.util.PagedResult;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.DateParam;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionBuilder;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionFactory;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ProjectUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.URIUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.AtomFeed;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.ProjectsAtomFeed;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.ProjectsXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLPage;

@Path(BaseResource.appPathSuffix + "/projects")
public class ProjectsResource extends BaseResource {

    @QueryParam("q")
    private String q;

    @QueryParam("from-publication")
    private DateParam startPublicationDateParam;

    @QueryParam("to-publication")
    private DateParam endPublicationDateParam;

    @QueryParam("from-creation")
    private DateParam startCreationDateParam;

    @QueryParam("to-creation")
    private DateParam endCreationDateParam;

    @QueryParam("offset")
    @DefaultValue("0")
    private int offset;

    @QueryParam("limit")
    @DefaultValue("20")
    private int limit;

    private Node parent;

    private PagedResult projectsResult;

    @PostConstruct
    public void init() throws RepositoryException {
        if (offset < 0) throw new WebApplicationException(404);
        if (limit <= 0) throw new WebApplicationException(404);
        this.parent = (Node) getSession().getItem("/projects");
        StringBuilder query = new StringBuilder();
        query.append("/jcr:root/projects/element(*, project)");
        if (q != null && !q.equals("")) {
            query.append("[");
            query.append("jcr:contains(@title, '" + escapeIllegalXpathSearchChars(q).replaceAll("'", "''") + "')");
            query.append(" or ");
            query.append("jcr:contains(@description, '" + escapeIllegalXpathSearchChars(q).replaceAll("'", "''") + "')");
            query.append("]");
        }
        if (startPublicationDateParam != null || endPublicationDateParam != null) {
            query.append("[@publishedAt]");
        }
        if (startPublicationDateParam != null) {
            String dateTime = startPublicationDateParam.format() + "T00:00:00.000+10:00";
            query.append("[@publishedAt >= xs:dateTime('" + dateTime.replaceAll("'", "''") + "')]");
        }
        if (endPublicationDateParam != null) {
            String dateTime = endPublicationDateParam.format() + "T23:59:59.999+10:00";
            query.append("[@publishedAt <= xs:dateTime('" + dateTime.replaceAll("'", "''") + "')]");
        }
        if (startCreationDateParam != null || endCreationDateParam != null) {
            query.append("[@created]");
        }
        if (startCreationDateParam != null) {
            String dateTime = startCreationDateParam.format() + "T00:00:00.000+10:00";
            query.append("[@created >= xs:dateTime('" + dateTime.replaceAll("'", "''") + "')]");
        }
        if (endCreationDateParam != null) {
            String dateTime = endCreationDateParam.format() + "T23:59:59.999+10:00";
            query.append("[@created <= xs:dateTime('" + dateTime.replaceAll("'", "''") + "')]");
        }
        query.append(" ");
        query.append("order by @lastModified descending");
        this.projectsResult = new DefaultPagedResult(getSession(), query.toString(), offset, limit);
        if (offset > 0 && offset >= projectsResult.getCount()) {
            throw new WebApplicationException(404);
        }
    }

    @GET
    @Produces("text/html;qs=1.000")
    public XHTMLPage getHTML() throws RepositoryException {
        return new ProjectsXHTMLPage(getViewContext(), parent, projectsResult, q, (startPublicationDateParam != null) ? startPublicationDateParam.getDate() : null, (endPublicationDateParam != null) ? endPublicationDateParam.getDate() : null, (startCreationDateParam != null) ? startCreationDateParam.getDate() : null, (endCreationDateParam != null) ? endCreationDateParam.getDate() : null);
    }

    @GET
    @Produces("application/atom+xml;qs=0.999")
    public AtomFeed getAtom() throws RepositoryException {
        return new ProjectsAtomFeed(getViewContext(), parent, projectsResult);
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response postForm(@HeaderParam("Slug") String name, @FormParam("title") String title, @FormParam("description") String description, @FormParam("manager-users") List<String> managerUserURLs, @FormParam("writer-users") List<String> writerUserURLs, @FormParam("reader-users") List<String> readerUserURLs, @FormParam("manager-groups") List<String> managerGroupURLs, @FormParam("writer-groups") List<String> writerGroupURLs, @FormParam("reader-groups") List<String> readerGroupURLs) throws RepositoryException {
        if (name != null && !getAccessPolicy().canCreateProject(parent.getPath() + "/" + name)) {
            throw ExceptionFactory.forbidden();
        }
        if (name.startsWith("filemonitor-")) {
            throw ExceptionBuilder.conflict().error("Projects cannot start with 'filemonitor-'.").build();
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
        if ((managerUserURLs == null || managerUserURLs.isEmpty()) && (managerGroupURLs == null || managerGroupURLs.isEmpty())) {
            throw ExceptionBuilder.badRequest().error("You must specify at least one user or group as manager.").build();
        }
        Node project = ProjectUtils.createProject(getViewContext(), parent, name, title, description, managerUserURLs, writerUserURLs, readerUserURLs, managerGroupURLs, writerGroupURLs, readerGroupURLs);
        getSession().save();
        return Response.created(URI.create(getViewContext().getAppURL(project.getPath()))).build();
    }
}
