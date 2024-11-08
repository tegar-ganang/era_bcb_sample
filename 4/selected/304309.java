package cn.openlab.game.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import cn.openlab.game.dao.HomeMapDao;
import cn.openlab.game.entity.HomeMap;

public class HomeMapEditorAction {

    private String description;

    private File imageFile;

    private HomeMapDao homeMapDao;

    private List<HomeMap> maps;

    public List<HomeMap> getMaps() {
        return maps;
    }

    public void setHomeMapDao(HomeMapDao homeMapDao) {
        this.homeMapDao = homeMapDao;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public String execute() {
        String dir = "E:\\ganymede_workspace\\training01\\web\\user_imgs\\";
        HomeMap map = new HomeMap();
        map.setDescription(description);
        Integer id = homeMapDao.saveHomeMap(map);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(dir + id);
            IOUtils.copy(new FileInputStream(imageFile), fos);
            IOUtils.closeQuietly(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list();
    }

    public String list() {
        maps = homeMapDao.getAllMap();
        return "list";
    }

    public String insert() {
        return "success";
    }
}
