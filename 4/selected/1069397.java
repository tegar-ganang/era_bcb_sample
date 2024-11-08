package com.javaeye.lonlysky.lforum.web.admin.rapidset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.config.ParentPackage;
import com.javaeye.lonlysky.lforum.AdminBaseAction;
import com.javaeye.lonlysky.lforum.comm.LForumRequest;
import com.javaeye.lonlysky.lforum.service.admin.MenuManager;

/**
 * 菜单备份管理
 * 
 * @author 黄磊
 *
 */
@ParentPackage("default")
public class ManagemenubackupfileAction extends AdminBaseAction {

    private static final long serialVersionUID = 1328167110147681607L;

    private List<String[]> backupFiles;

    @Override
    public String execute() throws Exception {
        if (!LForumRequest.isPost()) {
            bindDataGrid();
        }
        String submitMethod = LForumRequest.getParamValue("submitMethod");
        if (!submitMethod.equals("")) {
            submitMethod = submitMethod.substring(submitMethod.indexOf(":") + 1);
            if (submitMethod.equals("backupfile")) {
                backupfile();
            } else if (submitMethod.equals("delbackupfile")) {
                delbackupfile();
            }
        }
        if (!LForumRequest.getParamValue("filename").equals("")) {
            FileUtils.copyFile(new File(config.getWebpath() + "admin/xml/backup/" + LForumRequest.getParamValue("filename")), new File(config.getWebpath() + "admin/xml/navmenu.xml"));
            MenuManager.createMenuJson();
            registerStartupScript("", "<script>alert('恢复成功！');window.location.href='managemainmenu.action';</script>");
        }
        return SUCCESS;
    }

    /**
	 * 删除备份文件
	 * @throws IOException 
	 */
    private void delbackupfile() throws IOException {
        String backupname = LForumRequest.getParamValues("backupname", ",");
        if (backupname == "") {
            registerStartupScript("", "<script>alert('未选中任何记录！');</script>");
            return;
        }
        for (String file : backupname.split(",")) {
            FileUtils.forceDelete(new File(config.getWebpath() + "admin/xml/backup/" + file));
        }
        registerStartupScript("", "<script>alert('删除成功！');window.location.href='managemenubackupfile.action';</script>");
    }

    /**
	 * 备份菜单文件
	 * @throws IOException 
	 */
    private void backupfile() throws IOException {
        MenuManager.backupMenuFile();
        registerStartupScript("", "<script>alert('备份成功！');window.location.href='managemenubackupfile.action';</script>");
    }

    /**
	 * 绑定数据
	 */
    private void bindDataGrid() {
        File backFile = new File(config.getWebpath() + "admin/xml/backup/");
        backupFiles = new ArrayList<String[]>();
        for (File file : backFile.listFiles()) {
            if (file.getName().indexOf(".xml") != -1) {
                String[] tmp = { file.getName(), (file.getName().substring(0, file.getName().indexOf("."))).replace("_", ":") };
                backupFiles.add(tmp);
            }
        }
    }

    public List<String[]> getBackupFiles() {
        return backupFiles;
    }
}
