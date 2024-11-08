package au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml;

import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.buildLabel;
import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.buildTextArea;
import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.buildTextInput;
import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.getNodeOptions;
import static au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.xhtml;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Element;
import au.edu.uq.itee.eresearch.dimer.core.util.HandleUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.GroupUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.NodeUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.ProjectUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UserUtils;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.ViewContext;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xhtml.XHTMLUtils.Option;
import au.edu.uq.itee.eresearch.dimer.webapp.app.view.xml.XMLFragment;

public class ProjectXHTMLForm implements XMLFragment {

    private final ViewContext context;

    private final Node parent;

    private final Node project;

    private ProjectXHTMLForm(ViewContext context, Node parent, Node project) throws RepositoryException {
        this.context = context;
        this.parent = parent;
        this.project = project;
    }

    public static ProjectXHTMLForm createNew(ViewContext context, Node parent) throws RepositoryException {
        return new ProjectXHTMLForm(context, parent, null);
    }

    public static ProjectXHTMLForm createEdit(ViewContext context, Node project) throws RepositoryException {
        return new ProjectXHTMLForm(context, project.getParent(), project);
    }

    public Collection<Content> getContent() throws RepositoryException {
        Element script = new Element("script", xhtml).setAttribute("type", "text/javascript").addContent("function submitProject() {\n" + ((project == null) ? "  var url = '" + context.getAppURL(parent.getPath()) + "';\n" : "  var url = '" + context.getAppURL(project.getPath()) + "';\n") + "  var parameters = serializeHash('#project-form');\n" + (project == null || context.getAccessPolicy().canUpdateProjectAccessRights(project.getPath()) ? "  parameters['manager-users'] = allOptions('#project-form-manager-users-field');\n" + "  parameters['writer-users'] =  allOptions('#project-form-writer-users-field');\n" + "  parameters['reader-users'] =  allOptions('#project-form-reader-users-field');\n" + "  parameters['manager-groups'] = allOptions('#project-form-manager-groups-field');\n" + "  parameters['writer-groups'] =  allOptions('#project-form-writer-groups-field');\n" + "  parameters['reader-groups'] =  allOptions('#project-form-reader-groups-field');\n" : "") + "  jQuery.ajax({\n" + "    url: url,\n" + "    type: 'POST',\n" + "    beforeSend: function(xhr) {\n" + ((project == null) ? "      xhr.setRequestHeader('Slug', jQuery('#project-form-name-field').val());\n" : "      xhr.setRequestHeader('X-HTTP-Method-Override', 'PUT');\n") + "    },\n" + "    data: parameters,\n" + "    error: function(xhr, textStatus, errorThrown) {\n" + "      displayErrors(xhr, '#project-form-errors');\n" + "    },\n" + "    complete: function (xhr, textStatus) {\n" + "      if (textStatus == 'success') {\n" + ((project == null) ? "        window.location = xhr.getResponseHeader('Location');\n" : "        window.location = url;\n") + "      }\n" + "    }\n" + "  });\n" + "}\n");
        Element form = new Element("form", xhtml).setAttribute("id", "project-form").setAttribute("onsubmit", "submitProject(); return false;");
        Element table = new Element("table", xhtml);
        table.addContent(new Element("tr", xhtml).addContent(new Element("td", xhtml).setAttribute("class", "label").addContent(buildLabel("project", "title", "Title", true))).addContent(new Element("td", xhtml).setAttribute("class", "field").addContent(buildTextInput("project", "title", 60, project, "title"))));
        table.addContent(new Element("tr", xhtml).addContent(new Element("td", xhtml).setAttribute("class", "label").addContent(buildLabel("project", "description", "Description", false))).addContent(new Element("td", xhtml).setAttribute("class", "field").addContent(buildTextArea("project", "description", 10, 58, project, "description"))));
        if (project == null || context.getAccessPolicy().canUpdateProjectAccessRights(project.getPath())) {
            List<Node> allUsers = NodeUtils.getList(UserUtils.getAllUsers(parent.getSession()));
            List<Node> allGroups = NodeUtils.getList(GroupUtils.getAllGroups(parent.getSession()));
            table.addContent(buildAccessRightsRow(project, allUsers, allGroups, context.getUser()));
        }
        if (project == null) {
            table.addContent(new Element("tr", xhtml).addContent(new Element("td", xhtml).setAttribute("class", "label").addContent(buildLabel("project", "name", "Handle", true))).addContent(new Element("td", xhtml).setAttribute("class", "field").addContent(buildTextInput("project", "name", 15, HandleUtils.generateString(context.getSession(), context.getAppPath(parent.getPath())))).addContent(new Element("p", xhtml).setAttribute("class", "description").addContent("The handle is a short tag that determines the URL assigned to the project.").addContent(new Element("br", xhtml)).addContent("For example, entering \"sg2009\" will create a project at ").addContent(new Element("tt", xhtml).setText(context.getAppPath(parent.getPath() + "/sg2009"))).addContent("."))));
        }
        form.addContent(new Element("fieldset", xhtml).addContent(new Element("legend", xhtml).setText("Project details")).addContent(table));
        form.addContent(new Element("div", xhtml).setAttribute("id", "project-form-errors").addContent(new Comment("hack to prevent tag minimisation")));
        form.addContent(new Element("div", xhtml).setAttribute("class", "buttonset").addContent(new Element("input", xhtml).setAttribute("type", "submit").setAttribute("value", "Save")));
        return Arrays.asList((Content) script, (Content) form);
    }

    private Collection<Element> buildAccessRightsRow(Node project, List<Node> allUsers, List<Node> allGroups, Node defaultUser) throws RepositoryException {
        List<Option> includedUserOptions = new ArrayList<Option>();
        if (project != null) {
            includedUserOptions.addAll(getNodePermissionsOptions(context, "manager", NodeUtils.getList(ProjectUtils.getUsers(project, "managers"))));
            includedUserOptions.addAll(getNodePermissionsOptions(context, "writer", NodeUtils.getList(ProjectUtils.getUsers(project, "writers"))));
            includedUserOptions.addAll(getNodePermissionsOptions(context, "reader", NodeUtils.getList(ProjectUtils.getUsers(project, "readers"))));
        } else if (defaultUser != null) {
            includedUserOptions.addAll(getNodePermissionsOptions(context, "manager", Arrays.asList(defaultUser)));
        }
        @SuppressWarnings("unchecked") List<Option> excludedUserOptions = getNodeOptions(context, allUsers, includedUserOptions);
        List<Option> includedGroupOptions = new ArrayList<Option>();
        if (project != null) {
            includedGroupOptions.addAll(getNodePermissionsOptions(context, "manager", NodeUtils.getList(ProjectUtils.getGroups(project, "managers"))));
            includedGroupOptions.addAll(getNodePermissionsOptions(context, "writer", NodeUtils.getList(ProjectUtils.getGroups(project, "writers"))));
            includedGroupOptions.addAll(getNodePermissionsOptions(context, "reader", NodeUtils.getList(ProjectUtils.getGroups(project, "readers"))));
        }
        @SuppressWarnings("unchecked") List<Option> excludedGroupOptions = getNodeOptions(context, allGroups, includedGroupOptions);
        return Arrays.asList(new Element("tr", xhtml).addContent(new Element("td", xhtml).setAttribute("class", "label").addContent(buildLabel("project", "permission" + "-users-all", "Users", false))).addContent(new Element("td", xhtml).setAttribute("class", "field").addContent(buildPermissionsField("project", "users", includedUserOptions, excludedUserOptions))), new Element("tr", xhtml).addContent(new Element("td", xhtml).setAttribute("class", "label").addContent(buildLabel("project", "permission" + "-groups-all", "Groups", false))).addContent(new Element("td", xhtml).setAttribute("class", "field").addContent(buildPermissionsField("project", "groups", includedGroupOptions, excludedGroupOptions))));
    }

    public static List<Option> getNodePermissionsOptions(ViewContext context, String roleLabel, List<Node> nodes) throws RepositoryException {
        List<Option> options = new ArrayList<Option>();
        for (Node node : nodes) {
            Option option = new Option(NodeUtils.getNodeURL(context, node), NodeUtils.getNodeTitle(node, true) + " (" + roleLabel + ")");
            options.add(option);
        }
        return options;
    }

    public Element buildPermissionsField(String form, String entity, List<Option> includedOptions, List<Option> excludedOptions) throws RepositoryException {
        final int listSize = 8;
        final String listWidth = "19em";
        final String buttonWidth = "95px";
        final String[] roles = new String[] { "manager", "writer", "reader" };
        final String field = "permission-" + entity;
        Element excludedSelect = new Element("select", xhtml).setAttribute("id", form + "-form" + "-" + field + "-all" + "-field").setAttribute("multiple", "multiple").setAttribute("size", String.valueOf(listSize)).setAttribute("style", "width: " + listWidth + ";");
        for (Option option : excludedOptions) {
            excludedSelect.addContent(new Element("option", xhtml).setAttribute("value", option.getValue()).setText(option.getText()));
        }
        Element includedSelect = new Element("select", xhtml).setAttribute("id", form + "-form" + "-" + field + "-field").setAttribute("multiple", "multiple").setAttribute("size", String.valueOf(listSize)).setAttribute("style", "width: " + listWidth + ";");
        for (Option option : includedOptions) {
            includedSelect.addContent(new Element("option", xhtml).setAttribute("value", option.getValue()).setText(option.getText()));
        }
        Element fieldTable = new Element("table", xhtml);
        Element buttonCell = new Element("td", xhtml).setAttribute("style", "vertical-align: middle");
        for (String role : roles) {
            buttonCell.addContent(new Element("input", xhtml).setAttribute("type", "button").setAttribute("style", "width: " + buttonWidth + "; text-align: right;").setAttribute("value", StringUtils.capitalize(role) + " >").setAttribute("onclick", "moveOptions(" + "'#" + form + "-form" + "-" + field + "-all" + "-field'" + ", " + "'#" + form + "-form" + "-" + field + "-field'" + ", " + "function(option) { " + "  option.text += ' (' + '" + role + "' + ')'; " + "  copyOption('#project-form-" + role + "-" + entity + "-field', option); " + "}" + ");"));
            buttonCell.addContent(new Element("br", xhtml));
        }
        {
            buttonCell.addContent(new Element("input", xhtml).setAttribute("type", "button").setAttribute("style", "width: " + buttonWidth + "; text-align: left;").setAttribute("value", "< Delete").setAttribute("onclick", "moveOptions(" + "'#" + form + "-form" + "-" + field + "-field'" + ", " + "'#" + form + "-form" + "-" + field + "-all" + "-field'" + ", " + "function(option) { " + "  var t = option.text; " + "  var role = t.substring(t.lastIndexOf(' (') + 2, t.lastIndexOf(')'));" + "  deleteOption('#project-form-' + role + '-" + entity + "-field', option); " + "  option.text = t.substring(0, t.lastIndexOf(' (')); " + "}" + ");"));
        }
        fieldTable.addContent(new Element("tr", xhtml).addContent(new Element("td", xhtml).addContent(excludedSelect)).addContent(buttonCell).addContent(new Element("td", xhtml).addContent(includedSelect)));
        Element hiddenRow = new Element("tr", xhtml).setAttribute("style", "display: none;");
        for (String role : roles) {
            Element includedSelectRole = new Element("select", xhtml).setAttribute("id", "project-form-" + role + "-" + entity + "-field").setAttribute("multiple", "multiple").setAttribute("size", String.valueOf(listSize)).setAttribute("style", "width: " + listWidth + ";");
            for (Option option : includedOptions) {
                if (!option.getText().endsWith(" (" + role + ")")) continue;
                includedSelectRole.addContent(new Element("option", xhtml).setAttribute("value", option.getValue()).setText(option.getText()));
            }
            hiddenRow.addContent(new Element("td", xhtml).addContent(includedSelectRole));
        }
        fieldTable.addContent(hiddenRow);
        return fieldTable;
    }
}
