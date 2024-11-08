package uk.ac.bolton.archimate.templates.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipOutputStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.jdom.Document;
import org.jdom.Element;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.editor.utils.FileUtils;
import uk.ac.bolton.archimate.editor.utils.StringUtils;
import uk.ac.bolton.archimate.editor.utils.ZipUtils;
import uk.ac.bolton.jdom.JDOMUtils;

/**
 * Abstract Template
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractTemplate implements ITemplate, ITemplateXMLTags {

    private String fID;

    private String fName;

    private String fDescription;

    private File fFile;

    private boolean fManifestLoaded;

    private String fKeyThumbnailPath;

    private Image fKeyThumbnailImage;

    private Image[] fThumbnails;

    public AbstractTemplate() {
    }

    /**
     * @param id If this is null a new id will be generated
     */
    public AbstractTemplate(String id) {
        if (id == null) {
            id = UUID.randomUUID().toString().split("-")[0];
        }
        fID = id;
    }

    @Override
    public String getName() {
        if (!fManifestLoaded) {
            loadManifest();
        }
        return fName;
    }

    @Override
    public void setName(String name) {
        fName = StringUtils.safeString(name);
    }

    @Override
    public String getDescription() {
        if (!fManifestLoaded) {
            loadManifest();
        }
        return fDescription;
    }

    @Override
    public void setDescription(String description) {
        fDescription = StringUtils.safeString(description);
    }

    @Override
    public Image getKeyThumbnail() {
        if (!fManifestLoaded) {
            loadManifest();
        }
        if (fKeyThumbnailImage == null && fKeyThumbnailPath != null) {
            fKeyThumbnailImage = loadImage(fKeyThumbnailPath);
        }
        if (fKeyThumbnailImage == null) {
            return IArchimateImages.ImageFactory.getImage(IArchimateImages.DEFAULT_MODEL_THUMB);
        } else {
            return fKeyThumbnailImage;
        }
    }

    @Override
    public Image[] getThumbnails() {
        if (fThumbnails == null) {
            List<Image> list = new ArrayList<Image>();
            int i = 1;
            Image image;
            do {
                image = loadImage(TemplateManager.ZIP_ENTRY_THUMBNAILS + i++ + ".png");
                if (image != null) {
                    list.add(image);
                }
            } while (image != null);
            fThumbnails = list.toArray(new Image[list.size()]);
        }
        return fThumbnails;
    }

    @Override
    public File getFile() {
        return fFile;
    }

    @Override
    public void setFile(File file) {
        fFile = file;
    }

    @Override
    public String getID() {
        return fID;
    }

    @Override
    public void setID(String id) {
        fID = id;
    }

    @Override
    public void save() throws IOException {
        if (fFile == null || !fFile.exists()) {
            return;
        }
        Document doc = new Document();
        Element root = new Element(ITemplateXMLTags.XML_TEMPLATE_ELEMENT_MANIFEST);
        doc.setRootElement(root);
        Element elementName = new Element(ITemplateXMLTags.XML_TEMPLATE_ELEMENT_NAME);
        elementName.setText(getName());
        root.addContent(elementName);
        Element elementDescription = new Element(ITemplateXMLTags.XML_TEMPLATE_ELEMENT_DESCRIPTION);
        elementDescription.setText(getDescription());
        root.addContent(elementDescription);
        if (fKeyThumbnailPath != null) {
            Element elementKeyThumb = new Element(ITemplateXMLTags.XML_TEMPLATE_ELEMENT_KEY_THUMBNAIL);
            elementKeyThumb.setText(fKeyThumbnailPath);
            root.addContent(elementKeyThumb);
        }
        String manifest = JDOMUtils.write2XMLString(doc);
        String model = ZipUtils.extractZipEntry(fFile, TemplateManager.ZIP_ENTRY_MODEL);
        File tmpFile = File.createTempFile("architemplate", null);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));
        ZipOutputStream zOut = new ZipOutputStream(out);
        ZipUtils.addStringToZip(manifest, TemplateManager.ZIP_ENTRY_MANIFEST, zOut);
        ZipUtils.addStringToZip(model, TemplateManager.ZIP_ENTRY_MODEL, zOut);
        Image[] images = getThumbnails();
        int i = 1;
        for (Image image : images) {
            ZipUtils.addImageToZip(image, TemplateManager.ZIP_ENTRY_THUMBNAILS + i++ + ".png", zOut, SWT.IMAGE_PNG, null);
        }
        zOut.flush();
        zOut.close();
        fFile.delete();
        FileUtils.copyFile(tmpFile, fFile, false);
        tmpFile.delete();
    }

    private void loadManifest() {
        fManifestLoaded = true;
        fName = "";
        fDescription = "";
        if (fFile != null && fFile.exists()) {
            try {
                String manifest = ZipUtils.extractZipEntry(fFile, TemplateManager.ZIP_ENTRY_MANIFEST);
                if (manifest != null) {
                    Document doc = JDOMUtils.readXMLString(manifest);
                    Element rootElement = doc.getRootElement();
                    Element nameElement = rootElement.getChild(XML_TEMPLATE_ELEMENT_NAME);
                    if (nameElement != null) {
                        fName = nameElement.getText();
                    }
                    Element descriptionElement = rootElement.getChild(XML_TEMPLATE_ELEMENT_DESCRIPTION);
                    if (nameElement != null) {
                        fDescription = descriptionElement.getText();
                    }
                    Element keyThumbnailElement = rootElement.getChild(XML_TEMPLATE_ELEMENT_KEY_THUMBNAIL);
                    if (keyThumbnailElement != null) {
                        fKeyThumbnailPath = keyThumbnailElement.getText();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Image loadImage(String imgName) {
        Image image = null;
        if (fFile != null && fFile.exists() && imgName != null) {
            InputStream stream = null;
            try {
                stream = ZipUtils.getZipEntryStream(fFile, imgName);
                if (stream != null) {
                    image = new Image(null, stream);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return image;
    }

    @Override
    public void dispose() {
        if (fKeyThumbnailImage != null && !fKeyThumbnailImage.isDisposed()) {
            fKeyThumbnailImage.dispose();
            fKeyThumbnailImage = null;
        }
        if (fThumbnails != null) {
            for (Image image : fThumbnails) {
                if (image != null && !image.isDisposed()) {
                    image.dispose();
                }
            }
            fThumbnails = null;
        }
    }
}
