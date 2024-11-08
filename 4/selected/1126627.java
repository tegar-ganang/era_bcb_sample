package info.herkuang.res.test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import com.myres.dao.UserDao;
import com.myres.model.User;
import com.myres.util.ImgCut;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class StrutsTest extends ActionSupport {

    private UserDao userDao;

    private File upload;

    private String uploadFileName;

    private String targetFileName;

    private Integer x1, y1, width, height;

    private String myFileName;

    public String getMyFileName() {
        return myFileName;
    }

    public void setMyFileName(String myFileName) {
        this.myFileName = myFileName;
    }

    public Integer getX1() {
        return x1;
    }

    public void setX1(Integer x1) {
        this.x1 = x1;
    }

    public Integer getY1() {
        return y1;
    }

    public void setY1(Integer y1) {
        this.y1 = y1;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public String addUserToSession() throws Exception {
        User u = userDao.get(1);
        ActionContext.getContext().getSession().put("user", u);
        return SUCCESS;
    }

    public String changeImgSize() throws Exception {
        System.out.println(x1);
        System.out.println(y1);
        System.out.println(width);
        System.out.println(height);
        String srcDir = ServletActionContext.getServletContext().getRealPath("/img");
        String targetDir = ServletActionContext.getServletContext().getRealPath("/img/cut");
        int lastSlash = myFileName.lastIndexOf('/');
        String fileName = myFileName.substring(lastSlash);
        int lastPointPosition = myFileName.lastIndexOf(".");
        String extension = myFileName.substring(lastPointPosition + 1);
        System.out.println("extension: " + extension);
        System.out.println(srcDir + fileName);
        System.out.println(targetDir + fileName);
        ImgCut imgCut = new ImgCut(x1, y1, width, height);
        imgCut.setSrcpath(srcDir + "/" + fileName);
        imgCut.setSubpath(targetDir + "/" + fileName);
        imgCut.setType(extension);
        imgCut.cut();
        targetFileName = fileName;
        return SUCCESS;
    }

    public String fileUpload() throws Exception {
        String targetDirectory = ServletActionContext.getServletContext().getRealPath("/img");
        if (upload != null) {
            targetFileName = generateFileName(uploadFileName);
            File target = new File(targetDirectory, targetFileName);
            FileUtils.copyFile(upload, target);
        } else {
            targetFileName = "default.png";
        }
        System.out.println(targetFileName);
        return SUCCESS;
    }

    private String generateFileName(String fileName) {
        DateFormat format = new SimpleDateFormat("yyMMddHHmmss");
        String formatDate = format.format(new Date());
        int random = new Random().nextInt(10000);
        int position = fileName.lastIndexOf(".");
        String extension = fileName.substring(position);
        return formatDate + random + extension;
    }
}
