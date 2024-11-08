package com.webstersmalley.picweb.offline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import com.webstersmalley.picweb.offline.config.ConfigurationModel;
import com.webstersmalley.picweb.utils.FolderContents;
import com.webstersmalley.picweb.utils.IOUtils;
import com.webstersmalley.picweb.utils.PictureFilenameFilter;
import com.webstersmalley.picweb.utils.StringMapper;

/**
 * @author Matthew Smalley
 */
public class WebsiteGenerator {

    private static final String TEMPLATE_FILENAME = "/template.xml";

    /** Logger for the class. */
    Logger log = Logger.getLogger(WebsiteGenerator.class);

    private ConfigurationModel model;

    private File[] pictures;

    private TemplateModel template;

    private StringMapper mapper;

    public WebsiteGenerator(ConfigurationModel model) throws ParserConfigurationException, SAXException, IOException {
        this.model = model;
        this.template = new TemplateModel(TEMPLATE_FILENAME);
        this.mapper = new StringMapper();
        mapper.addMapping("title", model.getTitle());
        mapper.addMapping("cssfilename", "stylesheet.css");
    }

    public void go() throws IOException {
        populateList();
        FolderContents folder = new FolderContents(model.getRootFolder());
        copyArtifacts();
        HomepageGenerator hg = new HomepageGenerator(model, mapper, template, folder);
        hg.generate();
        generateThumbs(folder);
    }

    private void generateThumbs(FolderContents folder) throws FileNotFoundException {
        ThumbPageGenerator tpg = new ThumbPageGenerator(model, mapper, template, folder);
        tpg.generate();
        ShowPicturePageGenerator sppg = new ShowPicturePageGenerator(model, mapper, template, folder);
        sppg.generate();
        Iterator it = folder.getChildren().iterator();
        while (it.hasNext()) {
            FolderContents child = (FolderContents) it.next();
            generateThumbs(child);
        }
        List pictures = folder.getPictures();
        ImageProcessor processor = new ImageProcessor(pictures, model, folder.getRelativePath().replace("'", ""));
        processor.execute();
    }

    private void populateList() {
        File root = new File(model.getRootFolder());
        pictures = root.listFiles(new PictureFilenameFilter());
    }

    private void copyArtifact(String name) throws IOException {
        IOUtils.copyFromClassPath(name, model.getOutputFolder() + name);
    }

    private void copyArtifacts() throws IOException {
        copyArtifact("/stylesheet.css");
        copyArtifact("/images/blank.bmp");
        copyArtifact("/images/folder.bmp");
        copyArtifact("/images/minus.bmp");
        copyArtifact("/images/plus.bmp");
    }
}
