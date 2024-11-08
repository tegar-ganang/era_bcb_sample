package whf.file.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import whf.framework.log.Log;
import whf.framework.log.LogFactory;
import whf.framework.resource.thread.AbstractPoolableThread;
import whf.framework.util.images.ScaleImage;
import whf.framework.util.images.ScaleProperty;

/**
 * 将文件写入硬盘的服务
 * @author wanghaifeng
 * @create Nov 14, 2006 10:38:49 PM
 * 
 */
public class FormImageFileSaveThread extends AbstractPoolableThread {

    private static Log log = LogFactory.getLog(FormImageFileSaveThread.class);

    /**
	 * key:加入到文件名中的标志
	 * value：转换图片属性
	 * @property Map<String,ScaleProperties>:props
	 */
    private Map<String, ScaleProperty> props;

    private String fileName;

    private InputStream data;

    public FormImageFileSaveThread(InputStream data, String fileName, Map<String, ScaleProperty> props) {
        this.data = data;
        this.fileName = fileName;
        this.props = props;
    }

    @Override
    public void onTimeout() {
    }

    public String getFileName() {
        return this.fileName;
    }

    public void run() {
        try {
            byte[] buff = new byte[1024];
            FileOutputStream fos = new FileOutputStream(this.fileName);
            int readCount = data.read(buff);
            while (readCount >= 0) {
                fos.write(buff, 0, readCount);
                readCount = data.read(buff);
            }
        } catch (Exception e) {
            log.error(this, e);
        }
        if (this.props != null) {
            for (String name : this.props.keySet()) {
                ScaleProperty prop = (ScaleProperty) this.props.get(name);
                if (prop == null) continue;
                String fn = whf.file.entity.File.convertIconFileName(this.fileName, name);
                File file = new File(fn);
                try {
                    file.createNewFile();
                    ScaleImage scaleImage = new ScaleImage(this.fileName, fn);
                    scaleImage.setProperties(prop);
                    scaleImage.run();
                } catch (Exception e) {
                    log.error(this, e);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String fileName = "e:\\p\\c.xml";
        String fn = fileName.substring(0, fileName.lastIndexOf("."));
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        System.out.println(fn + ":" + ext);
    }
}
