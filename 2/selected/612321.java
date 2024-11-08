package cn.myapps.core.sysconfig.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import cn.myapps.core.sysconfig.util.ExportUtil;
import com.opensymphony.xwork.ActionSupport;

public class ExportAction extends ActionSupport {

    private static final long serialVersionUID = 4087246225170028124L;

    public InputStream getExportFile() {
        URL url = ExportAction.class.getClassLoader().getResource("sysConfig.xml");
        if (url != null) try {
            return url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String doExport() {
        try {
            ExportUtil.export();
        } catch (Exception e) {
            this.addFieldError("export.error", e.getMessage());
            return INPUT;
        }
        return SUCCESS;
    }
}
