package cn.openlab.game.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

public class ImageUploadAction {

    private String userDefName;

    public String getUserDefName() {
        return userDefName;
    }

    private File imgFile;

    public String execute() {
        return "success";
    }

    public void setUserDefName(String userDefName) {
        this.userDefName = userDefName;
    }

    public void setImgFile(File imgFile) {
        this.imgFile = imgFile;
    }

    public String upload() {
        System.out.println(imgFile);
        String destDir = "E:\\ganymede_workspace\\training01\\web\\user_imgs\\map_bg.jpg";
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(new File(destDir));
            IOUtils.copy(new FileInputStream(imgFile), fos);
            IOUtils.closeQuietly(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "show";
    }
}
