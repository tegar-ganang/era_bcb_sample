package com.infineon.dns.action;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.infineon.dns.form.UserForm;
import com.infineon.dns.model.User;
import com.infineon.dns.service.UserService;
import com.infineon.dns.util.Locator;
import com.infineon.dns.util.PagedListAndTotalCount;

public class UserAction extends BaseAction {

    public ActionForward listUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserService userService = Locator.lookupService(UserService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PagedListAndTotalCount<User> map = userService.getUsers(request.getParameter("sort"), request.getParameter("dir"), request.getParameter("start"), request.getParameter("limit"));
            StringBuffer json = new StringBuffer("{totalCount:" + map.getTotalCount() + ",users:[");
            for (User user : map.getPagedList()) {
                json.append("{'userId':'" + user.getUserId() + "','userLogin':'" + StringEscapeUtils.escapeHtml(user.getUserLogin()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','userLoginText':'" + StringEscapeUtils.escapeJavaScript(user.getUserLogin()) + "','userName':'" + StringEscapeUtils.escapeHtml(user.getUserName()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','userNameText':'" + StringEscapeUtils.escapeJavaScript(user.getUserName()) + "','userRemark':'" + StringEscapeUtils.escapeHtml(user.getUserRemark()).replace("\r\n", "<br>").replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','userRemarkText':'" + StringEscapeUtils.escapeJavaScript(user.getUserRemark()) + "'},");
            }
            if (map.getTotalCount() != 0) {
                json.deleteCharAt(json.length() - 1);
            }
            json.append("]}");
            response.getWriter().write(json.toString());
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            return mapping.findForward("");
        }
    }

    public ActionForward createUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UserForm userForm = (UserForm) form;
            User user = new User();
            UserService userService = Locator.lookupService(UserService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            if (userService.getUserByUserLogin(userForm.getUserLogin()).size() != 0) {
                response.getWriter().write("{success:false,message:'User: " + userForm.getUserLogin() + " already existed'}");
                return mapping.findForward("");
            }
            user.setUserLogin(userForm.getUserLogin());
            user.setUserName(userForm.getUserName());
            user.setUserRemark(userForm.getUserRemark());
            userService.insertUser(user);
            response.getWriter().write("{success:true,message:'New user successfully added'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward updateUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UserForm userForm = (UserForm) form;
            UserService userService = Locator.lookupService(UserService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            User user = userService.getUserByUserId(userForm.getUserId());
            if (user == null) {
                response.getWriter().write("{success:true,message:'This user information has already been deleted'}");
                return mapping.findForward("");
            }
            List<User> userList = userService.getUserByUserLogin(userForm.getUserLogin());
            if (userList.size() > 0 && userList.get(0).getUserId() != userForm.getUserId()) {
                response.getWriter().write("{success:false,message:'User: " + userForm.getUserLogin() + " already existed'}");
                return mapping.findForward("");
            }
            user.setUserLogin(userForm.getUserLogin());
            user.setUserName(userForm.getUserName());
            user.setUserRemark(userForm.getUserRemark());
            userService.updateUser(user);
            response.getWriter().write("{success:true,message:'Modify user information successfully'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward deleteUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UserForm userForm = (UserForm) form;
            UserService userService = Locator.lookupService(UserService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            User user = userService.getUserByUserId(userForm.getUserId());
            if (user == null) {
                response.getWriter().write("{success:true,message:'This user information has already been deleted'}");
                return mapping.findForward("");
            }
            userService.deleteUser(userForm.getUserId());
            response.getWriter().write("{success:true,message:'Successfully delete user information'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }
}
