package IO;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import org.apache.commons.io.FileUtils;

public class CopyFiles {

    public static String SRC_DIR = "\\\\exampledev01\\e$\\SITES\\COMMON\\params\\JOURNALS";

    public static String DSTN_DIR = "\\\\examplestage1\\temp1\\SITES\\COMMON\\params\\JOURNALS";

    public static String RELATIVE_FILE_PATH = "\\peerreview\\presentation\\guidelines.xml";

    public static void main(String[] args) throws IOException, IllegalArgumentException, IllegalAccessException {
        File srcDir = new File(SRC_DIR);
        String[] list = srcDir.list();
        int counter = 1;
        System.out.println("Starting..........");
        printMemberVariables();
        for (int i = 0; i < list.length; i++) {
            String fileOrDir = list[i];
            File fileOrDirSrcObj = new File(SRC_DIR, fileOrDir + RELATIVE_FILE_PATH);
            File fileOrDirDstnObj = new File(DSTN_DIR, fileOrDir + RELATIVE_FILE_PATH);
            if (fileOrDirSrcObj.exists()) {
                System.out.println((counter++) + fileOrDirDstnObj.getAbsolutePath());
                FileUtils.copyFile(fileOrDirSrcObj, fileOrDirDstnObj);
            }
        }
        System.out.println("Done..........");
    }

    private static void printMemberVariables() throws IllegalAccessException {
        Field[] fields = CopyFiles.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            System.out.println(fields[i].getName() + " : " + fields[i].get(CopyFiles.class));
        }
        System.out.println();
    }
}
