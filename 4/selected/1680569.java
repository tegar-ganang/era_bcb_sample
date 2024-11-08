package whf.framework.util.images;

import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import whf.framework.log.Log;
import whf.framework.log.LogFactory;

/**
 * 将一个大的图片文件缩小成一个小的图片文件
 * @author wanghaifeng
 * @create Nov 10, 2006 9:17:28 PM
 * 
 */
public class ScaleImage implements Runnable {

    private static Log log = LogFactory.getLog(ScaleImage.class);

    /**
	 * 新的文件名，全路径
	 * @property String:imageName
	 */
    private String imageName;

    private String oldname;

    private float xtrans = 0.0F;

    private float ytrans = 0.0F;

    private Interpolation interpolation = null;

    /**
	 * 文件扩展名
	 * @property String:ext
	 */
    private String ext;

    private String storePath = "c:\\upload\\scaleimage\\";

    /**
	 * 转变后的图片宽度
	 * @property int:width
	 */
    private int width = 80;

    /**
	 * 转变后的图片高度
	 * @property int:height
	 */
    private int height = 120;

    /**
	 * 图片的最大宽度，与maxheight结合使用，将图片按照原来的宽和高进行缩放，但是缩小后的图片的宽和高不能大于对应的最大值（非0）
	 * @property int:maxWidth
	 */
    private int maxWidth = 0;

    private int maxHeight = 0;

    /**
	 * 如果被设置宽度和高度将由原图片的倍数进行决定
	 * @property float:scale
	 */
    private float scale = 0.0f;

    /**
	 * 原始图片宽度
	 * @property int:userImageWidth
	 */
    private int sourceImageWidth = 0;

    /**
	 * 原是图片高度
	 * @property int:userImageHeight
	 */
    private int sourceImageHeight = 0;

    /**
	 * @param sourceFileName 图片文件名（全路径）
	 */
    public ScaleImage(String sourceFileName, String storePathOrFile) {
        this.oldname = sourceFileName;
        this.storePath = storePathOrFile;
        File file = new File(storePathOrFile);
        if (file.isFile()) {
            imageName = storePathOrFile;
            ext = imageName.substring(imageName.lastIndexOf(".") + 1);
        } else {
            imageName = createName(oldname);
        }
    }

    public ScaleImage(String sourceFileName, String storePathOrFile, int width, int height) {
        this(sourceFileName, storePathOrFile);
        this.width = width;
        this.height = height;
    }

    public ScaleImage(String sourceFileName, String storePathOrFile, int width, int height, float xtrans, float ytrans) {
        this(sourceFileName, storePathOrFile, width, height);
        this.xtrans = xtrans;
        this.ytrans = ytrans;
    }

    public void setProperties(ScaleProperty props) {
        if (props == null) return;
        this.setWidth(props.getWidth());
        this.setHeight(props.getHeight());
        this.setScale(props.getScale());
        this.setMaxHeight(props.getMaxHeight());
        this.setMaxWidth(this.getMaxWidth());
    }

    public void setWidth(int w) {
        width = w;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int h) {
        height = h;
    }

    public int getHeight() {
        return height;
    }

    /**
	 * @return Returns the maxHeight.
	 */
    public int getMaxHeight() {
        return maxHeight;
    }

    /**
	 * @param maxHeight The maxHeight to set.
	 */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
	 * @return Returns the maxWidth.
	 */
    public int getMaxWidth() {
        return maxWidth;
    }

    /**
	 * @param maxWidth The maxWidth to set.
	 */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setXtrans(float x) {
        xtrans = x;
    }

    public float getXtrans() {
        return xtrans;
    }

    public void setYtrans(float y) {
        ytrans = y;
    }

    public float getYtrans() {
        return ytrans;
    }

    public void setInterpolation(Interpolation i) {
        interpolation = i;
    }

    public Interpolation getInterpolation() {
        return interpolation;
    }

    public int getSourceImageWidth() {
        return sourceImageWidth;
    }

    public int getSourceImageHeight() {
        return sourceImageHeight;
    }

    public String getImageName() {
        return imageName;
    }

    public String getExt() {
        return this.ext;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    private String createName(String imgname) {
        String temp = null;
        if (imgname != null) {
            int count = 0;
            int length = imgname.lastIndexOf(".");
            this.ext = imgname.substring(length + 1);
            temp += ext;
            count++;
            int end_string = imgname.lastIndexOf("\\");
            temp = imgname.substring(end_string + 1, imgname.length());
            temp = storePath + temp;
        }
        return temp;
    }

    /**
	 * The method get an image file of any JAI supported formats, such as GIF,
	 * JPEG, TIFF, BMP, PNM, PNG, etc.
	 */
    public String getImageExtType(String filext) throws IllegalArgumentException {
        String _filext = filext.toUpperCase();
        if (_filext != null) {
            if (_filext.equals("BMP")) return _filext; else if (_filext.equals("GIF")) return _filext; else if (_filext.equals("JPG") || _filext.equals("JPEG")) {
                _filext = "JPEG";
                return _filext;
            } else if (_filext.equals("PNM")) return _filext; else if (_filext.equals("PNG")) return _filext; else if (_filext.equals("TIFF")) return _filext; else throw new IllegalArgumentException("The image format is not supported by JAI (1135).");
        } else return null;
    }

    private PlanarImage loadImage(String imageName) throws IOException {
        FileSeekableStream stream = new FileSeekableStream(imageName);
        RenderedOp image1 = JAI.create("stream", stream);
        sourceImageWidth = image1.getWidth();
        sourceImageHeight = image1.getHeight();
        if (this.scale > 0) {
            this.width = (int) (this.scale * sourceImageWidth);
            this.height = (int) (this.scale * sourceImageHeight);
        }
        if (this.maxHeight > 0 || this.maxWidth > 0) {
            float newscale = 0.0f;
            float xscale = (float) (this.maxWidth == 0 ? this.width : this.maxWidth) / this.sourceImageWidth;
            float yscale = (float) (this.maxHeight == 0 ? this.height : this.maxHeight) / this.sourceImageHeight;
            if (xscale > 1 || yscale > 1) {
                newscale = Math.max(xscale, yscale);
            } else {
                newscale = Math.min(xscale, yscale);
            }
            if (newscale != scale && newscale > 0) {
                this.scale = newscale;
                this.width = (int) (this.scale * sourceImageWidth);
                this.height = (int) (this.scale * sourceImageHeight);
            }
        }
        if (interpolation == null) interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        ParameterBlock params = new ParameterBlock();
        params.addSource(image1);
        float xscale = (float) width / sourceImageWidth;
        float yscale = (float) height / sourceImageHeight;
        params.add(new Float(xscale));
        params.add(new Float(yscale));
        params.add(new Float(xtrans));
        params.add(new Float(ytrans));
        params.add(interpolation);
        PlanarImage src = JAI.create("scale", params);
        if (src == null) {
            params = null;
            log.info("Error in loading image " + imageName);
            return null;
        }
        return src;
    }

    private void encodeImage(PlanarImage img, FileOutputStream out, String imgext) throws IOException {
        String extype = getImageExtType(imgext);
        ImageEncoder encoder = ImageCodec.createImageEncoder(extype, out, null);
        encoder.encode(img);
        out.close();
    }

    private void writeGif(String filename, String outputFile) throws IOException {
        File file = new File(filename);
        InputStream in = new FileInputStream(file);
        FileOutputStream fout = new FileOutputStream(outputFile);
        int totalRead = 0;
        int readBytes = 0;
        int blockSize = 65000;
        long fileLen = file.length();
        byte b[] = new byte[blockSize];
        while ((long) totalRead < fileLen) {
            readBytes = in.read(b, 0, blockSize);
            totalRead += readBytes;
            fout.write(b, 0, readBytes);
        }
        in.close();
        fout.close();
    }

    public synchronized void run() {
        try {
            if (ext.equalsIgnoreCase("gif")) writeGif(oldname, imageName); else {
                PlanarImage pi = loadImage(oldname);
                FileOutputStream fout = new FileOutputStream(imageName);
                encodeImage(pi, fout, ext);
            }
        } catch (Exception e) {
            log.error(this, e);
        }
    }

    public String getDescription() {
        return "Scale image file";
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void main(String[] args) throws Exception {
        ScaleImage si = new ScaleImage("E:\\photos\\20061031\\IMG_2627.jpg", "E:\\tmp\\", 200, 150);
        si.setMaxHeight(160);
        si.run();
        log.debug(si);
    }
}
