package com.creawor.hz_market.t_review_attach;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.aos.util.UploadFileOne;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import com.creawor.hz_market.t_advertisement.UploadFileForm;
import com.creawor.hz_market.t_attach.t_attach;
import com.creawor.hz_market.t_attach.t_attach_EditMap;
import com.creawor.imei.base.BaseAction;
import com.creawor.imei.util.UUIDGenerator;

public class UploadFileAction extends BaseAction {

    public String doAdd(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        UploadFileForm vo = (UploadFileForm) form;
        FormFile file = vo.getFile();
        String inforId = request.getParameter("inforId");
        System.out.println("inforId=" + inforId);
        if (file != null) {
            String realpath = getServlet().getServletContext().getRealPath("/");
            realpath = realpath.replaceAll("\\\\", "/");
            String rootFilePath = getServlet().getServletContext().getRealPath(request.getContextPath());
            rootFilePath = (new StringBuilder(String.valueOf(rootFilePath))).append(UploadFileOne.strPath).toString();
            String strAppend = (new StringBuilder(String.valueOf(UUIDGenerator.nextHex()))).append(UploadFileOne.getFileType(file)).toString();
            if (file.getFileSize() != 0) {
                file.getInputStream();
                String name = file.getFileName();
                String fullPath = realpath + "attach/" + strAppend + name;
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
                System.out.println("file name is :" + name);
            }
        }
        request.setAttribute("operating-status", "�����ɹ�!  ��ӭ����ʹ�á�");
        System.out.println("in the end....");
        return "aftersave";
    }
}
