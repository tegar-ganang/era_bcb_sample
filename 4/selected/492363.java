package com.city.itis.action;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.stereotype.Controller;
import com.city.itis.domain.Member;
import com.city.itis.domain.User;
import com.city.itis.service.UserService;
import com.city.itis.util.Constants;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

/**
 * 用户Action
 * @author WY
 *
 */
@Controller
public class UserAction extends ActionSupport implements ModelDriven<User> {

    private static final long serialVersionUID = 1L;

    private List<User> userList = null;

    private User user = new User();

    private String message = null;

    private String command = null;

    private UserService userService;

    private String dealPhoto = null;

    private File photo;

    private String photoFileName = null;

    private String photoContentType;

    private String result = null;

    Map<String, Object> map = null;

    /**
	 * 添加方法
	 * @return
	 */
    public String add() throws Exception {
        user.setPhotoName(photoFileName);
        int flag = userService.add(user);
        if (flag > 0) {
            if (user.getPhotoName() != null) {
                save_photo();
            }
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
	 * 修改方法
	 * @return
	 */
    public String modify() throws Exception {
        if (dealPhoto.equals(Constants.MODIFY)) {
            save_photo();
            user.setPhotoName(photoFileName);
        } else if (dealPhoto.equals(Constants.DELETE)) {
            delete_photo();
            user.setPhotoName(null);
        } else {
            user.setPhotoName(user.getPhotoName());
        }
        int flag = userService.modify(user);
        if (flag > 0) {
            if (Constants.DETAIL.equals(command)) {
                return Constants.DETAIL;
            }
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
	 * 删除方法
	 * @return
	 */
    public String delete() throws Exception {
        int flag = userService.delete(user);
        if (flag > 0) {
            delete_photo();
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
	 * 根据用户编号，查询用户信息
	 * @return
	 */
    public String find() throws Exception {
        Integer id = user.getId();
        user = userService.getUserById(id);
        if (Constants.MODIFY.equals(command)) {
            return Constants.MODIFY;
        } else if (Constants.DELETE.equals(command)) {
            return Constants.DELETE;
        } else {
            return NONE;
        }
    }

    /**
	 * 用户明细信息
	 * @return
	 * @throws Exception
	 */
    public String detail() throws Exception {
        map = ActionContext.getContext().getSession();
        User u = (User) map.get("login_user");
        if (u != null) {
            user = userService.getUserById(u.getId());
            return SUCCESS;
        }
        return ERROR;
    }

    /**
	 * 查询所有用户信息
	 * @return
	 */
    public String list() throws Exception {
        map = ActionContext.getContext().getSession();
        User u = (User) map.get("login_user");
        if (u != null) {
        }
        userList = userService.findAllById(u.getId());
        if (userList != null) {
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
     * 保存图片方法
     */
    public void save_photo() throws Exception {
        String realPath = ServletActionContext.getServletContext().getRealPath("/upload");
        if (photo != null) {
            File saveFile = new File(new File(realPath), photoFileName);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            FileUtils.copyFile(photo, saveFile);
        }
    }

    /**
	 * 删除图片方法
	 */
    public void delete_photo() throws Exception {
        String realPath = ServletActionContext.getServletContext().getRealPath("/upload");
        if (user.getPhotoName() != null) {
            File deleteFile = new File(new File(realPath), user.getPhotoName());
            if (!deleteFile.getParentFile().exists()) {
                deleteFile.getParentFile().mkdirs();
            }
            if (deleteFile != null) {
                deleteFile.delete();
            }
        }
    }

    public String getUserByUserId() throws Exception {
        User u = userService.getUserByUserId(user.getUserId());
        if (u != null) {
            result = "用户账号已经存在，请使用别的账号注册。";
        } else {
            result = "恭喜您，该账号可用。";
        }
        return SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public User getModel() {
        return user;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public UserService getUserService() {
        return userService;
    }

    @Resource
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public File getPhoto() {
        return photo;
    }

    public void setPhoto(File photo) {
        this.photo = photo;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public String getDealPhoto() {
        return dealPhoto;
    }

    public void setDealPhoto(String dealPhoto) {
        this.dealPhoto = dealPhoto;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
