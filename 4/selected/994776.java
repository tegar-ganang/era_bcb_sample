package com.nhncorp.cubridqa;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import com.nhncorp.cubridqa.utils.EnvGetter;
import com.nhncorp.cubridqa.utils.PropertiesUtil;

/**
 * @ClassName: ApplicationWorkbenchWindowAdvisor
 * @Description provides window-level advice .for example showing or hiding the
 *              menu ,toolbar ,and status line .and in configuring the controls
 *              shown in the window .
 * 
 * @date 2009-9-4
 * @version V1.0 Copyright (C) www.nhn.com
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    private static final class FileNameFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return !".svn".equalsIgnoreCase(name);
        }
    }

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }

    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setShowMenuBar(true);
        if (System.getProperty("os.name").equalsIgnoreCase("AIX")) {
            configurer.setInitialSize(new Point(1280, 1024));
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            configurer.setInitialSize(new Point(screenSize.width, screenSize.height));
        }
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(false);
        configurer.setTitle("CUBRIDQA");
        configurer.setShowProgressIndicator(true);
        System.out.println("================= begin to  checkPropertiesPath ====================== ");
        checkPropertiesPath();
        com.nhncorp.cubridqa.utils.FileUtil.createXml();
    }

    /**
	 * check the system configuration parameter PROPERTIES_PATH exist or not and
	 * get the detail configuration info through this parameter .
	 * 
	 */
    private void checkPropertiesPath() {
        Map<String, String> env = EnvGetter.getenv();
        System.out.println("================= check  PROPERTIES_PATH and QA_REPOSITORY ====================== ");
        if (!env.containsKey("PROPERTIES_PATH") || !env.containsKey("QA_REPOSITORY")) {
            MessageDialog.openError(null, "PROPERTIES_PATH NOT EXIST OR QA_REPOSITORY NOT EXIST!", "PROPERTIES_PATH or QA_REPOSITORY environment variable not exist, please set!");
        } else {
            System.out.println("================= PROPERTIES_PATH or QA_REPOSITORY is exists ====================== ");
            File configurationTemplate = new File(FilenameUtils.concat(EnvGetter.getenv("QA_REPOSITORY"), "configuration_template"));
            File configuration = new File(FilenameUtils.concat(EnvGetter.getenv("QA_REPOSITORY"), "configuration"));
            if (!configuration.exists()) {
                configuration.mkdir();
            }
            FileNameFilter fileNameFilter = new FileNameFilter();
            File[] configurationFiles = configuration.listFiles(fileNameFilter);
            if (configurationFiles.length == 0) {
                File[] configurations = configurationTemplate.listFiles(fileNameFilter);
                for (File file : configurations) {
                    try {
                        if (file.isFile()) {
                            FileUtils.copyFileToDirectory(file, configuration);
                        } else {
                            FileUtils.copyDirectoryToDirectory(file, configuration);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            StringBuilder sb = new StringBuilder(EnvGetter.getenv("PROPERTIES_PATH"));
            sb.append("/properties/local.properties");
            File propFile = new File(sb.toString());
            System.out.println("================= make file $PROPERTIES_PATH/properties/local.properties ====================== ");
            if (!propFile.exists()) {
                MessageDialog.openError(null, "PROPERTIES_PATH  ERROR!", "PROPERTIES_PATH environment variable error, please set it again!");
            } else {
                boolean importCubrid = false;
                String localPath = PropertiesUtil.getValue("local.path");
                System.out.println("================= localPath=" + localPath + " ====================== ");
                if (null == localPath || "".equals(localPath)) {
                    importCubrid = true;
                } else {
                    File dir = new File(localPath);
                    if (!dir.isDirectory() || !dir.exists()) {
                        importCubrid = true;
                    }
                }
                if (importCubrid) {
                    String qa_repository_path = EnvGetter.getenv("QA_REPOSITORY");
                    System.out.println("================= make file $QA_REPOSITORY ====================== ");
                    if (new File(qa_repository_path).exists() && new File(qa_repository_path).isDirectory()) {
                        qa_repository_path = qa_repository_path.replaceAll("\\\\", "/");
                        if (!qa_repository_path.endsWith("/")) {
                            qa_repository_path = qa_repository_path + "/";
                        }
                        PropertiesUtil.setValue("local.path", qa_repository_path);
                    }
                } else {
                    System.out.println("--- all ok! ---");
                }
            }
        }
    }
}
