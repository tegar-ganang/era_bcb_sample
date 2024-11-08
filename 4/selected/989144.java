package cn.openlab.game.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import cn.openlab.game.dao.HomeBuildingDao;
import cn.openlab.game.dao.HomeMapDao;
import cn.openlab.game.entity.HomeBuilding;
import cn.openlab.game.entity.HomeMap;

public class HomeMapDetailEditorAction {

    private HomeMap homeMap;

    private HomeMapDao homeMapDao;

    private HomeBuildingDao homeBuildingDao;

    private File imageFile;

    private HomeBuilding homeBuilding;

    public HomeBuilding getHomeBuilding() {
        return homeBuilding;
    }

    public void setHomeBuilding(HomeBuilding homeBuilding) {
        this.homeBuilding = homeBuilding;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public void setHomeBuildingDao(HomeBuildingDao homeBuildingDao) {
        this.homeBuildingDao = homeBuildingDao;
    }

    public void setHomeMapDao(HomeMapDao homeMapDao) {
        this.homeMapDao = homeMapDao;
    }

    public HomeMap getHomeMap() {
        return homeMap;
    }

    public void setHomeMap(HomeMap homeMap) {
        this.homeMap = homeMap;
    }

    public String execute() {
        Integer id = homeMap.getId();
        if (id != null) {
            homeMap = homeMapDao.getHomeMapById(id);
        }
        return "success";
    }

    public String insertBuilding() {
        homeMap = homeMapDao.getHomeMapById(homeMap.getId());
        homeBuilding.setHomeMap(homeMap);
        Integer id = homeBuildingDao.saveHomeBuilding(homeBuilding);
        String dir = "E:\\ganymede_workspace\\training01\\web\\user_buildings\\";
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
        return execute();
    }

    public String delete() {
        Integer id = homeBuilding.getId();
        if (id != null) {
            homeBuildingDao.deleteHomeBuilding(id);
        }
        return execute();
    }
}
