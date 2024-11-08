package org.dreamfly.netshop.actions.admin;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Namespace;
import org.dreamfly.netshop.common.GB2Alpha;
import org.dreamfly.netshop.entity.GoodsInfo;
import org.dreamfly.netshop.entity.Type;
import org.dreamfly.netshop.manage.ClassManager;
import org.dreamfly.netshop.manage.GoodsInfoManager;
import org.springframework.beans.factory.annotation.Autowired;
import com.opensymphony.xwork2.ActionSupport;

/**
 * 用户管理Action. 使用Struts2 convention-plugin annotation定义Action参数.
 * 
 * @author calvin
 */
@Namespace(value = "/admin/goods")
public class GoodsAction extends ActionSupport {

    @Autowired
    private ClassManager classManager;

    @Autowired
    private GoodsInfoManager goodsInfoManager;

    private List<Type> allClass;

    private File file;

    private String fileName;

    private GoodsInfo goodsInfo;

    private String type;

    public void setFileFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Type> getAllClass() {
        return allClass;
    }

    public String getType() {
        return type;
    }

    public GoodsInfo getGoodsInfo() {
        return goodsInfo;
    }

    public void setGoodsInfo(GoodsInfo goodsInfo) {
        this.goodsInfo = goodsInfo;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String execute() {
        allClass = classManager.getAll();
        return SUCCESS;
    }

    public String search() {
        return SUCCESS;
    }

    public String getByConfirm() {
        return SUCCESS;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    private String generateFileName(String fileName) {
        DateFormat format = new SimpleDateFormat("yyMMddHHmmss");
        String formatDate = format.format(new Date());
        int random = new Random().nextInt(10000);
        int position = fileName.lastIndexOf(".");
        String extension = fileName.substring(position);
        return formatDate + random + extension;
    }

    public String saveGood() throws IOException {
        Type type = classManager.get(goodsInfo.getType().getId());
        String path = "images/ftp/" + GB2Alpha.String2Alpha(type.getClassName());
        String targetDirectory = ServletActionContext.getServletContext().getRealPath(path);
        String targetFileName = generateFileName(fileName);
        File target = new File(targetDirectory, targetFileName);
        FileUtils.copyFile(file, target);
        goodsInfo.setGoodsUrl(path + "/" + targetFileName);
        goodsInfo.setAddDate(new Date());
        goodsInfoManager.save(goodsInfo);
        allClass = classManager.getAll();
        return SUCCESS;
    }

    public String manageGoods() {
        return "manage";
    }
}
