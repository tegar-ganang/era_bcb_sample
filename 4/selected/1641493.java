package com.medics.action;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.SessionAware;
import com.medics.dao.UsersDaoImpl;
import com.medics.entities.Users;
import com.opensymphony.xwork2.ActionSupport;

public class User extends ActionSupport implements SessionAware {

    public File userImage;

    private String userImageContentType;

    private String userImageFileName;

    public UsersDaoImpl userDao;

    boolean isUploaded;

    public Map session;

    String photoPath;

    Users user;

    public String uploadPhoto() {
        String filePath = ServletActionContext.getServletContext().getRealPath("/");
        Random random = new Random();
        String prefix = Integer.toString(random.nextInt(10000000));
        photoPath = "user\\images\\";
        filePath += photoPath;
        photoPath = photoPath.replace("\\", "/");
        photoPath += prefix + "-" + userImageFileName;
        userImageFileName = prefix + "-" + userImageFileName;
        File fileToCreate = new File(filePath, this.userImageFileName);
        try {
            FileUtils.copyFile(this.userImage, fileToCreate);
            user = userDao.findByUsername((String) session.get("username"));
            user.setPhotoName(filePath + userImageFileName);
            userDao.save(user);
            isUploaded = true;
        } catch (IOException e) {
            addActionError(e.getMessage());
            e.printStackTrace();
            return INPUT;
        }
        return "UPLOADED";
    }

    public UsersDaoImpl getUserDao() {
        return userDao;
    }

    public File getUserImage() {
        return userImage;
    }

    public void setUserImage(File userImage) {
        this.userImage = userImage;
    }

    public String getUserImageContentType() {
        return userImageContentType;
    }

    public void setUserImageContentType(String userImageContentType) {
        this.userImageContentType = userImageContentType;
    }

    public String getUserImageFileName() {
        return userImageFileName;
    }

    public void setUserImageFileName(String userImageFileName) {
        this.userImageFileName = userImageFileName;
    }

    public void setUserDao(UsersDaoImpl usersDao) {
        this.userDao = usersDao;
    }

    public void setSession(Map<String, Object> arg0) {
        session = arg0;
    }

    public boolean isUploaded() {
        return isUploaded;
    }

    public void setUploaded(boolean isUploaded) {
        this.isUploaded = isUploaded;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}
