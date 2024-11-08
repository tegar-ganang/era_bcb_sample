package com.action.user;

import java.io.File;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;
import com.common.ImageProcess;
import com.db.organization.DeptJobDAO;
import com.db.user.*;

public class UserInputAction extends ActionSupport implements ModelDriven<Object>, Preparable, ServletRequestAware {

    private File upload;

    private File saveFile;

    private String uploadFileName;

    private HttpServletRequest servletRequest;

    private UserVO uVO;

    private UserDAO userDAO;

    private DeptJobDAO orgDAO;

    private ImageProcess imgProc;

    public void setImgProc(ImageProcess imgProc) {
        this.imgProc = imgProc;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setOrgDAO(DeptJobDAO orgDAO) {
        this.orgDAO = orgDAO;
    }

    public void setuVO(UserVO uVO) {
        this.uVO = uVO;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    @Override
    public void prepare() throws Exception {
        uVO = new UserVO();
    }

    @Override
    public Object getModel() {
        return uVO;
    }

    @Override
    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public String insertUser() {
        String result = "";
        try {
            uVO.setSlevel(orgDAO.getSlevel(uVO.getJobno()));
            userDAO.insertUser(uVO);
            result = "success";
        } catch (Exception e) {
            System.out.println("UserInputAction.insertUser():" + e.toString());
            result = "error";
        }
        return result;
    }

    public String updateUser() {
        String result = "";
        String fn = "";
        try {
            String path = servletRequest.getSession().getServletContext().getRealPath("/upload/user/photo");
            if (upload != null && upload.exists()) {
                fn = "pi_" + uVO.getSabun() + ".jpg";
                saveFile = new File(path + "\\" + fn);
                FileUtils.copyFile(upload, saveFile);
                imgProc.createFixedSize(path, fn);
                uVO.setPhotofn(fn);
            }
            uVO.setSlevel(orgDAO.getSlevel(uVO.getJobno()));
            userDAO.updateUser(uVO);
            ActionContext context = ActionContext.getContext();
            Map<String, UserVO> session = (Map<String, UserVO>) context.getSession();
            uVO = userDAO.getUserInfo(uVO.getSabun());
            session.put("user", uVO);
            context.setSession(session);
            result = "success";
        } catch (Exception e) {
            System.out.println("UserInputAction.updateUser():" + e.toString());
            result = "error";
        }
        return result;
    }
}
