package au.edu.uq.itee.eresearch.dimer.webapp.app.controller;

import java.util.ArrayList;
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
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ExceptionFactory;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.NodeUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UploadUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UserUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.AtomView;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.atom.UserAtomView;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.UserProjectsXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.UserXHTMLPage;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLPage;

@Path(BaseResource.appPathSuffix + "/users/{user}")
public class UserResource extends BaseResource {

    @PathParam("user")
    private String name;

    private Node user;

    private PagedResult projectsResult;

    private PagedResult groupsResult;

    private PagedResult subordinateResult;

    @PostConstruct
    public void init() throws RepositoryException {
        if (!getAccessPolicy().canReadUser("/users/" + name)) {
            throw new WebApplicationException(403);
        }
        this.user = (Node) getSession().getItem("/users/" + name);
        this.projectsResult = getPagedProjects(0, 10);
        this.groupsResult = getPagedGroups(0, 100);
        this.subordinateResult = getPagedSubordinates(0, 10);
    }

    @GET
    @Produces("text/html;qs=1.000")
    public XHTMLPage getHTML() throws RepositoryException {
        return new UserXHTMLPage(getViewContext(), user, projectsResult, groupsResult, subordinateResult);
    }

    @PUT
    @Consumes("application/x-www-form-urlencoded")
    public void putForm(@FormParam("prefix") String prefix, @FormParam("first-name") String firstName, @FormParam("other-names") String otherNames, @FormParam("last-name") String lastName, @FormParam("suffix") String suffix, @FormParam("email") String email, @FormParam("alias") String alias, @FormParam("password") String password, @FormParam("password-confirm") String passwordConfirm, @FormParam("supervisor") String supervisor) throws RepositoryException {
        if (!getAccessPolicy().canUpdateUser(user.getPath())) {
            throw ExceptionFactory.forbidden();
        }
        if (name == null || name.equals("") || firstName == null || firstName.equals("")) {
            throw ExceptionFactory.missingRequiredFields();
        }
        if ((password == null && passwordConfirm != null) || (password != null && !password.equals(passwordConfirm))) {
            throw ExceptionFactory.passwordUnconfirmed();
        }
        String[] supervisors = supervisor.split(",");
        ArrayList<String> supervisorsURLs = new ArrayList<String>();
        for (String supervisorUrls : supervisors) {
            supervisorUrls = supervisorUrls.trim();
            if (!supervisorUrls.isEmpty()) {
                supervisorsURLs.add(supervisorUrls);
            }
        }
        user = UserUtils.updateUser(user, prefix, firstName, otherNames, lastName, suffix, email, password, NodeUtils.buildUUIDArray(getViewContext(), supervisorsURLs));
        if (getSession().getUserID().equals("admin")) {
            user.setProperty("alias", alias);
        }
        UploadUtils.updateAgent(getViewContext(), user);
        getSession().save();
    }

    @GET
    @Produces("application/atom+xml;qs=0.999")
    public AtomView getAtom() throws RepositoryException {
        return new UserAtomView(getViewContext(), user);
    }

    @GET
    @Path("projects")
    @Produces("text/html;qs=1.000")
    public XHTMLPage getProjectsHTML() throws RepositoryException {
        return new UserProjectsXHTMLPage(getViewContext(), user, projectsResult);
    }

    private PagedResult getPagedProjects(int offset, int limit) throws RepositoryException {
        return new DefaultPagedResult(getSession(), "/jcr:root/projects/element(*, project)[\n" + "  managers/@users = '" + user.getIdentifier().replaceAll("'", "''") + "'" + " or\n" + "  writers/@users = '" + user.getIdentifier().replaceAll("'", "''") + "'" + " or\n" + "  readers/@users = '" + user.getIdentifier().replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", offset, limit);
    }

    private PagedResult getPagedGroups(int offset, int limit) throws RepositoryException {
        return new DefaultPagedResult(getSession(), "/jcr:root/groups/element(*, group)[\n" + "  @managers = '" + user.getIdentifier().replaceAll("'", "''") + "'" + " or\n" + "  @members = '" + user.getIdentifier().replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", offset, limit);
    }

    private PagedResult getPagedSubordinates(int offset, int limit) throws RepositoryException {
        return new DefaultPagedResult(getSession(), "/jcr:root/users/element(*, user)[\n" + "  @supervisors = '" + user.getIdentifier().replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", offset, limit);
    }
}
