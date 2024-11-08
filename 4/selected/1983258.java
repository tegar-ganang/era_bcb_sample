package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import java.io.*;
import java.awt.*;

/** Initiates extention in Moneydance, passes Moneydance data
 * to other classes
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class Main extends FeatureModule {

    private SecReportFrame reportWindow = null;

    public void init() {
        FeatureModuleContext context = getContext();
        try {
            context.registerFeature(this, "showreportwindow", getIcon("invextension"), getName());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void cleanup() {
        closeConsole();
    }

    private Image getIcon(String action) {
        try {
            ClassLoader cl = getClass().getClassLoader();
            java.io.InputStream in = cl.getResourceAsStream("/com/moneydance/modules/features/myextension/icon.gif");
            if (in != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
                byte buf[] = new byte[256];
                int n = 0;
                while ((n = in.read(buf, 0, buf.length)) >= 0) bout.write(buf, 0, n);
                return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
            }
        } catch (Throwable e) {
        }
        return null;
    }

    public void invoke(String uri) {
        String command = uri;
        @SuppressWarnings("unused") String parameters = "";
        int theIdx = uri.indexOf('?');
        if (theIdx >= 0) {
            command = uri.substring(0, theIdx);
            parameters = uri.substring(theIdx + 1);
        } else {
            theIdx = uri.indexOf(':');
            if (theIdx >= 0) {
                command = uri.substring(0, theIdx);
            }
        }
        if (command.equals("showreportwindow")) {
            showReportWindow();
        }
    }

    public String getName() {
        return "Investment Reports";
    }

    private synchronized void showReportWindow() {
        if (reportWindow == null) {
            reportWindow = new SecReportFrame(this);
            reportWindow.setVisible(true);
        } else {
            reportWindow.setVisible(true);
            reportWindow.toFront();
            reportWindow.requestFocus();
        }
    }

    FeatureModuleContext getUnprotectedContext() {
        return getContext();
    }

    synchronized void closeConsole() {
        if (reportWindow != null) {
            reportWindow.goAway();
            reportWindow = null;
            System.gc();
        }
    }
}
