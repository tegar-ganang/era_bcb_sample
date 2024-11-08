package com.shine.sourceflow.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JsonConfig;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.shine.DBUtil.model.DBModel;
import com.shine.sourceflow.model.GenericDto;
import com.shine.sourceflow.service.GenericService;
import com.shine.sourceflow.utils.Pagination;

/**
 * 通用ACTION
 */
public abstract class GenericAction extends ActionSupport implements ServletRequestAware, ServletResponseAware {

    private static final long serialVersionUID = -1601552356762245009L;

    public static final String DATA_DEFAULT = "default";

    /** 页面输出编码 */
    private static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";

    /** 数据增删改查返回值 */
    public static final String DATA_LIST = "list";

    public static final String DATA_ADD = "add";

    public static final String DATA_EDIT = "edit";

    public static final String DATA_DELETE = "delete";

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected GenericService service;

    protected GenericDto dto;

    /** 查询返回数据，可能有多条不同查询结果集 */
    protected static Map<String, DBModel> dbModels = new HashMap<String, DBModel>();

    /** 查询报表JSON返回值，可能有多个报表 */
    protected Map<String, String> charts = new HashMap<String, String>();

    @Override
    public void setServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void setServletResponse(HttpServletResponse response) {
        this.response = response;
    }

    public GenericDto getDto() {
        return this.dto;
    }

    public Map<String, DBModel> getDbModels() {
        return dbModels;
    }

    public Map<String, String> getCharts() {
        return this.charts;
    }

    public DBModel getDefaultDbModel() {
        return dbModels.get(DATA_DEFAULT);
    }

    /**
	 * 查询数据
	 * 
	 * @return
	 */
    public String list() {
        this.dto.init(this.request);
        dbModels = this.service.list(this.dto);
        return DATA_LIST;
    }

    /**
	 * 添加数据
	 * 
	 * @return
	 */
    public String add() {
        return DATA_ADD;
    }

    /**
	 * 编辑数据
	 * 
	 * @return
	 */
    public String edit() {
        return DATA_EDIT;
    }

    /**
	 * 删除数据
	 * 
	 * @return
	 */
    public String delete() {
        return DATA_DELETE;
    }

    /**
	 * 把集合数据转换成对应的Json对象到页面上
	 */
    protected void printOutJsonArray(List<?> list, Pagination page) {
        JSONArray jsonarry = JSONArray.fromObject(list, new JsonConfig());
        StringBuffer jsonStr = new StringBuffer(500);
        jsonStr.append("{").append("page:{");
        jsonStr.append("perSize:").append(page.getPerPage());
        jsonStr.append(",total:").append(page.getTotalPage());
        jsonStr.append(",cur:").append(page.getCurrentPage());
        jsonStr.append(",recordStart:").append(page.getRecordStart());
        jsonStr.append(",recordEnd:").append(page.getRecordEnd());
        jsonStr.append(",recordsTotal:").append(page.getTotalRecord());
        jsonStr.append("},data:");
        jsonStr.append(jsonarry.toString());
        jsonStr.append("}");
        printOutText(jsonStr.toString());
    }

    protected void printOutText(String text) {
        printOut(text, CONTENT_TYPE_HTML);
    }

    protected void printOut(String text, String contentType) {
        if (text == null || contentType == null) return;
        try {
            printOut(text.getBytes("utf-8"), contentType);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    protected void printOut(final byte[] data, final String contentType) {
        if (data == null || contentType == null) return;
        ActionContext.getContext().getActionInvocation().getProxy().setExecuteResult(false);
        OutputStream os = null;
        try {
            this.response.setContentType(contentType);
            this.response.setCharacterEncoding("UTF-8");
            this.response.setHeader("Pragma", "No-cache");
            this.response.setHeader("Cache-Control", "no-cache");
            this.response.setDateHeader("Expires", 0);
            os = this.response.getOutputStream();
            os.write(data);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                os.flush();
                ServletActionContext.getPageContext().getOut().clear();
                ServletActionContext.getPageContext().pushBody();
            } catch (Exception e) {
            }
            try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * 下载文件
	 * @param srcFullName 服务器上完整文件名
	 * @param saveAsName 客户端保存的文件名
	 * @return
	 */
    protected void download(String srcFullName, String saveAsName) {
        HttpServletResponse response = ServletActionContext.getResponse();
        InputStream is = null;
        OutputStream os = null;
        try {
            response.setContentType("application/x-msdownload;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + saveAsName);
            os = response.getOutputStream();
            is = new FileInputStream(srcFullName);
            byte[] b = new byte[1024];
            while (is.read(b) != -1) os.write(b);
            b = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                os.flush();
                ServletActionContext.getPageContext().getOut().clear();
                ServletActionContext.getPageContext().pushBody();
            } catch (Exception e) {
            }
            try {
                os.close();
                is.close();
            } catch (Exception e) {
            }
        }
    }
}
