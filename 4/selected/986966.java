package net.sf.jimex.jira;

import com.atlassian.core.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.ofbiz.core.entity.GenericValue;
import org.apache.commons.lang.StringUtils;

/**
 * andrew 20.03.2006 0:23:45
 */
public class JimexImportUtils {

    private static final String JIMEX = "Jimex";

    public static void saveImportedFile(GenericValue project, File file, String filename) throws IOException {
        File attachmentDir = getAttachmentDir(project);
        File attachmentFile = new File(attachmentDir, filename);
        if (attachmentFile.exists()) {
            attachmentFile.delete();
        }
        FileUtils.copyFile(file, attachmentFile);
    }

    public static List getImportedProjects(GenericValue project) {
        File attachmentDir = getAttachmentDir(project);
        String[] fileNames = attachmentDir.list();
        List fileNamesList = Arrays.asList(fileNames);
        return fileNamesList;
    }

    public static File getAttachmentDir(GenericValue project) {
        File attachmentDirectory = new File(getDefaultAttachmentDir());
        File projectDirectory = new File(attachmentDirectory, project.getString("key"));
        if (!projectDirectory.exists()) {
            projectDirectory.mkdirs();
        }
        File issueDirectory = new File(projectDirectory, JIMEX);
        if (!issueDirectory.exists()) {
            issueDirectory.mkdirs();
        }
        return issueDirectory;
    }

    public static String getDefaultAttachmentDir() {
        String attachmentDir = com.atlassian.jira.ManagerFactory.getApplicationProperties().getString("jira.path.attachments");
        return StringUtils.isEmpty(attachmentDir) ? "." : attachmentDir;
    }
}
