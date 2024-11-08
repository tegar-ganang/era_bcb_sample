package es.uclm.inf_cr.alarcos.desglosa_web.persistence;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.springframework.web.context.ContextLoader;

public class FileUtil {

    private static final String UPLOAD_FOLDER = "upload";

    public static String uploadFile(String fileName, File upload) throws IOException {
        String fullFileName = ContextLoader.getCurrentWebApplicationContext().getServletContext().getRealPath(UPLOAD_FOLDER);
        File theFile = new File(fullFileName + "\\" + fileName);
        FileUtils.copyFile(upload, theFile);
        return UPLOAD_FOLDER + "/" + fileName;
    }
}
