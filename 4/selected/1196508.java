package de.herberlin.webapp.image.action;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;
import de.herberlin.webapp.core.AbstractAction;
import de.herberlin.webapp.core.AppException;
import de.herberlin.webapp.db.Service;
import de.herberlin.webapp.image.GalleryItem;
import de.herberlin.webapp.image.ItemType;

/**
 * @author herberlin
 *
 */
public class UploadAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private FileBean newImage = null;

    private String pageId = null;

    public UploadAction() {
    }

    @Validate(required = true)
    public void setNewImage(FileBean newImage) {
        this.newImage = newImage;
    }

    public Resolution upload() {
        final GalleryItem item = new GalleryItem();
        item.setName(newImage.getFileName());
        item.setType(ItemType.guessFromContent(newImage.getContentType()));
        item.setPageId(pageId);
        try {
            item.setData(read(newImage.getInputStream()));
        } catch (IOException e) {
            logger.error(e, e);
            throw new AppException(e);
        }
        logger.debug("GalleryItem=" + item);
        Service.persist(item);
        getContext().getRequest().setAttribute("success", new Boolean(true));
        getContext().getRequest().setAttribute("pageId", pageId);
        return new ForwardResolution("/admin/upload.jsp");
    }

    private byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read = -1;
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        return out.toByteArray();
    }

    @Validate(required = true)
    public void setPageId(String pageId) {
        this.pageId = pageId;
    }
}
