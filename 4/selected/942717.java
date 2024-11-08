package com.kenstevens.stratdom.ui.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.kenstevens.stratdom.main.Constants;
import com.kenstevens.stratdom.model.Unit;
import com.kenstevens.stratdom.model.UnitType;
import com.kenstevens.stratdom.site.SiteResponse;
import com.kenstevens.stratdom.site.StratSite;

@Component
public class ImageCache {

    @Autowired
    private StratSite stratSite;

    Image getImage(String filename) throws Exception, FileNotFoundException {
        File imageFile = getImageFromDisk(filename);
        if (!imageFile.canRead()) {
            downloadImage(filename, imageFile);
        }
        return fileToImage(imageFile);
    }

    Image fileToImage(File imageFile) throws FileNotFoundException {
        Display display = Display.getDefault();
        return new Image(display, new FileInputStream(imageFile));
    }

    File getImageFromDisk(String filename) {
        String filepath = Constants.NORMAL_IMAGE_DIR + "/" + filename;
        File imageFile = new File(filepath);
        return imageFile;
    }

    void downloadImage(String filename, File imageFile) throws Exception {
        String URL = Constants.IMAGE_URL + "/" + filename;
        SiteResponse response = stratSite.getResponse(URL);
        InputStream inputStream = response.getInputStream();
        OutputStream outputStream = new FileOutputStream(imageFile);
        IOUtils.copy(inputStream, outputStream);
    }

    String getBadWaterImageName(Unit.Type type) {
        String waterImageName = null;
        UnitType unitType = UnitType.getUnitType(type);
        if (unitType.isAir()) {
            waterImageName = "bad" + type + "water.jpg";
        } else if (unitType.isNavy()) {
            waterImageName = "bad" + type + ".jpg";
        }
        return waterImageName;
    }

    String getBadLandImageName(Unit.Type type) {
        String landImageName = null;
        UnitType unitType = UnitType.getUnitType(type);
        if (unitType.isAir()) {
            landImageName = "bad" + type + "land.jpg";
        } else if (unitType.isLand()) {
            landImageName = "bad" + type + ".jpg";
        }
        return landImageName;
    }
}
