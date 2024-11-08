package sheep.utils.fileio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import sheep.controller.Workspace;

public class FileUtils {

    private FileUtils() {
    }

    private static String fileExtension = "shp";

    public static String getFileExtension() {
        return fileExtension;
    }

    public static void copyFile(File oldFile, File newFile) throws Exception {
        newFile.getParentFile().mkdirs();
        newFile.createNewFile();
        FileChannel srcChannel = new FileInputStream(oldFile).getChannel();
        FileChannel dstChannel = new FileOutputStream(newFile).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    public static void saveWks(File saveDir) throws Exception {
        saveDir.mkdirs();
        File wksData = new File(saveDir.getAbsolutePath() + "/wks-data." + fileExtension);
        wksData.createNewFile();
        OutputStream out = new FileOutputStream(wksData);
        out.close();
    }

    public static Workspace readWks(File readDir) throws Exception {
        InputStream in = new FileInputStream(readDir.getAbsolutePath() + "/wks-data." + fileExtension);
        in.close();
        return null;
    }
}
