package org.eledge.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import metadata.FormatDescription;
import metadata.FormatIdentification;
import org.apache.tapestry.IExternalPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.event.PageRenderListener;
import org.eledge.Eledge;
import org.eledge.domain.StylesheetProvider;
import org.eledge.domain.permissions.ManageCoursePermission;
import org.eledge.domain.permissions.PermissionRequired;

/**
 * @author robertz
 * 
 */
public class EditStylesheet extends EledgeSecureBasePage implements PermissionRequired, PageRenderListener, IExternalPage {

    @Override
    public boolean isCurrentCourseRequired() {
        return false;
    }

    @Override
    public boolean shouldSetCurrentCourse() {
        return true;
    }

    public void pageBeginRender(PageEvent event) {
        if (!event.getRequestCycle().isRewinding()) {
            setupFile(event.getRequestCycle());
        }
    }

    public File getSFile() {
        return (File) getProperty("file");
    }

    public void setSFile(File f) {
        setProperty("file", f);
    }

    public String getPermissionName() {
        return ManageCoursePermission.NAME;
    }

    public boolean getfileIsTextual() {
        File f = getSFile();
        if (f == null) {
            return false;
        }
        if (!f.exists()) {
            return true;
        }
        FormatDescription desc = FormatIdentification.identify(f);
        if (desc == null) {
            try {
                FileInputStream fis = new FileInputStream(f);
                if (fis.getChannel().size() == 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        } else {
            return desc.isTextual();
        }
    }

    public String getFileText() {
        File f = getSFile();
        if (f == null || !f.exists()) {
            return "";
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            String s = new String(bytes);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void setFileText(String text) {
        File f = getSFile();
        if (f == null) {
            return;
        }
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fout = new FileOutputStream(f);
            fout.write(text.getBytes());
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void activateExternalPage(Object[] params, IRequestCycle cycle) {
        if (!Eledge.checkParameters(params, StylesheetProvider.class)) {
            Eledge.doErrorPage("badurl", cycle, true, this);
        }
        setProperty("provider", params[0]);
    }

    public void restoreFile(IRequestCycle cycle) {
        setupFile(cycle);
    }

    private void setupFile(IRequestCycle cycle) {
        if (getSFile() == null) {
            StylesheetProvider provider = (StylesheetProvider) getProperty("provider");
            String path = provider.getStylesheetPath();
            File f = Eledge.fileForStylesheetPath(path, cycle.getRequestContext().getServlet().getServletContext());
            setSFile(f);
        }
    }
}
