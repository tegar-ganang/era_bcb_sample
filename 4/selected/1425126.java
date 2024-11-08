package img_getter.img;

import img_getter.Img_getterView;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author daoyu
 */
public class ImgDownloadThread extends Thread {

    private int height, width;

    private String fileName, filePath;

    private Img_getterView view;

    private String[] acceptFormats;

    public ImgDownloadThread(String fn, int w, int h, String _filePath, String[] _acceptFormats, Img_getterView _view) {
        fileName = fn;
        width = w;
        height = h;
        filePath = _filePath;
        acceptFormats = _acceptFormats;
        view = _view;
    }

    private boolean checkImg(ImageReader ir) throws IOException {
        if (ir == null) {
            return false;
        }
        return ir.getHeight(0) >= this.height && ir.getWidth(0) >= this.width;
    }

    private String getRealName(String fullUrl) {
        String realName = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
        if (realName.contains("?")) {
            realName = realName.substring(0, realName.indexOf("?"));
        }
        return realName;
    }

    private String getFormat(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @Override
    public void run() {
        String localName = getRealName(fileName);
        String format = getFormat(localName);
        for (String _format : acceptFormats) {
            if (_format.equalsIgnoreCase(format)) {
                ImageReader ir = null;
                try {
                    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
                    ir = readers.next();
                    ir.setInput(ImageIO.createImageInputStream(new URL(fileName).openStream()));
                    if (this.checkImg(ir)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(filePath).append(localName);
                        File outTemp = new File(sb.toString());
                        if (!outTemp.exists()) {
                            ImageIO.write(ir.read(0), format, outTemp);
                            view.log("成功下载: " + sb.toString());
                        }
                    }
                } catch (IllegalArgumentException iie) {
                    view.log("由于没有找到图片解码器，无法下载: " + fileName);
                } catch (IOException e) {
                    view.log("由于网络或错误的图像地址，无法下载: " + fileName);
                } finally {
                    if (ir != null) {
                        ImageInputStream input = (ImageInputStream) (ir.getInput());
                        if (input != null) {
                            try {
                                input.close();
                            } catch (IOException e) {
                                view.log("无法关闭文件读取流。");
                            }
                        }
                        ir.abort();
                        ir.dispose();
                    }
                }
                return;
            }
        }
    }
}
