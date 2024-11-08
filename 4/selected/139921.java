package com.company.erp.metadata.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.company.common.Constant;
import com.company.common.Hibernate.action.BaseAction;
import com.company.common.util.DateUtil;
import com.company.erp.customer.model.TBCustomer;
import com.company.erp.metadata.dao.TBMaterielDAO;
import com.company.erp.metadata.dao.TBMaterielTypeDAO;
import com.company.erp.metadata.dao.TBUnitDAO;
import com.company.erp.metadata.model.TBMateriel;
import com.company.erp.metadata.model.TBMaterielType;
import com.company.erp.metadata.model.TBUnit;
import com.company.sys.model.LoginUser;

public class MaterielManager extends BaseAction {

    private static final Logger logger = LoggerFactory.getLogger(MaterielManager.class);

    private TBMateriel obj;

    private TBMaterielDAO dao;

    private List<TBMaterielType> typeList = new ArrayList<TBMaterielType>();

    private List<TBUnit> unitList = new ArrayList<TBUnit>();

    private TBMaterielTypeDAO tbMaterielTypeDAO;

    private TBUnitDAO tbUnitDAO;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private String fileCaption;

    public String listPage() {
        getRight();
        String value = getRightMap().get(Constant.COST_SEARCH);
        if (Constant.RIGHT_NO.equals(value)) {
            return "noPermission";
        } else {
            Map<String, Object> map = new HashMap<String, Object>();
            if (obj != null) {
                if (obj.getName() != null && !obj.getName().equals("")) {
                    map.put("name", obj.getName());
                }
                if (obj.getType() != null && !obj.getType().equals("")) {
                    map.put("type", obj.getType());
                }
            }
            ;
            pageInfo = dao.queryForPage(TBMateriel.class, map, getPageInfo());
            return "listPage";
        }
    }

    public void lxml() {
        String word = getServletRequest().getParameter("word");
        getServletRequest().setAttribute("word", word);
        String filePath;
        try {
            word = URLDecoder.decode(word, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        List list = dao.findbyAjax("TBMateriel", "name", word);
        Element root = new Element("words");
        Document Doc = new Document(root);
        for (int i = 0; i < list.size(); i++) {
            Element elements = new Element("word");
            Object[] object = (Object[]) list.get(i);
            elements.setText((String) object[1]);
            root.addContent(elements);
        }
        XMLOutputter XMLOut = new XMLOutputter();
        String xmlName = "temp" + System.currentTimeMillis() + ".xml";
        try {
            XMLOut.output(Doc, new FileOutputStream(getServletRequest().getRealPath("") + "\\" + xmlName));
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        try {
            getServletResponse().setContentType("text/xml;charset=utf-8");
            getServletRequest().getRequestDispatcher(xmlName).forward(getServletRequest(), getServletResponse());
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getMateerielList() {
        String s = dao.getNamesByType(null);
        getSuccessMsg(s);
    }

    public String addPage() {
        getRight();
        typeList = tbMaterielTypeDAO.findAll();
        unitList = tbUnitDAO.findAll();
        return "addPage";
    }

    public String viewPage() {
        getRight();
        obj = dao.findById(obj.getId());
        return "viewPage";
    }

    public String editPage() {
        getRight();
        typeList = tbMaterielTypeDAO.findAll();
        unitList = tbUnitDAO.findAll();
        obj = dao.findById(obj.getId());
        return "editPage";
    }

    public String create() {
        LoginUser loginUser = getLoginUser();
        obj.setCreator(loginUser.getUserId());
        dao.save(obj);
        obj = null;
        return listPage();
    }

    public String update() {
        TBMateriel old = dao.findById(obj.getId());
        copyUpdatedProperties("obj", this.obj, old);
        LoginUser loginUser = getLoginUser();
        old.setModifior(loginUser.getUserId());
        old.setModifyDate(DateUtil.getNowDateForShort());
        dao.attachDirty(old);
        obj = null;
        return listPage();
    }

    public void delete() {
        dao.delete(obj.getId());
        getSuccessMsg("删除成功");
    }

    public String datchDelete() {
        String checkBoxItem[] = getServletRequest().getParameterValues("checkBoxItem");
        dao.datchDelete(checkBoxItem);
        return listPage();
    }

    public void getMaterielList() {
        String s = dao.getNamesByType(null);
        getSuccessMsg(s);
    }

    public void getO() {
        obj = dao.findById(obj.getId());
        String tString = obj.getType() + ":" + obj.getPrice() + ":" + obj.getUnitName();
        getSuccessMsg(tString);
    }

    public String printPage() {
        Map<String, Object> map = new HashMap<String, Object>();
        if (obj != null) {
            if (obj.getName() != null && !obj.getName().equals("")) {
                map.put("name", obj.getName());
            }
            if (obj.getType() != null && !obj.getType().equals("")) {
                map.put("type", obj.getType());
            }
        }
        ;
        pageInfo = dao.queryForPage(TBMateriel.class, map, getPageInfo());
        return "printPage";
    }

    public String importPage() {
        return "importExcel";
    }

    public String importExcel() {
        String targetDirectory = getServletRequest().getRealPath("/upload");
        String targetFileName;
        try {
            targetFileName = uploadFileName;
            File target = new File(targetDirectory, targetFileName);
            if (target.exists()) {
                System.out.println("exists");
            } else {
                System.out.println("no exists");
            }
            FileUtils.copyFile(upload, target);
            if (target.exists()) {
                System.out.println("exists");
            } else {
                System.out.println("no exists");
            }
            upload.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "importResult";
    }

    public TBMaterielDAO getDao() {
        return dao;
    }

    public void setDao(TBMaterielDAO dao) {
        this.dao = dao;
    }

    public TBMateriel getObj() {
        return obj;
    }

    public void setObj(TBMateriel obj) {
        this.obj = obj;
    }

    public TBMaterielTypeDAO getTbMaterielTypeDAO() {
        return tbMaterielTypeDAO;
    }

    public void setTbMaterielTypeDAO(TBMaterielTypeDAO tbMaterielTypeDAO) {
        this.tbMaterielTypeDAO = tbMaterielTypeDAO;
    }

    public List<TBMaterielType> getTypeList() {
        return typeList;
    }

    public void setTypeList(List<TBMaterielType> typeList) {
        this.typeList = typeList;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public String getFileCaption() {
        return fileCaption;
    }

    public void setFileCaption(String fileCaption) {
        this.fileCaption = fileCaption;
    }

    public List<TBUnit> getUnitList() {
        return unitList;
    }

    public void setUnitList(List<TBUnit> unitList) {
        this.unitList = unitList;
    }

    public TBUnitDAO getTbUnitDAO() {
        return tbUnitDAO;
    }

    public void setTbUnitDAO(TBUnitDAO tbUnitDAO) {
        this.tbUnitDAO = tbUnitDAO;
    }
}
