package com.hs.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import com.hs.core.BaseAction;
import com.hs.core.Enterprise;
import com.hs.core.LicenceUtils;
import com.hs.core.SystemContext;
import com.opensymphony.webwork.ServletActionContext;

/**
 * @author <a href="mailto:guangzong@gmail.com">Guangzong Syu</a>
 *
 */
public class LicenseAction extends BaseAction {

    private static final long serialVersionUID = 7851740276652667356L;

    private File upload;

    /**
     * @param upload the upload to set
     */
    public void setUpload(File upload) {
        this.upload = upload;
    }

    @Override
    public String execute() throws Exception {
        SystemContext sc = getSystemContext();
        if (sc.getExpireTime() == -1) {
            return LOGIN;
        } else if (upload != null) {
            try {
                Enterprise e = LicenceUtils.get(upload);
                sc.setEnterpriseName(e.getEnterpriseName());
                sc.setExpireTime(e.getExpireTime());
                String webPath = ServletActionContext.getServletContext().getRealPath("/");
                File desFile = new File(webPath, LicenceUtils.LICENCE_FILE_NAME);
                FileChannel sourceChannel = new FileInputStream(upload).getChannel();
                FileChannel destinationChannel = new FileOutputStream(desFile).getChannel();
                sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
                sourceChannel.close();
                destinationChannel.close();
                return LOGIN;
            } catch (Exception e) {
            }
        }
        return "license";
    }
}
