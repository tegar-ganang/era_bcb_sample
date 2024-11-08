package org.designerator.media.thumbs;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.designerator.common.data.MediaFile;
import org.designerator.common.data.ThumbProxy;
import org.designerator.common.data.VideoInfo;
import org.designerator.common.string.StringUtil;
import org.designerator.image.algo.util.ImageConversion;
import org.designerator.media.builder.ThumbBuildManager;
import org.designerator.media.database.ExifDate;
import org.designerator.media.image.util.IO;
import org.designerator.media.util.ImageHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.ijg.jpeg.IJGLib;
import ffmpeg.FFmpeg;

public class ThumbIO {

    private static final int NR_OF_VIDEO_THUMBS = 4;

    public static final int DEFAULT_SIZE_X = 160;

    public static final int DEFAULT_SIZE_Y = 120;

    public static final int DEFAULT_SIZE_VIDEO_X = 650;

    public static final int DEFAULT_SIZE_VIDEO_Y = 124;

    public static final int DEFAULT_ICONSIZE = 96;

    public static final int MINSize = 20000;

    public static final String JPG = "jpg";

    public static final String LARGEJPG = "jpeg";

    public static final int LARGE_SIZE = 320;

    public static boolean createAWTThumb(Display display, File file, File savePath, MetaThumbData mdata) {
        if (file == null || savePath == null) {
            return false;
        }
        BufferedImage bi = IO.loadBufImage(file.getAbsolutePath());
        if (bi == null) {
            return false;
        }
        Point thumbSize = ImageConversion.fitToThumb2(bi.getWidth(), bi.getHeight(), ThumbIO.DEFAULT_SIZE_X, ThumbIO.DEFAULT_SIZE_Y);
        if (thumbSize != null) {
            if (mdata != null) {
                mdata.width = thumbSize.x;
                mdata.height = thumbSize.y;
            }
            BufferedImage thumb = ImageConversion.createQualityResizedImage(bi, thumbSize.x, thumbSize.y, null);
            try {
                IO.saveAsJPEG(thumb, savePath.getAbsolutePath(), 0.85f);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static ThumbProxy createError(Display display, MediaFile mediaFile) {
        IPath path = mediaFile.getPath();
        String item = null;
        if (path != null) {
            item = path.lastSegment();
        }
        Image v = ImageHelper.createImage(null, "/icons/large/image-x-generic.gif");
        Image error = IO.createErrorImage(display, v, item, 160, 120);
        ThumbProxy tm = new ThumbProxy(mediaFile, error, 160, 120);
        tm.isUnreadable = true;
        return tm;
    }

    public static ThumbProxy createErrorThumb(final MediaFile mediaFile, final Display display, boolean disposeImage) {
        ThumbProxy thumb = new ThumbProxy();
        String name;
        String osString;
        if (mediaFile != null) {
            name = mediaFile.getName();
            osString = mediaFile.getAbsolutePath();
            thumb.setMediaFile(mediaFile);
        } else {
            name = VideoInfo.empty;
            osString = VideoInfo.empty;
        }
        thumb.setMediaFile(mediaFile);
        thumb.setThumbWidth(ThumbProxy.DEFAULTWIDTH);
        thumb.setThumbHeight(ThumbProxy.DEFAULTHEIGHT);
        thumb.isUnreadable = true;
        if (!disposeImage) {
            Image v = ImageHelper.createImage(null, "/icons/large/image-x-generic.gif");
            Image strip2 = IO.createErrorImage(display, v, name, 160, 120);
            thumb.setThumbImages(strip2);
        }
        return thumb;
    }

    public static ThumbProxy createFilmStripThumb(final MediaFile mediaFile, final Display display, File thumbFile, List<Image> images) {
        if (images == null || images.size() < 1 || images.get(0) == null) {
            return createVideoErrorThumb(mediaFile, display, thumbFile, false, true);
        }
        ThumbProxy thumbProxy;
        Image strip = null;
        int size = 4;
        Image image = images.get(0);
        Rectangle srcBounds = image.getBounds();
        int width = srcBounds.width * size + (size * 2) + 2;
        int height = srcBounds.height + 4;
        strip = new Image(display, width, height);
        GC gc = new GC(strip);
        Color black = new Color(display, 0, 0, 0);
        gc.setBackground(black);
        gc.fillRectangle(0, 0, width, height);
        int x = 2;
        int y = 2;
        int last = 0;
        for (int i = 0; i < size; i++) {
            Image image2 = null;
            if (images.size() > i) {
                image2 = images.get(i);
                last = i;
            } else {
                image2 = images.get(last);
            }
            if (image2 != null && !image2.isDisposed()) {
                gc.drawImage(image2, 0, 0, srcBounds.width, srcBounds.height, x, y, srcBounds.width, srcBounds.height);
                x += srcBounds.width + 2;
            }
        }
        for (Image image2 : images) {
            image2.dispose();
        }
        black.dispose();
        gc.dispose();
        thumbProxy = new ThumbProxy();
        thumbProxy.setMediaFile(mediaFile);
        thumbProxy.setThumbWidth(width);
        thumbProxy.setThumbHeight(height);
        thumbProxy.isVideo = true;
        thumbProxy.setThumbImages(strip);
        thumbProxy.thumbPath = thumbFile.getAbsolutePath();
        IO.saveImageSWT(strip.getImageData(), thumbFile.getAbsolutePath(), SWT.IMAGE_JPEG, 85);
        return thumbProxy;
    }

    public static MetaThumbData createImageThumb(Display display, File file, File savePath, MetaThumbData meta, boolean returnThumb) {
        String absolutePath = file.getAbsolutePath();
        if (IO.isJpeg(file.getName()) && meta != null) {
            MetaThumbData m = SanExif.getExifThumbImage(display, absolutePath, true);
            if (m != null && m.thumb != null) {
                meta.exifDate = m.exifDate;
                if (meta.exifDate != null && meta.exifDate.indexOf("0000") != -1) {
                    meta.exifDate = ExifDate.getExifDateFromLastModified(file);
                }
                meta.height = m.height;
                meta.width = m.width;
                meta.thumb = m.thumb;
                meta.imageData = m.imageData;
                meta.paintRect = m.paintRect;
                meta.needsSaving = true;
                meta.thumbPath = savePath.getAbsolutePath();
            } else {
                createSWTThumb(display, file, savePath, meta, returnThumb);
            }
        } else {
            createSWTThumb(display, file, savePath, meta, returnThumb);
        }
        return meta;
    }

    public static MetaThumbData createImageThumb(Display display, IFile imagefile, MetaThumbData meta, boolean highQuality) {
        if (imagefile == null) {
            return meta;
        }
        final IPath location = imagefile.getLocation();
        if (location == null) {
            return meta;
        }
        File file = location.toFile();
        if (!file.exists()) {
            return meta;
        }
        if (getMinimalThumbSize() > file.length() && !(IO.isPsdImage(imagefile.getName()))) {
            setThumbSizeToMeta(meta, file);
            return meta;
        }
        File savePath = ThumbFileUtils.getDefaultThumbPath(imagefile);
        meta.thumbPath = savePath.getAbsolutePath();
        if (savePath == null) {
            setThumbSizeToMeta(meta, file);
            return meta;
        }
        File explorerPath = ThumbFileUtils.getExplorerThumbPath(imagefile);
        if (explorerPath.exists()) {
            try {
                FileUtils.copyFile(explorerPath, savePath);
                if (meta.exifDate == null) {
                    meta.exifDate = ExifDate.getExifDateFromLastModified(file);
                }
                return meta;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (highQuality) {
            Image largeThumb = loadLargeThumbImage(file.getAbsolutePath(), savePath.getAbsolutePath(), display);
            if (largeThumb != null) {
                int width = ThumbIO.DEFAULT_SIZE_X;
                int height = ThumbIO.DEFAULT_SIZE_Y;
                Rectangle bounds = largeThumb.getBounds();
                if (bounds.height > bounds.width) {
                    width = ThumbIO.DEFAULT_SIZE_Y;
                    height = ThumbIO.DEFAULT_SIZE_X;
                }
                Image thumb = ImageConversion.createQualityThumbImage(largeThumb, width, height, SWT.HIGH);
                if (meta != null) {
                    meta.width = thumb.getBounds().width;
                    meta.height = thumb.getBounds().height;
                    MetaThumbData findThumbData = SanExif.findThumbData(file.getAbsolutePath(), false);
                    if (findThumbData != null) {
                        meta.exifDate = findThumbData.exifDate;
                    }
                }
                largeThumb.dispose();
                if (thumb != null) {
                    IO.saveImageSWT(thumb.getImageData(), savePath.getAbsolutePath(), SWT.IMAGE_JPEG, 85);
                    thumb.dispose();
                }
            } else {
                createImageThumb(display, file, savePath, meta, false);
            }
        } else {
            createImageThumb(display, file, savePath, meta, false);
        }
        if (meta != null && meta.exifDate == null) {
            meta.exifDate = ExifDate.getExifDateFromLastModified(file);
        }
        return meta;
    }

    /**
	 * @param display
	 *            Display
	 * @param mediaFile
	 *            original image
	 * @param savePath
	 *            path to thumb file or null
	 * @param asImage
	 *            if true ThumbModel will contain image, otherwise
	 *            thumbImageJPGArray
	 * @return ThumbModel
	 */
    public static ThumbProxy createImageThumbModel(Display display, MediaFile mediaFile, String savePath) {
        ThumbProxy tp = new ThumbProxy();
        if (IO.isJpeg(mediaFile.getName()) && mediaFile.getFile().length() > getMinimalThumbSize() * 10) {
            tp = getThumbProxyfromExif(display, mediaFile);
            if (tp != null) {
                byte[] thumbBytes = tp.getThumbBytes();
                if (tp.getThumbBytes() != null && savePath != null) {
                    if (tp.getPaintRect() != null) {
                        thumbBytes = SanExif.writeExifThumbInfo(thumbBytes, tp);
                    }
                    writeBytesToPath(savePath, thumbBytes);
                    if (mediaFile.getEntry() != null && mediaFile.getEntry().index > 0) {
                        ThumbCacheManager.getInstance().set(mediaFile.getEntry().index, thumbBytes, savePath);
                    }
                    return tp;
                } else if (tp.getThumbImage() != null && savePath != null) {
                    IO.saveImageSWT(tp.getThumbImage().getImageData(), savePath, SWT.IMAGE_JPEG, 85);
                    return tp;
                }
            }
        }
        if (tp == null) {
            tp = new ThumbProxy();
        }
        Image img = IO.loadImage(mediaFile.getAbsolutePath(), display, false, true);
        if (img == null) {
            tp = createError(display, mediaFile);
            return tp;
        }
        Rectangle bounds = img.getBounds();
        tp.setMediaFile(mediaFile);
        Point size = getThumbSize(bounds.width, bounds.height);
        if (size != null) {
            Image thumb = ImageConversion.createQualityThumbImage(img, size.x, size.y, SWT.HIGH);
            if (savePath != null && (mediaFile.getFile().length() > getMinimalThumbSize() || IO.isPsdImage(mediaFile.getName()))) {
                IO.saveImageSWT(thumb.getImageData(), savePath, SWT.IMAGE_JPEG, 85);
            } else {
                tp.isIcon = true;
            }
            tp.setThumbWidth(thumb.getBounds().width);
            tp.setThumbHeight(thumb.getBounds().height);
            tp.thumbImages[0] = thumb;
            img.dispose();
        } else {
            tp.thumbImages[0] = img;
            tp.isIcon = true;
            tp.setThumbWidth(bounds.width);
            tp.setThumbHeight(bounds.height);
        }
        return tp;
    }

    public static boolean writeBytesToPath(String savePath, byte[] thumbBytes) {
        try {
            FileUtils.writeByteArrayToFile(new File(savePath), thumbBytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] readBytesFromPath(String savePath) {
        try {
            return FileUtils.readFileToByteArray(new File(savePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ThumbProxy createOrLoadThumbProxy(final MediaFile mediaFile, final Display display, IPath thumbdir, File thumbFile2) {
        if (mediaFile == null || thumbdir == null) {
            System.err.println("ThumbIO loadThumb null: " + mediaFile + " " + thumbdir);
            return createErrorThumb(mediaFile, display, false);
        }
        if (!IO.isValidFilePath(mediaFile.getAbsolutePath())) {
            return null;
        }
        ThumbProxy thumbProxy = null;
        File thumbFile = null;
        if (thumbFile2 != null) {
            thumbFile = thumbFile2;
        } else {
            thumbFile = ThumbFileUtils.getThumbFile(mediaFile.getName(), thumbdir);
        }
        if (thumbFile == null) {
            return createErrorThumb(mediaFile, display, false);
        }
        if (IO.isValidImageFile(mediaFile.getName())) {
            if (!thumbFile.exists() || isModified(mediaFile.getFile(), thumbFile)) {
                thumbProxy = createImageThumbModel(display, mediaFile, thumbFile.getAbsolutePath());
                thumbProxy.setMediaFile(mediaFile);
                thumbProxy.setThumbBytes(null);
                thumbProxy.isNew = true;
                if (!thumbProxy.isIcon) {
                    thumbProxy.thumbPath = thumbFile.getAbsolutePath();
                }
                setDateModified(mediaFile.getFile(), thumbFile);
            } else {
                thumbProxy = new ThumbProxy();
                thumbProxy.setMediaFile(mediaFile);
                thumbProxy = loadThumb(display, thumbFile.getAbsolutePath(), thumbProxy);
            }
        } else if (IO.isValidVideoOrLinkFile(mediaFile.getName())) {
            if (!thumbFile.exists()) {
                if (ThumbBuildManager.getInstance().hasJob()) {
                    thumbProxy = createVideoErrorThumb(mediaFile, display, thumbFile, false, false);
                } else {
                    thumbProxy = createVideoThumb(mediaFile, display, thumbdir, thumbFile, null);
                }
            } else {
                thumbProxy = new ThumbProxy();
                thumbProxy.setMediaFile(mediaFile);
                thumbProxy = loadThumb(display, thumbFile.getAbsolutePath(), thumbProxy);
                thumbProxy.isVideo = true;
                thumbProxy.thumbPath = thumbFile.getAbsolutePath();
            }
        }
        return thumbProxy;
    }

    public static boolean createSWTThumb(Display display, File file, File savePath, MetaThumbData mdata, boolean returnThumb) {
        if (file == null || savePath == null || display == null) {
            return false;
        }
        Image img = IO.loadImage(file.getAbsolutePath(), display, false, true);
        Rectangle bounds = img.getBounds();
        int width = ThumbIO.DEFAULT_SIZE_X;
        int height = ThumbIO.DEFAULT_SIZE_Y;
        if (bounds.height > bounds.width) {
            width = ThumbIO.DEFAULT_SIZE_Y;
            height = ThumbIO.DEFAULT_SIZE_X;
        }
        Image thumb = null;
        if (bounds.width < ThumbIO.DEFAULT_SIZE_X && bounds.height < ThumbIO.DEFAULT_SIZE_X) {
            thumb = img;
        } else {
            thumb = ImageConversion.createQualityThumbImage(img, width, height, SWT.HIGH);
        }
        if (thumb == null) {
            return false;
        }
        IO.saveImageSWT(thumb.getImageData(), savePath.getAbsolutePath(), SWT.IMAGE_JPEG, 85);
        deleteLargeThumbImage(savePath.getAbsolutePath());
        if (mdata != null) {
            mdata.width = thumb.getBounds().width;
            mdata.height = thumb.getBounds().height;
        }
        if (returnThumb && mdata != null) {
            mdata.thumb = thumb;
        } else {
            thumb.dispose();
            thumb = null;
        }
        if (!img.equals(thumb) && !img.isDisposed()) {
            img.dispose();
            img = null;
        }
        return true;
    }

    public static Image createSWTThumb(Display display, String file, String savePath, Point size) {
        Image img = IO.loadImage(file, display, false, true);
        if (size != null) {
            Image thumb = ImageConversion.createQualityThumbImage(img, size.x, size.y, SWT.HIGH);
            IO.saveImageSWT(thumb.getImageData(), savePath, SWT.IMAGE_JPEG, 85);
            img.dispose();
            img = null;
            return thumb;
        }
        img.dispose();
        img = null;
        return null;
    }

    protected static ThumbProxy createVideoErrorThumb(final MediaFile mediaFile, final Display display, boolean disposeImage) {
        ThumbProxy thumb = new ThumbProxy(mediaFile, null, DEFAULT_SIZE_X, DEFAULT_SIZE_Y);
        Image v = ImageHelper.createImage(null, "/icons/large/video-error.gif");
        thumb.setThumbImages(IO.createErrorImage(display, v, mediaFile.getName(), DEFAULT_SIZE_X, DEFAULT_SIZE_Y));
        thumb.isVideo = true;
        return thumb;
    }

    public static ThumbProxy createVideoErrorThumb(final MediaFile mediaFile, final Display display, File thumbFile, boolean disposeImage, boolean save) {
        ThumbProxy thumbProxy;
        thumbProxy = createVideoErrorThumb(mediaFile, display, disposeImage);
        thumbProxy.thumbPath = thumbFile.getAbsolutePath();
        thumbProxy.setMediaFile(mediaFile);
        thumbProxy.isUnreadable = true;
        if (thumbProxy.getThumbImage() != null) {
            if (save) {
                IO.saveImageSWT(thumbProxy.getThumbImage().getImageData(), thumbFile.getAbsolutePath(), SWT.IMAGE_JPEG, 85);
            }
            if (disposeImage) {
                thumbProxy.getThumbImage().dispose();
                thumbProxy.setThumbImages(null);
                thumbProxy.thumbImages = null;
            }
        }
        return thumbProxy;
    }

    public static ThumbProxy createVideoLinkThumb(final String videoFile, IFile linkFile, final Display display, IProgressMonitor monitor) {
        if (videoFile == null) {
            return null;
        }
        File originalFile = new File(videoFile);
        if (!originalFile.exists()) {
            return null;
        }
        File thumbSavePath = ThumbFileUtils.getDefaultThumbPath(linkFile);
        if (thumbSavePath == null) {
            return null;
        }
        Path thumbdir = new Path(thumbSavePath.getParentFile().getAbsolutePath());
        ThumbProxy thumbProxy = null;
        FFmpeg ffmpeg = FFmpeg.getInstance();
        VideoInfo result = ffmpeg.createFilmstrip(videoFile, NR_OF_VIDEO_THUMBS, thumbdir.toFile().getAbsolutePath(), monitor);
        if (result == null) {
            boolean save = !ffmpeg.isRunning();
            thumbProxy = createVideoErrorThumb(new MediaFile(originalFile), display, thumbSavePath, false, save);
        } else {
            List<File> tmpFiles = new ArrayList<File>();
            List<Image> images = loadVideoThumbs(display, thumbdir, originalFile.getName(), tmpFiles);
            thumbProxy = createFilmStripThumb(new MediaFile(linkFile), display, thumbSavePath, images);
            for (File tmp : tmpFiles) {
                try {
                    tmp.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        thumbProxy.setVideoInfo(result);
        return thumbProxy;
    }

    public static ThumbProxy createVideoThumb(final IFile videoFile, final Display display, IProgressMonitor monitor) {
        if (videoFile == null) {
            return null;
        }
        final IPath location = videoFile.getLocation();
        if (location == null) {
            return null;
        }
        File file = location.toFile();
        if (!file.exists()) {
            return null;
        }
        File savePath = ThumbFileUtils.getDefaultThumbPath(videoFile);
        if (savePath == null) {
            return null;
        }
        Path thumbdir = new Path(savePath.getParentFile().getAbsolutePath());
        return createVideoThumb(new MediaFile(videoFile), display, thumbdir, savePath, null);
    }

    public static ThumbProxy createVideoThumb(final MediaFile mediaFile, final Display display, IPath thumbdir, File thumbFile, IProgressMonitor monitor) {
        if (mediaFile.isFile()) {
            return createVideoErrorThumb(mediaFile, display, thumbFile, false, false);
        }
        ThumbProxy thumbProxy;
        FFmpeg ffmpeg = FFmpeg.getInstance();
        VideoInfo result = ffmpeg.createFilmstrip(mediaFile.getAbsolutePath(), NR_OF_VIDEO_THUMBS, thumbdir.toFile().getAbsolutePath(), monitor);
        if (result == null) {
            boolean save = !ffmpeg.isRunning();
            thumbProxy = createVideoErrorThumb(mediaFile, display, thumbFile, false, save);
        } else {
            List<File> tmpFiles = new ArrayList<File>();
            List<Image> images = loadVideoThumbs(display, thumbdir, mediaFile.getName(), tmpFiles);
            thumbProxy = createFilmStripThumb(mediaFile, display, thumbFile, images);
            for (File tmp : tmpFiles) {
                try {
                    tmp.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        thumbProxy.setVideoInfo(result);
        return thumbProxy;
    }

    public static void deleteLargeThumbImage(String thumbPath) {
        if (thumbPath == null) {
            return;
        }
        File largeThumb = getThumbLargeFile(thumbPath);
        if (largeThumb.exists()) {
            try {
                largeThumb.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteThumbModel(ThumbProxy thumbModel) {
        if (thumbModel != null) {
            deleteThumbModel(thumbModel.thumbPath);
        }
    }

    public static void deleteThumbModel(String thumbPath) {
        if (!StringUtil.isEmpty(thumbPath)) {
            try {
                File f = new File(thumbPath);
                if (f.exists()) {
                    f.delete();
                }
                IPath ext = new Path(thumbPath).removeFileExtension().addFileExtension("jpeg");
                f = ext.toFile();
                if (f.exists()) {
                    f.delete();
                }
                f = new File(thumbPath + ".xml");
                if (f.exists()) {
                    f.delete();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteThumb(IFile resource) {
        IPath dir = ThumbFileUtils.getDefaultThumbContainer(resource.getParent().getFullPath());
        File thumbFile = ThumbFileUtils.getThumbFile(resource.getName(), dir);
        if (thumbFile == null || !thumbFile.exists()) {
            return;
        }
        deleteThumbModel(thumbFile.getAbsolutePath());
    }

    public static void deleteThumb(File resource) {
        IPath folder = Path.fromPortableString(resource.getParentFile().getName());
        IPath dir = ThumbFileUtils.getExplorerThumbContainer(folder);
        File thumbFile = ThumbFileUtils.getThumbFile(resource.getName(), dir);
        if (thumbFile == null || !thumbFile.exists()) {
            return;
        }
        deleteThumbModel(thumbFile.getAbsolutePath());
    }

    public static void deleteThumbImage(ThumbProxy thumbProxy) {
        if (thumbProxy.getThumbImage() != null) {
            thumbProxy.getThumbImage().dispose();
            thumbProxy.setThumbImages(null);
            thumbProxy.thumbImages = null;
        }
    }

    public static Dimension getimageSize(File f) {
        Dimension imageSize = null;
        try {
            imageSize = Sanselan.getImageSize(f);
        } catch (ImageReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageSize;
    }

    public static long getMinimalThumbSize() {
        return MINSize;
    }

    public static File getThumbLargeFile(String thumbPath) {
        IPath ext = new Path(thumbPath).removeFileExtension().addFileExtension("jpeg");
        File thumbFile = ext.toFile();
        return thumbFile;
    }

    private static ThumbProxy getThumbProxyfromExif(Display display, MediaFile mediaFile) {
        MetaThumbData m = SanExif.getExifThumbImage(display, mediaFile.getAbsolutePath(), true);
        if (m != null && m.thumb != null) {
            ThumbProxy tm = new ThumbProxy();
            tm.setMediaFile(mediaFile);
            tm.setThumbHeight(m.height);
            tm.setThumbWidth(m.width);
            tm.setThumbImages(m.thumb);
            tm.setThumbBytes(m.imageData);
            tm.setPaintRect(m.paintRect);
            return tm;
        }
        return null;
    }

    public static Point getThumbSize(int width, int height) {
        if (width <= DEFAULT_SIZE_X + 32 && height <= DEFAULT_SIZE_X + 32) {
            return null;
        }
        return new Point(DEFAULT_SIZE_X, DEFAULT_SIZE_X);
    }

    public static boolean hasIconImageSize(Rectangle bounds) {
        return bounds.width < 33 && bounds.height < 33;
    }

    public static boolean isModified(File file, File thumbFile) {
        if (file == null || thumbFile == null || !file.exists() || !thumbFile.exists()) {
            return false;
        }
        try {
            if (file.lastModified() > thumbFile.lastModified()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isValidSize(Dimension d) {
        return d.width > DEFAULT_SIZE_X + 20 || d.height > DEFAULT_SIZE_X + 20;
    }

    public static Image loadLargeThumbImage(String mediaPath, String thumbPath, Display display) {
        if (thumbPath == null) {
            return null;
        }
        File thumbFile = getThumbLargeFile(thumbPath);
        if (thumbFile.exists()) {
            return IO.loadImage(thumbFile.getAbsolutePath(), display, false, false);
        }
        Image largeThumb = null;
        if (mediaPath != null) {
            File original = new File(mediaPath);
            if (!original.exists()) {
                return null;
            }
            if (IO.isJpeg(original.getName())) {
                Dimension d = getimageSize(original);
                if (d != null) {
                    if (!isValidSize(d)) {
                        return null;
                    } else if (d.width < 1000 && d.height < 1000) {
                        largeThumb = createSWTThumb(display, mediaPath, thumbFile.getAbsolutePath(), new Point(LARGE_SIZE, LARGE_SIZE));
                    } else if (d.width < 1601 && d.height < 1601) {
                        IJGLib.createFastResizeImage(mediaPath, thumbFile.getAbsolutePath(), "1/4", "80");
                        if (thumbFile.exists()) {
                            largeThumb = IO.loadImage(thumbFile.getAbsolutePath(), display, false, false);
                        }
                    } else if (d.width < 3600 && d.height < 3600) {
                        IJGLib.createFastResizeImage(mediaPath, thumbFile.getAbsolutePath(), "1/8", "80");
                        if (thumbFile.exists()) {
                            largeThumb = IO.loadImage(thumbFile.getAbsolutePath(), display, false, false);
                        }
                    } else {
                        IJGLib.createFastResizeImage(mediaPath, thumbFile.getAbsolutePath(), "1/16", "80");
                        if (thumbFile.exists()) {
                            largeThumb = IO.loadImage(thumbFile.getAbsolutePath(), display, false, false);
                        }
                    }
                }
                if (largeThumb == null) {
                    largeThumb = createSWTThumb(display, mediaPath, thumbFile.getAbsolutePath(), new Point(LARGE_SIZE, LARGE_SIZE));
                }
            } else if (IO.isValidImageFile(original.getName())) {
                Dimension d = getimageSize(original);
                if (d != null) {
                    if (isValidSize(d)) {
                        return null;
                    }
                }
                largeThumb = createSWTThumb(display, mediaPath, thumbFile.getAbsolutePath(), new Point(LARGE_SIZE, LARGE_SIZE));
            }
        }
        return largeThumb;
    }

    public static ThumbProxy loadThumb(Display display, String filePath, ThumbProxy thumbModel) {
        if (filePath == null || display == null) {
            return null;
        }
        if (thumbModel == null) {
            thumbModel = new ThumbProxy();
        }
        ThumbProxy tp2 = null;
        if (thumbModel.getIndex() < 0) {
            tp2 = ThumbCacheManager.getInstance().get(filePath);
        }
        if (tp2 == null || tp2.getThumbImage() == null) {
            tp2 = ThumbCacheManager.getInstance().getThumbImage(thumbModel.getIndex(), filePath);
        }
        if (tp2 == null || tp2.getThumbImage() == null) {
            ThumbProxy tm = createError(display, new MediaFile(new File(filePath)));
            return tm;
        }
        thumbModel.setThumbImages(tp2.getThumbImage());
        thumbModel.thumbPath = filePath;
        Rectangle paintRect = tp2.getPaintRect();
        if (paintRect != null) {
            thumbModel.setPaintRect(paintRect);
            thumbModel.setThumbHeight(paintRect.height);
            thumbModel.setThumbWidth(paintRect.width);
        } else {
            Rectangle bounds = tp2.getThumbImage().getBounds();
            thumbModel.setThumbHeight(bounds.height);
            thumbModel.setThumbWidth(bounds.width);
        }
        thumbModel.setIndex(tp2.getIndex());
        return thumbModel;
    }

    public static List<Image> loadVideoThumbs(final Display display, IPath folder, String name, List<File> imageTmpFiles) {
        List<Image> images = new ArrayList<Image>();
        for (int i = 0; i < 10; i++) {
            String thumbName = name + "." + i + ".jpg";
            IPath thumbPath = folder.append(thumbName);
            File thumbFile = thumbPath.toFile();
            if (thumbFile.exists()) {
                Image img = IO.loadImage(thumbFile.getAbsolutePath(), display, false, false);
                if (img != null) {
                    images.add(img);
                    if (imageTmpFiles != null) {
                        imageTmpFiles.add(thumbFile);
                    }
                } else {
                    try {
                        thumbFile.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                break;
            }
        }
        return images;
    }

    public static void saveThumb(ImageData d, String thumbPath) {
        IO.saveImageSWT(d, thumbPath, SWT.IMAGE_JPEG, 90);
    }

    public static void setDateModified(File file, File thumbFile) {
        if (file == null || thumbFile == null || !file.exists() || !thumbFile.exists()) {
            return;
        }
        try {
            thumbFile.setLastModified(file.lastModified());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Dimension setThumbSizeToMeta(MetaThumbData meta, File file) {
        Dimension s = getimageSize(file);
        if (s != null) {
            if (s.width <= 32 && s.height <= 32) {
                meta.height = DEFAULT_ICONSIZE;
                meta.width = DEFAULT_ICONSIZE;
            } else if (s.width < DEFAULT_SIZE_X && s.height < DEFAULT_SIZE_X) {
                meta.height = s.height;
                meta.width = s.width;
            } else {
                Point fit = ImageConversion.fitToThumb2(s.width, s.height, DEFAULT_SIZE_X, DEFAULT_SIZE_X);
                meta.height = fit.y;
                meta.width = fit.x;
            }
        } else {
            meta.height = DEFAULT_SIZE_X;
            meta.width = DEFAULT_SIZE_X;
        }
        return s;
    }

    public ThumbIO() {
    }
}
