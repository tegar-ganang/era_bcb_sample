package hrc.tool;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileDealer {

    private InputStream file;

    private String filePath;

    public FileDealer(InputStream file) {
        this.file = file;
    }

    public FileDealer(String filePath) {
        this.filePath = filePath;
    }

    public boolean saveStringContent(String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.filePath)));
            writer.write(content, 0, content.length());
            writer.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean copyTo(String targetFilePath) {
        try {
            FileInputStream srcFile = new FileInputStream(filePath);
            FileOutputStream target = new FileOutputStream(targetFilePath);
            byte[] buff = new byte[1024];
            int readed = -1;
            while ((readed = srcFile.read(buff)) > 0) target.write(buff, 0, readed);
            srcFile.close();
            target.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveTo(String filePath) {
        try {
            OutputStream outputStream = new FileOutputStream(filePath);
            int bytes = 0;
            byte[] buffer = new byte[8192];
            while ((bytes = file.read(buffer, 0, 8192)) != -1) {
                outputStream.write(buffer, 0, bytes);
            }
            outputStream.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
