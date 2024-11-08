package org.cmsuite2.business.handler;

import it.ec.commons.web.UploadBean;
import it.ec.commons.web.UploadUtil;
import it.ec.commons.web.ValidateException;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.cmsuite2.model.activity.ActivityDAO;

public class LicenseHandler {

    private static Logger logger = Logger.getLogger(LicenseHandler.class);

    private ActivityDAO activityDao;

    private UploadUtil licenseUploadUtil;

    public byte[] step1(UploadBean uploadBean) throws IOException, ValidateException {
        if (logger.isInfoEnabled()) logger.info("step1[" + uploadBean + "]");
        byte[] xml = null;
        try {
            licenseUploadUtil.setNullable(false);
            licenseUploadUtil.validate(uploadBean);
            if (uploadBean != null && uploadBean.getMyFile() != null) {
                xml = FileUtils.readFileToByteArray(uploadBean.getMyFile());
                File licenseDir = new File(ServletActionContext.getServletContext().getRealPath("license"));
                if (!licenseDir.exists()) licenseDir.mkdir();
                File licenseFile = new File(licenseDir.getAbsoluteFile() + File.separator + "license_temp.xml");
                FileUtils.copyFile(uploadBean.getMyFile(), licenseFile);
            }
        } catch (ValidateException e) {
            throw e;
        }
        return xml;
    }

    public void step2() throws IOException {
        if (logger.isInfoEnabled()) logger.info("step2[]");
        File licenseDir = new File(ServletActionContext.getServletContext().getRealPath("license"));
        if (!licenseDir.exists()) licenseDir.mkdir();
        File tempLicense = new File(licenseDir.getAbsoluteFile() + File.separator + "license_temp.xml");
        File licenseFile = new File(licenseDir.getAbsoluteFile() + File.separator + "license.xml");
        if (licenseFile.exists()) licenseFile.delete();
        FileUtils.copyFile(tempLicense, licenseFile);
        FileUtils.deleteQuietly(tempLicense);
        try {
            Thread.sleep(5000l);
        } catch (InterruptedException e) {
            logger.error(e, e);
        }
    }

    public UploadUtil getLicenseUploadUtil() {
        return licenseUploadUtil;
    }

    public void setLicenseUploadUtil(UploadUtil licenseUploadUtil) {
        this.licenseUploadUtil = licenseUploadUtil;
    }

    public ActivityDAO getActivityDao() {
        return activityDao;
    }

    public void setActivityDao(ActivityDAO activityDao) {
        this.activityDao = activityDao;
    }
}
