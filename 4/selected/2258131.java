package ar.com.coonocer.CodingJoy.menu.tools.plugins.installer.bl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import ar.com.coonocer.CodingJoy.model.ApplicationSettings;
import ar.com.coonocer.CodingJoy.model.Project;
import ar.com.coonocer.CodingJoy.model.TreeNode;
import ar.com.coonocer.CodingJoy.model.helpers.node.GeneratorsPackage_App_PIn_M1_EntityHelper;
import ar.com.coonocer.CodingJoy.model.helpers.node.Plugins_App_PIn_M1_EntityHelper;
import ar.com.coonocer.CodingJoy.model.helpers.utils.Project_App_Mdl_M1_Utils;
import ar.com.coonocer.CodingJoy.model.helpers.utils.Project_Prj_Mdl_M1_Utils;
import ar.com.coonocer.CodingJoy.model.serialization.application.ApplicationXMLPersistenceUtil;
import ar.com.coonocer.CodingJoy.model.serialization.project.ProjectXMLPersistenceUtil;
import ar.com.coonocer.CodingJoy.session.CodingJoySessionUtil;

public class GeneratorInstallerBL {

    private static final int BUFFER_SIZE = 1024 * 10;

    private static Logger logger = Logger.getLogger(GeneratorInstallerBL.class);

    public static boolean installGeerator(HttpServletRequest request) throws Exception {
        String auxTempRootFolder = ar.com.coonocer.CodingJoy.utils.FileUtils.getTempFolder();
        if (!new File(auxTempRootFolder).exists()) {
            if (!new File(auxTempRootFolder).mkdirs()) {
                return false;
            }
        }
        GeneratorInstallerBL.saveFilesInRequestToFolder(request, auxTempRootFolder);
        ApplicationSettings applicationSettings = CodingJoySessionUtil.getApplicationSettings(request.getSession());
        boolean ok = GeneratorInstallerBL.installFile(applicationSettings, auxTempRootFolder);
        if (ok) {
            ApplicationXMLPersistenceUtil.save(applicationSettings);
        }
        GeneratorInstallerBL.removeTempFolder(auxTempRootFolder);
        new File(auxTempRootFolder).delete();
        return ok;
    }

    private static boolean validatePackagesDoNotExistAlready(ApplicationSettings applicationSettings, List<TreeNode> generatorsPackages2) {
        for (Iterator<TreeNode> iter = generatorsPackages2.iterator(); iter.hasNext(); ) {
            TreeNode generatorsPackageNode = iter.next();
            String generatorsPackageKey = GeneratorsPackage_App_PIn_M1_EntityHelper.getKey(generatorsPackageNode);
            TreeNode aux2 = Project_App_Mdl_M1_Utils.getGeneratorsPackageByKey(applicationSettings, generatorsPackageKey);
            if (aux2 != null) {
                logger.error("Generators package with key=" + generatorsPackageKey + " already exists in current project.");
                return false;
            }
        }
        return true;
    }

    public static boolean installFile(ApplicationSettings applicationSettings, String auxTempRootFolder) throws Exception {
        Project generatorsProject2 = ProjectXMLPersistenceUtil.loadAnyFolder(auxTempRootFolder, null);
        List<TreeNode> generatorsPackages2 = Project_Prj_Mdl_M1_Utils.getGeneratorsPackages(generatorsProject2);
        if (generatorsPackages2.size() != 1) {
            logger.error("Invalid installer. One and only one generators package expected. Found:" + generatorsPackages2.size());
            return false;
        }
        boolean ok = validatePackagesDoNotExistAlready(applicationSettings, generatorsPackages2);
        if (!ok) {
            return false;
        }
        for (Iterator<TreeNode> iter = generatorsPackages2.iterator(); iter.hasNext(); ) {
            TreeNode generatorPackageNode = iter.next();
            installGenerator(applicationSettings, auxTempRootFolder, generatorPackageNode);
        }
        return true;
    }

    private static boolean installGenerator(ApplicationSettings applicationSettings, String auxTempRootFolder, TreeNode generatorPackageNode2) {
        String generatorsPackageKey = GeneratorsPackage_App_PIn_M1_EntityHelper.getKey(generatorPackageNode2);
        deployNodes(applicationSettings, generatorPackageNode2);
        boolean ok = false;
        try {
            deployFiles(applicationSettings, auxTempRootFolder, generatorsPackageKey);
            deployLibraries(applicationSettings, auxTempRootFolder, generatorsPackageKey);
            ok = true;
        } catch (Exception e) {
            logger.error("Error deploying files", e);
        }
        return ok;
    }

    private static void deployNodes(ApplicationSettings applicationSettings, TreeNode generatorPackageNode2) {
        TreeNode generatorsRootNode = Project_App_Mdl_M1_Utils.getGeneratorsRootNode(applicationSettings);
        TreeNode newGeneratorsPackageNode = generatorsRootNode.addChild(generatorPackageNode2.getEntityTypeKey());
        for (Iterator<String> iterator = generatorPackageNode2.getAttributeKeys().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String value = generatorPackageNode2.getAttribute(key);
            newGeneratorsPackageNode.setAttribute(key, value);
        }
        List children = generatorPackageNode2.getChildren();
        for (Iterator iter2 = children.iterator(); iter2.hasNext(); ) {
            TreeNode generatorNode = (TreeNode) iter2.next();
            TreeNode newGeneratorNode = newGeneratorsPackageNode.addChild(generatorNode.getEntityTypeKey());
            for (Iterator<String> iterator = generatorNode.getAttributeKeys().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String value = generatorNode.getAttribute(key);
                newGeneratorNode.setAttribute(key, value);
            }
        }
        logger.debug("Imported nodes:" + newGeneratorsPackageNode);
    }

    private static boolean deployFiles(ApplicationSettings applicationSettings, String auxTempRootFolder, String generatorsPackageKey) throws IOException {
        String from = auxTempRootFolder + "/" + ar.com.coonocer.CodingJoy.utils.FileUtils.TEMPLATES_FOLDER;
        if (!new File(from).exists()) {
            logger.error("Source folder for templates does not exist in installer:" + from);
            return true;
        }
        String appServerTemplatesRootPath = Plugins_App_PIn_M1_EntityHelper.getAppServerTemplatesRootFolder(applicationSettings.getPlugins());
        String to = ar.com.coonocer.CodingJoy.utils.FileUtils.getAbsolutePath(appServerTemplatesRootPath);
        if (!new File(to).exists()) {
            logger.info("Templates Web Application folder does not exist:" + to);
            return false;
        }
        to = to + "/" + ar.com.coonocer.CodingJoy.utils.FileUtils.TEMPLATES_FOLDER + "/" + generatorsPackageKey;
        File fileTo = new File(to);
        if (!fileTo.exists()) {
            if (!fileTo.mkdirs()) {
                logger.error("Destination folder could not be created:" + to);
                return false;
            }
        }
        File[] files = new File(from).listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                FileUtils.copyFileToDirectory(file, fileTo);
                logger.debug("Copied file " + file + " to " + to);
            }
        }
        return true;
    }

    private static boolean deployLibraries(ApplicationSettings applicationSettings, String auxTempRootFolder, String generatorsPackageKey) throws IOException {
        String from = auxTempRootFolder + "/" + ar.com.coonocer.CodingJoy.utils.FileUtils.LIBRARIES_FOLDER;
        if (!new File(from).exists()) {
            logger.info("Source folder for libraries does not exist in installer:" + from);
            return true;
        }
        String appServerTemplatesRootPath = Plugins_App_PIn_M1_EntityHelper.getAppServerTemplatesRootFolder(applicationSettings.getPlugins());
        String to = ar.com.coonocer.CodingJoy.utils.FileUtils.getAbsolutePath(appServerTemplatesRootPath) + "/WEB-INF/lib";
        if (!new File(to).exists()) {
            logger.error("Templates Web Application folder does not exist:" + to);
            return false;
        }
        File[] files = new File(from).listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                File fileTo = new File(to + "/GENLIB_" + generatorsPackageKey + "_" + file.getName());
                FileUtils.copyFile(file, fileTo);
                logger.debug("Copied file " + file + " to " + to);
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void saveFilesInRequestToFolder(HttpServletRequest request, String auxTempRootFolder) throws Exception {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = upload.parseRequest(request);
        for (Iterator<FileItem> iter = items.iterator(); iter.hasNext(); ) {
            FileItem fileItem = iter.next();
            String auxFileName = auxTempRootFolder + "/uploaded.zip";
            File uploadedFile = new File(auxFileName);
            fileItem.write(uploadedFile);
            processUploadedFile(auxTempRootFolder, auxFileName);
        }
        return;
    }

    public static void removeTempFolder(String tempRootFolder) throws Exception {
        FileUtils.deleteDirectory(tempRootFolder);
    }

    private static void processUploadedFile(String tempRootFolder, String fileName) throws Exception {
        ZipFile zipFile = new ZipFile(fileName);
        for (Enumeration enumeration = zipFile.entries(); enumeration.hasMoreElements(); ) {
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (!zipEntry.isDirectory()) {
                InputStream zis = new BufferedInputStream(zipFile.getInputStream(zipEntry), BUFFER_SIZE);
                String newFileName = tempRootFolder + "/" + zipEntry.getName();
                String aux = newFileName;
                int pos1 = aux.lastIndexOf("/");
                int pos2 = aux.lastIndexOf("\\");
                int pos = pos1 > pos2 ? pos1 : pos2;
                String newFileFolder = aux.substring(0, pos);
                new File(newFileFolder).mkdirs();
                int count;
                byte data[] = new byte[BUFFER_SIZE];
                FileOutputStream fos = new FileOutputStream(newFileName);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
        zipFile.close();
    }
}
