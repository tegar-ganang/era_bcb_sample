package ar.com.coonocer.CodingJoy.menu.tools.plugins.installer.revengengine;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import ar.com.coonocer.CodingJoy.menu.tools.plugins.installer.bl.GeneratorInstallerBL;
import ar.com.coonocer.CodingJoy.model.ApplicationSettings;
import ar.com.coonocer.CodingJoy.model.Project;
import ar.com.coonocer.CodingJoy.model.TreeNode;
import ar.com.coonocer.CodingJoy.model.helpers.node.RevEngEnginesPackage_App_PIn_M1_EntityHelper;
import ar.com.coonocer.CodingJoy.model.helpers.utils.Project_App_Mdl_M1_Utils;
import ar.com.coonocer.CodingJoy.model.helpers.utils.Project_Prj_Mdl_M1_Utils;
import ar.com.coonocer.CodingJoy.model.serialization.application.ApplicationXMLPersistenceUtil;
import ar.com.coonocer.CodingJoy.model.serialization.project.ProjectXMLPersistenceUtil;
import ar.com.coonocer.CodingJoy.session.CodingJoySessionUtil;

public class RevEngEngineInstallerBL {

    private static Logger logger = Logger.getLogger(RevEngEngineInstallerBL.class);

    public static boolean installRevEngEngine(HttpServletRequest request) throws Exception {
        String auxTempRootFolder = ar.com.coonocer.CodingJoy.utils.FileUtils.getTempFolder();
        if (!new File(auxTempRootFolder).exists()) {
            if (!new File(auxTempRootFolder).mkdirs()) {
                return false;
            }
        }
        GeneratorInstallerBL.saveFilesInRequestToFolder(request, auxTempRootFolder);
        ApplicationSettings applicationSettings = CodingJoySessionUtil.getApplicationSettings(request.getSession());
        boolean ok = RevEngEngineInstallerBL.installFile(applicationSettings, auxTempRootFolder);
        if (ok) {
            ApplicationXMLPersistenceUtil.save(applicationSettings);
        }
        RevEngEngineInstallerBL.removeTempFolder(auxTempRootFolder);
        new File(auxTempRootFolder).delete();
        return ok;
    }

    private static boolean validatePackagesDoNotExistAlready(ApplicationSettings applicationSettings, List<TreeNode> revEngEnginesPackages2) {
        for (Iterator<TreeNode> iter = revEngEnginesPackages2.iterator(); iter.hasNext(); ) {
            TreeNode revEngEnginesPackageNode = iter.next();
            String revEngEnginesPackageKey = RevEngEnginesPackage_App_PIn_M1_EntityHelper.getKey(revEngEnginesPackageNode);
            TreeNode aux2 = Project_App_Mdl_M1_Utils.getRevEngEnginesPackageByKey(applicationSettings, revEngEnginesPackageKey);
            if (aux2 != null) {
                logger.error("RevEngEngines package with key=" + revEngEnginesPackageKey + " already exists in current project.");
                return false;
            }
        }
        return true;
    }

    public static boolean installFile(ApplicationSettings applicationSettings, String auxTempRootFolder) throws Exception {
        Project revEngEnginesProject2 = ProjectXMLPersistenceUtil.loadAnyFolder(auxTempRootFolder, null);
        List<TreeNode> revEngEnginesPackages2 = Project_Prj_Mdl_M1_Utils.getRevEngEnginesPackages(revEngEnginesProject2);
        if (revEngEnginesPackages2.size() != 1) {
            logger.error("Invalid installer. One and only one Rev Eng Engines package expected. Found:" + revEngEnginesPackages2.size());
            return false;
        }
        boolean ok = validatePackagesDoNotExistAlready(applicationSettings, revEngEnginesPackages2);
        if (!ok) {
            return false;
        }
        for (Iterator<TreeNode> iter = revEngEnginesPackages2.iterator(); iter.hasNext(); ) {
            TreeNode revEngEnginesPackageNode = iter.next();
            installRevEngEngine(applicationSettings, auxTempRootFolder, revEngEnginesPackageNode);
        }
        return true;
    }

    private static boolean installRevEngEngine(ApplicationSettings applicationSettings, String auxTempRootFolder, TreeNode revEngEnginePackageNode2) {
        String revEngEnginesPackageKey = RevEngEnginesPackage_App_PIn_M1_EntityHelper.getKey(revEngEnginePackageNode2);
        deployNodes(applicationSettings, revEngEnginePackageNode2);
        boolean ok = false;
        try {
            deployLibraries(applicationSettings, auxTempRootFolder, revEngEnginesPackageKey);
            ok = true;
        } catch (Exception e) {
            logger.error("Error deploying files", e);
        }
        return ok;
    }

    private static void deployNodes(ApplicationSettings applicationSettings, TreeNode revEngEnginePackageNode2) {
        TreeNode revEngEnginesRootNode = Project_App_Mdl_M1_Utils.getRevEngEnginesRootNode(applicationSettings);
        TreeNode newRevEngEnginesPackageNode = revEngEnginesRootNode.addChild(revEngEnginePackageNode2.getEntityTypeKey());
        for (Iterator<String> iterator = revEngEnginePackageNode2.getAttributeKeys().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String value = revEngEnginePackageNode2.getAttribute(key);
            newRevEngEnginesPackageNode.setAttribute(key, value);
        }
        List children = revEngEnginePackageNode2.getChildren();
        for (Iterator iter2 = children.iterator(); iter2.hasNext(); ) {
            TreeNode revEngEngineNode = (TreeNode) iter2.next();
            TreeNode newRevEngEngineNode = newRevEngEnginesPackageNode.addChild(revEngEngineNode.getEntityTypeKey());
            for (Iterator<String> iterator = revEngEngineNode.getAttributeKeys().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String value = revEngEngineNode.getAttribute(key);
                newRevEngEngineNode.setAttribute(key, value);
            }
        }
        logger.debug("Imported nodes:" + newRevEngEnginesPackageNode);
    }

    private static boolean deployLibraries(ApplicationSettings applicationSettings, String auxTempRootFolder, String revEngEnginesPackageKey) throws IOException {
        String from = auxTempRootFolder + "/" + ar.com.coonocer.CodingJoy.utils.FileUtils.LIBRARIES_FOLDER;
        if (!new File(from).exists()) {
            logger.info("Source folder for libraries does not exist in installer:" + from);
            return true;
        }
        String destFolder = ar.com.coonocer.CodingJoy.utils.FileUtils.getCodingJoyWebAppPath() + "/WEB-INF/lib";
        String to = ar.com.coonocer.CodingJoy.utils.FileUtils.getAbsolutePath(destFolder);
        if (!new File(to).exists()) {
            logger.error("Web Application folder does not exist:" + to);
            return false;
        }
        File[] files = new File(from).listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                File fileTo = new File(to + "/REVENGLIB_" + revEngEnginesPackageKey + "_" + file.getName());
                FileUtils.copyFile(file, fileTo);
                logger.debug("Copied file " + file + " to " + to);
            }
        }
        return true;
    }

    public static void removeTempFolder(String tempRootFolder) throws Exception {
        FileUtils.deleteDirectory(tempRootFolder);
    }
}
