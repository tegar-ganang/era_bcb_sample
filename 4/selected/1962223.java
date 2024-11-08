package com.spring.rssReader.web;

import com.spring.rssReader.*;
import com.spring.rssReader.util.IParentChild;
import com.spring.rssReader.jdbc.IChannelController;
import com.spring.rssReader.validator.RoleValidator;
import com.spring.rssReader.validator.UserValidator;
import com.spring.workflow.WorkflowConstants;
import com.spring.workflow.login.AbstractAuthenticate;
import com.spring.workflow.login.IUser;
import com.spring.workflow.parser.PageDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.BindInitializer;
import org.springframework.web.bind.BindUtils;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 8-jan-2004
 * Time: 19:31:12
 * To change this template use Options | File Templates.
 */
public class AdminWebController extends AbstractAuthenticate {

    protected IUserController userController;

    protected IRoleController roleController;

    protected ICategoryDao categoryDao;

    private IChannelController channelController;

    public AdminWebController() throws ApplicationContextException {
    }

    public ModelAndView authenticateUser(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        IUser user = userController.findUser(new User(username, password));
        return makeSessionUser(request, user);
    }

    public ModelAndView setActiveTab(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        request.getSession().setAttribute("current", "adminMethods");
        return new ModelAndView("");
    }

    public ModelAndView newUser(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        User user = new User();
        return bindUser(request, user);
    }

    public ModelAndView editUser(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        IUser user = userController.findUser(new User(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME)));
        return bindUser(request, user);
    }

    private ModelAndView bindUser(HttpServletRequest request, IUser user) {
        BindException exception = BindUtils.bind(request, user, "user");
        Map model = exception.getModel();
        ModelAndView view = new ModelAndView("", model);
        view.addObject("user", user);
        view.addObject("roles", roleController.getRoles());
        request.getSession().setAttribute("selectedUser", user);
        return view;
    }

    public ModelAndView getCurrent(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String page = ((PageDefinition) request.getSession().getAttribute(WorkflowConstants.RESPONSE_PAGE)).getFullName();
        return new ModelAndView("forward:" + page + "/" + (String) request.getSession().getAttribute("current"));
    }

    public ModelAndView save(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        User user = (User) request.getSession().getAttribute("selectedUser");
        if (user == null) {
            ModelAndView current = getCurrent(request, response);
            current.addObject("message", "Dont use back button please");
            return current;
        }
        BindException exception = BindUtils.bind(request, user, "user", new BindInitializer() {

            public void initBinder(ServletRequest servletRequest, ServletRequestDataBinder servletRequestDataBinder) {
                servletRequestDataBinder.setRequiredFields(new String[] { "roles", "username", "password", "email" });
                servletRequestDataBinder.registerCustomEditor(List.class, "roles", new PropertyEditorSupport() {

                    public void setAsText(String text) throws IllegalArgumentException {
                        List list = new ArrayList();
                        String[] roles = text.split(",");
                        for (int i = 0; i < roles.length; i++) {
                            list.add(roleController.findRole(roles[i]));
                        }
                        super.setValue(list);
                    }
                });
            }
        });
        UserValidator validator = new UserValidator();
        validator.setUserController(getUserController());
        validator.validate(user, exception);
        if (exception.hasErrors()) {
            Map errors = exception.getModel();
            errors.put("roles", roleController.getRoles());
            return new ModelAndView("forward:", errors);
        }
        userController.save(user);
        request.getSession().removeAttribute("selectedUser");
        return new ModelAndView("", "user", user);
    }

    public ModelAndView showAllUsers(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        List users = userController.findAllUsers();
        return new ModelAndView("", "users", users);
    }

    public ModelAndView deleteUser(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        userController.deleteUser(new User(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME)));
        return new ModelAndView("");
    }

    public ModelAndView showAllRoles(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        List roles = roleController.getRoles();
        return new ModelAndView("", "roles", roles);
    }

    public ModelAndView deleteRole(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        roleController.deleteRole(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME));
        return new ModelAndView("");
    }

    public ModelAndView newRole(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        return bindRole(request, new Role());
    }

    public ModelAndView editRole(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Role role = roleController.getRole(RequestUtils.getLongParameter(request, WorkflowConstants.ID_NAME, -1));
        if (role == null) {
            role = new Role();
        }
        return bindRole(request, role);
    }

    public ModelAndView saveRole(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Role role = (Role) request.getSession().getAttribute("selectedRole");
        if (role == null) {
            ModelAndView current = getCurrent(request, response);
            current.addObject("message", "Dont use back button please");
            return current;
        }
        BindException exception = BindUtils.bind(request, role, "role");
        RoleValidator validator = new RoleValidator();
        validator.setRoleController(getRoleController());
        validator.validate(role, exception);
        if (exception.hasErrors()) {
            return new ModelAndView("forward:", exception.getModel());
        }
        roleController.save(role);
        request.getSession().removeAttribute("selectedRole");
        return new ModelAndView("");
    }

    private ModelAndView bindRole(HttpServletRequest request, Role role) {
        BindException exception = BindUtils.bind(request, role, "role");
        Map model = exception.getModel();
        ModelAndView view = new ModelAndView("", model);
        view.addObject("role", role);
        request.getSession().setAttribute("selectedRole", role);
        return view;
    }

    public ModelAndView getCategories(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ICategory category = categoryDao.load();
        ModelAndView modelAndView = new ModelAndView("");
        modelAndView.addObject("categories", (new FormattedCategory(category)).format());
        return modelAndView;
    }

    public ModelAndView editCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView modelAndView = findCategory(request, response);
        modelAndView.addObject("categoryMode", "edit");
        return modelAndView;
    }

    public ModelAndView addCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView modelAndView = findCategory(request, response);
        modelAndView.addObject("categoryMode", "add");
        modelAndView.addObject("editCategory", "");
        return modelAndView;
    }

    private ModelAndView findCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Long id = new Long(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME));
        IParentChild category = categoryDao.load(id);
        if (category == null) {
            return new ModelAndView("forward:", "message", "error.id.notFound");
        }
        ModelAndView modelAndView = new ModelAndView("");
        modelAndView.addObject("editCategory", category);
        modelAndView.addObject("categories", (new FormattedCategory(categoryDao.load())).format());
        List inverseBreadcrumbs = new ArrayList();
        inverseBreadcrumbs.add(category.getName());
        while (category.getParent() != null) {
            category = category.getParent();
            inverseBreadcrumbs.add(category.getName());
        }
        List breadcrumbs = new ArrayList(inverseBreadcrumbs.size());
        for (int i = inverseBreadcrumbs.size() - 1; i >= 0; i--) {
            breadcrumbs.add(inverseBreadcrumbs.get(i));
        }
        modelAndView.addObject("breadcrumbs", breadcrumbs);
        request.getSession().setAttribute("categoryId", id);
        return modelAndView;
    }

    public ModelAndView saveCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Long id = (Long) request.getSession().getAttribute("categoryId");
        ICategory category = categoryDao.load(id);
        if (request.getParameter("categoryMode").equals("edit") && !category.getName().equals(ICategory.ROOTNAME)) {
            category.setName(request.getParameter("category.name"));
        } else if (request.getParameter("categoryMode").equals("add")) {
            ICategory subCategory = new Category(request.getParameter("category.name"));
            category.addChild(subCategory);
            categoryDao.update(category);
            super.getApplicationContext().publishEvent(new CategoryDaoChangedEvent(category));
        }
        this.getServletContext().removeAttribute("categories");
        request.getSession().removeAttribute("categoryId");
        return new ModelAndView("");
    }

    public ModelAndView deleteCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long id = RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME);
        ICategory category = categoryDao.load(new Long(id));
        if (!category.getName().equals(ICategory.ROOTNAME)) {
            categoryDao.delete(category);
            super.getApplicationContext().publishEvent(new CategoryDaoChangedEvent(category));
        }
        this.getServletContext().removeAttribute("categories");
        return new ModelAndView("");
    }

    public IUserController getUserController() {
        return userController;
    }

    public void setUserController(IUserController userController) {
        this.userController = userController;
    }

    public IRoleController getRoleController() {
        return roleController;
    }

    public void setRoleController(IRoleController roleController) {
        this.roleController = roleController;
    }

    public ICategoryDao getCategoryDao() {
        return categoryDao;
    }

    public void setCategoryDao(ICategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public IChannelController getChannelController() {
        return channelController;
    }

    public void setChannelController(IChannelController channelController) {
        this.channelController = channelController;
    }
}
