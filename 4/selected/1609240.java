package com.jshop.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.apache.struts2.json.annotations.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import com.jshop.action.tools.Validate;
import com.jshop.entity.ServerFileInfo;
import com.opensymphony.xwork2.ActionSupport;

@ParentPackage("jshop")
@Controller("imgTAction")
public class ImgTAction extends ActionSupport implements ServletResponseAware, ServletRequestAware {

    private static final Logger log = LoggerFactory.getLogger(ImgTAction.class);

    private File fileupload;

    private String fileuploadFileName;

    private String allfilename;

    private String qqfile;

    private String directoryname;

    private String filestrs;

    private HttpServletResponse response;

    private HttpServletRequest request;

    /**
	 * 保存服务器文件目录
	 */
    private List list = new ArrayList();

    private String query;

    private String qtype;

    private int total = 0;

    private int rp;

    private int page = 1;

    private List rows = new ArrayList();

    private String creatorid;

    private String imgdirpath;

    private boolean slogin;

    private boolean sucflag;

    public File getFileupload() {
        return fileupload;
    }

    public void setFileupload(File fileupload) {
        this.fileupload = fileupload;
    }

    public String getFileuploadFileName() {
        return fileuploadFileName;
    }

    public void setFileuploadFileName(String fileuploadFileName) {
        this.fileuploadFileName = fileuploadFileName;
    }

    public String getAllfilename() {
        return allfilename;
    }

    public void setAllfilename(String allfilename) {
        this.allfilename = allfilename;
    }

    @JSON(serialize = false)
    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    @JSON(serialize = false)
    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setServletResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void setServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getQqfile() {
        return qqfile;
    }

    public void setQqfile(String qqfile) {
        this.qqfile = qqfile;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQtype() {
        return qtype;
    }

    public void setQtype(String qtype) {
        this.qtype = qtype;
    }

    public List getRows() {
        return rows;
    }

    public void setRows(List rows) {
        this.rows = rows;
    }

    public String getCreatorid() {
        return creatorid;
    }

    public void setCreatorid(String creatorid) {
        this.creatorid = creatorid;
    }

    public boolean isSlogin() {
        return slogin;
    }

    public void setSlogin(boolean slogin) {
        this.slogin = slogin;
    }

    public boolean isSucflag() {
        return sucflag;
    }

    public void setSucflag(boolean sucflag) {
        this.sucflag = sucflag;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getRp() {
        return rp;
    }

    public void setRp(int rp) {
        this.rp = rp;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getImgdirpath() {
        return imgdirpath;
    }

    public void setImgdirpath(String imgdirpath) {
        this.imgdirpath = imgdirpath;
    }

    public String getDirectoryname() {
        return directoryname;
    }

    public void setDirectoryname(String directoryname) {
        this.directoryname = directoryname;
    }

    public String getFilestrs() {
        return filestrs;
    }

    public void setFilestrs(String filestrs) {
        this.filestrs = filestrs;
    }

    /**
	 * 清理错误
	 */
    @Override
    public void validate() {
        this.clearErrorsAndMessages();
    }

    /**
	 * 读取服务器文件夹下得文件
	 */
    @Action(value = "readAllSeverDirectoryFile", results = { @Result(name = "json", type = "json") })
    public String readAllSeverDirectoryFile() {
        this.findDefaultDirectoryFile();
        return "json";
    }

    /**
	 * 获取服务器文件夹下文件
	 * 
	 * @return
	 */
    public void findDefaultDirectoryFile() {
        int currentPage = page;
        int lineSize = rp;
        if (Validate.StrNotNull(this.getDirectoryname())) {
            String savedir = "/Uploads/";
            String savePath = ServletActionContext.getServletContext().getRealPath("");
            savePath = savePath + savedir + this.getDirectoryname() + "/";
            File file = new File(savePath);
            String[] filelist = file.list();
            SimpleDateFormat sDateFormat;
            sDateFormat = new SimpleDateFormat("yyyyMMddmmss");
            String nowTimeStr = "";
            list.clear();
            for (int i = 0; i < filelist.length; i++) {
                File f = new File(file.getPath(), filelist[i]);
                if (f.isFile() && !f.isHidden()) {
                    nowTimeStr = sDateFormat.format(new Date(f.lastModified()));
                    ServerFileInfo sfi = new ServerFileInfo();
                    sfi.setDirectoryname(f.getName());
                    sfi.setCreatetime(nowTimeStr);
                    sfi.setImgfilepath(savedir + this.getDirectoryname() + "/" + f.getName());
                    list.add(sfi);
                }
            }
            if (list != null && list.size() > 0) {
                total = currentPage * lineSize > list.size() ? list.size() : currentPage * lineSize;
                list.subList((currentPage - 1) * lineSize, total);
                this.ProcessAllSeverDirectoryFile(list);
            }
        }
    }

    public void ProcessAllSeverDirectoryFile(List list) {
        rows.clear();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            ServerFileInfo sfi = (ServerFileInfo) it.next();
            Map<String, Object> cellMap = new HashMap<String, Object>();
            cellMap.put("id", sfi.getDirectoryname());
            cellMap.put("cell", new Object[] { "<img width='100px' height='100px' src='" + sfi.getImgfilepath() + "'/><br/><a target='_blank' href='" + sfi.getImgfilepath() + "'>" + sfi.getDirectoryname() + "</a>", sfi.getCreatetime() });
            rows.add(cellMap);
        }
    }

    /**
	 * 创建服务器目录
	 * 
	 * @return
	 */
    @Action(value = "createDirectory", results = { @Result(name = "json", type = "json") })
    public String createDirectory() {
        String savedir = "/Uploads/";
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        savePath = savePath + savedir + this.getImgdirpath() + "/";
        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
            this.setSucflag(true);
            return "json";
        } else {
            this.setSucflag(false);
            return "json";
        }
    }

    /**
	 * 检测目录是否存在
	 * 
	 * @return
	 */
    public String isexistdir() {
        String nowTimeStr = "";
        String savedir = "/Uploads/";
        String realpath = "";
        SimpleDateFormat sDateFormat;
        sDateFormat = new SimpleDateFormat("yyyyMMdd");
        nowTimeStr = sDateFormat.format(new Date());
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        savePath = savePath + savedir + nowTimeStr + "/";
        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
            realpath = savedir + nowTimeStr + "/";
            return realpath;
        } else {
            realpath = savedir + nowTimeStr + "/";
            return realpath;
        }
    }

    /**
	 * 异步图片
	 * 
	 * @throws IOException
	 */
    @Action(value = "ajaxFileUploads", results = {  })
    public void ajaxFileUploads() throws IOException {
        String extName = "";
        String newFilename = "";
        String nowTimeStr = "";
        String realpath = "";
        if (Validate.StrNotNull(this.getImgdirpath())) {
            realpath = "Uploads/" + this.getImgdirpath() + "/";
        } else {
            realpath = this.isexistdir();
        }
        SimpleDateFormat sDateFormat;
        Random r = new Random();
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        savePath = savePath + realpath;
        HttpServletResponse response = ServletActionContext.getResponse();
        int rannum = (int) (r.nextDouble() * (99999 - 1000 + 1)) + 10000;
        sDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        nowTimeStr = sDateFormat.format(new Date());
        String filename = request.getHeader("X-File-Name");
        if (filename.lastIndexOf(".") >= 0) {
            extName = filename.substring(filename.lastIndexOf("."));
        }
        newFilename = nowTimeStr + rannum + extName;
        PrintWriter writer = null;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            writer = response.getWriter();
        } catch (IOException ex) {
            log.debug(ImgTAction.class.getName() + "has thrown an exception:" + ex.getMessage());
        }
        try {
            is = request.getInputStream();
            fos = new FileOutputStream(new File(savePath + newFilename));
            IOUtils.copy(is, fos);
            response.setStatus(response.SC_OK);
            writer.print("{success:'" + realpath + newFilename + "'}");
        } catch (FileNotFoundException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            log.debug(ImgTAction.class.getName() + "has thrown an exception: " + ex.getMessage());
        } catch (IOException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            log.debug(ImgTAction.class.getName() + "has thrown an exception: " + ex.getMessage());
        } finally {
            try {
                this.setImgdirpath(null);
                fos.close();
                is.close();
            } catch (IOException ignored) {
            }
        }
        writer.flush();
        writer.close();
    }

    /**
	 * 读取服务器文件
	 */
    @Action(value = "readAllSeverDirectory", results = { @Result(name = "json", type = "json") })
    public String readAllSeverDirectory() {
        if ("sc".equals(this.getQtype())) {
            this.findDefaultAllSeverDirectory();
            return "json";
        } else {
            if (Validate.StrisNull(this.getQuery())) {
                return "json";
            } else {
                return "json";
            }
        }
    }

    public void findDefaultAllSeverDirectory() {
        int currentPage = page;
        int lineSize = rp;
        String savedir = "/Uploads/";
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        savePath = savePath + savedir;
        File file = new File(savePath);
        String[] filelist = file.list();
        SimpleDateFormat sDateFormat;
        sDateFormat = new SimpleDateFormat("yyyyMMddmmss");
        String nowTimeStr = "";
        list.clear();
        for (int i = 0; i < filelist.length; i++) {
            File f = new File(file.getPath(), filelist[i]);
            if (f.isDirectory() && !f.getName().equals(".svn") && !f.isHidden()) {
                String filecount[] = f.list();
                nowTimeStr = sDateFormat.format(new Date(f.lastModified()));
                ServerFileInfo sfi = new ServerFileInfo();
                sfi.setDirectoryname(f.getName());
                sfi.setCreatetime(nowTimeStr);
                sfi.setCount(filecount.length);
                list.add(sfi);
            }
        }
        if (list != null && list.size() > 0) {
            total = currentPage * lineSize > list.size() ? list.size() : currentPage * lineSize;
            list.subList((currentPage - 1) * lineSize, total);
            this.ProcessAllSeverDirectory(list);
        }
    }

    public void ProcessAllSeverDirectory(List list) {
        rows.clear();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            ServerFileInfo sfi = (ServerFileInfo) it.next();
            Map<String, Object> cellMap = new HashMap<String, Object>();
            cellMap.put("id", sfi.getDirectoryname());
            cellMap.put("cell", new Object[] { "<a href='serverimglistmanagement.jsp?directoryname=" + sfi.getDirectoryname() + "#images&session=true" + "'>" + sfi.getDirectoryname() + "</a>", sfi.getCount(), sfi.getCreatetime() });
            rows.add(cellMap);
        }
    }

    /**
	 * 删除服务器端文件
	 */
    @Action(value = "delServerDirectoryFile", results = { @Result(name = "json", type = "json") })
    public String delServerDirectoryFile() {
        String savedir = "/Uploads/";
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        if (Validate.StrNotNull(this.getDirectoryname())) {
            String dirs[] = this.getDirectoryname().split(",");
            for (int i = 0; i < dirs.length; i++) {
                savePath = savePath + savedir + dirs[i] + "/";
                File file = new File(savePath);
                if (file.exists()) {
                    String[] filelist = file.list();
                    for (int j = 0; j < filelist.length; j++) {
                        File f = new File(file.getPath(), filelist[j]);
                        if (f.exists() && f.isFile() && !f.isHidden()) {
                            f.delete();
                        }
                    }
                    file.delete();
                }
            }
            this.setSucflag(true);
            return "json";
        }
        this.setSucflag(false);
        return "json";
    }

    /**
	 * 删除服务器上单个文件
	 * 
	 * @return
	 */
    @Action(value = "delServerFile", results = { @Result(name = "json", type = "json") })
    public String delServerFile() {
        String savedir = "/Uploads/";
        String savePath = ServletActionContext.getServletContext().getRealPath("");
        if (Validate.StrNotNull(this.getFilestrs())) {
            String dir = this.getDirectoryname().trim();
            String files[] = this.getFilestrs().split(",");
            for (int i = 0; i < files.length; i++) {
                savePath = savePath + savedir + dir + "/" + files[i];
                File f = new File(savePath);
                if (f.exists() && f.isFile() && !f.isHidden()) {
                    f.delete();
                }
            }
            this.setSucflag(true);
            return "json";
        }
        this.setSucflag(false);
        return "json";
    }
}
