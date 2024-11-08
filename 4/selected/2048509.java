package kohary.datamodel.export;

import java.io.*;
import java.util.zip.*;
import org.apache.commons.io.IOUtils;

public class Ziper {

    private File rootDirectory;

    public Ziper(File rootDirectory) throws Exception {
        this.rootDirectory = rootDirectory;
        try {
            String outFilename = rootDirectory + ".zip";
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            File zipFolder = rootDirectory;
            int len = zipFolder.getAbsolutePath().lastIndexOf(File.separator);
            String baseName = zipFolder.getAbsolutePath().substring(0, len + 1);
            addFolderToZip(zipFolder, out, baseName);
            out.close();
            removeCommonFolder();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeCommonFolder() {
        rootDirectory.delete();
    }

    private static void addFolderToZip(File folder, ZipOutputStream zip, String baseName) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, zip, baseName);
            } else {
                String name = file.getAbsolutePath().substring(baseName.length());
                ZipEntry zipEntry = new ZipEntry(name);
                zip.putNextEntry(zipEntry);
                IOUtils.copy(new FileInputStream(file), zip);
                zip.closeEntry();
            }
        }
    }
}
