package com.creawor.hz_market.t_information;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.hibernate.HibernateException;
import org.aos.util.UploadFileOne;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import client.sms.SMSService;
import com.creawor.hz_market.bean.UserDetails;
import com.creawor.hz_market.t_attach.t_attach;
import com.creawor.hz_market.t_attach.t_attach_EditMap;
import com.creawor.hz_market.t_attach.t_attach_Form;
import com.creawor.hz_market.t_attach.t_attach_QueryMap;
import com.creawor.hz_market.t_info_level.TInfoLevel;
import com.creawor.hz_market.t_info_level.TInfoLevel_QueryMap;
import com.creawor.hz_market.t_infor_review.t_infor_review;
import com.creawor.hz_market.t_infor_review.t_infor_review_EditMap;
import com.creawor.hz_market.t_infor_review.t_infor_review_QueryMap;
import com.creawor.hz_market.t_role.t_role;
import com.creawor.hz_market.t_user.t_user;
import com.creawor.hz_market.util.ComQuery;
import com.creawor.hz_market.util.LoginUtils;
import com.creawor.imei.base.BaseAction;
import com.creawor.imei.util.UUIDGenerator;
import com.creawor.km.util.CatcheUtil;

public class t_information_Manager extends BaseAction {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(t_information_Manager.class);

    public String doList(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doList(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pagenos = request.getParameter("pageno");
        if (null == pagenos || "null".equalsIgnoreCase(pagenos)) pagenos = "1";
        String pagesizes = request.getParameter("pagesize");
        if (null == pagesizes || "null".equalsIgnoreCase(pagesizes)) pagesizes = "10";
        UserDetails user = LoginUtils.getLoginUser(request);
        try {
            this.pageno = Integer.valueOf(pagenos);
        } catch (NumberFormatException e) {
            logger.error("doList(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)");
            this.pageno = new Integer(1);
        }
        if (pagesizes != null) this.pagesize = Integer.valueOf(pagesizes.trim());
        t_information_QueryMap query_map = new t_information_QueryMap();
        query_map.pageno = this.pageno;
        query_map.pagesize = this.pagesize;
        java.util.Iterator sl = null;
        String type = request.getParameter("type");
        String review_flag = request.getParameter("review_flag");
        String tmp_review_flag = review_flag;
        if (null == review_flag || "".equalsIgnoreCase(review_flag)) review_flag = "5";
        request.setAttribute("review_flag", review_flag);
        if ("100".equals(review_flag)) review_flag = null;
        if (null == type || "".equals(type) || "null".equals(type)) {
            type = (String) request.getSession().getAttribute("infotype");
            if (null == type || "".equals(type) || "null".equals(type)) {
                type = "info";
                request.getSession().setAttribute("infotype", type);
            }
        } else {
            request.getSession().setAttribute("infotype", type);
        }
        String from = request.getParameter("from");
        request.getSession().setAttribute("from", from);
        if (sql_string == null || sql_string.equals("") || sql_string.equalsIgnoreCase("null")) {
            List list = null;
            list = (List) CatcheUtil.getInstance().getSYSObject("t_informationList");
            if (null == list) {
                String keyStr = request.getParameter("keyStr");
                request.getSession().setAttribute("infokeyStr", keyStr);
                String date1 = request.getParameter("date1");
                String date2 = request.getParameter("date2");
                request.getSession().setAttribute("infodate1", date1);
                request.getSession().setAttribute("infodate2", date2);
                list = query_map.findByTypeList(type, review_flag, keyStr, date1, date2);
                CatcheUtil.getInstance().cacheSYSObj("t_informationList", list);
            }
            sl = list.iterator();
        } else {
            sl = query_map.findAll(this.sql_string, this.sql_param);
            request.setAttribute("sql_string", this.sql_string);
            request.setAttribute("sql_param", this.sql_param_str);
        }
        String userCounty = user.getDeptCode();
        ArrayList list = new ArrayList();
        t_user userVO = user.getUser();
        String username = user.getUsercode();
        String county = userVO.getDept_code();
        Set roles = userVO.getRoles();
        int roleId = 0;
        java.util.Iterator it = roles.iterator();
        t_role role = null;
        boolean showall = false;
        while (it.hasNext()) {
            role = (t_role) it.next();
            roleId = role.getId();
            if (1 == roleId || 6 == roleId) {
                showall = true;
            }
        }
        if (!showall) {
            for (; sl.hasNext(); ) {
                t_information item = (t_information) sl.next();
                if ("5".equals(review_flag) || null == review_flag) {
                    if ((userCounty.equals(item.getCounty()) || "����".equals(item.getOpentype())) && roleId != 14) {
                        list.add(item);
                    } else if (roleId == 14 && userCounty.equals(item.getCounty()) && username.equals(item.getWriter())) {
                        list.add(item);
                    }
                } else if ("0".equals(review_flag) || null == review_flag) {
                    if ((userCounty.equals(item.getCounty())) && roleId != 14) {
                        list.add(item);
                    } else if (roleId == 14 && userCounty.equals(item.getCounty()) && username.equals(item.getWriter())) {
                        list.add(item);
                    }
                }
            }
            request.setAttribute("sl", list.iterator());
        } else request.setAttribute("sl", sl);
        request.setAttribute("curpageno", pageno);
        if (pageno.intValue() > 1) request.setAttribute("prepage", new Integer(pageno.intValue() - 1)); else request.setAttribute("prepage", new Integer(1));
        int totalpage = 0;
        if ((query_map.totalrow % pagesize.intValue()) == 0) totalpage = query_map.totalrow / pagesize.intValue(); else totalpage = query_map.totalrow / pagesize.intValue() + 1;
        request.setAttribute("totalpage", new Integer(totalpage));
        if (pageno.intValue() < totalpage) request.setAttribute("nextpage", new Integer(pageno.intValue() + 1)); else request.setAttribute("nextpage", new Integer(totalpage));
        if (logger.isDebugEnabled()) {
            logger.debug("doList(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "list";
    }

    /**������ϸ��Ϣ��ʾ����*/
    public String doDetail(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doDetail(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pageno;
        try {
            request.setAttribute("sql_string", this.sql_string);
            request.setAttribute("sql_param", this.sql_param_str);
            pageno = request.getParameter("pageno");
            request.setAttribute("pageno", pageno);
        } catch (RuntimeException e1) {
            logger.error("doDetail(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e1);
            e1.printStackTrace();
        }
        t_information_QueryMap sqm = new t_information_QueryMap();
        String uid = request.getParameter("id");
        if (null == uid) uid = (String) request.getAttribute("id");
        t_information_Form sf = null;
        sf = sqm.getByID(uid);
        String type = sf.getType();
        request.getSession().setAttribute("infotype", type);
        t_attach_QueryMap attachMap = new t_attach_QueryMap();
        Iterator it = attachMap.findByInforId(uid);
        sf.setAttaches(it);
        t_infor_review_QueryMap inforReviewQuery = new t_infor_review_QueryMap();
        Iterator reviews = inforReviewQuery.findByIforId(uid);
        sf.setReviews(reviews);
        request.setAttribute("t_information_Form", sf);
        String review = request.getParameter("review");
        String from = request.getParameter("from");
        if (null == from) from = (String) request.getAttribute("from");
        request.getSession().setAttribute("from", from);
        if (null == review) review = (String) request.getAttribute("review");
        try {
            TInfoLevel_QueryMap query = new TInfoLevel_QueryMap();
            List ls = query.findType(sf.getInfo_type());
            request.setAttribute("infoType", ls);
        } catch (HibernateException e) {
            logger.error("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            e.printStackTrace();
        }
        if (null != review && "client".equals(from.trim())) {
            if (logger.isDebugEnabled()) {
                logger.debug("doDetail(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "review";
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("doDetail(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "detail";
        }
    }

    public String doDetailView(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doDetailView(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pageno;
        try {
            request.setAttribute("sql_string", this.sql_string);
            request.setAttribute("sql_param", this.sql_param_str);
            pageno = request.getParameter("pageno");
            request.setAttribute("pageno", pageno);
        } catch (RuntimeException e1) {
            logger.error("doDetailView(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e1);
            e1.printStackTrace();
        }
        t_information_QueryMap sqm = new t_information_QueryMap();
        String uid = request.getParameter("id");
        t_information_Form sf = null;
        sf = sqm.getByID(uid);
        t_attach_QueryMap attachMap = new t_attach_QueryMap();
        Iterator it = attachMap.findByInforId(uid);
        sf.setAttaches(it);
        request.setAttribute("t_information_Form", sf);
        if (logger.isDebugEnabled()) {
            logger.debug("doDetailView(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "detailView";
    }

    /**��������ҳ��*/
    public String doCreate(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("doCreate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pageno;
        try {
            request.setAttribute("sql_string", this.sql_string);
            request.setAttribute("sql_param", this.sql_param_str);
            pageno = request.getParameter("pageno");
            request.setAttribute("pageno", pageno);
        } catch (RuntimeException e1) {
            logger.error("doCreate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e1);
            e1.printStackTrace();
        }
        t_information_Form formobj = null;
        try {
            String selinfo_type = request.getParameter("selinfo_type");
            if (null == selinfo_type) selinfo_type = "����Ӫ��";
            formobj = new t_information_Form();
            formobj.setInfo_type(selinfo_type);
            String type = request.getParameter("type");
            request.setAttribute("type", type);
            TInfoLevel_QueryMap query = new TInfoLevel_QueryMap();
            List ls = query.findType(selinfo_type);
            request.setAttribute("infoType", ls);
        } catch (HibernateException e) {
            logger.error("doCreate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            e.printStackTrace();
        }
        request.setAttribute("t_information_Form", formobj);
        if (logger.isDebugEnabled()) {
            logger.debug("doCreate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "addpage";
    }

    public String doAdd(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        t_information_EditMap editMap = new t_information_EditMap();
        try {
            t_information_Form vo = null;
            vo = (t_information_Form) form;
            vo.setCompany(vo.getCounty());
            if ("����".equals(vo.getInfo_type())) {
                vo.setInfo_level(null);
                vo.setAlert_level(null);
            }
            String str_postFIX = "";
            int i_p = 0;
            editMap.add(vo);
            try {
                logger.info("���͹�˾�鱨��");
                String[] mobiles = request.getParameterValues("mobiles");
                vo.setMobiles(mobiles);
                SMSService.inforAlert(vo);
            } catch (Exception e) {
                logger.error("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            }
            String filename = vo.getFile().getFileName();
            if (null != filename && !"".equals(filename)) {
                FormFile file = vo.getFile();
                String realpath = getServlet().getServletContext().getRealPath("/");
                realpath = realpath.replaceAll("\\\\", "/");
                String inforId = vo.getId();
                String rootFilePath = getServlet().getServletContext().getRealPath(request.getContextPath());
                rootFilePath = (new StringBuilder(String.valueOf(rootFilePath))).append(UploadFileOne.strPath).toString();
                String strAppend = (new StringBuilder(String.valueOf(UUIDGenerator.nextHex()))).append(UploadFileOne.getFileType(file)).toString();
                if (file.getFileSize() != 0) {
                    file.getInputStream();
                    String name = file.getFileName();
                    i_p = file.getFileName().lastIndexOf(".");
                    str_postFIX = file.getFileName().substring(i_p, file.getFileName().length());
                    String fullPath = realpath + "attach/" + strAppend + str_postFIX;
                    t_attach attach = new t_attach();
                    attach.setAttach_fullname(fullPath);
                    attach.setAttach_name(name);
                    attach.setInfor_id(Integer.parseInt(inforId));
                    attach.setInsert_day(new Date());
                    attach.setUpdate_day(new Date());
                    t_attach_EditMap attachEdit = new t_attach_EditMap();
                    attachEdit.add(attach);
                    File sysfile = new File(fullPath);
                    if (!sysfile.exists()) {
                        sysfile.createNewFile();
                    }
                    java.io.OutputStream out = new FileOutputStream(sysfile);
                    org.apache.commons.io.IOUtils.copy(file.getInputStream(), out);
                    out.close();
                }
            }
        } catch (HibernateException e) {
            logger.error("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            ActionErrors errors = new ActionErrors();
            errors.add("org.apache.struts.action.GLOBAL_ERROR", new ActionError("error.database.save", e.toString()));
            saveErrors(request, errors);
            e.printStackTrace();
            request.setAttribute("t_information_Form", form);
            if (logger.isDebugEnabled()) {
                logger.debug("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "addpage";
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "aftersave";
    }

    public String doQuery(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("doQuery(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pageno;
        try {
            pageno = request.getParameter("pageno");
            request.setAttribute("pageno", pageno);
        } catch (RuntimeException e1) {
            logger.error("doQuery(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e1);
            e1.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doQuery(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "searchpage";
    }

    public String doEdit(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String pageno;
        try {
            request.setAttribute("sql_string", this.sql_string);
            request.setAttribute("sql_param", this.sql_param_str);
            pageno = request.getParameter("pageno");
            request.setAttribute("pageno", pageno);
        } catch (RuntimeException e1) {
            logger.error("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e1);
            e1.printStackTrace();
        }
        t_information_QueryMap query_map = new t_information_QueryMap();
        String uid = request.getParameter("id");
        if (null == uid) uid = (String) request.getAttribute("id");
        t_information_Form vo = null;
        query_map.session.flush();
        vo = query_map.getByID(uid);
        t_attach_QueryMap attachMap = new t_attach_QueryMap();
        Iterator it = attachMap.findByInforId(uid);
        vo.setAttaches(it);
        t_infor_review_QueryMap inforReviewQuery = new t_infor_review_QueryMap();
        Iterator reviews = inforReviewQuery.findByIforId(uid);
        vo.setReviews(reviews);
        request.setAttribute("t_information_Form", vo);
        String act = request.getParameter("act");
        try {
            TInfoLevel_QueryMap query = new TInfoLevel_QueryMap();
            List ls = query.findType(vo.getInfo_type());
            request.setAttribute("infoType", ls);
        } catch (HibernateException e) {
            logger.error("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            e.printStackTrace();
        }
        if (null != act && "edit1".equalsIgnoreCase(act)) {
            if (logger.isDebugEnabled()) {
                logger.debug("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "edit1";
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("doEdit(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "editpage";
        }
    }

    public String doDelattach(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doDelattach(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String attachid = request.getParameter("attachId");
        String uid = request.getParameter("id");
        t_information_Form vo = null;
        t_information_QueryMap query_map = new t_information_QueryMap();
        query_map.session.flush();
        vo = query_map.getByID(uid);
        t_attach_QueryMap attachMap = new t_attach_QueryMap();
        t_attach_Form atchform = attachMap.getByID(attachid);
        String fullName = atchform.getAttach_fullname();
        File file = new File(fullName);
        if (file.exists()) file.delete();
        attachMap.remove(t_attach.class, new Integer(attachid));
        Iterator it = attachMap.findByInforId(uid);
        vo.setAttaches(it);
        t_infor_review_QueryMap inforReviewQuery = new t_infor_review_QueryMap();
        Iterator reviews = inforReviewQuery.findByIforId(uid);
        vo.setReviews(reviews);
        request.setAttribute("t_information_Form", vo);
        if (logger.isDebugEnabled()) {
            logger.debug("doDelattach(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "editpage";
    }

    public String doUpdate(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doUpdate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        try {
            t_information_EditMap edit_map = new t_information_EditMap();
            t_information_Form vo = null;
            vo = (t_information_Form) form;
            if ("����".equals(vo.getInfo_type())) {
                vo.setInfo_level(null);
                vo.setAlert_level(null);
            }
            edit_map.update(vo);
            try {
                logger.info("���͹�˾�鱨��");
                String[] mobiles = request.getParameterValues("mobiles");
                vo.setMobiles(mobiles);
                SMSService.inforAlert(vo);
            } catch (Exception e) {
                logger.error("doAdd(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            }
        } catch (HibernateException e) {
            logger.error("doUpdate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            ActionErrors errors = new ActionErrors();
            errors.add("org.apache.struts.action.GLOBAL_ERROR", new ActionError("error.database.save", e.toString()));
            saveErrors(request, errors);
            e.printStackTrace();
            request.setAttribute("t_information_Form", form);
            if (logger.isDebugEnabled()) {
                logger.debug("doUpdate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "editpage";
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doUpdate(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "aftersave";
    }

    public String doUpdateFlag(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doUpdateFlag(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        String infoId = request.getParameter("id");
        try {
            t_information_EditMap edit_map = new t_information_EditMap();
            String sql = "update t_information set review_flag=5 where id=" + infoId;
            ComQuery stm = new ComQuery();
            stm.updateDB(sql);
        } catch (Exception e) {
            logger.error("doUpdateFlag(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            ActionErrors errors = new ActionErrors();
            errors.add("org.apache.struts.action.GLOBAL_ERROR", new ActionError("error.database.save", e.toString()));
            saveErrors(request, errors);
            e.printStackTrace();
            request.setAttribute("t_information_Form", form);
            if (logger.isDebugEnabled()) {
                logger.debug("doUpdateFlag(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "list";
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doUpdateFlag(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "aftersave";
    }

    public String doDelete(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - start");
        }
        t_information_EditMap edit_map = new t_information_EditMap();
        String uid = request.getParameter("id");
        try {
            try {
                t_attach_EditMap attacheeditMap = new t_attach_EditMap();
                attacheeditMap.delByInfo(uid);
            } catch (Exception e) {
                logger.error("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
                System.out.println("ɾ������?������");
            }
            try {
                t_infor_review_QueryMap reviewQuery = new t_infor_review_QueryMap();
                java.util.Iterator reviews = reviewQuery.findByIforId(uid);
                if (null != reviews) {
                    while (reviews.hasNext()) {
                        this.delReview((t_infor_review) reviews.next());
                    }
                }
            } catch (Exception e) {
                logger.error("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
                e.printStackTrace();
                System.out.println("ɾ�����۳��?����");
            }
            edit_map.delete(uid);
        } catch (HibernateException e) {
            logger.error("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)", e);
            ActionErrors errors = new ActionErrors();
            errors.add("org.apache.struts.action.GLOBAL_ERROR", new ActionError("error.database.save", e.toString()));
            saveErrors(request, errors);
            e.printStackTrace();
            if (logger.isDebugEnabled()) {
                logger.debug("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
            }
            return "list";
        }
        if (logger.isDebugEnabled()) {
            logger.debug("doDelete(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse) - end");
        }
        return "aftersave";
    }

    private void delReview(t_infor_review review) {
        if (logger.isDebugEnabled()) {
            logger.debug("delReview(t_infor_review) - start");
        }
        if (null != review) {
            try {
                delsoFile(review.getAttachfullname1());
                delsoFile(review.getAttachfullname2());
                delsoFile(review.getAttachfullname3());
                t_infor_review_EditMap reviewEdit = new t_infor_review_EditMap();
                reviewEdit.delete(String.valueOf(review.getId()));
            } catch (Exception e) {
                logger.error("delReview(t_infor_review)", e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("delReview(t_infor_review) - end");
        }
    }

    private void delsoFile(String path) {
        if (logger.isDebugEnabled()) {
            logger.debug("delsoFile(String) - start");
        }
        if (null != path) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("delsoFile(String) - end");
        }
    }
}
