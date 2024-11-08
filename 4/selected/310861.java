package code.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;
import code.model.Note;
import code.model.Program;
import code.model.User;
import code.service.ManageNoteService;
import code.service.ManageProgramService;
import code.service.ManageUserService;
import com.opensymphony.xwork2.ActionSupport;

public class ManageUserAction extends ActionSupport implements ServletRequestAware {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1501062849909460855L;

    private String resultmsg;

    private String userName;

    private User user = new User();

    private User other = new User();

    private List<User> userList = new ArrayList<User>();

    private HttpServletRequest servletRequest;

    private ManageUserService manageUserService;

    private ManageProgramService manageProgramService;

    private ManageNoteService manageNoteService;

    private File imagefile;

    private String filename;

    private String dir;

    private String targetFileName;

    private static final String root = "/head";

    private boolean inputvalidate() {
        if (user.getEmail() == null || ("").equals(user.getEmail().trim())) {
            resultmsg = "����������";
            return false;
        }
        if (user.getName() == null || ("").equals(user.getName().trim())) {
            resultmsg = "����������";
            return false;
        }
        if (user.getPassword() == null || ("").equals(user.getPassword().trim())) {
            resultmsg = "����������";
            return false;
        }
        if (user.getGender() == null || ("").equals(user.getGender().trim())) {
            resultmsg = "��ѡ���Ա�";
            return false;
        }
        if (filename == null || ("").equals(filename.trim())) {
            resultmsg = "��ѡ��ͷ��";
            return false;
        }
        return true;
    }

    public String register() {
        try {
            if (inputvalidate() == false) return INPUT; else {
                System.out.println("uploadFile execute");
                user.setUserimage(servletRequest.getContextPath() + "/head/" + filename);
                String realPath = ServletActionContext.getRequest().getRealPath(root);
                String targetDirectory = realPath;
                targetFileName = filename;
                setDir(targetDirectory + "\\" + targetFileName);
                if (!dir.substring(dir.length() - 3, dir.length()).equals("jpg")) {
                    resultmsg = "���ϴ�jpg���͵�ͼƬ";
                    System.out.println(resultmsg);
                    return INPUT;
                }
                File target = new File(targetDirectory, targetFileName);
                FileUtils.copyFile(imagefile, target);
            }
            user.setUsertype("common");
            manageUserService.insertUser(user);
            return SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR;
        }
    }

    public String adminSearchUser() {
        System.out.println("search user:" + userName);
        userList = manageUserService.getUserlistByName(userName);
        if (userList == null || userList.size() == 0) {
            resultmsg = "No Such User~~";
            return INPUT;
        }
        resultmsg = "Total find: " + userList.size() + " users";
        return SUCCESS;
    }

    public String adminShowUserHome() {
        other = manageUserService.getUserById(other.getUserid());
        if (other == null) {
            resultmsg = "��û��Ȩ�޷��ʸÿռ�";
            return INPUT;
        }
        servletRequest.getSession().setAttribute("other", other);
        return SUCCESS;
    }

    public String deleteUser() {
        System.out.println("delete" + user.getUserid());
        long result = manageUserService.deleteUser(user.getUserid());
        if (result != user.getUserid()) {
            resultmsg = "Deletion failure, user not found!";
            return INPUT;
        }
        userName = "";
        return SUCCESS;
    }

    public String modifyUserInfo() {
        user = (User) servletRequest.getSession().getAttribute("user");
        return SUCCESS;
    }

    public String modifyUserInfoSubmit() {
        System.out.println("modify" + user.getEmail());
        User oldUser = (User) servletRequest.getSession().getAttribute("user");
        user.setName(oldUser.getName());
        user.setPassword(oldUser.getPassword());
        user.setUserid(oldUser.getUserid());
        user.setUserimage(oldUser.getUserimage());
        user.setUsertype(oldUser.getUsertype());
        boolean result = manageUserService.updateUser(user);
        if (!result) {
            resultmsg = "�����û���Ϣ�쳣";
            return INPUT;
        }
        servletRequest.getSession().setAttribute("user", user);
        resultmsg = "�����û���Ϣ�ɹ�";
        return SUCCESS;
    }

    @SuppressWarnings("unchecked")
    public String noteUserList() {
        List<Note> noteList = (List<Note>) servletRequest.getSession().getAttribute("noteList");
        userList.clear();
        for (int i = 0; i < noteList.size(); i++) userList.add(noteList.get(i).getUser());
        servletRequest.getSession().setAttribute("userList", userList);
        return SUCCESS;
    }

    public String showOtherHome() {
        user = (User) servletRequest.getSession().getAttribute("user");
        other = manageUserService.getUserById(other.getUserid());
        if (other == null) {
            resultmsg = "��û��Ȩ�޷��ʸÿռ�";
            return INPUT;
        }
        if (user.getUserid() == other.getUserid()) {
            return ERROR;
        }
        List<Note> noteList = manageNoteService.getNotelistByUserid(other.getUserid());
        List<Program> otherNotedPrograms = new ArrayList<Program>();
        for (int i = 0; i < noteList.size(); i++) otherNotedPrograms.add(manageProgramService.getProgramByProgramid(noteList.get(i).getProgramid()));
        servletRequest.getSession().setAttribute("otherNotedPrograms", otherNotedPrograms);
        servletRequest.getSession().setAttribute("other", other);
        return SUCCESS;
    }

    public void setResultmsg(String resultmsg) {
        this.resultmsg = resultmsg;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public ManageUserService getManageUserService() {
        return manageUserService;
    }

    public void setManageUserService(ManageUserService manageUserService) {
        this.manageUserService = manageUserService;
    }

    public void setImagefile(File imagefile) {
        this.imagefile = imagefile;
    }

    public void setImagefileFileName(String filename) {
        this.filename = filename;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public void setOther(User other) {
        this.other = other;
    }

    public User getOther() {
        return other;
    }

    public void setManageProgramService(ManageProgramService manageProgramService) {
        this.manageProgramService = manageProgramService;
    }

    public ManageProgramService getManageProgramService() {
        return manageProgramService;
    }

    public void setManageNoteService(ManageNoteService manageNoteService) {
        this.manageNoteService = manageNoteService;
    }

    public ManageNoteService getManageNoteService() {
        return manageNoteService;
    }
}
