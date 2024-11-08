package jmemento.web.controller.images;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import jmemento.api.domain.photo.IPhoto;
import jmemento.api.service.photo.IPhotoService;
import jmemento.api.service.storage.IStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Rusty Wright
 */
@Controller
@RequestMapping("/image")
public final class ImageController {

    private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IPhotoService photoService;

    @Autowired
    private IStorageService storageService;

    @Autowired
    private ImageCommandValidator validator;

    private static final String errorView = "error";

    public void setup(final IPhotoService _photoService, final IStorageService _storageService, final ImageCommandValidator _validator) {
        if (_photoService != null) photoService = _photoService;
        if (_storageService != null) storageService = _storageService;
        if (_validator != null) validator = _validator;
    }

    @RequestMapping(method = RequestMethod.GET)
    protected String handle(@ModelAttribute(value = "image") final ImageCommand image, final HttpServletResponse response) throws IOException {
        if (!validator.validate(image)) {
            log.error("bind errors");
            return (errorView);
        }
        final String photoId = image.getPhotoId();
        final String size = image.getSize();
        log.debug("imageId: {}, size: {}", photoId, size);
        final IPhoto photo = photoService.getPhotoById(photoId);
        if (photo == null) return (errorView);
        final File fileIn = new File(storageService.getPhotoPath(photo, size));
        if (!fileIn.isFile()) {
            log.error(String.format("fileIn: {} (isn't a file)", fileIn));
            return (errorView);
        }
        sendImage(fileIn, response);
        return (null);
    }

    private void sendImage(final File file, final HttpServletResponse response) throws IOException {
        log.debug("file: {}", file);
        response.setContentLength((int) file.length());
        response.setContentType("image/jpeg");
        response.setHeader("Content-Disposition", "inline;filename=" + file.getName());
        final OutputStream httpOut = response.getOutputStream();
        final int len = 8 * 1024;
        final InputStream fileIstream = new FileInputStream(file);
        final InputStream buffIstream = new BufferedInputStream(fileIstream, len);
        final byte[] buf = new byte[len];
        int bytesRead;
        while ((bytesRead = buffIstream.read(buf, 0, len)) != -1) httpOut.write(buf, 0, bytesRead);
        httpOut.flush();
        httpOut.close();
        fileIstream.close();
        buffIstream.close();
    }
}
